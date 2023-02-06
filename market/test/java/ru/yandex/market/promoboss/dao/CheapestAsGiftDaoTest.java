package ru.yandex.market.promoboss.dao;

import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.dao.mechanics.CheapestAsGiftDao;
import ru.yandex.market.promoboss.model.mechanics.CheapestAsGift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(classes = CheapestAsGiftDao.class)
class CheapestAsGiftDaoTest extends AbstractPromoTest {
    @Autowired
    protected CheapestAsGiftDao cheapestAsGiftDao;

    @Test
    public void shouldNotReturnRecordIfNotExists() {

        // act
        Optional<CheapestAsGift> cheapestAsGiftByPromoId = cheapestAsGiftDao.getCheapestAsGiftByPromoId(0L);

        // verify
        assertFalse(cheapestAsGiftByPromoId.isPresent());
    }

    @Test
    @DbUnitDataSet(
            before = "CheapestAsGiftDaoTest.shouldReturnRecordByPromoId.before.csv"
    )
    public void shouldReturnRecordByPromoId() {

        // setup
        CheapestAsGift expected = new CheapestAsGift(11);

        // act
        Optional<CheapestAsGift> cheapestAsGift = cheapestAsGiftDao.getCheapestAsGiftByPromoId(PROMO_ID_1);

        // verify
        assertTrue(cheapestAsGift.isPresent());
        assertEquals(expected, cheapestAsGift.get());
    }

    @Test
    @DbUnitDataSet(
            before = "CheapestAsGiftDaoTest.shouldInsertNewRecord.before.csv",
            after = "CheapestAsGiftDaoTest.shouldInsertNewRecord.after.csv"
    )
    public void shouldInsertNewRecord() {
        // act
        cheapestAsGiftDao.insertCheapestAsGift(PROMO_ID_2, 22);
    }

    @Test
    @DbUnitDataSet(
            before = "CheapestAsGiftDaoTest.shouldThrowExceptionDuringCreationIfRecordAlreadyExists.before.csv"
    )
    public void shouldThrowExceptionDuringCreationIfRecordAlreadyExists() {
        // act and verify
        assertThrows(DuplicateKeyException.class, () -> cheapestAsGiftDao.insertCheapestAsGift(PROMO_ID_1, 3));
    }

    @Test
    public void shouldThrowExceptionDuringCreationIfPromoRecordDoesNotExist() {

        // act and verify
        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> cheapestAsGiftDao.insertCheapestAsGift(100L, 3));

        assertTrue(Objects.requireNonNull(exception.getMessage()).contains("ERROR: insert or update on table \"mechanics_cheapest_as_gift\" " +
                "violates foreign key constraint \"cheapest_as_gift_promos_id_fk\""));
    }

    @Test
    @DbUnitDataSet(
            before = "CheapestAsGiftDaoTest.shouldUpdateExistedRecord.before.csv",
            after = "CheapestAsGiftDaoTest.shouldUpdateExistedRecord.after.csv"
    )
    public void shouldUpdateExistedRecord() {
        // act
        cheapestAsGiftDao.updateCheapestAsGift(PROMO_ID_1, 111);
    }

    @Test
    public void shouldThrowOnUpdatedRecordsIfRecordDoesNotExist() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> cheapestAsGiftDao.updateCheapestAsGift(100L, 4));
        assertEquals("Не удалось обновить запись в таблице mechanics_cheapest_as_gift, id = 100", e.getMessage());
    }

    @Test
    @DbUnitDataSet(
            before = "CheapestAsGiftDaoTest.shouldDeleteExistedRecord.before.csv",
            after = "CheapestAsGiftDaoTest.shouldDeleteExistedRecord.after.csv"
    )
    public void shouldDeleteExistedRecord() {
        // act
        cheapestAsGiftDao.deleteCheapestAsGift(PROMO_ID_2);
    }

    @Test
    public void shouldDeleteNotExistedRecord() {
        // act
        cheapestAsGiftDao.deleteCheapestAsGift(101L);
    }
}
