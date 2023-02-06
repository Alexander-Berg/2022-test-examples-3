package ru.yandex.market.ff.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.market.api.cpa.yam.dto.OrganizationInfoDTO;
import ru.yandex.market.api.cpa.yam.dto.PrepayRequestDTO;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.enums.ExternalOperationType;
import ru.yandex.market.ff.listener.event.RequestStatusChangeEvent;
import ru.yandex.market.ff.model.bo.ItemCountDetails;
import ru.yandex.market.ff.model.bo.ItemDetails;
import ru.yandex.market.ff.model.bo.RequestStatusInfo;
import ru.yandex.market.ff.model.bo.RequestStatusResult;
import ru.yandex.market.ff.model.bo.SupplierSkuKeyWithOrderId;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.entity.SupplierBusinessType;
import ru.yandex.market.ff.service.exception.InconsistentRequestChangeException;
import ru.yandex.market.ff.service.implementation.lgw.LgwRequestXDocSupplyService;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundDetailsXDoc;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundStatus;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundUnitDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundStatus;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundUnitDetails;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Status;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StatusCode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link LgwRequestService}.
 *
 * @author avetokhin 19/09/17.
 */
class FulfillmentRequestServiceTest extends IntegrationTest {

    private static final Long REQ_ID_1 = 1L;
    private static final Long REQ_ID_2 = 2L;

    private static final String REQ_EXT_ID_1 = "11";
    private static final String REQ_EXT_ID_2 = "22";

    private static final ResourceId RES_ID_1 = ResourceId.builder()
            .setYandexId(REQ_ID_1.toString())
            .setPartnerId(REQ_EXT_ID_1)
            .build();
    private static final ResourceId RES_ID_2 = ResourceId.builder()
            .setYandexId(REQ_ID_2.toString())
            .setPartnerId(REQ_EXT_ID_2)
            .build();
    private static final LocalDateTime FIXED_DT = LocalDateTime.of(2017, 1, 1, 10, 0, 0, 0);
    private static final String FIXED_DT_STR = "2017-01-01T10:00:00";
    private static final Long SERVICE_ID_1 = 555L;
    private static final Long SERVICE_ID_2 = 666L;
    private static final Partner PARTNER_1 = partner(SERVICE_ID_1);

    private static final long SUPPLIER_ID = 42;

    @Autowired
    private ShopRequestFetchingService shopRequestFetchingService;

    @Autowired
    private LgwRequestService service;

    @Autowired
    private LgwRequestXDocSupplyService xDocSupplyService;

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private MbiApiClient mbiApiClient;

    /**
     * При отправке в LGW для всех статусов кроме VALIDATED бросается исключение.
     */
    @Test
    void pushRequestWithInvalidStatus() {
        final List<RequestStatus> requestStatuses =
                allStatusesExcept(RequestStatus.VALIDATED, RequestStatus.SENT_TO_SERVICE);
        requestStatuses.forEach(status -> {
            for (RequestType type : RequestType.REAL_TYPES) {
                final ShopRequest request = request(status, type);
                expectedInconsistentRequestChangeException(() ->
                        service.pushRequest(request, false), " request: " + request);
            }
        });
    }

    /**
     * При отправке в LGW проверяет склады в игноре
     */
    @Test
    @DatabaseSetup("classpath:service/shop-request-lgw/before-push-request-to-ignored-warehouse.xml")
    @ExpectedDatabase(value = "classpath:service/shop-request-lgw/before-push-request-to-ignored-warehouse.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void pushRequestToIgnoredWarehouse() {
        final ShopRequest request = request(RequestStatus.VALIDATED, RequestType.SUPPLY);
        request.setServiceId(337L);
        service.pushRequest(request, false);
    }

    /**
     * При отправке в LGW xDoc поставок для всех статусов кроме ACCEPTED_BY_SERVICE бросается исключение.
     */
    @Test
    void pushXDocRequestWithInvalidStatus() {
        final List<RequestStatus> requestStatuses = allStatusesExcept(RequestStatus.ACCEPTED_BY_SERVICE);
        requestStatuses.forEach(status -> {
            final ShopRequest request = request(status, RequestType.SUPPLY);
            expectedInconsistentRequestChangeException(() ->
                    service.pushRequest(request, true), " request: " + request);
        });
    }

