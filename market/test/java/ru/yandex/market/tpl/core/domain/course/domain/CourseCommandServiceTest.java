package ru.yandex.market.tpl.core.domain.course.domain;

import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.LockModeType;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

import ru.yandex.market.tpl.api.model.course.CourseStatus;
import ru.yandex.market.tpl.api.model.course.UserStatusRequirement;
import ru.yandex.market.tpl.core.domain.base.CommandServiceUnitTest;
import ru.yandex.market.tpl.core.domain.base.DomainEvent;
import ru.yandex.market.tpl.core.domain.base.EntityEvent;
import ru.yandex.market.tpl.core.domain.course.domain.history.CourseEntityDifference;
import ru.yandex.market.tpl.core.domain.course.events.CourseConfigChangedEvent;
import ru.yandex.market.tpl.core.domain.course.events.CourseCreatedEvent;
import ru.yandex.market.tpl.core.domain.course.events.CoursePublishedEvent;
import ru.yandex.market.tpl.core.domain.course.events.CourseStatusChangedEvent;
import ru.yandex.market.tpl.core.domain.course.events.CourseUnpublishedEvent;
import ru.yandex.market.tpl.core.domain.course.events.CourseUpdatedEvent;
import ru.yandex.market.tpl.core.domain.eventlog.CommandProcessedEvent;
import ru.yandex.market.tpl.core.exception.CommandFailedException;
import ru.yandex.market.tpl.core.exception.TplInvalidTransitionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

/**
 * @author sekulebyakin
 */
public class CourseCommandServiceTest extends CommandServiceUnitTest {

    @InjectMocks
    private CourseCommandService courseCommandService;

    @Test
    void createCourseTest() {
        var command = createTestCourseCommand(UserStatusRequirement.NEWBIE_ONLY);
        doAnswer(invocation -> {
            var entity = invocation.getArgument(0, Course.class);
            entity.setId(55L);
            return entity;
        }).when(entityManager).persist(any(Course.class));
        var course = courseCommandService.createCourse(command);
        checkCourseData(course, command, CourseStatus.NEW, false);

        var events = getPublishedEvents();
        assertThat(events).hasSize(1);
        verifyEvent(events.get(0), CourseCreatedEvent.class, course);
    }

    @Test
    void updateCourseTest() {
        var course = createTestCourse();
        var command = CourseCommand.Update.builder()
                .courseId(course.getId())
                .name("new-name")
                .programId("new-program")
                .imageUrl("new-img-url")
                .userStatusRequirement(UserStatusRequirement.NEWBIE_ONLY)
                .description("new-desc")
                .expectedDurationInMinutes(25)
                .beta(true)
                .build();
        course = courseCommandService.updateCourse(command);
        checkCourseData(course, command, CourseStatus.NEW, true);
        var events = getPublishedEvents();
        assertThat(events).hasSize(2); // 2 - CmdOK
        verifyEvent(events.get(0), CourseUpdatedEvent.class, course);
    }

    @Test
    void updateCourseNoChanges() {
        var course = createTestCourse();
        var command = CourseCommand.Update.builder()
                .courseId(course.getId())
                .name(course.getName())
                .programId(course.getProgramId())
                .imageUrl(course.getImageUrl())
                .userStatusRequirement(course.getUserStatusRequirement())
                .description(course.getDescription())
                .expectedDurationInMinutes(course.getExpectedDurationMinutes())
                .build();
        courseCommandService.updateCourse(command);
        var events = getPublishedEvents();
        assertThat(events).hasSize(1); // 1 - CmdOK
        assertThat(events.get(0)).isInstanceOf(CommandProcessedEvent.class);
    }

