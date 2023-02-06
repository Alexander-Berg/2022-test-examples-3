package ru.yandex.market.mbi.api.controller.onboarding;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Stream;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.api.cpa.yam.service.PrepayRequestService;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.application.meta.PartnerApplicationService;
import ru.yandex.market.core.application.selfemployed.PartnerNpdStatus;
import ru.yandex.market.core.application.selfemployed.SelfEmployedApplication;
import ru.yandex.market.core.application.selfemployed.SelfEmployedApplicationService;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.ff4shops.FF4ShopsPartnerState;
import ru.yandex.market.core.ff4shops.PartnerFulfillmentLinkForFF4Shops;
import ru.yandex.market.core.id.service.MarketIdGrpcService;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.stocks.FF4ShopsClient;
import ru.yandex.market.core.supplier.state.service.FF4ShopsPartnerStateService;
import ru.yandex.market.core.util.LogisticPartnerServiceUtil;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageFFIntervalClient;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.FFIntervalDto;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.open.api.client.model.ApiError;
import ru.yandex.market.mbi.open.api.client.model.DayOfWeekDTO;
import ru.yandex.market.mbi.open.api.client.model.DeliveryServiceTypeDTO;
import ru.yandex.market.mbi.open.api.client.model.LastUploadedFeedInfoDTO;
import ru.yandex.market.mbi.open.api.client.model.OrderProcessingType;
import ru.yandex.market.mbi.open.api.client.model.PartnerAppNpdStatus;
import ru.yandex.market.mbi.open.api.client.model.PartnerContractOption;
import ru.yandex.market.mbi.open.api.client.model.PartnerFulfillmentLinkDTO;
import ru.yandex.market.mbi.open.api.client.model.PartnerModerationResult;
import ru.yandex.market.mbi.open.api.client.model.PartnerModerationResultRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerNpdRequest;
import ru.yandex.market.mbi.open.api.client.model.PartnerOnboardingInfoResponse;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementType;
import ru.yandex.market.mbi.open.api.client.model.ReturnContactDTO;
import ru.yandex.market.mbi.open.api.client.model.ReturnContactTypeDTO;
import ru.yandex.market.mbi.open.api.client.model.ScheduleLineDTO;
import ru.yandex.market.mbi.open.api.client.model.SignatoryDocType;
import ru.yandex.market.mbi.open.api.client.model.TaxSystemDTO;
import ru.yandex.market.mbi.open.api.client.model.TestingStatusDTO;
import ru.yandex.market.mbi.open.api.client.model.TestingTypeDTO;
import ru.yandex.market.mbi.open.api.client.model.VatRateDTO;
import ru.yandex.market.mbi.open.api.client.model.VatSourceDTO;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.entity.type.PartnerStatus.ACTIVE;

class PartnerOnboardingControllerTest extends FunctionalTest {

    private static final String STOCK_STORAGE_FF_INTERVALS_SUPPLIERS_ALL = "stock.storage.ff.intervals.suppliers.all";

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private StockStorageFFIntervalClient stockStorageFFIntervalClient;

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private CheckouterShopApi checkouterShopApi;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private FF4ShopsClient ff4ShopsClient;

    @Autowired
    private FF4ShopsPartnerStateService ff4ShopsPartnerStateService;

    @Autowired
    private PrepayRequestService prepayRequestService;

    @Autowired
    private PartnerApplicationService partnerApplicationService;

    @Autowired
    private MarketIdGrpcService marketIdGrpcService;

