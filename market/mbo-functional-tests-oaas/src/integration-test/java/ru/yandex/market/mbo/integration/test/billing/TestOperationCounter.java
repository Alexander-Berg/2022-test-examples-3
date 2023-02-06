package ru.yandex.market.mbo.integration.test.billing;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.billing.counter.AbstractOperationCounter;
import ru.yandex.market.mbo.billing.counter.BatchUpdateData;
import ru.yandex.market.mbo.billing.counter.info.BillingOperationInfoWithPrice;
import ru.yandex.market.mbo.billing.counter.info.BillingOperationInfoWithoutPrice;
import ru.yandex.market.mbo.billing.tarif.TarifProvider;
import ru.yandex.market.mbo.utils.BatchUpdaterListener;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class TestOperationCounter extends AbstractOperationCounter {
    private List<BillingOperationInfoWithPrice> operations;
    private List<BillingOperationInfoWithoutPrice> suspendedOperations;

    public void setOperations(List<BillingOperationInfoWithPrice> operations,
                              List<BillingOperationInfoWithoutPrice> suspendedOperations) {
        this.operations = operations;
        this.suspendedOperations = suspendedOperations;
    }

    @PostConstruct
    private void init() {
        BatchUpdaterListener<BatchUpdateData> failListener = (data, e) -> {
            throw new IllegalStateException("Failed to insert data: " + data, e);
        };
        getOperationsUpdater().addFailListener(failListener);
        getSuspendedOperationsUpdater().addFailListener(failListener);
    }

    public void setOperations(List<BillingOperationInfoWithPrice> operations) {
        setOperations(operations, Collections.emptyList());
    }

    @Override
    protected void doLoad(Pair<Calendar, Calendar> interval, TarifProvider tarifProvider) {
        operations.stream()
            .filter(o -> interval.getFirst().before(o.getTime()))
            .filter(o -> o.getTime().before(interval.getSecond()))
            .forEach(o -> addOperation(interval, o));
        suspendedOperations.stream()
            .filter(o -> interval.getFirst().before(o.getTime()))
            .filter(o -> o.getTime().before(interval.getSecond()))
            .forEach(o -> addOperation(interval, tarifProvider, o));
    }
}
