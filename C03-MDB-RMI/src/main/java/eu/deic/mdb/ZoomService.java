package eu.deic.mdb;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ZoomService extends Remote {
    byte[] zoomImage(byte[] imageData, int zoomPercent) throws RemoteException;
}
