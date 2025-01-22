package eu.deic.jservlets;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/ws")
public class WebSocketServer {

    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        System.out.println("WebSocket opened: " + session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        System.out.println("WebSocket closed: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Message from " + session.getId() + ": " + message);
        // Typically no action needed unless you want 2-way communication
    }

    public static void broadcast(String msg) {
        synchronized (sessions) {
            for (Session s : sessions) {
                if (s.isOpen()) {
                    try {
                        s.getBasicRemote().sendText(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
