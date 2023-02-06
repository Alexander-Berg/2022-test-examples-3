package ru.yandex.market.sc.api.resttest.infra;

import java.time.Clock;
import java.util.Map;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * @author valter
 */
public class RestTestExtension implements BeforeAllCallback {

    private static final Map<String, Runnable> ENVIRONMENT_INITIALIZERS = Map.of(
            "testing", () -> RestTestContext.set(new RestTestContext.Values(
                    Clock.systemDefaultZone(), 1L, 1123533710L,
                    "Ag" + "AAAABC98OOAAY7wFe8LwvBh00cv8o9tIszIUA"
            ))
    );

    @Override
    public void beforeAll(ExtensionContext context) {
        EnvironmentUtil.initializeEnvironment(ENVIRONMENT_INITIALIZERS);
    }

}
