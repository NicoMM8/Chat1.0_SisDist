package es.ubu.lsi.client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

/**
 * Implementación del cliente de chat.
 * 
 * Esta clase establece la conexión con el servidor, gestiona el envío y recepción de mensajes,
 * y proporciona un mecanismo para que el cliente interactúe con otros usuarios conectados.
 * 
 * @author Alejandro Navas García
 * @author Nicolás Muñoz Miguel
 * 
 * @version 1.0
 */
public class ChatClientImpl implements ChatClient {

    /** Dirección del servidor (IP o hostname). */
    private final String server;

    /** Nombre del usuario en el chat. */
    private final String username;

    /** Puerto del servidor para la conexión (por defecto 1500). */
    private int port = 1500;

    /** Estado del cliente (activo o desconectado). */
    private boolean alive = true;

    /** Identificador único del cliente. */
    private int id;

    /** Socket utilizado para la conexión al servidor. */
    private Socket socket;

    /** Flujo de salida hacia el servidor. */
    private ObjectOutputStream outputStream;

    /** Flujo de entrada desde el servidor. */
    private ObjectInputStream inputStream;

    /**
     * Constructor que inicializa el cliente con los datos del servidor y del usuario.
     * 
     * @param server Dirección del servidor (puede ser "localhost" si no se especifica).
     * @param port Puerto en el que el servidor está escuchando.
     * @param username Nombre del usuario que se conectará.
     */
    public ChatClientImpl(String server, int port, String username) {
        this.server = (server == null || server.isEmpty()) ? "localhost" : server;
        this.port = port;
        this.username = username;
    }

    /**
     * Establece la conexión del cliente con el servidor.
     * Configura los flujos de entrada y salida para la comunicación.
     * 
     * @return true si la conexión se establece correctamente, false si ocurre algún error.
     */
    @Override
    public boolean start() {
        try {
            // Intento de conexión al servidor
            socket = new Socket(server, port);
            System.out.println("Conectado al servidor " + server + " en el puerto " + port);

            // Configuración de flujos de datos
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            // Envia el nickname al servidor como primer mensaje
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

    /**
     * Envía un mensaje al servidor para que sea procesado y retransmitido.
     * 
     * @param msg Objeto de tipo {@link ChatMessage} que contiene la información
     *            del mensaje que se enviará.
     */
    @Override
    public void sendMessage(ChatMessage msg) {
        try {
            // Log del mensaje enviado
            System.out.println("Alejandro y Nico" + " patrocinan el mensaje: " + msg.getMessage());
            outputStream.writeObject(msg); // Enviamos el mensaje al servidor
        } catch (Exception e) {
            System.err.println("Error al enviar el mensaje: " + e.getMessage());
        }
    }

    /**
     * Desconecta el cliente del servidor, enviando un mensaje de tipo LOGOUT
     * y cerrando los recursos asociados.
     */
    @Override
    public void disconnect() {
        alive = false;
        try {
            // Envia mensaje de logout antes de cerrar la conexión
            sendMessage(new ChatMessage(id, MessageType.LOGOUT, "logout"));
            if (socket != null) socket.close();
            System.out.println("Desconectado del servidor.");
        } catch (Exception e) {
            System.err.println("Error al desconectar: " + e.getMessage());
        }
    }

    /**
     * Método principal que lanza el cliente de chat.
     * Requiere los argumentos del servidor, puerto y nombre de usuario.
     * 
     * @param args Argumentos: servidor, puerto y nickname.
     */
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

    /**
     * Maneja la entrada del usuario desde la consola.
     * Permite que el cliente introduzca mensajes para enviarlos al servidor,
     * y procesa el comando 'logout' para desconexión.
     */
    private void listenForUserInput() {
        Scanner scanner = new Scanner(System.in);
        while (alive) {
            System.out.print("> ");
            String input = scanner.nextLine();

            if ("logout".equalsIgnoreCase(input)) {
                disconnect();
                break;
            }

            // Envia el mensaje escrito por el usuario
            sendMessage(new ChatMessage(id, MessageType.MESSAGE, input));
        }
        scanner.close();
    }

    /**
     * Clase interna que escucha mensajes enviados por el servidor.
     * Procesa los mensajes recibidos y actúa según su tipo (por ejemplo, mensajes
     * de texto o apagado del servidor).
     */
    private class ChatClientListener implements Runnable {

    	/**
    	 * Constructor.
    	 */
    	private ChatClientListener() {
    		
    	}
        /**
         * Método que ejecuta el hilo de escucha.
         * Procesa los mensajes recibidos del servidor y actúa según su tipo.
         */
        @Override
        public void run() {
            try {
                while (alive) {
                    ChatMessage message = (ChatMessage) inputStream.readObject();

                    // Log del mensaje recibido
                    System.out.println("Alejandro y Nico" + " patrocinan el mensaje: " + message.getMessage());

                    // Procesa el mensaje según su tipo
                    if (message.getType() == MessageType.MESSAGE) {
                        System.out.println("[" + message.getId() + "] " + message.getMessage());
                    } else if (message.getType() == MessageType.SHUTDOWN) {
                        System.out.println("El servidor está apagándose. Desconectando...");
                        alive = false;
                        break;
                    }
                }
            } catch (Exception e) {
                if (alive) {
                    System.err.println("Error en ChatClientListener: " + e.getMessage());
                }
            }
        }
    }
}

