package ru.yandex.market.adv.promo.mvc.multi.dao;

import java.util.List;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.mvc.multi.model.assortment.OfferPromo;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.Matchers.containsInAnyOrder;

class AssortmentProcessingDetailsDaoTest extends FunctionalTest {
    @Autowired
    private AssortmentProcessingDetailsDao dao;

    @Test
    @DbUnitDataSet(
            before = "AssortmentProcessingDetailsDaoTest/createInitialAssortment_discountPromoTest.before.csv",
            after = "AssortmentProcessingDetailsDaoTest/createInitialAssortment_discountPromoTest.after.csv"
    )
    void createInitialAssortment_discountPromoTest() {
        dao.createInitialAssortment(
                "newProcessingId",
                List.of(
                        new OfferPromo("id1", true, 100L, 200L),
                        new OfferPromo("id2", false, null, null),
                        new OfferPromo("id3", true, 1000L, 2000L)
                )
        );
    }

    @Test
    @DbUnitDataSet(
            before = "AssortmentProcessingDetailsDaoTest/createInitialAssortment_nonDiscountPromoTest.before.csv",
            after = "AssortmentProcessingDetailsDaoTest/createInitialAssortment_nonDiscountPromoTest.after.csv"
    )
    void createInitialAssortment_nonDiscountPromoTest() {
        dao.createInitialAssortment(
                "newProcessingId",
                List.of(
                        new OfferPromo("id1", true, null, null),
                        new OfferPromo("id2", false, null, null),
                        new OfferPromo("id3", true, null, null)
                )
        );
    }

    @Test
    @DbUnitDataSet(before = "AssortmentProcessingDetailsDaoTest/getAssortment_discountPromoTest.before.csv")
    void getAssortment_discountPromoTest() {
        List<OfferPromo> offerPromos = dao.getOfferPromos("newProcessingId", Set.of("id1", "id3", "id5"));
        MatcherAssert.assertThat(
                offerPromos,
                containsInAnyOrder(
                        new OfferPromo("id1", true, 100L, 300L),
                        new OfferPromo("id3", true, 400L, 1400L),
                        new OfferPromo("id5", false, null, null)
                )
        );
    }

    @Test
    @DbUnitDataSet(before = "AssortmentProcessingDetailsDaoTest/getAssortment_nonDiscountPromoTest.before.csv")
    void getAssortment_nonDiscountPromoTest() {
        List<OfferPromo> offerPromos = dao.getOfferPromos("newProcessingId", Set.of("id1", "id3", "id5"));
        MatcherAssert.assertThat(
                offerPromos,
                containsInAnyOrder(
                        new OfferPromo("id1", true, null, null),
                        new OfferPromo("id3", false, null, null),
                        new OfferPromo("id5", true, null, null)
                )
        );
    }

    @Test
    @DbUnitDataSet(before = "AssortmentProcessingDetailsDaoTest/getOfferPromosWithMultiConflicts_pagingTest.before.csv")
    void getOfferPromosWithMultiConflicts_pagingTest() {
        int limit = 2;
        int offset = 0;
        List<String> offerPromos = dao.getOfferPromosWithMultiConflicts("newProcessingId", limit, offset);
        MatcherAssert.assertThat(
                offerPromos,
                containsInAnyOrder("id1", "id4")
        );
        offset += limit;
        offerPromos = dao.getOfferPromosWithMultiConflicts("newProcessingId", limit, offset);
        MatcherAssert.assertThat(
                offerPromos,
                containsInAnyOrder("id7", "id8")
        );
        offset += limit;
        offerPromos = dao.getOfferPromosWithMultiConflicts("newProcessingId", limit, offset);
        MatcherAssert.assertThat(
                offerPromos,
                containsInAnyOrder("id9")
        );
    }

    @Test
    @DbUnitDataSet(
            before = "AssortmentProcessingDetailsDaoTest/markOffersWithMultiConflictsTest.before.csv",
            after = "AssortmentProcessingDetailsDaoTest/markOffersWithMultiConflictsTest.after.csv"
    )
    void markOffersWithMultiConflictsTest() {
        dao.markOffersWithMultiConflicts("newProcessingId2", Set.of("id1", "id5"));
    }
}
