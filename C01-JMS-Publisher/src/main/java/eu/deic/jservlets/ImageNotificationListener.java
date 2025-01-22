 package eu.deic.jservlets;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.apache.activemq.ActiveMQConnectionFactory;

@WebListener
public class ImageNotificationListener implements ServletContextListener {

    private Connection connection;
    private Session session;
    private MessageConsumer consumer;

    private static final String BROKER_URL = "tcp://c02-activemq:61616";
    private static final String NOTIFICATION_TOPIC_NAME = "imageNotifications";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("ImageNotificationListener starting...");
        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(BROKER_URL);
            connection = factory.createConnection();
            connection.start();

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic notifTopic = session.createTopic(NOTIFICATION_TOPIC_NAME);

            consumer = session.createConsumer(notifTopic);
            consumer.setMessageListener(msg -> {
                if (msg instanceof TextMessage) {
                    try {
                        String text = ((TextMessage) msg).getText();
                        System.out.println("Received notification: " + text);
                        if (text.startsWith("NewImage:")) {
                            String idStr = text.substring("NewImage:".length());
                            // Broadcast via WebSocket
                            WebSocketServer.broadcast(idStr);
                        }
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("ImageNotificationListener shutting down...");
        try {
            if (consumer != null) consumer.close();
            if (session != null) session.close();
            if (connection != null) connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
