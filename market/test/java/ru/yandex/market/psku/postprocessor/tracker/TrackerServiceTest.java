package ru.yandex.market.psku.postprocessor.tracker;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.GenerationTaskType;
import ru.yandex.market.psku.postprocessor.config.TrackerServicesManualTestConfig;
import ru.yandex.market.psku.postprocessor.service.tracker.PskuTrackerService;
import ru.yandex.market.psku.postprocessor.service.tracker.models.CategoryTrackerInfo;
import ru.yandex.market.psku.postprocessor.service.tracker.models.InfoTicketProcessingResult;
import ru.yandex.market.psku.postprocessor.service.tracker.models.PskuTrackerInfo;

import java.util.Collections;

@ContextConfiguration(classes = TrackerServicesManualTestConfig.class)
public class TrackerServiceTest extends BaseDBTest {

    @Autowired
    TrackerServicesManualTestConfig config;

    @Ignore("Мануальный тест для проверки создания и закрытия тикетов в тестовом стартрекере")
    @Test
    public void createTicket() {
        PskuTrackerService trackerService = config.pskuTrackerService();

        CategoryTrackerInfo categoryInfo = new CategoryTrackerInfo(91L, "Test category name");
        PskuTrackerInfo pskuInfo = new PskuTrackerInfo(92L, "Test psku name", "Test comment");

        String ticketKey = trackerService.createGenerationTicket(
            categoryInfo,
            Collections.singleton(pskuInfo),
            GenerationTaskType.CLUSTER
        );

        InfoTicketProcessingResult result = new InfoTicketProcessingResult();
        result.setCanceled(true);

        trackerService.closeGenerationTicket(ticketKey, result, GenerationTaskType.CLUSTER);
    }
}
