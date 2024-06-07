package org.example.server.receive_message;

import org.example.client.ClientThread;
import org.example.interfaces.MessagesQueueDriver;
import org.example.utilities.Validator;

import java.io.IOException;
import java.util.Collections;

import static org.example.settings.Settings.QUEUE_CHECK_INTERVAL_MS;

public class ReceivedMessagesQueueMonitorThread extends Thread {

    private final MessagesQueueDriver messagesQueueDriver;

    public ReceivedMessagesQueueMonitorThread(MessagesQueueDriver driver) {
        this.messagesQueueDriver = driver;
    }


    @Override
    public void run() {

        try {
            while (true) {
                ReceivedMessage receivedMessage = messagesQueueDriver.pollMessage();

                if (receivedMessage == null) {
                    Thread.sleep(QUEUE_CHECK_INTERVAL_MS);
                    continue;
                }

                manageMessage(receivedMessage);
            }
        } catch (InterruptedException ignored) {
        }

    }


    private void manageMessage(ReceivedMessage receivedMessage) {
        String content = receivedMessage.content();
        ClientThread client = receivedMessage.client();

        try {
            if (!Validator.isValidReceivedMessage(content)) {
                client.sendMessage("Validation error");
                return;
            }

            // TODO: parsing JSON into class

            client.setClientId("TEST ID");

            messagesQueueDriver.addMessageToSendQueue("OKEJ", Collections.singletonList(client));

        } catch (IOException e) {
            client.disconnect();
        }
    }

}
