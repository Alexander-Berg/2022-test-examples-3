package ru.yandex.market.sc.api.resttest.suite.format;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sc.api.resttest.infra.RestTest;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

@RestTest
public class FormatUsersTest {

    @Test
    @DisplayName("GET /api/users/check")
    void usersCheck() {
        usersCheckCommon("/api/users/check");
    }

    @Test
    @DisplayName("GET /api/checkUser Deprecated")
    void usersCheckDeprecated() {
        usersCheckCommon("/api/checkUser");
    }

    private void usersCheckCommon(String path) {
        when()
                .get(path)
                .then()
                .statusCode(200)
                .rootPath("")
                .body(
                        "sortingCenterId", greaterThanOrEqualTo(0),
                        "sortingCenter", notNullValue()
                );
    }

}
