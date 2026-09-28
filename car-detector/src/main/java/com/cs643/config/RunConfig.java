package com.cs643.config;
// Import Statements
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

// Parses CLI flags and creates a typed config object.
public class RunConfig {
    // Required CLI args
    public final String bucket;        // S3 bucket that holds the images
    public final String queueUrl;      // SQS queue to send filenames to
    public final int maxImages;        // how many images to scan (default 10)
    public final Path outputFile;      // ec2_b uses this; ec2_a leaves null
    // Initialize RunConfig object based on parameters
    private RunConfig(String bucket, String queueUrl, int maxImages, Path outputFile) {
        this.bucket = bucket;
        this.queueUrl = queueUrl;
        this.maxImages = maxImages;
        this.outputFile = outputFile;
    }

    // Take CLI Args and Initialize RunConfig object
    public static RunConfig fromArgs(String[] args) {
        Map<String,String> kv = parse(args);
        // Validate & read args
        String bucket = need(kv, "--bucket");
        String queueUrl = need(kv, "--queue-url");
        int maxImages = parseIntOrDefault(kv.get("--max-images"), 10);
        Path out = kv.containsKey("--out") ? Path.of(kv.get("--out")) : null;
        return new RunConfig(bucket, queueUrl, maxImages, out);
    }

    // Parse CLI args into a Map of key-value pairs by scanning args
    private static Map<String,String> parse(String[] args) {
        Map<String,String> m = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--")) {
                String v = (i + 1 < args.length && !args[i+1].startsWith("--")) ? args[++i] : "";
                m.put(a, v);
            }
        }
        return m;
    }

    // Ensure that required CLI flags exist. If no, then throw IllegalArgumentException.
    private static String need(Map<String,String> kv, String key) {
        String v = kv.get(key);
        if (v == null || v.isBlank())
            throw new IllegalArgumentException("Missing required arg: " + key);
        return v;
    }

    // Parse string to int with fallback return default if value is blank or not a valid number.
    private static int parseIntOrDefault(String v, int d) {
        try { return (v == null || v.isBlank()) ? d : Integer.parseInt(v); }
        catch (NumberFormatException e) { return d; }
    }
}
