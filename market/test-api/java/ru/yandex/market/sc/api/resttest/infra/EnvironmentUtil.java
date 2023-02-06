package ru.yandex.market.sc.api.resttest.infra;

import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

import ru.yandex.devtools.test.Params;

/**
 * @author valter
 */
@Slf4j
public class EnvironmentUtil {

    private static final String REST_ENVIRONMENT_PROPERTY = "REST_ENVIRONMENT";
    private static final String DEFAULT_REST_ENVIRONMENT_PROPERTY = "testing";

    private static String getEnvironment() {
        return getSystemProperty(REST_ENVIRONMENT_PROPERTY).orElse(DEFAULT_REST_ENVIRONMENT_PROPERTY);
    }

    public static void initializeEnvironment(Map<String, Runnable> initializers) {
        String environment = EnvironmentUtil.getEnvironment();
        var initializer = initializers.get(environment);
        if (initializer == null) {
            throw new IllegalStateException("Unknown environment " + environment);
        }
        log.info("Initializing environment " + environment);
        initializer.run();
    }

    @SuppressWarnings("SameParameterValue")
    private static Optional<String> getSystemProperty(String propertyName) {
        return Optional.ofNullable(System.getProperty(propertyName))
                .or(() -> Optional.ofNullable(Params.params.get(propertyName)));
    }

}
