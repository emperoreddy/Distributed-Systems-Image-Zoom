package eu.deic.mdb;

import org.apache.activemq.ActiveMQConnectionFactory;
import jakarta.jms.*;
import eu.deic.mdb.ZoomService;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ImageProcessorConsumer listens to the JMS topic, processes incoming BMP images by splitting
 * them into two halves, sending each half to separate RMI servers for zooming, reassembling
 * the processed halves, and sending the final image to Container 6 (C06).
 */
public class ImageProcessorConsumer {

    private static final String BROKER_URL = "tcp://c02:61616"; // ActiveMQ broker URL
    private static final String TOPIC_NAME = "imageTopic";       // JMS Topic name

    // RMI Servers (Docker service names)
    private static final String RMI_SERVER_C04 = "c04-rmi-server"; // Service name for C04
    private static final String RMI_SERVER_C05 = "c05-rmi-server"; // Service name for C05
    private static final int C04_PORT = 1099;            // RMI port for C04
    private static final int C05_PORT = 1100;            // RMI port for C05

    // C06 REST API endpoint
    private static final String C06_UPLOAD_URL = "http://c06-nodejs:3000/api/bmp/upload";

    // Logger for logging information and errors
    private static final Logger LOGGER = Logger.getLogger(ImageProcessorConsumer.class.getName());

