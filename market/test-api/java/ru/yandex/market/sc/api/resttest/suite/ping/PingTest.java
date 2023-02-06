package ru.yandex.market.sc.api.resttest.suite.ping;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sc.api.resttest.infra.RestTest;

import static io.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;

/**
 * @author valter
 */
@RestTest
public class PingTest {

    @Test
    @DisplayName("Проверка живости сервера")
    void ping() {
        when()
                .get("/ping")
                .then()
                .statusCode(200)
                .body(containsString("0;ok"));
    }

}
