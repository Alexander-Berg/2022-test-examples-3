package ru.yandex.market.tpl.core.domain.course;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.course.CourierCourseDto;
import ru.yandex.market.tpl.api.model.course.CourseDataRequest;
import ru.yandex.market.tpl.api.model.course.CourseStatus;
import ru.yandex.market.tpl.api.model.course.CourseType;
import ru.yandex.market.tpl.api.model.course.UserStatusRequirement;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.course.domain.Course;
import ru.yandex.market.tpl.core.domain.course.domain.CourseAssignment;
import ru.yandex.market.tpl.core.domain.course.repository.CourseAssignmentRepository;
import ru.yandex.market.tpl.core.domain.course.repository.CourseRepository;
import ru.yandex.market.tpl.core.domain.course.service.CourseAssignmentService;
import ru.yandex.market.tpl.core.domain.course.service.CourseService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserCommandService;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.course.CourseAssignmentStatus.COMPLETED;
import static ru.yandex.market.tpl.api.model.course.CourseStatus.NEW;
import static ru.yandex.market.tpl.api.model.course.CourseStatus.PUBLISHED;
import static ru.yandex.market.tpl.api.model.course.CourseStatus.UNPUBLISHED;

@RequiredArgsConstructor
public class CourseServiceTest extends TplAbstractTest {

    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final CourseAssignmentService assignmentService;
    private final CourseAssignmentRepository assignmentRepository;
    private final Clock clock;
    private final ConfigurationServiceAdapter configuration;
    private final TestUserHelper testUserHelper;
    private final UserScheduleService userScheduleService;
    private final UserCommandService userCommandService;
    private final UserShiftCommandService userShiftCommandService;
    private final UserPropertyService userPropertyService;
    private final TransactionTemplate transactionTemplate;
    private final UserRepository userRepository;

    private SortingCenter sortingCenter;
    private User user;

