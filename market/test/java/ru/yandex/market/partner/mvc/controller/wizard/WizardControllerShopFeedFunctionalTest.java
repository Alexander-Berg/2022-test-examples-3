package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.Map;
import java.util.Objects;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.experiment.WizardExperimentService;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.mvc.controller.wizard.utils.DatacampFlagResponseMocker;

import static org.mockito.ArgumentMatchers.argThat;
import static ru.yandex.market.core.wizard.experiment.WizardExperimentsConfig.DBS_FEED_CHECK_PRICE_VAR;
import static ru.yandex.market.core.wizard.experiment.WizardExperimentsConfig.SAAS_STATUS_EXP_VAR;
import static ru.yandex.market.core.wizard.experiment.WizardExperimentsConfig.SHOP_SELF_CHECK_STOCKS_EXP_VAR;

/**
 * Функциональные тесты для шага wizard'a "Фиды магазина".
 * См {@link ru.yandex.market.core.wizard.step.ShopFeedStepStatusCalculator}
 *
 * @author Vladislav Bauer
 */
@DbUnitDataSet(before = "csv/commonWhiteWizardData.before.csv")
class WizardControllerShopFeedFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @Autowired
    @Qualifier("environmentService")
    private EnvironmentService environmentService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private WizardExperimentService dbsShopFeedExperiment;

    private DatacampFlagResponseMocker datacampMocker;

    @BeforeEach
    void setUp() {
        dbsShopFeedExperiment.close();
        datacampMocker = new DatacampFlagResponseMocker(dataCampShopClient);
    }

    /**
     * Проверить что фиды не настроены.
     */
    @Test
    void testFeedStepWithoutFeeds() {
        mockPartnerOffers(0);
        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY,
                false, false));
    }

    /**
     * Проверить что есть фид, но он протух.
     */
    @Test
    @DbUnitDataSet(before = "csv/testFeedStepWithAllExpired.before.csv")
    void testFeedStepWithAllExpired() {
        mockPartnerOffers(0);
        mockSaaSWithoutStocks(0);
        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatus(Status.FULL,
                false, false));
    }

    /**
     * Проверить что есть фид и среди протухших фидов, есть непротухшие.
     */
    @Test
    @DbUnitDataSet(before = "csv/testFeedStepWithSomeExpired.before.csv")
    void testFeedStepWithSomeExpired() {
        mockPartnerOffers(0);
        mockSaaSWithoutStocks(0);
        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatus(Status.FULL,
                false, false));
    }

    /**
     * Проверить что есть фид и он не протух.
     */
    @Test
    @DbUnitDataSet(before = "csv/testFeedStepFilled.before.csv")
    void testFeedStepFilled() {
        mockPartnerOffers(0);
        mockSaaSWithoutStocks(0);
        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatus(Status.FULL,
                false, false));
    }

    /**
     * Проверить что есть дефолтный фид не учитывается при расчете статуса шага визарда.
     */
    @Test
    @DbUnitDataSet(before = "csv/testFeedStepDefaultFeedIsIgnored.csv")
    void testFeedStepDefaultFeedIsIgnored() {
        mockPartnerOffers(0);
        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY,
                false, false));
    }

    /**
     * Пуш партнер, загружающий каталог через файл, у которого оффера пока не проросли в офферное.
     */
    @Test
    @DbUnitDataSet(before = "csv/testDsbsPushShop.csv")
    void testPushPartnerHasNoOffersInDatacamp() {
        mockPartnerOffers(0);

        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatus(Status.FILLED,
                true, false));
    }

    /**
     * Пуш партнер, загружающий каталог через файл, у которого оффера проросли в офферное.
     */
    @Test
    @DbUnitDataSet(before = "csv/testDsbsPushShop.csv")
    void testPushPartnerHasOffersInDatacamp() {
        mockPartnerOffers(1);
        mockSaaSWithoutStocks(1);

        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatus(Status.FULL,
                true, false, true));
    }

    /**
     * Пуш партнер, загружающий каталог по ссылке, у которого оффера пока не проросли в офферное.
     */
    @Test
    @DbUnitDataSet(before = "csv/testDsbsPushShop_viaLink.csv")
    void testPushPartnerViaLink_noOffersInDatacamp() {
        mockPartnerOffers(0);

        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatus(Status.FILLED,
                true, true));
    }

    /**
     * Пуш партнер, создавший оффера в ПИ, у которого оффера пока не проросли в офферное.
     */
    @Test
    @DbUnitDataSet(before = "csv/testDsbsPushShop_viaPI.csv")
    void testPushPartnerWithOfferCreateInPI_noOffersInDatacamp() {
        mockPartnerOffers(0);

        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY,
                true, false));
    }

    /**
     * Пуш партнер, создавший оффера в ПИ, у которого оффера проросли в офферное.
     */
    @Test
    @DbUnitDataSet(before = "csv/testDsbsPushShop_viaPI.csv")
    void testPushPartnerWithOfferCreateInPI_withOffersInDatacamp() {
        mockPartnerOffers(1);
        mockSaaSWithoutStocks(1);

        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatus(Status.FULL,
                true, false, true));
    }

    /**
     * Пуш партнер, создавший оффера в ПИ, у которого оффера пока не проросли в офферное.
     */
    @Test
    @DbUnitDataSet(before = "csv/testDsbsPushShop_viaPI.csv")
    void testPushPartnerWithOfferCreateInPI_noOffersInDatacampExperiment() {
        environmentService.setValue(DBS_FEED_CHECK_PRICE_VAR, "1");
        mockPartnerOffers(0);

        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY,
                true, false));
    }

    /**
     * Пуш партнер, создавший оффера в ПИ, у которого оффера проросли в офферное.
     */
    @Test
    @DbUnitDataSet(before = "csv/testDsbsPushShop_viaPI.csv")
    void testPushPartnerWithOfferCreateInPI_withOffersInDatacampExperiment() {
        environmentService.setValue(DBS_FEED_CHECK_PRICE_VAR, "1");
        mockPartnerOffers(1);
        mockSaaSWithoutStocks(1);

        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatus(Status.FULL,
                true, false, true));
    }

    /**
     * Пуш партнер, создавший оффера в ПИ, у которого оффера проросли в офферное, но цен там нет.
     */
    @Test
    @DbUnitDataSet(before = "csv/testDsbsPushShop_viaPIWithFeed.csv")
    void testPushPartnerWithOfferCreateInPI_withOffersNoPriceInDatacampExperiment() {
        environmentService.setValue(DBS_FEED_CHECK_PRICE_VAR, "1");
        mockPartnerOffers(1);
        mockSaaSWithoutStocks(0);

        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatusSaaS(Status.FILLED,
                true, false, false));
    }

    @Test
    @DbUnitDataSet(before = "csv/testDsbsPushShop_viaPIWithFeed.csv")
    void testPushPartnerWithOfferCreateInPI_withOffersSaas() {
        environmentService.setValue(DBS_FEED_CHECK_PRICE_VAR, "1");
        environmentService.setValue(SAAS_STATUS_EXP_VAR, "1");
        final var dataCampResponse =
                ProtoTestUtil.getProtoMessageByJson(SyncGetOffer.GetUnitedOffersResponse.class,
                        "json/datacamp.filled.json", getClass());
        Mockito.doReturn(DataCampStrollerConversions.fromStrollerResponse(dataCampResponse))
                .when(dataCampShopClient)
                .searchBusinessOffers(
                        argThat(request -> Objects.equals(request.getPartnerId(), 4001L) &&
                                request.getPricePresence() == null)
                );
        datacampMocker.setHasPriceResponse(false);
        mockSaasService(1);

        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatusSaas(Status.FULL,
                true, false, true));
    }

    /**
     * Проверка со стоками
     */
    @Test
    @DbUnitDataSet(before = "csv/testDsbsPushShop_viaLink.csv")
    void testDbs_hasStocks() {
        environmentService.setValue(DBS_FEED_CHECK_PRICE_VAR, "1");
        environmentService.setValue(SHOP_SELF_CHECK_STOCKS_EXP_VAR, "1");

        mockPartnerOffers(10);
        mockSaaSWithStocks(10);

        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatus(Status.FULL,
                true, true, true));
    }

    /**
     * Проверка со стоками, нет стоков
     */
    @Test
    @DbUnitDataSet(before = "csv/testDsbsPushShop_viaLink.csv")
    void testDbs_withoutStocks() {
        environmentService.setValue(DBS_FEED_CHECK_PRICE_VAR, "1");
        environmentService.setValue(SHOP_SELF_CHECK_STOCKS_EXP_VAR, "1");

        mockPartnerOffers(1);
        mockSaaSWithStocks(0);

        final ResponseEntity<String> response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SHOP_FEED);
        assertResponse(response, makeResponseStepStatus(Status.FILLED,
                true,  true, false));
    }

    private static WizardStepStatus makeResponseStepStatus(
            Status status,
            boolean isPushPartner,
            boolean isUsingFeedLink) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.SHOP_FEED)
                .withStatus(status)
                .withDetails(Map.of(
                        "isPushPartner", isPushPartner,
                        "isUsingFeedLink", isUsingFeedLink)
                )
                .build();
    }

    private static WizardStepStatus makeResponseStepStatus(
            Status status,
            boolean isPushPartner,
            boolean isUsingFeedLink,
            boolean hasOfferWithPrice) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.SHOP_FEED)
                .withStatus(status)
                .withDetails(Map.of(
                        "isPushPartner", isPushPartner,
                        "isUsingFeedLink", isUsingFeedLink,
                        "hasOfferWithPrice", hasOfferWithPrice)
                )
                .build();
    }

    private static WizardStepStatus makeResponseStepStatusSaaS(
            Status status,
            boolean isPushPartner,
            boolean isUsingFeedLink,
            boolean hasValidOffer) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.SHOP_FEED)
                .withStatus(status)
                .withDetails(Map.of(
                        "isPushPartner", isPushPartner,
                        "isUsingFeedLink", isUsingFeedLink,
                        "hasValidOffer", hasValidOffer)
                )
                .build();
    }

    private static WizardStepStatus makeResponseStepStatusSaas(
            Status status,
            boolean isPushPartner,
            boolean isUsingFeedLink,
            boolean hasValidOffer) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.SHOP_FEED)
                .withStatus(status)
                .withDetails(Map.of(
                        "isPushPartner", isPushPartner,
                        "isUsingFeedLink", isUsingFeedLink,
                        "hasValidOffer", hasValidOffer)
                )
                .build();
    }
}
