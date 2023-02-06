package ru.yandex.market.marketpromo.core.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.generator.Promos;
import ru.yandex.market.marketpromo.core.test.utils.YtTestHelper;
import ru.yandex.market.marketpromo.model.DatacampOffer;
import ru.yandex.market.marketpromo.model.SupplierType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.datacampOffer;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.disabled;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.hasDefaults;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.supplierType;
import static ru.yandex.market.marketpromo.utils.IdentityUtils.hashId;

public class DatacampOfferYtDaoTest extends ServiceTestBase {

    private static final String OFFER_1 = "offer 1";
    private static final String OFFER_2 = "offer 2";

    @Autowired
    private OfferYtDao offerYtDao;
    @Autowired
    private YtTestHelper ytTestHelper;

    @Test
    void shouldLoadOffers() {
        ytTestHelper.mockOffersResponse(List.of(
                datacampOffer(
                        name(OFFER_1),
                        shopSku(OFFER_1),
                        price(1000),
                        potentialPromo(Promos.DEFAULT_PROMO_ID, BigDecimal.valueOf(1500)),
                        supplierType(SupplierType._1P)
                ),
                datacampOffer(
                        name(OFFER_2),
                        shopSku(OFFER_2),
                        price(1500),
                        potentialPromo(Promos.DEFAULT_PROMO_ID, BigDecimal.valueOf(1500)),
                        supplierType(SupplierType._1P)
                )
        ));

        List<DatacampOffer> offersResult = new ArrayList<>();
        offerYtDao.loadOffers(offersResult::addAll);

        assertThat(offersResult, not(Matchers.empty()));
        assertThat(offersResult, hasItems(
                allOf(
                        hasDefaults(),
                        hasProperty("shopSku", is(OFFER_1)),
                        hasProperty("name", is(OFFER_1)),
                        hasProperty("price", comparesEqualTo(BigDecimal.valueOf(1000))),
                        hasProperty("potentialPromos", hasValue(allOf(
                                hasProperty("id", is(hashId(Promos.DEFAULT_PROMO_ID))),
                                hasProperty("basePrice", comparesEqualTo(BigDecimal.valueOf(1500)))
                        )))
                ), allOf(
                        hasDefaults(),
                        hasProperty("shopSku", is(OFFER_2)),
                        hasProperty("name", is(OFFER_2)),
                        hasProperty("price", comparesEqualTo(BigDecimal.valueOf(1500))),
                        hasProperty("potentialPromos", hasValue(allOf(
                                hasProperty("id", is(hashId(Promos.DEFAULT_PROMO_ID))),
                                hasProperty("basePrice", comparesEqualTo(BigDecimal.valueOf(1500)))
                                ))
                        ))));
    }

    @Test
    @Disabled
    void shouldLoadOffersWithoutDisabled() {
        ytTestHelper.mockOffersResponse(List.of(
                datacampOffer(
                        name(OFFER_1),
                        shopSku(OFFER_1),
                        price(1000),
                        disabled(true),
                        potentialPromo(Promos.DEFAULT_PROMO_ID, BigDecimal.valueOf(1500)),
                        supplierType(SupplierType._1P)
                ),
                datacampOffer(
                        name(OFFER_2),
                        shopSku(OFFER_2),
                        price(1500),
                        potentialPromo(Promos.DEFAULT_PROMO_ID, BigDecimal.valueOf(1500)),
                        supplierType(SupplierType._1P)
                )
        ));

        List<DatacampOffer> offersResult = new ArrayList<>();
        offerYtDao.loadOffers(offersResult::addAll);

        assertThat(offersResult, not(Matchers.empty()));
        assertThat(offersResult, hasSize(1));
        assertThat(offersResult, hasItems(
                allOf(
                        hasDefaults(),
                        hasProperty("shopSku", is(OFFER_2)),
                        hasProperty("name", is(OFFER_2)),
                        hasProperty("price", comparesEqualTo(BigDecimal.valueOf(1500))),
                        hasProperty("potentialPromos", hasValue(allOf(
                                hasProperty("id", is(hashId(Promos.DEFAULT_PROMO_ID))),
                                hasProperty("basePrice", comparesEqualTo(BigDecimal.valueOf(1500)))
                                ))
                        ))));
    }
}
