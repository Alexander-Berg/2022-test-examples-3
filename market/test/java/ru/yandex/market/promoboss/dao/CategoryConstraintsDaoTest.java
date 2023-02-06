package ru.yandex.market.promoboss.dao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.dao.constraints.CategoryConstraintsDao;
import ru.yandex.market.promoboss.model.postgres.CategoryConstraintDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CategoryConstraintsDaoTest extends AbstractPromoRepositoryTest {
    @Autowired
    private CategoryConstraintsDao dao;

    @Test
    @DbUnitDataSet(before = "CategoryConstraintsDaoTest.getByPromoId.before.csv")
    public void getByPromoId() {
        List<CategoryConstraintDto> expected = List.of(
                CategoryConstraintDto.builder()
                        .promoId(2L)
                        .categoryId("category2")
                        .exclude(true)
                        .build(),
                CategoryConstraintDto.builder()
                        .promoId(2L)
                        .categoryId("category3")
                        .exclude(true)
                        .build()
        );

        List<CategoryConstraintDto> actual = dao.findByPromoId(2L);

        assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(
            before = "CategoryConstraintsDaoTest.updateByPromoId_not_exists.before.csv",
            after = "CategoryConstraintsDaoTest.updateByPromoId_not_exists.after.csv"
    )
    public void updateByPromoId_not_exists() {
        dao.updateByPromoId(List.of(
                CategoryConstraintDto.builder()
                        .promoId(2L)
                        .categoryId("category3")
                        .exclude(true)
                        .build(),
                CategoryConstraintDto.builder()
                        .promoId(2L)
                        .categoryId("category4")
                        .exclude(true)
                        .build()
        ), 2L);
    }

    @Test
    @DbUnitDataSet(
            before = "CategoryConstraintsDaoTest.updateByPromoId_exists.before.csv",
            after = "CategoryConstraintsDaoTest.updateByPromoId_exists.after.csv"
    )
    public void updateByPromoId_exists() {
        dao.updateByPromoId(List.of(
                CategoryConstraintDto.builder()
                        .promoId(2L)
                        .categoryId("category3")
                        .exclude(true)
                        .build(),
                CategoryConstraintDto.builder()
                        .promoId(2L)
                        .categoryId("category4")
                        .exclude(true)
                        .build()
        ), 2L);
    }
}
