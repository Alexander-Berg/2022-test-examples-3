package ru.yandex.market.sc.api.resttest.infra;

import java.util.Map;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import ru.yandex.market.sc.core.resttest.infra.RestTestEnvironment;

/**
 * @author valter
 */
@Slf4j
public class RestAssuredExtension implements BeforeAllCallback, AfterAllCallback {

    private static final Map<String, Runnable> ENVIRONMENT_INITIALIZERS = Map.of(
            "testing", () -> initFromSettings(RestTestEnvironment.TESTING_SC_API)
    );

    @SneakyThrows
    private static void initFromSettings(RestTestEnvironment.Settings settings) {
        RestAssured.port = settings.getPort();
        RestAssured.baseURI = settings.getBaseUri();
        RestAssured.authentication = RestTestContext.get().getStockmanToken() == null
                ? RestAssured.DEFAULT_AUTH
                : httpBuilder -> httpBuilder.setHeaders(Map.of(
                "Authorization",
                "OAuth " + RestTestContext.get().getStockmanToken()
        ));
        RestAssured.filters(new AllureRestAssured());
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        EnvironmentUtil.initializeEnvironment(ENVIRONMENT_INITIALIZERS);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        RestAssured.reset();
    }


}
