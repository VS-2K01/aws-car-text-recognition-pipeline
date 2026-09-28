package com.cs643;
//Import Statements
import com.cs643.config.RunConfig;
import com.cs643.awssdk.AwsClients;
import com.cs643.model.Messages;
import com.cs643.s3.S3Utils;
import com.cs643.sqs.SqsUtils;
import com.cs643.rek.RekognitionUtils;
import com.cs643.util.IoUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.model.Message;

import java.nio.file.Path;
import java.util.*;

/**
 * EC2-B Text Reader:
 *  - Long-polls SQS for ".jpg" filenames.
 *  - For each, downloads file from S3, runs Rekognition Text, keeps files with recognition above 80%.
 *  - Writes text recognition results to output.txt when there is no more photos to review - special "-1" message is received.
 */
public class TextReaderMain {
    private static final Logger log = LoggerFactory.getLogger(TextReaderMain.class);
    
    public static void main(String[] args) {
        try {
            RunConfig cfg = RunConfig.fromArgs(args);
            log.info("Starting text-reader with bucket={}, queue={}, out={}",
                    cfg.bucket, cfg.queueUrl, cfg.outputFile);

            var s3  = AwsClients.s3();
            var sqs = AwsClients.sqs();
            var rek = AwsClients.rekognition();

            Set<String> seen = new HashSet<>();  // de-dupe in case SQS redelivers
            List<String> results = new ArrayList<>();
            boolean producerDone = false;

            while (true) {
                // Long poll up to 20s; empty means nothing yet
                Optional<Message> om = SqsUtils.receiveOne(sqs, cfg.queueUrl);
                if (om.isEmpty()) {
                    if (producerDone) break; // no more msgs after sentinel seen -> stop
                    continue;                // otherwise keep waiting
                }

                Message m = om.get();
                String body = m.body();

                if (Messages.SENTINEL.equals(body)) {
                    // Mark that producer is done. We still keep polling until queue is empty.
                    producerDone = true;
                    SqsUtils.delete(sqs, cfg.queueUrl, m);
                    log.info("Saw sentinel; will exit once the queue is empty.");
                    continue;
                }

                if (seen.add(body)) {
                    // Download the image to a temp file (key may include folders → sanitize)
                    Path tmp = Path.of("/tmp", body.replace("/", "_"));
                    S3Utils.downloadToFile(s3, cfg.bucket, body, tmp);

                    // Run text detection (keep LINE detections >=80%)
                    var lines = RekognitionUtils.detectTextLinesAbove80(rek, tmp);
                    if (!lines.isEmpty()) {
                        results.add(body + ": \"" + String.join(" ", lines) + "\"");
                        log.info("Text found in {} -> {}", body, lines);
                    } else {
                        log.info("No high-confidence text in {}", body);
                    }
                }

                // Always delete the message after processing
                SqsUtils.delete(sqs, cfg.queueUrl, m);
            }

            // Write results to output file
            String out = String.join(System.lineSeparator(), results);
            if (!out.isEmpty()) out += System.lineSeparator();
            IoUtil.writeString(cfg.outputFile, out);
            log.info("Wrote {} lines to {}", results.size(), cfg.outputFile);

        } catch (Exception e) {
            System.err.println("FATAL: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
