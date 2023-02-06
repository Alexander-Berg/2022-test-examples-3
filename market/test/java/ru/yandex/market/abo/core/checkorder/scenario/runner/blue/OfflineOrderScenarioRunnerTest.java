package ru.yandex.market.abo.core.checkorder.scenario.runner.blue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioErrorType;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.ScenarioErrorParam;
import ru.yandex.market.abo.core.checkorder.CheckOrderAttemptService;
import ru.yandex.market.abo.core.checkorder.CheckOrderCreationException;
import ru.yandex.market.abo.core.checkorder.CheckOrderOffersProvider;
import ru.yandex.market.abo.core.checkorder.CheckOrderService;
import ru.yandex.market.abo.core.checkorder.model.CheckOrderScenario;
import ru.yandex.market.abo.core.checkorder.model.OfflineScenarioOrderStatus;
import ru.yandex.market.abo.core.checkorder.model.ScenarioPayload;
import ru.yandex.market.abo.core.cutoff.feature.FeatureCutoff;
import ru.yandex.market.abo.core.cutoff.feature.FeatureStatusManager;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.logistics.lom.model.dto.ShipmentApplicationDto;
import ru.yandex.market.mbi.api.client.entity.CutoffActionStatus;
import ru.yandex.market.mbi.api.client.entity.GenericStatusResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.checkorder.scenario.runner.blue.OfflineOrderScenarioRunner.CREATION_TIMEOUT;
import static ru.yandex.market.abo.core.checkorder.scenario.runner.blue.OfflineOrderScenarioRunner.DAYS_TO_INIT;
import static ru.yandex.market.abo.core.checkorder.scenario.runner.blue.OfflineOrderScenarioRunner.MAX_HOURS_TO_INDEX;
import static ru.yandex.market.abo.core.checkorder.scenario.runner.blue.OfflineOrderScenarioRunner.NO_FEATURE_CUTOFF_ERRORS;
import static ru.yandex.market.abo.core.checkorder.scenario.runner.blue.OfflineOrderScenarioRunner.TIME_TO_WAIT_REPORT_INDEX_FINISH;
import static ru.yandex.market.abo.core.checkorder.scenario.runner.blue.OfflineOrderScenarioRunner.WAIT_AFTER_SHIPMENT;

/**
 * @author artemmz
 * @date 02/12/2019.
 */
class OfflineOrderScenarioRunnerTest {
    private static final Long SHOP_ID = 231312L;
    private static final long USER_ID = 324234L;
    private static final long SHIPMENT_ID = 4235345L;
    private static final Long ORDER_ID = 5342534L;

    @InjectMocks
    OfflineOrderScenarioRunner runner;
    @Mock
    FeatureStatusManager featureStatusManager;
    @Mock
    MbiApiService mbiApiService;
    @Mock
    CheckOrderService checkOrderService;
    @Mock
    CheckOrderOffersProvider checkOrderOffersProvider;
    @Mock
    CheckOrderAttemptService attemptService;
    @Mock
    OfflineScenarioOrderCanceller offlineScenarioOrderCanceller;
    @Mock
    CheckOrderScenario scenario;
    @Mock
    Order order;
    @Mock
    Delivery delivery;
    @Mock
    Parcel parcel;
    @Captor
    ArgumentCaptor<FeatureCutoff> cutoffCaptor;
    @Mock
    OrderHistoryEvent event;
    @Mock
    ClientInfo cancelledClient;
    @Mock
    ShipmentCreator shipmentCreator;
    @Mock
    WarehouseRegionService warehouseRegionService;
    @Mock
    OfflineOrderNotifier offlineOrderNotifier;
    @Mock
    ScenarioPayload payload;

