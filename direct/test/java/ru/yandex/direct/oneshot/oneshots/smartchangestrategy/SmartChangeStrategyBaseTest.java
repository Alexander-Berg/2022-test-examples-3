package ru.yandex.direct.oneshot.oneshots.smartchangestrategy;

import java.math.BigDecimal;

import ru.yandex.direct.core.entity.campaign.model.DbStrategy;

import static java.math.BigDecimal.valueOf;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpaPerCamprStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpaPerFilterrStrategy;

class SmartChangeStrategyBaseTest {

    static final BigDecimal FILTER_AVG_CPA = valueOf(100);
    static final BigDecimal FILTER_AVG_BID = valueOf(101);
    static final BigDecimal AVG_CPA = valueOf(102);
    static final BigDecimal AVG_BID = valueOf(103);
    static final BigDecimal BID = valueOf(104);
    static final BigDecimal SUM = valueOf(301);

    static DbStrategy averageCpaPerFilterStrategy(BigDecimal filterAvgCpa,
                                                  BigDecimal filterAvgBid,
                                                  BigDecimal bid,
                                                  BigDecimal sum) {
        DbStrategy strategy = defaultAverageCpaPerFilterrStrategy(100L, filterAvgCpa, bid, sum);
        strategy.getStrategyData()
                .withFilterAvgBid(filterAvgBid);
        return strategy;
    }

    static DbStrategy averageCpaPerCampStrategy(BigDecimal avgCpa,
                                                BigDecimal avgBid,
                                                BigDecimal bid,
                                                BigDecimal sum) {
        DbStrategy strategy = defaultAverageCpaPerCamprStrategy(100L, avgCpa, bid, sum);
        strategy.getStrategyData()
                .withVersion(1L)
                .withAvgBid(avgBid);
        return strategy;
    }
}
