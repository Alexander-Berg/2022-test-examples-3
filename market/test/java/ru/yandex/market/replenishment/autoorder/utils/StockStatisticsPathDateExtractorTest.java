package ru.yandex.market.replenishment.autoorder.utils;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Test;

class StockStatisticsPathDateExtractorTest {

    @Test
    void getStatisticDate() {
        StockStatisticsPathDateExtractor converter = new StockStatisticsPathDateExtractor();
        LocalDate actualDate = converter.getStatisticDate("//home/market/production/mstat/dictionaries/stock_sku/1d/2021-01-11");
        Assert.assertEquals(LocalDate.of(2021, 1, 11), actualDate);
    }
}
