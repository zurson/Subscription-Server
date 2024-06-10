package org.example.server.receive_message.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.client.ClientThread;

import java.util.List;

@Data
@AllArgsConstructor
public class MessageResponse {

    private String content;
    private List<ClientThread> recipients;

}
