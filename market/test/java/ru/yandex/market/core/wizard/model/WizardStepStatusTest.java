package ru.yandex.market.core.wizard.model;

import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.market.api.cpa.log.model.LogStat;
import ru.yandex.market.core.program.partner.model.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для {@link WizardStepStatus}.
 *
 * @author Vladislav Bauer
 */
class WizardStepStatusTest {

    /**
     * Проверить что метод {@link WizardStepStatus#toString()} возвращает полную информацию о статусе.
     * Полезно для отладки и разбора проблем.
     */
    @Test
    void testToString() {
        WizardStepStatus status = WizardStepStatus.newBuilder()
                .withStep(WizardStepType.FEED)
                .withStatus(Status.FILLED)
                .withDetails(Map.of("logStat", LogStat.newBuilder()
                        .withSuccessCount(101L)
                        .withErrorCount(0L)
                        .withCount(101L)
                        .build()))
                .build();
        String actual = status.toString();
        assertThat(actual).isEqualTo("WizardStepStatus{step=FEED, status=FILLED, " +
                "details={logStat=LogStat{minEventtime=null, maxEventtime=null, " +
                "successCount=101, errorCount=0, count=101}}}");
    }

}
