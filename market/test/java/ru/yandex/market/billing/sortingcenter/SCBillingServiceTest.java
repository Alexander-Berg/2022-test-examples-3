package ru.yandex.market.billing.sortingcenter;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class SCBillingServiceTest extends FunctionalTest {

    private static final LocalDate START_DATE = LocalDate.of(2021, 12, 20);

    @Autowired
    private SCBillingService scBillingService;

    @DbUnitDataSet(
            before = "SCBillingServiceTest.process.before.csv",
            after = "SCBillingServiceTest.process.after.csv"
    )
    @DisplayName("Общий тест функционирования сервиса.")
    @Test
    void process() {
        scBillingService.process(START_DATE);
    }

    @DbUnitDataSet(
            before = "SCBillingServiceTest.process.before.csv",
            after = "SCBillingServiceTest.process.after.csv"
    )
    @DisplayName("Проверка при кол-ве операций ниже минимума ")
    @Test
    void processBelowMinimal() {
        scBillingService.process(START_DATE);
    }

    @DbUnitDataSet(
            before = "SCBillingServiceTest.emptyTariffs.before.csv",
            after = "SCBillingServiceTest.emptyTariffs.after.csv"
    )
    @DisplayName("Тест с пустыми КГТ тарифами. (Как в Московских СЦ)")
    @Test
    void emptyTariffs() {
        scBillingService.process(START_DATE);
    }
}
