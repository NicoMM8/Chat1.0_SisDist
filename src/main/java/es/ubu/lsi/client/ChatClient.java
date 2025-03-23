package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

/**
 * Interfaz que define las operaciones básicas de un cliente de chat.
 * Proporciona métodos para iniciar la conexión, enviar mensajes
 * y desconectarse del servidor.
 * 
 * @author Alejandro Navas García
 * @author Nicolás Muñoz Miguel
 * 
 * @version 1.0
 */
public interface ChatClient {

	/**
     * Inicia la conexión del cliente con el servidor.
     * Este método configura los recursos necesarios para la comunicación y
     * establece la conexión al servidor.
     *
     * @return true si la conexión se establece correctamente, false en caso contrario.
     */
	boolean start();
	
	/**
     * Envía un mensaje al servidor para que sea retransmitido a otros clientes.
     * Este método permite al cliente comunicarse con otros usuarios
     * conectados al servidor.
     *
     * @param msg Objeto de tipo {@link ChatMessage} que contiene la información
     *            del mensaje a enviar.
     */
	void sendMessage(ChatMessage msg);
	
	/**
     * Finaliza la conexión del cliente con el servidor.
     * Este método libera los recursos utilizados y asegura
     * que la conexión se cierre de forma ordenada.
     */
	void disconnect();
}
