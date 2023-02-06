package ru.yandex.market.partner.mvc.controller.fulfillment;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.mds.s3.client.model.ResourceListing;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Тест контроллера {@link StatisticsReportController}.
 */
@DbUnitDataSet(before = "StatisticsReportControllerTest.before.csv")
class StatisticsReportControllerTest extends FunctionalTest {
    @Autowired
    private MdsS3Client mdsS3Client;

    @BeforeEach
    void setUp() throws MalformedURLException {
        doReturn(ResourceListing.create("bucketName",
                Collections.singletonList("key"),
                Arrays.asList("statistics-reports/1000/2018-01/", "statistics-reports/1000/2018-02/")))
                .when(mdsS3Client).list(any(), eq(false));
        doReturn(new URL("http", "localhost", "/report/100/2018/report.xlsx")).when(mdsS3Client).getUrl(any());
    }

    @Test
    void testGetMonths() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/statistics-report/months?id=100");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result",
                "[\"2018-01\", \"2018-02\"]")));

        verify(mdsS3Client).list(any(), eq(false));
        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    void testGetMonthExceptCurrent() {
        YearMonth now = YearMonth.now();
        YearMonth prevNow = YearMonth.now().minusMonths(1);
        YearMonth prevPrevNow = YearMonth.now().minusMonths(2);

        doReturn(ResourceListing.create("bucketName",
                Collections.singletonList("key"),
                Stream.of(prevPrevNow, prevNow, now)
                        .map(YearMonth::toString)
                        .map(yearMonth -> "statistics-reports/1000/" + yearMonth + "/")
                        .collect(Collectors.toList())))
                .when(mdsS3Client).list(any(), eq(false));
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/statistics-report/months?id=100");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result",
                "[\"" + prevPrevNow + "\", \"" + prevNow + "\"]")));
    }

    @Test
    void testGetUrl() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/statistics-report/url?id=100&month=2018-01");
        assertThat(response, MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonPropertyEquals("result",
                "\"http://localhost/report/100/2018/report.xlsx\"")));
    }
}
