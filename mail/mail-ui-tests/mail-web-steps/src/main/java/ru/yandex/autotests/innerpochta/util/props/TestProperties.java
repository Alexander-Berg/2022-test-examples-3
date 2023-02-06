package ru.yandex.autotests.innerpochta.util.props;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.PropertyLoader;
import ru.yandex.autotests.passport.api.common.data.PassportEnv;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource.Classpath;

import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.EnumUtils.getEnum;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.upperCase;

/**
 * @author pavponn
 */
@Classpath("project.properties")
public class TestProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestProperties.class);

    private static TestProperties instance;

    public static TestProperties testProperties() {
        if (null == instance) {
            instance = new TestProperties();
        }
        return instance;
    }

    @Property("mailing.account")
    private String mailingAccount;

    @Property("filter.expression")
    private String filterExpression;

    public String getMailingAccount() {
        return mailingAccount;
    }

    public String getFilterExpression() {
        return filterExpression;
    }

    private TestProperties() {
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

    private static ThreadLocal<TestProperties> propsManager = new ThreadLocal<>();

}
