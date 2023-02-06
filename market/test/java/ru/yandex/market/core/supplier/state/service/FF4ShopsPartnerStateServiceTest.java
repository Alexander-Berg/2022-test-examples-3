package ru.yandex.market.core.supplier.state.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.ff4shops.FF4ShopsPartnerState;
import ru.yandex.market.core.ff4shops.PartnerFulfillmentLinkForFF4Shops;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageFFIntervalClient;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.FFIntervalDto;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "FF4ShopsPartnerStateServiceTest.before.csv")
class FF4ShopsPartnerStateServiceTest extends FunctionalTest {
    private static final long DROPSHIP_PARTNER_ID = 104;
    private static final long DBS_PARTNER_ID = 107;
    private static final long CROSSDOCK_PARTNER_ID = 108;
    private static final long DEFAULT_BUSINESS_ID = 1010;

    @Autowired
    private FF4ShopsPartnerStateService ff4ShopsPartnerStateService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private ParamService paramService;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private CheckouterShopApi checkouterShopApi;

    @Autowired
    private StockStorageFFIntervalClient stockStorageFFIntervalClient;

    @BeforeEach
    void setUp() {
        when(checkouterAPI.shops()).thenReturn(checkouterShopApi);
    }


    @ParameterizedTest
    @DisplayName("Состояния вычисляются только для определённых типов сервиса доставки.")
    @CsvSource({
            "DROPSHIP,true",
            "CROSSDOCK,true",
            "DROPSHIP_BY_SELLER,true",
            "CARRIER,false",
            "FULFILLMENT,false"
    })
    void testGetPartnerStatesBulkByDeliveryServiceType(DeliveryServiceType deliveryServiceType, boolean isPresent) {
        List<FF4ShopsPartnerState> partnerStates = ff4ShopsPartnerStateService.getPartnerStates(
                Set.of(deliveryServiceType),
                SeekSliceRequest.firstN(10)
        );

        assertNotNull(partnerStates);
        assertEquals(isPresent ? 1 : 0, partnerStates.size());
    }

    @Test
    @DisplayName("Получение состояния DROPSHIP-партнёра")
    void testGetDropshipPartnerState() {
        Optional<FF4ShopsPartnerState> partnerStateOptional = ff4ShopsPartnerStateService.getPartnerState(DROPSHIP_PARTNER_ID);
        assertTrue(partnerStateOptional.isPresent());

        FF4ShopsPartnerState partnerState = partnerStateOptional.get();
        assertEquals(DROPSHIP_PARTNER_ID, partnerState.getPartnerId());
        assertEquals(DEFAULT_BUSINESS_ID, partnerState.getBusinessId());
        assertEquals(ParamCheckStatus.SUCCESS, partnerState.getFeatureStatus());
        assertEquals(FeatureType.DROPSHIP, partnerState.getFeatureType());

        List<PartnerFulfillmentLinkForFF4Shops> expectedFfLinks = List.of(
                PartnerFulfillmentLinkForFF4Shops.newBuilder()
                        .withServiceId(778L)
                        .withFeedId(14L)
                        .withDeliveryServiceType(DeliveryServiceType.DROPSHIP)
                        .build()
        );
        assertThat(partnerState.getFulfillmentLinks(), containsInAnyOrder(expectedFfLinks.toArray()));
    }

    @Test
    @DisplayName("Получение состояния CROSSDOCK-партнёра")
    void testGetCrossdockPartnerState() {
        Optional<FF4ShopsPartnerState> partnerStateOptional = ff4ShopsPartnerStateService.getPartnerState(CROSSDOCK_PARTNER_ID);
        assertTrue(partnerStateOptional.isPresent());

        FF4ShopsPartnerState partnerState = partnerStateOptional.get();
        assertEquals(CROSSDOCK_PARTNER_ID, partnerState.getPartnerId());
        assertEquals(DEFAULT_BUSINESS_ID, partnerState.getBusinessId());
        assertEquals(ParamCheckStatus.SUCCESS, partnerState.getFeatureStatus());
        assertEquals(FeatureType.CROSSDOCK, partnerState.getFeatureType());

        List<PartnerFulfillmentLinkForFF4Shops> expectedFfLinks = List.of(
                PartnerFulfillmentLinkForFF4Shops.newBuilder()
                        .withServiceId(779L)
                        .withFeedId(null)
                        .withDeliveryServiceType(DeliveryServiceType.CROSSDOCK)
                        .build()
        );
        assertThat(partnerState.getFulfillmentLinks(), containsInAnyOrder(expectedFfLinks.toArray()));
    }

