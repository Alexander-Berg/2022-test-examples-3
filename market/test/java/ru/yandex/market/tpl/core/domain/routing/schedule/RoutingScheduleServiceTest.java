package ru.yandex.market.tpl.core.domain.routing.schedule;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.lms.routingschedule.RoutingScheduleRuleCreateDto;
import ru.yandex.market.tpl.core.domain.lms.routingschedule.RoutingScheduleRuleSearchRequest;
import ru.yandex.market.tpl.core.domain.lms.routingschedule.view.RoutingScheduleRuleGridView;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterUtil;
import ru.yandex.market.tpl.core.domain.routing.schedule.dynamic.MainRoutingQuartzTaskRepository;
import ru.yandex.market.tpl.core.domain.routing.schedule.dynamic.PreRoutingQuartzTaskRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.ROUTING_SCHEDULE_RULE_MAX_MINUTES_BETWEEN_ENABLED;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class RoutingScheduleServiceTest {

    private static final long SORTING_CENTER_ID = 125L;
    private static final long ANOTHER_SORTING_CENTER_ID = 467L;

    private final EntityManager entityManager;

    private final RoutingScheduleService routingScheduleService;
    private final MainRoutingQuartzTaskRepository mainRoutingQuartzTaskRepository;
    private final PreRoutingQuartzTaskRepository preRoutingQuartzTaskRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.insertValue(ROUTING_SCHEDULE_RULE_MAX_MINUTES_BETWEEN_ENABLED, true);

        SortingCenter sc1 = SortingCenterUtil.sortingCenter(SORTING_CENTER_ID);
        entityManager.persist(sc1);

        SortingCenter sc2 = SortingCenterUtil.sortingCenter(ANOTHER_SORTING_CENTER_ID);
        entityManager.persist(sc2);
    }

    @Test
    void shouldReturnAllScheduleRules() {
        RoutingScheduleRuleCreateDto createDto = RoutingScheduleRuleCreateDto.builder()
                .sortingCenterId(SORTING_CENTER_ID)
                .preRoutingStartTime(LocalTime.of(21, 0))
                .mainRoutingStartTime(LocalTime.of(22, 0))
                .isSameDay(false)
                .build();
        routingScheduleService.save(createDto);

        RoutingScheduleRuleCreateDto createDto2 = RoutingScheduleRuleCreateDto.builder()
                .sortingCenterId(ANOTHER_SORTING_CENTER_ID)
                .preRoutingStartTime(LocalTime.of(21, 0))
                .mainRoutingStartTime(LocalTime.of(22, 0))
                .isSameDay(false)
                .build();
        routingScheduleService.save(createDto2);

        var routingSchedules =
                routingScheduleService.findRoutingSchedules(new RoutingScheduleRuleSearchRequest(), Pageable.unpaged());
        assertThat(routingSchedules.getTotalCount()).isEqualTo(2);
        assertThat(routingSchedules.getItems())
                .extracting(gridItem -> gridItem.getValues().get("sortingCenterId"))
                .containsExactlyInAnyOrder(SORTING_CENTER_ID, ANOTHER_SORTING_CENTER_ID);
    }

    @Test
    void shouldFilterScheduleRules() {
        RoutingScheduleRuleCreateDto createDto = RoutingScheduleRuleCreateDto.builder()
                .sortingCenterId(SORTING_CENTER_ID)
                .preRoutingStartTime(LocalTime.of(21, 0))
                .mainRoutingStartTime(LocalTime.of(22, 0))
                .isSameDay(false)
                .build();
        routingScheduleService.save(createDto);

        RoutingScheduleRuleCreateDto routingScheduleRuleGridView2 = RoutingScheduleRuleCreateDto.builder()
                .sortingCenterId(ANOTHER_SORTING_CENTER_ID)
                .preRoutingStartTime(LocalTime.of(21, 0))
                .mainRoutingStartTime(LocalTime.of(22, 0))
                .isSameDay(false)
                .build();
        routingScheduleService.save(routingScheduleRuleGridView2);

        RoutingScheduleRuleSearchRequest searchRequest = new RoutingScheduleRuleSearchRequest();
        searchRequest.setSortingCenterId(SORTING_CENTER_ID);
        var routingSchedules =
                routingScheduleService.findRoutingSchedules(searchRequest, Pageable.unpaged());
        assertThat(routingSchedules.getTotalCount()).isEqualTo(1);
        assertThat(routingSchedules.getItems())
                .extracting(gridItem -> gridItem.getValues().get("sortingCenterId"))
                .containsExactlyInAnyOrder(SORTING_CENTER_ID);
    }

    @Test
    void shouldSavePreAndMainRoutingQuartzTak() {
        RoutingScheduleRuleCreateDto routingScheduleRuleGridView = RoutingScheduleRuleCreateDto.builder()
                .sortingCenterId(SORTING_CENTER_ID)
                .preRoutingStartTime(LocalTime.of(21, 0))
                .mainRoutingStartTime(LocalTime.of(22, 0))
                .isSameDay(false)
                .build();
        RoutingScheduleRuleGridView saved = routingScheduleService.save(routingScheduleRuleGridView);

        List<PreRoutingQuartzTask> allPreRoutingQuartzTasks = preRoutingQuartzTaskRepository.findAll();
        assertThat(allPreRoutingQuartzTasks).hasSize(1);
        PreRoutingQuartzTask preRoutingQuartzTask = allPreRoutingQuartzTasks.iterator().next();
        assertThat(preRoutingQuartzTask.getSortingCenterId()).isEqualTo(SORTING_CENTER_ID);

        List<MainRoutingQuartzTask> allMainRoutingQuartzTasks = mainRoutingQuartzTaskRepository.findAll();
        assertThat(allMainRoutingQuartzTasks).hasSize(1);
        MainRoutingQuartzTask mainRoutingQuartzTask = allMainRoutingQuartzTasks.iterator().next();
        assertThat(mainRoutingQuartzTask.getSortingCenterId()).isEqualTo(SORTING_CENTER_ID);
    }

    @MethodSource("validateMaxTimeBetweenRoutingsInvalidData")
    @ParameterizedTest
    void validateMaxTimeBetweenRoutings_invalid(
            int rule1Hour,
            int rule1Minutes,
            boolean rule1IsSameDay,
            int rule2Hour,
            int rule2Minutes,
            boolean rule2IsSameDay
    ) {
        RoutingScheduleRuleCreateDto routingScheduleRuleCreateCommand = RoutingScheduleRuleCreateDto.builder()
                .sortingCenterId(SORTING_CENTER_ID)
                .mainRoutingStartTime(LocalTime.of(rule1Hour, rule1Minutes))
                .isSameDay(rule1IsSameDay)
                .build();
        routingScheduleService.save(routingScheduleRuleCreateCommand);

        assertThrows(TplIllegalArgumentException.class, () ->
                routingScheduleService.save(
                        RoutingScheduleRuleCreateDto.builder()
                                .sortingCenterId(SORTING_CENTER_ID)
                                .mainRoutingStartTime(LocalTime.of(rule2Hour, rule2Minutes))
                                .isSameDay(rule2IsSameDay)
                                .build()
                )
        );
    }

    @MethodSource("validateMaxTimeBetweenRoutingsValidData")
    @ParameterizedTest
    void validateMaxTimeBetweenRoutings_valid(
            int rule1Hour,
            int rule1Minutes,
            boolean rule1IsSameDay,
            int rule2Hour,
            int rule2Minutes,
            boolean rule2IsSameDay
    ) {
        RoutingScheduleRuleCreateDto routingScheduleRuleCreateCommand = RoutingScheduleRuleCreateDto.builder()
                .sortingCenterId(SORTING_CENTER_ID)
                .mainRoutingStartTime(LocalTime.of(rule1Hour, rule1Minutes))
                .isSameDay(rule1IsSameDay)
                .build();
        routingScheduleService.save(routingScheduleRuleCreateCommand);

        assertDoesNotThrow(() ->
                routingScheduleService.save(
                        RoutingScheduleRuleCreateDto.builder()
                                .sortingCenterId(SORTING_CENTER_ID)
                                .mainRoutingStartTime(LocalTime.of(rule2Hour, rule2Minutes))
                                .isSameDay(rule2IsSameDay)
                                .build()
                )
        );
    }

    private static Stream<Arguments> validateMaxTimeBetweenRoutingsInvalidData() {
        return Stream.of(
                /** запуск в следующий день */
                Arguments.of(1, 0, false, 5, 1, false),
                Arguments.of(5, 1, false, 1, 0, false),
                Arguments.of(23, 0, false, 0, 5, false),
                /** запуск в тот же день */
                Arguments.of(19, 58, true, 23, 59, true),
                Arguments.of(23, 59, true, 19, 58, true),
                /** запуск в следующий день пересекая полночь */
                Arguments.of(23, 0, false, 3, 5, false),
                Arguments.of(3, 5, false, 23, 0, false),
                /** запуск в тот же день пересекая полночь */
                Arguments.of(23, 59, true, 0, 5, true),
                Arguments.of(0, 5, true, 23, 59, true),
                /** один запуск в следующий день, один в тот же */
                Arguments.of(0, 5, true, 2, 0, false),
                Arguments.of(2, 0, false, 0, 5, true),
                /** конкретные кейсы с прода */
                Arguments.of(23, 0, false, 0, 5, false)
        );
    }

    private static Stream<Arguments> validateMaxTimeBetweenRoutingsValidData() {
        return Stream.of(
                /** запуск в следующий день */
                Arguments.of(1, 0, false, 5, 0, false),
                Arguments.of(5, 0, false, 1, 0, false),
                /** запуск в тот же день */
                Arguments.of(20, 0, true, 23, 59, true),
                Arguments.of(23, 59, true, 20, 0, true),
                /** запуск пересекая полночь */
                Arguments.of(23, 0, false, 0, 5, true),
                Arguments.of(0, 5, true, 23, 0, false)
        );
    }

    @Test
    void shouldDeleteScheduleRule() {
        RoutingScheduleRuleCreateDto routingScheduleRuleGridView = RoutingScheduleRuleCreateDto.builder()
                .sortingCenterId(SORTING_CENTER_ID)
                .preRoutingStartTime(LocalTime.of(21, 0))
                .mainRoutingStartTime(LocalTime.of(22, 0))
                .isSameDay(false)
                .build();
        RoutingScheduleRuleGridView savedRuleDto = routingScheduleService.save(routingScheduleRuleGridView);

        var routingSchedules =
                routingScheduleService.findRoutingSchedules(new RoutingScheduleRuleSearchRequest(), Pageable.unpaged());
        assertThat(routingSchedules.getTotalCount()).isEqualTo(1);

        routingScheduleService.deleteRoutingScheduleRule(savedRuleDto.getId());

        routingSchedules =
                routingScheduleService.findRoutingSchedules(new RoutingScheduleRuleSearchRequest(), Pageable.unpaged());
        assertThat(routingSchedules.getTotalCount()).isEqualTo(0);

        List<PreRoutingQuartzTask> allPreRoutingQuartzTasks = preRoutingQuartzTaskRepository.findAll();
        assertThat(allPreRoutingQuartzTasks).hasSize(0);

        List<MainRoutingQuartzTask> allMainRoutingQuartzTasks = mainRoutingQuartzTaskRepository.findAll();
        assertThat(allMainRoutingQuartzTasks).hasSize(0);
    }
}
