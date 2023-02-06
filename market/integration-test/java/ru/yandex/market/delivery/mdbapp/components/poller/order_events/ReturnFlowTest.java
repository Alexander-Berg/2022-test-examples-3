package ru.yandex.market.delivery.mdbapp.components.poller.order_events;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.checkouter.client.CheckouterReturnApi;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.returns.ReturnRequest;
import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnRequestState;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.PickupPointRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.ReturnRequestItemRepository;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.ReturnRequestRepository;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.COMMITED_ORDER_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.COMMITED_RETURN_BARCODE_FF;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.COMMITED_RETURN_ID_STR;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ITEM_SKU_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ITEM_SUPPLIER_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ORDER_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.PRICE_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.checkouterReturn;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.checkouterReturnWithDelivery;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.item;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.lomOrder;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.marketPvzPartnerResponse;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.nonMarketPvzPartnerResponse;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.pickupPoint;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.pvzLogisticsPointResponse;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequest;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.terminalLogisticsPointResponse;
import static ru.yandex.market.delivery.mdbapp.testutils.MockUtils.prepareMockServer;
import static steps.outletSteps.OutletSteps.getDefaultOutlet;

public class ReturnFlowTest extends MockContextualTest {

    private MockRestServiceServer checkouterMockServer;

    @Autowired
    @Qualifier("orderEventsPoller0")
    private OrderEventsPoller poller;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Autowired
    private CheckouterReturnApi checkouterReturnApi;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private LomClient lomClient;

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private ReturnRequestItemRepository returnRequestItemRepository;

    @Autowired
    private PickupPointRepository pickupPointRepository;

    @Autowired
    private TransactionOperations transactionOperations;

    @Before
    public void setUp() {
        checkouterMockServer = MockRestServiceServer.createServer(checkouterRestTemplate);
    }

    @After
    public void tearDown() {
        returnRequestItemRepository.deleteAll();
        returnRequestRepository.deleteAll();
        pickupPointRepository.deleteAll();
    }

