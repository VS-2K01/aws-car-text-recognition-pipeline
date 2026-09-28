package com.cs643.s3;
// Import Statements
import com.cs643.util.KeySort;                  
import com.cs643.util.Retry;                    

// AWS SDK v2 imports
import software.amazon.awssdk.core.ResponseInputStream;  
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

// Java stdlib imports
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

// S3 helper functions used by the text-reader (EC2-B).
public final class S3Utils {
    private S3Utils() {}

    // Return 'n' keys ending with ".jpg" from the specified bucket sorted numerically
    public static List<String> firstNImages(S3Client s3, String bucket, int n) {
        List<String> keys = new ArrayList<>();
        String token = null; // continuation token for pagination

        do {
            // Build a paginated list request of all keys
            ListObjectsV2Request req = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .continuationToken(token)
                    .build();

            // Call S3 with retry in case of transient network hiccups
            ListObjectsV2Response resp = Retry.withBackoff("s3-list", 4, () -> s3.listObjectsV2(req));

            // Collect only .jpg keys
            resp.contents().forEach(o -> {
                String key = o.key();
                if (key.toLowerCase().endsWith(".jpg")) {
                    keys.add(key);
                }
            });

            // If S3 gave us a next page token, keep going
            token = resp.nextContinuationToken();

            // We check if there is a continuation token from the S3 or we have gotten enough images, if so end loop
        } while (token != null && keys.size() < n);

        // Sort numerically by the number in the filename
        keys.sort(KeySort.numericJpgs());

        // Trim to N if we gathered more than needed
        if (keys.size() > n) {
            return new ArrayList<>(keys.subList(0, n));
        }
        return keys;
    }

    // Download an S3 object (bucket/key) to the local filesystem at 'dest'.
    public static Path downloadToFile(S3Client s3, String bucket, String key, Path dest) {
        // Ensure the parent directory exists (e.g., /home/ec2-user/tmp_images)
        try {
            Files.createDirectories(dest.getParent());
        } catch (IOException e) {
            // Convert checked IOException into an unchecked RuntimeException
            throw new RuntimeException("Failed to create parent dir for " + dest + ": " + e.getMessage(), e);
        }

        // Build the get request referencing the S3 object
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        // Perform the download with retry
        return Retry.withBackoff("s3-get", 4, () -> {
            // Open a streaming S3 GET request and auto-close the input stream when done.
            try (ResponseInputStream<GetObjectResponse> in = s3.getObject(req)) {
                // Stream the bytes to disk, overwriting if the file already exists
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException io) {
                // If writing fails throw a clear error
                throw new RuntimeException("Failed to write to " + dest + ": " + io.getMessage(), io);
            }
            return dest;
        });
    }
}