    @Test
    @DisplayName("Получение состояния DBS-партнёра")
    void testGetDropshipBySellerPartnerState() {
        Optional<FF4ShopsPartnerState> partnerStateOptional = ff4ShopsPartnerStateService.getPartnerState(DBS_PARTNER_ID);
        assertTrue(partnerStateOptional.isPresent());

        FF4ShopsPartnerState partnerState = partnerStateOptional.get();
        assertEquals(DBS_PARTNER_ID, partnerState.getPartnerId());
        assertEquals(1007L, partnerState.getBusinessId());
        assertEquals(ParamCheckStatus.SUCCESS, partnerState.getFeatureStatus());
        assertEquals(FeatureType.MARKETPLACE_SELF_DELIVERY, partnerState.getFeatureType());

        List<PartnerFulfillmentLinkForFF4Shops> expectedFfLinks = List.of(
                PartnerFulfillmentLinkForFF4Shops.newBuilder()
                        .withServiceId(780L)
                        .withFeedId(null)
                        .withDeliveryServiceType(DeliveryServiceType.DROPSHIP_BY_SELLER)
                        .build()
        );
        assertThat(partnerState.getFulfillmentLinks(), containsInAnyOrder(expectedFfLinks.toArray()));
    }

    @Test
    @DisplayName("Балковая ручка возвращает тот же результат, что и множественный вызов ручки получения состояния партнёра")
    void testGetPartnerStatesBulkResultEqualsToSeparateInvocations() {
        List<FF4ShopsPartnerState> partnerStates = ff4ShopsPartnerStateService.getPartnerStates(
                Set.of(
                        DeliveryServiceType.DROPSHIP,
                        DeliveryServiceType.CROSSDOCK,
                        DeliveryServiceType.DROPSHIP_BY_SELLER
                ),
                SeekSliceRequest.firstN(10)
        );

        assertNotNull(partnerStates);
        assertEquals(3, partnerStates.size());

        for (FF4ShopsPartnerState stateFromBulk : partnerStates) {
            long partnerId = stateFromBulk.getPartnerId();
            Optional<FF4ShopsPartnerState> stateIndividualOpt =
                    ff4ShopsPartnerStateService.getPartnerState(partnerId);
            if (stateIndividualOpt.isEmpty()) {
                fail("No individual state result for partner " + partnerId);
            }

            assertEquals(stateFromBulk, stateIndividualOpt.get());
        }
    }

