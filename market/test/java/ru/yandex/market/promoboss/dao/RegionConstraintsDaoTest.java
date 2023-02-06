package ru.yandex.market.promoboss.dao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.dao.constraints.RegionConstraintsDao;
import ru.yandex.market.promoboss.model.postgres.RegionConstraintDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegionConstraintsDaoTest extends AbstractPromoRepositoryTest {
    @Autowired
    private RegionConstraintsDao dao;

    @Test
    @DbUnitDataSet(before = "RegionConstraintsDaoTest.getByPromoId.before.csv")
    public void getByPromoId() {
        List<RegionConstraintDto> expected = List.of(
                RegionConstraintDto.builder()
                        .promoId(2L)
                        .regionId("region2")
                        .exclude(true)
                        .build(),
                RegionConstraintDto.builder()
                        .promoId(2L)
                        .regionId("region3")
                        .exclude(true)
                        .build()
        );

        List<RegionConstraintDto> actual = dao.findByPromoId(2L);

        assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(
            before = "RegionConstraintsDaoTest.updateByPromoId_not_exists.before.csv",
            after = "RegionConstraintsDaoTest.updateByPromoId_not_exists.after.csv"
    )
    public void updateByPromoId_not_exists() {
        dao.updateByPromoId(List.of(
                RegionConstraintDto.builder()
                        .promoId(2L)
                        .regionId("region3")
                        .exclude(true)
                        .build(),
                RegionConstraintDto.builder()
                        .promoId(2L)
                        .regionId("region4")
                        .exclude(true)
                        .build()
        ), 2L);
    }

    @Test
    @DbUnitDataSet(
            before = "RegionConstraintsDaoTest.updateByPromoId_exists.before.csv",
            after = "RegionConstraintsDaoTest.updateByPromoId_exists.after.csv"
    )
    public void updateByPromoId_exists() {
        dao.updateByPromoId(List.of(
                RegionConstraintDto.builder()
                        .promoId(2L)
                        .regionId("region3")
                        .exclude(true)
                        .build(),
                RegionConstraintDto.builder()
                        .promoId(2L)
                        .regionId("region4")
                        .exclude(true)
                        .build()
        ), 2L);
    }
}
