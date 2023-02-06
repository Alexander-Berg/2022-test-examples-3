package ru.yandex.market.tpl.billing.processor;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.queue.calculatepvzreturnsfee.CalculatePvzReturnsFeeProcessor;
import ru.yandex.market.tpl.billing.queue.model.DatePayload;

public class CalculatePvzReturnsFeeProcessorTest extends AbstractFunctionalTest {
    @Autowired
    CalculatePvzReturnsFeeProcessor calculatePvzReturnsFeeProcessor;

    @Test
    @DbUnitDataSet(after = "/database/processor/calculatepvzreturnsfee/after/no_return_transactions_created.csv")
    void noPvzReturnTariff() {
        calculatePvzReturnsFeeProcessor.processPayload(new DatePayload(REQUEST_ID, LocalDate.of(2021, 4, 15)));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/processor/calculatepvzreturnsfee/before/pvz_tariff_exist.csv",
            after = "/database/processor/calculatepvzreturnsfee/after/no_return_transactions_created.csv")
    void noPvzReturns() {
        calculatePvzReturnsFeeProcessor.processPayload(new DatePayload(REQUEST_ID, LocalDate.of(2021, 4, 15)));
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/processor/calculatepvzreturnsfee/before/pvz_tariff_exist.csv",
                    "/database/processor/calculatepvzreturnsfee/before/single_pvz_return_for_date.csv"},
            after = "/database/processor/calculatepvzreturnsfee/after/single_return_transaction_created.csv")
    void singlePvzReturnForDate() {
        calculatePvzReturnsFeeProcessor.processPayload(new DatePayload(REQUEST_ID, LocalDate.of(2021, 4, 15)));
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/processor/calculatepvzreturnsfee/before/pvz_tariff_exist.csv",
                    "/database/processor/calculatepvzreturnsfee/before/multiple_pvz_return_for_date.csv"},
            after = "/database/processor/calculatepvzreturnsfee/after/multiple_return_transaction_created.csv"
    )
    void multiplePvzReturnForDate() {
        calculatePvzReturnsFeeProcessor.processPayload(new DatePayload(REQUEST_ID, LocalDate.of(2021, 4, 15)));
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "/database/processor/calculatepvzreturnsfee/before/pvz_tariff_exist.csv",
                    "/database/processor/calculatepvzreturnsfee/before/additional_pickup_point.csv",
                    "/database/processor/calculatepvzreturnsfee/before/multiple_pvz_return_for_multiple_pvz.csv"},
            after = "/database/processor/calculatepvzreturnsfee/after/multiple_return_transaction_for_multiple_pvz_created.csv"
    )
    void pvzReturnsForMultiplePickupPoints() {
        calculatePvzReturnsFeeProcessor.processPayload(new DatePayload(REQUEST_ID, LocalDate.of(2021, 4, 15)));
    }
}
