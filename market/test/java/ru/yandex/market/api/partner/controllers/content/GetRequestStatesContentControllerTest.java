package ru.yandex.market.api.partner.controllers.content;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.ir.http.MboRobot;
import ru.yandex.market.ir.http.PartnerContentApi;
import ru.yandex.market.ir.http.PartnerContentService;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.mbi.util.io.MbiFiles;

/**
 * Тесты для {@link ContentController}.
 */
@ParametersAreNonnullByDefault
class GetRequestStatesContentControllerTest extends FunctionalTest {
    private static final int SOURCE_ID = 23407453;
    private static final long CAMPAIGN_ID = 1000571241;
    private static final PartnerContentApi.GetSourceTicketsResponse GET_SOURCE_TICKETS_RESPONSE =
            createGetSourceTicketsResponse();
    private static final String PAGE_TOKEN = "eyJvcCI6Ij4iLCJrZXkiOjI2MzQ4NzI2fQ";
    private static final int OFFSET_FOR_PAGE_TOKEN = 26348726;
    private static final PartnerContentApi.GetSourceTicketsResponse GET_SOURCE_TICKETS_RESPONSE_WITH_NEXT_PAGE =
            createGetSourceTicketsResponseWithNextPage();
    @Autowired
    private PartnerContentService marketProtoPartnerContentService;

    static List<Arguments> testGetRequestStatesWithStatusFilterArguments() {
        return Arrays.asList(
                Arguments.of("OK", PartnerContentApi.TicketStatus.SUCCESS),
                Arguments.of("ERROR", PartnerContentApi.TicketStatus.ERROR),
                Arguments.of("PROCESSING", PartnerContentApi.TicketStatus.PROCESSING)
        );
    }

    @Nonnull
    private static PartnerContentApi.GetSourceTicketsResponse createGetSourceTicketsResponse() {
        return PartnerContentApi.GetSourceTicketsResponse.newBuilder()
                .addTicketInfo(PartnerContentApi.TicketInfo.newBuilder()
                        .setTicketId(12341254)
                        .setTicketStatus(PartnerContentApi.TicketStatus.ERROR)
                        .build())
                .addTicketInfo(PartnerContentApi.TicketInfo.newBuilder()
                        .setTicketId(94857359)
                        .setTicketStatus(PartnerContentApi.TicketStatus.SUCCESS)
                        .build())
                .addTicketInfo(PartnerContentApi.TicketInfo.newBuilder()
                        .setTicketId(39450304)
                        .setTicketStatus(PartnerContentApi.TicketStatus.PROCESSING)
                        .build())
                .build();
    }

    private static PartnerContentApi.GetSourceTicketsResponse createGetSourceTicketsResponseWithNextPage() {
        return PartnerContentApi.GetSourceTicketsResponse.newBuilder(GET_SOURCE_TICKETS_RESPONSE)
                .setNextOffsetToken(OFFSET_FOR_PAGE_TOKEN)
                .build();
    }

