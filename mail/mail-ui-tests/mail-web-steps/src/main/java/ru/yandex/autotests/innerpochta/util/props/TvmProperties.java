package ru.yandex.autotests.innerpochta.util.props;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.PropertyLoader;
import ru.yandex.autotests.passport.api.common.data.PassportEnv;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource.Classpath;
import ru.yandex.qatools.secrets.SecretWithKey;
import ru.yandex.qatools.secrets.SecretsLoader;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.EnumUtils.getEnum;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.upperCase;

/**
 * @author eremin-n-s
 */
@Classpath("project.properties")
public class TvmProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(TvmProperties.class);
    private static final int MAX_ATTEMPTS_TO_LOAD_SECRETS = 5;

    @Property("tvm.client.secret")
    @SecretWithKey(secret = "sec-01fbs0ksr6pvnhcj1dfvm98ekb", key = "tvm.client.secret")
    private String tvmClientSecret;

    @Property("tvm.client.id")
    @SecretWithKey(secret = "sec-01fbs0ksr6pvnhcj1dfvm98ekb", key = "tvm.client.id")
    private String tvmClientId;

    public String getTvmClientSecret() { return reloadSecret(() -> tvmClientSecret); }

    public int getTvmClientId() {  return Integer.parseInt(reloadSecret(() -> tvmClientId)); }

    private TvmProperties() {
        java.util.Properties defaults = new java.util.Properties();
        PropertyLoader.newInstance()
                .withDefaults(defaults)
                .register(scope ->
                        ofNullable(getEnum(PassportEnv.class, upperCase(scope)))
                                .orElseGet(() -> {
                                    LOGGER.warn("Wrong env! Use one of: {}", asList(PassportEnv.values()));
                                    return null;
                                }), PassportEnv.class)
                .register(from -> Stream.of(YandexDomain.values())
                        .filter(ydomain -> contains(from, "yandex." + ydomain.getDomain()))
                        .findFirst().orElse(YandexDomain.RU), YandexDomain.class)
                .populate(this);
    }

    private static ThreadLocal<TvmProperties> propsManager = new ThreadLocal<>();

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

    public static TvmProperties tvmProps() {
        if (propsManager.get() == null) {
            propsManager.set(new TvmProperties());
        }
        return propsManager.get();
    }

}
