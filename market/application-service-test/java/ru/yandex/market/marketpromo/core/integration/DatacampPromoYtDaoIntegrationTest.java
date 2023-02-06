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
import ru.yandex.market.marketpromo.core.dao.PromoYtDao;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.service.PromoService;

import static org.junit.Assert.assertThat;
import static ru.yandex.market.marketpromo.core.test.config.TestApplicationProfiles.YT_ACTIVE;

@Disabled
@ActiveProfiles(YT_ACTIVE)
@TestPropertySource(
        properties = {
                YtProperties.PREFIX + ".promoStoragePath=//home/market/testing/indexer/datacamp/promo/backups/recent",
                YtProperties.PREFIX + ".homePath=//home/market/production/promo-factory",
                YtProperties.PREFIX + ".token={token}"
        }
)
public class DatacampPromoYtDaoIntegrationTest extends ServiceTestBase {

    @Autowired
    private PromoYtDao promoYtDao;
    @Autowired
    private PromoService promoService;

    @Test
    public void shouldLoadOffers() {
        List<Promo> promosList = new ArrayList<>();
        promoYtDao.loadRecords(promosList::addAll);

        assertThat(promosList, Matchers.not(Matchers.empty()));

        promoService.createOrUpdatePromos(promosList);
    }
}
