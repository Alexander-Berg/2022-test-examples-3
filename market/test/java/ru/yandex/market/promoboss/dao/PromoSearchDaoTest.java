package ru.yandex.market.promoboss.dao;


import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.config.JdbcTestConfig;
import ru.yandex.market.promoboss.dao.search.PromoSearchDao;
import ru.yandex.market.promoboss.model.MechanicsType;
import ru.yandex.market.promoboss.model.SourceType;
import ru.yandex.market.promoboss.model.Status;
import ru.yandex.market.promoboss.model.search.PromoSearchItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ContextConfiguration(classes = {JdbcTestConfig.class, PromoSearchDao.class})
public class PromoSearchDaoTest extends AbstractPromoTest {
    private static final Long DATE_1 = OffsetDateTime.of(2022, 5, 10, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond();
    private static final Long DATE_2 = OffsetDateTime.of(2022, 10, 10, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond();

    @Autowired
    private PromoSearchDao promoSearchDao;

    @Test
    @DbUnitDataSet(
            before = "PromoSearchDaoTest.createItem.before.csv",
            after = "PromoSearchDaoTest.createItem.after.csv"
    )
    void createItem() {
        promoSearchDao.insert(buildPromoSearchItem1(PROMO_ID_1));
    }

    @Test
    @DbUnitDataSet(
            before = "PromoSearchDaoTest.updateItem.before.csv",
            after = "PromoSearchDaoTest.updateItem.after.csv"
    )
    void updateItem() {
        promoSearchDao.update(buildPromoSearchItem2(PROMO_ID_1));
    }

    @Test
    @DbUnitDataSet(
            before = "PromoSearchDaoTest.getSearchItem.before.csv"
    )
    void getSearchItem() {
        PromoSearchItem expectedSearchItem = buildPromoSearchItem1(PROMO_ID_1);
        assertEquals(expectedSearchItem, promoSearchDao.getById(PROMO_ID_1));
    }

    @Test
    @DbUnitDataSet(
            before = "PromoSearchDaoTest.getSearchItemWithEmptySource.before.csv"
    )
    void getSearchItemWithEmptySource() {
        PromoSearchItem expectedSearchItem = buildPromoSearchItem1(PROMO_ID_1);
        expectedSearchItem.setSource(null);
        assertEquals(expectedSearchItem, promoSearchDao.getById(PROMO_ID_1));
    }

    private static PromoSearchItem buildPromoSearchItem1(@SuppressWarnings("SameParameterValue") Long id) {
        return PromoSearchItem.builder()
                .id(id)
                .promoId("cf_123")
                .parentPromoId("123")
                .status(Status.NEW)
                .startAt(DATE_1)
                .endAt(DATE_2)
                .name("Gender promo")
                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                .productsCount1p(10)
                .productsCount3p(0)
                .srcCifacePromotionBudgetFact(10000L)
                .srcCifaceAuthor("ivan")
                .srcCifaceCompensationSource("MARKET")
                .srcCifaceSupplierType("1P")
                .srcCifacePromoKind("promo-kind")
                .srcCifaceTradeManager("semenov")
                .srcCifaceCategoryDepartments(null)
                .srcCifaceFinalBudget(false)
                .active(true)
                .source(SourceType.ANAPLAN)
                .srcCifaceAssortmentLoadMethod("PI")
                .build();
    }

    private static PromoSearchItem buildPromoSearchItem2(@SuppressWarnings("SameParameterValue") Long id) {
        return PromoSearchItem.builder()
                .id(id)
                .promoId("cf_123_new")
                .parentPromoId("123_new")
                .status(Status.READY)
                .startAt(DATE_1 + 1)
                .endAt(DATE_2 + 1)
                .name("New gender promo")
                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                .productsCount1p(15)
                .productsCount3p(0)
                .srcCifacePromotionBudgetFact(15000L)
                .srcCifaceAuthor("sergey")
                .srcCifaceCompensationSource("MARKET_AND_VENDOR")
                .srcCifaceSupplierType("1P")
                .srcCifacePromoKind("promo-kind")
                .srcCifaceTradeManager("igor")
                .srcCifaceCategoryDepartments(null)
                .srcCifaceFinalBudget(true)
                .active(false)
                .source(SourceType.CATEGORYIFACE)
                .srcCifaceAssortmentLoadMethod("OTHER_METHOD")
                .build();
    }
}
