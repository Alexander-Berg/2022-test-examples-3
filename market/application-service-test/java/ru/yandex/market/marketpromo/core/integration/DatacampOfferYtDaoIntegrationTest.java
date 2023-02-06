package ru.yandex.market.marketpromo.core.integration;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.marketpromo.core.application.properties.YtProperties;
import ru.yandex.market.marketpromo.core.dao.OfferYtDao;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.model.DatacampOffer;
import ru.yandex.market.marketpromo.service.AssortmentService;

import static org.junit.Assert.assertThat;
import static ru.yandex.market.marketpromo.core.test.config.TestApplicationProfiles.YT_ACTIVE;

@Disabled
@ActiveProfiles(YT_ACTIVE)
@TestPropertySource(
        properties = {
                YtProperties.PREFIX + ".offersPath=//home/market/production/indexer/datacamp/united/blue_out/recent",
                YtProperties.PREFIX + ".offersResultPath=hahn://home/market/production/promo-factory/actual-promo-assortment-temp",
                YtProperties.PREFIX + ".homePath=//home/market/production/promo-factory",
                YtProperties.PREFIX + ".token={token}"
        }
)
public class DatacampOfferYtDaoIntegrationTest extends ServiceTestBase {

    @Autowired
    private OfferYtDao offerYtDao;
    @Autowired
    private AssortmentService assortmentService;

    @Test
    public void shouldLoadOffers() {
        List<DatacampOffer> datacampOfferList = new ArrayList<>();
        offerYtDao.loadOffers(datacampOfferList::addAll);

        assertThat(datacampOfferList, Matchers.not(Matchers.empty()));
    }
}
