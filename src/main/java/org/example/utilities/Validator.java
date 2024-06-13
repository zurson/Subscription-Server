package org.example.utilities;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validator {

    private static final String CIDR_REGEX = "^([0-9]{1,3}\\.){3}[0-9]{1,3}/([0-9]|[1-2][0-9]|3[0-2])$";
    private static final Pattern CIDR_PATTERN = Pattern.compile(CIDR_REGEX);

    private final static ValidatorFactory factory = Validation.buildDefaultValidatorFactory();


    private Validator() {
    }


//    public static boolean isValidIPv4Address(String ip) {
//        if (ip == null || ip.isEmpty())
//            return false;
//
//        if (ip.equals("localhost"))
//            return true;
//
//        String ipv4Pattern = "^(([0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
//
//        Pattern ipv4 = Pattern.compile(ipv4Pattern);
//        Matcher ipv4Matcher = ipv4.matcher(ip);
//
//        return ipv4Matcher.matches();
//    }


    public static boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }


    public static boolean isValidTimeout(int timeout) {
        return timeout >= 1;
    }


    public static <T> Set<ConstraintViolation<T>> validateJsonObject(T object) {
        if (object == null)
            return null;

        javax.validation.Validator validator = factory.getValidator();
        return validator.validate(object);
    }


    public static <T> Optional<String> validatePayload(T payload, Class<T> payloadClass) {
        if (payload == null)
            return Optional.of("Payload is null");

        Set<ConstraintViolation<T>> payloadViolations = factory.getValidator().validate(payload);
        if (!payloadViolations.isEmpty()) {
            StringBuilder errors = new StringBuilder("Payload validation error: ");
            payloadViolations.forEach(violation -> errors.append(violation.getPropertyPath()).append(" ").append(violation.getMessage()).append("; "));
            return Optional.of(errors.toString());
        }

        return Optional.empty();
    }


    public static boolean validateCIDRList(List<String> ipAddresses) {
        for (String cidr : ipAddresses) {
            if (!isValidCIDR(cidr))
                return false;
        }

        return true;
    }


    public static boolean isValidCIDR(String cidr) {
        Matcher matcher = CIDR_PATTERN.matcher(cidr);
        if (!matcher.matches()) {
            return false;
        }

        String[] parts = cidr.split("/");
        String ip = parts[0];
        String prefix = parts[1];

        if (!isValidIPAddress(ip)) {
            return false;
        }

        int prefixLength = Integer.parseInt(prefix);
        return prefixLength >= 0 && prefixLength <= 32;
    }


    public static boolean isValidIPAddress(String ip) {
        try {
            InetAddress inet = InetAddress.getByName(ip);
            return inet.getHostAddress().equals(ip);
        } catch (UnknownHostException e) {
            return false;
        }
    }


    public static boolean isValidSizeLimit(int sizeLimit) {
        return sizeLimit > 0;
    }
}
