package org.example.server.receive_message.withdraw;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.client.ClientThread;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class SubscriptionRemoveData {

    private String topicName;
    private List<ClientThread> clientsToDisconnect;
    private List<ClientThread> clientsToNotify;

    public SubscriptionRemoveData(String topicName) {
        clientsToDisconnect = new ArrayList<>();
        clientsToNotify = new ArrayList<>();
    }

    public void addClientToDisconnect(ClientThread client) {
        clientsToDisconnect.add(client);
    }

    public void addClientToNotify(ClientThread client) {
        clientsToNotify.add(client);
    }

}
