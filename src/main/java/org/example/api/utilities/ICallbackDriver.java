package org.example.api.utilities;

import java.util.function.Consumer;

public interface ICallbackDriver {

    Consumer<Message> getCallback(String key);

}