    @BeforeEach
    void setUp() throws CheckOrderCreationException {
        MockitoAnnotations.openMocks(this);
        when(scenario.getCreationTime()).thenReturn(LocalDateTime.now());
        when(scenario.getShopId()).thenReturn(SHOP_ID);
        when(scenario.getPayload()).thenReturn(payload);
        when(order.getUid()).thenReturn(USER_ID);
        when(order.getDelivery()).thenReturn(delivery);
        when(order.getId()).thenReturn(ORDER_ID);
        when(order.getCreationDate()).thenReturn(new Date());
        when(delivery.getParcels()).thenReturn(List.of(parcel));
        when(featureStatusManager.sendResult(any())).thenReturn(GenericStatusResponse.OK_RESPONSE);
        when(checkOrderOffersProvider.findOffersForOrder(any())).thenReturn(List.of(new Offer()));
        when(checkOrderService.createOrder(any(), any())).thenReturn(order);
        when(shipmentCreator.createFrom(any())).thenReturn(ShipmentApplicationDto.builder().id(SHIPMENT_ID).build());
        when(warehouseRegionService.chooseRegionForCheckOrder(anyLong())).thenReturn(Optional.of(Regions.MOSCOW));
    }

    @Test
    void switchOnDropShip() throws CheckOrderCreationException {
        when(mbiApiService.getFeatureStatus(SHOP_ID, FeatureType.DROPSHIP)).thenReturn(ParamCheckStatus.NEW);
        assertFalse(runner.readyToInitOrder(scenario));
        verify(featureStatusManager).sendResult(cutoffCaptor.capture());

        FeatureCutoff cutoff = cutoffCaptor.getValue();
        assertEquals(SHOP_ID, cutoff.getShopId());
        assertEquals(ParamCheckStatus.SUCCESS, cutoff.getStatus());
        assertEquals(FeatureType.DROPSHIP, cutoff.getFeatureType());
        assertTrue(cutoff.isExperiment());
    }

    @Test
    void cannotTurnOnDropship() {
        when(mbiApiService.getFeatureStatus(SHOP_ID, FeatureType.DROPSHIP)).thenReturn(ParamCheckStatus.NEW);
        when(featureStatusManager.sendResult(any())).thenReturn(new GenericStatusResponse(CutoffActionStatus.ERROR, "foo"));
        assertThrows(IllegalStateException.class, () -> runner.readyToInitOrder(scenario));
    }

    @Test
    void initTimeout() {
        when(scenario.getCreationTime()).thenReturn(LocalDateTime.now().minusDays(DAYS_TO_INIT).minusDays(1));
        assertThrows(IllegalStateException.class, () -> runner.readyToInitOrder(scenario));
    }

    @Test
    void reachedTimeout() {
        LocalDate now = LocalDate.now();
        when(order.getCreationDate())
                .thenReturn(DateUtil.asDate(now.minusDays(CREATION_TIMEOUT).plusDays(1)));
        when(parcel.getShipmentDate()).thenReturn(now);
        assertFalse(runner.reachedTimeout(order));

        when(parcel.getShipmentDate()).thenReturn(now.minusDays(WAIT_AFTER_SHIPMENT).minusDays(1));
        assertTrue(runner.reachedTimeout(order));

        when(parcel.getShipmentDate()).thenReturn(null);
        when(order.getCreationDate())
                .thenReturn(DateUtil.asDate(now.minusDays(CREATION_TIMEOUT).minusDays(1)));
        assertTrue(runner.reachedTimeout(order));
    }

    @Test
    void handleTimeout() {
        CheckOrderScenarioStatus resultStatus = runner.handleTimeout(scenario, order);

        assertEquals(CheckOrderScenarioStatus.FAIL, resultStatus);
        verify(scenario).setErrorType(CheckOrderScenarioErrorType.FAIL_BY_TIMEOUT);
        verify(offlineScenarioOrderCanceller).cancelOrderByUser(order, CheckOrderScenarioStatus.FAIL);
        assertFeatureFailed();
    }