    @Test
    void testGetRequestStatesJson() throws IOException {
        Mockito.when(marketProtoPartnerContentService.getSourceTickets(Mockito.any()))
                .thenReturn(GET_SOURCE_TICKETS_RESPONSE);
        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(SOURCE_ID)
                        .build());
        String url = String.format("%s/campaigns/%d/models/requests", urlBasePrefix, CAMPAIGN_ID);
        String expectedResponse = MbiFiles.readText(
                () -> this.getClass()
                        .getResourceAsStream(this.getClass().getSimpleName() + ".testGetRequestStates.json"),
                StandardCharsets.UTF_8
        );
        MatcherAssert.assertThat(
                FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.JSON),
                MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonEquals(expectedResponse))
        );
        Mockito.verify(marketProtoPartnerContentService)
                .getSourceTickets(ArgumentMatchers.argThat(
                        r -> r.getSourceId() == SOURCE_ID && !r.hasStatus() && r.getLimit() == 100
                                && !r.hasOffsetToken() && !r.hasShopSku()
                ));
    }

    @Test
    void testGetRequestStatesXml() throws IOException {
        Mockito.when(marketProtoPartnerContentService.getSourceTickets(Mockito.any()))
                .thenReturn(GET_SOURCE_TICKETS_RESPONSE);
        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(SOURCE_ID)
                        .build());
        String url = String.format("%s/campaigns/%d/models/requests", urlBasePrefix, CAMPAIGN_ID);
        String expectedResponse = MbiFiles.readText(
                () -> this.getClass()
                        .getResourceAsStream(this.getClass().getSimpleName() + ".testGetRequestStates.xml"),
                StandardCharsets.UTF_8
        );
        MatcherAssert.assertThat(
                FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.XML),
                MoreMbiMatchers.responseBodyMatches(MbiMatchers.xmlEquals(expectedResponse))
        );
        Mockito.verify(marketProtoPartnerContentService)
                .getSourceTickets(ArgumentMatchers.argThat(
                        r -> r.getSourceId() == SOURCE_ID && !r.hasStatus() && r.getLimit() == 100
                                && !r.hasOffsetToken() && !r.hasShopSku()
                ));
    }

    @Test
    void testGetRequestStatesWithNextPageJson() throws IOException {
        Mockito.when(marketProtoPartnerContentService.getSourceTickets(Mockito.any()))
                .thenReturn(GET_SOURCE_TICKETS_RESPONSE_WITH_NEXT_PAGE);
        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(SOURCE_ID)
                        .build());
        String url = String.format("%s/campaigns/%d/models/requests", urlBasePrefix, CAMPAIGN_ID);
        String expectedResponse = MbiFiles.readText(
                () -> this.getClass()
                        .getResourceAsStream(this.getClass().getSimpleName() + ".testGetRequestStatesWithNextPage.json"),
                StandardCharsets.UTF_8
        );
        MatcherAssert.assertThat(
                FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.JSON),
                MoreMbiMatchers.responseBodyMatches(MbiMatchers.jsonEquals(expectedResponse))
        );
        Mockito.verify(marketProtoPartnerContentService)
                .getSourceTickets(ArgumentMatchers.argThat(
                        r -> r.getSourceId() == SOURCE_ID && !r.hasStatus() && r.getLimit() == 100
                                && !r.hasOffsetToken() && !r.hasShopSku()
                ));
    }

    @Test
    void testGetRequestStatesWithNextPageXml() throws IOException {
        Mockito.when(marketProtoPartnerContentService.getSourceTickets(Mockito.any()))
                .thenReturn(GET_SOURCE_TICKETS_RESPONSE_WITH_NEXT_PAGE);
        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(SOURCE_ID)
                        .build());
        String url = String.format("%s/campaigns/%d/models/requests", urlBasePrefix, CAMPAIGN_ID);
        String expectedResponse = MbiFiles.readText(
                () -> this.getClass()
                        .getResourceAsStream(this.getClass().getSimpleName() + ".testGetRequestStatesWithNextPage.xml"),
                StandardCharsets.UTF_8
        );
        MatcherAssert.assertThat(
                FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.XML),
                MoreMbiMatchers.responseBodyMatches(MbiMatchers.xmlEquals(expectedResponse))
        );
        Mockito.verify(marketProtoPartnerContentService)
                .getSourceTickets(ArgumentMatchers.argThat(
                        r -> r.getSourceId() == SOURCE_ID && !r.hasStatus() && r.getLimit() == 100
                                && !r.hasOffsetToken() && !r.hasShopSku()
                ));
    }

    @Test
    void testGetRequestStatesWithPageToken() {
        Mockito.when(marketProtoPartnerContentService.getSourceTickets(Mockito.any()))
                .thenReturn(GET_SOURCE_TICKETS_RESPONSE);
        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(SOURCE_ID)
                        .build());
        String url = String.format("%s/campaigns/%d/models/requests?page_token=%s",
                urlBasePrefix, CAMPAIGN_ID, PAGE_TOKEN);
        FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.JSON);
        Mockito.verify(marketProtoPartnerContentService)
                .getSourceTickets(ArgumentMatchers.argThat(
                        r -> r.getSourceId() == SOURCE_ID && !r.hasStatus() && r.getLimit() == 100
                                && r.hasOffsetToken() && r.getOffsetToken() == OFFSET_FOR_PAGE_TOKEN && !r.hasShopSku()
                ));
    }

    @ValueSource(ints = {1, 11, 123, 345, 998, 999})
    @ParameterizedTest
    void testGetRequestStatesWithCustomLimit(int limit) {
        Mockito.when(marketProtoPartnerContentService.getSourceTickets(Mockito.any()))
                .thenReturn(GET_SOURCE_TICKETS_RESPONSE);
        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(SOURCE_ID)
                        .build());
        String url = String.format("%s/campaigns/%d/models/requests?limit=%d",
                urlBasePrefix, CAMPAIGN_ID, limit);
        FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.JSON);
        Mockito.verify(marketProtoPartnerContentService)
                .getSourceTickets(ArgumentMatchers.argThat(
                        r -> r.getSourceId() == SOURCE_ID && !r.hasStatus() && r.getLimit() == limit
                                && !r.hasOffsetToken() && !r.hasShopSku()
                ));
    }

    @ValueSource(ints = {-10, -1})
    @ParameterizedTest
    void testGetRequestStatesWithNonPositiveLimit(int limit) {
        Mockito.when(marketProtoPartnerContentService.getSourceTickets(Mockito.any()))
                .thenReturn(GET_SOURCE_TICKETS_RESPONSE);
        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(SOURCE_ID)
                        .build());
        String url = String.format("%s/campaigns/%d/models/requests?limit=%d",
                urlBasePrefix, CAMPAIGN_ID, limit);
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.XML)
        );
        //language=xml
        String expected = "" +
                "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code='NON_POSITIVE_LIMIT' message='non positive limit'/> " +
                "  </errors>" +
                "</response>";
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(MbiMatchers.xmlEquals(expected))
                )
        );
    }

    @ValueSource(ints = {1001, 1002, 2000, 5000})
    @ParameterizedTest
    void testGetRequestStatesWithWrongLimit(int limit) {
        Mockito.when(marketProtoPartnerContentService.getSourceTickets(Mockito.any()))
                .thenReturn(GET_SOURCE_TICKETS_RESPONSE);
        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(SOURCE_ID)
                        .build());
        String url = String.format("%s/campaigns/%d/models/requests?limit=%d",
                urlBasePrefix, CAMPAIGN_ID, limit);
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.XML)
        );
        //language=xml
        String expected = "" +
                "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code='LIMIT_EXCEEDED' message='limit exceeded'/> " +
                "  </errors>" +
                "</response>";
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(MbiMatchers.xmlEquals(expected))
                )
        );
    }

    @MethodSource("testGetRequestStatesWithStatusFilterArguments")
    @ParameterizedTest
    void testGetRequestStatesWithStatusFilter(String status, PartnerContentApi.TicketStatus irStatus) {
        Mockito.when(marketProtoPartnerContentService.getSourceTickets(Mockito.any()))
                .thenReturn(GET_SOURCE_TICKETS_RESPONSE);
        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(SOURCE_ID)
                        .build());
        String url = String.format("%s/campaigns/%d/models/requests?status=%s",
                urlBasePrefix, CAMPAIGN_ID, status);
        FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.JSON);
        Mockito.verify(marketProtoPartnerContentService)
                .getSourceTickets(ArgumentMatchers.argThat(
                        r -> r.getSourceId() == SOURCE_ID && r.hasStatus() && r.getStatus() == irStatus
                                && r.getLimit() == 100 && !r.hasOffsetToken() && !r.hasShopSku()
                ));
    }

    @Test
    void testGetRequestStatesWithWrongStatusFilter() {
        Mockito.when(marketProtoPartnerContentService.getSourceTickets(Mockito.any()))
                .thenReturn(GET_SOURCE_TICKETS_RESPONSE);
        Mockito.when(marketProtoPartnerContentService.addSource(Mockito.any()))
                .thenReturn(MboRobot.AddSourceResponse.newBuilder()
                        .setSourceId(SOURCE_ID)
                        .build());
        String url = String.format("%s/campaigns/%d/models/requests?status=%s",
                urlBasePrefix, CAMPAIGN_ID, "DSFDSF");
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(url, HttpMethod.GET, Format.XML)
        );
        //language=xml
        String expected = "" +
                "<response>" +
                "  <status>ERROR</status>" +
                "  <errors>" +
                "    <error code='INVALID_QUERY_PARAMETER' message='status: DSFDSF'/> " +
                "  </errors>" +
                "</response>";
        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(MbiMatchers.xmlEquals(expected))
                )
        );
    }
}
