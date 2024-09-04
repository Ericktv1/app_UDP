package org.vinni.dto;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MiDatagrama {
    public static DatagramPacket crearDataG(String ip, int puerto, String mensaje){
        try {
            InetAddress direccion = InetAddress.getByName(ip);
            byte[] mensajeB = mensaje.getBytes();
            DatagramPacket paquete = new DatagramPacket(mensajeB, mensaje.length(), direccion, puerto);
            return paquete;
        } catch (UnknownHostException ex) {
            Logger.getLogger(MiDatagrama.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
