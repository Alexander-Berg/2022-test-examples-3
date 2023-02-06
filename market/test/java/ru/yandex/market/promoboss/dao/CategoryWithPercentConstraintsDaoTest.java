package ru.yandex.market.promoboss.dao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.dao.constraints.CategoryConstraintsDao;
import ru.yandex.market.promoboss.model.postgres.CategoryConstraintDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CategoryWithPercentConstraintsDaoTest extends AbstractPromoRepositoryTest {
    @Autowired
    private CategoryConstraintsDao dao;

    @Test
    @DbUnitDataSet(before = "CategoryWithPercentConstraintsDaoTest.getByPromoId.before.csv")
    public void getByPromoId() {
        List<CategoryConstraintDto> expected = List.of(
                CategoryConstraintDto.builder()
                        .promoId(2L)
                        .categoryId("category2")
                        .exclude(true)
                        .percent(22)
                        .build(),
                CategoryConstraintDto.builder()
                        .promoId(2L)
                        .categoryId("category3")
                        .exclude(true)
                        .percent(33)
                        .build()
        );

        List<CategoryConstraintDto> actual = dao.findByPromoId(2L);

        assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(
            before = "CategoryWithPercentConstraintsDaoTest.updateByPromoId_not_exists.before.csv",
            after = "CategoryWithPercentConstraintsDaoTest.updateByPromoId_not_exists.after.csv"
    )
    public void updateByPromoId_not_exists() {
        dao.updateByPromoId(List.of(
                CategoryConstraintDto.builder()
                        .promoId(2L)
                        .categoryId("category3")
                        .exclude(true)
                        .percent(33)
                        .build(),
                CategoryConstraintDto.builder()
                        .promoId(2L)
                        .categoryId("category4")
                        .exclude(true)
                        .percent(44)
                        .build()
        ), 2L);
    }

    @Test
    @DbUnitDataSet(
            before = "CategoryWithPercentConstraintsDaoTest.updateByPromoId_exists.before.csv",
            after = "CategoryWithPercentConstraintsDaoTest.updateByPromoId_exists.after.csv"
    )
    public void updateByPromoId_exists() {
        dao.updateByPromoId(List.of(
                CategoryConstraintDto.builder()
                        .promoId(2L)
                        .categoryId("category3")
                        .exclude(true)
                        .percent(33)
                        .build(),
                CategoryConstraintDto.builder()
                        .promoId(2L)
                        .categoryId("category4")
                        .exclude(true)
                        .percent(44)
                        .build()
        ), 2L);
    }
}
