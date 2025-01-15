package eu.deic.rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIServer {
    public static void main(String[] args) {
        try {
            // Create and bind the ZoomService implementation
            ZoomService service = new ZoomServiceImpl();
            Registry registry = LocateRegistry.createRegistry(1100); // Use 1100 for C05
            registry.rebind("ZoomService", service);

            System.out.println("RMI Server is running on port 1100...");

            // Keep the server running
            Thread.sleep(Long.MAX_VALUE); // Prevents the main thread from exiting
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
