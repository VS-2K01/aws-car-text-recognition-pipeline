CS643 - PROJECT 1 - Vivek Shah - README



---------------------------------------------------------------------

1\) Overview

---------------------------------------------------------------------



The goal of this project is to implement a distributed cloud-based image analysis system using multiple AWS components. The analysis system will be capable of detecting cars in images and extracting any visible text. To do so, it the system will leverage machine learning tools provided by AWS Rekognition. The solution leverages Amazon EC2 for computation, Amazon S3 for storage, Amazon SQS for inter-instance communication, and the AWS SDK for programmatic access to these services.



Two separate JAVA applications are deployed in this system, one to each EC2 instance to perform distinct tasks.



EC2-A - Car Detector:

This instance analyzes images pulled from an AWS S3 bucket. Using Rekognition's DetectLabels feature it identifies cars in images with at least 80% confidence. For each identified image, it sends the image's filename to an SQS queue. Once all images in the S3 bucket have been analyzed it sends a special "-1" message to signal the end of messages being sent.



EC2-B - Text Reader:

This instance reads messages from the same SQS queue, retrieving the image filenames sent. It downloads the corresponding images from the same S3 bucket and uses Rekognition's DetectText feature to extract visible images from each image. Only text with an 80% or higher confidence is extracted. The extracted text is then written to a local output file for verification.



This README will provide an overview of the architecture as well as a guide to setting up, running, and validating the app.



Sections:

1. Overview
2. Architecture Diagram
3. Step-By-Step Environment Setup
4. Step-By-Step Running the Application
5. Step-By-Step Shutdown of Environment
6. YouTube Link
7. Use of AI Assistance



---------------------------------------------------------------------

2\) Architecture Diagram

---------------------------------------------------------------------

&nbsp;				S3 Bucket

&nbsp;  \_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_|\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_\_ 

&nbsp; |                                 |                             |

EC2-A-------------------------->SQS Queue---------------------->EC2-B--->output.txt (On EC2-B)

&nbsp; |                                 |                             |

&nbsp; |<------------------------>AWS Rekognition<-------------------->|



Data Flow:

S3 Bucket -> EC2-A - Car Detector pulls images from the S3 bucket

EC2-A -> Rekognition (DetectLabels) - EC2-A sends each image to AWS Rekognition To Detect Cars

Rekognition (DetectLabels) -> EC2-A - Rekognition identifies which images contain cars with 80% or more confidence

EC2-A -> SQS - EC2-A sends messages containing the filenames of car images, then a special "-1" message to indicate no more messages to be sent

EC2-B <-> SQS - Polls SQS queue and retrieves messages

S3 -> EC2-B - EC2-B downloads corresponding images for each message received

EC2-B <-> Rekognition (DetectText) - Send each image to AWS Rekognition and sends back extracted text lines (>= 80% confidence)

EC2-B -> output.txt - Writes the extracted text results locally



---------------------------------------------------------------------

2\) Step-By-Step Environment Setup

---------------------------------------------------------------------



Step 1: Launch the AWS Learner Lab

1. Log into your course portal and navigate to AWS Academy Learner Lab
2. Click "Start Lab" to initialize your cloud environment.
3. Click "AWS Details" and save: Access Key ID / Secret Access Key / Session Token (temp credentials)
4. Also save the SSH key as a .pem file locally on your laptop. Note its location it will be used to ssh into instances later.
5. Click "AWS" button to open the AWS Management Console
6. Verify that the region displayed in the top-right corner is US East (N. Virginia) - us-east-1



Step 2: Prepare Networking (Security Groups)

1. In the AWS Management Console, search for "EC2", then go to "Security Groups", then "Create Security Group".
2. Set the name of the security group. Ex: cs643-project1-ec2-sg
3. Set VPC to default
4. Set Inbound Rules for SSH, HTTPS, and HTTP to MY IP
5. Leave Outbound Rules to All Traffic
6. Create Security Group



Step 3: Create Two EC2 Instances

1. In the AWS Management Console, search for "EC2" and open the "Instances" page.
2. Click "Launch Instance"
3. Provide Name of Instance
4. Select "Amazon Linux", then "Amazon Linux 2023 kernel-6.12 AMI" for AMI
5. Select "t3.micro" for Instance Type
6. Select "vockey" for Key Pair
7. For Network Settings: select "Select Existing Security Group", then select the security you created in step 2
8. Click "Launch Instance"
9. Save the Public IP address of the EC2 instance, it will be used later.
10. Repeat this process to create a second instance with a different name.



Step 4: Create the SQS Queue

1. In the AWS Management Console, search for "SQS" and click "Create Queue".
2. Click "Standard"
3. Provide name of Queue
4. Leave the rest as its default settings
5. Click "Create Queue"
6. Save the SQS Queue URL, this will be needed later.



Step 5: Connect via SSH to Each Instance

1. Open a local terminal session in the location of the SSH Key you saved earlier.
2. SSH into the instance using the following command: ssh -i <key\_filename>.pem ec2-user@<EC2-PUBLIC-IP>
3. Open terminal sessions open, one for each EC2 instance.



Step 6: Update and Install Required Tools