    @Autowired
    private SelfEmployedApplicationService selfEmployedApplicationService;

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.getPartnerInfo.before.csv")
    void testPiFbs() {
        var onboardingInfo = getMbiOpenApiClient().getPartnerOnboardingInfo(1);
        Assertions.assertEquals(
                new PartnerOnboardingInfoResponse()
                        .partnerId(1L)
                        .businessId(10L)
                        .partnerPlacementType(PartnerPlacementType.FBS)
                        .orderProcessingType(OrderProcessingType.PI)
                        .isClickAndCollect(false)
                        .addFulfillmentLinksItem(
                                new PartnerFulfillmentLinkDTO()
                                        .deliveryServiceType(DeliveryServiceTypeDTO.DROPSHIP)
                                        .serviceId(11L)
                        ),
                onboardingInfo
        );
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.getPartnerInfo.before.csv")
    void testPiClickAndCollect() {
        var onboardingInfo = getMbiOpenApiClient().getPartnerOnboardingInfo(3);
        Assertions.assertEquals(
                new PartnerOnboardingInfoResponse()
                        .partnerId(3L)
                        .partnerPlacementType(PartnerPlacementType.FBS)
                        .orderProcessingType(OrderProcessingType.PI)
                        .isClickAndCollect(true)
                        .fulfillmentLinks(Collections.emptyList()),
                onboardingInfo
        );
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.getPartnerInfo.before.csv")
    void testApiDbs() {
        var onboardingInfo = getMbiOpenApiClient().getPartnerOnboardingInfo(2);
        Assertions.assertEquals(
                new PartnerOnboardingInfoResponse()
                        .partnerId(2L)
                        .partnerPlacementType(PartnerPlacementType.DBS)
                        .orderProcessingType(OrderProcessingType.API)
                        .isClickAndCollect(false)
                        .fulfillmentLinks(Collections.emptyList()),
                onboardingInfo
        );
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.getLastRequest.before.csv")
    void testGetOnboardingLegalInfo() {
        var legalDataResponse = getMbiOpenApiClient().getPartnerOnboardingLegalData(1);

        assertThat(legalDataResponse.getLastPrepayRequest()).isNotNull();

        var lastRequest = legalDataResponse.getLastPrepayRequest();

        assertThat(lastRequest.getId()).isEqualTo(4);
        assertThat(lastRequest.getDatasourceId()).isEqualTo(1);
        assertThat(lastRequest.getPartnerId()).isEqualTo(1);
        assertThat(lastRequest.getInn()).isEqualTo("7743880975");
        assertThat(lastRequest.getOgrn()).isEqualTo("12345");
        assertThat(lastRequest.getFactAddress()).isEqualTo("factAddr");
        assertThat(lastRequest.getOrganizationName()).isEqualTo("orgName");
        assertThat(lastRequest.getSignatory()).isEqualTo("signatory");
        assertThat(lastRequest.getSignatoryLastName()).isEqualTo("signatory");
        assertThat(lastRequest.getSignatoryDocInfo()).isEqualTo("signatoryDocInfo");
        assertThat(lastRequest.getSignatoryPosition()).isEqualTo("Accountant");
        assertThat(lastRequest.getSignatoryDocType()).isEqualTo(SignatoryDocType.AOA_OR_ENTREPRENEUR);
        assertThat(lastRequest.getKpp()).isEqualTo("1111-2222");
        assertThat(lastRequest.getBankName()).isEqualTo("bankName");
        assertThat(lastRequest.getAccountNumber()).isEqualTo("12345678901234567890");
        assertThat(lastRequest.getCorrAccountNumber()).isEqualTo("12345678901234567890");
        assertThat(lastRequest.getLicenseNum()).isEqualTo("licenseNum");
        assertThat(lastRequest.getPostcode()).isEqualTo("109341");
        assertThat(lastRequest.getPersonId()).isEqualTo(811);
        assertThat(lastRequest.getContractId()).isEqualTo(911);
        assertThat(lastRequest.getLicenseDate()).isEqualTo(OffsetDateTime.of(
                LocalDateTime.parse("2017-11-17T00:00:00"), OffsetDateTime.now().getOffset()));

        var shopVat = legalDataResponse.getShopVat();
        assertThat(shopVat.getDatasourceId()).isEqualTo(1);
        assertThat(shopVat.getTaxSystem()).isEqualTo(TaxSystemDTO.USN);
        assertThat(shopVat.getVatRate()).isEqualTo(VatRateDTO.NO_VAT);
        assertThat(shopVat.getVatSource()).isEqualTo(VatSourceDTO.WEB_AND_FEED);
        assertThat(shopVat.getDeliveryVatRate()).isEqualTo(VatRateDTO.VAT_20);

        var schedule = legalDataResponse.getSchedule();
        assertThat(schedule.getId()).isEqualTo(1L);
        assertThat(schedule.getLines()).hasSize(2)
                .containsExactlyInAnyOrder(
                        new ScheduleLineDTO()
                                .startDay(DayOfWeekDTO.MONDAY)
                                .days(4)
                                .startMinute(540)
                                .minutes(600),
                        new ScheduleLineDTO()
                                .startDay(DayOfWeekDTO.FRIDAY)
                                .days(1)
                                .startMinute(600)
                                .minutes(360)
                );

        var returnContacts = legalDataResponse.getReturnContacts();
        assertThat(returnContacts).hasSize(2)
                .containsExactlyInAnyOrder(
                        new ReturnContactDTO()
                                .datasourceId(1L)
                                .email("vasya@yandex.ru")
                                .firstName("Василий")
                                .secondName("Александрович")
                                .lastName("Теркин")
                                .phoneNumber("+79161002030")
                                .type(ReturnContactTypeDTO.PERSON)
                                .isEnabled(true),
                        new ReturnContactDTO()
                                .datasourceId(1L)
                                .email("vasya@yandex.ru")
                                .firstName("Василий")
                                .secondName("Александрович")
                                .lastName("Теркин")
                                .phoneNumber("+79161002030")
                                .type(ReturnContactTypeDTO.SELF)
                                .isEnabled(false)
                );

        var partnerName = legalDataResponse.getPartnerName();
        assertThat(partnerName).isEqualTo("Бизнес 1");
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.getLastRequest.before.csv")
    void testLastPrepayRequestNotFound() {
        var lastRequestResponse = getMbiOpenApiClient().getPartnerOnboardingLegalData(3);

        assertThat(lastRequestResponse.getLastPrepayRequest()).isNull();
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.getPartnerContractOptions.before.csv")
    void testGetContractsWithoutFrequencies() {
        long partnerId = 1L;
        var response = getMbiOpenApiClient().getPartnerContractOptions(partnerId);

        assertThat(response.getCurrentContractId()).isEqualTo(1000L);

        List<PartnerContractOption> optionList = response.getContractOptions();
        assertThat(optionList).hasSize(2);

        PartnerContractOption info1 = optionList.get(0);
        assertThat(info1.getContractId()).isEqualTo(1000);
        assertThat(info1.getJurName()).isEqualTo("Организация 1");
        assertThat(info1.getContractEid()).isEqualTo("100");

        PartnerContractOption info2 = optionList.get(1);
        assertThat(info2.getContractId()).isEqualTo(2000);
        assertThat(info2.getJurName()).isEqualTo("Организация 2");
        assertThat(info2.getContractEid()).isEqualTo("200");

    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.getLastUploadedFeedInfo.before.csv")
    void testLastUploadedFeedInfoNotFound() {
        long partnerId = 2L;
        var response = getMbiOpenApiClient().getPartnerLastUploadedFeedInfo(partnerId);

        assertThat(response.getLastUploadedFeedInfo()).isNull();
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.getLastUploadedFeedInfo.before.csv")
    void testGetDataFeeds() {
        long partnerId = 6;
        var response = getMbiOpenApiClient().getDataFeedsInfo(partnerId);
        Assertions.assertEquals(
                List.of(
                        new LastUploadedFeedInfoDTO()
                                .id(26L)
                                .url("www.ru")
                                .uploadId(2L)
                ),
                response.getLastDataFeedsInfo()
        );
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.hasOutlets.before.csv")
    void testHasOutlets() {
        var response = getMbiOpenApiClient().hasOutlets(1);
        Assertions.assertTrue(response.getHasOutlets());
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.testModerationResult.before.csv")
    void testGetNullableSelfCheck() {
        var response = getMbiOpenApiClient()
                .getModerationResult(
                        1,
                        new PartnerModerationResultRequest().testingType(TestingTypeDTO.SELF_CHECK)
                );
        Assertions.assertNull(response.getResult());
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.testModerationResult.before.csv")
    void testGetCpaModerationResult() {
        var response = getMbiOpenApiClient()
                .getModerationResult(
                        3,
                        new PartnerModerationResultRequest().testingType(TestingTypeDTO.CPA)
                );
        Assertions.assertEquals(
                new PartnerModerationResult().testingStatus(TestingStatusDTO.INITED).isCancelled(true),
                response.getResult()
        );
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.switchSelfCheck.before.fromApi.dbs.csv",
            after = "PartnerOnboardingControllerTest.switchSelfCheck.after.fromApi.dbs.csv")
    void testSwitchSelfCheckDbsFromApi() {
        getMbiOpenApiClient().switchSelfCheck(2L, false, 1L);
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.switchSelfCheck.before.fromPi.dbs.csv",
            after = "PartnerOnboardingControllerTest.switchSelfCheck.after.fromPi.dbs.csv")
    void testSwitchSelfCheckDbsFromPi() {
        getMbiOpenApiClient().switchSelfCheck(2L, true, 1L);
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.switchSelfCheck.before.fromPi.fbs.csv",
            after = "PartnerOnboardingControllerTest.switchSelfCheck.after.fromPi.fbs.csv")
    void testSwitchSelfCheckFbsFromPi() {
        getMbiOpenApiClient().switchSelfCheck(11L, true, 1L);
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.switchSelfCheck.before.fromApi.fbs.csv",
            after = "PartnerOnboardingControllerTest.switchSelfCheck.after.fromApi.fbs.csv")
    void testSwitchSelfCheckFbsFromApi() {
        getMbiOpenApiClient().switchSelfCheck(11L, false, 1L);
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.updateSyncStocks.before.csv")
    public void testSyncStocksFromPi() {
        long partnerId = 12L;
        when(lmsClient.getPartner(eq(partnerId)))
                .thenReturn(LogisticPartnerServiceUtil.getMockedLmsResponse(false, ACTIVE));
        ArgumentCaptor<PartnerSettingDto> capture = ArgumentCaptor.forClass(PartnerSettingDto.class);

        getMbiOpenApiClient().updateStockSettings(partnerId, true, 1L);

        verify(lmsClient).getPartner(partnerId);
        verify(lmsClient).updatePartnerSettings(same(12L), capture.capture());
        verify(nesuClient).setStockSyncStrategy(12L, 12L, true);

        PartnerSettingDto settingValue = capture.getValue();
        assertNotNull(settingValue);
        assertFalse(settingValue.getAutoSwitchStockSyncEnabled());
        assertTrue(settingValue.getKorobyteSyncEnabled());
        assertTrue(settingValue.getStockSyncEnabled());
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.updateSyncStocks.before.csv")
    public void testSyncStocksFromApi() {
        long partnerId = 2L;
        when(lmsClient.getPartner(eq(partnerId)))
                .thenReturn(LogisticPartnerServiceUtil.getMockedLmsResponse(false, ACTIVE));
        ArgumentCaptor<PartnerSettingDto> capture = ArgumentCaptor.forClass(PartnerSettingDto.class);

        getMbiOpenApiClient().updateStockSettings(partnerId, false, 1L);

        verify(lmsClient).getPartner(partnerId);
        verify(lmsClient).updatePartnerSettings(same(12L), capture.capture());
        verify(nesuClient).setStockSyncStrategy(12L, 2L, false);

        PartnerSettingDto settingValue = capture.getValue();
        assertNotNull(settingValue);
        assertTrue(settingValue.getAutoSwitchStockSyncEnabled());
        assertFalse(settingValue.getKorobyteSyncEnabled());
        assertFalse(settingValue.getStockSyncEnabled());
    }

    @Test
    @DisplayName("Создание интервала синхронизации стоков при переключении API->ЛК")
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.updateStockSyncIntervals.before.csv")
    public void testCreateStockSyncJobInterval() {
        long partnerId = 11L;
        int warehouseId = 11;
        String syncJobName = "FullSync";
        boolean isActive = true;
        int batchSize = 500;

        environmentService.setValue(STOCK_STORAGE_FF_INTERVALS_SUPPLIERS_ALL, "true");

        when(stockStorageFFIntervalClient.getSyncJobInterval(syncJobName, warehouseId))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, "Not found", new HttpHeaders(),
                        new byte[]{}, null));

        getMbiOpenApiClient().updateStockIntervals(partnerId, true, 1L);

        ArgumentCaptor<FFIntervalDto> capture = ArgumentCaptor.forClass(FFIntervalDto.class);

        verify(stockStorageFFIntervalClient, times(1))
                .getSyncJobInterval(syncJobName, warehouseId);
        verify(stockStorageFFIntervalClient, times(1))
                .createSyncJobInterval(capture.capture());
        verifyNoMoreInteractions(stockStorageFFIntervalClient);

        FFIntervalDto value = capture.getValue();
        Assertions.assertEquals(warehouseId, value.getWarehouseId());
        Assertions.assertEquals(syncJobName, value.getSyncJobName());
        Assertions.assertEquals(isActive, value.getActive());
        Assertions.assertEquals(batchSize, value.getBatchSize());
    }

    @Test
    @DisplayName("Обновление интервала синхронизации стоков при переключении API->ЛК")
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.updateStockSyncIntervals.before.csv")
    public void testUpdateStockSyncJobInterval() {
        long partnerId = 11L;
        long syncIntervalId = 123L;
        int warehouseId = 11;
        String syncJobName = "FullSync";
        int syncIntervalValue = 100500;
        boolean isActive = true;
        int batchSize = 500;

        environmentService.setValue(STOCK_STORAGE_FF_INTERVALS_SUPPLIERS_ALL, "true");

        when(stockStorageFFIntervalClient.getSyncJobInterval(syncJobName, warehouseId))
                .thenReturn(new FFIntervalDto(syncIntervalId, warehouseId, syncJobName, syncIntervalValue, isActive,
                        batchSize
                ));

        getMbiOpenApiClient().updateStockIntervals(partnerId, true, 1L);

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
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.updateStockSyncIntervals.before.csv")
    public void testDeleteStockSyncJobInterval() {
        long partnerId = 12L;
        long syncIntervalId = 123L;
        int warehouseId = 12;
        String syncJobName = "FullSync";
        int syncIntervalValue = 100500;
        boolean isActive = true;
        int batchSize = 500;

        environmentService.setValue(STOCK_STORAGE_FF_INTERVALS_SUPPLIERS_ALL, "true");

        when(stockStorageFFIntervalClient.getSyncJobInterval(syncJobName, warehouseId))
                .thenReturn(new FFIntervalDto(syncIntervalId, warehouseId, syncJobName, syncIntervalValue, isActive,
                        batchSize
                ));

        getMbiOpenApiClient().updateStockIntervals(partnerId, false, 1L);

        verify(stockStorageFFIntervalClient, times(1))
                .getSyncJobInterval(syncJobName, warehouseId);
        verify(stockStorageFFIntervalClient, times(1))
                .deleteSyncJobInterval(syncIntervalId);
        verifyNoMoreInteractions(stockStorageFFIntervalClient);
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.updateShipmentDateCalculationRule.before.csv")
    public void testSwitchToAPIShipmentDateCalculationRuleDeleted() {
        long entityId = 2L;

        when(checkouterClient.shops()).thenReturn(checkouterShopApi);
        when(checkouterShopApi.getShopData(anyLong())).thenReturn(ShopMetaData.DEFAULT);

        getMbiOpenApiClient().updateShipmentDateCalculationRule(entityId, false, 1L);

        verify(checkouterClient.shops(), times(1))
                .deleteShipmentDateCalculationRule(entityId);

        verify(checkouterClient.shops(), never())
                .saveShipmentDateCalculationRules(eq(entityId), any());
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.updateShipmentDateCalculationRule.before.csv")
    public void testSwitchToPIShipmentDateCalculationRuleUpdated() {
        long entityId = 2L;

        when(checkouterClient.shops()).thenReturn(checkouterShopApi);
        when(checkouterShopApi.getShopData(anyLong())).thenReturn(ShopMetaData.DEFAULT);

        getMbiOpenApiClient().updateShipmentDateCalculationRule(entityId, true, 1L);

        verify(checkouterClient.shops(), times(1))
                .saveShipmentDateCalculationRules(eq(entityId), any());

        verify(checkouterClient.shops(), never())
                .deleteShipmentDateCalculationRule(entityId);
    }

    @ParameterizedTest
    @DisplayName("Включение AutoAcceptOrder в ПИ, выключение в ПАПИ")
    @CsvSource(value = {
            "FBS,true,SUCCESS",
            "FBS,false,DONT_WANT",
            "DBS,true,SUCCESS",
            "DBS,false,DONT_WANT"
    })
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.updateOrderAutoAcceptFeature.before.csv")
    public void testUpdateOrderProcessingCpaIsPartnerInterfaceParamValueChange(
            String partnerType,
            boolean isPartnerInterface,
            String updateOrderProcessingName
    ) {
        ParamCheckStatus updateOrderProcessingStatus = ParamCheckStatus.find(updateOrderProcessingName);

        long partnerId = partnerType.equals("FBS") ? 10L : 2L;

        getMbiOpenApiClient().updateOrderProcessing(partnerId, isPartnerInterface, 1L);

        MatcherAssert.assertThat(
                featureService.getFeature(partnerId, FeatureType.ORDER_AUTO_ACCEPT).getStatus(),
                equalTo(updateOrderProcessingStatus)
        );
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.updateFf4shopsParent.before.csv")
    public void testUpdateFf4shopsPartner() {
        long partnerId = 11L;
        long feedId = 11L;
        int warehouseId = 11;
        long businessId = 1000L;

        environmentService.setValue(STOCK_STORAGE_FF_INTERVALS_SUPPLIERS_ALL, "true");

        getMbiOpenApiClient().updateFf4ShopsPartner(11L, 1L);

        verify(ff4ShopsClient, times(1)).updatePartnerState(
                eq(FF4ShopsPartnerState.newBuilder()
                        .withPartnerId(partnerId)
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
    }

    @ParameterizedTest
    @MethodSource
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.testSaveNpdSelfEmployed.before.csv")
    void testSaveNpdSelfEmployed(long partnerId,
                                 String inn,
                                 PartnerAppNpdStatus appNpdStatus,
                                 PartnerNpdStatus npdStatus,
                                 ParamCheckStatus marketplaceStatus) {
        PartnerNpdRequest request = new PartnerNpdRequest();
        request.setCheckResult(appNpdStatus);
        request.setInn(inn);
        Mockito.doReturn(OptionalLong.of(1L)).when(marketIdGrpcService).getOrCreateMarketId(anyLong(), anyBoolean());
        getMbiOpenApiClient().saveNpdSelfEmployed(partnerId, 123, request);

        PrepayRequest lastRequest = prepayRequestService.findLastRequest(partnerId);
        PartnerNpdStatus actualStatus = selfEmployedApplicationService.getApplication(lastRequest.getId())
                .map(SelfEmployedApplication::getStatus)
                .orElse(null);
        Assertions.assertEquals(featureService.getFeature(partnerId, FeatureType.MARKETPLACE).getStatus(),
                                marketplaceStatus);
        Assertions.assertEquals(inn, lastRequest.getInn());
        Assertions.assertEquals(npdStatus, actualStatus);
    }

    @Test
    void testSaveNpdSelfEmployedNoRequests() {
        PartnerNpdRequest request = new PartnerNpdRequest();
        request.setCheckResult(PartnerAppNpdStatus.DONE);
        request.setInn("123456789");

        var exception = Assertions.assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().saveNpdSelfEmployed(1001, 123, request)
        );
        Assertions.assertEquals(
                ApiError.MessageCodeEnum.INCORRECT_PARTNER_ID,
                exception.getApiError().getMessageCode()
        );
    }

    @Test
    @DbUnitDataSet(before = "PartnerOnboardingControllerTest.testSaveNpdPartnerIsDeleted.before.csv")
    void testSaveNpdPartnerIsDeleted() {
        PartnerNpdRequest request = new PartnerNpdRequest();
        request.setCheckResult(PartnerAppNpdStatus.DONE);
        request.setInn("123456789");

        var exception = Assertions.assertThrows(
                MbiOpenApiClientResponseException.class,
                () -> getMbiOpenApiClient().saveNpdSelfEmployed(1001, 123, request)
        );
        Assertions.assertEquals(
                ApiError.MessageCodeEnum.PARTNER_IS_DELETED,
                exception.getApiError().getMessageCode()
        );
    }

    static Stream<Arguments> testSaveNpdSelfEmployed() {
        return Stream.of(
               Arguments.of(
                       1000002L,
                       "123123123",
                       PartnerAppNpdStatus.NEW,
                       PartnerNpdStatus.NEW,
                       ParamCheckStatus.FAIL
               ),
                Arguments.of(
                        1000003L,
                        "780888088803",
                        PartnerAppNpdStatus.DONE,
                        PartnerNpdStatus.DONE,
                        ParamCheckStatus.FAIL
                ),
                Arguments.of(
                        1000004L,
                        "780888088804",
                        PartnerAppNpdStatus.PENDING,
                        PartnerNpdStatus.PENDING,
                        ParamCheckStatus.FAIL
                ),
                Arguments.of(
                        1000005L,
                        "780888088805",
                        PartnerAppNpdStatus.DONE,
                        PartnerNpdStatus.DONE,
                        ParamCheckStatus.SUCCESS
                ),
                Arguments.of(
                        1000006L,
                        "780888088806",
                        PartnerAppNpdStatus.DONE,
                        PartnerNpdStatus.DONE,
                        ParamCheckStatus.FAIL
                ),
                Arguments.of(
                        1000007L,
                        "780888088807",
                        PartnerAppNpdStatus.DONE,
                        PartnerNpdStatus.DONE,
                        ParamCheckStatus.FAIL
                ),
                Arguments.of(
                        1000008L,
                        "780888088808",
                        PartnerAppNpdStatus.DONE,
                        PartnerNpdStatus.DONE,
                        ParamCheckStatus.FAIL
                ),
                Arguments.of(
                        1000009L,
                        "780888088809",
                        PartnerAppNpdStatus.DONE,
                        PartnerNpdStatus.DONE,
                        ParamCheckStatus.FAIL
                ),
                Arguments.of(
                        1000010L,
                        "780888088810",
                        PartnerAppNpdStatus.DONE,
                        PartnerNpdStatus.DONE,
                        ParamCheckStatus.FAIL
                ),
                Arguments.of(
                        1000002L,
                        "780888088802",
                        PartnerAppNpdStatus.DONE,
                        PartnerNpdStatus.DONE,
                        ParamCheckStatus.FAIL
                ),
                Arguments.of(
                        1000011L,
                        "780888088811",
                        PartnerAppNpdStatus.DONE,
                        PartnerNpdStatus.DONE,
                        ParamCheckStatus.FAIL
                )
        );
    }
}
