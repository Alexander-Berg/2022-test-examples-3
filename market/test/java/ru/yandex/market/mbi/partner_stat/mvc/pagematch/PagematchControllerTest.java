package ru.yandex.market.mbi.partner_stat.mvc.pagematch;

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
import ru.yandex.market.mbi.partner_stat.FunctionalTest;

/**
 * Тесты для {@link PagematchController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class PagematchControllerTest extends FunctionalTest {

    @Test
    void testPagematch() {
        final ResponseEntity<String> responseEntity = FunctionalTestHelper.get(baseUrl() + "/pagematch", String.class);

        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        final String body = responseEntity.getBody();
        final List<String> split = Arrays.asList(StringUtils.split(body, "\n"));

        MatcherAssert.assertThat(split, Matchers.hasItems(
                "ping\t/ping\tmbi-partner-stat",
                "testResponse_fail_excel\t/testResponse/<fail>/excel\tmbi-partner-stat"
        ));
    }
}
