
package carrecognition;


import com.amazonaws.regions.Regions;
//Rekognition service
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
//S3 service
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
//SQS service
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;
import java.util.*;

//Bucket link
//https://njit-cs-643.s3.us-east-1.amazonaws.com
//Region Service = US_EAST_1

public class CarRecognitionApplication {

    public static void main(String[] args) {
		// -1 is the last on to get processed in the FIFO queue
        String bucketName = "njit-cs-643";
        String queueName = "car.fifo"; 
		String queueGroup = "group1";
		
		System.out.println("Building S3Client...");
		
        AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
		
		System.out.println("Successfully created S3Client");
		System.out.println("Building RekognitionClient...");
		
        AmazonRekognition rek = AmazonRekognitionClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
		
		System.out.println("Successfully created RekognitionClient");
		System.out.println("Building SQS...");
		
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
		
		System.out.println("Successfully created SQS");
		
        processBucketImages(s3, rek, sqs, bucketName, queueName, queueGroup);
    }

    public static void processBucketImages(AmazonS3 s3, AmazonRekognition rek, AmazonSQS sqs, String bucketName,
                                           String queueName, String queueGroup) {
		
		System.out.println("Process Bucket Images:");
        // Create queue or retrieve the queueUrl if it already exists.
		String queueUrl = "";
        try {
			ListQueuesResult list_queues = sqs.listQueues();
			System.out.println("Queue Size = " + list_queues.getQueueUrls().size());
			if (list_queues.getQueueUrls().size() == 0) {
				System.out.println("Making Queue...");
				CreateQueueRequest create_request = new CreateQueueRequest(queueName)
						.addAttributesEntry("MessageRetentionPeriod", "86400")
						.addAttributesEntry("FifoQueue", "true")
						.addAttributesEntry("ContentBasedDeduplication", "true");
				sqs.createQueue(create_request);
				queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
			System.out.println("Successfully made Queue with url: " + queueUrl);
				
			} else {
				queueUrl = list_queues.getQueueUrls().get(0);
			}
        } catch (Exception e) {
            System.out.println(e);
        }

        // Process the 10 images in the S3 bucket
        try {
            ListObjectsV2Result listObjResponse = s3.listObjectsV2(bucketName);
			List<S3ObjectSummary> objects = listObjResponse.getObjectSummaries();

            for (S3ObjectSummary obj : objects) {
				String imgname = obj.getKey();
                System.out.println("Retrieved image in " + bucketName + " S3 bucket: " + imgname);
				
				Image img = new Image().withS3Object(new S3Object().withName(imgname).withBucket(bucketName));
				
				//Detecting labels with minimum 90% confidence
                DetectLabelsRequest request = new DetectLabelsRequest()
					.withImage(img)
					.withMaxLabels(10)
					.withMinConfidence(90F);
					
                DetectLabelsResult result = rek.detectLabels(request);
                List<Label> labels = result.getLabels();
				
                for (Label label : labels) {
                    if (label.getName().equals("Car")) {
						System.out.println(imgname + ": Detected Car! Confidence: " + label.getConfidence().toString());
                        SendMessageRequest send_msg_request = new SendMessageRequest()
							.withQueueUrl(queueUrl)
							.withMessageBody(imgname)
							.withMessageGroupId(queueGroup);
						sqs.sendMessage(send_msg_request);
						
						System.out.println("Sending " + imgname + " to queue");
                        break;
                    }
                }
            }

            // Signal the end of image processing by sending "-1" to the queue
                        SendMessageRequest send_msg_request = new SendMessageRequest()
							.withQueueUrl(queueUrl)
							.withMessageBody("-1")
							.withMessageGroupId(queueGroup);
						sqs.sendMessage(send_msg_request);
						System.out.println("Sending -1 to end queue");
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }
    }
}