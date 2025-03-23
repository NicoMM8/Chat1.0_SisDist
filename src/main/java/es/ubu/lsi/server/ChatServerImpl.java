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
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación del servidor de chat.
 * El servidor por defecto escucha en el puerto 1500 y gestiona
 * la conexión de múltiples clientes mediante hilos independientes.
 * 
 * @author Alejandro Navas García
 * @author Nicolás Muñoz Miguel
 * 
 * @version 1.0
 */
public class ChatServerImpl implements ChatServer {
	/** Puerto por defecto para el servidor. */
    private static int DEFAULT_PORT = 1500;
    /** Mapa concurrente de clientes conectados identificados por su ID. */
    private final ConcurrentHashMap<Integer, ServerThreadForClient> clients = new ConcurrentHashMap<>();
    /** Formato de fecha para los logs del servidor. */
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    /** Indica si el servidor está en ejecución. */
    private boolean alive = true;
    /** Contador para asignar IDs únicos a los clientes. */
    private int clientId = 0;
    /** Mapa de clientes identificados por sus nombres de usuario (nickname). */
    private final Map<String, ServerThreadForClient> clientsByName = new ConcurrentHashMap<>();
    
    
    /**
     * Constructor que permite definir un puerto para el servidor.
     * Si el puerto no se especifica o no es válido, se usará el puerto predeterminado 1500.
     * 
     * @param port El puerto en el que el servidor escuchará conexiones.
     */
    public ChatServerImpl(int port) {
        // Valida el puerto proporcionado
        if (port <= 0 || port > 65535) {
            System.out.println("Puerto inválido. Usando el puerto predeterminado: 1500.");
            port = 1500; // Configuración del puerto predeterminado
        }
        DEFAULT_PORT = port; // Asignación del puerto al atributo
        System.out.println("Servidor configurado para escuchar en el puerto " + DEFAULT_PORT);
    }

    /**
     * Constructor por defecto que usa el puerto 1500.
     */
    private ChatServerImpl() {
        this(1500);
    }

    
    /**
     * Método principal que arranca el servidor de chat.
     * Configura el servidor y permite interacciones desde la consola.
     * 
     * @param args No requiere argumentos.
     */
    public static void main(String[] args) {
        ChatServerImpl server = new ChatServerImpl();
        server.startup();
        server.listenForUserInput();
    }

