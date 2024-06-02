package org.example.interfaces;

import org.example.client.ClientThread;

public interface ServerDriver {

    void addClient(ClientThread clientThread);
    void removeClient(ClientThread clientThread);

}
