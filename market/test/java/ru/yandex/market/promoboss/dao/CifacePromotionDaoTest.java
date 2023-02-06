package ru.yandex.market.promoboss.dao;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.model.CifacePromotion;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CifacePromotionDaoTest extends AbstractPromoRepositoryTest {
    @Autowired
    private CifacePromotionDao cifacePromotionDao;

    @Test
    @DbUnitDataSet(
            before = "CifacePromotionDaoTest.getByPromoId.before.csv"
    )
    void getByPromoId() {
        List<CifacePromotion> expected =
                List.of(
                        CifacePromotion.builder()
                                .promoId(PROMO_ID_1)
                                .catteam("catteam1")
                                .category("category1")
                                .channel("channel1")
                                .count(1L)
                                .countUnit("count_unit1")
                                .budgetFact(11L)
                                .budgetPlan(111L)
                                .isCustomBudgetPlan(true)
                                .comment("comment1")
                                .build(),
                        CifacePromotion.builder()
                                .promoId(PROMO_ID_1)
                                .catteam("catteam2")
                                .category("category2")
                                .channel("channel2")
                                .count(2L)
                                .countUnit("count_unit2")
                                .budgetFact(22L)
                                .budgetPlan(222L)
                                .isCustomBudgetPlan(false)
                                .comment("comment2")
                                .build()
                );

        List<CifacePromotion> actual = cifacePromotionDao.findByPromoId(PROMO_ID_1);

        assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(
            before = "CifacePromotionDaoTest.saveAll.before.csv",
            after = "CifacePromotionDaoTest.saveAll.after.csv"
    )
    void saveAll() {
        List<CifacePromotion> list =
                List.of(
                        CifacePromotion.builder()
                                .id(101L)
                                .promoId(PROMO_ID_1)
                                .catteam("catteam1")
                                .category("category1")
                                .channel("channel1")
                                .count(1L)
                                .countUnit("count_unit1")
                                .budgetFact(11L)
                                .budgetPlan(111L)
                                .isCustomBudgetPlan(true)
                                .comment("comment1")
                                .build(),
                        CifacePromotion.builder()
                                .id(null)
                                .promoId(PROMO_ID_1)
                                .catteam("catteam2")
                                .category("category2")
                                .channel("channel2")
                                .count(2L)
                                .countUnit("count_unit2")
                                .budgetFact(22L)
                                .budgetPlan(222L)
                                .isCustomBudgetPlan(false)
                                .comment("comment2")
                                .build()
                );

        cifacePromotionDao.saveAll(list);
    }

    @Test
    @DbUnitDataSet(
            before = "CifacePromotionDaoTest.deleteAll.before.csv",
            after = "CifacePromotionDaoTest.deleteAll.after.csv"
    )
    void deleteAll() {
        var list = cifacePromotionDao
                .findByPromoId(1L)
                .stream()
                .filter(f -> f.getId() == 101L || f.getId() == 102L)
                .collect(Collectors.toList());

        cifacePromotionDao.deleteAll(list);
    }
}
