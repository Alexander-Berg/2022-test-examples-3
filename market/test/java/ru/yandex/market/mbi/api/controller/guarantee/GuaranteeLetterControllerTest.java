package ru.yandex.market.mbi.api.controller.guarantee;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.guarantee.GuaranteeLetterInfoDTO;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * Тесты на {@link ru.yandex.market.mbi.api.controller.GuaranteeLetterController}.
 */
@DbUnitDataSet(before = "GuaranteeLetterControllerTest.before.csv")
public class GuaranteeLetterControllerTest extends FunctionalTest {

    private static final String BASE_URL = "mbi.mds.s3.bucket.market.mds.s3.path";

    @Autowired
    private MdsS3Client mdsS3Client;

    @Test
    @DisplayName("Тест на получение информации о ГП")
    void testGetInfo() throws MalformedURLException {
        Mockito.doReturn(new URL("https", BASE_URL, "/guarantee-letter/10/guarantee-letter-10.pdf"))
                .when(mdsS3Client).getUrl(any());
        mbiApiClient.getGuaranteeLetterInfo(10);
        Assertions.assertEquals(
                new GuaranteeLetterInfoDTO(
                        10,
                        "our-shiny-guarantee-10.pdf",
                        LocalDate.of(2019, Month.JANUARY, 8).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                        "https://mbi.mds.s3.bucket.market.mds.s3.path/guarantee-letter/10/guarantee-letter-10.pdf"),
                mbiApiClient.getGuaranteeLetterInfo(10));
    }


    @Test
    @DisplayName("Тест на ошибку при попытке получить несуществующее ГП")
    void testGetInfoNotFound() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.getGuaranteeLetterInfo(11)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );
        MbiAsserts.assertXmlEquals(
                "<error>\n" +
                        "    <message>Guarantee letter not found for partnerId: 11</message>\n" +
                        "</error>",
                exception.getResponseBodyAsString()
        );
    }
}
