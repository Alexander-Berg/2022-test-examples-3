package ru.yandex.market.mbi.logprocessor.pagematch;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.mbi.logprocessor.FunctionalTest;

public class PagematchControllerTest extends FunctionalTest {
    @Test
    void testPagematch() {
        final ResponseEntity<String> responseEntity = testRestTemplate.getForEntity("/pagematch", String.class);

        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        final String body = responseEntity.getBody();
        final List<String> split = Arrays.asList(Objects.requireNonNull(StringUtils.split(body, "\n")));

        MatcherAssert.assertThat(split, Matchers.hasItems(
                "ping\t/ping\tmbi-log-processor",
                "close\t/close\tmbi-log-processor",
                "error\t/error\tmbi-log-processor",
                "monitoring\t/monitoring\tmbi-log-processor",
                "open\t/open\tmbi-log-processor",
                "pagematch\t/pagematch\tmbi-log-processor",
                "ping\t/ping\tmbi-log-processor"
        ));
    }
}
