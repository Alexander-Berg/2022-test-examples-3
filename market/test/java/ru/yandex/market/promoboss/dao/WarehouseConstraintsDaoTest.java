package ru.yandex.market.promoboss.dao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.dao.constraints.WarehouseConstraintsDao;
import ru.yandex.market.promoboss.model.postgres.WarehouseConstraintDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WarehouseConstraintsDaoTest extends AbstractPromoRepositoryTest {
    @Autowired
    private WarehouseConstraintsDao dao;

    @Test
    @DbUnitDataSet(before = "WarehouseConstraintsDaoTest.getByPromoId.before.csv")
    public void getByPromoId() {
        List<WarehouseConstraintDto> expected = List.of(
                WarehouseConstraintDto.builder()
                        .promoId(2L)
                        .warehouseId(22L)
                        .exclude(true)
                        .build(),
                WarehouseConstraintDto.builder()
                        .promoId(2L)
                        .warehouseId(33L)
                        .exclude(true)
                        .build()
        );

        List<WarehouseConstraintDto> actual = dao.findByPromoId(2L);

        assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(
            before = "WarehouseConstraintsDaoTest.updateByPromoId_not_exists.before.csv",
            after = "WarehouseConstraintsDaoTest.updateByPromoId_not_exists.after.csv"
    )
    public void updateByPromoId_not_exists() {
        dao.updateByPromoId(List.of(
                WarehouseConstraintDto.builder()
                        .promoId(2L)
                        .warehouseId(33L)
                        .exclude(true)
                        .build(),
                WarehouseConstraintDto.builder()
                        .promoId(2L)
                        .warehouseId(44L)
                        .exclude(true)
                        .build()
        ), 2L);
    }

    @Test
    @DbUnitDataSet(
            before = "WarehouseConstraintsDaoTest.updateByPromoId_exists.before.csv",
            after = "WarehouseConstraintsDaoTest.updateByPromoId_exists.after.csv"
    )
    public void updateByPromoId_exists() {
        dao.updateByPromoId(List.of(
                WarehouseConstraintDto.builder()
                        .promoId(2L)
                        .warehouseId(33L)
                        .exclude(true)
                        .build(),
                WarehouseConstraintDto.builder()
                        .promoId(2L)
                        .warehouseId(44L)
                        .exclude(true)
                        .build()
        ), 2L);
    }
}
