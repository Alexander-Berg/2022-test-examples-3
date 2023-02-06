package ru.yandex.market.loyalty.admin.tms;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.dao.promocode.PromocodeEntryDao;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.promocode.PromocodeService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static java.sql.Timestamp.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@TestFor(PromocodeEmptyCleanProcessor.class)
public class PromocodeEmptyCleanProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private PromocodeService promocodeService;
    @Autowired
    private PromocodeEntryDao promocodeEntryDao;
    @Autowired
    private PromocodeEmptyCleanProcessor promocodeEmptyCleanProcessor;
    @Autowired
    private PromoManager promoManager;

    @Test
    public void shouldNotRemovePromocodesWithPromoId() {
        clock.setDate(valueOf("2019-01-09 10:00:00"));
        var promoCode = "some_promocode";
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode(promoCode).setLandingUrl("some"));
        clock.setDate(valueOf("2019-01-12 10:00:00"));

        var hasExist = promocodeEntryDao.countByCode(promoCode) == 1;

        assertThat(hasExist, equalTo(true));

        promocodeEmptyCleanProcessor.cleanEmptyPromocodes();

        hasExist = promocodeEntryDao.countByCode(promoCode) == 1;

        assertThat(hasExist, equalTo(true));
    }

    @Test
    public void shouldRemovePromocodesWithoutPromoId() {
        clock.setDate(valueOf("2019-01-09 10:00:00"));
        var promoCode = promocodeService.generateNewPromocode();
        clock.setDate(valueOf("2019-01-12 10:00:00"));

        var hasExist = promocodeEntryDao.countByCode(promoCode) == 1;

        assertThat(hasExist, equalTo(true));

        promocodeEmptyCleanProcessor.cleanEmptyPromocodes();

        hasExist = promocodeEntryDao.countByCode(promoCode) == 1;

        assertThat(hasExist, equalTo(false));
    }
}