    /**
     * Проверка запроса к LGW для получения актуальных статусов xDoc поставок.
     */
    @Test
    void getXDocRequestsStatusesSupply() {
        InboundStatus status1 = inboundStatus(RES_ID_1, StatusCode.ARRIVED, FIXED_DT_STR);
        InboundStatus status2 = inboundStatus(RES_ID_2, StatusCode.SHIPPED, FIXED_DT_STR);

        when(fulfillmentClient.getInboundsStatus(Arrays.asList(RES_ID_1, RES_ID_2), PARTNER_1))
                .thenReturn(Arrays.asList(status1, status2));

        callAndAssertGetXDocInboundsStatus();
    }

    /**
     * Проверка запроса к LGW для получения актуальных статусов поставок.
     */
    @Test
    @DatabaseSetup("classpath:service/shop-request-lgw/before-push-request-to-ignored-warehouse.xml")
    void getRequestsStatusesSupply() {
        final InboundStatus status1 = inboundStatus(RES_ID_1, StatusCode.ARRIVED, FIXED_DT_STR);
        final InboundStatus status2 = inboundStatus(RES_ID_2, StatusCode.ACCEPTED, FIXED_DT_STR);

        when(fulfillmentClient.getInboundsStatus(Arrays.asList(RES_ID_1, RES_ID_2), PARTNER_1))
                .thenReturn(Arrays.asList(status1, status2));

        callAndAssertGetInboundsStatus(RequestType.SUPPLY);
    }

    /**
     * Проверка запроса к LGW для получения актуальных статусов пользовательских возвратов.
     */
    @Test
    void getRequestsStatusesCustomerReturn() {
        final InboundStatus status1 = inboundStatus(RES_ID_1, StatusCode.ARRIVED, FIXED_DT_STR);
        final InboundStatus status2 = inboundStatus(RES_ID_2, StatusCode.ACCEPTED, FIXED_DT_STR);

        when(fulfillmentClient.getInboundsStatus(Arrays.asList(RES_ID_1, RES_ID_2), PARTNER_1))
                .thenReturn(Arrays.asList(status1, status2));
        callAndAssertGetInboundsStatus(RequestType.CUSTOMER_RETURN_SUPPLY);
    }

    /**
     * Проверка запроса к LGW для получения актуальных статусов годных невыкупов.
     */
    @Test
    void getRequestsStatusesValidUnredeemed() {
        InboundStatus status1 = inboundStatus(RES_ID_1, StatusCode.ARRIVED, FIXED_DT_STR);
        InboundStatus status2 = inboundStatus(RES_ID_2, StatusCode.ACCEPTED, FIXED_DT_STR);

        when(fulfillmentClient.getInboundsStatus(Arrays.asList(RES_ID_1, RES_ID_2), PARTNER_1))
                .thenReturn(Arrays.asList(status1, status2));

        callAndAssertGetInboundsStatus(RequestType.VALID_UNREDEEMED);
    }

    /**
     * Проверка запроса к LGW для получения актуальных статусов негодных невыкупов.
     */
    @Test
    void getRequestsStatusesInvalidUnredeemed() {
        InboundStatus status1 = inboundStatus(RES_ID_1, StatusCode.ARRIVED, FIXED_DT_STR);
        InboundStatus status2 = inboundStatus(RES_ID_2, StatusCode.ACCEPTED, FIXED_DT_STR);

        when(fulfillmentClient.getInboundsStatus(Arrays.asList(RES_ID_1, RES_ID_2), PARTNER_1))
                .thenReturn(Arrays.asList(status1, status2));

        callAndAssertGetInboundsStatus(RequestType.INVALID_UNREDEEMED);
    }

    /**
     * Проверка запроса к LGW для получения актуальных статусов пользовательских возвратов.
     */
//    @Test
    void getRequestsStatusesCustomerReturnRegistry() {
        InboundStatus status1 = inboundStatus(RES_ID_1, StatusCode.ARRIVED, FIXED_DT_STR);
        InboundStatus status2 = inboundStatus(RES_ID_2, StatusCode.ACCEPTED, FIXED_DT_STR);

        when(fulfillmentClient.getInboundsStatus(Arrays.asList(RES_ID_1, RES_ID_2), PARTNER_1))
                .thenReturn(Arrays.asList(status1, status2));

        callAndAssertGetInboundsStatus(RequestType.CUSTOMER_RETURN);
    }

