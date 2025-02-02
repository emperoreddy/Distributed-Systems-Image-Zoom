package eu.deic.mdb;

import org.apache.activemq.ActiveMQConnectionFactory;
import jakarta.jms.*;

import eu.deic.rmi.ZoomService;
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
import org.json.*;


public class ImageProcessorConsumer {

    private static final Logger LOGGER = Logger.getLogger(ImageProcessorConsumer.class.getName());

    // ActiveMQ
    private static final String BROKER_URL = "tcp://c02-activemq:61616"; 
    private static final String TOPIC_NAME = "imageTopic";
    private static final String NOTIFICATION_TOPIC_NAME = "imageNotifications";

    // RMI Servers
    private static final String RMI_SERVER_C04 = "c04-rmi-server";
    private static final int C04_PORT = 1099;
    private static final String RMI_SERVER_C05 = "c05-rmi-server";
    private static final int C05_PORT = 1100;

    // C06 upload endpoint
    private static final String C06_UPLOAD_URL = "http://c06-nodejs:3000/api/bmp/upload";

    // JMS session
    private static Session session;
    private static Connection connection;

    public static void main(String[] args) {
        final AtomicReference<Connection> connectionRef = new AtomicReference<>();
        final AtomicReference<Session> sessionRef = new AtomicReference<>();

        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            connection = factory.createConnection();
            connectionRef.set(connection);
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            sessionRef.set(session);

            Topic topic = session.createTopic(TOPIC_NAME);
            MessageConsumer consumer = session.createConsumer(topic);

            LOGGER.info("Waiting for messages on topic: " + TOPIC_NAME);

            consumer.setMessageListener(message -> {
                if (message instanceof TextMessage) {
                    try {
                        TextMessage textMessage = (TextMessage) message;
                        String content = textMessage.getText();
                        LOGGER.info("Received message: " + content);

                        // Parse out the image and zoom
                        if (!content.contains("Image=") || !content.contains("Zoom=")) {
                            LOGGER.severe("Invalid message format: " + content);
                            return;
                        }
                        String[] parts = content.split(";");
                        String imageBase64 = parts[0].substring("Image=".length());
                        int zoomLevel = Integer.parseInt(parts[1].substring("Zoom=".length()));

                        byte[] imageData = Base64.getDecoder().decode(imageBase64);
                        processImage(imageData, zoomLevel);

                    } catch (JMSException e) {
                        LOGGER.log(Level.SEVERE, "JMS error", e);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error processing message", e);
                    }
                } else {
                    LOGGER.severe("Unsupported message type received.");
                }
            });

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize ImageProcessorConsumer", e);
        } finally {
            
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

    private static void processImage(byte[] imageData, int zoomLevel) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageData));
            if (original == null) {
                throw new IOException("Failed to decode BMP image. Possibly corrupt.");
            }
            LOGGER.info("Original image size: " + imageData.length + " bytes");
            LOGGER.info("Dimensions: " + original.getWidth() + "x" + original.getHeight());

            int w = original.getWidth();
            int h = original.getHeight();

            // Split
            BufferedImage topHalf = original.getSubimage(0, 0, w, h / 2);
            BufferedImage bottomHalf = original.getSubimage(0, h / 2, w, h - h / 2);

            byte[] topBytes = bufferedImageToBytes(topHalf, "png");
            byte[] bottomBytes = bufferedImageToBytes(bottomHalf, "png");

            // RMI
            ZoomService zc04 = lookupZoomService(RMI_SERVER_C04, C04_PORT);
            ZoomService zc05 = lookupZoomService(RMI_SERVER_C05, C05_PORT);

            byte[] processedTop = zc04.zoomImage(topBytes, zoomLevel);
            byte[] processedBottom = zc05.zoomImage(bottomBytes, zoomLevel);

            BufferedImage ptImg = ImageIO.read(new ByteArrayInputStream(processedTop));
            BufferedImage pbImg = ImageIO.read(new ByteArrayInputStream(processedBottom));

            if (ptImg == null || pbImg == null) {
                throw new IOException("Failed to decode processed images.");
            }

            // Assemble img
            int newHeight = ptImg.getHeight() + pbImg.getHeight();
            BufferedImage combined = new BufferedImage(ptImg.getWidth(), newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = combined.createGraphics();
            g2d.drawImage(ptImg, 0, 0, null);
            g2d.drawImage(pbImg, 0, ptImg.getHeight(), null);
            g2d.dispose();

            byte[] finalBytes = bufferedImageToBytes(combined, "png");

            // Send to C06
            sendToC06(finalBytes);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing image", e);
        }
    }

    private static byte[] bufferedImageToBytes(BufferedImage img, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, format, baos);
        return baos.toByteArray();
    }

    private static ZoomService lookupZoomService(String host, int port) throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        return (ZoomService) registry.lookup("ZoomService");
    }

    private static void sendToC06(byte[] imageData) throws IOException, JMSException {
        HttpURLConnection conn = null;
        int pictureId = -1;
        try {
            URL url = new URL(C06_UPLOAD_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Length", String.valueOf(imageData.length));

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                baos.write(imageData);
                conn.getOutputStream().write(baos.toByteArray());
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                LOGGER.info("Image successfully uploaded to C06. Response code: " + responseCode);
                // read the response to get pictureId
                try (ByteArrayInputStream bais = new ByteArrayInputStream(conn.getInputStream().readAllBytes())) {
                    String respString = new String(bais.readAllBytes());
                    LOGGER.info("C06 Response: " + respString);
                    org.json.JSONObject json = new org.json.JSONObject(respString);
                    pictureId = json.optInt("pictureId", -1);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error parsing response from C06", ex);
                }
            } else {
                LOGGER.severe("Failed to upload image to C06. Response Code: " + responseCode);
            }
        } finally {
            if (conn != null) conn.disconnect();
        }

        if (pictureId != -1) {
            publishNotification(pictureId);
        }
    }

    private static void publishNotification(int pictureId) throws JMSException {
        Topic notifTopic = session.createTopic(NOTIFICATION_TOPIC_NAME);
        MessageProducer producer = session.createProducer(notifTopic);
        TextMessage msg = session.createTextMessage("NewImage:" + pictureId);
        producer.send(msg);
        LOGGER.info("Notification sent for picture ID: " + pictureId);
        producer.close();
    }
}