    public static void main(String[] args) {
        final AtomicReference<Connection> connectionRef = new AtomicReference<>();
        final AtomicReference<Session> sessionRef = new AtomicReference<>();

        try {
            // 1) Connect to ActiveMQ
            ConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            Connection connection = factory.createConnection();
            connectionRef.set(connection);
            connection.start();

            // 2) Create JMS session and subscribe to the topic
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            sessionRef.set(session);

            Topic topic = session.createTopic(TOPIC_NAME);
            MessageConsumer consumer = session.createConsumer(topic);

            LOGGER.info("Waiting for messages on topic: " + TOPIC_NAME);

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        String content = textMessage.getText();
                        LOGGER.info("Received message: " + content);

                        // Parse the message to extract image data and zoom level
                        String[] parts = content.split(";");
                        if (parts.length < 2 || !parts[0].startsWith("Image=") || !parts[1].startsWith("Zoom=")) {
                            LOGGER.severe("Invalid message format: " + content);
                            return;
                        }

                        String imageBase64 = parts[0].substring("Image=".length());
                        int zoomLevel = Integer.parseInt(parts[1].substring("Zoom=".length()));

                        // Decode the Base64 image data
                        byte[] imageData = Base64.getDecoder().decode(imageBase64);
                        LOGGER.info("Image received with zoom level: " + zoomLevel);

                        // Process the image
                        processImage(imageData, zoomLevel);

                    } else {
                        LOGGER.severe("Unsupported message type received.");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error processing message", e);
                }
            });

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize ImageProcessorConsumer", e);
        } finally {
            // Clean up JMS resources on shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (sessionRef.get() != null) sessionRef.get().close();
                    if (connectionRef.get() != null) connectionRef.get().close();
                } catch (JMSException e) {
                    LOGGER.log(Level.SEVERE, "Error closing JMS connection/session", e);
                }
            }));
        }
    }

    /**
     * Processes the image by splitting it, sending halves to RMI servers, reassembling,
     * and sending the final image to C06.
     *
     * @param imageData  The original BMP image data as a byte array.
     * @param zoomLevel  The zoom percentage to apply.
     */
    private static void processImage(byte[] imageData, int zoomLevel) {
        try {
            // 1) Decode the entire BMP into a BufferedImage
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
            if (originalImage == null) {
                throw new IOException("Failed to decode BMP image. The image data may be corrupted or in an unsupported format.");
            }
            LOGGER.info("Image size before processing: " + imageData.length + " bytes");
            LOGGER.info("Original image dimensions: " + originalImage.getWidth() + "x" + originalImage.getHeight());

            // 2) Split the image into two halves (top and bottom)
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();
            BufferedImage topHalf = originalImage.getSubimage(0, 0, width, height / 2);
            BufferedImage bottomHalf = originalImage.getSubimage(0, height / 2, width, height - (height / 2));

            // 3) Encode each half to a byte array (e.g., PNG format)
            byte[] topHalfData = bufferedImageToBytes(topHalf, "png");
            byte[] bottomHalfData = bufferedImageToBytes(bottomHalf, "png");

            // 4) Send each half to a different RMI server for processing
            ZoomService zoomServiceC04 = lookupZoomService(RMI_SERVER_C04, C04_PORT);
            ZoomService zoomServiceC05 = lookupZoomService(RMI_SERVER_C05, C05_PORT);

            LOGGER.info("Sending top half to RMI server " + RMI_SERVER_C04 + ":" + C04_PORT);
            byte[] processedTop = zoomServiceC04.zoomImage(topHalfData, zoomLevel);
            LOGGER.info("Received processed top half from " + RMI_SERVER_C04);

            LOGGER.info("Sending bottom half to RMI server " + RMI_SERVER_C05 + ":" + C05_PORT);
            byte[] processedBottom = zoomServiceC05.zoomImage(bottomHalfData, zoomLevel);
            LOGGER.info("Received processed bottom half from " + RMI_SERVER_C05);

            // 5) Decode the processed halves back into BufferedImages
            BufferedImage processedTopImg = ImageIO.read(new ByteArrayInputStream(processedTop));
            BufferedImage processedBottomImg = ImageIO.read(new ByteArrayInputStream(processedBottom));

            if (processedTopImg == null || processedBottomImg == null) {
                throw new IOException("Failed to decode processed image halves. They may be corrupted or in an unsupported format.");
            }

            // 6) Reassemble the processed halves into a single image
            int newHeight = processedTopImg.getHeight() + processedBottomImg.getHeight();
            BufferedImage combinedImage = new BufferedImage(processedTopImg.getWidth(), newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = combinedImage.createGraphics();
            g2d.drawImage(processedTopImg, 0, 0, null);
            g2d.drawImage(processedBottomImg, 0, processedTopImg.getHeight(), null);
            g2d.dispose();

            // 7) Convert the final combined image back to a byte array (e.g., PNG format)
            byte[] finalImageData = bufferedImageToBytes(combinedImage, "png");

            // 8) Send the final image to Container 6 (C06) via REST API
            sendToC06(finalImageData);
            LOGGER.info("Final image sent to C06 successfully.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during image processing", e);
        }
    }

    /**
     * Converts a BufferedImage to a byte array in the specified format.
     *
     * @param image   The BufferedImage to convert.
     * @param format  The image format (e.g., "png", "jpg").
     * @return        The byte array representation of the image.
     * @throws IOException If an error occurs during writing.
     */
    private static byte[] bufferedImageToBytes(BufferedImage image, String format) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            boolean success = ImageIO.write(image, format, baos);
            if (!success) {
                throw new IOException("Failed to write image in format: " + format);
            }
            return baos.toByteArray();
        }
    }

    /**
     * Looks up the ZoomService RMI service from the specified host and port.
     *
     * @param host  The hostname or service name of the RMI server.
     * @param port  The port number of the RMI registry.
     * @return      The ZoomService stub.
     * @throws Exception If the service cannot be found or connected.
     */
    private static ZoomService lookupZoomService(String host, int port) throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        return (ZoomService) registry.lookup("ZoomService");
    }

    /**
     * Sends the final processed image to Container 6 (C06) via a REST API endpoint.
     *
     * @param imageData The final image data as a byte array.
     */
    private static void sendToC06(byte[] imageData) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(C06_UPLOAD_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("Content-Length", String.valueOf(imageData.length));

            // Write the image data to the request body
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                baos.write(imageData);
                connection.getOutputStream().write(baos.toByteArray());
            }

            // Get the response code to ensure the request was successful
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || 
                responseCode == HttpURLConnection.HTTP_CREATED) {
                LOGGER.info("Image successfully uploaded to C06. Response Code: " + responseCode);
            } else {
                LOGGER.severe("Failed to upload image to C06. Response Code: " + responseCode);
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error sending image to C06", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