    /**
     * Проверка запроса к LGW для получения актуальных статусов изъятий.
     */
    @Test
    void getRequestsStatusesWithdraw() {
        final OutboundStatus status1 = outboundStatus(RES_ID_1, StatusCode.ASSEMBLED, FIXED_DT_STR);
        final OutboundStatus status2 = outboundStatus(RES_ID_2, StatusCode.TRANSFERRED, FIXED_DT_STR);

        when(fulfillmentClient.getOutboundsStatus(Arrays.asList(RES_ID_1, RES_ID_2), PARTNER_1))
                .thenReturn(Arrays.asList(status1, status2));

        final Map<Long, RequestStatusInfo> requestsStatuses = service.getRequestsStatuses(Arrays.asList(
                request(REQ_ID_1, REQ_EXT_ID_1, RequestType.WITHDRAW, SERVICE_ID_1),
                request(REQ_ID_2, REQ_EXT_ID_2, RequestType.WITHDRAW, SERVICE_ID_1)),
                false);

        assertThat(requestsStatuses, notNullValue());
        assertThat(requestsStatuses.get(REQ_ID_1),
                equalTo(new RequestStatusInfo(RequestStatusResult.of(RequestStatus.READY_TO_WITHDRAW), FIXED_DT)));

        assertThat(requestsStatuses.get(REQ_ID_2),
                equalTo(new RequestStatusInfo(RequestStatusResult.of(RequestStatus.PROCESSED), FIXED_DT)));
    }

    /**
     * Не слать нотификаций о появлении на складе, если это перемещение Яндекс.Маркета
     */
    @Test
    void dontGetRequestsStatusesWithdrawOnMove() {
        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(1L);
        shopRequest.setStatus(RequestStatus.READY_TO_WITHDRAW);
        shopRequest.setType(RequestType.SUPPLY);
        shopRequest.setExternalOperationType(ExternalOperationType.MOVE);

        RequestStatusChangeEvent event = new RequestStatusChangeEvent(
                shopRequest, null, null, RequestStatus.IN_PROGRESS, shopRequest.getStatus());

        eventPublisher.publishEvent(event);

        verifyZeroInteractions(mbiApiClient);
    }

    private void callAndAssertGetInboundsStatus(final RequestType requestType) {
        final Map<Long, RequestStatusInfo> requestsStatuses = service.getRequestsStatuses(Arrays.asList(
                request(REQ_ID_1, REQ_EXT_ID_1, requestType, SERVICE_ID_1),
                request(REQ_ID_2, REQ_EXT_ID_2, requestType, SERVICE_ID_1)),
                false);

        assertThat(requestsStatuses, notNullValue());
        assertThat(requestsStatuses.get(REQ_ID_1),
                equalTo(new RequestStatusInfo(RequestStatusResult.of(RequestStatus.ARRIVED_TO_SERVICE), FIXED_DT)));

        assertThat(requestsStatuses.get(REQ_ID_2),
                equalTo(new RequestStatusInfo(RequestStatusResult.of(RequestStatus.PROCESSED), FIXED_DT)));
    }

    private void callAndAssertGetXDocInboundsStatus() {
        final Map<Long, RequestStatusInfo> requestsStatuses = service.getRequestsStatuses(Arrays.asList(
                xDocRequest(REQ_ID_1, REQ_EXT_ID_1, RequestType.SUPPLY, SERVICE_ID_1),
                xDocRequest(REQ_ID_2, REQ_EXT_ID_2, RequestType.SUPPLY, SERVICE_ID_1)),
                true);

        assertThat(requestsStatuses, notNullValue());
        assertThat(requestsStatuses.get(REQ_ID_1),
                equalTo(new RequestStatusInfo(RequestStatusResult.of(RequestStatus.ARRIVED_TO_XDOC_SERVICE),
                        FIXED_DT)));

        assertThat(requestsStatuses.get(REQ_ID_2),
                equalTo(new RequestStatusInfo(RequestStatusResult.of(RequestStatus.SHIPPED_TO_SERVICE), FIXED_DT)));
    }

