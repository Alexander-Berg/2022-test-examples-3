package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.partner.mvc.controller.wizard.utils.DatacampFlagResponseMocker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты для шага wizard'a "Шаг загрузка прайс-листа".
 * См {@link ru.yandex.market.core.wizard.step.SupplierFeedStepStatusCalculator}
 */
@DbUnitDataSet(before = {"csv/commonBlueWizardData.before.csv",
                         "csv/partnerOnboardingUseAssortmentCalculator.csv"})
@Disabled("Нет мерчей не в едином каталоге")
class WizardControllerSupplierFeedFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    @Qualifier("environmentService")
    private EnvironmentService environmentService;

    @Autowired
    private FeatureService featureService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    private DatacampFlagResponseMocker datacampMocker;

    @BeforeEach
    public void init() {
        datacampMocker = new DatacampFlagResponseMocker(dataCampShopClient);
    }

    @Test
    @DbUnitDataSet(before = "csv/testSupplierFeedStepNoneApplicationNotApproved.csv")
    @DisplayName("Заявка не подтверждена -> Шаг в NONE")
    void testSupplierFeedStepNone_prepayRequestNotCompleted() {
        environmentService.setValue("select.latest.completed.request", "false");
        var response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.SUPPLIER_FEED);
        assertResponse(
                response,
                makeResponseStepStatus(
                        Status.NONE,
                        Map.of(
                                "feedUrl", "http://mds1.url",
                                "feedName", "file.name",
                                "feedDate", "2017-01-01T00:00:00"
                        )
                )
        );
    }

    @Test
    @DbUnitDataSet(before = "csv/testSupplierFeedStepDatacampPriceWithStocksInDB.csv")
    @DisplayName("В базе есть цена и стоки, загруженные ранее из оферного хранилища -> Шаг в FULL")
    void hasDatacampPriceWithStocksInDB() {
        mockMboMappingService(SupplierOffer.OfferProcessingStatus.READY);

        var response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.SUPPLIER_FEED);
        assertResponse(response, makeResponseStepStatus(Status.FULL, Map.of()));
    }

    @Test
    @DbUnitDataSet(
            before = "csv/testSupplierFeedStepPriceFromDatacamp.csv",
            after = "csv/testSupplierFeedStepPriceFromDatacamp.after.csv"
    )
    @DisplayName("В базе цены из оферного хранилища нет, ходим в хранилище и складываем в базу.")
    void hasPriceAndStocksInDatacamp() {
        //given
        mockMboMappingService(SupplierOffer.OfferProcessingStatus.READY);
        when(campaignService.getCampaignByDatasource(anyLong()))
                .thenReturn(new CampaignInfo(0, 0, 0, 0, CampaignType.SUPPLIER));

        datacampMocker.setHasPriceResponse(true);
        datacampMocker.setHasStocksResponse(true);
        datacampMocker.setHasPriceWithStocksResponse(true);

        assertEquals(true, featureService.getFeatureInfo(2100L, FeatureType.DROPSHIP).getCanEnable());

        //when
        var response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.SUPPLIER_FEED);

        //then
        assertResponse(response, makeResponseStepStatus(Status.FULL, Map.of()));
        /*
        Так как после подгрузки цен из датакэмпа вызывается checkPreconditions, то кэш фч должен сброситься,
        CanEnable должен стать true
         */
        assertEquals(true, featureService.getFeatureInfo(2100L, FeatureType.DROPSHIP).getCanEnable());
    }

    @Test
    @DisplayName("Есть оффер с ценой, но нет оффера со стоками -> EMPTY для дропшипа")
    void hasPriceAndNoStocksInDatacamp() {
        //given
        mockMboMappingService(SupplierOffer.OfferProcessingStatus.READY);
        when(campaignService.getCampaignByDatasource(anyLong()))
                .thenReturn(new CampaignInfo(0, 0, 0, 0, CampaignType.SUPPLIER));

        assertEquals(false, featureService.getFeatureInfo(2100L, FeatureType.DROPSHIP).getCanEnable());

        datacampMocker.setHasPriceResponse(true);
        datacampMocker.setHasStocksResponse(false);
        datacampMocker.setHasPriceWithStocksResponse(false);

        //when
        var response = requestStep(DROPSHIP_WITH_CPA_PARTNER_INTERFACE_CAMPAIGN_ID, WizardStepType.SUPPLIER_FEED);

        //then
        //Фича должна включаться при подгрузке цены
        assertEquals(false, featureService.getFeatureInfo(2100L, FeatureType.DROPSHIP).getCanEnable());
        assertResponse(response, makeResponseStepStatus(Status.EMPTY, Map.of(
                "hasOfferWithPrice", true,
                "hasOfferWithStocks", false,
                "hasOfferWithPriceAndStocks", false
        )));
    }

    @Test
    @DisplayName("Есть оффер с ценой, но нет оффера со стоками -> шаг FULL для фулфилмента")
    void hasPriceAndNoStocksInDatacampForFulfillment() {
        //given
        mockMboMappingService(SupplierOffer.OfferProcessingStatus.READY);
        when(campaignService.getCampaignByDatasource(anyLong()))
                .thenReturn(new CampaignInfo(0, 0, 0, 0, CampaignType.SUPPLIER));

        datacampMocker.setHasPriceResponse(true);
        datacampMocker.setHasStocksResponse(false);
        datacampMocker.setHasPriceWithStocksResponse(false);

        //when
        var response = requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.SUPPLIER_FEED);

        //then
        assertResponse(response, makeResponseStepStatus(Status.FULL, Map.of()));
    }

    @Test
    @DisplayName("Есть оффер с ценой,и со стоками, но это разные оффера -> Шаг в EMPTY")
    void hasPriceAndStocksOnDifferentOffers() {
        //given
        mockMboMappingService(SupplierOffer.OfferProcessingStatus.READY);
        when(campaignService.getCampaignByDatasource(anyLong()))
                .thenReturn(new CampaignInfo(0, 0, 0, 0, CampaignType.SUPPLIER));

        datacampMocker.setHasPriceResponse(true);
        datacampMocker.setHasStocksResponse(true);
        datacampMocker.setHasPriceWithStocksResponse(false);

        //when
        var response = requestStep(DROPSHIP_WITH_CPA_PARTNER_INTERFACE_CAMPAIGN_ID, WizardStepType.SUPPLIER_FEED);

        //then
        assertResponse(response, makeResponseStepStatus(Status.EMPTY, Map.of(
                "hasOfferWithPrice", true,
                "hasOfferWithStocks", true,
                "hasOfferWithPriceAndStocks", false
        )));
    }

    @Test
    @DisplayName("Нет ни оффера с ценой, ни оффера со стоками -> Шаг в EMPTY")
    void hasNoPriceAndNoStocks() {
        //given
        mockMboMappingService(SupplierOffer.OfferProcessingStatus.READY);
        when(campaignService.getCampaignByDatasource(anyLong()))
                .thenReturn(new CampaignInfo(0, 0, 0, 0, CampaignType.SUPPLIER));

        datacampMocker.setHasPriceResponse(false);
        datacampMocker.setHasStocksResponse(false);
        datacampMocker.setHasPriceWithStocksResponse(false);

        //when
        var response = requestStep(DS_SUPPLIER_CAMPAIGN_ID, WizardStepType.SUPPLIER_FEED);

        //then
        assertResponse(response, makeResponseStepStatus(Status.EMPTY, Map.of(
                "hasOfferWithPrice", false,
                "hasOfferWithStocks", false,
                "hasOfferWithPriceAndStocks", false
        )));
    }

    private static WizardStepStatus makeResponseStepStatus(Status status, Map<String, ?> details) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.SUPPLIER_FEED)
                .withStatus(status)
                .withDetails(details)
                .build();
    }
}
