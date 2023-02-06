package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioDTO;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.SelfCheckDTO;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.datacamp.DataCampClientStub;
import ru.yandex.market.core.datacamp.DataCampService;
import ru.yandex.market.core.delivery.LogisticPartnerService;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureCutoffInfo;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.WizardRequestContext;
import ru.yandex.market.core.wizard.experiment.WizardExperimentsConfig;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.core.wizard.step.MarketplacePlacementStatusCalculator;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTOContainer;
import ru.yandex.market.ff.client.dto.ShopRequestFilterDTO;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.mbi.api.billing.client.MbiBillingClient;
import ru.yandex.market.mbi.api.billing.client.model.PayoutFrequencyDTO;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты для шага wizard'a "Размещение на маркетплейсе".
 * См {@link ru.yandex.market.core.wizard.step.MarketplacePlacementStatusCalculator}
 */
@DbUnitDataSet(before = {"csv/commonBlueWizardData.before.csv",
                         "csv/partnerOnboardingUseAssortmentCalculator.csv"})
class WizardControllerMarketplaceStepFunctionalTest extends AbstractWizardControllerFunctionalTest {
    @Autowired
    private FulfillmentWorkflowClientApi fulfillmentWorkflowClientApi;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private DataCampService dataCampService;

    @Autowired
    private LogisticPartnerService logisticPartnerService;

    @Autowired
    private AboPublicRestClient aboPublicRestClient;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private MbiBillingClient mbiBillingClient;

    @Autowired
    private MarketplacePlacementStatusCalculator marketplacePlacementStatusCalculator;

    @Autowired
    private WizardRequestContext.Factory requestSessionFactory;

    @Test
    @DisplayName("Ошибка, если шаг недоступен")
    void notAvailableForFulfillment() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(CROSSDOCK_SUPPLIER_CAMPAIGN_ID, WizardStepType.MARKETPLACE)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    @DisplayName("Возвращаем NONE, если нет цены в хранилище")
    void testNoDatacampPrice() {
        mockSaaSWithoutStocks(0);
        when(campaignService.getCampaignByDatasource(anyLong()))
                .thenReturn(new CampaignInfo(0, 0, 0, 0, CampaignType.SUPPLIER));
        ((DataCampClientStub) dataCampService.defaultClient()).setUseFilterByPartnerId(true);

        var response = requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.MARKETPLACE);
        assertResponse(response, makeResponseStepStatus(WizardStepType.MARKETPLACE, Status.NONE));

