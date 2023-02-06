package ru.yandex.market.api.partner.controllers.feedback;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException.InternalServerError;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Функциональные тесты для {@link FeedbackController}.
 *
 * @author Vladislav Bauer
 */
@DbUnitDataSet(before = "FeedbackControllerFunctionalTest.before.csv")
class FeedbackControllerFunctionalTest extends FunctionalTest {

    private static final long SHOP_ID = 774;
    private static final long CAMPAIGN_ID = 10774;
    private static final int DEFAULT_LIMIT = 20;

    private static final List<Long> GRADE_IDS = Arrays.asList(
            59917888L, 59917890L, 59917892L, 59917918L, 59917929L,
            59917939L, 59918085L, 59918452L, 59918519L, 59918633L
    );

    @Autowired
    @Qualifier("persQaRestTemplate")
    private RestTemplate persQaRestTemplate;

    @Autowired
    @Qualifier("persGradeRestTemplate")
    private RestTemplate persGradeRestTemplate;

    @Autowired
    private PassportService passportService;

    @Value("${market.pers.qa.url}")
    private String persQaUrl;

    @Value("${market.pers.grade.url}")
    private String persGradeUrl;

    private MockRestServiceServer persGradeServer;
    private MockRestServiceServer persQaServer;
    private Integer limit;


    @BeforeEach
    void initMock() {
        persGradeServer = MockRestServiceServer.createServer(persGradeRestTemplate);
        persQaServer = MockRestServiceServer.createServer(persQaRestTemplate);
        limit = null; // Используем limit по умолчанию

        when(passportService.getPublicNames(anyCollection())).thenAnswer(i ->
                i.<Collection<Long>>getArgument(0).stream()
                        .collect(Collectors.toMap(Function.identity(), id -> "User " + id)));
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    @DisplayName("Проверить пустой ответ PAPI по отзывам и комментариям, когда PERS не вернули данные")
    void testEmptyResponse(final Format format) {
        mockPersGradeResponse("pers/pers-grade-empty.json");
        checkPapiResponse("papi-opinions-empty", format);
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    @DisplayName("Проверить ответ PAPI по отзывам и комментариям")
    void testDataResponse(final Format format) {
        mockPersGradeResponse("pers/pers-grade-data.json");
        mockPersQaResponse("pers/pers-qa-data.json", GRADE_IDS);
        checkPapiResponse("papi-opinions-data", format);
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    @DisplayName("Проверить ответ PAPI по отзывам и комментариям со специфичным лимитом")
    void testLimitedDataResponse(final Format format) {
        limit = 10; // Устанавливаем специфичный лимит на количество возвращаемых записей
        mockPersGradeResponse("pers/pers-grade-data.json");
        mockPersQaResponse("pers/pers-qa-data.json", GRADE_IDS);
        checkPapiResponse("papi-opinions-data", format);
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    @DisplayName("Проверить ответ 5xx в случае ошибки PERS Grade")
    void testPersGradeInternalError(final Format format) {
        persGradeServer.expect(requestTo(buildPersGradeURI()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThrows(InternalServerError.class, () -> sendPapiRequest(format));
    }

    @ParameterizedTest
    @EnumSource(Format.class)
    @DisplayName("Проверить ответ 5xx в случае ошибки PERS QA")
    void testPersQaInternalError(final Format format) {
        mockPersGradeResponse("pers/pers-grade-data.json");
        persQaServer.expect(requestTo(buildPersQaURI(GRADE_IDS)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThrows(InternalServerError.class, () -> sendPapiRequest(format));
    }


    private void checkPapiResponse(final String fileName, final Format format) {
        final ResponseEntity<String> response = sendPapiRequest(format);
        final String actualData = Preconditions.checkNotNull(response.getBody(), "HTTP body must be not empty");

        final String ext = format.toString().toLowerCase();
        final String path = String.format("%s/%s.%s", ext, fileName, ext);
        final String expected = getResourceAsString(path);

        if (format == Format.JSON) {
            JsonTestUtil.assertEquals(expected, actualData);
        } else if (format == Format.XML) {
            MbiAsserts.assertXmlEquals(expected, actualData);
        }
    }

    private ResponseEntity<String> sendPapiRequest(final Format format) {
        final URI uri = buildURI(CAMPAIGN_ID, null, null);
        return FunctionalTestHelper.makeRequest(uri, HttpMethod.GET, format);
    }

    private void mockPersGradeResponse(final String fileName) {
        persGradeServer.expect(requestTo(buildPersGradeURI()))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(getResourceAsString(fileName), MediaType.APPLICATION_JSON));
    }

    private void mockPersQaResponse(final String fileName, final List<Long> gradeIds) {
        persQaServer.expect(requestTo(buildPersQaURI(gradeIds)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(getResourceAsString(fileName), MediaType.APPLICATION_JSON));
    }

    private URI buildURI(final long campaignId, final String pageToken, final String fromDate) {
        final String baseUri = urlBasePrefix + "/campaigns/{campaignId}/feedback/updates";
        final UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUri)
                .queryParam("page_token", pageToken)
                .queryParam("from_date", fromDate);

        if (limit != null) {
            uriBuilder.queryParam("limit", limit);
        }
        return uriBuilder.buildAndExpand(campaignId).toUri();
    }

    private String buildPersGradeURI() {
        return String.format(
                "%s/api/grade/papi/shop/%d?lastTimestamp&lastGradeId&page_size=%d",
                persGradeUrl, SHOP_ID, Objects.requireNonNullElse(limit, DEFAULT_LIMIT)
        );
    }

    private String buildPersQaURI(final List<Long> gradeIds) {
        return String.format("%s/partner/api/%d/grade/comments?%s", persQaUrl, SHOP_ID, gradeIds.stream()
                .map(id -> "gradeId=" + id)
                .collect(Collectors.joining("&")));
    }

    private String getResourceAsString(final String fileName) {
        return StringTestUtil.getString(getClass(), fileName);
    }

}
