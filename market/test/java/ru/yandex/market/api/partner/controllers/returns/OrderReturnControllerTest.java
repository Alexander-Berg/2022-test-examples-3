package ru.yandex.market.api.partner.controllers.returns;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectReader;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.api.partner.client.orderservice.PapiOrderServiceClient;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.order.ResourceUtilitiesMixin;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.orderservice.client.model.ApiError;
import ru.yandex.market.orderservice.client.model.LogisticItemDTO;
import ru.yandex.market.orderservice.client.model.LogisticReturnStatusDTO;
import ru.yandex.market.orderservice.client.model.MerchantItemStatusDTO;
import ru.yandex.market.orderservice.client.model.OrderReturnRefundStatus;
import ru.yandex.market.orderservice.client.model.PagedReturnResponse;
import ru.yandex.market.orderservice.client.model.PagedReturnsResponse;
import ru.yandex.market.orderservice.client.model.PagerWithToken;
import ru.yandex.market.orderservice.client.model.RefundDTO;
import ru.yandex.market.orderservice.client.model.ReturnDTO;
import ru.yandex.market.orderservice.client.model.ReturnItemDecisionType;
import ru.yandex.market.orderservice.client.model.ReturnLineDTO;
import ru.yandex.market.orderservice.client.model.ReturnReasonType;
import ru.yandex.market.orderservice.client.model.ReturnSubreasonType;
import ru.yandex.market.orderservice.client.model.StockTypeDTO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.partner.config.PapiOrderServiceConfig.PapiOrderServiceClientConfig.PAPI_ORDER_SERVICE_MAPPER;
import static ru.yandex.market.core.util.DateTimes.MOSCOW_TIME_ZONE;
import static ru.yandex.market.mbi.util.MbiMatchers.jsonEquals;
import static ru.yandex.market.mbi.util.MbiMatchers.xmlEquals;

public class OrderReturnControllerTest extends FunctionalTest implements ResourceUtilitiesMixin {

    private static final ZoneId DEFAULT_ZONE_ID = TimeZone.getDefault().toZoneId();

    private static final long CAMPAIGN_ID = 10774;

    @Autowired
    private PapiOrderServiceClient papiOrderServiceClient;

    @BeforeAll
    static void beforeAll() {
        TimeZone.setDefault(TimeZone.getTimeZone(MOSCOW_TIME_ZONE));
    }

