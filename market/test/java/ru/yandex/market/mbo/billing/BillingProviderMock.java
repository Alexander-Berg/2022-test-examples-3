package ru.yandex.market.mbo.billing;

import org.apache.commons.lang.time.DateUtils;
import ru.yandex.common.util.collections.Pair;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author s-ermakov
 */
public class BillingProviderMock implements BillingProvider {

    private final Date startDate;
    private final Date endDate;

    public BillingProviderMock(Date endDate) {
        this(DateUtils.addDays(endDate, -1), endDate);
    }

    public BillingProviderMock(Date startDate, Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public Pair<Date, Date> getInterval() {
        return Pair.of(startDate, endDate);
    }

    @Override
    public BigDecimal getPrice(Date actionDate, PaidAction paidAction) {
        throw new RuntimeException("Not implemented in mock class");
    }
}
