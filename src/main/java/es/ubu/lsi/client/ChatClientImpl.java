package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ChatClientImpl implements ChatClient {
    private final String server; // Dirección del servidor
    private final String username; // Nombre del usuario
    private final int port; // Puerto para la conexión
    private boolean carryOn = true; // Control de ejecución del cliente
    private int id; // ID del cliente

    private Socket socket; // Socket para la conexión
    private ObjectOutputStream outputStream; // Flujo de salida
    private ObjectInputStream inputStream; // Flujo de entrada

    // Constructor que inicializa el cliente con los datos del servidor y el usuario
    public ChatClientImpl(String server, int port, String username) {
        this.server = (server == null || server.isEmpty()) ? "localhost" : server;
        this.port = port;
        this.username = username;
    }

    @Override
    public boolean start() {
        try {
            // Intento de conexión al servidor
            socket = new Socket(server, port);
            System.out.println("Conectado al servidor " + server + " en el puerto " + port);

            // Configuración de flujos de datos
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            // Enviar el nickname al servidor como primer mensaje
            outputStream.writeObject(new ChatMessage(id, MessageType.MESSAGE, username));

            // Inicia el hilo que escucha los mensajes entrantes
            Thread listenerThread = new Thread(new ChatClientListener());
            listenerThread.start();

            return true;
        } catch (Exception e) {
            System.err.println("Error al conectar al servidor: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void sendMessage(ChatMessage msg) {
        try {
            outputStream.writeObject(msg); // Enviar mensaje al servidor
        } catch (Exception e) {
            System.err.println("Error al enviar el mensaje: " + e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        carryOn = false;
        try {
            // Enviar mensaje de logout antes de cerrar la conexión
            sendMessage(new ChatMessage(id, MessageType.LOGOUT, "logout"));
            if (socket != null) socket.close();
            System.out.println("Desconectado del servidor.");
        } catch (Exception e) {
            System.err.println("Error al desconectar: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso: java es.ubu.lsi.client.ChatClientImpl <servidor> <puerto> <nickname>");
            return;
        }
        String server = args[0];
        int port = Integer.parseInt(args[1]);
        String username = args[2];

        ChatClientImpl client = new ChatClientImpl(server, port, username);
        if (client.start()) {
            client.listenForUserInput(); // Iniciar manejo de entrada del usuario
        }
    }

    private void listenForUserInput() {
        Scanner scanner = new Scanner(System.in);
        while (carryOn) {
            System.out.print("> ");
            String input = scanner.nextLine();

            if ("logout".equalsIgnoreCase(input)) {
                disconnect();
                break;
            }

            // Enviar el mensaje escrito por el usuario
            sendMessage(new ChatMessage(id, MessageType.MESSAGE, input));
        }
        scanner.close();
    }

    // Clase interna para manejar la escucha de mensajes del servidor
    private class ChatClientListener implements Runnable {
        @Override
        public void run() {
            try {
                while (carryOn) {
                    ChatMessage message = (ChatMessage) inputStream.readObject();

                    if (message.getType() == MessageType.MESSAGE) {
                        System.out.println("[" + message.getId() + "] " + message.getMessage());
                    } else if (message.getType() == MessageType.SHUTDOWN) {
                        System.out.println("El servidor está apagándose. Desconectando...");
                        carryOn = false;
                        break;
                    }
                }
            } catch (Exception e) {
                if (carryOn) {
                    System.err.println("Error en ChatClientListener: " + e.getMessage());
                }
            }
        }
    }
}

