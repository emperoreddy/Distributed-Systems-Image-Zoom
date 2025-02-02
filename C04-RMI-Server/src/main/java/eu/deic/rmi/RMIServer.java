package eu.deic.rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIServer {
    public static void main(String[] args) {
        try {

            ZoomService service = new ZoomServiceImpl();
            Registry registry = LocateRegistry.createRegistry(1099); 
            registry.rebind("ZoomService", service);

            System.out.println("RMI Server is running on port 1099...");

            Thread.sleep(Long.MAX_VALUE); 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
