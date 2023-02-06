package ru.yandex.market.api.partner.controllers.model.getmodels;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.api.partner.model.DefaultPapiModelService;
import ru.yandex.market.common.report.CommonMarketReportService;
import ru.yandex.market.common.report.model.SearchRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Тест написан, чтобы проверить, что мы не ходим в report с modelId = 0
 * Навеяно тикетом: MBI-30387
 *
 * @author belmatter
 */
public class DefaultPapiModelServiceTest {

    private DefaultPapiModelService papiModelService;

    @Test
    void testFor0ModelId() throws IOException, InterruptedException {

        CommonMarketReportService<SearchRequest> reportService = mock(CommonMarketReportService.class);
        papiModelService = new DefaultPapiModelService(reportService);
        Mockito.verify(reportService, never()).executeSearchAndParse(any(), any());
        papiModelService.getModels(Collections.singletonList(0L), 213L, "RUR");
    }

    @Test
    void testForNon0lModelId() throws IOException, InterruptedException {
        CommonMarketReportService<SearchRequest> reportService = mock(CommonMarketReportService.class);
        papiModelService = new DefaultPapiModelService(reportService);
        papiModelService.getModels(Collections.singletonList(444L), 213L, "RUR");
        Mockito.verify(reportService, atLeastOnce()).executeSearchAndParse(any(), any());
    }

}
