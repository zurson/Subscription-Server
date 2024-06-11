package org.example.server.receive_message.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.client.ClientThread;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
public class MessageResponse {

    private String content;
    private Set<ClientThread> recipients;
    private boolean success;


    public MessageResponse(String content, List<ClientThread> recipients, boolean success) {
        this.content = content;
        this.recipients = new HashSet<>(recipients);
        this.success = success;
    }


    public List<ClientThread> getRecipients() {
        return new ArrayList<>(recipients);
    }
}
