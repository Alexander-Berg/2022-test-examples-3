package ru.yandex.market.promoboss.dao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.dao.constraints.SupplierConstraintsDao;
import ru.yandex.market.promoboss.model.postgres.SupplierConstraintDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SuppliersConstraintsDaoTest extends AbstractPromoRepositoryTest {
    @Autowired
    private SupplierConstraintsDao dao;

    @Test
    @DbUnitDataSet(before = "SuppliersConstraintsDaoTest.getByPromoId.before.csv")
    public void getByPromoId() {
        List<SupplierConstraintDto> expected = List.of(
                SupplierConstraintDto.builder()
                        .promoId(2L)
                        .supplierId(22L)
                        .exclude(true)
                        .build(),
                SupplierConstraintDto.builder()
                        .promoId(2L)
                        .supplierId(33L)
                        .exclude(true)
                        .build()
        );

        List<SupplierConstraintDto> actual = dao.findByPromoId(2L);

        assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(
            before = "SuppliersConstraintsDaoTest.updateByPromoId_not_exists.before.csv",
            after = "SuppliersConstraintsDaoTest.updateByPromoId_not_exists.after.csv"
    )
    public void updateByPromoId_not_exists() {
        dao.updateByPromoId(List.of(
                SupplierConstraintDto.builder()
                        .promoId(2L)
                        .supplierId(33L)
                        .exclude(true)
                        .build(),
                SupplierConstraintDto.builder()
                        .promoId(2L)
                        .supplierId(44L)
                        .exclude(true)
                        .build()
        ), 2L);
    }

    @Test
    @DbUnitDataSet(
            before = "SuppliersConstraintsDaoTest.updateByPromoId_exists.before.csv",
            after = "SuppliersConstraintsDaoTest.updateByPromoId_exists.after.csv"
    )
    public void updateByPromoId_exists() {
        dao.updateByPromoId(List.of(
                SupplierConstraintDto.builder()
                        .promoId(2L)
                        .supplierId(33L)
                        .exclude(true)
                        .build(),
                SupplierConstraintDto.builder()
                        .promoId(2L)
                        .supplierId(44L)
                        .exclude(true)
                        .build()
        ), 2L);
    }
}
