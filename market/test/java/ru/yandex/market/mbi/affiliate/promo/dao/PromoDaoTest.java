package ru.yandex.market.mbi.affiliate.promo.dao;

import java.util.List;

import org.dbunit.database.DatabaseConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.Discount;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.DiscountType;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.OfferMatching;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.PromoDescriptionStatus;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.PromoOffers;
import ru.yandex.market.mbi.affiliate.promo.config.FunctionalTestConfig;
import ru.yandex.market.mbi.affiliate.promo.model.PromoDescription;
import ru.yandex.market.mbi.affiliate.promo.model.Promocode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionalTestConfig.class)
@TestExecutionListeners(listeners = {
        DbUnitTestExecutionListener.class,
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"))
public class PromoDaoTest {
    @Autowired
    private PromoDao dao;

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "promo_dao_before.csv")
    public void testFindPromoDescriptions() {
        var result = dao.findPromoDescriptionsByIds(List.of(1005L, 1006L));
        assertThat(result.keySet(), containsInAnyOrder(1005L, 1006L));
        var descr1005 = result.get(1005L);
        assertThat(descr1005, is(PromoDescription.builder()
                        .withId(1005L)
                .withPromoOffers(new PromoOffers()
                        .description("Stels")
                        .url("http://market.yandex.ru/brand--stels/1338")
                        .matchingInclude(new OfferMatching()
                                .categoryList(List.of())
                                .brandList(List.of(1338L))
                                .mskuList(List.of())
                                .supplierList(List.of(90001L))
                        )
                        .matchingExclude(new OfferMatching()
                                .categoryList(List.of())
                                .brandList(List.of())
                                .mskuList(List.of())
                                .supplierList(List.of())
                        ))
                .withDiscount(new Discount().discountType(DiscountType.PERCENT).discountValue(4)
                    .bucketMinPrice(1500)
                    .bucketMaxPrice(30000)
                )
                .withStatus(PromoDescriptionStatus.ACTIVE)
                .withOneOrderPromocode(true)
                .build()
        ));
        var descr1006 = result.get(1006L);
        assertThat(descr1006, is(PromoDescription.builder()
                .withId(1006L)
                .withPromoOffers(new PromoOffers()
                        .description("Всё")
                        .url("http://market.yandex.ru")
                        .matchingInclude(new OfferMatching()
                                .categoryList(List.of())
                                .brandList(List.of())
                                .mskuList(List.of())
                                .supplierList(List.of()))
                        .matchingExclude(new OfferMatching()
                                .categoryList(List.of())
                                .brandList(List.of())
                                .mskuList(List.of())
                                .supplierList(List.of())
                        ))
                .withDiscount(new Discount().discountType(DiscountType.FIXED).discountValue(1))
                .withStatus(PromoDescriptionStatus.INACTIVE)
                .withGeneratedPromoParent(true)
                .build()
        ));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "promo_dao_before.csv", after = "promo_dao_insert_after.csv")
    public void testInsert() {
        long id = dao.insertPromoDescription(PromoDescription.builder()
                .withPromoOffers(new PromoOffers()
                        .description("Телевизоры LG")
                        .url("http://market.yandex.ru/category--tv/3333?glfilter=7893318:4444")
                        .matchingInclude(new OfferMatching()
                                .brandList(List.of(4444L))
                                .categoryList(List.of(3333L))
                                .supplierList(List.of(7777L))
                        )
                        .matchingExclude(new OfferMatching()
                                .categoryList(List.of(67777L))
                        )
                )
                .withDiscount(new Discount().discountType(DiscountType.PERCENT).discountValue(8))
                .withStatus(PromoDescriptionStatus.INACTIVE)
                .withGeneratedPromoParent(true)
                .build());
        assertThat(id, greaterThan(0L));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "promo_dao_before.csv", after = "promo_dao_update_after.csv")
    public void testUpdateStatus() {
        dao.updateStatus(List.of(1005L), PromoDescriptionStatus.CANCELLED);
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "promo_dao_before.csv", after = "promo_dao_activate_after.csv")
    public void testUpdateStatusActive() {
        dao.updateStatus(List.of(1007L), PromoDescriptionStatus.ACTIVE);
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "promo_dao_before.csv",
            after = "promo_dao_update_groups_after.csv")
    public void testUpdatePartnerGroups() {
        dao.updatePartnerGroups(1005L, List.of("vip1"));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "promo_dao_before.csv",
            after = "promo_dao_update_partner_groups_empty_after.csv")
    public void testUpdatePartnerGroupsEmpty() {
        dao.updatePartnerGroups(1005L, List.of());
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "promo_dao_before.csv",
            after = "promo_dao_update_partner_groups_empty_after.csv")
    public void testUpdatePartnerGroupsNull() {
        dao.updatePartnerGroups(1005L, null);
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "promo_dao_before.csv")
    public void testFindNonExisting() {
        var result = dao.findPromoDescriptionsByIds(List.of(1005L, 2000L));
        assertThat(result.keySet(), containsInAnyOrder(1005L));
        result = dao.findPromoDescriptionsByIds(List.of(3000L));
        assertThat(result.keySet(), empty());
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "promo_dao_before.csv", after = "promo_dao_before.csv")
    public void testUpdateNonExisting() {
        dao.updateStatus(List.of(2000L), PromoDescriptionStatus.CANCELLED);
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "promo_dao_before.csv", after = "promo_dao_insert_promo_after.csv")
    public void testInsertPromocode() {
        dao.insertPromocode("stels_bicycle", 3333L, 1005L);
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "promo_dao_before.csv", after = "promo_dao_before.csv")
    public void testInsertPromocodeBadParent() {
        assertThrows(Exception.class,
                () -> dao.insertPromocode("stels_bicycle", 3333, 20000L)
        );
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "promocode_get_before.csv", after = "promocode_get_before.csv")
    public void testGetByParent() {
        List<Promocode> result = dao.getPromosByParent(1005L);
        assertThat(result, containsInAnyOrder(
                new Promocode(1, 1005L, "stels_bicycle", 3333L),
                new Promocode(2, 1005L, "stels_bicycle_2", 4444L)));
    }
}