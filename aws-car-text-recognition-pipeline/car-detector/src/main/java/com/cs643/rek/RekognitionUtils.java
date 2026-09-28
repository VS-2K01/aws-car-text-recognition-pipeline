package com.cs643.rek;
// Import Statement
import com.cs643.util.Retry;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

// Rekognition helper functions for car-detector: detect cars in images with above 80% confidence.
public final class RekognitionUtils {
    private RekognitionUtils(){}

    // Returns true if the image in S3 has label "Car" with confidence >= 80%. */
    public static boolean hasCarAbove80(RekognitionClient rek, String bucket, String key) {
        // Build a reference to an S3 object for Rekognition
        Image img = Image.builder()
                .s3Object(S3Object.builder().bucket(bucket).name(key).build())
                .build();

        // Ask Rekognition for labels with a minimum confidence of 80
        DetectLabelsRequest req = DetectLabelsRequest.builder()
                .image(img)
                .minConfidence(80f)
                .maxLabels(50)  
                .build();

        // Wrap Rekognition AWS API call in retry logic
        DetectLabelsResponse resp = Retry.withBackoff("rek-detectLabels", 3, () -> rek.detectLabels(req));

        // Look for "Car" label that meets the threshold
        for (Label lab : resp.labels()) {
            if ("Car".equalsIgnoreCase(lab.name()) && lab.confidence() != null && lab.confidence() >= 80f) {
                return true;
            }
        }
        return false;
    }
}
