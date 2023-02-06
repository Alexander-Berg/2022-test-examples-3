package ru.yandex.market.ff4shops.pagematch;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.util.FF4ShopsUrlBuilder;

/**
 * Тесты для {@link PagematchController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class PagematchControllerTest extends FunctionalTest {

    @Test
    void testPagematch() {
        final ResponseEntity<String> responseEntity =
                FunctionalTestHelper.get(FF4ShopsUrlBuilder.getPagematchUrl(randomServerPort), String.class);

        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        final String body = responseEntity.getBody();
        final List<String> split = Arrays.asList(StringUtils.split(body, "\n"));

        MatcherAssert.assertThat(split, Matchers.hasItems(
                "getDebugStock\t/getDebugStock\tff4shops",
                "jobStatus\t/jobStatus\tff4shops",
                "ping\t/ping\tff4shops",
                "reference_serviceId_getReferenceItems\t/reference/<serviceId>/getReferenceItems\tff4shops",
                "reference_serviceId_getStocks\t/reference/<serviceId>/getStocks\tff4shops",
                "solomon\t/solomon\tff4shops",
                "solomon_jvm\t/solomon-jvm\tff4shops",
                "stocks_debug_status\t/stocks/debug/status\tff4shops"
        ));
    }
}


