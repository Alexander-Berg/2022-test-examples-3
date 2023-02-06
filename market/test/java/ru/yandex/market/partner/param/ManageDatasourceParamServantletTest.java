package ru.yandex.market.partner.param;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;

import ru.yandex.market.abo.api.client.AboAPI;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioDTO;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod;
import ru.yandex.market.abo.api.entity.checkorder.PlacementType;
import ru.yandex.market.abo.api.entity.checkorder.SelfCheckDTO;
import ru.yandex.market.api.cpa.CPADataPusher;
import ru.yandex.market.api.cpa.CpaIsPartnerInterfaceSyncService;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.PushPartnerStatus;
import ru.yandex.market.core.param.model.UnitedCatalogStatus;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.model.ShopWithSendersDto;
import ru.yandex.market.logistics.nesu.client.model.filter.ShopWithSendersFilter;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.bpmn.client.model.ProcessInstanceRequest;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStatus;
import ru.yandex.market.mbi.bpmn.client.model.ProcessType;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты на http метод manageParam, {@link ManageDatasourceParamServantlet}.
 *
 * @author stani on 30.07.18.
 */
class ManageDatasourceParamServantletTest extends FunctionalTest {
    @Autowired
    private AboAPI aboPublicRestClient;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private NesuClient nesuClient;
    @Autowired
    private PushApi pushApiClient;
    @Autowired
    private ParamService paramService;
    @Autowired
    CPADataPusher cpaDataPusher;
    @Autowired
    private CheckouterAPI checkouterClient;
    private CheckouterShopApi checkouterShopApi;
    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private MbiBpmnClient mbiBpmnClient;


    @BeforeEach
    void initMock() {
        checkouterShopApi = mock(CheckouterShopApi.class);
        doReturn(checkouterShopApi).when(checkouterClient).shops();
    }

    private static Stream<Arguments> shopNames() {
        return Stream.of(
                arguments("http://test.org", false),
                arguments("test.org", true),
                arguments("http://www.org.ru", false),
                arguments("www.org.ru", true),
                arguments("https://test.org", false),
                arguments("https://www.test.org", false),
                arguments(StringUtils.repeat("a", 50) + ".ru", false),
                arguments("ООО Ромашка", true),
                arguments("www.yandex.boutique", true),
                arguments("yandex.boutique", true),
                arguments("abc", true),
                arguments("abc.2 абв-1_1 \"test\"", true),
                arguments("yandex.http://boutique", false),
                arguments("yandex.https://boutique", false),
                arguments("yandex.boutique~1", false),
                arguments("yandex.boutique^1", false),
                arguments("yandex.boutique$1", false),
                arguments("yandex.boutique%1", false),
                arguments("яндекс.москва", true),
                arguments("яндекс.москва!", true),
                arguments("яндекс,москва", true),
                arguments("proto&zelenograd.ru", true)
        );
    }

    @Test
    @DbUnitDataSet(before = "ManageDatasourceParamServantletTest.before.csv")
    void testManageDatasourceParamServantlet() {
        var response = FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=CPA_IS_PARTNER_INTERFACE&value=1", 101L);
        JsonTestUtil.assertEquals(response, getClass(), "/mvc/param/managerDatasourceParam.json");
        verify(cpaDataPusher).pushShopInfoToCheckout(1L);
    }

    @Test
    @DbUnitDataSet(before = "ManageDatasourceParamServantletTest.before.csv")
    void testManagePartnerInterfaceWithConfig() {
        var response = FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=CPA_IS_PARTNER_INTERFACE&value=1", 201L);
        JsonTestUtil.assertEquals(response, getClass(), "/mvc/param/managerSupplierParam.json");

        var captorSettings = ArgumentCaptor.forClass(Settings.class);
        verify(pushApiClient).settings(eq(2L), captorSettings.capture(), eq(false));
        assertThat(captorSettings.getValue().getPartnerInterface()).isTrue();

        clearInvocations(pushApiClient);
        FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=CPA_IS_PARTNER_INTERFACE&value=0", 201L);
        verify(pushApiClient).settings(eq(2L), captorSettings.capture(), eq(false));
        verify(cpaDataPusher, times(2)).pushShopInfoToCheckout(2L);
        assertThat(captorSettings.getValue().getPartnerInterface()).isFalse();
        assertThat(captorSettings.getValue().getUrlPrefix()).isEqualTo("http://shop1.com");
        assertThat(captorSettings.getValue().getAuthToken()).isEqualTo("1111");
        assertThat(DataType.XML).isEqualTo(captorSettings.getValue().getDataType());
        assertThat(AuthType.URL).isEqualTo(captorSettings.getValue().getAuthType());
    }

