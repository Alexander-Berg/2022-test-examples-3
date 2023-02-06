package ru.yandex.market.crm.core.test;

import javax.annotation.Nonnull;

import com.google.common.base.Strings;

/**
 * @author apershukov
 */
public class EnvProvider {

    @Nonnull
    public static String getEnv(String name) {
        String value = System.getenv(name);
        if (Strings.isNullOrEmpty(value)) {
            throw new IllegalStateException("Environment variable '" + name + "' is not set");
        }
        return value;
    }
}
