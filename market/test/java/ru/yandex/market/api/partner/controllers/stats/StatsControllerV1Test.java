package ru.yandex.market.api.partner.controllers.stats;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.api.partner.controllers.stats.model.OffersStats;
import ru.yandex.market.api.partner.request.InvalidRequestException;
import ru.yandex.market.api.partner.request.PartnerServletRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.core.clickreport.ClickReportService.MAX_PAGE_SIZE;

/**
 * @author zoom
 */
public class StatsControllerV1Test extends Assert {

    @Mock
    private StatService statService;

    @Mock
    private PartnerServletRequest request;

    private StatsControllerV1 controller;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        controller = new StatsControllerV1(statService);
        prepareStatService();
    }

    @Test(expected = InvalidRequestException.class)
    public void shouldThrowExceptionWhenPageSizeIsOutOfLowerBound() {
        controller.offersStats(request, 0, null, null, null, null, 0);
        fail();
    }

    @Test(expected = InvalidRequestException.class)
    public void shouldThrowExceptionWhenPageSizeIsOutOfUpperBound() {
        controller.offersStats(request, 0, null, null, null, null, MAX_PAGE_SIZE + 1);
        fail();
    }

    @Test
    public void shouldNotThrowExceptionWhenNullPageSize() {
        controller.offersStats(request, 0, null, null, null, null, null);
    }

    @Test
    public void shouldNotThrowExceptionWheValidPageSize() {
        controller.offersStats(request, 0, null, null, null, null, 1);
        controller.offersStats(request, 0, null, null, null, null, MAX_PAGE_SIZE);
        controller.offersStats(request, 0, null, null, null, null, MAX_PAGE_SIZE / 2);
    }


    private void prepareStatService() {
        OffersStats stats = new OffersStats();
        stats.setOfferStats(Collections.emptyList());
        doReturn(stats)
                .when(statService).
                getOfferStat(
                        any(),
                        anyLong(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any());
    }
}