    @AfterAll
    static void afterAll() {
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_ZONE_ID));
    }

    @Test
    public void testXmlSimpleResult() {
        when(papiOrderServiceClient.listReturnsByPartner(any(), any(), any(), any(), any(), any()))
                .thenReturn(mockResponseFuture());

        String url = String.format("%s/campaigns/%s/returns",
                urlBasePrefix, CAMPAIGN_ID);
        ResponseEntity<String> response = doGetXml(url);

        MatcherAssert.assertThat(response.getBody(), xmlEquals(resourceAsString("simple_result.xml")));
    }

    @Test
    public void testParseOrderServiceJson() throws IOException {
        String json = resourceAsString("order-service-response.json");
        ObjectReader reader = PAPI_ORDER_SERVICE_MAPPER.readerFor(PagedReturnResponse.class);

        PagedReturnResponse resp = reader.readValue(json);
        assertThat(resp).isEqualTo(mockResponse());
    }

    @Test
    public void testJsonSimpleResult() {
        when(papiOrderServiceClient.listReturnsByPartner(any(), any(), any(), any(), any(), any()))
                .thenReturn(mockResponseFuture());

        String url = String.format("%s/campaigns/%s/returns",
                urlBasePrefix, CAMPAIGN_ID);
        ResponseEntity<String> response = doGetJson(url);

        MatcherAssert.assertThat(response.getBody(), jsonEquals(resourceAsString("simple_result.json")));
    }

    @Test
    public void testOrderServiceBadRequest() {
        when(papiOrderServiceClient.listReturnsByPartner(any(), any(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture((PagedReturnResponse) new PagedReturnResponse()
                        .errors(List.of(
                                new ApiError()
                                        .code(ApiError.CodeEnum.BAD_PARAM)
                                        .message("Wrong format of 'fromDate' parameter")
                        ))));

        String url = String.format("%s/campaigns/%s/returns",
                urlBasePrefix, CAMPAIGN_ID);

        var expectedResponse = resourceAsString("error_bad_request.json");
        assertThatExceptionOfType(HttpClientErrorException.BadRequest.class)
                .isThrownBy(() -> doGetJson(url))
                .satisfies(ex -> JsonTestUtil.assertEquals(expectedResponse, ex.getResponseBodyAsString()));
    }

    @Test
    public void testOrderServiceInternalError() {
        when(papiOrderServiceClient.listReturnsByPartner(any(), any(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture((PagedReturnResponse) new PagedReturnResponse()
                        .errors(List.of(
                                new ApiError()
                                        .code(ApiError.CodeEnum.INTERNAL_ERROR)
                                        .message("Exception in some class")
                        ))));

        String url = String.format("%s/campaigns/%s/returns",
                urlBasePrefix, CAMPAIGN_ID);

        var expectedResponse = resourceAsString("error_internal_error.json");
        assertThatExceptionOfType(HttpServerErrorException.InternalServerError.class)
                .isThrownBy(() -> doGetJson(url))
                .satisfies(ex -> JsonTestUtil.assertEquals(expectedResponse, ex.getResponseBodyAsString()));
    }

    @Test
    public void testResultWithNullLists() {
        when(papiOrderServiceClient.listReturnsByPartner(any(), any(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mockResponseWithNullLists()));

        String url = String.format("%s/campaigns/%s/returns",
                urlBasePrefix, CAMPAIGN_ID);

        assertThatCode(() -> doGetJson(url)).doesNotThrowAnyException();
    }

    @Test
    public void testResultWithNullReturnLines() {
        when(papiOrderServiceClient.listReturnsByPartner(any(), any(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mockResponseWithNullReturnLines()));

        String url = String.format("%s/campaigns/%s/returns",
                urlBasePrefix, CAMPAIGN_ID);

        assertThatCode(() -> doGetJson(url)).doesNotThrowAnyException();
    }

    private CompletableFuture<PagedReturnResponse> mockResponseFuture() {
        return CompletableFuture.completedFuture(mockResponse());
    }

    private PagedReturnResponse mockResponse() {
        var returnsResponse = new PagedReturnsResponse()
                .orderReturns(List.of(
                        new ReturnDTO()
                                .returnId(20L)
                                .orderId(200L)
                                .partnerOrderId("222-20")
                                .createdAt(OffsetDateTime.parse("2020-02-01T14:30:30+03:00"))
                                .updatedAt(OffsetDateTime.parse("2020-02-02T14:30:30+03:00"))
                                .returnStatus(OrderReturnRefundStatus.STARTED_BY_USER)
                                .logisticStatus(LogisticReturnStatusDTO.IN_TRANSIT)
                                .refundAmount(new BigDecimal(2000))
                                .returnLines(
                                        List.of(
                                                new ReturnLineDTO()
                                                        .shopSku("sku-20")
                                                        .marketSku(2000L)
                                                        .count(1L)
                                                        .refunds(List.of(
                                                                new RefundDTO()
                                                                        .count(1)
                                                                        .returnReasonType(ReturnReasonType.BAD_QUALITY)
                                                                        .returnSubreasonType(ReturnSubreasonType.USER_DID_NOT_LIKE)
                                                                        .returnReason("Не подошел размер. Большая")
                                                                        .partnerCompensation(new BigDecimal(100))
                                                                        .refundAmount(new BigDecimal(1000))
                                                        ))
                                                        .logisticItems(List.of())
                                        )
                                ),
                        new ReturnDTO()
                                .returnId(21L)
                                .orderId(210L)
                                .partnerOrderId("222-21")
                                .createdAt(OffsetDateTime.parse("2020-02-03T14:30:30+03:00"))
                                .updatedAt(OffsetDateTime.parse("2020-02-04T14:30:30+03:00"))
                                .returnStatus(OrderReturnRefundStatus.REFUNDED)
                                .logisticStatus(LogisticReturnStatusDTO.READY_FOR_PICKUP)
                                .refundAmount(new BigDecimal(3000))
                                .returnLines(List.of(
                                        new ReturnLineDTO()
                                                .shopSku("sku-21-0")
                                                .marketSku(2100L)
                                                .count(3L)
                                                .refunds(List.of(
                                                                new RefundDTO()
                                                                        .count(1)
                                                                        .returnReasonType(ReturnReasonType.BAD_QUALITY)
                                                                        .returnSubreasonType(ReturnSubreasonType.USER_DID_NOT_LIKE)
                                                                        .returnReason("Не подошел размер. Большая")
                                                                        .decisionType(ReturnItemDecisionType.REFUND_MONEY)
                                                                        .partnerCompensation(new BigDecimal(100))
                                                                        .refundAmount(new BigDecimal(1000)),
                                                                new RefundDTO()
                                                                        .count(2)
                                                                        .returnReasonType(ReturnReasonType.WRONG_ITEM)
                                                                        .returnSubreasonType(ReturnSubreasonType.WRONG_ITEM)
                                                                        .returnReason("Не подошел размер. Большая")
                                                                        .decisionType(ReturnItemDecisionType.OTHER_DECISION)
                                                                        .partnerCompensation(new BigDecimal(200))
                                                                        .refundAmount(new BigDecimal(2000))
                                                        )
                                                )
                                                .logisticItems(List.of(
                                                                new LogisticItemDTO()
                                                                        .stockType(StockTypeDTO.FIT)
                                                                        .status(MerchantItemStatusDTO.RETURN_READY_FOR_PICKUP)
                                                                        .itemInfo(Map.of("CIS"
                                                                                , "12345"))
                                                        )
                                                ))
                                )
                ));
        returnsResponse.pager(new PagerWithToken()
                .pageSize(20)
                .nextPageToken("eyBuZXh0SWQ6IDIzNDIgfQ=="));


        return new PagedReturnResponse().result(returnsResponse);

    }

    private PagedReturnResponse mockResponseWithNullLists() {
        var returnsResponse = new PagedReturnsResponse()
                .orderReturns(List.of(
                        new ReturnDTO()
                                .returnId(20L)
                                .orderId(200L)
                                .partnerOrderId("222-20")
                                .createdAt(OffsetDateTime.parse("2020-02-01T14:30:30+03:00"))
                                .updatedAt(OffsetDateTime.parse("2020-02-02T14:30:30+03:00"))
                                .returnStatus(OrderReturnRefundStatus.STARTED_BY_USER)
                                .logisticStatus(LogisticReturnStatusDTO.IN_TRANSIT)
                                .refundAmount(new BigDecimal(2000))
                                .returnLines(
                                        List.of(
                                                new ReturnLineDTO()
                                                        .shopSku("sku-20")
                                                        .marketSku(2000L)
                                                        .count(1L)
                                                        .refunds(null)
                                                        .logisticItems(null)
                                        )
                                )
                ));
        returnsResponse.pager(new PagerWithToken()
                .pageSize(20)
                .nextPageToken("eyBuZXh0SWQ6IDIzNDIgfQ=="));


        return new PagedReturnResponse().result(returnsResponse);

    }

    private PagedReturnResponse mockResponseWithNullReturnLines() {
        var returnsResponse = new PagedReturnsResponse()
                .orderReturns(List.of(
                        new ReturnDTO()
                                .returnId(20L)
                                .orderId(200L)
                                .partnerOrderId("222-20")
                                .createdAt(OffsetDateTime.parse("2020-02-01T14:30:30+03:00"))
                                .updatedAt(OffsetDateTime.parse("2020-02-02T14:30:30+03:00"))
                                .returnStatus(OrderReturnRefundStatus.STARTED_BY_USER)
                                .logisticStatus(LogisticReturnStatusDTO.IN_TRANSIT)
                                .refundAmount(new BigDecimal(2000))
                                .returnLines(
                                        null
                                )
                ));
        returnsResponse.pager(new PagerWithToken()
                .pageSize(20)
                .nextPageToken("eyBuZXh0SWQ6IDIzNDIgfQ=="));


        return new PagedReturnResponse().result(returnsResponse);

    }



    private static ResponseEntity<String> doGetXml(String url) {
        System.out.println("Getting " + url);
        return FunctionalTestHelper
                .makeRequestWithContentType(url, HttpMethod.GET, String.class, MediaType.APPLICATION_XML);
    }

    private static ResponseEntity<String> doGetJson(String url) {
        System.out.println("Getting " + url);
        return FunctionalTestHelper
                .makeRequestWithContentType(url, HttpMethod.GET, String.class, MediaType.APPLICATION_JSON);
    }

}
