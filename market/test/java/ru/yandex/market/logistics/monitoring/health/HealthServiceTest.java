package ru.yandex.market.logistics.monitoring.health;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.application.monitoring.MonitoringUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Сервис живости приложения")
class HealthServiceTest extends AbstractTest {

    @Test
    @DisplayName("Дублирование имен хелсчекеров")
    void duplicateNames() {
        List<HealthChecker> healthCheckers = prepareMockCheckers(List.of("name", "name"));

        softly.assertThatThrownBy(() -> new HealthService(new ComplexMonitoring(), healthCheckers))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unit name already exists.");
    }

    @Test
    @DisplayName("Уже существующий мониторинг")
    void existingMonitoringUnitName() {
        List<HealthChecker> healthCheckers = prepareMockCheckers(List.of("name"));
        ComplexMonitoring monitoring = new ComplexMonitoring();
        monitoring.createUnit("name");

        softly.assertThatThrownBy(() -> new HealthService(monitoring, healthCheckers))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unit name already exists.");
    }

    @Test
    @DisplayName("Не указано имя хелсчекера")
    void nullName() {
        List<HealthChecker> healthCheckers = prepareMockCheckers(Collections.singletonList(null));

        softly.assertThatThrownBy(() -> new HealthService(new ComplexMonitoring(), healthCheckers))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Регистрация каждого хелсчекера в мониторинге")
    void monitoringUnitsCreation() {
        List<String> names = List.of("name1", "name2", "name3");
        List<HealthChecker> healthCheckers = prepareMockCheckers(names);
        ComplexMonitoring monitoring = spy(ComplexMonitoring.class);

        new HealthService(monitoring, healthCheckers);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(monitoring, times(3)).createUnit(nameCaptor.capture());
        softly.assertThat(nameCaptor.getAllValues()).isEqualTo(names);
    }

    @Test
    @DisplayName("Метод updateChecks дергает каждый хелсчекер")
    void updateChecksEveryone() {
        List<String> names = List.of("name1", "name2", "name3");
        List<HealthChecker> healthCheckers = prepareMockCheckers(names);

        new HealthService(new ComplexMonitoring(), healthCheckers).updateChecks();

        EntryStream.of(names)
            .forKeyValue((index, name) -> verify(healthCheckers.get(index)).doCheck(new MonitoringUnit(name)));
    }

    @Test
    @DisplayName("Метод getHealthStatus дергает проверки и возвращает ответ мониторинга")
    void getHealthStatus() {
        List<HealthChecker> healthCheckers = prepareMockCheckers(List.of("name"));
        ComplexMonitoring monitoring = spy(ComplexMonitoring.class);
        HealthService healthService = new HealthService(monitoring, healthCheckers);

        ComplexMonitoring.Result monitoringResult = new ComplexMonitoring.Result(MonitoringStatus.WARNING, "some msg");
        when(monitoring.getResult()).thenReturn(monitoringResult);

        softly.assertThat(healthService.getHealthStatus())
            .isEqualTo(monitoringResult);
        verify(healthCheckers.get(0)).doCheck(new MonitoringUnit("name"));
    }

    @Nonnull
    private static List<HealthChecker> prepareMockCheckers(List<String> names) {
        return StreamEx.of(names)
            .mapToEntry(name -> mock(HealthChecker.class))
            .peekKeyValue((name, healthChecker) -> when(healthChecker.getName()).thenReturn(name))
            .values()
            .toList();
    }
}
