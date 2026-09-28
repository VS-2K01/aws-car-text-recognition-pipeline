package com.cs643.awssdk;
// Import Statements
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;  // for region fallback
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProviderChain;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

// Initialize AWS SDK v2 clients to be used / reused by the entire program
public final class AwsClients {
    private AwsClients() {}
    private static volatile S3Client S3;
    private static volatile SqsClient SQS;
    private static volatile RekognitionClient REK;

    // Resolve AWS region from AwsRegionProviderChain, if none, then fall back to us-east-1.
    private static Region resolveRegion() {
        try { return new AwsRegionProviderChain().getRegion(); }
        catch (SdkClientException e) { return Region.US_EAST_1; }
    }
    // Initialize a S3Client using resolved region and default credentials.
    public static S3Client s3() {
        if (S3 == null) {
            synchronized (AwsClients.class) {
                if (S3 == null) {
                    S3 = S3Client.builder()
                            .region(resolveRegion())
                            .credentialsProvider(DefaultCredentialsProvider.create())
                            .build();
                }
            }
        }
        return S3;
    }
    // Initialize a SqsClient using resolved region and default credentials.
    public static SqsClient sqs() {
        if (SQS == null) {
            synchronized (AwsClients.class) {
                if (SQS == null) {
                    SQS = SqsClient.builder()
                            .region(resolveRegion())
                            .credentialsProvider(DefaultCredentialsProvider.create())
                            .build();
                }
            }
        }
        return SQS;
    }
    // Initialize a RekognitionClient using resolved region and default credentials.
    public static RekognitionClient rekognition() {
        if (REK == null) {
            synchronized (AwsClients.class) {
                if (REK == null) {
                    REK = RekognitionClient.builder()
                            .region(resolveRegion())
                            .credentialsProvider(DefaultCredentialsProvider.create())
                            .build();
                }
            }
        }
        return REK;
    }
}
