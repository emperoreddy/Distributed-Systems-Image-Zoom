package eu.deic.mdb;

import org.apache.activemq.ActiveMQConnectionFactory;

import eu.deic.rmi.ZoomService;
import jakarta.jms.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

public class ImageProcessorConsumer {

    private static final String BROKER_URL = "tcp://localhost:61616"; // ActiveMQ broker URL
    private static final String TOPIC_NAME = "imageTopic";           // Topic name
    private static final String RMI_SERVER_C04 = "localhost";        // RMI server C04 hostname
    private static final String RMI_SERVER_C05 = "localhost";        // RMI server C05 hostname
    private static final int C04_PORT = 1099;                        // Port for C04
    private static final int C05_PORT = 1100;                        // Port for C05

    public static void main(String[] args) {
        final AtomicReference<Connection> connectionRef = new AtomicReference<>();
        final AtomicReference<Session> sessionRef = new AtomicReference<>();

        try {
            // Step 1: Connect to ActiveMQ
            ConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            Connection connection = factory.createConnection();
            connectionRef.set(connection); // Store in AtomicReference
            connection.start();

            // Step 2: Create JMS session and subscribe to topic
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            sessionRef.set(session); // Store in AtomicReference

            Topic topic = session.createTopic(TOPIC_NAME);
            MessageConsumer consumer = session.createConsumer(topic);

            System.out.println("Waiting for messages on topic: " + TOPIC_NAME);

            // Step 3: Listen for messages
            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        String content = textMessage.getText();

                        System.out.println("Received message: " + content);

                        // Step 4: Validate and parse message
                        String[] parts = content.split(";");
                        if (parts.length < 2 || !parts[0].contains("=") || !parts[1].contains("=")) {
                            System.err.println("Invalid message format: " + content);
                            return;
                        }

                        String imageBase64 = parts[0].split("=")[1];
                        int zoomLevel = Integer.parseInt(parts[1].split("=")[1]);

                        // Step 5: Decode image from Base64
                        byte[] imageData = Base64.getDecoder().decode(imageBase64);
                        System.out.println("Image received with zoom level: " + zoomLevel);

                        // Step 6: Process image using RMI servers
                        processImage(imageData, zoomLevel);
                    } else {
                        System.err.println("Unsupported message type received.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Ensure resources are cleaned up in a shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (sessionRef.get() != null) sessionRef.get().close();
                    if (connectionRef.get() != null) connectionRef.get().close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }));
        }
    }

    private static void processImage(byte[] imageData, int zoomLevel) {
        try {
            // Step 1: Connect to RMI servers
            Registry registryC04 = LocateRegistry.getRegistry(RMI_SERVER_C04, C04_PORT);
            Registry registryC05 = LocateRegistry.getRegistry(RMI_SERVER_C05, C05_PORT);

            ZoomService zoomServiceC04 = (ZoomService) registryC04.lookup("ZoomService");
            ZoomService zoomServiceC05 = (ZoomService) registryC05.lookup("ZoomService");

            System.out.println("Connected to RMI servers...");

            // Step 2: Split image into two parts
            int midPoint = imageData.length / 2;
            byte[] part1 = java.util.Arrays.copyOfRange(imageData, 0, midPoint);
            byte[] part2 = java.util.Arrays.copyOfRange(imageData, midPoint, imageData.length);

            // Step 3: Process each part using RMI servers
            byte[] processedPart1 = zoomServiceC04.zoomImage(part1, zoomLevel);
            byte[] processedPart2 = zoomServiceC05.zoomImage(part2, zoomLevel);

            System.out.println("Processed part 1 size: " + processedPart1.length);
            System.out.println("Processed part 2 size: " + processedPart2.length);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
