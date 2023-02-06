package ru.yandex.market.promoboss.dao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.dao.constraints.MskuConstraintsDao;
import ru.yandex.market.promoboss.model.postgres.MskuConstraintDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MskuConstraintsDaoTest extends AbstractPromoRepositoryTest {
    @Autowired
    private MskuConstraintsDao dao;

    @Test
    @DbUnitDataSet(before = "MskuConstraintsDaoTest.getByPromoId.before.csv")
    public void getByPromoId() {
        List<MskuConstraintDto> expected = List.of(
                MskuConstraintDto.builder()
                        .promoId(2L)
                        .mskuId(22L)
                        .exclude(true)
                        .build(),
                MskuConstraintDto.builder()
                        .promoId(2L)
                        .mskuId(33L)
                        .exclude(true)
                        .build()
        );

        List<MskuConstraintDto> actual = dao.findByPromoId(2L);

        assertEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(
            before = "MskuConstraintsDaoTest.updateByPromoId_not_exists.before.csv",
            after = "MskuConstraintsDaoTest.updateByPromoId_not_exists.after.csv"
    )
    public void updateByPromoId_not_exists() {
        dao.updateByPromoId(List.of(
                MskuConstraintDto.builder()
                        .promoId(2L)
                        .mskuId(33L)
                        .exclude(true)
                        .build(),
                MskuConstraintDto.builder()
                        .promoId(2L)
                        .mskuId(44L)
                        .exclude(true)
                        .build()
        ), 2L);
    }

    @Test
    @DbUnitDataSet(
            before = "MskuConstraintsDaoTest.updateByPromoId_exists.before.csv",
            after = "MskuConstraintsDaoTest.updateByPromoId_exists.after.csv"
    )
    public void updateByPromoId_exists() {
        dao.updateByPromoId(List.of(
                MskuConstraintDto.builder()
                        .promoId(2L)
                        .mskuId(33L)
                        .exclude(true)
                        .build(),
                MskuConstraintDto.builder()
                        .promoId(2L)
                        .mskuId(44L)
                        .exclude(true)
                        .build()
        ), 2L);
    }
}
