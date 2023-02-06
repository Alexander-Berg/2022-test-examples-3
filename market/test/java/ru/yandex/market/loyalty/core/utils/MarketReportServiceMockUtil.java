package ru.yandex.market.loyalty.core.utils;

import ru.yandex.market.common.parser.LiteInputStreamParser;
import ru.yandex.market.common.report.DefaultMarketReportService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class MarketReportServiceMockUtil {
    public static <T extends Throwable> void mockSearchAndParseWithException(
            DefaultMarketReportService marketReportService,
            Class<T> exceptionClazz
    ) throws Exception {
        when(marketReportService.executeSearchAndParse(any(), any(LiteInputStreamParser.class)))
                .thenThrow(exceptionClazz);
    }

    public static void mockSearchAndParseWithSleep(
            DefaultMarketReportService marketReportService,
            long sleepMillis
    ) throws Exception {
        when(marketReportService.executeSearchAndParse(any(), any(LiteInputStreamParser.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(sleepMillis);
                    return null;
                });
    }
}
