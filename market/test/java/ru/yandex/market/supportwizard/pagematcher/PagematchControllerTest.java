package ru.yandex.market.supportwizard.pagematcher;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.supportwizard.config.BaseFunctionalTest;

public class PagematchControllerTest extends BaseFunctionalTest {

    @Autowired
    private TestRestTemplate testRestTemplate;


    @Test
    void testPagematch() {
        final ResponseEntity<String> responseEntity = testRestTemplate.getForEntity("/pagematch", String.class);

        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        final String body = responseEntity.getBody();
        final List<String> split = Arrays.asList(Objects.requireNonNull(StringUtils.split(body, "\n")));

        MatcherAssert.assertThat(split, Matchers.hasItems(
                "ping\t/ping\tsupport-wizard",
                "close\t/close\tsupport-wizard",
                "error\t/error\tsupport-wizard",
                "monitoring\t/monitoring\tsupport-wizard",
                "open\t/open\tsupport-wizard",
                "pagematch\t/pagematch\tsupport-wizard"
        ));
    }

}

