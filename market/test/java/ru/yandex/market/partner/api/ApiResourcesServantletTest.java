package ru.yandex.market.partner.api;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.gson.JsonElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

class ApiResourcesServantletTest extends FunctionalTest {

    @Test
    void getApiResources() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "/apiResources?_user_id=79668854&format=json&type=CPA&type=STOCKS");
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        var patterns = getPatterns(response);
        assertThat(patterns, hasItems(
                "/campaigns/*/orders",
                "/campaigns/*/offers/stocks"
        ));
        assertThat(patterns, is(not(hasItems(
                "/models"
        ))));
    }

    @Test
    void getApiStockResources() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "/apiResources?_user_id=79668854&format=json&type=STOCKS");
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        var patterns = getPatterns(response);
        assertThat(patterns, hasItems(
                "/campaigns/*/offers/stocks"
        ));
        assertThat(patterns, is(not(hasItems(
                "/campaigns/*/orders",
                "/models"
        ))));
    }

    @Test
    void getApiResourcesNoTypeFilter() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "/apiResources?_user_id=79668854&format=json");
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        assertThat(getPatterns(response), hasItems(
                "/campaigns/*/offers/stocks",
                "/campaigns/*/orders",
                "/models"
        ));
    }

    List<String> getPatterns(ResponseEntity<String> response) {
        return StreamSupport.stream(JsonTestUtil.parseJson(response.getBody())
                .getAsJsonObject().get("result").getAsJsonArray().get(0).getAsJsonArray().spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .map(o -> o.get("pattern"))
                .map(JsonElement::getAsString)
                .collect(Collectors.toList());
    }
}