1. Run the following commands on both EC2 Instances: "sudo yum update -y" and "sudo dnf install -y java-17-amazon-corretto maven awscli"
2. Verify installation with the following commands: "java -version", "mvn -v", and "aws --version"



Step 7: Configure AWS Credentials

1. Create AWS Credential Directory: "mkdir -p ~/.aws"
2. Create AWS Credential File: "nano ~/.aws/credentials"
3. Copy and Paste the Credentials copied in Step 1 into this file. Save and Exit File
4. Export Region Variables: "export AWS\_REGION=us-east-1" and "export AWS\_DEFAULT\_REGION=us-east-1"
5. Verify Credentials are Valid: "aws sts get-caller-identity"
6. Repeat this process for both EC2 instances



Step 8: Environment Variables for Convenience

1. On both EC2 instances execute these commands: "export BUCKET="cs643-njit-project1"" and "export QUEUE\_URL="<your Queue URL saved in Step 4>""



Step 9: Check Connectivity to AWS Components

1. From either EC2 instance, verify S3 connection: "aws ls s3://cs643-njit-project1/ | head"
2. From either EC2 instance, verify SQS connection: "aws sqs get-queue-attributes --queue-url "$QUEUE\_URL" --attribute-names ApproximateNumberOfMessages"



Step 10: Prepare Application Directories

1. On EC2-A, run the following commands in order: "mkdir ~/car-detector" then "cd ~/car-detector"
2. Upload or clone the folders, files, and code provided alongside this README file (car-detector) to this directory
3. On EC2-B, run the following commands in order:"mkdir ~/text-reader" then "cd ~/text-reader"
4. Upload or clone the folders, files, and code provided alongside this README file (text-reader) to this directory
5. For each instance confirm it contains a valid pom.xml file and the Java source folders under src/main/java/com/cs643



Step 11: Build the Applications

1. On EC2-A instance, run the following commands in order: "cd ~/car-detector" then "mvn clean package"
2. Verify Build: "ls target/"
3. On EC2-B instance, run the following commands in order: "cd ~/text-reader" then "mvn clean package"
4. Verify Build: "ls target/"





---------------------------------------------------------------------

4\) Step-By-Step Running The Application

---------------------------------------------------------------------

Step 1 (Optional): Purge the SQS Queue

1. Run on either EC2 instance: "aws sqs purge-queue --queue-url "$QUEUE\_URL" then "sleep 65"
2. Verify the queue is empty: "aws sqs get-queue-attributes --queue-url "$QUEUE\_URL" --attribute-names ApproximateNumberOfMessages"



Step 2: Run Car Detector Application

1. Execute the following commands on EC2-A: 
   "cd ~/car-detector"
   "java -jar target/car-detector-1.0.0.jar --bucket "$BUCKET" --queue-url "$QUEUE\_URL" --max-images 10"



Step 3: Run Text Reader Application

1. Execute the following commands on EC2-B:
   "cd ~/text-reader"
   "java -jar target/text-reader-1.0.0.jar --bucket "$BUCKET" --queue-url "$QUEUE\_URL" --out /home/ec2-user/output.txt"



NOTE: Step 2 and 3 are interchangeable. Step 3 can be done first and the solution would still work.



Step 4: Verify Output

1. On EC2-A after executing commands in Step 2, you would see output reviewing each image and indicating true/false if there was a car detected.
   It would end with a message indicating that the sentinel message was sent.
2. On EC2-B, after executing commands in Step 3, it will wait for messages to arrive in the SQS queue. It will print out logs indicating if there was text detected in the image. If there was it will also print out the text in the image.
3. The text lines will also be written to the output.txt file after all images have been processed.
4. Verify text in file: "nano /home/ec2-user/output.txt"



---------------------------------------------------------------------

5\) Step-By-Step Shutdown of Environment

---------------------------------------------------------------------
Once application testing has been completed, proper shutdown of the applications and environment is critical.



Step 1: Closing SSH Sessions

1. Enter "exit" into terminal, this will end the SSH session.
2. Close Terminal



Step 2: Stop EC2 Instances

1. In the AWS Management Console, navigate to EC2 then Instances.
2. Select both EC2 instances, then click "Instance State", then "Stop Instance"
3. Once stopped, close out the AWS Management Console Window.



Step 3: End Learner Lab Session

1. In the Learner Lab Window, click "End Lab"
2. You have successfully ended the AWS Learner Lab session.



---------------------------------------------------------------------

6\) YouTube Link

---------------------------------------------------------------------

Link: https://youtu.be/V_pxyKxnAhU



---------------------------------------------------------------------

7\) Use of AI Assistance

---------------------------------------------------------------------

An AI assistant was used to breakdown the project guidelines document into tasks to be completed in order to achieve the desired result. It was also used to assist in error explanations to assist in troubleshooting errors. I found the AI assistant very helpful in breaking down this project into separate and meaningful tasks. This helped me organize my thoughts and determine a plan of action I could follow. As I completed tasks, I checked them off the list and eventually completed the project. This helped from a project management perspective and allow me to plan out and focus my energy effectively. Additionally, working with AWS Rekognition for the first time, there were a couple of errors that I did not understand. The AI assistant was helpful in explaining the bugs and pinpointing where the issue was. With this help, I was able to resolve bugs and complete the project. This was very helpful in that respect.

