package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;

/**

* Interfaz que define las operaciones básicas del servidor de chat.

* Incluye métodos para arranque, multidifusión de mensajes,

* eliminación de clientes y apagado del servidor.

*

* @author Alejandro Navas García

* @author Nicolás Muñoz Miguel

*/

public interface ChatServer {

/**

* Inicia el servidor, configurando los recursos necesarios.

*/

void startup();

/**

* Cierra todas las conexiones activas y apaga el servidor.

*/

void shutdown();

/**

* Envía un mensaje a todos los clientes conectados.

*

* @param message Mensaje que será enviado a los clientes.

*/

void broadcast(ChatMessage message);

/**

* Elimina un cliente de la lista de clientes activos.

*

* @param client Cliente que será eliminado.

*/

void remove(int id);

}
