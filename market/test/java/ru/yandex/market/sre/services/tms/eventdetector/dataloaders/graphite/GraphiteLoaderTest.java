package ru.yandex.market.sre.services.tms.eventdetector.dataloaders.graphite;

import org.junit.Test;

public class GraphiteLoaderTest {
    @Test
    public void testReplace() {
        String src = "divideSeries(movingAverage(one_min.market-front-blue-touch.timings-dynamic.page" +
                ".blue-market_search.0_99,%2730minute%27),sumSeries(minSeries(timeStack(movingAverage(one_min" +
                ".market-front-blue-touch.timings-dynamic.page.blue-market_search.0_99,%2730minute%27),%27-1week%27," +
                "1,4)),stddevSeries(timeStack(movingAverage(one_min.market-front-blue-touch.timings-dynamic.page" +
                ".blue-market_search.0_99,%2730minute%27),%27-1week%27,1,4),movingAverage(one_min" +
                ".market-front-blue-touch.timings-dynamic.page.blue-market_search.0_99,%2730minute%27)),stdev(one_min" +
                ".market-front-blue-touch.timings-dynamic.page.blue-market_search.0_99,60,0)))";
        System.out.println(src.replaceAll("\\)", "%29"));
    }
}
