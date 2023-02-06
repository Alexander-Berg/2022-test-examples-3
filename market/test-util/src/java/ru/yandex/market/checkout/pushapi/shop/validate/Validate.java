package ru.yandex.market.checkout.pushapi.shop.validate;

import java.math.BigDecimal;
import java.util.Collection;

public class Validate {
    
    private static void exception(String message) {
        throw new ValidationException("validate error: " + message);
    }
    
    public static void notNull(Object object, String message) throws ValidationException {
        if(object == null) {
            exception(message);
        }
    }
    
    public static void isNull(Object object, String message) throws ValidationException {
        if(object != null) {
            exception(message);
        }
    }
    
    public static void nonEmpty(Collection list, String message) throws ValidationException {
        if(list.isEmpty() || list.size() <= 0) {
            exception(message);
        }
    }
    
    public static void positive(Long value, String message) throws ValidationException {
        if(value <= 0) {
            exception(message);
        }
    }

    public static void positive(Integer value, String message) throws ValidationException {
        if(value <= 0) {
            exception(message);
        }
    }
    
    public static void positive(BigDecimal value, String message) throws ValidationException {
        if(value.compareTo(BigDecimal.ZERO) <= 0) {
            exception(message);
        }
    }
    
    public static void nonNegative(Integer value, String message) throws ValidationException {
        if(value < 0) {
            exception(message);
        }
    }

    public static void nonNegative(Long value, String message) throws ValidationException {
        if(value < 0) {
            exception(message);
        }
    }

    public static void nonNegative(BigDecimal value, String message) throws ValidationException {
        if(value.compareTo(BigDecimal.ZERO) < 0) {
            exception(message);
        }
    }
    
    public static void nonEmpty(String value, String message) throws ValidationException {
        if(value.trim().isEmpty()) {
            exception(message);
        }
    }

    public static void eq(Object o1, Object o2, String message) throws ValidationException {
        if(!o1.equals(o2)) {
            exception(message);
        }
    }

    public static void notLessThan(int o1, int o2, String message) throws ValidationException {
        if(o1 < o2) {
            exception(message);
        }
    }

    public static void isTrue(boolean condition, String message) throws ValidationException {
        if(!condition) {
            exception(message);
        }
    }

    public static void maximumLength(String value, int maxLength, String message) throws ValidationException {
        if(value.length() > maxLength) {
            exception(message);
        }
    }
    
}
