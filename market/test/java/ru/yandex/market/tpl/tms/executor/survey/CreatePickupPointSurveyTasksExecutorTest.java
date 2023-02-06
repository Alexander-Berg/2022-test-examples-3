package ru.yandex.market.tpl.tms.executor.survey;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.PickupPointSurveyType;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTaskRepository;
import ru.yandex.market.tpl.core.domain.pickup_point_survey.task.PickupPointSurveyTaskSubtaskStatus;
import ru.yandex.market.tpl.core.service.pickup_point_survey.PickupPointSurveyGeneratorService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SURVEY_FREQUENCY_DAYS;


@RequiredArgsConstructor
public class CreatePickupPointSurveyTasksExecutorTest extends TplTmsAbstractTest {
    private final CreatePickupPointSurveyTasksExecutor executor;
    private final PickupPointSurveyTaskRepository surveyTaskRepository;
    private final PickupPointSurveyGeneratorService pickupPointSurveyGeneratorService;
    private final PickupPointRepository pickupPointRepository;
    private final TestDataFactory testDataFactory;
    private final ConfigurationProviderAdapter configurationProviderAdapter;

    /**
     * Даже пустой {@link AfterEach} нужен
     * для срабатывания {@link ru.yandex.market.tpl.core.test.TplAbstractTest#clearAfterTest(Object)}
     */
    @AfterEach
    void afterEach() {

    }

    @Test
    public void successfullyCreatedTask() {
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1L, true)
        );
        pickupPointSurveyGeneratorService.generatePickupPointSurvey(
                List.of("test.com", "test2.com"), PickupPointSurveyType.PVZ_MARKET_BRANDED, true
        );
        assertThat(surveyTaskRepository.findAll()).isEmpty();

        executor.doRealJob(null);

        var tasks = surveyTaskRepository.findAll();

        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.CREATED);

        clearAfterTest(pickupPoint);
    }

    @Test
    public void tasksAreNotCreated() {
        // ПВЗ небрендированный, таски на опрос не создаем
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1L, false)
        );
        pickupPointSurveyGeneratorService.generatePickupPointSurvey(
                List.of("test.com", "test2.com"), PickupPointSurveyType.PVZ_MARKET_BRANDED, true
        );
        assertThat(surveyTaskRepository.findAll()).isEmpty();

        executor.doRealJob(null);

        var tasks = surveyTaskRepository.findAll();

        assertThat(tasks).hasSize(0);
        clearAfterTest(pickupPoint);
    }

    @Test
    @DisplayName("Новая таска не создается, когда есть 1 активная (на 1 пвз)")
    public void newTaskIsNotCreatedBecauseThereIsActiveOne() {
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1L, true)
        );
        pickupPointSurveyGeneratorService.generatePickupPointSurvey(
                List.of("test.com", "test2.com"), PickupPointSurveyType.PVZ_MARKET_BRANDED, true
        );
        assertThat(surveyTaskRepository.findAll()).isEmpty();

        executor.doRealJob(null);

        var tasks = surveyTaskRepository.findAll();

        assertThat(tasks).hasSize(1);
        var surveyTask = tasks.get(0);
        assertThat(surveyTask.getStatus()).isEqualTo(PickupPointSurveyTaskSubtaskStatus.CREATED);

        // Еще раз вызываю этот метод чтобы убедиться что не будет выкинуто исключение unique constraint
        executor.doRealJob(null);

        clearAfterTest(pickupPoint);
    }

    @Test
    @DisplayName("Новая таска не создается, когда есть 1 завершенная и 1 активная (на 1 пвз)")
    public void newTaskIsNotCreatedWithOldBecauseThereIsActiveOne() {
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1L, true)
        );
        pickupPointSurveyGeneratorService.generatePickupPointSurvey(
                List.of("test.com", "test2.com"), PickupPointSurveyType.PVZ_MARKET_BRANDED, true
        );
        assertThat(surveyTaskRepository.findAll()).isEmpty();

        executor.doRealJob(null);

        var tasks = surveyTaskRepository.findAll();

        assertThat(tasks).hasSize(1);
        var surveyTask = tasks.get(0);

        // создаю условие чтобы создалась вторая задача на опрос
        surveyTask.setStatus(PickupPointSurveyTaskSubtaskStatus.FINISHED);
        surveyTask.setClosedAt(Instant.EPOCH);
        surveyTaskRepository.save(surveyTask);

        executor.doRealJob(null);

        tasks = surveyTaskRepository.findAll();
        assertThat(tasks).hasSize(2);

        // тут не должна создаться задача, т.к. есть 1 активная
        assertDoesNotThrow(() -> executor.doRealJob(null));
        tasks = surveyTaskRepository.findAll();
        assertThat(tasks).hasSize(2);

        clearAfterTest(pickupPoint);
    }

    @Test
    @DisplayName("Новая таска не создается, когда есть 1 завершенная более чем 14(FREQUENCY) дней назад " +
            "и 1 менее чем 14(FREQUENCY) дней назад (на 1 пвз)")
    public void newTaskIsNotCreatedWithOldBecauseThereIsRecentlyClosedOne() {
        var surveyFrequency = 14;
        Mockito.doReturn(Optional.of(surveyFrequency))
                .when(configurationProviderAdapter).getValueAsInteger(SURVEY_FREQUENCY_DAYS);

        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1L, true)
        );
        pickupPointSurveyGeneratorService.generatePickupPointSurvey(
                List.of("test.com", "test2.com"), PickupPointSurveyType.PVZ_MARKET_BRANDED, true
        );
        assertThat(surveyTaskRepository.findAll()).isEmpty();

        executor.doRealJob(null);

        var tasks = surveyTaskRepository.findAll();

        assertThat(tasks).hasSize(1);
        var surveyTask = tasks.get(0);

        // создаю условие чтобы создалась вторая задача на опрос
        surveyTask.setStatus(PickupPointSurveyTaskSubtaskStatus.FINISHED);
        surveyTask.setClosedAt(Instant.EPOCH);
        surveyTaskRepository.save(surveyTask);

        executor.doRealJob(null);

        tasks = surveyTaskRepository.findAll();
        assertThat(tasks).hasSize(2);
        tasks.stream()
                .filter(task -> task.getStatus() != PickupPointSurveyTaskSubtaskStatus.FINISHED)
                .forEach(
                        task -> {
                            task.setStatus(PickupPointSurveyTaskSubtaskStatus.FINISHED);
                            // Имитирую что запрос завершился меньше, чем 14 (surveyFrequency) дней назад
                            task.setClosedAt(task.getCreatedAt().minus(Duration.ofDays(surveyFrequency - 5)));
                            surveyTaskRepository.save(task);
                        }
                );

        // тут не должна создаться задача, т.к. есть 1 недавно завершенная
        // и 1, которая завершилась более, чем 14(surveyFrequency) дней назад
        assertDoesNotThrow(() -> executor.doRealJob(null));
        tasks = surveyTaskRepository.findAll();
        assertThat(tasks).hasSize(2);

        clearAfterTest(pickupPoint);
    }
}
