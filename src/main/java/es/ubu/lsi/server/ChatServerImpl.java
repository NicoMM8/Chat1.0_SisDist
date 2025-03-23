package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación del servidor de chat.
 * El servidor por defecto escucha en el puerto 1500 y gestiona
 * la conexión de múltiples clientes mediante hilos independientes.
 * 
 * @author Alejandro Navas García
 * @author Nicolás Muñoz Miguel
 */
public class ChatServerImpl {
    private static final int DEFAULT_PORT = 1500; // Puerto predeterminado
    
    private int port; // Puerto del servidor
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss"); // Formato de hora
    private final Map<Integer, ServerThreadForClient> clients = new ConcurrentHashMap<>(); // Clientes conectados
    private boolean alive; // Estado del servidor
    private int clientId; // Contador de IDs para los clientes

    /**
     * Constructor por defecto (usa el puerto 1500).
     */
    public ChatServerImpl() {
        this(DEFAULT_PORT);
    }

    /**
     * Constructor que permite definir un puerto.
     * 
     * @param port Puerto del servidor.
     */
    public ChatServerImpl(int port) {
        this.port = port;
        this.alive = true;
        this.clientId = 0; // Inicializar clientId
    }

    /**
     * Método principal que arranca el servidor.
     */
    public static void main(String[] args) {
        ChatServerImpl server = new ChatServerImpl();
        server.startup();
    }

    /**
     * Inicia el servidor y espera conexiones de clientes.
     */
    public void startup() {
        System.out.println("Iniciando servidor en el puerto " + port + "...");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor iniciado correctamente. Esperando conexiones...");
            while (alive) {
                Socket clientSocket = serverSocket.accept(); // Aceptar conexión

                // Incrementar clientId y asignarlo al cliente
                int newClientId = ++clientId; 
                System.out.println(sdf.format(new Date()) + " Cliente conectado con ID " + newClientId);

                // Crear y arrancar el hilo del cliente
                ServerThreadForClient clientThread = new ServerThreadForClient(clientSocket, newClientId);
                clients.put(newClientId, clientThread);
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    /**
     * Elimina un cliente de la lista de clientes activos.
     * 
     * @param id ID del cliente a eliminar.
     */
    public void remove(int id) {
        ServerThreadForClient client = clients.remove(id);
        if (client != null) {
            System.out.println(sdf.format(new Date()) + " Cliente con ID " + id + " eliminado.");
            client.closeConnection();
        } else {
            System.out.println(sdf.format(new Date()) + " Cliente con ID " + id + " no encontrado.");
        }
    }

    /**
     * Apaga el servidor y cierra todas las conexiones.
     */
    public void shutdown() {
        System.out.println("Apagando el servidor...");
        alive = false;
        for (ServerThreadForClient client : clients.values()) {
            client.closeConnection();
        }
        clients.clear();
        System.out.println("Servidor apagado correctamente.");
    }


    /**
     * Clase interna que gestiona la comunicación con un cliente.
     */
    private class ServerThreadForClient extends Thread {
        private final Socket socket; // Conexión del cliente
        private final int id; // ID único del cliente
        private String username; // Nombre del usuario
        private ObjectInputStream input; // Flujo de entrada
        private ObjectOutputStream output; // Flujo de salida

        /**
         * Constructor del hilo de cliente.
         * 
         * @param socket Socket del cliente.
         * @param id ID único del cliente.
         */
        public ServerThreadForClient(Socket socket, int id) {
            this.socket = socket;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                // Configuración de los flujos (salida primero para evitar problemas)
                output = new ObjectOutputStream(socket.getOutputStream());
                input = new ObjectInputStream(socket.getInputStream());

                // Leer el nombre de usuario al conectarse
                ChatMessage initialMessage = (ChatMessage) input.readObject();
                this.username = initialMessage.getMessage();
                System.out.println(sdf.format(new Date()) + " Cliente ID " + id + " registrado como " + username);

                // Bucle para recibir mensajes
                while (true) {
                    ChatMessage message = (ChatMessage) input.readObject();
                    if (message.getType() == ChatMessage.MessageType.LOGOUT) {
                        System.out.println(sdf.format(new Date()) + " Cliente " + username + " desconectado.");
                        break;
                    } else if (message.getType() == ChatMessage.MessageType.MESSAGE) {
                        System.out.println(sdf.format(new Date()) + " [" + username + "]: " + message.getMessage());
                        broadcast(message); // Enviar mensaje a todos
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error en el cliente " + id + ": " + e.getMessage());
            } finally {
                remove(id); // Eliminar al cliente
                closeConnection();
            }
        }

        /**
         * Envía un mensaje al cliente.
         * 
         * @param message Mensaje a enviar.
         */
        private void sendMessage(ChatMessage message) {
            try {
                output.writeObject(message);
            } catch (IOException e) {
                System.err.println("Error al enviar mensaje al cliente " + id + ": " + e.getMessage());
            }
        }

        /**
         * Cierra la conexión del cliente.
         */
        private void closeConnection() {
            safeClose(socket);
            safeClose(input);
            safeClose(output);
            System.out.println(sdf.format(new Date()) + " Conexión cerrada para cliente " + id);
        }

        /**
         * Método helper para cerrar recursos de forma segura.
         * 
         * @param resource Recurso a cerrar.
         */
        private void safeClose(Closeable resource) {
            try {
                if (resource != null) resource.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar recurso: " + e.getMessage());
            }
        }
    }

}
