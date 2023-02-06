package ru.yandex.market.gutgin.tms.service.datacamp.scheduling;

import java.util.function.Predicate;

import Market.DataCamp.DataCampOfferMapping;
import org.junit.Test;

import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.gutgin.tms.config.OfferSchedulingConfig.IS_BETA_PARTNER;
import static ru.yandex.market.gutgin.tms.config.OfferSchedulingConfig.OFFER_INFO_IS_NEW_PREDICATE;

public class CskuProcessingStrategyTest {

    private final Predicate<DatacampOfferDao.OfferInfo> notNewOffer = offerInfo -> !offerInfo.isNew();
    private final Predicate<DatacampOfferDao.OfferInfo> newOffer = DatacampOfferDao.OfferInfo::isNew;

    private final OfferProcessingStrategy fastCardEditStrategy = new FastOffersProcessingStrategy(
            notNewOffer,
            OfferProcessingStrategy.Priority.DEFAULT);

    private final OfferProcessingStrategy cskuProcessingCreateStrategy = new CskuProcessingStrategy(
            OFFER_INFO_IS_NEW_PREDICATE.and(IS_BETA_PARTNER.negate()),
            OfferProcessingStrategy.Priority.DEFAULT);

    private final OfferProcessingStrategy cskuProcessingEditStrategy = new CskuProcessingStrategy(
            OFFER_INFO_IS_NEW_PREDICATE.negate().and(IS_BETA_PARTNER.negate()),
            OfferProcessingStrategy.Priority.DEFAULT);

    private final OfferProcessingStrategy cskuProcessingEditStrategyProduction = new CskuProcessingStrategy(
            notNewOffer,
            OfferProcessingStrategy.Priority.LOW);

    private final OfferProcessingStrategy cskuProcessingEditStrategyBetaTest = new CskuProcessingStrategyBeta(
            OFFER_INFO_IS_NEW_PREDICATE.negate().and(IS_BETA_PARTNER),
            OfferProcessingStrategy.Priority.DEFAULT);

    private final OfferProcessingStrategy cskuProcessingCreateStrategyBetaTest = new CskuProcessingStrategyBeta(
            OFFER_INFO_IS_NEW_PREDICATE.and(IS_BETA_PARTNER),
            OfferProcessingStrategy.Priority.DEFAULT);

    @Test
    public void isCsku() {
        DatacampOfferDao.OfferInfo offerInfo = new DatacampOfferDao.OfferInfo(
                0L, 11062549, null, null,
                10L,
                null, null, DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_PSKU,
                5L, true,
                false, true, false);
        boolean fastCardOfferProcessingStrategySuitable = fastCardEditStrategy.isSuitable(offerInfo);
        boolean cskuStrategySuitable = cskuProcessingCreateStrategy.isSuitable(offerInfo);
        boolean cskuEditStrategySuitable = cskuProcessingEditStrategy.isSuitable(offerInfo);

        assertThat(cskuEditStrategySuitable).isFalse();
        assertThat(cskuStrategySuitable).isTrue();
        assertThat(fastCardOfferProcessingStrategySuitable).isFalse();
    }

    @Test
    public void isCskuWithGroups() {
        DatacampOfferDao.OfferInfo offerInfo = new DatacampOfferDao.OfferInfo(
                0L, null, null, 100L,
                10L,
                null, 100L, DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_PSKU,
                5L, true,
                false, false, false);

        boolean fastCardOfferProcessingStrategySuitable = fastCardEditStrategy.isSuitable(offerInfo);
        boolean cskuStrategySuitable = cskuProcessingCreateStrategy.isSuitable(offerInfo);
        boolean cskuEditStrategySuitable = cskuProcessingEditStrategy.isSuitable(offerInfo);
        boolean cskuEditStrategyProductionSuitable = cskuProcessingEditStrategyProduction.isSuitable(offerInfo);

        assertThat(cskuEditStrategySuitable).isTrue();
        assertThat(cskuStrategySuitable).isFalse();
        assertThat(cskuEditStrategyProductionSuitable).isTrue();
        assertThat(fastCardOfferProcessingStrategySuitable).isFalse();
    }

    @Test
    public void isCskuWithMskuMapping() {
        DatacampOfferDao.OfferInfo offerInfo = new DatacampOfferDao.OfferInfo(
                0L, null, null, null,
                10L,
                null, 100L, DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_MSKU,
                0L, true,
                false, false, true);

        boolean fastCardOfferProcessingStrategySuitable = fastCardEditStrategy.isSuitable(offerInfo);
        boolean cskuStrategySuitable = cskuProcessingCreateStrategy.isSuitable(offerInfo);
        boolean cskuEditStrategySuitable = cskuProcessingEditStrategy.isSuitable(offerInfo);
        boolean cskuEditStrategyProductionSuitable = cskuProcessingEditStrategyProduction.isSuitable(offerInfo);

        assertThat(cskuEditStrategySuitable).isTrue();
        assertThat(cskuStrategySuitable).isFalse();
        assertThat(cskuEditStrategyProductionSuitable).isTrue();
        assertThat(fastCardOfferProcessingStrategySuitable).isFalse();
    }