    @ParameterizedTest
    @CsvSource({"324234,SUCCESS", "324235,", "324234, CANCELLED"})
    void checkProgress_cancelled(long cancelledUserId, CheckOrderScenarioStatus nextStatus) {
        var scenarioStatusByOrder = mock(OfflineScenarioOrderStatus.class);
        if (nextStatus != null) {
            when(scenarioStatusByOrder.getScenarioNextStatus()).thenReturn(nextStatus);
            when(offlineScenarioOrderCanceller.getScenarioStatusByOrder(order.getId()))
                    .thenReturn(Optional.of(scenarioStatusByOrder));
        } else {
            when(offlineScenarioOrderCanceller.getScenarioStatusByOrder(order.getId())).thenReturn(Optional.empty());
        }
        boolean cancelledByAbo = cancelledUserId == USER_ID;

        when(order.getStatus()).thenReturn(OrderStatus.CANCELLED);
        when(checkOrderService.getOrderHistoryEvents(any(), any(), any())).thenReturn(List.of(event));
        when(event.getAuthor()).thenReturn(cancelledClient);
        when(cancelledClient.getUid()).thenReturn(cancelledUserId);

        CheckOrderScenarioStatus resultStatus = runner.checkProgress(scenario, order);
        assertEquals(cancelledByAbo ? nextStatus : CheckOrderScenarioStatus.FAIL, resultStatus);
        if (resultStatus == CheckOrderScenarioStatus.FAIL) {
            verify(shipmentCreator).cancelShipmentIfAnyCreated(order);
            verify(offlineScenarioOrderCanceller).cancelOrderByUser(order, resultStatus);
            assertFeatureFailed();
        }
    }

    private void assertFeatureFailed() {
        verify(featureStatusManager).sendResult(cutoffCaptor.capture());
        assertNotNull(cutoffCaptor.getValue().getInfo());
        assertNotNull(cutoffCaptor.getValue().getTid());
        FeatureCutoff cutoff = cutoffCaptor.getValue();
        assertEquals(SHOP_ID, cutoff.getShopId());
        assertEquals(ParamCheckStatus.FAIL, cutoff.getStatus());
        assertEquals(FeatureType.DROPSHIP, cutoff.getFeatureType());
        verify(offlineOrderNotifier).sendTgNotification(scenario, false);
    }

    @Test
    void checkProgress_success() {
        when(order.getStatus()).thenReturn(OrderStatus.DELIVERY);
        assertEquals(CheckOrderScenarioStatus.SUCCESS, runner.checkProgress(scenario, order));
        verify(offlineScenarioOrderCanceller).cancelOrderByUser(order, CheckOrderScenarioStatus.SUCCESS);
        verify(offlineOrderNotifier).sendTgNotification(scenario, true);

        verify(featureStatusManager).sendResult(cutoffCaptor.capture());
        FeatureCutoff featureCutoff = cutoffCaptor.getValue();
        assertEquals(SHOP_ID, featureCutoff.getShopId());
        assertEquals(ParamCheckStatus.SUCCESS, featureCutoff.getStatus());
        assertEquals(FeatureType.DROPSHIP, featureCutoff.getFeatureType());
        assertFalse(featureCutoff.isExperiment());
    }