    /**
     * Проверка ошибки в случае вызова с заявками разных типов.
     */
    @Test
    void getRequestsStatusesDifferentTypes() {
        final IllegalArgumentException e =
                Assertions.assertThrows(IllegalArgumentException.class, () ->
                        service.getRequestsStatuses(Arrays.asList(
                                request(REQ_ID_1, REQ_EXT_ID_1, RequestType.CUSTOMER_RETURN_SUPPLY, SERVICE_ID_1),
                                request(REQ_ID_2, REQ_EXT_ID_2, RequestType.SUPPLY, SERVICE_ID_1)), false));
        assertThat(e.getMessage(), equalTo("Could not retrieve statuses of requests with different types"));
    }

    /**
     * Проверка ошибки в случае вызова с заявками разных складов.
     */
    @Test
    void getRequestsStatusesDifferentService() {
        final IllegalArgumentException e =
                Assertions.assertThrows(IllegalArgumentException.class, () ->
                        service.getRequestsStatuses(Arrays.asList(
                                request(REQ_ID_1, REQ_EXT_ID_1, RequestType.WITHDRAW, SERVICE_ID_1),
                                request(REQ_ID_2, REQ_EXT_ID_2, RequestType.WITHDRAW, SERVICE_ID_2)), false));
        assertThat(e.getMessage(), equalTo("Could not retrieve statuses of requests with different services"));
    }

    /**
     * Проверка запроса к LGW для получения истории статусов поставок.
     */
    @Test
    void getRequestStatusesHistorySupply() {
        callAndAssertGetRequestStatusesHistoryForInbound(RequestType.SUPPLY);
    }

    /**
     * Проверка запроса к LGW для получения истории статусов годных невыкупов.
     */
    @Test
    void getRequestStatusesHistoryValidUnredeemed() {
        callAndAssertGetRequestStatusesHistoryForInbound(RequestType.VALID_UNREDEEMED);
    }

    /**
     * Проверка запроса к LGW для получения истории статусов негодных невыкупов.
     */
    @Test
    void getRequestStatusesHistoryInvalidUnredeemed() {
        callAndAssertGetRequestStatusesHistoryForInbound(RequestType.INVALID_UNREDEEMED);
    }

    /**
     * Проверка запроса к LGW для получения истории статусов пользовательских возвратов.
     */
//    @Test
    void getRequestStatusesHistoryCustomerReturnRegistry() {
        callAndAssertGetRequestStatusesHistoryForInbound(RequestType.CUSTOMER_RETURN);
    }

    private void callAndAssertGetRequestStatusesHistoryForInbound(@Nonnull RequestType requestType) {
        List<Status> statuses = Arrays.asList(
                status(StatusCode.CREATED, "2017-01-01T10:00:00"),
                status(StatusCode.ARRIVED, "2017-01-01T10:05:00"),
                status(StatusCode.ACCEPTANCE, "2017-01-01T10:07:00"),
                status(StatusCode.ACCEPTED, "2017-01-01T10:10:00")
        );
        InboundStatusHistory inboundHistory = new InboundStatusHistory(statuses, RES_ID_1);
        when(fulfillmentClient.getInboundHistory(RES_ID_1, PARTNER_1)).thenReturn(inboundHistory);

        List<RequestStatusInfo> history =
                service.getRequestStatusesHistory(request(REQ_ID_1, REQ_EXT_ID_1, requestType, SERVICE_ID_1), false);

        assertThat(history, notNullValue());
        assertThat(history, hasSize(3));
        assertThat(history.get(0).getRequestStatusResult(),
                equalTo(RequestStatusResult.of(RequestStatus.ARRIVED_TO_SERVICE)));
        assertThat(history.get(0).getDate(), equalTo(LocalDateTime.of(2017, 1, 1, 10, 5, 0)));

        assertThat(history.get(1).getRequestStatusResult(), equalTo(RequestStatusResult.of(RequestStatus.IN_PROGRESS)));
        assertThat(history.get(1).getDate(), equalTo(LocalDateTime.of(2017, 1, 1, 10, 7, 0)));

        assertThat(history.get(2).getRequestStatusResult(), equalTo(RequestStatusResult.of(RequestStatus.PROCESSED)));
        assertThat(history.get(2).getDate(), equalTo(LocalDateTime.of(2017, 1, 1, 10, 10, 0)));
    }

