package ru.yandex.market.core.param.listener;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioDTO;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod;
import ru.yandex.market.abo.api.entity.checkorder.PlacementType;
import ru.yandex.market.abo.api.entity.checkorder.SelfCheckDTO;
import ru.yandex.market.api.cpa.CPADataPusher;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.ff4shops.FF4ShopsPartnerState;
import ru.yandex.market.core.ff4shops.PartnerFulfillmentLinkForFF4Shops;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.stocks.FF4ShopsClient;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageFFIntervalClient;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.FFIntervalDto;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.param.model.ParamType.CPA_IS_PARTNER_INTERFACE;

@DbUnitDataSet(before = "CpaIsPartnerInterfaceListenerTest.before.csv")
public class CpaIsPartnerInterfaceListenerTest extends FunctionalTest {

    private static final LocalDate LOCAL_DATE = LocalDate.parse("2021-01-01");
    private static final String STOCK_STORAGE_FF_INTERVALS_SUPPLIERS_ALL = "stock.storage.ff.intervals.suppliers.all";

    @Autowired
    private CpaIsPartnerInterfaceListener cpaIsPartnerInterfaceListener;

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private CheckouterShopApi checkouterShopApi;

    @Autowired
    private CPADataPusher cpaDataPusher;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private StockStorageFFIntervalClient stockStorageFFIntervalClient;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private FF4ShopsClient ff4ShopsClient;

    @Autowired
    private MbiBpmnClient mbiBpmnClient;

    @Autowired
    private AboPublicRestClient aboPublicRestClient;

    @Autowired
    Clock clock;

    @Autowired
    FeatureService featureService;

    @Autowired
    private ParamService paramService;

    @BeforeEach
    public void setUp() {
        when(checkouterClient.shops()).thenReturn(checkouterShopApi);
        when(checkouterShopApi.getShopData(anyLong())).thenReturn(ShopMetaData.DEFAULT);

        Clock fixedClock = Clock.fixed(LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
    }

    @Test
    public void testSwitchToAPIShipmentDateCalculationRuleDeleted() {
        long paramId = 1L;
        long entityId = 2L;
        long actionId = 3L;

//        Is API
        BooleanParamValue newParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId, false);
//        Was PI
        BooleanParamValue oldParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId, true);

        cpaIsPartnerInterfaceListener.onChange(newParam, oldParam, actionId);

        verify(cpaDataPusher, times(1))
                .deleteShipmentDateCalculationRule(entityId);

        verify(cpaDataPusher, never())
                .pushShipmentDateCalculationRule(entityId);
    }

    @Test
    public void testSwitchToPIShipmentDateCalculationRuleUpdated() {
        long paramId = 1L;
        long entityId = 2L;
        long actionId = 3L;

//        Is PI
        BooleanParamValue newParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId, true);
//        Was API
        BooleanParamValue oldParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId, false);

        cpaIsPartnerInterfaceListener.onChange(newParam, oldParam, actionId);

        verify(cpaDataPusher, times(1))
                .pushShipmentDateCalculationRule(entityId);

        verify(cpaDataPusher, never())
                .deleteShipmentDateCalculationRule(entityId);
    }

    @Test
    public void testIsNotDsbs() {
        long paramId = 1L;
        long entityId = 10L;
        long actionId = 3L;

//        Is PI
        BooleanParamValue newParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId, true);