    @Test
    public void createReturnRequestWithoutPickupPointWhenReturnHasNoDelivery() {
        transactionOperations.execute(tc -> {
            try {
                // given:
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/events",
                    "/data/events/return_created.json"
                );
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/167802870",
                    "/data/checkouter/return_order.json"
                );
                OrderSearchFilter filter = OrderSearchFilter.builder()
                    .externalIds(Set.of(String.valueOf(ORDER_ID)))
                    .senderIds(Set.of(1L))
                    .build();
                when(lomClient.searchOrders(eq(filter), eq(Set.of()), any(Pageable.class), eq(false)))
                    .thenReturn(PageResult.of(List.of(lomOrder(PartnerType.DROPSHIP)), 1, 1, 1));
                doReturn(checkouterReturn())
                    .when(checkouterReturnApi)
                    .getReturn(
                        any(RequestClientInfo.class),
                        any(ReturnRequest.class)
                    );

                // when:
                poller.poll();

                // then:
                verify(checkouterReturnApi).getReturn(any(RequestClientInfo.class), any(ReturnRequest.class));
                verifyZeroInteractions(mbiApiClient);
                verifyZeroInteractions(lmsClient);
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    @Test
    public void createReturnRequestWithPickupPointByMbi() {
        transactionOperations.execute(tc -> {
            try {
                // given:
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/events",
                    "/data/events/return_created.json"
                );
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/167802870",
                    "/data/checkouter/return_order.json"
                );
                OrderSearchFilter filter = OrderSearchFilter.builder()
                    .externalIds(Set.of(String.valueOf(ORDER_ID)))
                    .senderIds(Set.of(1L))
                    .build();
                when(lomClient.searchOrders(eq(filter), eq(Set.of()), any(Pageable.class), eq(false)))
                    .thenReturn(PageResult.of(List.of(lomOrder(PartnerType.DROPSHIP)), 1, 1, 1));
                doReturn(checkouterReturnWithDelivery())
                    .when(checkouterReturnApi)
                    .getReturn(
                        any(RequestClientInfo.class),
                        any(ReturnRequest.class)
                    );
                doReturn(getDefaultOutlet())
                    .when(mbiApiClient)
                    .getOutlet(
                        anyLong(),
                        anyBoolean()
                    );
                doReturn(List.of(pvzLogisticsPointResponse()))
                    .when(lmsClient)
                    .getLogisticsPoints(any(LogisticsPointFilter.class));
                doReturn(Optional.of(marketPvzPartnerResponse()))
                    .when(lmsClient)
                    .getPartner(anyLong());

                // when:
                poller.poll();

                // then:
                verify(checkouterReturnApi).getReturn(any(RequestClientInfo.class), any(ReturnRequest.class));
                verify(mbiApiClient).getOutlet(anyLong(), anyBoolean());
                verify(lmsClient).getLogisticsPoints(any(LogisticsPointFilter.class));
                verify(lmsClient).getPartner(anyLong());
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    @Test
    public void createReturnRequestWithPickupPointByLms() {
        transactionOperations.execute(tc -> {
            try {
                // given:
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/events",
                    "/data/events/return_created.json"
                );
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/167802870",
                    "/data/checkouter/return_order.json"
                );
                OrderSearchFilter filter = OrderSearchFilter.builder()
                    .externalIds(Set.of(String.valueOf(ORDER_ID)))
                    .senderIds(Set.of(1L))
                    .build();
                when(lomClient.searchOrders(eq(filter), eq(Set.of()), any(Pageable.class), eq(false)))
                    .thenReturn(PageResult.of(List.of(lomOrder(PartnerType.DROPSHIP)), 1, 1, 1));
                doReturn(checkouterReturnWithDelivery())
                    .when(checkouterReturnApi)
                    .getReturn(
                        any(RequestClientInfo.class),
                        any(ReturnRequest.class)
                    );
                doReturn(null)
                    .when(mbiApiClient)
                    .getOutlet(
                        anyLong(),
                        anyBoolean()
                    );
                doReturn(Optional.of(pvzLogisticsPointResponse()))
                    .when(lmsClient)
                    .getLogisticsPoint(anyLong());
                doReturn(Optional.of(marketPvzPartnerResponse()))
                    .when(lmsClient)
                    .getPartner(anyLong());

                // when:
                poller.poll();

                // then:
                verify(checkouterReturnApi).getReturn(any(RequestClientInfo.class), any(ReturnRequest.class));
                verify(mbiApiClient).getOutlet(anyLong(), anyBoolean());
                verify(lmsClient).getLogisticsPoint(anyLong());
                verify(lmsClient).getPartner(anyLong());
                verifyZeroInteractions(lmsClient);
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    @Test
    public void createReturnRequestWithoutPickupPoint_whenLogisticsPointIsNotPvz() {
        transactionOperations.execute(tc -> {
            try {
                // given:
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/events",
                    "/data/events/return_created.json"
                );
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/167802870",
                    "/data/checkouter/return_order.json"
                );
                OrderSearchFilter filter = OrderSearchFilter.builder()
                    .externalIds(Set.of(String.valueOf(ORDER_ID)))
                    .senderIds(Set.of(1L))
                    .build();
                when(lomClient.searchOrders(eq(filter), eq(Set.of()), any(Pageable.class), eq(false)))
                    .thenReturn(PageResult.of(List.of(lomOrder(PartnerType.DROPSHIP)), 1, 1, 1));
                doReturn(checkouterReturnWithDelivery())
                    .when(checkouterReturnApi)
                    .getReturn(
                        any(RequestClientInfo.class),
                        any(ReturnRequest.class)
                    );
                doReturn(null)
                    .when(mbiApiClient)
                    .getOutlet(
                        anyLong(),
                        anyBoolean()
                    );
                doReturn(Optional.of(terminalLogisticsPointResponse()))
                    .when(lmsClient)
                    .getLogisticsPoint(anyLong());

                // when:
                poller.poll();

                // then:
                verify(checkouterReturnApi).getReturn(any(RequestClientInfo.class), any(ReturnRequest.class));
                verify(mbiApiClient).getOutlet(anyLong(), anyBoolean());
                verify(lmsClient).getLogisticsPoint(anyLong());
                verifyZeroInteractions(lmsClient);
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    @Test
    public void createReturnRequestWithoutPickupPoint_whenLogisticsPointIsNotMarketPvz() {
        transactionOperations.execute(tc -> {
            try {
                // given:
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/events",
                    "/data/events/return_created.json"
                );
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/167802870",
                    "/data/checkouter/return_order.json"
                );
                OrderSearchFilter filter = OrderSearchFilter.builder()
                    .externalIds(Set.of(String.valueOf(ORDER_ID)))
                    .senderIds(Set.of(1L))
                    .build();
                when(lomClient.searchOrders(eq(filter), eq(Set.of()), any(Pageable.class), eq(false)))
                    .thenReturn(PageResult.of(List.of(lomOrder(PartnerType.DROPSHIP)), 1, 1, 1));
                doReturn(checkouterReturnWithDelivery())
                    .when(checkouterReturnApi)
                    .getReturn(
                        any(RequestClientInfo.class),
                        any(ReturnRequest.class)
                    );
                doReturn(null)
                    .when(mbiApiClient)
                    .getOutlet(
                        anyLong(),
                        anyBoolean()
                    );
                doReturn(Optional.of(pvzLogisticsPointResponse()))
                    .when(lmsClient)
                    .getLogisticsPoint(anyLong());
                doReturn(Optional.of(nonMarketPvzPartnerResponse()))
                    .when(lmsClient)
                    .getPartner(anyLong());

                // when:
                poller.poll();

                // then:
                verify(checkouterReturnApi).getReturn(any(RequestClientInfo.class), any(ReturnRequest.class));
                verify(mbiApiClient).getOutlet(anyLong(), anyBoolean());
                verify(lmsClient).getLogisticsPoint(anyLong());
                verify(lmsClient).getPartner(anyLong());
                verifyZeroInteractions(lmsClient);
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    @Test
    public void createReturnRequestWithoutPickupPointWhenFailedEveryGettingOfPickupPoint() {
        transactionOperations.execute(tc -> {
            try {
                // given:
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/events",
                    "/data/events/return_created.json"
                );
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/167802870",
                    "/data/checkouter/return_order.json"
                );
                OrderSearchFilter filter = OrderSearchFilter.builder()
                    .externalIds(Set.of(String.valueOf(ORDER_ID)))
                    .senderIds(Set.of(1L))
                    .build();
                when(lomClient.searchOrders(eq(filter), eq(Set.of()), any(Pageable.class), eq(false)))
                    .thenReturn(PageResult.of(List.of(lomOrder(PartnerType.DROPSHIP)), 1, 1, 1));
                doReturn(checkouterReturnWithDelivery())
                    .when(checkouterReturnApi)
                    .getReturn(
                        any(RequestClientInfo.class),
                        any(ReturnRequest.class)
                    );
                doThrow(new RuntimeException())
                    .when(mbiApiClient)
                    .getOutlet(
                        anyLong(),
                        anyBoolean()
                    );
                doThrow(new RuntimeException())
                    .when(lmsClient)
                    .getLogisticsPoint(anyLong());

                // when:
                poller.poll();

                // then:
                verify(checkouterReturnApi).getReturn(any(RequestClientInfo.class), any(ReturnRequest.class));
                verify(mbiApiClient).getOutlet(anyLong(), anyBoolean());
                verify(lmsClient).getLogisticsPoint(anyLong());
                verifyZeroInteractions(lmsClient);
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    @Test
    @Sql("/data/repository/returnRequest/return-request-awaiting-for-data.sql")
    public void updateReturnRequest() {
        transactionOperations.execute(tc -> {
            try {
                // given:
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/events",
                    "/data/events/return_status_updated.json"
                );
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/167802870",
                    "/data/checkouter/return_order.json"
                );
                OrderSearchFilter filter = OrderSearchFilter.builder()
                    .externalIds(Set.of(String.valueOf(ORDER_ID)))
                    .senderIds(Set.of(1L))
                    .build();
                when(lomClient.searchOrders(eq(filter), eq(Set.of()), any(Pageable.class), eq(false)))
                    .thenReturn(PageResult.of(List.of(lomOrder(PartnerType.DROPSHIP)), 1, 1, 1));
                doReturn(checkouterReturnWithDelivery())
                    .when(checkouterReturnApi)
                    .getReturn(
                        any(RequestClientInfo.class),
                        any(ReturnRequest.class)
                    );
                doReturn(getDefaultOutlet())
                    .when(mbiApiClient)
                    .getOutlet(
                        anyLong(),
                        anyBoolean()
                    );
                doReturn(List.of(pvzLogisticsPointResponse()))
                    .when(lmsClient)
                    .getLogisticsPoints(any(LogisticsPointFilter.class));
                doReturn(Optional.of(marketPvzPartnerResponse()))
                    .when(lmsClient)
                    .getPartner(anyLong());

                // when:
                poller.poll();

                // then:
                verify(checkouterReturnApi).getReturn(any(RequestClientInfo.class), any(ReturnRequest.class));
                verify(mbiApiClient).getOutlet(anyLong(), anyBoolean());
                verify(lmsClient).getLogisticsPoints(any(LogisticsPointFilter.class));
                verify(lmsClient).getPartner(anyLong());
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    @Test
    @Sql("/data/repository/returnRequest/return-requests.sql")
    @Sql("/data/repository/returnRequestItem/return-request-item.sql")
    public void updateReturnRequest_shouldNotUpdateReturnRequestState() {
        transactionOperations.execute(tc -> {
            try {
                // given:
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/events",
                    "/data/events/commited_return_status_updated.json"
                );
                prepareMockServer(
                    checkouterMockServer,
                    "/orders/167802872",
                    "/data/checkouter/commited_return_order.json"
                );
                OrderSearchFilter filter = OrderSearchFilter.builder()
                    .externalIds(Set.of(String.valueOf(COMMITED_ORDER_ID)))
                    .senderIds(Set.of(1L))
                    .build();
                when(lomClient.searchOrders(eq(filter), eq(Set.of()), any(Pageable.class), eq(false)))
                    .thenReturn(PageResult.of(List.of(lomOrder(PartnerType.DROPSHIP)), 1, 1, 1));
                doReturn(checkouterReturnWithDelivery())
                    .when(checkouterReturnApi)
                    .getReturn(
                        any(RequestClientInfo.class),
                        any(ReturnRequest.class)
                    );
                doReturn(getDefaultOutlet())
                    .when(mbiApiClient)
                    .getOutlet(
                        anyLong(),
                        anyBoolean()
                    );
                doReturn(List.of(pvzLogisticsPointResponse()))
                    .when(lmsClient)
                    .getLogisticsPoints(any(LogisticsPointFilter.class));
                doReturn(Optional.of(marketPvzPartnerResponse()))
                    .when(lmsClient)
                    .getPartner(anyLong());
                var expected = returnRequest(102L)
                    .setReturnId(COMMITED_RETURN_ID_STR)
                    .setBarcode(COMMITED_RETURN_BARCODE_FF)
                    .setExternalOrderId(COMMITED_ORDER_ID)
                    .setRequestDate(LocalDate.of(2021, 1, 2))
                    .setState(ReturnRequestState.FINAL);
                expected.addReturnRequestItem(item(100L, PRICE_1, ITEM_SKU_1, ITEM_SUPPLIER_1));
                pickupPoint().addReturnRequest(expected);

                // when:
                poller.poll();

                // then:
                verify(checkouterReturnApi).getReturn(any(RequestClientInfo.class), any(ReturnRequest.class));
                verify(mbiApiClient).getOutlet(anyLong(), anyBoolean());
                verify(lmsClient).getLogisticsPoints(any(LogisticsPointFilter.class));
                verify(lmsClient).getPartner(anyLong());

                returnRequestRepository.flush();
                var actual = returnRequestRepository.findByReturnId(COMMITED_RETURN_ID_STR);
                softly.assertThat(actual).isPresent();
                softly.assertThat(actual.get()).isEqualToComparingFieldByField(expected);
            } catch (Exception ignored) {
            }
            return null;
        });
    }
}
