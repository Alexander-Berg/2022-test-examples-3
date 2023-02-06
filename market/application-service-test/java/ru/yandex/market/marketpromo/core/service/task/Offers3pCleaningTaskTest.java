package ru.yandex.market.marketpromo.core.service.task;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.dao.ConfigurationDao;
import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.LocalPromoOfferDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.test.ServiceTaskTestBase;
import ru.yandex.market.marketpromo.filter.AssortmentRequest;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.SupplierType;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.generateDatacampOfferList;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.supplierType;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.id;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promoName;

public class Offers3pCleaningTaskTest extends ServiceTaskTestBase {

    private static final String PROMO_ID = "some promo";
    @Autowired
    private Offers3pCleaningTask offers3pCleaningTask;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    private DatacampOfferDao offerDao;
    @Autowired
    private LocalPromoOfferDao localOfferDao;
    @Autowired
    private ConfigurationDao configurationDao;

    private Promo promo;

    @BeforeEach
    void configure() {
        promo = promoDao.replace(promo(
                id(IdentityUtils.hashId(PROMO_ID)),
                promoName(PROMO_ID),
                directDiscount(
                        minimalDiscountPercentSize(10)
                )
        ));

        configurationDao.set("assortment.cache.useAssortmentCache", false);
    }

    @Test
    void shouldMarkToDeleteOffersOnCall() {
        offerDao.replace(generateDatacampOfferList(100,
                supplierType(SupplierType._3P),
                potentialPromo(promo.getId())
        ));
        offerDao.replace(generateDatacampOfferList(50,
                supplierType(SupplierType._1P),
                potentialPromo(promo.getId())
        ));

        assertThat(offerDao.stream(1000).count(), comparesEqualTo(100L));

        offers3pCleaningTask.process();

        assertThat(localOfferDao.getOffersCountByRequest(
                AssortmentRequest.builder(promo.toPromoKey())
                .build()
        ), comparesEqualTo(50L));
    }
}
