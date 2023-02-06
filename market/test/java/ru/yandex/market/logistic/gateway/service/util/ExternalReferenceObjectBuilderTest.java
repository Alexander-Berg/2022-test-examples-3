package ru.yandex.market.logistic.gateway.service.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.service.converter.util.ExternalReferenceObjectBuilder;
import ru.yandex.market.logistics.front.library.dto.ExternalReferenceObject;

import static ru.yandex.market.logistic.gateway.service.converter.util.YqlQueryTemplate.CREATION_DAY_TEMPLATE;
import static ru.yandex.market.logistic.gateway.service.converter.util.YqlQueryTemplate.DB_DECLARATION;
import static ru.yandex.market.logistic.gateway.service.converter.util.YqlQueryTemplate.FOR_24H_QUERY;
import static ru.yandex.market.logistic.gateway.service.converter.util.YqlQueryTemplate.FOR_LAST_DAY_QUERY;
import static ru.yandex.market.logistic.gateway.service.converter.util.YqlQueryTemplate.TASK_ID_TEMPLATE;
import static ru.yandex.market.logistic.gateway.service.converter.util.YqlQueryTemplate.YQL_URL;

public class ExternalReferenceObjectBuilderTest extends BaseTest {

    private static final String DISPLAY_NAME = "some text";
    private static final Long TASK_ID = 0L;
    private static final String YQL_QUERY_URL = YQL_URL + String.join(
            System.lineSeparator(),
            DB_DECLARATION,
            String.format(TASK_ID_TEMPLATE, TASK_ID)
    );
    private static final String CORRECT_24H_QUERY = String.join(System.lineSeparator(), YQL_QUERY_URL, FOR_24H_QUERY);
    private static final String CORRECT_LAST_DAY_QUERY =
        String.join(System.lineSeparator(), YQL_QUERY_URL, CREATION_DAY_TEMPLATE, FOR_LAST_DAY_QUERY);

    @Test
    public void testCorrect24HQuery() {
        LocalDateTime now = LocalDateTime.now();
        ExternalReferenceObject actual = new ExternalReferenceObjectBuilder(
            DISPLAY_NAME,
            TASK_ID,
            now,
            true
        ).build();

        assertions.assertThat(actual.getDisplayName()).isEqualTo(DISPLAY_NAME);
        assertions.assertThat(decodeUrl(actual.getUrl())).isEqualTo(CORRECT_24H_QUERY);
        assertions.assertThat(actual.isOpenNewTab()).isEqualTo(true);
    }

    @Test
    public void testCorrectLastDayQuery() {
        LocalDateTime prevDay = LocalDateTime.now().minusHours(24);
        ExternalReferenceObject expected = new ExternalReferenceObjectBuilder(
            DISPLAY_NAME,
            TASK_ID,
            prevDay,
            true
        ).build();

        assertions.assertThat(expected.getDisplayName()).isEqualTo(DISPLAY_NAME);
        assertions.assertThat(decodeUrl(expected.getUrl()))
            .isEqualTo(String.format(CORRECT_LAST_DAY_QUERY, prevDay.toLocalDate()));
        assertions.assertThat(expected.isOpenNewTab()).isEqualTo(true);
    }

    @Test
    public void testEdgeTimeQueryCreation() throws Exception {
        LocalDateTime edgeTime = LocalDateTime.now().minusHours(24).plusSeconds(1);
        ExternalReferenceObject objWith24hQuery = new ExternalReferenceObjectBuilder(
            DISPLAY_NAME,
            TASK_ID,
            edgeTime,
            true
        ).build();

        assertions.assertThat(decodeUrl(objWith24hQuery.getUrl())).isEqualTo(CORRECT_24H_QUERY);

        new CountDownLatch(1).await(1L, TimeUnit.SECONDS);

        ExternalReferenceObject objWithLastDayQuery = new ExternalReferenceObjectBuilder(
            DISPLAY_NAME,
            TASK_ID,
            edgeTime,
            true
        ).build();

        assertions.assertThat(decodeUrl(objWithLastDayQuery.getUrl()))
            .isEqualTo(String.format(CORRECT_LAST_DAY_QUERY, edgeTime.toLocalDate()));
    }

    @Test
    public void testBuilderParameters() {
        ExternalReferenceObject expected = new ExternalReferenceObjectBuilder(
            null,
            null,
            null,
            false
        ).build();

        assertions.assertThat(expected.getDisplayName()).isNull();
        assertions.assertThat(decodeUrl(expected.getUrl())).isEqualTo(CORRECT_24H_QUERY);
        assertions.assertThat(expected.isOpenNewTab()).isEqualTo(false);
    }

    private String decodeUrl(String url) {
        return URLDecoder.decode(url, StandardCharsets.UTF_8);
    }

}
