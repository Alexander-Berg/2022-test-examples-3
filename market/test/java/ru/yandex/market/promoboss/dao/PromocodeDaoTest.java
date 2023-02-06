package ru.yandex.market.promoboss.dao;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.dao.mechanics.PromocodeDao;
import ru.yandex.market.promoboss.model.mechanics.Promocode;
import ru.yandex.market.promoboss.model.mechanics.PromocodeType;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(classes = {
        PromocodeDao.class
})
public class PromocodeDaoTest extends AbstractPromoTest {
    @Autowired
    private PromocodeDao promocodeDao;

    public Promocode buildPromocode() {
        return Promocode.builder()
                .codeType(PromocodeType.FIXED_DISCOUNT)
                .value(11)
                .code("code1")
                .minCartPrice(111L)
                .maxCartPrice(1111L)
                .applyMultipleTimes(true)
                .budget(11111L)
                .additionalConditions("additional_conditions1")
                .build();
    }

    public Promocode buildUpdatePromocode() {
        return Promocode.builder()
                .codeType(PromocodeType.PERCENTAGE)
                .value(112)
                .code("code12")
                .minCartPrice(1112L)
                .maxCartPrice(11112L)
                .applyMultipleTimes(false)
                .budget(111112L)
                .additionalConditions("additional_conditions12")
                .build();
    }

    @Test
    public void shouldNotReturnRecordIfNotExists() {

        // act
        Optional<Promocode> promocodeByPromoId = promocodeDao.getPromocodeByPromoId(0L);

        // verify
        assertFalse(promocodeByPromoId.isPresent());
    }

    @Test
    @DbUnitDataSet(before = "PromocodeDaoTest.shouldReturnRecordByPromoId.before.csv")
    public void shouldReturnRecordByPromoId() {

        // setup
        Promocode expected = buildPromocode();

        // act
        Optional<Promocode> promocode = promocodeDao.getPromocodeByPromoId(PROMO_ID_1);

        // verify
        assertTrue(promocode.isPresent());
        assertEquals(expected, promocode.get());
    }

    @Test
    @DbUnitDataSet(
            before = "PromocodeDaoTest.shouldInsertNewRecord.before.csv",
            after = "PromocodeDaoTest.shouldInsertNewRecord.after.csv"
    )
    public void shouldInsertNewRecord() {
        // act
        promocodeDao.insertPromocode(PROMO_ID_1, buildPromocode());
    }

    @Test
    @DbUnitDataSet(
            before = "PromocodeDaoTest.shouldThrowExceptionDuringCreationIfRecordAlreadyExists.before.csv"
    )
    public void shouldThrowExceptionDuringCreationIfRecordAlreadyExists() {
        // act and verify
        assertThrows(DataIntegrityViolationException.class, () -> promocodeDao.insertPromocode(PROMO_ID_1, buildPromocode()));
    }

    @Test
    public void shouldThrowExceptionDuringCreationIfPromoRecordDoesNotExist() {
        // act and verify
        assertThrows(
                DataIntegrityViolationException.class,
                () -> promocodeDao.insertPromocode(100L, buildPromocode()));
    }

    @Test
    @DbUnitDataSet(
            before = "PromocodeDaoTest.shouldUpdateExistedRecord.before.csv",
            after = "PromocodeDaoTest.shouldUpdateExistedRecord.after.csv"
    )
    public void shouldUpdateExistedRecord() {
        promocodeDao.updatePromocode(PROMO_ID_1, buildUpdatePromocode());
    }

    @Test
    @DbUnitDataSet(
            before = "PromocodeDaoTest.shouldDeleteExistedRecord.before.csv",
            after = "PromocodeDaoTest.shouldDeleteExistedRecord.after.csv"
    )
    public void shouldDeleteExistedRecord() {
        promocodeDao.deletePromocode(PROMO_ID_1);
    }

    @Test
    public void shouldDeleteNotExistedRecord() {
        assertDoesNotThrow(() -> promocodeDao.deletePromocode(PROMO_ID_1));
    }
}
