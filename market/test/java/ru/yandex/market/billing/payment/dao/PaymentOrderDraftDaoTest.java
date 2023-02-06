package ru.yandex.market.billing.payment.dao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.Platform;
import ru.yandex.market.billing.core.factoring.PayoutFrequency;
import ru.yandex.market.billing.factoring.model.PayoutSchedule;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.partner.model.PartnerContractType;
import ru.yandex.market.core.util.DateTimes;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для {@link PaymentOrderDraftDao}.
 */
public class PaymentOrderDraftDaoTest extends FunctionalTest {

    private static final LocalDate TEST_DRAFT_DATE = LocalDate.of(2021, 7, 21);
    private static final Instant TEST_TIME = DateTimes.toInstantAtDefaultTz(
            LocalDateTime.of(TEST_DRAFT_DATE, LocalTime.parse("20:32:00"))
    );
    private static final List<Platform> DEFAULT_PLATFORMS = List.of(
            Platform.YANDEX_MARKET, Platform.GLOBAL_MARKET
    );
    private static final PartnerContractType INCOME_CONTRACT = PartnerContractType.INCOME;

    private static final PayoutSchedule DAILY = new PayoutSchedule(PayoutFrequency.DAILY, "0 0 0 * * ?", 0);
    private static final PayoutSchedule WEEKLY = new PayoutSchedule(PayoutFrequency.WEEKLY, "0 0 0 1,8,16,24 * ?", 0);
    private static final PayoutSchedule BI_WEEKLY = new PayoutSchedule(PayoutFrequency.BI_WEEKLY, "0 0 0 1,16 * ?", 0);

    @Autowired
    private PaymentOrderDraftDao paymentOrderDraftDao;

    private static Stream<Arguments> getSuccessfulAllowedFrequencies() {
        return Stream.of(
                Arguments.of("Разрешены только ежедневные выплаты", Set.of(DAILY), List.of(0)),
                Arguments.of("Разрешены ежедневные и еженедельные выплаты", Set.of(DAILY, WEEKLY)),
                Arguments.of("Разрешены ежедневные и двухнедельные выплаты", Set.of(DAILY, BI_WEEKLY)),
                Arguments.of("Разрешены все выплаты", Set.of(DAILY, WEEKLY, BI_WEEKLY))
        );
    }

    private static Stream<Arguments> getUnsuccessfulAllowedFrequencies() {
        return Stream.of(
                Arguments.of("Разрешены только еженедельные выплаты", Set.of(WEEKLY)),
                Arguments.of("Разрешены только двухнедельные выплаты", Set.of(BI_WEEKLY)),
                Arguments.of("Разрешены еженедельные и двухнедельные выплаты", Set.of(WEEKLY, BI_WEEKLY))
        );
    }

    @Test
    @DisplayName("Формирование команд за один проход")
    @DbUnitDataSet(
            before = "PaymentOrderDraftDaoTest.collectPaymentOrdersFromDrafts.before.csv",
            after = "PaymentOrderDraftDaoTest.collectPaymentOrdersFromDrafts.after.csv"
    )
    void collectPaymentOrdersFromDrafts() {
        Set<PayoutSchedule> allowedSchedules = Set.of(DAILY);

        Long count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(8, count);

        count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(0, count);
    }

    @Test
    @DisplayName("Формирование команд в несколько проходов")
    @DbUnitDataSet(
            before = "PaymentOrderDraftDaoTest.collectPaymentOrdersFromDrafts.before.csv",
            after = "PaymentOrderDraftDaoTest.collectPaymentOrdersFromDrafts.after.csv"
    )
    void collectPaymentOrdersFromDraftsByChunks() {
        Set<PayoutSchedule> allowedSchedules = Set.of(DAILY);
        Long count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 3, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(3, count);

        count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 1, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(1, count);

        count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 10, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(4, count);
    }

    @Test
    @DisplayName("Формирование команд из нескольких черновиков за разные даты")
    @DbUnitDataSet(
            before = "PaymentOrderDraftDaoTest.collectPaymentOrdersFromRangeOfDrafts.before.csv",
            after = "PaymentOrderDraftDaoTest.collectPaymentOrdersFromRangeOfDrafts.after.csv"
    )
    void collectPaymentOrdersFromRangeOfDrafts() {
        Set<PayoutSchedule> allowedSchedules = Set.of(DAILY);
        Long count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(8, count);

        count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(0, count);
    }

    @Test
    @DisplayName("Формирование команд с обработанными черновиками")
    @DbUnitDataSet(
            before = "PaymentOrderDraftDaoTest.collectPaymentOrdersWithProcessedDrafts.before.csv",
            after = "PaymentOrderDraftDaoTest.collectPaymentOrdersWithProcessedDrafts.after.csv"
    )
    void collectPaymentOrdersWithProcessedDrafts() {
        Set<PayoutSchedule> allowedSchedules = Set.of(DAILY, WEEKLY);
        Long count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(8, count);

        count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(0, count);
    }

