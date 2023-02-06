package ru.yandex.market.crm.core.test;

import javax.validation.constraints.NotNull;

import com.google.common.base.Strings;

import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;

/**
 * Created by vivg on 23.06.17.
 */
public class TestEnvironmentResolver implements EnvironmentResolver {

    private Environment environment;

    private static IllegalArgumentException invalidEnvironment(String value) {
        return new IllegalArgumentException(String.format("Invalid environment = %s", value));
    }

    @Override
    public Environment get() {
        return detectEnvironment();
    }

    @NotNull
    private Environment detectEnvironment() {
        if (null != environment) {
            return environment;
        }
        String fromProperties = System.getProperty("environment");
        if (!Strings.isNullOrEmpty(fromProperties)) {
            return parse(fromProperties);
        }
        return Environment.INTEGRATION_TEST;
    }

    private Environment parse(String value) {
        String prepared = value.toUpperCase();
        try {
            return Environment.valueOf(prepared);
        } catch (Throwable t) {
            throw invalidEnvironment(prepared);
        }
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public void reset() {
        this.environment = null;
    }
}
