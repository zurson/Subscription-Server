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

    private String error;


    public MessageResponse(String content, List<ClientThread> recipients, boolean success, String error) {
        this.content = content;
        this.recipients = new HashSet<>(recipients);
        this.success = success;
        this.error = error;
    }


    public List<ClientThread> getRecipients() {
        return new ArrayList<>(recipients);
    }
}
