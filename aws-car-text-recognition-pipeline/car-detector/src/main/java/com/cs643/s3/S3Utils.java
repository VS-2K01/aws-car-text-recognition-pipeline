package com.cs643.s3;
// Import Statements
import com.cs643.util.KeySort;
import com.cs643.util.Retry;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// S3 helper functions used by the car-detector (EC2-A).
public final class S3Utils {
    private S3Utils(){}

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
                String k = o.key();
                if (k.toLowerCase().endsWith(".jpg")) keys.add(k);
            });
            // If S3 gave us a next page token, keep going
            token = resp.nextContinuationToken();
            // We check if there is a continuation token from the S3 or we have gotten enough images, if so end loop
        } while (token != null && keys.size() < n);

        // Sort numerically by the number in the filename
        keys.sort(KeySort.numericJpgs());
        // Trim to N if we got more than needed
        if (keys.size() > n) return new ArrayList<>(keys.subList(0, n));
        return keys;
    }

    // Download an S3 object (bucket/key) to the local filesystem at 'dest'.
    public static Path downloadToFile(S3Client s3, String bucket, String key, Path dest) {
        File f = dest.toFile();
        // Ensure local directory exists
        f.getParentFile().mkdirs();
        GetObjectRequest req = GetObjectRequest.builder().bucket(bucket).key(key).build();
        // Retry Logic
        Retry.withBackoff("s3-get", 4, () -> {
            s3.getObject(req, ResponseTransformer.toFile(f.toPath()));
            return true;
        });
        return f.toPath();
    }
}