    @BeforeEach
    void setUp() {
        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime());
        user = testUserHelper.createUser(TestUserHelper.UserGenerateParam.builder()
                .workdate(LocalDate.now(clock))
                .build());
        var userSchedule = userScheduleService.findUserScheduleForDate(user, LocalDate.now(clock));
        sortingCenter = userSchedule.getActiveRules().stream().findFirst().orElseThrow().getSortingCenter();
        configuration.mergeValue(ConfigurationProperties.SHIFTS_AMOUNT_FOR_GRADE_UP, 99);
        configuration.mergeValue(ConfigurationProperties.COURSE_ASSIGNMENT_FILTERS_ENABLED, true);
        userCommandService.activate(new UserCommand.Activate(user.getId()));
        assignmentRepository.deleteAll();
        courseRepository.deleteAll();
    }

    @Test
    public void shouldCreateCourse() {
        var course = createTestCourse("Курс 1", "program-uuid-1", UserStatusRequirement.ALL, true,
                CourseType.PRO_APPLICATION);
        assertThat(course).extracting("name", "programId", "imageUrl", "status", "description",
                        "expectedDurationMinutes", "hasExam", "courseType")
                .containsOnly("Курс 1", "program-uuid-1", "https://ya.ru", CourseStatus.NEW, "test-description",
                        15, true, CourseType.PRO_APPLICATION);
    }

    @Test
    public void shouldUpdateCourse() {
        var course = createTestCourse("Курс 2", "program-uuid-2");

        var courseUpdateDto = CourseDataRequest.builder()
                .name("Курс 3")
                .programId("program-uuid-3")
                .imageUrl("https://yandex.ru")
                .userStatusRequirement(UserStatusRequirement.NEWBIE_ONLY)
                .description("test2")
                .expectedDurationMinutes(10)
                .hasExam(false)
                .courseType(CourseType.PRO_APPLICATION)
                .build();

        var updatedCourse = courseService.updateCourse(course.getId(), courseUpdateDto);
        assertThat(updatedCourse).extracting("id", "name", "programId", "imageUrl", "status",
                        "userStatusRequirement", "description", "expectedDurationMinutes", "hasExam", "courseType")
                .containsOnly(course.getId(), "Курс 3", "program-uuid-3", "https://yandex.ru", NEW, // Статус не
                        // меняется
                        UserStatusRequirement.NEWBIE_ONLY, "test2", 10, false, CourseType.PRO_APPLICATION);

        courseUpdateDto = CourseDataRequest.builder()
                .name("Курс 3")
                .programId("program-uuid-3")
                .imageUrl("https://yandex.ru")
                .userStatusRequirement(UserStatusRequirement.NEWBIE_ONLY)
                .description("test2")
                .expectedDurationMinutes(10)
                .hasExam(false)
                .build();

        updatedCourse = courseService.updateCourse(course.getId(), courseUpdateDto);
        assertThat(updatedCourse).extracting("id", "name", "programId", "imageUrl", "status",
                        "userStatusRequirement", "description", "expectedDurationMinutes", "hasExam", "courseType")
                .containsOnly(course.getId(), "Курс 3", "program-uuid-3", "https://yandex.ru", NEW, // Статус не
                        // меняется
                        UserStatusRequirement.NEWBIE_ONLY, "test2", 10, false, CourseType.OLD_APPLICATION);
    }

    @Test
    public void shouldReturnCorrectUrl() {
        String courseUrl = courseService.getCourseUrl(123L, "program-uuid-1", 123L);
        assertThat(courseUrl).isEqualTo("https://education.training.yandex/?programUuid=program-uuid-1&externalId=123" +
                "&assignmentId=123");
    }

    @Test
    public void shouldReturnCorrectUrlWithNull() {
        String courseUrl = courseService.getCourseUrl(123L, "program-uuid-1", null);
        assertThat(courseUrl).isEqualTo("https://education.training.yandex/?programUuid=program-uuid-1&externalId=123" +
                "&assignmentId=");
    }

    @Test
    public void shouldReturnCoursesWithRightType() {
        User user = testUserHelper.createUser(TestUserHelper.UserGenerateParam.builder()
                .workdate(LocalDate.now(clock))
                .userId(4598760394673L)
                .build());
        var course1 = createTestCourse("Курс 11", "program-uuid-11", UserStatusRequirement.ALL,
                true, CourseType.PRO_APPLICATION);
        var course2 = createTestCourse("Курс 22", "program-uuid-22", UserStatusRequirement.ALL,
                true, CourseType.PRO_APPLICATION);
        var course3 = createTestCourse("Курс 33", "program-uuid-33", UserStatusRequirement.ALL,
                true, CourseType.OLD_APPLICATION);
        var course4 = createTestCourse("Курс 44", "program-uuid-44", UserStatusRequirement.ALL,
                true, CourseType.OLD_APPLICATION);
        courseService.publishCourse(course1.getId());
        courseService.publishCourse(course2.getId());
        courseService.publishCourse(course3.getId());
        courseService.publishCourse(course4.getId());

        transactionTemplate.execute(status -> {
            User user2 = userRepository.findByUid(user.getUid()).get();
            userPropertyService.addPropertyToUser(user2, UserProperties.COURIER_WITH_PRO_APP, true);
            return status;
        });
        List<String> courierCourses =
                courseService.getCourierCourses(user.getId()).stream()
                        .map(CourierCourseDto::getName)
                        .collect(Collectors.toList());
        assertThat(courierCourses).containsExactlyInAnyOrder(course1.getName(), course2.getName());
    }

    @Test
    public void shouldReturnCoursesWithRightType2() {
        var course1 = createTestCourse("Курс 111", "program-uuid-111", UserStatusRequirement.ALL,
                true, CourseType.PRO_APPLICATION);
        var course2 = createTestCourse("Курс 222", "program-uuid-222", UserStatusRequirement.ALL,
                true, CourseType.PRO_APPLICATION);
        var course3 = createTestCourse("Курс 333", "program-uuid-333", UserStatusRequirement.ALL,
                true, CourseType.OLD_APPLICATION);
        var course4 = createTestCourse("Курс 444", "program-uuid-444", UserStatusRequirement.ALL,
                true, CourseType.OLD_APPLICATION);
        courseService.publishCourse(course1.getId());
        courseService.publishCourse(course2.getId());
        courseService.publishCourse(course3.getId());
        courseService.publishCourse(course4.getId());


        transactionTemplate.execute(status -> {
            User user = userRepository.getById(this.user.getId());
            userPropertyService.addPropertyToUser(user, UserProperties.COURIER_WITH_PRO_APP, false);
            return status;
        });

        var courierCourses =
                courseService.getCourierCourses(user.getId()).stream()
                        .map(CourierCourseDto::getName)
                        .collect(Collectors.toList());
        assertThat(courierCourses).containsExactlyInAnyOrder(course3.getName(), course4.getName());
    }

    @Test
    public void shouldReturnNotCompletedCourierCourses() {
        var course = createTestCourse("Курс 4", "program-uuid-4");
        var courseB = createTestCourse("Курс 4Б", "program-uuid-4B");
        courseService.publishCourse(course.getId());

        CourseDataRequest courseRequest = CourseDataRequest.builder()
                .name(courseB.getName())
                .programId(courseB.getProgramId())
                .imageUrl(courseB.getImageUrl())
                .userStatusRequirement(courseB.getUserStatusRequirement())
                .beta(true)
                .build();
        courseService.updateCourse(courseB.getId(), courseRequest);

        var assignment = createTestCourseAssignment(course.getId(), user.getId());
        assignmentService.completeCourseAssignment(user.getId(), "session-uuid", "program-uuid-4B", null,
                Instant.now(clock), true);


        List<CourierCourseDto> courierCourses = courseService.getCourierCourses(user.getId());
        assertThat(courierCourses).hasSize(1)
                .extracting("name", "courseUrl", "imageUrl", "status", "description",
                        "expectedDurationMinutes", "userId", "courseAssignment")
                .containsOnly(Tuple.tuple("Курс 4",
                        "https://education.training.yandex/?programUuid=program-uuid-4&externalId=" + user.getId() +
                                "&assignmentId=" + assignment.getId(),
                        "https://ya.ru", PUBLISHED, "test-description", 15, user.getId(), null));
    }

    @Test
    public void shouldReturnNotStartedCourierCourses() {
        var course = createTestCourse("Курс 5", "program-uuid-5");
        courseService.publishCourse(course.getId());

        List<CourierCourseDto> courierCourses = courseService.getCourierCourses(user.getId());
        assertThat(courierCourses).hasSize(1)
                .extracting("name", "courseUrl", "imageUrl", "status", "description",
                        "expectedDurationMinutes", "userId", "courseAssignment")
                .containsOnly(Tuple.tuple("Курс 5",
                        "https://education.training.yandex/?programUuid=program-uuid-5&externalId=" + user.getId() +
                                "&assignmentId=",
                        "https://ya.ru", PUBLISHED, "test-description", 15, user.getId(), null));
    }

    @Test
    public void shouldReturnCompletedCourierCourses() {
        var course = createTestCourse("Курс 6", "program-uuid-6");
        courseService.publishCourse(course.getId());

        var assignment = createTestCourseAssignment(course.getId(), user.getId());
        assignmentService.completeCourseAssignment(user.getId(), "session-uuid", "program-uuid-6", assignment.getId()
                , Instant.now(clock), true);

        List<CourierCourseDto> courierCourses = courseService.getCourierCourses(user.getId());
        assertThat(courierCourses).hasSize(1)
                .extracting("name", "courseUrl", "imageUrl", "status", "description",
                        "expectedDurationMinutes", "userId",
                        "courseAssignment.sessionId", "courseAssignment.status", "courseAssignment.completedAt",
                        "courseAssignment.passed")
                .containsOnly(Tuple.tuple("Курс 6", "https://education.training" +
                                ".yandex/?programUuid=program-uuid-6&externalId=" + user.getId() + "&assignmentId=",
                        "https://ya.ru", PUBLISHED, "test-description", 15, user.getId(),
                        "session-uuid", COMPLETED, Instant.now(clock), true));
    }

    @Test
    public void shouldReturnCompletedCourierCoursesPhase1() {
        var course = createTestCourse("Курс 7", "program-uuid-7");
        var course2 = createTestCourse("Курс 8", "program-uuid-8");
        courseService.publishCourse(course.getId());
        courseService.publishCourse(course2.getId());

        var assignment = createTestCourseAssignment(course.getId(), user.getId());
        var assignment2 = createTestCourseAssignment(course2.getId(), user.getId());
        assignmentService.completeCourseAssignment(user.getId(), "session-uuid", "program-uuid-7", assignment.getId()
                , Instant.now(clock), true);
        assignmentService.completeCourseAssignment(user.getId(), "session-uuid", "program-uuid-8",
                assignment2.getId(), Instant.now(clock), true);

        courseService.unpublish(course.getId());

        List<CourierCourseDto> courierCourses = courseService.getCourierCourses(user.getId());
        assertThat(courierCourses).hasSize(2)
                .extracting("name", "courseUrl", "imageUrl", "status", "description",
                        "expectedDurationMinutes", "userId", "isCourseAvailable",
                        "courseAssignment.sessionId", "courseAssignment.status", "courseAssignment.completedAt",
                        "courseAssignment.passed")
                .containsOnly(Tuple.tuple("Курс 7", "https://education.training" +
                                        ".yandex/?programUuid=program-uuid-7&externalId=" + user.getId() +
                                        "&assignmentId=",
                                "https://ya.ru", UNPUBLISHED, "test-description", 15, user.getId(), false,
                                "session-uuid", COMPLETED, Instant.now(clock), true),
                        Tuple.tuple("Курс 8", "https://education.training" +
                                        ".yandex/?programUuid=program-uuid-8&externalId=" + user.getId() +
                                        "&assignmentId=",
                                "https://ya.ru", PUBLISHED, "test-description", 15, user.getId(), true,
                                "session-uuid", COMPLETED, Instant.now(clock), true));
    }

    @Test
    public void shouldSortCourses() {
        Course course1 = createTestCourse("Курс 1", "uuid1");
        courseService.publishCourse(course1.getId());
        createTestCourseAssignment(course1.getId(), user.getId());
        assignmentService.completeCourseAssignment(user.getId(), "session-uuid", "uuid1", null, Instant.now(clock),
                true);

        Course course2 = createTestCourse("Курс 2", "uuid2");
        courseService.publishCourse(course2.getId());

        ClockUtil.initFixed(clock, ClockUtil.defaultDateTime());
        List<CourierCourseDto> courierCourses = courseService.getCourierCourses(user.getId());

        assertThat(courierCourses).hasSize(2)
                .extracting("name", "isNewCourse")
                .contains(Tuple.tuple("Курс 1", false), Tuple.tuple("Курс 2", true));
    }

    @Test
    void shouldFilterCoursesByScConfig() {
        var course1 = createTestCourse("course 1", "program-uuid-1");
        var course2 = createTestCourse("course 2", "program-uuid-2");
        courseService.publishCourse(course1.getId());
        courseService.publishCourse(course2.getId());

        var courses = courseService.getCourierCourses(user.getId());
        assertThat(courses).hasSize(2);

        // Деактивируем курс 2 - он не должен попасть в выборку
        courseService.deactivateCourseForSc(course2.getId(), Set.of(sortingCenter.getId()));
        courses = courseService.getCourierCourses(user.getId());
        assertThat(courses).hasSize(1)
                .extracting("name", "courseUrl", "imageUrl", "status", "description",
                        "expectedDurationMinutes", "userId", "courseAssignment")
                .containsOnly(Tuple.tuple("course 1",
                        "https://education.training.yandex/?programUuid=program-uuid-1&externalId=" + user.getId() +
                                "&assignmentId=",
                        "https://ya.ru", PUBLISHED, "test-description", 15, user.getId(), null));
    }

    @Test
    void shouldFilterCoursesByUserStatus() {
        var course1 = createTestCourse("course 1", "program-uuid-1", UserStatusRequirement.NEWBIE_ONLY);
        var course2 = createTestCourse("course 2", "program-uuid-2", UserStatusRequirement.ALL);

        courseService.publishCourse(course1.getId());
        courseService.publishCourse(course2.getId());

        var courses = courseService.getCourierCourses(user.getId());
        assertThat(courses).hasSize(2);

        // Убираем статус новичка - второй курс не должен попадать в выборку
        configuration.mergeValue(ConfigurationProperties.SHIFTS_AMOUNT_FOR_GRADE_UP, 0);
        userCommandService.activate(new UserCommand.Activate(user.getId()));

        courses = courseService.getCourierCourses(user.getId());
        assertThat(courses).hasSize(1)
                .extracting("name", "courseUrl", "imageUrl", "status", "description",
                        "expectedDurationMinutes", "userId", "courseAssignment")
                .containsOnly(Tuple.tuple("course 2",
                        "https://education.training.yandex/?programUuid=program-uuid-2&externalId=" + user.getId() +
                                "&assignmentId=",
                        "https://ya.ru", PUBLISHED, "test-description", 15, user.getId(), null));
    }

    @Test
    void shouldNotFilterCoursesIfFiltersDisabled() {
        var course1 = createTestCourse("course 1", "program-uuid-1", UserStatusRequirement.NEWBIE_ONLY);
        var course2 = createTestCourse("course 2", "program-uuid-2", UserStatusRequirement.ALL);
        courseService.publishCourse(course1.getId());
        courseService.publishCourse(course2.getId());

        // Первый курс не подходит - юзер не новичек
        configuration.mergeValue(ConfigurationProperties.SHIFTS_AMOUNT_FOR_GRADE_UP, 0);
        userCommandService.activate(new UserCommand.Activate(user.getId()));
        // Второй курс не подходит - не включен на СЦ
        courseService.deactivateCourseForSc(course2.getId(), Set.of(sortingCenter.getId()));

        // Фильтры выключены, оба курса должны быть выбраны
        configuration.mergeValue(ConfigurationProperties.COURSE_ASSIGNMENT_FILTERS_ENABLED, false);
        var courses = courseService.getCourierCourses(user.getId());
        assertThat(courses).hasSize(2);
    }

    @Test
    void shouldFilterCoursesForNewCourierWithoutSchedule() {
        var newUser = testUserHelper.findOrCreateUserWithoutSchedule(2L);

        var scFilteredCourse = createTestCourse("course1", "program-uuid-1", UserStatusRequirement.ALL);
        var allAvailableCourse = createTestCourse("course2", "program-uuid-2", UserStatusRequirement.ALL, false);
        courseService.publishCourse(scFilteredCourse.getId());
        courseService.publishCourse(allAvailableCourse.getId());

        var courses = courseService.getCourierCourses(newUser.getId());
        assertThat(courses).hasSize(1).extracting("name").containsOnly("course2");
    }

    @Test
    void shouldFilterCoursesForNotActiveCourier() {
        var scFilteredCourse = createTestCourse("course1", "program-uuid-1", UserStatusRequirement.ALL);
        var allAvailableCourse = createTestCourse("course2", "program-uuid-2", UserStatusRequirement.ALL, false);
        var newOnlyCourse = createTestCourse("course3", "program-uuid-3", UserStatusRequirement.NEWBIE_ONLY);
        courseService.publishCourse(scFilteredCourse.getId());
        courseService.publishCourse(allAvailableCourse.getId());
        courseService.publishCourse(newOnlyCourse.getId());

        // юзер в статусе NOT_ACTIVE и были смены
        createUserShiftForUser(user);
        userCommandService.recover(new UserCommand.Recover(user.getId()));

        var courses = courseService.getCourierCourses(user.getId());
        assertThat(courses).isEmpty();
    }

    @Test
    void shouldFilterCoursesForNotActiveNewbieCourier() {
        var newUser = testUserHelper.findOrCreateUserWithoutSchedule(2L);

        var scNewOnlyCourse = createTestCourse("course1", "program-uuid-1", UserStatusRequirement.NEWBIE_ONLY);
        var newOnlyCourse = createTestCourse("course2", "program-uuid-2", UserStatusRequirement.NEWBIE_ONLY, false);
        courseService.publishCourse(scNewOnlyCourse.getId());
        courseService.publishCourse(newOnlyCourse.getId());

        var newUserCourses = courseService.getCourierCourses(newUser.getId());
        assertThat(newUserCourses).hasSize(1)
                .extracting("name")
                .contains("course2");

        var newbieUserCourses = courseService.getCourierCourses(user.getId());
        assertThat(newbieUserCourses).hasSize(2)
                .extracting("name")
                .contains("course1", "course2");
    }

    @Test
    void shouldPublishCourse() {
        var course = createTestCourse("course 1", "program-uuid-1");
        courseService.publishCourse(course.getId());
        assertThat(courseService.getCourseById(course.getId()).getStatus()).isEqualTo(PUBLISHED);
        courseService.unpublish(course.getId());
        assertThat(courseService.getCourseById(course.getId()).getStatus()).isEqualTo(UNPUBLISHED);
        courseService.publishCourse(course.getId());
        assertThat(courseService.getCourseById(course.getId()).getStatus()).isEqualTo(PUBLISHED);
    }

    private Course createTestCourse(String name, String programId) {
        return createTestCourse(name, programId, UserStatusRequirement.ALL);
    }

    private Course createTestCourse(String name, String programId, UserStatusRequirement userStatusRequirement) {
        return createTestCourse(name, programId, userStatusRequirement, true);
    }

    private Course createTestCourse(String name, String programId, UserStatusRequirement userStatusRequirement,
                                    boolean scFiltersEnabled) {
        return createTestCourse(name, programId, userStatusRequirement, scFiltersEnabled, CourseType.OLD_APPLICATION);
    }

    private Course createTestCourse(String name, String programId, UserStatusRequirement userStatusRequirement,
                                    boolean scFiltersEnabled, CourseType courseType) {
        var courseDto = courseService.createCourse(CourseDataRequest.builder()
                .name(name)
                .programId(programId)
                .imageUrl("https://ya.ru")
                .description("test-description")
                .expectedDurationMinutes(15)
                .userStatusRequirement(userStatusRequirement)
                .scFiltersEnabled(scFiltersEnabled)
                .courseType(courseType)
                .hasExam(true)
                .build());
        var courseOpt = courseRepository.findById(courseDto.getId());
        assertThat(courseOpt).isNotEmpty();
        var course = courseOpt.get();
        if (scFiltersEnabled) {
            courseService.activateCourseForSc(course.getId(), Set.of(sortingCenter.getId()));
        }
        return course;
    }

    private CourseAssignment createTestCourseAssignment(Long courseId, Long userId) {
        var assignmentDto = assignmentService.createCourseAssignment(userId, courseId);
        var assignmentOpt = assignmentRepository.findById(assignmentDto.getId());
        assertThat(assignmentOpt).isNotEmpty();
        return assignmentOpt.get();
    }

    private void createUserShiftForUser(User user) {
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        userShiftCommandService.createUserShift(UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .build());
    }
}
