package ru.yandex.market.promoboss.dao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.dao.constraints.VendorConstraintsDao;
import ru.yandex.market.promoboss.model.postgres.VendorConstraintDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VendorConstraintsDaoTest extends AbstractPromoRepositoryTest {
    @Autowired
    private VendorConstraintsDao dao;

    @Test
    @DbUnitDataSet(before = "VendorConstraintsDaoTest.getByPromoId.before.csv")
    public void getByPromoId() {
        List<VendorConstraintDto> expected = List.of(
                VendorConstraintDto.builder()
                        .promoId(2L)
                        .vendorId("vendor2")
                        .exclude(true)
                        .build(),
                VendorConstraintDto.builder()
                        .promoId(2L)
                        .vendorId("vendor3")
                        .exclude(true)
                        .build()
        );

        List<VendorConstraintDto> actual = dao.findByPromoId(2L);

        assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(
            before = "VendorConstraintsDaoTest.updateByPromoId_not_exists.before.csv",
            after = "VendorConstraintsDaoTest.updateByPromoId_not_exists.after.csv"
    )
    public void updateByPromoId_not_exists() {
        dao.updateByPromoId(List.of(
                VendorConstraintDto.builder()
                        .promoId(2L)
                        .vendorId("vendor3")
                        .exclude(true)
                        .build(),
                VendorConstraintDto.builder()
                        .promoId(2L)
                        .vendorId("vendor4")
                        .exclude(true)
                        .build()
        ), 2L);
    }

    @Test
    @DbUnitDataSet(
            before = "VendorConstraintsDaoTest.updateByPromoId_exists.before.csv",
            after = "VendorConstraintsDaoTest.updateByPromoId_exists.after.csv"
    )
    public void updateByPromoId_exists() {
        dao.updateByPromoId(List.of(
                VendorConstraintDto.builder()
                        .promoId(2L)
                        .vendorId("vendor3")
                        .exclude(true)
                        .build(),
                VendorConstraintDto.builder()
                        .promoId(2L)
                        .vendorId("vendor4")
                        .exclude(true)
                        .build()
        ), 2L);
    }
}
