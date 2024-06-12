package org.example.api.utilities.status;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
public class ClientTopics {

    private List<String> producing;
    private List<String> subscribing;


    public ClientTopics() {
        this.producing = new ArrayList<>();
        this.subscribing = new ArrayList<>();
    }

}
