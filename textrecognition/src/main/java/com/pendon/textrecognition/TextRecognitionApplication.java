package textrecognition;

import com.amazonaws.regions.Regions;
//Rekognition service
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
//S3 Bucket service
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
//SQS service
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;
import java.util.*;
import java.io.*;

//Bucket link
//https://njit-cs-643.s3.us-east-1.amazonaws.com
//Region Service = US_EAST_1

public class TextRecognitionApplication {
	public static String filename = "output.txt";
    public static void main(String[] args) {

        String bucketName = "njit-cs-643";
        String queueName = "car.fifo"; 
		// -1 is the last on to get processed in the FIFO queue
		
		System.out.println("Building S3Client...");
		
        AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
		
		System.out.println("Successfully created S3Client");
		System.out.println("Building RekognitionClient...");
		
        AmazonRekognition rek = AmazonRekognitionClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
		
 		System.out.println("Successfully created RekognitionClient");
		System.out.println("Building SQS...");
		
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
		
		System.out.println("Successfully created SQS");
		

        processCarImages(s3, rek, sqs, bucketName, queueName);
    }

    public static void processCarImages(AmazonS3 s3, AmazonRekognition rek, AmazonSQS sqs, String bucketName,
                                        String queueName) {

        // Poll SQS until the queue is created (by CarRecognitionApplication)
        boolean QExists = false;
		System.out.println("Searching for queue...");
        while (!QExists) {
			ListQueuesResult list_queues = sqs.listQueues();
            if (list_queues.getQueueUrls().size() > 0) {
				System.out.println("Queue found!");
                QExists = true;
			}
        }

        // Retrieve the queueURL
        String queueUrl = "";
        try {
            queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
        } catch (QueueNameExistsException e) {
            throw e;
        }

        // Process every car image
        try {
            boolean endOfQ = false;
			//use hash map to store data
            HashMap<String, String> outputs = new HashMap<String, String>();
			
			System.out.println("Process Car Images:");
			
            while (!endOfQ) {
                // Retrieve next image index
                List<Message> messages = sqs.receiveMessage(queueUrl).getMessages();
				
                if (messages.size() > 0) {
                    Message message = messages.get(0);
                    String label = message.getBody();

                    if (label.equals("-1")) {
                        //When instance A terminates its image processing, it adds index -1 to the queue
                        // to signal to instance B that no more indexes will come.
						// Move to outputs.txt writing
						sqs.deleteMessage(queueUrl, message.getReceiptHandle());
                        endOfQ = true;
                    } else {
                        System.out.println("Processing car image with text from njit-cs-643 S3 bucket: " + label);

                        Image img = new Image().withS3Object(new S3Object().withName(label).withBucket(bucketName));
						// Text Detection - All car images with Type WORD
						DetectTextRequest request = new DetectTextRequest()
							.withImage(img);
						
                        DetectTextResult result = rek.detectText(request);
                        List<TextDetection> textDetections = result.getTextDetections();

                        if (textDetections.size() != 0) {
                            String text = "";
                            for (TextDetection textDetection : textDetections) {
								//Get only WORD text that is detected in the list
								//System.out.println(textDetection.getType() + ": " + textDetection.getConfidence().toString() + ": " + textDetection.getDetectedText());
                                if (textDetection.getType().equals("WORD")) {
                                    text = text + " " + textDetection.getDetectedText();
								}
                            }
							System.out.println(text);
                            outputs.put(label, text);
                        }
                    }

                    // Delete the message in the queue now that it's been handled
					sqs.deleteMessage(queueUrl, message.getReceiptHandle());
                }
            }
            try {
				//Write to output.txt
                FileWriter writer = new FileWriter(filename);

                Iterator<Map.Entry<String, String>> it = outputs.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, String> pair = it.next();
                    writer.write(pair.getKey() + ":" + pair.getValue() + "\n");
                    it.remove();
                }

                writer.close();
                System.out.println("Results written to file " + filename);
				//Delete queue
				sqs.deleteQueue(queueUrl);
            } catch (IOException e) {
                System.out.println("An error occurred writing to file.");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }
    }
}