    @Test
    @DisplayName("Формирование команд с минимальной суммой выплаты")
    @DbUnitDataSet(
            before = "PaymentOrderDraftDaoTest.collectPaymentOrdersWithMinAmount.before.csv",
            after = "PaymentOrderDraftDaoTest.collectPaymentOrdersWithMinAmount.after.csv"
    )
    void collectPaymentOrdersWithMinAmount() {
        Set<PayoutSchedule> allowedSchedules = Set.of(DAILY);
        Long count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                1000, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(3, count);

        count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                1000, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(0, count);
    }

    @Test
    @DisplayName("Формирование команд для outcome-контрактов")
    @DbUnitDataSet(
            before = "PaymentOrderDraftDaoTest.collectPaymentOrdersForOutcomeContract.before.csv",
            after = "PaymentOrderDraftDaoTest.collectPaymentOrdersForOutcomeContract.after.csv"
    )
    void collectPaymentOrdersForOutcome() {
        Set<PayoutSchedule> allowedSchedules = Set.of(DAILY);
        Long count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(3, count);

        count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(0, count);
    }

    @Test
    @DisplayName("Формирование команд для случая с двумя расписаниями по клиенту")
    @DbUnitDataSet(
            before = "PaymentOrderDraftDaoTest.collectPaymentOrdersForDoubleContracts.before.csv",
            after = "PaymentOrderDraftDaoTest.collectPaymentOrdersForDoubleContracts.after.csv"
    )
    void collectPaymentOrdersForDoubleContracts() {
        Set<PayoutSchedule> allowedSchedules = Set.of(DAILY);
        Long count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(10, count);

        count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(0, count);
    }

    @Test
    @DisplayName("Формирование команд за обрабатываемую дату")
    @DbUnitDataSet(
            before = "PaymentOrderDraftDaoTest.collectPaymentOrdersForProcessingDate.before.csv",
            after = "PaymentOrderDraftDaoTest.collectPaymentOrdersForProcessingDate.after.csv"
    )
    void collectPaymentOrdersForProcessingDate() {
        Set<PayoutSchedule> allowedSchedules = Set.of(DAILY);
        Long count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(2, count);

        count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(0, count);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getSuccessfulAllowedFrequencies")
    @DisplayName("Формирование команд в случае нескольких расписаний для партнёра")
    @DbUnitDataSet(
            before = "PaymentOrderDraftDaoTest.collectPaymentOrdersWithMultipleSchedules.before.csv",
            after = "PaymentOrderDraftDaoTest.collectPaymentOrdersWithMultipleSchedules.after.csv"
    )
    void collectPaymentOrdersWithMultipleSchedules(String name, Set<PayoutSchedule> allowedSchedules) {
        Long count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(6, count);

        count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(0, count);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getSuccessfulAllowedFrequencies")
    @DisplayName("Формирование команд в случае отсутствия расписаний для партнёра")
    @DbUnitDataSet(
            before = "PaymentOrderDraftDaoTest.collectPaymentOrdersWithoutSchedule.before.csv",
            after = "PaymentOrderDraftDaoTest.collectPaymentOrdersWithoutSchedule.after.csv"
    )
    void collectPaymentOrdersWithoutSchedule(String name, Set<PayoutSchedule> allowedSchedules) {
        Long count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(6, count);

        count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(0, count);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getUnsuccessfulAllowedFrequencies")
    @DisplayName("Формирование команд в случае нескольких расписаний для партнёра")
    @DbUnitDataSet(
            before = "PaymentOrderDraftDaoTest.collectPaymentOrdersWithMultipleSchedules.before.csv",
            after = "PaymentOrderDraftDaoTest.collectPaymentOrdersWithMultipleSchedules.before.csv"
    )
    void collectNoPaymentOrdersWithMultipleSchedules(String name, Set<PayoutSchedule> allowedSchedules) {
        Long count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(0, count);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getUnsuccessfulAllowedFrequencies")
    @DisplayName("Формирование команд в случае отсутствия расписаний для партнёра")
    @DbUnitDataSet(
            before = "PaymentOrderDraftDaoTest.collectPaymentOrdersWithoutSchedule.before.csv",
            after = "PaymentOrderDraftDaoTest.collectPaymentOrdersWithoutSchedule.before.csv"
    )
    void collectNoPaymentOrdersWithoutSchedule(String name, Set<PayoutSchedule> allowedSchedules) {
        Long count = paymentOrderDraftDao.collectPaymentOrdersFromDrafts(TEST_DRAFT_DATE, TEST_TIME, allowedSchedules,
                0, 100, DEFAULT_PLATFORMS, INCOME_CONTRACT);
        assertEquals(0, count);
    }

}
