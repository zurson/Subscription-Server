package org.example.server;

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
        try {

            if (!Validator.isValidReceivedMessage(receivedMessage.content())) {
                receivedMessage.client().sendMessage("Validation error");
                return;
            }

            messagesQueueDriver.addMessageToSendQueue("OKEJ", Collections.singletonList(receivedMessage.client()));

        } catch (IOException e) {
            receivedMessage.client().disconnect();
        }
    }

}
