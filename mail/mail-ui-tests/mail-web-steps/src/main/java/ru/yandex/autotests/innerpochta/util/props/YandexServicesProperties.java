package ru.yandex.autotests.innerpochta.util.props;

import ru.yandex.qatools.properties.annotations.Property;
import ru.yandex.qatools.secrets.SecretWithKey;
import ru.yandex.qatools.secrets.SecretsLoader;

import java.util.function.Supplier;

import static java.util.Objects.isNull;

/**
 * @author pavponn
 */
public class YandexServicesProperties {

    private static final int MAX_ATTEMPTS_TO_LOAD_SECRETS = 5;

    private static YandexServicesProperties yandexServicesProperties;

    @Property("startrek.token")
    @SecretWithKey(secret = "sec-01dgfkz7fardc6meyd19vj8w8c", key = "startrek.token")
    private String startrekToken;

    @Property("testpalm.token")
    @SecretWithKey(secret = "sec-01dgfkz7fardc6meyd19vj8w8c", key = "testpalm.token")
    private String testpalmToken;

    @Property("tus.token")
    @SecretWithKey(secret = "sec-01dgfkz7fardc6meyd19vj8w8c", key = "tus.token.pinkie")
    private String tusToken;

    @Property("stat.token")
    @SecretWithKey(secret = "sec-01dgfkz7fardc6meyd19vj8w8c", key = "stat.token")
    private String statToken;

    public static YandexServicesProperties yandexServicesProps() {
        if (yandexServicesProperties == null) {
            yandexServicesProperties = new YandexServicesProperties();
        }
        return yandexServicesProperties;
    }

    public String getStartrekToken() {
        return reloadSecret(() -> startrekToken);
    }

    public String getTestpalmToken() {
        return reloadSecret(() -> testpalmToken);
    }

    public String getTusToken() { return reloadSecret(() -> tusToken); }

    public String getStatToken() {
        return reloadSecret(() -> statToken);
    }

    private <T> T reloadSecret(Supplier<T> secretSupplier) {
        int loadAttempts = MAX_ATTEMPTS_TO_LOAD_SECRETS;
        T secretValue = secretSupplier.get();
        while (isNull(secretValue) && loadAttempts > 0) {
            SecretsLoader.populate(this);
            secretValue = secretSupplier.get();
            loadAttempts--;
        }
        if (isNull(secretValue)) {
            throw new RuntimeException("Failed to fetch secrets!");
        }
        return secretValue;
    }
}
