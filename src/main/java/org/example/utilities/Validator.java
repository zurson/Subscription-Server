package org.example.utilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validator {

    public static boolean isValidIPv4Address(String ip) {
        String ipv4Pattern = "^(([0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";

        Pattern ipv4 = Pattern.compile(ipv4Pattern);
        Matcher ipv4Matcher = ipv4.matcher(ip);

        return ipv4Matcher.matches();
    }


    public static boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }


    public static boolean isValidTimeout(int timeout) {
        return timeout >= 1;
    }


    public static boolean isValidReceivedMessage(String receivedMessage) {
        if (receivedMessage == null || receivedMessage.isEmpty())
            return false;

        return true;
    }
}
