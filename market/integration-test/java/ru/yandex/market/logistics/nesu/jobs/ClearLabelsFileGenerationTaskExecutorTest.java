package ru.yandex.market.logistics.nesu.jobs;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.executor.ClearLabelsFileGenerationTaskExecutor;
import ru.yandex.market.logistics.nesu.service.document.LabelsFileGenerationService;
import ru.yandex.market.logistics.nesu.service.mds.MdsFileService;

@DisplayName("Удаление старых заданий на печать ярлыков заказов")
@DatabaseSetup("/jobs/executors/clear_label_file_generation_task_setup.xml")
public class ClearLabelsFileGenerationTaskExecutorTest extends AbstractContextualTest {
    private static final int TIME_TO_LIVE_IN_DAYS = 2;
    private static final Instant INSTANT = Instant.parse("2020-02-05T12:00:00.00Z");

    @Autowired
    private LabelsFileGenerationService labelsFileGenerationService;

    @Autowired
    private MdsFileService mdsFileService;

    private ClearLabelsFileGenerationTaskExecutor clearLabelsFileGenerationTaskExecutor;

    @BeforeEach
    void setup() {
        clearLabelsFileGenerationTaskExecutor = new ClearLabelsFileGenerationTaskExecutor(
            labelsFileGenerationService,
            mdsFileService,
            clock,
            TIME_TO_LIVE_IN_DAYS
        );
        clock.setFixed(INSTANT, ZoneId.systemDefault());
    }

    @Test
    @DisplayName("Удаление старых заданий на печать ярлыков заказов")
    @ExpectedDatabase(
        value = "/jobs/executors/clear_label_file_generation_task_result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void doJob() {
        clearLabelsFileGenerationTaskExecutor.doJob(null);
    }
}
