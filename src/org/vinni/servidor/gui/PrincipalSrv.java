package org.vinni.servidor.gui;


import org.vinni.dto.MiDatagrama;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Author: Vinni
 */
public class PrincipalSrv extends JFrame {

    private final int PORT = 12345;

    /**
     * Creates new form Principal1
     */
    public PrincipalSrv() {
        initComponents();
        this.mensajesTxt.setEditable(false);
    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {
        this.setTitle("Servidor ...");

        bIniciar = new JButton();
        jLabel1 = new JLabel();
        mensajesTxt = new JTextArea();
        jScrollPane1 = new JScrollPane();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        bIniciar.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        bIniciar.setText("INICIAR SERVIDOR");
        bIniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bIniciarActionPerformed(evt);
            }
        });
        getContentPane().add(bIniciar);
        bIniciar.setBounds(150, 50, 250, 40);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(204, 0, 0));
        jLabel1.setText("SERVIDOR UDP : FERINK");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(150, 10, 160, 17);

        mensajesTxt.setColumns(25);
        mensajesTxt.setRows(5);

        jScrollPane1.setViewportView(mensajesTxt);

        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(20, 150, 500, 120);

        setSize(new java.awt.Dimension(570, 320));
        setLocationRelativeTo(null);
    }// </editor-fold>

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new PrincipalSrv().setVisible(true);
            }
        });

    }
    private void bIniciarActionPerformed(java.awt.event.ActionEvent evt) {
        iniciar();
    }

    public void iniciar(){
        mensajesTxt.append("Servidor UDP iniciado en el puerto"+PORT+"\n");
        byte[] buf = new byte[1000];

        new Thread(() -> {
            DatagramPacket dp = null;
            try {
                DatagramSocket socketudp = new DatagramSocket(PORT);
                boolean inicio = true;
                this.bIniciar.setEnabled(false);
                while (inicio) {
                    mensajesTxt.append("Escuchando ...\n ");
                    dp = new DatagramPacket(buf, buf.length);
                    socketudp.receive(dp);
                    String elmensaje = new String(dp.getData(), 0, dp.getLength());
                    File f = new File("c:\\acasertvidor\\", "elarchivo.*");
                    mensajesTxt.append("El mensaje recibido es " +
                            elmensaje+"\n");

                    DatagramPacket mensajeServ = MiDatagrama.crearDataG(dp.getAddress().getHostAddress(),
                            dp.getPort(), "Mensaje recibido en el servidor");
                    socketudp.send(mensajeServ);
//                if (dp.getData()!= null){
//                    inicio = false;
//                    System.out.println(" Fin");
//                }

                }

            } catch (SocketException ex) {
                Logger.getLogger(PrincipalSrv.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(PrincipalSrv.class.getName()).log(Level.SEVERE, null, ex);
            }
        }).start();

    }
    // Variables declaration - do not modify
    private JButton bIniciar;
    private JLabel jLabel1;
    private JTextArea mensajesTxt;
    private JScrollPane jScrollPane1;


}