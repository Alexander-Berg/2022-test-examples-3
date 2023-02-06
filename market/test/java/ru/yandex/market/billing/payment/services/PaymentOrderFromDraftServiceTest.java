package ru.yandex.market.billing.payment.services;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тест для {@link PaymentOrderFromDraftService}.
 */
class PaymentOrderFromDraftServiceTest extends FunctionalTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2021, 8, 1);
    private static final LocalDate DAILY_TEST_DATE = LocalDate.of(2021, 8, 2);
    private static final LocalDate WEEKLY_TEST_DATE = LocalDate.of(2021, 8, 8);
    private static final LocalDate BIWEEKLY_TEST_DATE = LocalDate.of(2021, 8, 16);
    private static final LocalDate TPL_BIWEEKLY_TEST_DATE = LocalDate.of(2021, 8, 18);

    @Autowired
    private PaymentOrderFromDraftService paymentOrderFromDraftService;
    @Autowired
    private PaymentOrderFromDraftService tplCourierPaymentOrderFromDraftService;

    @Test
    @DisplayName("Данных нет")
    @DbUnitDataSet(
            before = "PaymentOrderFromDraftServiceTest.createPaymentOrderNoData.before.csv",
            after = "PaymentOrderFromDraftServiceTest.createPaymentOrderNoData.before.csv"
    )
    void createPaymentOrderFromDraftsWithNoData() {
        paymentOrderFromDraftService.createPaymentOrderFromDrafts(TEST_DATE);
    }

    @Test
    @DisplayName("Создание команд для всех расписаний")
    @DbUnitDataSet(
            before = "PaymentOrderFromDraftServiceTest.createPaymentOrderFromDrafts.before.csv",
            after = "PaymentOrderFromDraftServiceTest.createPaymentOrderFromDrafts.after.csv"
    )
    void createPaymentOrderFromDrafts() {
        paymentOrderFromDraftService.createPaymentOrderFromDrafts(TEST_DATE);
    }

    @Test
    @DisplayName("Создание команд для расписания daily")
    @DbUnitDataSet(
            before = "PaymentOrderFromDraftServiceTest.createPaymentOrderFromDraftsForDailySchedule.before.csv",
            after = "PaymentOrderFromDraftServiceTest.createPaymentOrderFromDraftsForDailySchedule.after.csv"
    )
    void createPaymentOrderFromDraftsForDailySchedule() {
        paymentOrderFromDraftService.createPaymentOrderFromDrafts(DAILY_TEST_DATE);
    }

    @Test
    @DisplayName("Создание команд для расписания weekly")
    @DbUnitDataSet(
            before = "PaymentOrderFromDraftServiceTest.createPaymentOrderFromDraftsForWeeklySchedule.before.csv",
            after = "PaymentOrderFromDraftServiceTest.createPaymentOrderFromDraftsForWeeklySchedule.after.csv"
    )
    void createPaymentOrderFromDraftsForWeeklySchedule() {
        paymentOrderFromDraftService.createPaymentOrderFromDrafts(WEEKLY_TEST_DATE);
    }

    @Test
    @DisplayName("Создание команд для расписания bi-weekly")
    @DbUnitDataSet(
            before = "PaymentOrderFromDraftServiceTest.createPaymentOrderFromDraftsForBiWeeklySchedule.before.csv",
            after = "PaymentOrderFromDraftServiceTest.createPaymentOrderFromDraftsForBiWeeklySchedule.after.csv"
    )
    void createPaymentOrderFromDraftsForBiWeeklySchedule() {
        paymentOrderFromDraftService.createPaymentOrderFromDrafts(BIWEEKLY_TEST_DATE);
    }

    @Test
    @DisplayName("Данных нет для платформы tpl")
    @DbUnitDataSet(
            before = "PaymentOrderFromDraftServiceTest.createPaymentOrderNoDataForTpl.before.csv",
            after = "PaymentOrderFromDraftServiceTest.createPaymentOrderNoDataForTpl.before.csv"
    )
    void createPaymentOrderFromDraftsWithNoDataForTpl() {
        tplCourierPaymentOrderFromDraftService.createPaymentOrderFromDrafts(TEST_DATE);
    }

    @Test
    @DisplayName("Создание команд для всех расписаний, для платформы tpl")
    @DbUnitDataSet(
            before = "PaymentOrderFromDraftServiceTest.createPaymentOrderFromDrafts.before.csv",
            after = "PaymentOrderFromDraftServiceTest.createTplCourierPaymentOrderFromDrafts.after.csv"
    )
    void createTplCourierPaymentOrderFromDrafts() {
        tplCourierPaymentOrderFromDraftService.createPaymentOrderFromDrafts(TEST_DATE);
    }

    @Test
    @DisplayName("Создание команд для расписания daily, для платформы tpl")
    @DbUnitDataSet(
            before = "PaymentOrderFromDraftServiceTest.createPaymentOrderFromDraftsForDailySchedule.before.csv",
            after = "PaymentOrderFromDraftServiceTest.createTplCourierPaymentOrderFromDraftsForDailySchedule.after.csv"
    )
    void createTplCourierPaymentOrderFromDraftsForDailySchedule() {
        tplCourierPaymentOrderFromDraftService.createPaymentOrderFromDrafts(DAILY_TEST_DATE);
    }

    @Test
    @DisplayName("Создание команд для расписания weekly, для платформы tpl")
    @DbUnitDataSet(
            before = "PaymentOrderFromDraftServiceTest.createPaymentOrderFromDraftsForWeeklySchedule.before.csv",
            after = "PaymentOrderFromDraftServiceTest.createTplCourierPaymentOrderFromDraftsForWeeklySchedule.after.csv"
    )
    void createTplCourierPaymentOrderFromDraftsForWeeklySchedule() {
        tplCourierPaymentOrderFromDraftService.createPaymentOrderFromDrafts(WEEKLY_TEST_DATE);
    }

    @Test
    @DisplayName("Создание команд для расписания bi-weekly, для платформы tpl")
    @DbUnitDataSet(
            before = "PaymentOrderFromDraftServiceTest.createPaymentOrderFromDraftsForBiWeeklySchedule.before.csv",
            after =
                 "PaymentOrderFromDraftServiceTest.createTplCourierPaymentOrderFromDraftsForBiWeeklySchedule.after.csv"
    )
    void createTplCourierPaymentOrderFromDraftsForBiWeeklySchedule() {
        tplCourierPaymentOrderFromDraftService.createPaymentOrderFromDrafts(BIWEEKLY_TEST_DATE);
    }

    @Test
    @DisplayName("Создание команд для курьерских компаний с offset на границе месяца")
    @DbUnitDataSet(
            before = "PaymentOrderFromDraftServiceTest.offsetAtMonthBegining.before.csv",
            after =
                    "PaymentOrderFromDraftServiceTest.offsetAtMonthBegining.after.csv"
    )
    void createPaymentOrderFromDraftsWithOffsetAtMonthBegining() {
        tplCourierPaymentOrderFromDraftService.createPaymentOrderFromDrafts(WEEKLY_TEST_DATE);
    }

    @Test
    @DisplayName("Создание команд для курьерских компаний с offset")
    @DbUnitDataSet(
            before = "PaymentOrderFromDraftServiceTest.createPaymentOrderFromDraftsWithOffset.before.csv",
            after =
                    "PaymentOrderFromDraftServiceTest.createPaymentOrderFromDraftsWithOffset.after.csv"
    )
    void createPaymentOrderFromDraftsWithOffset() {
        tplCourierPaymentOrderFromDraftService.createPaymentOrderFromDrafts(TPL_BIWEEKLY_TEST_DATE);
    }
}