    /**
     * Проверка запроса к LGW для получения истории статусов xDoc поставок.
     */
    @Test
    void getRequestStatusesHistoryXDocSupply() {
        final List<Status> statuses = Arrays.asList(
                status(StatusCode.CREATED, "2017-01-01T10:00:00"),
                status(StatusCode.ARRIVED, "2017-01-01T10:05:00"),
                status(StatusCode.SHIPPED, "2017-01-01T10:07:00")
        );
        final InboundStatusHistory inboundHistory = new InboundStatusHistory(statuses, RES_ID_1);
        when(fulfillmentClient.getInboundHistory(RES_ID_1, PARTNER_1)).thenReturn(inboundHistory);

        final List<RequestStatusInfo> history = service
                .getRequestStatusesHistory(xDocRequest(REQ_ID_1, REQ_EXT_ID_1, RequestType.SUPPLY, SERVICE_ID_1), true);

        assertThat(history, notNullValue());
        assertThat(history, hasSize(2));

        assertThat(history.get(0).getRequestStatusResult(),
                equalTo(RequestStatusResult.of(RequestStatus.ARRIVED_TO_XDOC_SERVICE)));
        assertThat(history.get(0).getDate(), equalTo(LocalDateTime.of(2017, 1, 1, 10, 5, 0)));

        assertThat(history.get(1).getRequestStatusResult(),
                equalTo(RequestStatusResult.of(RequestStatus.SHIPPED_TO_SERVICE)));
        assertThat(history.get(1).getDate(), equalTo(LocalDateTime.of(2017, 1, 1, 10, 7, 0)));
    }