    @Test
    public void isCskuOnProductionEnvWithGroups() {
        DatacampOfferDao.OfferInfo offerInfo = new DatacampOfferDao.OfferInfo(
                0L, null, null, 100L,
                10L,
                null, 100L, DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_PSKU,
                5L, true,
                false, true, false);

        boolean fastCardOfferProcessingStrategySuitable = fastCardEditStrategy.isSuitable(offerInfo);
        boolean cskuStrategySuitable = cskuProcessingCreateStrategy.isSuitable(offerInfo);
        boolean cskuEditStrategySuitable = cskuProcessingEditStrategy.isSuitable(offerInfo);
        boolean cskuEditStrategyProductionSuitable = cskuProcessingEditStrategyProduction.isSuitable(offerInfo);

        assertThat(cskuEditStrategySuitable).isTrue();
        assertThat(cskuStrategySuitable).isFalse();
        assertThat(cskuEditStrategyProductionSuitable).isTrue();
        assertThat(fastCardOfferProcessingStrategySuitable).isFalse();
    }

    @Test
    public void isCskuByWhitelist() {
        Integer businessId = 223;
        DatacampOfferDao.OfferInfo offerInfo = new DatacampOfferDao.OfferInfo(
                0L, businessId, null, 100L,
                10L,
                null, 100L, DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_PSKU,
                5L, true,
                false, false, false);

        boolean fastCardOfferProcessingStrategySuitable = fastCardEditStrategy.isSuitable(offerInfo);
        boolean cskuStrategySuitable = cskuProcessingCreateStrategy.isSuitable(offerInfo);
        boolean cskuEditStrategySuitable = cskuProcessingEditStrategy.isSuitable(offerInfo);
        boolean cskuEditStrategyProductionSuitable = cskuProcessingEditStrategyProduction.isSuitable(offerInfo);

        assertThat(cskuEditStrategySuitable).isTrue();
        assertThat(cskuStrategySuitable).isFalse();
        assertThat(cskuEditStrategyProductionSuitable).isTrue();
        assertThat(fastCardOfferProcessingStrategySuitable).isFalse();
    }

    @Test
    public void whenPskuModificationWithoutDeduplicatedFlagThenCsku() {
        DatacampOfferDao.OfferInfo offerInfo = new DatacampOfferDao.OfferInfo(
                0L, null, null, null,
                null,
                null, 100L, DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_PSKU,
                5L, true,
                false, false,
                false);
        boolean fastCardOfferProcessingStrategySuitable = fastCardEditStrategy.isSuitable(offerInfo);
        boolean cskuNewStrategySuitable = cskuProcessingCreateStrategy.isSuitable(offerInfo);
        boolean cskuEditStrategySuitable = cskuProcessingEditStrategy.isSuitable(offerInfo);

        assertThat(cskuEditStrategySuitable).isTrue();
        assertThat(cskuNewStrategySuitable).isFalse();
        assertThat(fastCardOfferProcessingStrategySuitable).isFalse();
    }

    @Test
    public void whenNewInDisabledForFCThenCsku() {
        DatacampOfferDao.OfferInfo offerInfo = new DatacampOfferDao.OfferInfo(
                0L, null, null, null,
                null,
                null, null, DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_PSKU,
                5L, true,
                false, true,
                false);

        boolean fastCardOfferProcessingStrategySuitable = fastCardEditStrategy.isSuitable(offerInfo);
        boolean cskuStrategySuitable = cskuProcessingCreateStrategy.isSuitable(offerInfo);

        assertThat(cskuStrategySuitable).isTrue();
        assertThat(fastCardOfferProcessingStrategySuitable).isFalse();
    }


    @Test
    public void whenBetaPartnerThenUseBetaStrategy() {
        Integer betaPartner = BusinessIds.BETA_PARTNERS.stream().findFirst().orElseThrow();
        DatacampOfferDao.OfferInfo offerInfo = new DatacampOfferDao.OfferInfo(
                0L, betaPartner , null, null,
                null,
                null, null, DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_PSKU,
                5L, true,
                false, true,
                false);

        boolean fastCardOfferProcessingStrategySuitable = fastCardEditStrategy.isSuitable(offerInfo);
        boolean cskuStrategySuitable = cskuProcessingCreateStrategy.isSuitable(offerInfo);
        boolean cskuBetaEdit = cskuProcessingEditStrategyBetaTest.isSuitable(offerInfo);
        boolean cskuBetaCreate = cskuProcessingCreateStrategyBetaTest.isSuitable(offerInfo);

        assertThat(cskuStrategySuitable).isFalse();
        assertThat(fastCardOfferProcessingStrategySuitable).isFalse();
        assertThat(cskuBetaEdit).isFalse();
        assertThat(cskuBetaCreate).isTrue();
    }


    @Test
    public void whenBetaPartnerAndEditThenUseBetaStrategy() {
        Integer betaPartner = BusinessIds.BETA_PARTNERS.stream().findFirst().orElseThrow();
        DatacampOfferDao.OfferInfo offerInfo = new DatacampOfferDao.OfferInfo(
                0L, betaPartner , null, null,
                null,
                null, 100L, DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_PSKU,
                5L, true,
                false, true,
                false);

        boolean fastCardOfferProcessingStrategySuitable = fastCardEditStrategy.isSuitable(offerInfo);
        boolean cskuStrategySuitable = cskuProcessingCreateStrategy.isSuitable(offerInfo);
        boolean cskuBetaEditTesting = cskuProcessingEditStrategyBetaTest.isSuitable(offerInfo);
        boolean cskuBetaCreateTesting = cskuProcessingCreateStrategyBetaTest.isSuitable(offerInfo);

        assertThat(cskuStrategySuitable).isFalse();
        assertThat(fastCardOfferProcessingStrategySuitable).isFalse();
        assertThat(cskuBetaEditTesting).isTrue();
        assertThat(cskuBetaCreateTesting).isFalse();
    }
}