    @ParameterizedTest
    @DisplayName("Проверка вычисления флага pushStocks в зависимости от флага dsbs.all.")
    @ValueSource(booleans = {true, false})
    void testGetPushStockIsEnabledFlag_dbsAllFlag(boolean cpaIsPartnerInterface) {
        environmentService.setValue("stock.storage.ff.intervals.dsbs.all", "true");

        paramService.setParam(new BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, DROPSHIP_PARTNER_ID, cpaIsPartnerInterface), 100500L);
        paramService.setParam(new BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, DBS_PARTNER_ID, cpaIsPartnerInterface), 100500L);
        paramService.setParam(new BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, CROSSDOCK_PARTNER_ID, cpaIsPartnerInterface), 100500L);

        List<FF4ShopsPartnerState> partnerStates = ff4ShopsPartnerStateService.getPartnerStates(
                Set.of(
                        DeliveryServiceType.DROPSHIP,
                        DeliveryServiceType.CROSSDOCK,
                        DeliveryServiceType.DROPSHIP_BY_SELLER
                ),
                SeekSliceRequest.firstN(10)
        );

        for (FF4ShopsPartnerState partnerState : partnerStates) {

            if (cpaIsPartnerInterface) {
                // Если партнёр работает через PI, флаг pushStocksIsEnabled выставляется в true для DBS-партнёров
                boolean isDbs = partnerState.getPartnerId() == DBS_PARTNER_ID;
                assertEquals(
                        isDbs,
                        partnerState.isPushStocksIsEnabled(),
                        "Wrong value for partner " + partnerState.getPartnerId()
                );
            } else {
                // Если партнёр работает через API, флаг pushStocksIsEnabled всегда false
                assertFalse(
                        partnerState.isPushStocksIsEnabled(),
                        "Wrong value for partner " + partnerState.getPartnerId()
                );
            }
        }
    }

    @ParameterizedTest
    @DisplayName("Проверка вычисления флага pushStocks в зависимости от флага suppliers.all.")
    @ValueSource(booleans = {true, false})
    void testGetPushStockIsEnabledFlag_suppliersAllFlag(boolean cpaIsPartnerInterface) {
        long syncIntervalId = 123L;
        int warehouseId = 12;
        String syncJobName = "FullSync";
        int syncIntervalValue = 100500;
        boolean isActive = true;
        int batchSize = 500;

        environmentService.setValue("stock.storage.ff.intervals.suppliers.all", "true");

        when(stockStorageFFIntervalClient.getSyncJobInterval(anyString(), anyInt()))
                .thenReturn(new FFIntervalDto(syncIntervalId, warehouseId, syncJobName, syncIntervalValue, isActive, batchSize));

        paramService.setParam(new BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, DROPSHIP_PARTNER_ID, cpaIsPartnerInterface), 100500L);
        paramService.setParam(new BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, DBS_PARTNER_ID, cpaIsPartnerInterface), 100500L);
        paramService.setParam(new BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, CROSSDOCK_PARTNER_ID, cpaIsPartnerInterface), 100500L);

        List<FF4ShopsPartnerState> partnerStates = ff4ShopsPartnerStateService.getPartnerStates(
                Set.of(
                        DeliveryServiceType.DROPSHIP,
                        DeliveryServiceType.CROSSDOCK,
                        DeliveryServiceType.DROPSHIP_BY_SELLER
                ),
                SeekSliceRequest.firstN(10)
        );

        for (FF4ShopsPartnerState partnerState : partnerStates) {

            if (cpaIsPartnerInterface) {
                // Если партнёр работает через PI, флаг pushStocksIsEnabled выставляется в true для синих партнёров
                boolean isSupplier = partnerState.getPartnerId() == DROPSHIP_PARTNER_ID
                        || partnerState.getPartnerId() == CROSSDOCK_PARTNER_ID;
                assertEquals(
                        isSupplier,
                        partnerState.isPushStocksIsEnabled(),
                        "Wrong value for partner " + partnerState.getPartnerId()
                );
            } else {
                // Если партнёр работает через API, флаг pushStocksIsEnabled всегда false
                assertFalse(
                        partnerState.isPushStocksIsEnabled(),
                        "Wrong value for partner " + partnerState.getPartnerId()
                );
            }
        }
    }

    @ParameterizedTest
    @DisplayName("Проверка вычисления флага pushStocks в зависимости от white-листа DBS-партнёров.")
    @CsvSource({
            "true,true",
            "true,false",
            "false,true",
            "false,false",
    })
    void testGetPushStockIsEnabledFlag_dbsWhiteList(boolean cpaIsPartnerInterface, boolean isInWhiteList) {
        environmentService.setValues("stock.storage.ff.intervals.dsbs.list", List.of(isInWhiteList ? String.valueOf(DBS_PARTNER_ID) : "-1"));

        paramService.setParam(new BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, DROPSHIP_PARTNER_ID, cpaIsPartnerInterface), 100500L);
        paramService.setParam(new BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, DBS_PARTNER_ID, cpaIsPartnerInterface), 100500L);
        paramService.setParam(new BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, CROSSDOCK_PARTNER_ID, cpaIsPartnerInterface), 100500L);

        List<FF4ShopsPartnerState> partnerStates = ff4ShopsPartnerStateService.getPartnerStates(
                Set.of(
                        DeliveryServiceType.DROPSHIP,
                        DeliveryServiceType.CROSSDOCK,
                        DeliveryServiceType.DROPSHIP_BY_SELLER
                ),
                SeekSliceRequest.firstN(10)
        );

        for (FF4ShopsPartnerState partnerState : partnerStates) {

            if (cpaIsPartnerInterface) {
                // Если партнёр работает через PI, флаг pushStocksIsEnabled выставляется в true для DBS-партнёров из white-листа
                boolean isDbs = partnerState.getPartnerId() == DBS_PARTNER_ID;
                assertEquals(
                        isDbs && isInWhiteList,
                        partnerState.isPushStocksIsEnabled(),
                        "Wrong value for partner " + partnerState.getPartnerId()
                );
            } else {
                // Если партнёр работает через API, флаг pushStocksIsEnabled всегда false
                assertFalse(
                        partnerState.isPushStocksIsEnabled(),
                        "Wrong value for partner " + partnerState.getPartnerId()
                );
            }
        }
    }

    @ParameterizedTest
    @DisplayName("Проверка вычисления флага pushStocks в зависимости от white-листа синих партнёров.")
    @CsvSource({
            "true,true",
            "true,false",
            "false,true",
            "false,false",
    })
    void testGetPushStockIsEnabledFlag_suppliersWhiteList(boolean cpaIsPartnerInterface, boolean isInWhiteList) {
        long syncIntervalId = 123L;
        int warehouseId = 12;
        String syncJobName = "FullSync";
        int syncIntervalValue = 100500;
        boolean isActive = true;
        int batchSize = 500;

        environmentService.setValues(
                "stock.storage.ff.intervals.suppliers.list",
                isInWhiteList ? List.of(String.valueOf(DROPSHIP_PARTNER_ID), String.valueOf(CROSSDOCK_PARTNER_ID)) : List.of("-1")
        );

        when(stockStorageFFIntervalClient.getSyncJobInterval(anyString(), anyInt()))
                .thenReturn(new FFIntervalDto(syncIntervalId, warehouseId, syncJobName, syncIntervalValue, isActive, batchSize));

        paramService.setParam(new BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, DROPSHIP_PARTNER_ID, cpaIsPartnerInterface), 100500L);
        paramService.setParam(new BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, DBS_PARTNER_ID, cpaIsPartnerInterface), 100500L);
        paramService.setParam(new BooleanParamValue(ParamType.CPA_IS_PARTNER_INTERFACE, CROSSDOCK_PARTNER_ID, cpaIsPartnerInterface), 100500L);

        List<FF4ShopsPartnerState> partnerStates = ff4ShopsPartnerStateService.getPartnerStates(
                Set.of(
                        DeliveryServiceType.DROPSHIP,
                        DeliveryServiceType.CROSSDOCK,
                        DeliveryServiceType.DROPSHIP_BY_SELLER
                ),
                SeekSliceRequest.firstN(10)
        );

        for (FF4ShopsPartnerState partnerState : partnerStates) {

            if (cpaIsPartnerInterface) {
                // Если партнёр работает через PI, флаг pushStocksIsEnabled выставляется в true для синих партнёров из white-листа
                boolean isSupplier = partnerState.getPartnerId() == DROPSHIP_PARTNER_ID
                        || partnerState.getPartnerId() == CROSSDOCK_PARTNER_ID;
                assertEquals(
                        isSupplier && isInWhiteList,
                        partnerState.isPushStocksIsEnabled(),
                        "Wrong value for partner " + partnerState.getPartnerId()
                );
            } else {
                // Если партнёр работает через API, флаг pushStocksIsEnabled всегда false
                assertFalse(
                        partnerState.isPushStocksIsEnabled(),
                        "Wrong value for partner " + partnerState.getPartnerId()
                );
            }
        }
    }
}
