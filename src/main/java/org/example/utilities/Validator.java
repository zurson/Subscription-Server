package org.example.utilities;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validator {

    private final static ValidatorFactory factory = Validation.buildDefaultValidatorFactory();


    private Validator() {
    }


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


    public static <T> Set<ConstraintViolation<T>> validateJsonObject(T object) {
        if (object == null)
            return null;

        javax.validation.Validator validator = factory.getValidator();
        return validator.validate(object);
    }

    public static  <T> Optional<String> validatePayload(T payload, Class<T> payloadClass) {
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
}
