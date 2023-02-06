package com.google.gwt.core.shared;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * При компиляции заменяет GWT класс в тестах, тем самым позволяя писать в лог во время тестов.
 * НЕ УДАЛЯТЬ!
 *
 * @author s-ermakov
 */
public class GWT {

    private static final Logger log = LogManager.getLogger(GWT.class);

    private GWT() {

    }

    // не удалять, подставляется при компиляции
    public static void log(String message) {
        log(message, null);
    }

    // не удалять, подставляется при компиляции
    public static void log(String message, Throwable e) {
        if (e != null) {
            log.info(message, e);
        } else {
            log.info(message);
        }
    }
}
