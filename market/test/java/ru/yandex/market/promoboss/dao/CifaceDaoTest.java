package ru.yandex.market.promoboss.dao;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.model.CifacePromo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(classes = {CifaceDao.class})
public class CifaceDaoTest extends AbstractPromoRepositoryTest {
    @Autowired
    private CifaceDao cifaceDao;

    @Test
    public void shouldNotReturnRecordIfNotExists() {

        // act
        Optional<CifacePromo> result = cifaceDao.findByPromoId(0L);

        // verify
        assertTrue(result.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "CifaceDaoTest.shouldReturnRecordByPromoId.before.csv")
    public void shouldReturnRecordByPromoId() {
        // setup
        CifacePromo expected = CifacePromo.builder()
                .promoId(PROMO_ID_1)
                .promoPurpose("promoPurpose")
                .compensationSource("compensationSource")
                .tradeManager("tradeManager")
                .markom("catManager")
                .promoKind("promoKind")
                .supplierType("supplierType")
                .author("author")
                .budgetOwner("TRADE_MARKETING")
                .finalBudget(true)
                .autoCompensation(false)
                .mediaPlanS3Key("mediaPlanS3Key")
                .mediaPlanS3FileName("mediaPlanS3FileName")
                .compensationTicket("compensationTicket")
                .assortmentLoadMethod("assortmentLoadMethod")
                .piPublishedAt(LocalDateTime.of(2022, 1, 1, 1, 1, 1, 0).toEpochSecond(ZoneOffset.UTC))
                .build();

        // act
        Optional<CifacePromo> result = cifaceDao.findByPromoId(PROMO_ID_1);

        // verify
        assertTrue(result.isPresent());
        assertEquals(expected, result.get());
    }

    @Test
    @DbUnitDataSet(before = "CifaceDaoTest.shouldReturnRecordByPromoIdWithNullFields.before.csv")
    public void shouldReturnRecordByPromoIdWithNullFields() {
        // setup
        CifacePromo expected = CifacePromo.builder()
                .promoId(PROMO_ID_1)
                .promoPurpose("promoPurpose")
                .compensationSource("compensationSource")
                .tradeManager("tradeManager")
                .markom("catManager")
                .promoKind("promoKind")
                .supplierType("supplierType")
                .author("author")
                .budgetOwner("TRADE_MARKETING")
                .finalBudget(true)
                .autoCompensation(false)
                .mediaPlanS3Key("mediaPlanS3Key")
                .mediaPlanS3FileName("mediaPlanS3FileName")
                .piPublishedAt(null)
                .build();

        // act
        AtomicReference<Optional<CifacePromo>> result = new AtomicReference<>();
        assertDoesNotThrow(() -> result.set(cifaceDao.findByPromoId(PROMO_ID_1)));

        // verify
        assertNotNull(result.get());
        assertTrue(result.get().isPresent());
        assertEquals(expected, result.get().get());
    }

    @Test
    @DbUnitDataSet(after = "CifaceDaoTest.shouldInsertNewRecord.after.csv")
    public void shouldInsertNewRecord() {
        // setup
        CifacePromo expected = CifacePromo.builder()
                .promoId(PROMO_ID_1)
                .promoPurpose("promoPurpose")
                .compensationSource("compensationSource")
                .tradeManager("tradeManager")
                .markom("catManager")
                .promoKind("promoKind")
                .supplierType("supplierType")
                .author("author")
                .budgetOwner("TRADE_MARKETING")
                .finalBudget(true)
                .autoCompensation(false)
                .mediaPlanS3Key("mediaPlanS3Key")
                .mediaPlanS3FileName("mediaPlanS3FileName")
                .compensationTicket("compensationTicket")
                .assortmentLoadMethod("assortmentLoadMethod")
                .piPublishedAt(LocalDateTime.of(2022, 1, 1, 1, 1, 1, 0).toEpochSecond(ZoneOffset.UTC))
                .build();

        // act
        cifaceDao.insertCiface(PROMO_ID_1, expected);
    }

    @Test
    @DbUnitDataSet(before = "CifaceDaoTest.shouldThrowExceptionDuringCreationIfRecordAlreadyExists.before.csv")
    public void shouldThrowExceptionDuringCreationIfRecordAlreadyExists() {
        // setup
        CifacePromo cifacePromo = CifacePromo.builder()
                .promoId(PROMO_ID_1)
                .promoPurpose("promoPurpose")
                .compensationSource("compensationSource")
                .tradeManager("tradeManager")
                .markom("catManager")
                .promoKind("promoKind")
                .supplierType("supplierType")
                .author("author")
                .budgetOwner("TRADE_MARKETING")
                .finalBudget(true)
                .autoCompensation(false)
                .mediaPlanS3Key("mediaPlanS3Key")
                .mediaPlanS3FileName("mediaPlanS3FileName")
                .piPublishedAt(LocalDateTime.of(2022, 1, 1, 1, 1, 1, 1).toEpochSecond(ZoneOffset.UTC))
                .build();

        // act and verify
        assertThrows(DbActionExecutionException.class, () -> cifaceDao.insertCiface(PROMO_ID_1, cifacePromo));
    }

    @Test
    public void shouldThrowExceptionDuringCreationIfPromoRecordDoesNotExist() {
        // act and verify
        var exception = assertThrows(
                DbActionExecutionException.class,
                () -> cifaceDao.insertCiface(100L, CifacePromo.builder()
                        .promoPurpose("promoPurpose")
                        .compensationSource("compensationSource")
                        .tradeManager("tradeManager")
                        .markom("catManager")
                        .promoKind("promoKind")
                        .supplierType("supplierType")
                        .author("author")
                        .budgetOwner("TRADE_MARKETING")
                        .finalBudget(true)
                        .autoCompensation(false)
                        .mediaPlanS3Key("mediaPlanS3Key")
                        .mediaPlanS3FileName("mediaPlanS3FileName")
                        .compensationTicket("compensationTicket")
                        .assortmentLoadMethod("assortmentLoadMethod")
                        .piPublishedAt(LocalDateTime.of(2022, 1, 1, 1, 1, 1, 1).toEpochSecond(ZoneOffset.UTC))
                        .build())
        );

        assertTrue(exception.getMessage().startsWith("Failed to execute DbAction.InsertRoot"));
    }

    @Test
    @DbUnitDataSet(
            before = "CifaceDaoTest.shouldUpdateExistedRecord.before.csv",
            after = "CifaceDaoTest.shouldUpdateExistedRecord.after.csv"
    )
    public void shouldUpdateExistedRecord() {
        // setup
        CifacePromo cifacePromo = CifacePromo.builder()
                .promoId(PROMO_ID_1)
                .promoPurpose("promoPurposeNew")
                .compensationSource("compensationSourceNew")
                .tradeManager("tradeManagerNew")
                .markom("catManagerNew")
                .promoKind("promoKindNew")
                .supplierType("supplierTypeNew")
                .author("authorNew")
                .budgetOwner("TRADE_MARKETING_2")
                .finalBudget(false)
                .autoCompensation(true)
                .mediaPlanS3Key("mediaPlanS3KeyNew")
                .mediaPlanS3FileName("mediaPlanS3FileNameNew")
                .compensationTicket("compensationTicketNew")
                .assortmentLoadMethod("assortmentLoadMethodNew")
                .piPublishedAt(LocalDateTime.of(2023, 2, 2, 2, 2, 2, 0).toEpochSecond(ZoneOffset.UTC))
                .build();

        // act
        cifaceDao.updateCiface(PROMO_ID_1, cifacePromo);
    }

    @Test
    @DbUnitDataSet(
            before = "CifaceDaoTest.shouldDeleteExistedRecord.before.csv",
            after = "CifaceDaoTest.shouldDeleteExistedRecord.after.csv"
    )
    public void shouldDeleteExistedRecord() {
        // act
        cifaceDao.deleteCiface(PROMO_ID_1);
    }

    @Test
    @DbUnitDataSet(
            before = "CifaceDaoTest.shouldDeleteNotExistedRecord.before.csv",
            after = "CifaceDaoTest.shouldDeleteNotExistedRecord.after.csv"
    )
    public void shouldDeleteNotExistedRecord() {
        // act
        cifaceDao.deleteCiface(PROMO_ID_1);
    }
}
