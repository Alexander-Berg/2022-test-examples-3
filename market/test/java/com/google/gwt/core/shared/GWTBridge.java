package com.google.gwt.core.shared;

/**
 * При компиляции заменяет GWT класс в тестах!
 * НЕ УДАЛЯТЬ!
 *
 * @author s-ermakov
 */
public abstract class GWTBridge {
    public GWTBridge() {
    }

    public abstract <T> T create(Class<?> var1);

    public String getThreadUniqueID() {
        return "";
    }

    public abstract String getVersion();

    public abstract boolean isClient();

    public abstract void log(String var1, Throwable var2);
}
