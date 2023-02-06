package ru.yandex.market.loyalty.core.service.report;

import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.common.parser.LiteInputStreamParser;
import ru.yandex.market.common.report.DefaultMarketReportService;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.ReportException;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.monitor.CoreMonitorType;
import ru.yandex.market.loyalty.core.service.ReportService;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.monitoring.PushMonitor;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.loyalty.core.utils.MarketReportServiceMockUtil.mockSearchAndParseWithException;
import static ru.yandex.market.loyalty.core.utils.MarketReportServiceMockUtil.mockSearchAndParseWithSleep;

@ActiveProfiles("monitor-mock-test")
public class ReportExecutorTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final long REGION_ID = 213;

    @Autowired
    private ReportService reportService;
    @Autowired
    private DefaultMarketReportService marketReportService;
    @Autowired
    private PushMonitor monitor;

    @Test
    public void shouldRetryReportThreeTimesForEachBatch() throws Exception {
        mockSearchAndParseWithException(marketReportService, ReportException.class);

        var itemKeys = LongStream.range(1, 31)
                .mapToObj(l -> ItemKey.ofFeedOffer(l, String.valueOf(l)))
                .collect(Collectors.toList());

        reportService.getOffers(new HashSet<>(itemKeys), CoreMarketPlatform.WHITE, REGION_ID);

        verify(monitor, times(0))
                .addTemporaryCritical(eq(CoreMonitorType.REPORT_SERVICE_FULL_QUEUE), anyString(), anyLong(), any());
        verify(monitor, times(0))
                .addTemporaryCritical(eq(CoreMonitorType.REPORT_SERVICE_EXECUTION_ERROR), anyString(), anyLong(),
                        any());
        for (int i = 0; i < itemKeys.size(); i += ReportService.CHUNK_SIZE) {
            var itemKey = itemKeys.get(i);
            verify(marketReportService, times(3))
                    .executeSearchAndParse(
                            argThat(req -> req.getOfferIds().contains(
                                    FeedOfferId.from(itemKey.getFeedId(), itemKey.getOfferId())
                            )),
                            any(LiteInputStreamParser.class)
                    );
        }
    }

    @Test
    public void shouldCatchExceptionWhenExecutorQueueOverfilled() throws Exception {
        mockSearchAndParseWithSleep(marketReportService, 10000);

        var itemKeys = LongStream.range(1, 1001)
                .mapToObj(l -> ItemKey.ofFeedOffer(l, String.valueOf(l)))
                .collect(Collectors.toList());

        var result = reportService.getOffers(
                new HashSet<>(itemKeys),
                CoreMarketPlatform.WHITE,
                REGION_ID
        );

        assertTrue("Result isn't null", result.isEmpty());
        verify(monitor, atLeastOnce())
                .addTemporaryCritical(eq(CoreMonitorType.REPORT_SERVICE_FULL_QUEUE), anyString(), anyLong(), any());
        verify(monitor, times(0))
                .addTemporaryCritical(eq(CoreMonitorType.REPORT_SERVICE_EXECUTION_ERROR), anyString(), anyLong(),
                        any());
    }
}