    /**
     * Método que inicia el servidor.
     * Crea un socket del servidor y acepta conexiones de clientes.
     */
    @Override
    public void startup() {
        try (ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT)) {
            System.out.println("Servidor iniciado en puerto " + DEFAULT_PORT);

            while (alive) {
                Socket socket = serverSocket.accept();
                int id = ++clientId;
                System.out.println(sdf.format(new Date()) + " Cliente conectado con ID " + id);

                ServerThreadForClient thread = new ServerThreadForClient(socket, id);
                clients.put(id, thread);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    /**
     * Método para retransmitir mensajes a todos los clientes conectados.
     * 
     * @param message Mensaje a retransmitir.
     */
    @Override
    public void broadcast(ChatMessage message) {
        clients.values().forEach(client -> client.sendMessage(message));
    }

    /**
     * Método que elimina un cliente del servidor.
     * 
     * @param id Identificador único del cliente a eliminar.
     */
    @Override
    public void remove(int id) {
        ServerThreadForClient client = clients.remove(id);
        if (client != null) {
            clientsByName.remove(client.username);
            client.closeConnection();
        }
    }

    /**
     * Método que apaga el servidor.
     * Cierra todas las conexiones activas de los clientes.
     */
    @Override
    public void shutdown() {
        alive = false;
        clients.values().forEach(ServerThreadForClient::closeConnection);
        clients.clear();
    }
    
    /**
     * Escucha comandos administrativos desde la consola del servidor.
     * Comandos admitidos:
     * - "logout": Apaga el servidor.
     * - "ban ": Bloquea a un cliente.
     * - "unban ": Desbloquea a un cliente.
     */
    private void listenForUserInput() {
        Scanner scanner = new Scanner(System.in);
        while (alive) {
            System.out.print("> ");
            String input = scanner.nextLine();

            if ("logout".equalsIgnoreCase(input)) {
                shutdown();
                break;
            } else if (input.startsWith("ban ") || input.startsWith("unban ")) {
                broadcast(new ChatMessage(0, ChatMessage.MessageType.MESSAGE, input));
            } else {
                broadcast(new ChatMessage(0, ChatMessage.MessageType.MESSAGE, input));
            }
        }
        scanner.close();
    }


    /**
     * Clase interna que representa un hilo de cliente conectado al servidor.
     * Gestiona la comunicación con un cliente específico.
     */
    private class ServerThreadForClient extends Thread {
    	/** Socket del cliente. */
    	private final Socket socket;
    	/** ID único del cliente. */
    	private final int id;
    	/** Nombre de usuario (nickname) del cliente. */
    	private String username;
    	/** Flujo de salida hacia el cliente. */
    	private ObjectOutputStream outputStream;
    	/** Flujo de entrada desde el cliente. */
    	private Set<Integer> blockedUsers = ConcurrentHashMap.newKeySet();
    	/** Lista de IDs de clientes bloqueados por este cliente. */
    	private ObjectInputStream inputStream;

        /**
         * Constructor que configura la conexión con el cliente.
         * 
         * @param socket Socket del cliente.
         * @param id ID único del cliente.
         * @throws IOException Si ocurre un error en los flujos o el socket.
         */
    	public ServerThreadForClient(Socket socket, int id) throws IOException {
    	    this.socket = socket;
    	    this.id = id;

    	    // Inicializa flujos en orden
    	    this.outputStream = new ObjectOutputStream(socket.getOutputStream());
    	    this.inputStream = new ObjectInputStream(socket.getInputStream());

    	    // Lee el mensaje inicial (nickname)
    	    try {
    	        ChatMessage initialMessage = (ChatMessage) inputStream.readObject();
    	        this.username = initialMessage.getMessage();

    	        // Valida si el nickname ya está en uso
    	        if (clientsByName.containsKey(username)) {
    	            throw new IOException("El apodo ya está en uso: " + username);
    	        }

    	        clientsByName.put(username, this); // Registrar al cliente por nombre
    	    } catch (ClassNotFoundException e) {
    	        System.err.println("Clase no encontrada al leer mensaje del cliente: " + e.getMessage());
    	    }
    	}

        /**
         * Método que ejecuta el hilo del cliente.
         * Procesa mensajes entrantes y comandos como "ban" y "unban".
         */
        @Override
        public void run() {
            try {
                // Bucle para procesar mensajes de los clientes
                while (true) {
                    ChatMessage message = (ChatMessage) inputStream.readObject();

                    if (message.getType() == ChatMessage.MessageType.LOGOUT) {
                        // Cliente solicita desconexión
                        System.out.println(sdf.format(new Date()) + " Cliente " + username + " (ID " + id + ") se ha desconectado.");
                        break;
                    } else if (message.getType() == ChatMessage.MessageType.MESSAGE) {
                        String text = message.getMessage();

                        // Procesa comando "ban"
                        if (text.startsWith("ban ")) {
                            try {
                                int blockedId = Integer.parseInt(text.split(" ")[1]);
                                banUser(blockedId);

                                // Notifica a todos los clientes del bloqueo
                                String blockNotification = username + " ha bloqueado al usuario con ID " + blockedId;
                                broadcast(new ChatMessage(id, ChatMessage.MessageType.MESSAGE, blockNotification));

                            } catch (NumberFormatException e) {
                                // Error en el formato del ID
                                sendMessage(new ChatMessage(id, ChatMessage.MessageType.MESSAGE, "Error: Formato de comando inválido. Usa 'ban <id>'."));
                            }
                        }
                        // Procesa comando "unban"
                        else if (text.startsWith("unban ")) {
                            try {
                                int unblockedId = Integer.parseInt(text.split(" ")[1]);
                                unbanUser(unblockedId);

                                // Notifica a todos los clientes del desbloqueo
                                String unblockNotification = username + " ha desbloqueado al usuario con ID " + unblockedId;
                                broadcast(new ChatMessage(id, ChatMessage.MessageType.MESSAGE, unblockNotification));

                            } catch (NumberFormatException e) {
                                // Error en el formato del ID
                                sendMessage(new ChatMessage(id, ChatMessage.MessageType.MESSAGE, "Error: Formato de comando inválido. Usa 'unban <id>'."));
                            }
                        }
                        // Procesa mensaje normal
                        else {
                            System.out.println(sdf.format(new Date()) + " [" + username + "]: " + text);
                            broadcast(message);
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error en la comunicación con el cliente " + username + " (ID " + id + "): " + e.getMessage());
            } finally {
                // Desconexión del cliente
                closeConnection();
                remove(id); // Remueve el cliente del mapa del servidor
            }
        }

        /**
         * Envía un mensaje al cliente, verificando que no esté bloqueado.
         * 
         * @param message Mensaje a enviar.
         */
        public void sendMessage(ChatMessage message) {
            if (!blockedUsers.contains(message.getId())) { // Solo enviar si no está bloqueado
                try {
                    outputStream.writeObject(message);
                } catch (IOException e) {
                    System.err.println("Error al enviar mensaje al cliente " + id + ": " + e.getMessage());
                }
            }
        }


        /**
         * Cierra la conexión con el cliente, liberando recursos.
         */
        private void closeConnection() {
        	try {
        	    if (inputStream != null) inputStream.close();
        	    if (outputStream != null) outputStream.close();
        	    if (socket != null) socket.close();
        	} catch (IOException e) {
        	    System.err.println("Error cerrando recursos del cliente " + id + ": " + e.getMessage());
        	}
        }
        
        /**
         * Bloquea los mensajes de un cliente específico.
         * 
         * @param userId ID del cliente a bloquear.
         */
        public void banUser(int userId) {
            blockedUsers.add(userId);
            System.out.println(username + " ha bloqueado al usuario con ID " + userId);
        }

        /**
         * Desbloquea los mensajes de un cliente específico.
         * 
         * @param userId ID del cliente a desbloquear.
         */
        public void unbanUser(int userId) {
            blockedUsers.remove(userId);
            System.out.println(username + " ha desbloqueado al usuario con ID " + userId);
        }

    }
}