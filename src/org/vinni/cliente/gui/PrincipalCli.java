package org.vinni.cliente.gui;

import org.vinni.dto.MiDatagrama;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * author: Vinni 2024
 */
public class PrincipalCli extends JFrame {

    private final int PORT = 12345;
    private String nombreCliente;
    private DatagramSocket socket;

    private JButton btEnviar;
    private JButton btEnviarArchivo;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JScrollPane jScrollPane1;
    private JTextArea mensajesTxt;
    private JTextField mensajeTxt;
    private JComboBox<String> comboClientes; // 🔹 Lista de clientes conectados

    public PrincipalCli(String nombre) {
        this.nombreCliente = nombre;
        try {
            // Cada cliente usa un socket en puerto aleatorio, pero siempre el mismo
            socket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
        initComponents();
        this.btEnviar.setEnabled(true);
        this.mensajesTxt.setEditable(false);
        registrarEnServidor();

        // hilo para escuchar respuestas del servidor
        new Thread(this::escucharServidor).start();
    }

    private void initComponents() {
        this.setTitle("Cliente UDP - " + nombreCliente);
        jLabel1 = new JLabel();
        jScrollPane1 = new JScrollPane();
        mensajesTxt = new JTextArea();
        mensajeTxt = new JTextField();
        jLabel2 = new JLabel();
        btEnviar = new JButton();
        btEnviarArchivo = new JButton();
        comboClientes = new JComboBox<>();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 14));
        jLabel1.setForeground(new java.awt.Color(204, 0, 0));
        jLabel1.setText("CLIENTE UDP : " + nombreCliente);
        getContentPane().add(jLabel1);
        jLabel1.setBounds(110, 10, 250, 17);

        mensajesTxt.setColumns(20);
        mensajesTxt.setRows(5);
        jScrollPane1.setViewportView(mensajesTxt);
        getContentPane().add(jScrollPane1);
        jScrollPane1.setBounds(30, 210, 410, 110);

        mensajeTxt.setFont(new java.awt.Font("Verdana", 0, 14));
        getContentPane().add(mensajeTxt);
        mensajeTxt.setBounds(40, 120, 350, 30);

        jLabel2.setFont(new java.awt.Font("Verdana", 0, 14));
        jLabel2.setText("Mensaje:");
        getContentPane().add(jLabel2);
        jLabel2.setBounds(20, 90, 120, 30);

        btEnviar.setFont(new java.awt.Font("Verdana", 0, 14));
        btEnviar.setText("Enviar");
        btEnviar.addActionListener(evt -> enviarMensaje());
        getContentPane().add(btEnviar);
        btEnviar.setBounds(327, 160, 120, 27);

        btEnviarArchivo.setFont(new java.awt.Font("Verdana", 0, 14));
        btEnviarArchivo.setText("Enviar Archivo");
        btEnviarArchivo.addActionListener(evt -> enviarArchivo());
        getContentPane().add(btEnviarArchivo);
        btEnviarArchivo.setBounds(40, 160, 150, 27);

        // 🔹 ComboBox para lista de clientes
        getContentPane().add(comboClientes);
        comboClientes.setBounds(200, 160, 120, 27);

        setSize(new java.awt.Dimension(491, 375));
        setLocationRelativeTo(null);
    }

    private void registrarEnServidor() {
            try {
                String msg = "REGISTER:" + nombreCliente;
                InetAddress direccion = InetAddress.getByName("127.0.0.1");
                DatagramPacket dp = new DatagramPacket(msg.getBytes(), msg.length(), direccion, PORT);
                socket.send(dp); //  usa el mismo socket
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    private void enviarMensaje() {
        String mensaje = mensajeTxt.getText();
        if (mensaje.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay mensaje para enviar");
            return;
        }
        String destinatario = (String) comboClientes.getSelectedItem();
        if (destinatario == null || destinatario.equals(nombreCliente)) {
            JOptionPane.showMessageDialog(this, "Seleccione un destinatario válido");
            return;
        }

        try {
            String msg = "MSG:" + nombreCliente + ":" + destinatario + ":" + mensaje;
            DatagramPacket mensajeDG = MiDatagrama.crearDataG("127.0.0.1", PORT, msg);
            socket.send(mensajeDG); //  usa el mismo socket
            mensajesTxt.append("Tú -> " + destinatario + ": " + mensaje + "\n");
            mensajeTxt.setText("");
        } catch (Exception ex) {
            Logger.getLogger(PrincipalCli.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void enviarArchivo() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String destinatario = (String) comboClientes.getSelectedItem();
            if (destinatario == null || destinatario.equals(nombreCliente)) {
                JOptionPane.showMessageDialog(this, "Seleccione un destinatario válido");
                return;
            }

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                InetAddress direccion = InetAddress.getByName("127.0.0.1");

                // inicio de archivo
                String initMsg = "FILE:" + nombreCliente + ":" + destinatario + ":" + file.getName();
                DatagramPacket initPacket = new DatagramPacket(initMsg.getBytes(), initMsg.length(), direccion, PORT);
                socket.send(initPacket); //  mismo socket

                while ((bytesRead = fis.read(buffer)) != -1) {
                    DatagramPacket dp = new DatagramPacket(buffer, bytesRead, direccion, PORT);
                    socket.send(dp);
                }

                // fin de archivo
                String finMsg = "ENDFILE";
                DatagramPacket finPacket = new DatagramPacket(finMsg.getBytes(), finMsg.length(), direccion, PORT);
                socket.send(finPacket);

                mensajesTxt.append("Archivo enviado a " + destinatario + ": " + file.getName() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void escucharServidor() {
        try {
            FileOutputStream fos = null;
            boolean recibiendoArchivo = false;
            String nombreArchivo = "";
            File carpeta = new File("ArchivosRecibidos"); //  Carpeta local
            carpeta.mkdir(); // se crea si no existe

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket respuesta = new DatagramPacket(buffer, buffer.length);
                socket.receive(respuesta);

                String msgServidor = new String(respuesta.getData(), 0, respuesta.getLength());

                //  Inicio de archivo
                if (msgServidor.startsWith("FILE:")) {
                    String[] partes = msgServidor.split(":");
                    nombreArchivo = partes[3];
                    recibiendoArchivo = true;

                    File archivo = new File(carpeta, nombreArchivo);
                    fos = new FileOutputStream(archivo);

                    mensajesTxt.append(" Recibiendo archivo: " + nombreArchivo + "\n");
                    mensajesTxt.append(" Guardando en: " + archivo.getAbsolutePath() + "\n\n");
                }
                //  Fin de archivo
                else if (msgServidor.equals("ENDFILE")) {
                    if (fos != null) {
                        fos.close();
                    }
                    recibiendoArchivo = false;
                    mensajesTxt.append(" Archivo recibido correctamente: " + nombreArchivo + "\n\n");
                }
                //  Datos de archivo
                else if (recibiendoArchivo) {
                    fos.write(respuesta.getData(), 0, respuesta.getLength());
                }
                //  Lista de clientes
                else if (msgServidor.startsWith("CLIENTLIST:")) {
                    String[] nombres = msgServidor.substring(11).split(";");
                    SwingUtilities.invokeLater(() -> {
                        comboClientes.removeAllItems();
                        for (String n : nombres) {
                            if (!n.trim().isEmpty())
                                comboClientes.addItem(n.trim());
                        }
                    });
                }
                //  Mensajes normales
                else {
                    mensajesTxt.append(msgServidor + "\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new PrincipalCli(
                JOptionPane.showInputDialog("Ingrese su nombre de cliente")
        ).setVisible(true));
    }
}