    /**
     * Проверка запроса к LGW для получения истории статусов изъятий.
     */
    @Test
    void getRequestStatusesHistoryWithdraw() {
        final List<Status> statuses = Arrays.asList(
                status(StatusCode.CREATED, "2017-01-01T09:00:00"),
                status(StatusCode.ASSEMBLING, "2017-01-01T10:00:00"),
                status(StatusCode.ASSEMBLED, "2017-01-01T10:05:00"),
                status(StatusCode.TRANSFERRED, "2017-01-01T10:07:00")
        );
        final OutboundStatusHistory outboundStatusHistory = new OutboundStatusHistory(RES_ID_1, statuses);
        when(fulfillmentClient.getOutboundHistory(RES_ID_1, PARTNER_1)).thenReturn(outboundStatusHistory);

        final List<RequestStatusInfo> history = service
                .getRequestStatusesHistory(request(REQ_ID_1, REQ_EXT_ID_1, RequestType.WITHDRAW, SERVICE_ID_1), false);

        assertThat(history, notNullValue());
        assertThat(history, hasSize(3));
        assertThat(history.get(0).getRequestStatusResult(), equalTo(RequestStatusResult.of(RequestStatus.IN_PROGRESS)));
        assertThat(history.get(0).getDate(), equalTo(LocalDateTime.of(2017, 1, 1, 10, 0, 0)));

        assertThat(history.get(1).getRequestStatusResult(),
                equalTo(RequestStatusResult.of(RequestStatus.READY_TO_WITHDRAW)));
        assertThat(history.get(1).getDate(), equalTo(LocalDateTime.of(2017, 1, 1, 10, 5, 0)));

        assertThat(history.get(2).getRequestStatusResult(), equalTo(RequestStatusResult.of(RequestStatus.PROCESSED)));
        assertThat(history.get(2).getDate(), equalTo(LocalDateTime.of(2017, 1, 1, 10, 7, 0)));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-lgw/before-push-withdraw.xml")
    void cancelWithdraw() throws GatewayApiException {
        final ShopRequest request = shopRequestFetchingService.getRequestOrThrow(REQ_ID_1);
        service.cancelRequest(request);

        final ResourceId resourceId = ResourceId.builder()
                .setYandexId(REQ_ID_1.toString())
                .setPartnerId("10")
                .build();
        verify(fulfillmentClient).cancelOutbound(resourceId, partner(121L));
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-lgw/before-push-supply.xml")
    void cancelSupply() throws GatewayApiException {
        callCancelInboundAndAssertResult();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-lgw/before-push-valid-unredeemed.xml")
    void cancelValidUnredeemed() throws GatewayApiException {
        callCancelInboundAndAssertResult();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-lgw/before-push-invalid-unredeemed.xml")
    void cancelInvalidUnredeemed() throws GatewayApiException {
        callCancelInboundAndAssertResult();
    }

    @Test
    @DatabaseSetup("classpath:service/shop-request-lgw/before-push-customer-return.xml")
    void cancelCustomerReturn() throws GatewayApiException {
        callCancelInboundAndAssertResult();
    }

    private void callCancelInboundAndAssertResult() throws GatewayApiException {
        ShopRequest request = shopRequestFetchingService.getRequestOrThrow(REQ_ID_1);
        service.cancelRequest(request);

        ResourceId resourceId = ResourceId.builder()
                .setYandexId(REQ_ID_1.toString())
                .setPartnerId("10")
                .build();

        verify(fulfillmentClient).cancelInbound(resourceId, new Partner(121L));
    }

    @Test
    void getXDocSupplyRequestDetails() {
        final ResourceId resourceId = ResourceId.builder()
                .setYandexId(REQ_ID_1.toString())
                .setPartnerId(REQ_EXT_ID_1)
                .build();

        InboundDetailsXDoc inboundDetailsXDoc = new InboundDetailsXDoc(resourceId, 5, 10);
        when(fulfillmentClient.getInboundDetailsXDoc(resourceId, PARTNER_1))
                .thenReturn(inboundDetailsXDoc);

        InboundDetailsXDoc details = xDocSupplyService.getXDocRequestDetails(xDocRequest(REQ_ID_1,
                REQ_EXT_ID_1,
                RequestType.SUPPLY,
                SERVICE_ID_1));

        verify(fulfillmentClient).getInboundDetailsXDoc(resourceId, PARTNER_1);

        assertThat(details, notNullValue());
        assertThat(details.getActualPalletAmount(), equalTo(5));
        assertThat(details.getActualBoxAmount(), equalTo(10));
    }

    @Test
    void getWithdrawRequestDetails() {
        final ResourceId resourceId = ResourceId.builder()
                .setYandexId(REQ_ID_1.toString())
                .setPartnerId(REQ_EXT_ID_1)
                .build();

        final OutboundUnitDetails outboundDetails1 = outboundUnitDetails("art1", 4, 2);
        final OutboundUnitDetails outboundDetails2 = outboundUnitDetails("art2", 3, 3);

        final OutboundDetails outboundDetails =
                new OutboundDetails(resourceId, Arrays.asList(outboundDetails1, outboundDetails2));
        when(fulfillmentClient.getOutboundDetails(resourceId, PARTNER_1))
                .thenReturn(outboundDetails);

        ShopRequest request = request(REQ_ID_1, REQ_EXT_ID_1, RequestType.WITHDRAW, SERVICE_ID_1);
        final Map<SupplierSkuKeyWithOrderId, ItemDetails> details = service.getRequestDetails(request);
        verify(fulfillmentClient).getOutboundDetails(resourceId, PARTNER_1);

        assertThat(details, notNullValue());
        assertThat(details.size(), equalTo(2));
        assertThat(details.get(new SupplierSkuKeyWithOrderId(SUPPLIER_ID, "art1")),
                equalTo(new ItemDetails(new ItemCountDetails("art1", 4, 2, 0, 0))));
        assertThat(details.get(new SupplierSkuKeyWithOrderId(SUPPLIER_ID, "art2")),
                equalTo(new ItemDetails(new ItemCountDetails("art2", 3, 3, 0, 0))));
    }

    @Test
    @DatabaseSetup("classpath:db-queue/service/modification-shop-request/5/before.xml")
    @ExpectedDatabase(value = "classpath:db-queue/service/modification-shop-request/5/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void updateDemandBbxdStatusTest() {
        PrepayRequestDTO prepayRequest = new PrepayRequestDTO();
        OrganizationInfoDTO organizationInfo = createOrganizationInfo();
        prepayRequest.setOrganizationInfo(organizationInfo);
        when(mbiApiClient.getPrepayRequest(anyLong(), anyLong()))
                .thenReturn(prepayRequest);

        ShopRequest shopRequest = shopRequestFetchingService.findRequest(4L).get();
        service.pushRequest(shopRequest, false);
    }

    private static List<RequestStatus> allStatusesExcept(final RequestStatus... statuses) {
        final List<RequestStatus> requestStatuses = Stream.of(RequestStatus.values()).collect(Collectors.toList());
        requestStatuses.removeAll(Arrays.asList(statuses));
        return requestStatuses;
    }

    private static ShopRequest request(long id, String externalId, RequestType requestType, long serviceId) {
        final ShopRequest request = new ShopRequest();
        request.setId(id);
        request.setServiceRequestId(externalId);
        request.setType(requestType);
        request.setServiceId(serviceId);
        request.setSupplier(supplier());
        request.setCreatedAt(LocalDateTime.now());
        return request;
    }

    private static ShopRequest xDocRequest(long id, String externalId, RequestType requestType, long serviceId) {
        final ShopRequest request = new ShopRequest();
        request.setId(id);
        request.setxDocServiceRequestId(externalId);
        request.setType(requestType);
        request.setxDocServiceId(serviceId);
        request.setSupplier(supplier());
        request.setCreatedAt(LocalDateTime.now());
        return request;
    }

    private static Supplier supplier() {
        return new Supplier(
                SUPPLIER_ID,
                "test_supplier",
                "test_organization",
                0L,
                SupplierType.FIRST_PARTY,
                new SupplierBusinessType()
        );
    }

    private static InboundStatus inboundStatus(final ResourceId resourceId, final StatusCode statusCode,
                                               final String dateTime) {
        return new InboundStatus(resourceId, new Status(statusCode, new DateTime(dateTime)));
    }

    private static OutboundStatus outboundStatus(final ResourceId resourceId, final StatusCode statusCode,
                                                 final String dateTime) {
        return new OutboundStatus(resourceId, new Status(statusCode, new DateTime(dateTime)));
    }

    private static Status status(final StatusCode statusCode, final String dateTime) {
        return new Status(statusCode, new DateTime(dateTime));
    }

    private void expectedInconsistentRequestChangeException(Runnable runnable, String message) {
        try {
            runnable.run();
        } catch (InconsistentRequestChangeException ignored) {
            return;
        }
        fail("Expected exception, " + message);
    }

    private static ShopRequest request(final RequestStatus status, final RequestType requestType) {
        final ShopRequest request = new ShopRequest();
        request.setId(1L);
        final Supplier supplier = new Supplier();
        supplier.setId(2L);
        request.setSupplier(supplier);
        request.setType(requestType);
        request.setStatus(status);
        request.setCreatedAt(LocalDateTime.now());
        return request;
    }

    private static InboundUnitDetails inboundUnitDetails(String article, Integer declared, Integer actual,
                                                         Integer defect, Integer surplus) {
        final UnitId unitId = new UnitId(null, SUPPLIER_ID, article);
        return new InboundUnitDetails(unitId, declared, actual, defect, surplus);
    }

    private static OutboundUnitDetails outboundUnitDetails(String article, int declared, int actual) {
        final UnitId unitId = new UnitId(null, SUPPLIER_ID, article);
        return new OutboundUnitDetails(unitId, declared, actual);
    }


    private static Partner partner(long id) {
        return new Partner(id);
    }

    private static OrganizationInfoDTO createOrganizationInfo() {
        return OrganizationInfoDTO.builder()
                .type(OrganizationType.IP)
                .inn("someInn")
                .ogrn("someOgrn")
                .kpp("someKpp")
                .name("someOrgName")
                .accountNumber("accNumber")
                .corrAccountNumber("corrAccNumber")
                .bankName("bankName")
                .bik("someBik")
                .factAddress("factAddr")
                .juridicalAddress("jurAddr")
                .build();
    }
}
