package ru.yandex.direct.testing.config;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.env.EnvironmentType;

import static java.util.stream.Collectors.toList;

/**
 * Тест стоит подключать как Suite во все приложения.
 * Предохраняет от забывчивости - когда настройку не прописывают в production-конфиг
 */
@RunWith(Parameterized.class)
@SuppressWarnings("checkstyle:visibilitymodifier") // for .env
public class ProductionConfigContainsAllSettings {
    @Parameterized.Parameter
    public EnvironmentType env;

    public static final String[] OPTIONAL_PREFIXES = {
            "jetty.keystore.",
            "jetty.ssl_port",
            "tvm_api_auth.",
    };

    private static DirectConfig productionConfig;

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> allNonProductionEnvTypes() {
        // в development/db_testing могут быть отличающиеся настройки
        return Stream.of(
                EnvironmentType.DEVTEST,
                EnvironmentType.DEVELOPMENT,
                EnvironmentType.DEV7,
                EnvironmentType.TESTING,
                EnvironmentType.TESTING2,
                EnvironmentType.PRESTABLE,
                EnvironmentType.SANDBOX,
                EnvironmentType.SANDBOX_DEVELOPMENT,
                EnvironmentType.SANDBOX_TESTING
        ).map(e -> new Object[]{e})
                .collect(toList());
    }

    @BeforeClass
    public static void init() {
        productionConfig = DirectConfigFactory.getConfigWithoutSystem(EnvironmentType.PRODUCTION);
    }

    @Test
    public void allSettingsExistsInProduction() {
        DirectConfig config = DirectConfigFactory.getConfigWithoutSystem(env);

        SoftAssertions soft = new SoftAssertions();

        config.entrySet().stream()
                .map(Map.Entry::getKey)
                .filter(k -> !this.isOptional(k))
                .forEach(k -> {
                    soft.assertThat(productionConfig.hasPath(k))
                            .as("Path %s exists in production", k)
                            .isTrue();
                });

        soft.assertAll();
    }

    private boolean isOptional(String key) {
        for (String prefix : OPTIONAL_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