    @Test
    @DbUnitDataSet(before = "ManageDatasourceParamServantletTest.before.csv")
    void testManagePartnerInterfaceWithoutConfig() {
        FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=CPA_IS_PARTNER_INTERFACE&value=1", 301L);

        var captorSettings = ArgumentCaptor.forClass(Settings.class);
        verify(pushApiClient).settings(eq(3L), captorSettings.capture(), eq(false));
        assertThat(captorSettings.getValue().getPartnerInterface()).isTrue();

        clearInvocations(pushApiClient);
        FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=CPA_IS_PARTNER_INTERFACE&value=0", 301L);
        verify(pushApiClient, never()).settings(anyLong(), any(), anyBoolean());
        verify(cpaDataPusher, times(2)).pushShopInfoToCheckout(3L);
    }

    @Test
    @DbUnitDataSet(before = "ManageDatasourceParamServantletTest.before.csv")
    void testManagePartnerInterfaceWithBpmn() {
        environmentService.setValue(CpaIsPartnerInterfaceSyncService.CHANGE_ORDER_PROCESSING_METHOD_ENABLED, "true");
        Mockito.when(mbiBpmnClient.postProcess(Mockito.eq(
                new ProcessInstanceRequest()
                        .processType(ProcessType.CHANGE_ORDER_PROCESSING_METHOD)
                        .params(Map.of(
                                "uid", "100",
                                "partnerId", "3",
                                "isPartnerInterface", "true",
                                "operationId", "1",
                                "partnerInterface", "true"
                        ))
        ))).thenReturn(
                new ProcessStartResponse().records(List.of(
                        (ProcessStartInstance) new ProcessStartInstance()
                                .started(true)
                                .processInstanceId("id")
                                .status(ProcessStatus.ACTIVE)
                ))
        );

        FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=CPA_IS_PARTNER_INTERFACE&value=1&_user_id=100",
                301L
        );

