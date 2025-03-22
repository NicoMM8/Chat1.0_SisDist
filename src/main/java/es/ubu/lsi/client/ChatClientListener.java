package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;
import java.io.ObjectInputStream;

public class ChatClientListener implements Runnable {
    private final ObjectInputStream inputStream;

    ChatClientListener(ObjectInputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void run() {
        try {
            while (true) {
                // Leer el siguiente mensaje del servidor
                ChatMessage message = (ChatMessage) inputStream.readObject();

                // Mostrar el mensaje en consola
                System.out.println(message.getSender() + ": " + message.getContent());
            }
        } catch (Exception e) {
            System.err.println("Error en ChatClientListener: " + e.getMessage());
        }
    }
}
