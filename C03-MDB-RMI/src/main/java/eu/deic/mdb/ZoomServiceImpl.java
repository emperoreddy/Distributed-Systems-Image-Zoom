package eu.deic.rmi;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class ZoomServiceImpl extends UnicastRemoteObject implements ZoomService {


	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(ZoomServiceImpl.class.getName());

    public ZoomServiceImpl() throws RemoteException {
        super();
    }

    @Override
    public byte[] zoomImage(byte[] imageData, int zoomPercent) throws RemoteException {
        LOGGER.info("Received request to zoom image with zoom level: " + zoomPercent);

        logImageData(imageData);
        
        if (imageData == null || imageData.length == 0) {
            String errorMessage = "Invalid image data: null or empty";
            LOGGER.severe(errorMessage);
            throw new RemoteException(errorMessage);
        }
        if (zoomPercent <= 0) {
            String errorMessage = "Invalid zoom percentage: must be > 0";
            LOGGER.severe(errorMessage);
            throw new RemoteException(errorMessage);
        }

        try {

            LOGGER.info("Image size before processing: " + imageData.length + " bytes");

      
            BufferedImage originalImage = readImageFromBytes(imageData);
            if (originalImage == null) {
                throw new RemoteException("Image could not be read. Invalid format or corrupted data.");
            }

       
            LOGGER.info("Original image dimensions: " + originalImage.getWidth() + "x" + originalImage.getHeight());

         
            BufferedImage zoomedImage = resizeImage(originalImage, zoomPercent);

            // Convert the zoomed image back to byte array   ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(zoomedImage, "jpg", outputStream);
            byte[] zoomedImageData = outputStream.toByteArray();

      
            LOGGER.info("Zoomed image size: " + zoomedImageData.length + " bytes");

            return zoomedImageData;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during image processing", e);
            throw new RemoteException("Error during image processing: " + e.getMessage(), e);
        }
    }

    
    private BufferedImage readImageFromBytes(byte[] imageData) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData)) {
            return ImageIO.read(inputStream);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to read image data", e);
            return null;
        }
    }


    private BufferedImage resizeImage(BufferedImage originalImage, int zoomPercent) {
        int newWidth = originalImage.getWidth() * zoomPercent / 100;
        int newHeight = originalImage.getHeight() * zoomPercent / 100;

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        Graphics2D g2d = resizedImage.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return resizedImage;
    }
    
    private void logImageData(byte[] imageData) {
        LOGGER.info("Image data size: " + imageData.length);
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < Math.min(imageData.length, 16); i++) {
            hexString.append(String.format("%02X ", imageData[i]));
        }
        LOGGER.info("First 16 bytes of image data (hex): " + hexString.toString());
    }

}
