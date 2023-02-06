package ru.yandex.market.logistics.monitoring.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.application.monitoring.MonitoringUnit;
import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.common.ping.PingChecker;

@DisplayName("Хелсчекер на основе PingChecker")
class PingCheckerHealthCheckTest extends AbstractTest {

    @ParameterizedTest
    @EnumSource(CheckResult.Level.class)
    @DisplayName("Передача статуса и сообщений")
    void resultPassing(CheckResult.Level level) {
        MonitoringUnit monitoringUnit = new MonitoringUnit("ping");
        String message = "Result " + level;
        PingChecker pingChecker = () -> new CheckResult(level, message);

        new PingCheckerHealthCheck("ping", pingChecker).doCheck(monitoringUnit);

        softly.assertThat(monitoringUnit.getMessage()).isEqualTo(message);
        softly.assertThat(monitoringUnit.getException()).isNull();
        softly.assertThat(monitoringUnit.getStatus())
            .extracting(Enum::name)
            .extracting(CheckResult.Level::valueOf)
            .isEqualTo(level);
    }

    @Test
    @DisplayName("Выпадение исключения перехватится")
    void exceptionCapture() {
        MonitoringUnit monitoringUnit = new MonitoringUnit("ping");
        String message = "Something bad happen";
        PingChecker pingChecker = () -> {
            throw new IllegalStateException(message);
        };

        new PingCheckerHealthCheck("ping", pingChecker).doCheck(monitoringUnit);

        softly.assertThat(monitoringUnit.getMessage()).isEqualTo(message);
        softly.assertThat(monitoringUnit.getStatus()).isEqualTo(MonitoringStatus.CRITICAL);
        softly.assertThat(monitoringUnit.getException())
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(message);
    }
}
