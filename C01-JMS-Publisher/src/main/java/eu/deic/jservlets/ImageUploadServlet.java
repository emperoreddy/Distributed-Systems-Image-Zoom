package eu.deic.jservlets;

import org.apache.activemq.ActiveMQConnectionFactory;
import jakarta.jms.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@WebServlet("/ImageUploadServlet")
@MultipartConfig
public class ImageUploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    // private static final String BROKER_URL = "tcp://localhost:61616";
    private static final String BROKER_URL = "tcp://c02:61616";
    private static final String TOPIC_NAME = "imageTopic";
    private static final String UPLOAD_DIR = "/opt/uploaded-images";

    @Override
    public void init() throws ServletException {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        response.getWriter().write(
            "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<title>Upload Image</title>" +
            "<style>" +
            "    body {" +
            "        display: flex;" +
            "        justify-content: center;" +
            "        align-items: center;" +
            "        min-height: 100vh;" +
            "        margin: 0;" +
            "        background-color: #f5f5f5;" +
            "        font-family: Arial, sans-serif;" +
            "    }" +
            "    .upload-container {" +
            "        background: white;" +
            "        padding: 2rem;" +
            "        border-radius: 10px;" +
            "        box-shadow: 0 0 20px rgba(0,0,0,0.1);" +
            "        width: 400px;" +
            "    }" +
            "    h1 {" +
            "        color: #333;" +
            "        text-align: center;" +
            "        margin-bottom: 2rem;" +
            "    }" +
            "    .form-group {" +
            "        margin-bottom: 1.5rem;" +
            "    }" +
            "    label {" +
            "        display: block;" +
            "        margin-bottom: 0.5rem;" +
            "        color: #555;" +
            "    }" +
            "    input[type='file'] {" +
            "        width: 100%;" +
            "        padding: 0.5rem;" +
            "        border: 1px solid #ddd;" +
            "        border-radius: 4px;" +
            "    }" +
            "    input[type='range'] {" +
            "        width: 100%;" +
            "        margin: 1rem 0;" +
            "    }" +
            "    .zoom-value {" +
            "        text-align: center;" +
            "        color: #666;" +
            "        font-size: 1.1rem;" +
            "    }" +
            "    button {" +
            "        width: 100%;" +
            "        padding: 0.8rem;" +
            "        background-color: #4CAF50;" +
            "        color: white;" +
            "        border: none;" +
            "        border-radius: 4px;" +
            "        cursor: pointer;" +
            "        font-size: 1rem;" +
            "        transition: background-color 0.3s;" +
            "    }" +
            "    button:hover {" +
            "        background-color: #45a049;" +
            "    }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class='upload-container'>" +
            "    <h1>Image Upload</h1>" +
            "    <form action='ImageUploadServlet' method='POST' enctype='multipart/form-data'>" +
            "        <div class='form-group'>" +
            "            <label for='file'>Choose an image:</label>" +
            "            <input type='file' name='file' id='file' accept='image/*' required>" +
            "        </div>" +
            "        <div class='form-group'>" +
            "            <label for='zoom'>Zoom Level:</label>" +
            "            <div class='zoom-value'><span id='zoomValue'>50</span>%</div>" +
            "            <input type='range' name='zoom' id='zoom' min='1' max='200' value='50' oninput='updateZoomValue(this.value)'>" +
            "        </div>" +
            "        <button type='submit'>Upload</button>" +
            "    </form>" +
            "</div>" +
            "<script>" +
            "function updateZoomValue(val) {" +
            "    document.getElementById('zoomValue').textContent = val;" +
            "}" +
            "</script>" +
            "</body>" +
            "</html>"
        );
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Part filePart = request.getPart("file");
        String zoomLevel = request.getParameter("zoom");

        if (filePart == null || zoomLevel == null || zoomLevel.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("File and zoom level are required.");
            return;
        }

        String fileName = filePart.getSubmittedFileName();
        File savedFile = new File(UPLOAD_DIR, fileName);

        try (InputStream fileContent = filePart.getInputStream();
             FileOutputStream fos = new FileOutputStream(savedFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileContent.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }

        byte[] imageBytes;
        try (InputStream fileContent = filePart.getInputStream()) {
            imageBytes = fileContent.readAllBytes();
        }
        String encodedImage = Base64.getEncoder().encodeToString(imageBytes);

        try {
            publishToJMS(encodedImage, zoomLevel);
            response.getWriter().write(
                "Image saved at: " + savedFile.getAbsolutePath() +
                "<br>Image and zoom level published successfully!</br>"
            );
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Failed to publish message to JMS.");
        }
    }

    private void publishToJMS(String imageBase64, String zoomLevel) throws JMSException {
        ConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
        Connection connection = factory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(TOPIC_NAME);

        MessageProducer producer = session.createProducer(topic);
        TextMessage message = session.createTextMessage();
        message.setText("Image=" + imageBase64 + ";Zoom=" + zoomLevel);
        producer.send(message);

        System.out.println("Message sent to topic: " + TOPIC_NAME);

        producer.close();
        session.close();
        connection.close();
    }
}