    @Test
    void publishCourseTest() {
        var course = createTestCourse();
        var command = CourseCommand.Publish.builder().courseId(course.getId()).build();
        course = courseCommandService.publishCourse(command);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.PUBLISHED);
        var events = getPublishedEvents();
        assertThat(events).hasSize(3); // 3 - CmdOK
        verifyEvent(events.get(0), CourseStatusChangedEvent.class, course);
        verifyEvent(events.get(1), CoursePublishedEvent.class, course);
    }

    @Test
    void unpublishCourseTest() {
        var course = createTestCourse();
        var command = CourseCommand.Unpublish.builder().courseId(course.getId()).build();
        assertThatThrownBy(() -> courseCommandService.unpublishCourse(command))
                .asInstanceOf(InstanceOfAssertFactories.type(CommandFailedException.class))
                .matches(ex -> ex.getCause() instanceof TplInvalidTransitionException);
        Mockito.clearInvocations(eventPublisher);
        course.setStatus(CourseStatus.PUBLISHED);
        course = courseCommandService.unpublishCourse(command);
        assertThat(course.getStatus()).isEqualTo(CourseStatus.UNPUBLISHED);
        var events = getPublishedEvents();
        assertThat(events).hasSize(3); // 3 - CmdOK
        verifyEvent(events.get(0), CourseStatusChangedEvent.class, course);
        verifyEvent(events.get(1), CourseUnpublishedEvent.class, course);
    }

    @Test
    void activateTest() {
        var course = createTestCourse();
        var command1 = CourseCommand.Activate.builder()
                .courseId(course.getId())
                .scIds(Set.of(5L, 6L, 7L, 8L, 9L))
                .build();
        var command2 = CourseCommand.Activate.builder()
                .courseId(course.getId())
                .scIds(Set.of(7L, 8L, 9L, 10L, 11L))
                .build();

        // Первая активация, для всех ИД СЦ должны создаться настройки
        assertThat(course.getScConfigs()).isEmpty();
        course = courseCommandService.activate(command1);
        assertThat(course.getScConfigs()).hasSize(5);
        for (var scId : command1.getScIds()) {
            assertThat(course.isAvailableForSCs(Set.of(scId, 999L))).isTrue();
        }
        var events = getPublishedEvents();
        assertThat(events).hasSize(2); // 2 - CmdOK
        var event = verifyEvent(events.get(0), CourseConfigChangedEvent.class, course);
        assertThat(event.getConfigDiffs().stream()
                .filter(diff -> diff.getStatus() != null && diff.getStatus().getNewValue() == CourseConfigStatus.ACTIVE)
                .map(CourseEntityDifference.CourseConfigDifference::getScId)
                .collect(Collectors.toList()))
                .containsAll(command1.getScIds());
        assertThat(event.getConfigDiffs().stream()
                .filter(diff -> diff.getStatus() != null && diff.getStatus().getNewValue() == CourseConfigStatus.INACTIVE)
                .map(CourseEntityDifference.CourseConfigDifference::getScId)
                .collect(Collectors.toList()))
                .isEmpty();

        // Вторая активация, для ИД СЦ 7, 8 и 9 повторной активации быть не должно
        Mockito.clearInvocations(eventPublisher);
        course = courseCommandService.activate(command2);
        assertThat(course.getScConfigs()).hasSize(7);
        for (long scId = 5L; scId <= 11L; scId++) {
            assertThat(course.isAvailableForSCs(Set.of(scId, 999L))).isTrue();
        }
        events = getPublishedEvents();
        assertThat(events).hasSize(2); // 2 - CmdOK
        event = verifyEvent(events.get(0), CourseConfigChangedEvent.class, course);
        assertThat(event.getConfigDiffs().stream()
                .filter(diff -> diff.getStatus() != null && diff.getStatus().getNewValue() == CourseConfigStatus.ACTIVE)
                .map(CourseEntityDifference.CourseConfigDifference::getScId)
                .collect(Collectors.toList()))
                .containsOnly(10L, 11L);
        assertThat(event.getConfigDiffs().stream()
                .filter(diff -> diff.getStatus() != null && diff.getStatus().getNewValue() == CourseConfigStatus.INACTIVE)
                .map(CourseEntityDifference.CourseConfigDifference::getScId)
                .collect(Collectors.toList()))
                .isEmpty();

        assertThat(course.isAvailableForSCs(Set.of(999L))).isFalse();
    }

    @Test
    void deactivateTest() {
        var course = createTestCourse();
        var activateCommand = CourseCommand.Activate.builder()
                .courseId(course.getId())
                .scIds(Set.of(5L, 6L, 7L, 8L, 9L))
                .build();
        var deactivateCommand = CourseCommand.Deactivate.builder()
                .courseId(course.getId())
                .scIds(Set.of(6L, 7L, 8L, 12L, 13L))
                .build();

        // Сначала активируем настройки по СЦ
        course = courseCommandService.activate(activateCommand);
        assertThat(course.getScConfigs()).hasSize(5);
        for (var scId : activateCommand.getScIds()) {
            assertThat(course.isAvailableForSCs(Set.of(scId))).isTrue();
        }
        Mockito.clearInvocations(eventPublisher);

        // Деактивация настроек по СЦ
        course = courseCommandService.deactivate(deactivateCommand);
        assertThat(course.getScConfigs()).hasSize(5); // все еще 5, но часть должна быть в INACTIVE

        // В ивенте должно быть 3 деактивированных СЦ
        var events = getPublishedEvents();
        assertThat(events).hasSize(2); // 2 - CmdOK
        var event = verifyEvent(events.get(0), CourseConfigChangedEvent.class, course);
        assertThat(event.getConfigDiffs().stream()
                .filter(diff -> diff.getStatus() != null && diff.getStatus().getNewValue() == CourseConfigStatus.ACTIVE)
                .map(CourseEntityDifference.CourseConfigDifference::getScId)
                .collect(Collectors.toList()))
                .isEmpty();
        assertThat(event.getConfigDiffs().stream()
                .filter(diff -> diff.getStatus() != null && diff.getStatus().getNewValue() == CourseConfigStatus.INACTIVE)
                .map(CourseEntityDifference.CourseConfigDifference::getScId)
                .collect(Collectors.toList()))
                .containsOnly(6L, 7L, 8L);

        // по всем курс должен быть недоступен
        for (var scId : deactivateCommand.getScIds()) {
            assertThat(course.isAvailableForSCs(Set.of(scId))).isFalse();
        }
        // Не попали под деактивацию
        assertThat(course.isAvailableForSCs(Set.of(5L))).isTrue();
        assertThat(course.isAvailableForSCs(Set.of(9L))).isTrue();
    }

    private Course createTestCourse() {
        var command = createTestCourseCommand(UserStatusRequirement.ALL);
        var course = new Course();
        course.init(command);
        course.setId(12345L);
        doReturn(course).when(entityManager).find(eq(Course.class), eq(course.getId()), any(LockModeType.class));
        doReturn(course).when(entityManager).find(eq(Course.class), eq(course.getId()));
        return course;
    }

    @SuppressWarnings("unchecked")
    private <T extends DomainEvent> T verifyEvent(DomainEvent event, Class<T> expectedClass, Course entity) {
        assertThat(event)
                .isInstanceOf(expectedClass)
                .asInstanceOf(InstanceOfAssertFactories.type(EntityEvent.class))
                .matches(e -> e.getAggregate() == entity);
        return (T) event;
    }

    private CourseCommand.Create createTestCourseCommand(UserStatusRequirement userStatusRequirement) {
        return CourseCommand.Create.builder()
                .name("test-course")
                .programId("test-program")
                .imageUrl("test-img-url")
                .userStatusRequirement(userStatusRequirement)
                .description("test-desc")
                .expectedDurationInMinutes(15)
                .beta(false)
                .build();
    }

    private void checkCourseData(Course course, CourseCommand.CourseBodyCommand command, CourseStatus status, boolean isBeta) {
        assertThat(course).extracting("name", "programId", "imageUrl", "status",
                "userStatusRequirement", "description", "expectedDurationMinutes", "beta")
                .contains(command.getName(), command.getProgramId(), command.getImageUrl(), status,
                        command.getUserStatusRequirement(), command.getDescription(), command.getExpectedDurationInMinutes(), isBeta);
    }
}
