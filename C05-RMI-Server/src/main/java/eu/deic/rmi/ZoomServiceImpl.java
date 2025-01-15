package eu.deic.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ZoomServiceImpl extends UnicastRemoteObject implements ZoomService {

    public ZoomServiceImpl() throws RemoteException {
        super(); // Automatically exports the object
    }

    @Override
    public byte[] zoomImage(byte[] imageData, int zoomPercent) throws RemoteException {
        System.out.println("Processing request with zoom level: " + zoomPercent);
        
        // Placeholder logic: Return the same data for now
        return imageData;
    }
}
