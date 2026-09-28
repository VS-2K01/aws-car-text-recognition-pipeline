package com.cs643.rek;
// Import Statements
import com.cs643.util.Retry;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

// Rekognition helper functions for text-reader: detect text lines with above 80% confidence.
public final class RekognitionUtils {
    private RekognitionUtils(){}

    // Detects text lines in a local image file using Rekognition; returns only text with above 80% confidence.
    public static List<String> detectTextLinesAbove80(RekognitionClient rek, Path localImage) {
        byte[] bytes;
        // Try/Catch - Read Image File Into Bytes - Catches Exception if read fails
        try { bytes = Files.readAllBytes(localImage); }
        catch (IOException e) { throw new RuntimeException("Read file failed: " + localImage, e); }
        // Using Image.builder() - take bytes read and build Image object
        Image img = Image.builder().bytes(SdkBytes.fromByteArray(bytes)).build();
        DetectTextRequest req = DetectTextRequest.builder().image(img).build();
        // Invoke Rekognition DetectText AWS API with exponential backoff (up to 3 attempts) and capture the response.
        DetectTextResponse resp = Retry.withBackoff("rek-detectText", 3, () -> rek.detectText(req));
        // Return a list of text lines with confidence above or equal to 80%, extracted from the Rekognition response.
        return resp.textDetections().stream()
                .filter(td -> td.type() == TextTypes.LINE && td.confidence() != null && td.confidence() >= 80f)
                .map(TextDetection::detectedText)
                .collect(Collectors.toList());
    }
}
