package ru.yandex.market.core.clickreport.model;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.core.clickreport.ClickReportService;

/**
 * @author zoom
 */
public class ClickSearchRequestTest extends Assert {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalExceptionWhenPageSizeIsOutOfLowerBound() {
        ClickSearchRequest request = new ClickSearchRequest();
        request.setPageSize(0);
        fail();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalExceptionWhenPageSizeIsOutOfUpperBound() {
        ClickSearchRequest request = new ClickSearchRequest();
        request.setPageSize(ClickReportService.MAX_PAGE_SIZE + 1);
        fail();
    }

    @Test
    public void shouldNotThrowAnyExceptionWhenPageSizeIsNull() {
        ClickSearchRequest request = new ClickSearchRequest();
        request.setPageSize(null);
    }

    @Test
    public void shouldNotThrowAnyExceptionWhenPageSizeIsValid() {
        ClickSearchRequest request = new ClickSearchRequest();
        request.setPageSize(1);
        request.setPageSize(ClickReportService.MAX_PAGE_SIZE);
        request.setPageSize(ClickReportService.MAX_PAGE_SIZE / 2);
    }

}