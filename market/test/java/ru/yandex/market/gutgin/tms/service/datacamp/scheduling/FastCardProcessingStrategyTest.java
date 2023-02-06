package ru.yandex.market.gutgin.tms.service.datacamp.scheduling;

import java.util.function.Predicate;

import Market.DataCamp.DataCampOfferMapping;
import org.junit.Test;

import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.gutgin.tms.config.OfferSchedulingConfig.IS_BETA_PARTNER;
import static ru.yandex.market.gutgin.tms.config.OfferSchedulingConfig.OFFER_INFO_IS_NEW_PREDICATE;

public class FastCardProcessingStrategyTest {

    private final Predicate<DatacampOfferDao.OfferInfo> newOffer = DatacampOfferDao.OfferInfo::isNew;

    private final OfferProcessingStrategy fastCardCreateStrategy = new FastOffersProcessingStrategy(
            newOffer,
            OfferProcessingStrategy.Priority.DEFAULT);

    private final OfferProcessingStrategy cskuProcessingCreateStrategy = new CskuProcessingStrategy(
            newOffer,
            OfferProcessingStrategy.Priority.DEFAULT);

    @Test
    public void whenMarketSkuTypeIsFast_andFastCardAllowed_thenIsFast() {
        DatacampOfferDao.OfferInfo offerInfo = new DatacampOfferDao.OfferInfo(
                0L, 11062549, null, null,
                10L,
                null, null, DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_FAST,
                0L, true,
                false, true, true);
        boolean fastCardOfferProcessingStrategySuitable = fastCardCreateStrategy.isSuitable(offerInfo);
        boolean cskuStrategySuitable = cskuProcessingCreateStrategy.isSuitable(offerInfo);

        assertThat(cskuStrategySuitable).isFalse();
        assertThat(fastCardOfferProcessingStrategySuitable).isTrue();
    }

    @Test
    public void whenMarketSkuTypeIsPSKU_andFastCardAllowed_thenNotFast() {
        DatacampOfferDao.OfferInfo offerInfo = new DatacampOfferDao.OfferInfo(
                0L, 11062549, null, null,
                10L,
                null, null, DataCampOfferMapping.Mapping.MarketSkuType.MARKET_SKU_TYPE_PSKU,
                0L, true,
                false, true, false);
        boolean fastCardOfferProcessingStrategySuitable = fastCardCreateStrategy.isSuitable(offerInfo);
        boolean cskuStrategySuitable = cskuProcessingCreateStrategy.isSuitable(offerInfo);

        assertThat(cskuStrategySuitable).isTrue();
        assertThat(fastCardOfferProcessingStrategySuitable).isFalse();
    }

}
