package ru.yandex.market.mbi.partner_stat.mvc.partner;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.mbi.partner_stat.FunctionalTest;
import ru.yandex.market.mbi.partner_stat.entity.statistics.SummaryEntity;
import ru.yandex.market.mbi.partner_stat.repository.common.model.TimeSeries;
import ru.yandex.market.mbi.partner_stat.repository.common.model.TimeSeriesPoint;
import ru.yandex.market.mbi.partner_stat.repository.stat.StatClickHouseDao;
import ru.yandex.market.mbi.partner_stat.service.stat.model.TimeSeriesType;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Тесты для {@link MarketSalesController}
 */
public class MarketSalesControllerTest extends FunctionalTest {
    @LocalServerPort
    private int port;

    @Autowired
    private StatClickHouseDao statClickHouseDao;

    @BeforeEach
    public void init() {
        Mockito.when(
                statClickHouseDao.getTimeSeries(eq(TimeSeriesType.GMV_DELIVERED), argThat(filter -> filter.getPartnerIds().equals(List.of(1L))))
        ).thenReturn(
                new TimeSeries(
                        List.of(
                                new TimeSeriesPoint(BigDecimal.valueOf(12), 13L)
                        )
                )
        );
        Mockito.when(
                statClickHouseDao.getTimeSeries(
                        eq(TimeSeriesType.GMV_DELIVERED), argThat(filter -> filter.getPartnerIds().equals(List.of())))
        ).thenReturn(
                new TimeSeries(
                        List.of(
                                new TimeSeriesPoint(BigDecimal.valueOf(12), 13L),
                                new TimeSeriesPoint(BigDecimal.valueOf(14), 15L)
                        )
                )
        );
        Mockito.when(
                statClickHouseDao.getTimeSeriesSummary(eq(TimeSeriesType.GMV_DELIVERED), argThat(filter -> filter.getPartnerIds().equals(List.of(1L))))
        ).thenReturn(
                new SummaryEntity(BigDecimal.valueOf(1000), BigDecimal.valueOf(500))
        );
        Mockito.when(
                statClickHouseDao.getTimeSeriesSummary(eq(TimeSeriesType.GMV_DELIVERED), argThat(filter -> filter.getPartnerIds().equals(List.of())))
        ).thenReturn(
                new SummaryEntity(BigDecimal.valueOf(2000), BigDecimal.valueOf(-3000))
        );
        Mockito.when(
                statClickHouseDao.getTimeSeriesSummary(eq(TimeSeriesType.ITEMS_DELIVERED), argThat(filter -> filter.getPartnerIds().equals(List.of(1L))))
        ).thenReturn(
                new SummaryEntity(BigDecimal.valueOf(10), BigDecimal.valueOf(10))
        );
        Mockito.when(
                statClickHouseDao.getTimeSeriesSummary(eq(TimeSeriesType.ITEMS_DELIVERED), argThat(filter -> filter.getPartnerIds().equals(List.of())))
        ).thenReturn(
                new SummaryEntity(BigDecimal.valueOf(10), BigDecimal.valueOf(0))
        );
    }

    @DisplayName("Проверка получения графика продаж")
    @Test
    void testGetSalesPlot() {
        String url = getUrl("/market-sales/plot");
        String request = "" +
                "{" +
                "   \"partners\": [1]," +
                "   \"category\": 12345," +
                "   \"type\": \"GMV_DELIVERED\"" +
                "}";
        ResponseEntity<String> responseEntity = FunctionalTestHelper.post(url, new HttpEntity<>(request, jsonHeaders()));
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }

    private String getUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