        ((DataCampClientStub) dataCampService.defaultClient()).reset();
    }

    @Test
    @DisplayName("Возвращаем NONE, если договор не оформлен")
    void testPrepayRequestNotApproved() {
        mockServices();

        var response = requestStep(SUPPLIER_WITH_FAILED_REQ, WizardStepType.MARKETPLACE);

        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.MARKETPLACE)
                .withStatus(Status.NONE)
                .build());
    }

    @Test
    @DisplayName("Возвращаем NONE, если нет маппингов")
    @DbUnitDataSet(before = "csv/testSupplyStepFeed.before.csv")
    void testAssortmentEmpty() {
        mockSaaSWithoutStocks(0);

        var response = requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.MARKETPLACE);
        assertResponse(response, makeResponseStepStatus(WizardStepType.MARKETPLACE, Status.NONE));
    }

    @Test
    @DisplayName("Возвращаем NONE, если нет подвержденных маппингов")
    @DbUnitDataSet(before = "csv/testSupplyStepFeed.before.csv")
    void testAssortmentNotApproveEmpty() {
        mockSaaSWithoutStocks(0);

        var response = requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.MARKETPLACE);
        assertResponse(response, makeResponseStepStatus(WizardStepType.MARKETPLACE, Status.NONE));
    }

    @Test
    @DisplayName("Нет заявок - EMPTY")
    @DbUnitDataSet(before = "csv/testSupplyStepFeed.before.csv")
    void testFulfillmentWorkflowEmpty() {
        mockSaaSWithoutStocks(1);
        mockServices();

        var response = requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.MARKETPLACE);
        assertResponse(response, makeResponseStepStatus(WizardStepType.MARKETPLACE, Status.EMPTY));
    }

    @Test
    @DisplayName("Все заявки в статусе: отменена, ошибка")
    @DbUnitDataSet(before = "csv/testSupplyStepFeed.before.csv")
    void testFulfillmentWorkflowError() {
        mockSaaSWithoutStocks(1);
        mockServices(RequestStatus.INVALID, RequestStatus.CANCELLED);

        var response = requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.MARKETPLACE);
        assertResponse(response, makeResponseStepStatus(WizardStepType.MARKETPLACE, Status.EMPTY));
    }

    @Test
    @DisplayName("Есть хотя бы одна заявка И все заявки в статусе: создана, утверждена, в обработке," +
            " машина разгружена, товары принимаются")
    @DbUnitDataSet(before = "csv/testSupplyStepFeed.before.csv")
    void testFulfillmentWorkflowProcessed() {
        mockServices(RequestStatus.CREATED, RequestStatus.VALIDATED, RequestStatus.IN_PROGRESS,
                RequestStatus.CANCELLED);
        mockSaaSWithoutStocks(1);

        var response = requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.MARKETPLACE);
        assertResponse(response, makeResponseStepStatus(WizardStepType.MARKETPLACE, Status.EMPTY));
    }

    @Test
    @DisplayName("Есть хотя бы одна заявка И все заявки в статусе: создана, утверждена, в обработке," +
            " машина разгружена, товары принимаются, частота выплат не настроена, флаг выплат включен")
    void testFulfillmentWorkflowProcessedWithNoPayout() {
        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any())).thenReturn(
                List.of(
                        createFrequencyDTO(21100, PayoutFrequencyDTO.DAILY, PayoutFrequencyDTO.DAILY, true)
                )
        );

        mockServices(RequestStatus.CREATED, RequestStatus.VALIDATED, RequestStatus.IN_PROGRESS,
                RequestStatus.CANCELLED);
        mockSaaSWithoutStocks(1);

        var response = requestStep(FF_WITH_CONTRACT, WizardStepType.MARKETPLACE);
        assertResponse(response, makeResponseStepStatus(WizardStepType.MARKETPLACE, Status.NONE));
    }

    @Test
    @DisplayName("Есть хотя бы одна заявка И все заявки в статусе: создана, утверждена, в обработке," +
            " машина разгружена, товары принимаются, частота выплат настроена, флаг выплат включен")
    void testFulfillmentWorkflowProcessedWithPayout() {
        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any())).thenReturn(
                List.of(
                        createFrequencyDTO(21100, PayoutFrequencyDTO.WEEKLY, PayoutFrequencyDTO.WEEKLY, false)
                )
        );

        mockServices(RequestStatus.CREATED, RequestStatus.VALIDATED, RequestStatus.IN_PROGRESS,
                RequestStatus.CANCELLED);
        mockSaaSWithoutStocks(1);

        var response = requestStep(FF_WITH_CONTRACT, WizardStepType.MARKETPLACE);
        assertResponse(response, makeResponseStepStatus(WizardStepType.MARKETPLACE, Status.EMPTY));
    }


    @Test
    @DisplayName("Есть годные офферы на витрине")
    @DbUnitDataSet(before = "csv/testSupplyStepFeed.before.csv")
    void testFulfillmentWorkflowFull() {
        mockServices(RequestStatus.CANCELLED, RequestStatus.FINISHED, RequestStatus.VALIDATED,
                RequestStatus.IN_PROGRESS);
        mockSaaSWithoutStocks(1);

        var response = requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.MARKETPLACE);
        assertResponse(response, makeResponseStepStatus(WizardStepType.MARKETPLACE, Status.EMPTY));
    }

    @Test
    @DbUnitDataSet(before = "csv/testMarketplaceDropshipNone.csv")
    void testDropshipNoneStatus() {
        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());
        mockSaaSWithStocks(1);
        var response = requestStep(811000, WizardStepType.MARKETPLACE);
        assertResponse(
                response,
                makeResponseStepStatus(
                        WizardStepType.MARKETPLACE,
                        Status.NONE,
                        Map.of("featureStatus", "NEW")
                )
        );
    }

    @Test
    @DbUnitDataSet(before = "csv/testMarketplaceDropshipIndexing.csv")
    void testDropshipIndexing() {
        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());
        mockSaasService(1);
        var response = requestStep(810000, WizardStepType.MARKETPLACE);
        assertResponse(
                response,
                makeResponseStepStatus(
                        WizardStepType.MARKETPLACE,
                        Status.EMPTY,
                        Map.of("featureStatus", "SUCCESS")
                )
        );
        Collection<FeatureCutoffInfo> cutoffs = featureService.getCutoffs(8100, FeatureType.DROPSHIP);
        Assertions.assertEquals(2, cutoffs.size());
        Assertions.assertEquals(
                Set.of(FeatureCutoffType.EXPERIMENT, FeatureCutoffType.MARKETPLACE_PLACEMENT),
                cutoffs.stream().map(FeatureCutoffInfo::getFeatureCutoffType).collect(Collectors.toSet())
        );
    }

    @Test
    @DbUnitDataSet(before = "csv/testMarketplaceDropshipFailed.csv")
    void testDropshipFailedIndexation() {
        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());
        mockSaaSWithStocks(1);
        List<SelfCheckDTO> scenarios = createNewbieSelfCheckDto(2106L);
        when(aboPublicRestClient.getSelfCheckScenarios(eq(2106L), any(), any()))
                .thenReturn(scenarios);
        var response = requestStep(12106, WizardStepType.MARKETPLACE);
        assertResponse(
                response,
                makeResponseStepStatus(
                        WizardStepType.MARKETPLACE,
                        Status.EMPTY,
                        Map.of("featureStatus", "SUCCESS")
                )
        );
    }

    @Test
    @DbUnitDataSet(before = "csv/testMarketplaceDropshipReady.csv")
    void testDropshipReady() {
        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());
        mockSaaSWithStocks(1);
        List<SelfCheckDTO> scenarios = createNewbieSelfCheckDto(2106L);
        when(aboPublicRestClient.getSelfCheckScenarios(eq(2106L), any(), any()))
                .thenReturn(scenarios);
        var response = requestStep(12106, WizardStepType.MARKETPLACE);
        assertResponse(
                response,
                makeResponseStepStatus(
                        WizardStepType.MARKETPLACE,
                        Status.EMPTY,
                        Map.of("featureStatus", "SUCCESS")
                )
        );
    }

    @Test
    void testDropshipReadyWithoutPayout() {
        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any())).thenReturn(
                List.of(
                        createFrequencyDTO(21110, PayoutFrequencyDTO.DAILY, PayoutFrequencyDTO.DAILY, true)
                )
        );

        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());
        mockSaaSWithStocks(1);
        List<SelfCheckDTO> scenarios = createNewbieSelfCheckDto(2111L);
        when(aboPublicRestClient.getSelfCheckScenarios(eq(2111L), any(), any()))
                .thenReturn(scenarios);
        var response = requestStep(DROPSHIP_WITH_CONTRACT, WizardStepType.MARKETPLACE);
        assertResponse(
                response,
                makeResponseStepStatus(
                        WizardStepType.MARKETPLACE,
                        Status.NONE,
                        Map.of("featureStatus", "DONT_WANT")
                )
        );
    }

    @Test
    void testDropshipReadyWithPayout() {
        when(mbiBillingClient.getCurrentAndNextMonthPayoutFrequencies(any())).thenReturn(
                List.of(
                        createFrequencyDTO(21110, PayoutFrequencyDTO.DAILY, PayoutFrequencyDTO.DAILY, false)
                )
        );

        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());
        mockSaaSWithStocks(1);
        List<SelfCheckDTO> scenarios = createNewbieSelfCheckDto(2111L);
        when(aboPublicRestClient.getSelfCheckScenarios(eq(2111L), any(), any()))
                .thenReturn(scenarios);
        var response = requestStep(DROPSHIP_WITH_CONTRACT, WizardStepType.MARKETPLACE);
        assertResponse(
                response,
                makeResponseStepStatus(
                        WizardStepType.MARKETPLACE,
                        Status.EMPTY,
                        Map.of("featureStatus", "SUCCESS")
                )
        );
    }

    @Test
    @DisplayName("Возвращаем NONE, если нет цены в Saas")
    void testSaasNoPrice() {
        mockMboMappingService(SupplierOffer.OfferProcessingStatus.READY);
        environmentService.setValue(WizardExperimentsConfig.SAAS_STATUS_EXP_VAR, "1");
        mockSaasService(0);

        when(campaignService.getCampaignByDatasource(anyLong()))
                .thenReturn(new CampaignInfo(0, 0, 0, 0, CampaignType.SUPPLIER));

        var response = requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.MARKETPLACE);
        assertResponse(response, makeResponseStepStatus(WizardStepType.MARKETPLACE, Status.NONE));
    }

    @Test
    @DisplayName("Возвращаем EMPTY, если есть цены в Saas")
    void testSaasPrice() {
        mockMboMappingService(SupplierOffer.OfferProcessingStatus.READY);
        environmentService.setValue(WizardExperimentsConfig.SAAS_STATUS_EXP_VAR, "1");
        mockSaasService(1);

        when(campaignService.getCampaignByDatasource(anyLong()))
                .thenReturn(new CampaignInfo(0, 0, 0, 0, CampaignType.SUPPLIER));

        var response = requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.MARKETPLACE);
        assertResponse(response, makeResponseStepStatus(WizardStepType.MARKETPLACE, Status.EMPTY));
    }

    @Test
    @DbUnitDataSet(before = "csv/testMarketplaceDropshipReady.csv")
    void testDropshipReadySaas() {
        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());
        List<SelfCheckDTO> scenarios = createNewbieSelfCheckDto(2106L);
        when(aboPublicRestClient.getSelfCheckScenarios(eq(2106L), any(), any()))
                .thenReturn(scenarios);
        environmentService.setValue(WizardExperimentsConfig.SAAS_STATUS_EXP_VAR, "1");
        mockSaasService(1);

        var response = requestStep(12106, WizardStepType.MARKETPLACE);
        assertResponse(
                response,
                makeResponseStepStatus(
                        WizardStepType.MARKETPLACE,
                        Status.EMPTY,
                        Map.of("featureStatus", "SUCCESS")
                )
        );
    }

    @Test
    @DbUnitDataSet(before = "csv/testDropshipNotNewbie.csv")
    void testDropshipNotNewbie() {
        doReturn(true).when(logisticPartnerService).hasActivePartnerRelation(any());

        var context = requestSessionFactory.makeRequestContext(
                PartnerId.partnerId(8111, CampaignType.SUPPLIER),
                true
        );
        var wizardStepStatus = marketplacePlacementStatusCalculator.calculateStepStatus(context);
        Assertions.assertEquals(Status.FULL, wizardStepStatus.getStatus());
    }

    @Test
    @DisplayName("Возвращаем EMPTY без проверки маппингов, так как ходим в Saas")
    @DbUnitDataSet(before = "csv/testSupplyStepFeed.before.csv")
    void testFbySaas() {
        environmentService.setValue(WizardExperimentsConfig.SAAS_STATUS_EXP_VAR, "1");
        mockSaasService(1);
        var response = requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.MARKETPLACE);
        assertResponse(response, makeResponseStepStatus(WizardStepType.MARKETPLACE, Status.EMPTY));
    }

    private void mockServices(RequestStatus... requestStatuses) {
        mockMboMappingService(SupplierOffer.OfferProcessingStatus.READY);

        ShopRequestDTOContainer container = new ShopRequestDTOContainer();
        for (RequestStatus requestStatus : requestStatuses) {
            ShopRequestDTO request = new ShopRequestDTO();
            request.setStatus(requestStatus);
            container.getRequests().add(request);
        }
        when(fulfillmentWorkflowClientApi.getRequests(any(ShopRequestFilterDTO.class))).thenReturn(container);
    }

    private List<SelfCheckDTO> createNewbieSelfCheckDto(long shopId) {
        return List.of(
                new SelfCheckDTO(shopId, CheckOrderScenarioDTO.builder(shopId)
                        .withStatus(CheckOrderScenarioStatus.SUCCESS)
                        .build())
        );
    }


}
