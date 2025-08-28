package org.vinni.servidor.gui;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Author: Vinni
 */
public class PrincipalSrv extends JFrame {

    private final int PORT = 12345;
    private Map<String, DatagramPacket> clientes = new HashMap<>();

    private JButton bIniciar;
    private JLabel jLabel1;
    private JTextArea mensajesTxt;
    private JScrollPane jScrollPane1;

    public PrincipalSrv() {
        initComponents();
        this.mensajesTxt.setEditable(false);
    }

    private void initComponents() {
        this.setTitle("Servidor ...");

        bIniciar = new JButton();
        jLabel1 = new JLabel();
        mensajesTxt = new JTextArea();
        jScrollPane1 = new JScrollPane();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        bIniciar.setFont(new java.awt.Font("Segoe UI", 0, 18));
        bIniciar.setText("INICIAR SERVIDOR");
        bIniciar.addActionListener(evt -> iniciar());
        getContentPane().add(bIniciar);
        bIniciar.setBounds(150, 50, 250, 40);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14));
        jLabel1.setForeground(new java.awt.Color(204, 0, 0));
        jLabel1.setText("SERVIDOR UDP : FERINK");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(150, 10, 200, 17);

        mensajesTxt.setColumns(25);
        mensajesTxt.setRows(5);
        jScrollPane1.setViewportView(mensajesTxt);

        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(20, 150, 500, 120);

        setSize(new java.awt.Dimension(570, 320));
        setLocationRelativeTo(null);
    }

    private void enviarListaClientes(DatagramSocket socketudp) {
        StringBuilder sb = new StringBuilder("CLIENTLIST:");
        for (String nombre : clientes.keySet()) {
            sb.append(nombre).append(";");
        }
        String lista = sb.toString();

        for (DatagramPacket dpCliente : clientes.values()) {
            try {
                DatagramPacket paquete = new DatagramPacket(lista.getBytes(), lista.length(),
                        dpCliente.getAddress(), dpCliente.getPort());
                socketudp.send(paquete);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void iniciar() {
        mensajesTxt.append("Servidor UDP iniciado en el puerto " + PORT + "\n");
        byte[] buf = new byte[1024];

        new Thread(() -> {
            DatagramPacket dp;
            try {
                DatagramSocket socketudp = new DatagramSocket(PORT);
                this.bIniciar.setEnabled(false);
                while (true) {
                    dp = new DatagramPacket(buf, buf.length);
                    socketudp.receive(dp);
                    String recibido = new String(dp.getData(), 0, dp.getLength());

                    if (recibido.startsWith("REGISTER:")) {
                        String nombre = recibido.substring(9);
                        clientes.put(nombre, new DatagramPacket(new byte[0], 0, dp.getAddress(), dp.getPort()));
                        mensajesTxt.append("Cliente registrado: " + nombre + "\n");

                        // enviar lista a todos
                        enviarListaClientes(socketudp);
                    }
                    else if (recibido.startsWith("MSG:")) {
                        String[] partes = recibido.split(":", 4);
                        String origen = partes[1];
                        String destino = partes[2];
                        String mensaje = partes[3];

                        DatagramPacket destinoDP = clientes.get(destino);
                        if (destinoDP != null) {
                            String fullMsg = origen + ": " + mensaje;
                            DatagramPacket forward = new DatagramPacket(fullMsg.getBytes(), fullMsg.length(),
                                    destinoDP.getAddress(), destinoDP.getPort());
                            socketudp.send(forward);
                            mensajesTxt.append("Mensaje de " + origen + " para " + destino + "\n");
                        }
                    }
                    else if (recibido.startsWith("FILE:")) {
                        String[] partes = recibido.split(":", 4);
                        String origen = partes[1];
                        String destino = partes[2];
                        String nombreArchivo = partes[3];

                        DatagramPacket destinoDP = clientes.get(destino);
                        if (destinoDP != null) {
                            mensajesTxt.append("Archivo de " + origen + " para " + destino + ": " + nombreArchivo + "\n");
                            DatagramPacket aviso = new DatagramPacket(recibido.getBytes(), recibido.length(),
                                    destinoDP.getAddress(), destinoDP.getPort());
                            socketudp.send(aviso);

                            // recibir chunks hasta ENDFILE y reenviarlos
                            boolean recibiendo = true;
                            while (recibiendo) {
                                dp = new DatagramPacket(buf, buf.length);
                                socketudp.receive(dp);
                                String posibleFin = new String(dp.getData(), 0, dp.getLength());
                                if (posibleFin.equals("ENDFILE")) {
                                    DatagramPacket fin = new DatagramPacket(posibleFin.getBytes(), posibleFin.length(),
                                            destinoDP.getAddress(), destinoDP.getPort());
                                    socketudp.send(fin);
                                    recibiendo = false;
                                } else {
                                    DatagramPacket forward = new DatagramPacket(dp.getData(), dp.getLength(),
                                            destinoDP.getAddress(), destinoDP.getPort());
                                    socketudp.send(forward);
                                }
                            }
                        }
                    }
                }

            } catch (SocketException ex) {
                Logger.getLogger(PrincipalSrv.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(PrincipalSrv.class.getName()).log(Level.SEVERE, null, ex);
            }
        }).start();
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new PrincipalSrv().setVisible(true));
    }
}
