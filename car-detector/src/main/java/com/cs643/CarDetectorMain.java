package com.cs643;
// Import Statements
import com.cs643.config.RunConfig;
import com.cs643.awssdk.AwsClients;
import com.cs643.model.Messages;
import com.cs643.s3.S3Utils;
import com.cs643.sqs.SqsUtils;
import com.cs643.rek.RekognitionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// EC2 A - Car Detector - Get first N images from S3 Bucket, Run Image Rekognition AWS API, Send Filename to SQS Queue if above 80% confidence
public class CarDetectorMain {
    private static final Logger log = LoggerFactory.getLogger(CarDetectorMain.class);

    public static void main(String[] args) {
        try {
            // Parse CLI flags (throws if required flags missing)
            RunConfig cfg = RunConfig.fromArgs(args);
            log.info("Starting car-detector with bucket={}, queue={}, maxImages={}",
                    cfg.bucket, cfg.queueUrl, cfg.maxImages);

            // Build AWS clients
            var s3  = AwsClients.s3();
            var sqs = AwsClients.sqs();
            var rek = AwsClients.rekognition();

            // Step 1: fetch up to N image keys from the bucket
            List<String> keys = S3Utils.firstNImages(s3, cfg.bucket, cfg.maxImages);
            log.info("Found {} candidate images", keys.size());

            // Step 2 & 3: run label detection, send qualifying filenames to SQS
            for (String key : keys) {
                boolean hasCar = RekognitionUtils.hasCarAbove80(rek, cfg.bucket, key);
                log.info("Image {} -> car? {}", key, hasCar);
                if (hasCar) {
                    SqsUtils.send(sqs, cfg.queueUrl, key);
                }
            }

            // Step 4: send special "-1" message to mark completion
            SqsUtils.send(sqs, cfg.queueUrl, Messages.SENTINEL);
            log.info("Done. Sent sentinel; exiting.");
        } catch (Exception e) {
            // If anything goes wrong, print a helpful message and non-zero exit
            System.err.println("FATAL: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