    @Test
    void checkProgress_inProgress() {
        when(order.getStatus()).thenReturn(OrderStatus.PROCESSING);
        assertEquals(CheckOrderScenarioStatus.IN_PROGRESS, runner.checkProgress(scenario, order));
        verifyNoMoreInteractions(featureStatusManager);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void noOffers(boolean noOffersLongerThanTimeout) throws CheckOrderCreationException {
        when(checkOrderOffersProvider.findOffersForOrder(any())).thenReturn(Collections.emptyList());
        doThrow(new CheckOrderCreationException(CheckOrderScenarioErrorType.NO_OFFERS))
                .when(checkOrderOffersProvider).collectNoOfferDetailsAndThrowException(anyLong(), anyBoolean());

        if (noOffersLongerThanTimeout) {
            when(scenario.getCreationTime()).thenReturn(LocalDateTime.now().minusHours(MAX_HOURS_TO_INDEX + 1));
            assertThrows(CheckOrderCreationException.class, () -> runner.hasOffers(scenario));
        } else {
            assertFalse(runner.hasOffers(scenario));
        }
        verify(checkOrderOffersProvider).findOffersForOrder(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void offerDetected(boolean alreadyDetected) throws CheckOrderCreationException {
        when(scenario.getOrCreatePayload())
                .thenReturn(new ScenarioPayload().setOfferDetectionTimestamp(alreadyDetected ? 234L : null));
        assertTrue(runner.hasOffers(scenario));
        verify(scenario, times(alreadyDetected ? 0 : 1)).addOfferDetectionTimestamp(anyLong());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void enoughTimeSinceFirstOffer(boolean enough) {
        long now = new Date().getTime();
        when(payload.getOfferDetectionTimestamp()).thenReturn(enough ? now - TIME_TO_WAIT_REPORT_INDEX_FINISH - 1 : now);
        assertEquals(enough, runner.enoughTimeSinceFirstOffer(scenario));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void revertOnUnknownException(boolean exceededRetryCnt) {
        when(scenario.getFailedAttemptsCnt()).thenReturn(exceededRetryCnt ? Integer.MAX_VALUE : 0);

        runner.handleCheckOrderException(scenario, new IllegalStateException("foo"));
        verify(scenario, times(exceededRetryCnt ? 1 : 0)).setStatus(CheckOrderScenarioStatus.INTERNAL_ERROR);
        verify(attemptService).revertAttempt(SHOP_ID);
        verify(scenario).addFailedAttemptTrace(any());
        verify(offlineOrderNotifier).sendTgNotification(scenario, false);
    }

    @Test
    void initOrder() throws CheckOrderCreationException {
        assertEquals(CheckOrderScenarioStatus.IN_PROGRESS, runner.initOrder(scenario));

        verify(checkOrderService).createOrder(any(), any());
        verify(shipmentCreator).createFrom(any());
        verify(scenario).addShipment(SHIPMENT_ID);
        verify(scenario).setOrderId(ORDER_ID);
        verify(scenario).setOrderCreationTime(any());
    }

    @ParameterizedTest
    @EnumSource(value = CheckOrderScenarioErrorType.class)
    void failScenario(CheckOrderScenarioErrorType errorType) {
        when(scenario.getErrorType()).thenReturn(errorType);
        assertEquals(CheckOrderScenarioStatus.FAIL, runner.failScenario(scenario));
        if (!NO_FEATURE_CUTOFF_ERRORS.contains(errorType)) {
            assertFeatureFailed();
        } else {
            verifyNoMoreInteractions(featureStatusManager);
            verify(offlineOrderNotifier).sendTgNotification(scenario, false);
        }
    }

    @ParameterizedTest(name = "failedShipment_{index}")
    @MethodSource("shipmentExceptions")
    void failedShipment(Throwable ex) throws CheckOrderCreationException {
        when(shipmentCreator.createFrom(any())).thenThrow(ex);

        try {
            runner.initOrder(scenario);
            fail("order should not have been created!");
        } catch (CheckOrderCreationException e) {
            assertEquals(CheckOrderScenarioErrorType.SHIPMENT_NOT_CREATED, e.getScenarioError().getErrorType());
            verify(scenario).setOrderId(anyLong());
            verify(checkOrderService).createCancellationRequest(any(), anyLong(), any(), any(), any(), anyList());
            runner.handleOrderCreationException(scenario, e);
            verify(scenario).setStatus(CheckOrderScenarioStatus.FAIL);
            verify(scenario).setErrorType(CheckOrderScenarioErrorType.SHIPMENT_NOT_CREATED);
            verify(scenario).withErrorDetails(any());
        }
    }

    private static Stream<Arguments> shipmentExceptions() {
        return Stream.of(
                new RuntimeException("unknown exc!"),
                new CheckOrderCreationException(
                        CheckOrderScenarioErrorType.SHIPMENT_NOT_CREATED,
                        ScenarioErrorParam.SHIPMENT_PROBLEMS,
                        "houston, some fields are missing")
        ).map(Arguments::of);
    }

}