        Mockito.verify(mbiBpmnClient)
                .postProcess(
                        Mockito.eq(
                                new ProcessInstanceRequest()
                                        .processType(ProcessType.CHANGE_ORDER_PROCESSING_METHOD)
                                        .params(Map.of(
                                                "uid", "100",
                                                "partnerId", "3",
                                                "isPartnerInterface", "true",
                                                "operationId", "1",
                                                "partnerInterface", "true"
                                        ))
                        )
                );
    }

    @ParameterizedTest
    @MethodSource("shopNames")
    @DbUnitDataSet(before = "ManageDatasourceParamServantletTest.before.csv")
    @DisplayName("Проверка смены имени магазина")
    void testChangeShopName(String name, boolean expected) {
        var response = FunctionalTestHelper.put(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=SHOP_NAME&value={shopName}",
                new HttpEntity(null), 101L, name);

        assertThat(response.getBody()).isNotNull();
        var responseErrorsArray = JsonTestUtil.parseJson(response.getBody())
                .getAsJsonObject()
                .get("errors")
                .getAsJsonArray();
        if (!expected) {
            assertThat(responseErrorsArray.size()).as("Нет ошибки валидации").isEqualTo(1);
            var errorMessage = responseErrorsArray
                    .get(0)
                    .getAsJsonObject()
                    .get("message")
                    .getAsString();
            assertThat(errorMessage).isEqualTo("wrong-shopname");
        } else {
            assertThat(responseErrorsArray.size()).isEqualTo(0);
        }
    }

    @Test
    @DbUnitDataSet(before = "ChangePiApiParam.before.csv",
            after = "DropshipChangePItoAPI.firstTime.after.csv")
    @DisplayName("Отправка на самопроверку дропшип-поставщика при переключении на АПИ")
    void switchPIOff() {
        when(aboPublicRestClient.getSelfCheckScenarios(any(Long.class), eq(PlacementType.DSBB),
                eq(OrderProcessMethod.API)
        )).thenReturn(List.of(new SelfCheckDTO(1L,
                CheckOrderScenarioDTO.builder(1L).withStatus(CheckOrderScenarioStatus.IN_PROGRESS).build())));

        var partnerResponse = generatePartnerResponse(1001L);
        when(lmsClient.getPartner(anyLong())).thenReturn(
                Optional.of(partnerResponse));
        FunctionalTestHelper.get(baseUrl + "/manageParam?type={type}&value={value}&id={id}",
                ParamType.CPA_IS_PARTNER_INTERFACE.ordinal(),
                false,
                1);

        //CpaIsPartnerInterfaceListener LmsPartnerStateListener
        verify(lmsClient, times(2)).getPartner(1001L);
        verify(lmsClient, times(1)).updatePartnerSettings(1001L,
                PartnerSettingDto.newBuilder()
                        .locationId(partnerResponse.getLocationId())
                        .trackingType(partnerResponse.getTrackingType())
                        .stockSyncEnabled(false)
                        .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                        .autoSwitchStockSyncEnabled(true)
                        .korobyteSyncEnabled(partnerResponse.getKorobyteSyncEnabled())
                        .build());
        //CpaIsPartnerInterfaceListener LmsPartnerStateListener
        verify(nesuClient, times(2)).setStockSyncStrategy(1001L, 1L, false);
    }

    @Test
    @DbUnitDataSet(before = "ChangePiApiParam.before.csv",
            after = "DropshipChangePItoAPI.secondTime.after.csv")
    @DisplayName("Не отправляем на самопроверку дропшип постащика, если он уже проходил самопроверку по этой " +
            "модели")
    void switchPIOff_NoSelfCheck() {
        when(aboPublicRestClient.getSelfCheckScenarios(any(Long.class), eq(PlacementType.DSBB),
                eq(OrderProcessMethod.API)
        )).thenReturn(List.of(new SelfCheckDTO(1L,
                CheckOrderScenarioDTO.builder(1L).withStatus(CheckOrderScenarioStatus.SUCCESS).build())));

        var partnerResponse = generatePartnerResponse(1001L);
        when(lmsClient.getPartner(anyLong())).thenReturn(
                Optional.of(partnerResponse));
        FunctionalTestHelper.get(baseUrl + "/manageParam?type={type}&value={value}&id={id}",
                ParamType.CPA_IS_PARTNER_INTERFACE.ordinal(),
                false,
                1);

        assertThat(featureService.getCutoff(1L, FeatureType.DROPSHIP, FeatureCutoffType.EXPERIMENT)).isNotPresent();
    }

    @Test
    @DbUnitDataSet(before = "ChangePiApiParam.before.csv")
    @DisplayName("Проверка, что кроссдок-поставщик не уйдет на самопроверку при переключении на ПИ")
    void switchPIOn() {
        FunctionalTestHelper.get(baseUrl + "/manageParam?type={type}&value={value}&id={id}",
                ParamType.CPA_IS_PARTNER_INTERFACE.ordinal(),
                true,
                2);
        assertThat(ParamCheckStatus.SUCCESS).isEqualTo(featureService.getFeature(2, FeatureType.CROSSDOCK).getStatus());
    }

    @Test
    @DbUnitDataSet(before = "ChangePiApiParam.before.csv")
    @DisplayName("Проверить lms действия на переключении на режим ПИ")
    void checkLmsActionsOnswitchPIOn() {
        var partnerResponse = generatePartnerResponse(1003L);
        when(lmsClient.getPartner(anyLong())).thenReturn(
                Optional.of(partnerResponse));
        FunctionalTestHelper.get(baseUrl + "/manageParam?type={type}&value={value}&id={id}",
                ParamType.CPA_IS_PARTNER_INTERFACE.ordinal(),
                true,
                3);

        //CpaIsPartnerInterfaceListener LmsPartnerStateListener
        verify(lmsClient, times(2)).updatePartnerSettings(1003L,
                PartnerSettingDto.newBuilder()
                        .locationId(partnerResponse.getLocationId())
                        .trackingType(partnerResponse.getTrackingType())
                        .stockSyncEnabled(true)
                        .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                        .autoSwitchStockSyncEnabled(false)
                        .korobyteSyncEnabled(partnerResponse.getKorobyteSyncEnabled())
                        .build());
        verify(nesuClient, times(2)).setStockSyncStrategy(1003L, 3L, true);
    }

    @Test
    @DbUnitDataSet(before = "ChangePiApiParam.before.csv")
    @DisplayName("Проверить lms действия на переключении на режим ПИ при статусе DROPSHIP=DONT_WANT")
    void checkDropshipDontWant() {
        long partnerId = 4L;
        long marketId = 1000892L;
        var partnerResponse = generatePartnerResponse(1004L);
        when(lmsClient.getPartner(anyLong())).thenReturn(
                Optional.of(partnerResponse));
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            var marketAccount = MarketAccount.newBuilder().setMarketId(marketId).build();
            var response = GetByPartnerResponse.newBuilder()
                    .setMarketId(marketAccount).setSuccess(true).build();

            marketAccountStreamObserver.onNext(response);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(any(), any());
        doAnswer(invocation -> List.of(ShopWithSendersDto.builder().id(partnerId).marketId(marketId).build()))
                .when(nesuClient).searchShopWithSenders(eq(ShopWithSendersFilter.builder()
                    .shopIds(Collections.singleton(partnerId)).build()));

        FunctionalTestHelper.get(baseUrl + "/manageParam?type={type}&value={value}&id={id}",
                ParamType.CPA_IS_PARTNER_INTERFACE.ordinal(),
                true,
                4);
        verify(lmsClient, times(1)).updatePartnerSettings(1004L,
                PartnerSettingDto.newBuilder()
                        .locationId(partnerResponse.getLocationId())
                        .trackingType(partnerResponse.getTrackingType())
                        .stockSyncEnabled(false)
                        .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                        .autoSwitchStockSyncEnabled(false)
                        .korobyteSyncEnabled(false)
                        .build());
        FunctionalTestHelper.get(baseUrl + "/manageParam?type={type}&value={value}&id={id}",
                ParamType.CPA_IS_PARTNER_INTERFACE.ordinal(),
                false,
                4);
        verify(lmsClient, times(1)).updatePartnerSettings(1004L,
                PartnerSettingDto.newBuilder()
                        .locationId(partnerResponse.getLocationId())
                        .trackingType(partnerResponse.getTrackingType())
                        .stockSyncEnabled(false)
                        .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                        .autoSwitchStockSyncEnabled(true)
                        .korobyteSyncEnabled(false)
                        .build());
    }

    @Test
    @DbUnitDataSet(before = "ChangePiApiParam.before.csv")
    @DisplayName("Проверить lms действия на переключении на режим ПИ при статусе CROSSDOCK=DONT_WANT")
    void checkCrossdockDontWantDisable() {
        var partnerResponse = generatePartnerResponse(1005L);
        when(lmsClient.getPartner(anyLong())).thenReturn(
                Optional.of(partnerResponse));
        FunctionalTestHelper.get(baseUrl + "/manageParam?type={type}&value={value}&id={id}",
                ParamType.CPA_IS_PARTNER_INTERFACE.ordinal(),
                true,
                5);
        verify(lmsClient, times(1)).updatePartnerSettings(1005L,
                PartnerSettingDto.newBuilder()
                        .locationId(partnerResponse.getLocationId())
                        .trackingType(partnerResponse.getTrackingType())
                        .stockSyncEnabled(false)
                        .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                        .autoSwitchStockSyncEnabled(false)
                        .korobyteSyncEnabled(false)
                        .build());
        FunctionalTestHelper.get(baseUrl + "/manageParam?type={type}&value={value}&id={id}",
                ParamType.CPA_IS_PARTNER_INTERFACE.ordinal(),
                false,
                5);
        verify(lmsClient, times(1)).updatePartnerSettings(1005L,
                PartnerSettingDto.newBuilder()
                        .locationId(partnerResponse.getLocationId())
                        .trackingType(partnerResponse.getTrackingType())
                        .stockSyncEnabled(false)
                        .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                        .autoSwitchStockSyncEnabled(true)
                        .korobyteSyncEnabled(false)
                        .build());
    }

    private PartnerResponse generatePartnerResponse(long partnerId) {
        return EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class)
                .id(partnerId)
                .korobyteSyncEnabled(false)
                .build();
    }

    @Test
    @DbUnitDataSet(before = "switchPIForShop.before.csv", after = "switchPIForShop.autoaccept.after.csv")
    @DisplayName("Проверка, что белому магазину проставляется параметр CPA_IS_PARTNER_INTERFACE" +
            " и вместе с ним автоподтверждение заказов")
    void switchPIForWhiteShopWithAutoAccept() {
        var response = FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=CPA_IS_PARTNER_INTERFACE&value=1", 101L);
        JsonTestUtil.assertEquals(response, getClass(), "/mvc/param/managePiApiForShop.json");
        verifyNoInteractions(lmsClient);
    }

    @Test
    @DbUnitDataSet(before = "switchPIForShop.before.csv")
    @DisplayName("Проверка, что белому магазину проставляется параметр CPA_IS_PARTNER_INTERFACE")
    void setMaxOrdersInShipmentForDSBSShop() {
        var response = FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=CPA_MAX_ORDERS_IN_SHIPMENT&value=52", 101L);
        JsonTestUtil.assertEquals(response, getClass(), "/mvc/param/manageMaxOrdersInShipmentForShop.json");
        verifyNoInteractions(lmsClient);
    }

    @Test
    @DbUnitDataSet(before = "switchPIForShop.before.csv")
    @DisplayName("Проверка, что нельзя установить паарметр CPA_MAX_ORDERS_IN_SHIPMENT в значение > 10000")
    void setInvalidMaxOrdersInShipmentForDSBSShop() {
        var response = FunctionalTestHelper.get(baseUrl + "/manageParam?id={campaignId}&format" +
                "=json&type=CPA_MAX_ORDERS_IN_SHIPMENT&value=10001", 101L);
        var error = JsonTestUtil.parseJson(response.getBody())
                .getAsJsonObject().get("errors")
                .getAsJsonArray().get(0)
                .getAsJsonObject();
        assertThat(error.get("message").getAsString()).isEqualTo("invalid_value");
    }

    @Test
    @DbUnitDataSet(before = "switchPIForShop.before.csv")
    @DisplayName("Проверка, что белому магазину возвращается параметр CPA_MAX_ORDERS_IN_SHIPMENT")
    void getOrdersInShipment() {
        var response = FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=CPA_MAX_ORDERS_IN_SHIPMENT", 101L);
        JsonTestUtil.assertEquals(response, getClass(), "/mvc/param/getMaxOrdersInShipmentForShop.json");
    }

    @Test
    @DbUnitDataSet(before = "ignoreStocksForDsbs.before.csv")
    @DisplayName("Проверка, что ДСБС магазину проставляется параметр IGNORE_STOCKS")
    void setIgnoreStocksForDsbs() {
        var partnerResponse = generatePartnerResponse(1L);
        when(lmsClient.getPartner(anyLong())).thenReturn(
                Optional.of(partnerResponse));
        var response = FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=IGNORE_STOCKS&value=1", 101L);
        JsonTestUtil.assertEquals(response, getClass(), "/mvc/param/manageIgnoreStocksForDsbs.json");
        verify(lmsClient, times(1)).updatePartnerSettings(anyLong(), any());
    }

    @Test
    @DbUnitDataSet(before = "freeLiftingDisabled.before.csv",
            after = "freeLiftingEnabled.after.csv")
    @DisplayName("Проверка, что проставляется флаг бесплатного подъема КГТ")
    void testSetFreeLiftingEnabled() {
        var responseBefore = FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=FREE_LIFTING_ENABLED", 101L);
        JsonTestUtil.assertEquals(responseBefore, getClass(), "/mvc/param/manageFreeLiftingDisabled.json");

        var responseAfter = FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=FREE_LIFTING_ENABLED&value=1", 101L);
        JsonTestUtil.assertEquals(responseAfter, getClass(), "/mvc/param/manageFreeLiftingEnabledSwitch.json");
        verifyNoInteractions(lmsClient);
    }


    @Test
    @DbUnitDataSet(before = "freeLiftingEnabled.before.csv")
    @DisplayName("Проверка, что проставляется флаг бесплатного подъема КГТ")
    void testGetFreeLiftingEnabled() {
        var response = FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=FREE_LIFTING_ENABLED", 101L);
        JsonTestUtil.assertEquals(response, getClass(), "/mvc/param/manageFreeLiftingEnabledResponse.json");
    }

    private static Stream<Arguments> pullToPush() {
        return Stream.of(
                arguments(1,101, PushPartnerStatus.REAL, UnitedCatalogStatus.SUCCESS),
                arguments(2,201, PushPartnerStatus.REAL, UnitedCatalogStatus.NO),
                arguments(1,101, PushPartnerStatus.NO, UnitedCatalogStatus.NO),
                arguments(2,201, PushPartnerStatus.NO, UnitedCatalogStatus.NO)
        );
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "ManageDatasourceParamServantletTest.before.csv")
    @MethodSource("pullToPush")
    void testWhitePushWithEkat(long partnerId,long campaignId, PushPartnerStatus status, UnitedCatalogStatus unitedCatalogStatus){
        var response = FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=IS_PUSH_PARTNER&value="+status.name(), campaignId);
        Assertions.assertEquals(response.getStatusCode(), HttpStatus.OK);
        assertThat(UnitedCatalogStatus.valueOf(
                paramService.getParamStringValue(ParamType.UNITED_CATALOG_STATUS, partnerId, UnitedCatalogStatus.NO.name()))).isEqualTo(unitedCatalogStatus);
    }
}
