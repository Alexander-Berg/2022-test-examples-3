package ru.yandex.market.tpl.billing.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;

public class PaymentsServiceTest extends AbstractFunctionalTest {

    @Autowired
    private PaymentsService paymentsService;
    @Autowired
    private TestableClock clock;

    private static final LocalDate CALC_DATE = LocalDate.of(2022, 3, 22);

    @Test
    @DbUnitDataSet(
            before = "/database/service/paymentsservice/before/payments_created.csv",
            after = "/database/service/paymentsservice/after/payments_created.csv")
    void paymentsCreated() {
        clock.setFixed(Instant.parse("2022-03-22T12:00:00Z"), ZoneOffset.ofHours(+3));
        paymentsService.calculateNewPayments(CALC_DATE);
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/paymentsservice/before/payments_created_filtered.csv",
            after = "/database/service/paymentsservice/after/payments_created_filtered.csv"
    )
    void paymentsCreatedOnlyForAllowed() {
        clock.setFixed(Instant.parse("2022-03-22T12:00:00Z"), ZoneOffset.ofHours(+3));
        paymentsService.calculateNewPayments(CALC_DATE);
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/paymentsservice/before/payments_not_created_for_disabled_partners.csv",
            after = "/database/service/paymentsservice/after/payments_not_created_for_disabled_partners.csv"
    )
    void paymentsNotCreateForDisabledPartners() {
        clock.setFixed(Instant.parse("2022-03-22T12:00:00Z"), ZoneOffset.ofHours(+3));
        paymentsService.calculateNewPayments(CALC_DATE);
    }
}