//        Was API
        BooleanParamValue oldParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId, false);

        cpaIsPartnerInterfaceListener.onChange(newParam, oldParam, actionId);

        verify(cpaDataPusher, never())
                .pushShipmentDateCalculationRule(entityId);

        verify(cpaDataPusher, never())
                .deleteShipmentDateCalculationRule(entityId);
    }


    @ParameterizedTest
    @DisplayName("Настройка синхронизации стоков не меняется при переключении API/ЛК")
    @CsvSource(value = {
            "true,true",
            "true,false",
            "false,true",
            "false,false"
    })
    public void testSyncStocksNotChangedForDbs(boolean newCpaIsPartnerInterfaceValue, boolean stockSyncEnabled) {
        long paramId = 1L;
        long entityId = 2L;
        long actionId = 3L;

        when(lmsClient.getPartner(entityId))
                .thenReturn(Optional.of(
                        PartnerResponse.newBuilder()
                                .id(entityId)
                                .stockSyncEnabled(stockSyncEnabled)
                                .build()
                ));

        BooleanParamValue newParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId,
                newCpaIsPartnerInterfaceValue);
        BooleanParamValue oldParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId,
                !newCpaIsPartnerInterfaceValue);

        cpaIsPartnerInterfaceListener.onChange(newParam, oldParam, actionId);

        ArgumentCaptor<PartnerSettingDto> capture = ArgumentCaptor.forClass(PartnerSettingDto.class);
        verify(lmsClient, times(1))
                .updatePartnerSettings(eq(entityId), capture.capture());
        PartnerSettingDto value = capture.getValue();
        Assertions.assertEquals(stockSyncEnabled, value.getStockSyncEnabled());
    }

    @Test
    @DisplayName("Создание интервала синхронизации стоков при переключении API->ЛК")
    public void testCreateStockSyncJobInterval() {
        long paramId = 1L;
        long entityId = 11L;
        long feedId = 11L;
        long actionId = 3L;
        int warehouseId = 11;
        long businessId = 1000L;
        String syncJobName = "FullSync";
        boolean isActive = true;
        int batchSize = 500;

        environmentService.setValue(STOCK_STORAGE_FF_INTERVALS_SUPPLIERS_ALL, "true");

        when(stockStorageFFIntervalClient.getSyncJobInterval(syncJobName, warehouseId))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not found", new HttpHeaders(),
                        new byte[]{}, null));

        BooleanParamValue newParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId, true);
        BooleanParamValue oldParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId, false);

        cpaIsPartnerInterfaceListener.onChange(newParam, oldParam, actionId);

        ArgumentCaptor<FFIntervalDto> capture = ArgumentCaptor.forClass(FFIntervalDto.class);

        verify(stockStorageFFIntervalClient, times(1))
                .getSyncJobInterval(syncJobName, warehouseId);
        verify(stockStorageFFIntervalClient, times(1))
                .createSyncJobInterval(capture.capture());
        verifyNoMoreInteractions(stockStorageFFIntervalClient);

        verify(ff4ShopsClient, times(1)).updatePartnerState(
                eq(FF4ShopsPartnerState.newBuilder()
                        .withPartnerId(entityId)
                        .withBusinessId(businessId)
                        .withFeatureStatus(ParamCheckStatus.SUCCESS)
                        .withCpaIsPartnerInterface(true)
                        .withFeatureType(FeatureType.CROSSDOCK)
                        .withFulfillmentLinks(singletonList(PartnerFulfillmentLinkForFF4Shops.newBuilder()
                                .withServiceId(warehouseId)
                                .withFeedId(feedId)
                                .withDeliveryServiceType(DeliveryServiceType.CROSSDOCK)
                                .build()))
                        .withPushStocksIsEnabled(true)
                        .build()));

        FFIntervalDto value = capture.getValue();
        Assertions.assertEquals(warehouseId, value.getWarehouseId());
        Assertions.assertEquals(syncJobName, value.getSyncJobName());
        Assertions.assertEquals(isActive, value.getActive());
        Assertions.assertEquals(batchSize, value.getBatchSize());
    }

    @Test
    @DisplayName("Обновление интервала синхронизации стоков при переключении API->ЛК")
    public void testUpdateStockSyncJobInterval() {
        long paramId = 1L;
        long entityId = 11L;
        long actionId = 3L;
        long syncIntervalId = 123L;
        int warehouseId = 11;
        String syncJobName = "FullSync";
        int syncIntervalValue = 100500;
        boolean isActive = true;
        int batchSize = 500;

        environmentService.setValue(STOCK_STORAGE_FF_INTERVALS_SUPPLIERS_ALL, "true");

        when(stockStorageFFIntervalClient.getSyncJobInterval(syncJobName, warehouseId))
                .thenReturn(new FFIntervalDto(syncIntervalId, warehouseId, syncJobName, syncIntervalValue, isActive,
                        batchSize));

        BooleanParamValue newParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId, true);
        BooleanParamValue oldParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId, false);

        cpaIsPartnerInterfaceListener.onChange(newParam, oldParam, actionId);

        ArgumentCaptor<FFIntervalDto> capture = ArgumentCaptor.forClass(FFIntervalDto.class);

        verify(stockStorageFFIntervalClient, times(1))
                .getSyncJobInterval(syncJobName, warehouseId);
        verify(stockStorageFFIntervalClient, times(1))
                .updateSyncJobInterval(eq(syncIntervalId), capture.capture());
        verifyNoMoreInteractions(stockStorageFFIntervalClient);

        FFIntervalDto value = capture.getValue();
        Assertions.assertEquals(syncIntervalId, value.getId());
        Assertions.assertEquals(warehouseId, value.getWarehouseId());
        Assertions.assertEquals(syncJobName, value.getSyncJobName());
        Assertions.assertEquals(isActive, value.getActive());
        Assertions.assertEquals(batchSize, value.getBatchSize());
    }

    @Test
    @DisplayName("Удаление интервала синхронизации стоков при переключении ЛК->API")
    public void testDeleteStockSyncJobInterval() {
        long paramId = 1L;
        long entityId = 12L;
        long actionId = 3L;
        long syncIntervalId = 123L;
        int warehouseId = 12;
        String syncJobName = "FullSync";
        int syncIntervalValue = 100500;
        boolean isActive = true;
        int batchSize = 500;

        environmentService.setValue(STOCK_STORAGE_FF_INTERVALS_SUPPLIERS_ALL, "true");

        when(stockStorageFFIntervalClient.getSyncJobInterval(syncJobName, warehouseId))
                .thenReturn(new FFIntervalDto(syncIntervalId, warehouseId, syncJobName, syncIntervalValue, isActive,
                        batchSize));

        BooleanParamValue newParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId, false);
        BooleanParamValue oldParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId, true);

        cpaIsPartnerInterfaceListener.onChange(newParam, oldParam, actionId);

        verify(stockStorageFFIntervalClient, times(1))
                .getSyncJobInterval(syncJobName, warehouseId);
        verify(stockStorageFFIntervalClient, times(1))
                .deleteSyncJobInterval(syncIntervalId);
        verifyNoMoreInteractions(stockStorageFFIntervalClient);
    }

    @ParameterizedTest
    @DisplayName("Включение AutoAcceptOrder в ПИ, выключение в ПАПИ")
    @CsvSource(value = {
            "FBS,true,SUCCESS",
            "FBS,false,DONT_WANT",
            "DBS,true,SUCCESS",
            "DBS,false,DONT_WANT"
    })
    public void testAutoAcceptOrderFeatureUpdateOnCpaIsPartnerInterfaceParamValueChange(
            String partnerType,
            boolean isPartnerInterfaceValue,
            String orderAutoAcceptFeatureStatusName
    ) {
        ParamCheckStatus orderAutoAcceptFeatureStatus = ParamCheckStatus.find(orderAutoAcceptFeatureStatusName);

        long entityId = partnerType.equals("FBS") ? 10L : 2L;
        long paramId = 1L;
        long actionId = 3L;

        BooleanParamValue newParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId,
                isPartnerInterfaceValue);
        BooleanParamValue oldParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, entityId,
                !isPartnerInterfaceValue);

        cpaIsPartnerInterfaceListener.onChange(newParam, oldParam, actionId);

        assertThat(featureService.getFeature(entityId, FeatureType.ORDER_AUTO_ACCEPT).getStatus(),
                equalTo(orderAutoAcceptFeatureStatus));
    }

    @Test
    @DbUnitDataSet(after = "CpaIsPartnerInterfaceListenerTest.testEnableSelfCheckInPI2APISwitch.after.csv")
    void testEnableSelfCheckInPI2APISwitch() {
        BooleanParamValue newParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, 1, 2, false);
        BooleanParamValue oldParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, 1, 2, true);

        cpaIsPartnerInterfaceListener.onChange(
                newParam,
                oldParam,
                3
        );
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "CpaIsPartnerInterfaceListenerTest.dsbs.before.csv",
                    "NewDbsSelfcheck.disabled.env.csv"
            },
            after = "CpaIsPartnerInterfaceListenerTest.dsbs.after.csv")
    void testSelfCheckNotRequestedForRegisteredShop() {
        BooleanParamValue newParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, 1, 2, false);
        BooleanParamValue oldParam = new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, 1, 2, true);

        cpaIsPartnerInterfaceListener.onChange(
                newParam,
                oldParam,
                3
        );
    }

    @Test
    @DisplayName("Самопроверка не нужна для магазина, который ее уже проходил")
    @DbUnitDataSet(
            before = {
                    "CpaIsPartnerInterfaceListenerTest.dsbs.pi.before.csv",
                    "NewDbsSelfcheck.enabled.env.csv"
            },
            after = "CpaIsPartnerInterfaceListenerTest.testDbsSelfcheckNotRequired.after.csv"
    )
    void testDbsSelfcheckNotRequired() {
        long shopId = 2L;
        long paramId = 200L;

        List<SelfCheckDTO> scenarios = createSelfCheckScenarioDto(shopId);
        when(aboPublicRestClient.getSelfCheckScenarios(eq(shopId), eq(PlacementType.DSBS), eq(OrderProcessMethod.API)))
                .thenReturn(scenarios);

        paramService.setParam(new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, shopId, false), 123L);

        verify(aboPublicRestClient, times(1))
                .getSelfCheckScenarios(eq(shopId), eq(PlacementType.DSBS), eq(OrderProcessMethod.API));
        verifyNoMoreInteractions(aboPublicRestClient);
    }

    @Test
    @DisplayName("Проверка, что накладывается катоф на магазин в проде, которому нужна самопроверка")
    @DbUnitDataSet(
            before = {
                    "CpaIsPartnerInterfaceListenerTest.dsbs.pi.before.csv",
                    "NewDbsSelfcheck.enabled.env.csv"
            },
            after = "CpaIsPartnerInterfaceListenerTest.testDbsSelfcheckRequiredForEverActivated.after.csv"
    )
    void testDbsSelfcheckRequiredForEverActivated() {
        long shopId = 2L;
        long paramId = 200L;

        when(aboPublicRestClient.getSelfCheckScenarios(eq(shopId), eq(PlacementType.DSBS), eq(OrderProcessMethod.API)))
                .thenReturn(emptyList());

        paramService.setParam(new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, shopId, false), 123L);

        verify(aboPublicRestClient, times(1))
                .getSelfCheckScenarios(eq(shopId), eq(PlacementType.DSBS), eq(OrderProcessMethod.API));
        verifyNoMoreInteractions(aboPublicRestClient);
    }

    @Test
    @DisplayName("Проверка, что накладывается катоф на незарегистрированный магазин, которому нужна самопроверка, и есть datasources_in_testing")
    @DbUnitDataSet(
            before = {
                    "CpaIsPartnerInterfaceListenerTest.testDbsSelfcheckRequiredForNotEverActivated.before.csv",
                    "NewDbsSelfcheck.enabled.env.csv"
            },
            after = {
                    "CpaIsPartnerInterfaceListenerTest.after.csv",
                    "CpaIsPartnerInterfaceListenerTest.testDbsSelfcheckRequiredForNotEverActivated.after.csv"
            }
    )
    void testDbsSelfcheckRequiredForNotEverActivated() {
        long shopId = 700L;
        long paramId = 200L;

        when(aboPublicRestClient.getSelfCheckScenarios(eq(shopId), eq(PlacementType.DSBS), eq(OrderProcessMethod.API)))
                .thenReturn(emptyList());

        paramService.setParam(new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, shopId, false), 123L);

        verify(aboPublicRestClient, times(1))
                .getSelfCheckScenarios(eq(shopId), eq(PlacementType.DSBS), eq(OrderProcessMethod.API));
        verifyNoMoreInteractions(aboPublicRestClient);
    }

    @Test
    @DisplayName("Проверка переключения с API на PI не зарегистрированного магазина")
    @DbUnitDataSet(
            before = {
                    "CpaIsPartnerInterfaceListenerTest.testDbsSelfcheckSwitchToPI.before.csv",
                    "NewDbsSelfcheck.enabled.env.csv"
            },
            after = {
                    "CpaIsPartnerInterfaceListenerTest.after.csv",
                    "CpaIsPartnerInterfaceListenerTest.testDbsSelfcheckSwitchToPI.after.csv"
            }
    )
    void testNewDbsSelfcheckSwitchToPI() {
        long shopId = 700L;
        long paramId = 200L;

        paramService.setParam(new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, shopId, true), 123L);

        verifyNoMoreInteractions(aboPublicRestClient);
    }

    @Test
    @DisplayName("Проверка переключения с API на PI уже размещающегося магазина")
    @DbUnitDataSet(
            before = {
                    "CpaIsPartnerInterfaceListenerTest.testRegisteredDbsSelfcheckSwitchToPI.before.csv",
                    "NewDbsSelfcheck.enabled.env.csv"
            },
            after = {
                    "CpaIsPartnerInterfaceListenerTest.after.csv",
                    "CpaIsPartnerInterfaceListenerTest.testRegisteredDbsSelfcheckSwitchToPI.after.csv"
            }
    )
    void testRegisteredDbsSelfcheckSwitchToPI() {
        long shopId = 700L;
        long paramId = 200L;

        paramService.setParam(new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, shopId, true), 123L);

        verifyNoMoreInteractions(aboPublicRestClient);
    }

    @Test
    @DisplayName("Проверка переключения с API на PI c открытыми катофами")
    @DbUnitDataSet(
            before = {
                    "CpaIsPartnerInterfaceListenerTest.testDbsSwitchToPIWithQualityCutoffs.before.csv",
                    "NewDbsSelfcheck.enabled.env.csv"
            },
            after = {
                    "CpaIsPartnerInterfaceListenerTest.after.csv",
                    "CpaIsPartnerInterfaceListenerTest.testDbsSwitchToPIWithQualityCutoffs.after.csv"
            }
    )
    void testDbsSwitchToPIWithQualityCutoffs() {
        long shopId = 700L;
        long paramId = 200L;

        paramService.setParam(new BooleanParamValue(CPA_IS_PARTNER_INTERFACE, paramId, shopId, true), 123L);

        verifyNoMoreInteractions(aboPublicRestClient);
    }

    private List<SelfCheckDTO> createSelfCheckScenarioDto(long shopId) {
        return List.of(
                new SelfCheckDTO(shopId, CheckOrderScenarioDTO.builder(shopId)
                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                        .build())
        );
    }
}
