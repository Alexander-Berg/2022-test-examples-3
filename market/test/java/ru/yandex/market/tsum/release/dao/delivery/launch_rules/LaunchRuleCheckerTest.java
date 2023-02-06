package ru.yandex.market.tsum.release.dao.delivery.launch_rules;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import ru.yandex.market.tsum.clients.calendar.CalendarClient;
import ru.yandex.market.tsum.clients.calendar.Holidays;
import ru.yandex.market.tsum.clients.staff.StaffApiClient;
import ru.yandex.market.tsum.clients.staff.StaffPerson;
import ru.yandex.market.tsum.clients.utils.ZoneUtils;
import ru.yandex.market.tsum.dao.ProjectsDao;
import ru.yandex.market.tsum.entity.project.DeliveryMachineEntity;
import ru.yandex.market.tsum.entity.project.ProjectEntity;
import ru.yandex.market.tsum.release.dao.DeliveryMachineSettings;
import ru.yandex.market.tsum.release.dao.Release;
import ru.yandex.market.tsum.release.dao.ReleaseDao;
import ru.yandex.market.tsum.release.dao.title_providers.OrdinalTitleProvider;
import ru.yandex.market.tsum.release.delivery.VcsChange;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 18.02.2019
 */
public class LaunchRuleCheckerTest {
    @Mock
    private StaffApiClient staffApiClient;

    @Mock
    private CalendarClient calendarClient;

    @Mock
    private ReleaseDao releaseDao;

    @Mock
    private ProjectsDao projectsDao;

    @Mock
    private MongoConverter mongoConverter;

    @InjectMocks
    private LaunchRuleChecker launchRuleChecker;

    private static final String VALID_PERSON_NAME = "validName";
    private static final String ANOTHER_VALID_PERSON_NAME = "anotherValidName";
    private static final String INVALID_PERSON_NAME = "invalidName";

    private static final String VALID_DEPARTMENT = "validDepartment";

    private static final String VALID_PROJECT_ID = "project";
    private static final String INVALID_PROJECT_ID = "invalidProject";
    private static final String VALID_PIPELINE_ID = "validPipelineId";
    private static final String INVALID_PIPELINE_ID = "invalidPipelineId";
    private static final DeliveryMachineEntity DELIVERY_MACHINE_ENTITY = new DeliveryMachineEntity(
        "title", VALID_PIPELINE_ID, null
    );
    private static final DeliveryMachineEntity DELIVERY_MACHINE_ENTITY_WITHOUT_RELEASES = new DeliveryMachineEntity(
        "title_without_releases", VALID_PIPELINE_ID, null
    );

    private static final String VALID_QUEUE = "TESTQUEUE";
    private static final int VALID_NUMBER_OF_COMMITS = 3;
    private static final int VALID_HOURS_PASSED = 3;

    private static final DepartmentRule DEPARTMENT_RULE = new DepartmentRule(VALID_DEPARTMENT);
    private static final DirectoryRule DIRECTORY_RULE =
        new DirectoryRule("\\/a\\/b\\/c(\\/.*)*", PathPatternType.REGEX);
    private static final DirectoryRule GLOB_DIRECTORY_RULE = new DirectoryRule("/a/b/c/d/e/**", PathPatternType.GLOB);
    private static final HasTicketRule HAS_TICKET_RULE = new HasTicketRule(VALID_QUEUE);
    private static final HaveNumberOfCommitsRule HAVE_NUMBER_OF_COMMITS_RULE =
        new HaveNumberOfCommitsRule(VALID_NUMBER_OF_COMMITS);
    private static final HoursPassedRule HOURS_PASSED_RULE = new HoursPassedRule(VALID_HOURS_PASSED);
    private static final NotHolidayRule NOT_HOLIDAY_RULE = new NotHolidayRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(staffApiClient.getPerson(VALID_PERSON_NAME)).thenReturn(
            Optional.of(new StaffPerson(
                VALID_PERSON_NAME, -1, null, null, null, new
                StaffPerson.DepartmentGroup(VALID_DEPARTMENT, "Valid Department")
            ))
        );
        when(staffApiClient.getPerson(ANOTHER_VALID_PERSON_NAME)).thenReturn(
            Optional.of(new StaffPerson(
                ANOTHER_VALID_PERSON_NAME, -1, null, null, null, new
                StaffPerson.DepartmentGroup(
                "Some_Department", "Some Department", Collections.singletonList(
                new StaffPerson.DepartmentGroup(VALID_DEPARTMENT, "Valid Department")
            )
            )
            ))
        );
        when(staffApiClient.getPerson(INVALID_PERSON_NAME)).thenReturn(
            Optional.of(new StaffPerson(
                INVALID_PERSON_NAME, -1, null, null, null,
                new StaffPerson.DepartmentGroup("invalidDepartment", "Invalid Department")
            ))
        );

        when(releaseDao.getLastReleaseWithChanges(VALID_PROJECT_ID, DELIVERY_MACHINE_ENTITY.getPipelineId()))
            .thenReturn(
                Release.builder()
                    .withProjectId(VALID_PROJECT_ID)
                    .withPipeId(VALID_PIPELINE_ID)
                    .withPipeLaunchId("pipeLaunchId")
                    .withCreatedDate(
                        new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(VALID_HOURS_PASSED + 1))
                    ).build()
            );

        when(releaseDao.getLastReleaseWithChanges(INVALID_PROJECT_ID, DELIVERY_MACHINE_ENTITY.getPipelineId()))
            .thenReturn(
                Release.builder()
                    .withPipeId(INVALID_PIPELINE_ID)
                    .withProjectId(INVALID_PROJECT_ID)
                    .withPipeLaunchId("pipeLaunchId")
                    .withCreatedDate(
                        new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(VALID_HOURS_PASSED - 1))
                    )
                    .build()
        );

        when(
            releaseDao.getLastReleaseWithChanges(VALID_PROJECT_ID,
            DELIVERY_MACHINE_ENTITY_WITHOUT_RELEASES.getPipelineId())
        )
            .thenReturn(null);

        ProjectEntity project = new ProjectEntity();
        project.setId(VALID_PROJECT_ID);
        project.setTitle("test title");

        when(projectsDao.get(VALID_PROJECT_ID)).thenReturn(project);
    }

    @Test
    public void testDepartmentRuleTrue() {
        VcsChange vcsChange = new VcsChange(null, null, null, VALID_PERSON_NAME);
        List<VcsChange> vcsChanges = Collections.singletonList(vcsChange);

        Assert.assertTrue(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(DEPARTMENT_RULE), vcsChange, vcsChanges, null, null
        ));

        vcsChange = new VcsChange(null, null, null, ANOTHER_VALID_PERSON_NAME);
        vcsChanges = Collections.singletonList(vcsChange);

        Assert.assertTrue(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(DEPARTMENT_RULE), vcsChange, vcsChanges, null, null
        ));
    }

    @Test
    public void testDepartmentRuleFalse() {
        VcsChange vcsChange = new VcsChange(null, null, null, INVALID_PERSON_NAME);
        List<VcsChange> vcsChanges = Collections.singletonList(vcsChange);

        Assert.assertFalse(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(DEPARTMENT_RULE), vcsChange, vcsChanges, null, null
        ));
    }

    @Test
    public void testDirectoryRuleTrue() {
        VcsChange vcsChange = new VcsChange(null, null, Collections.singleton("/a/b/c/asdf"), null, null);
        List<VcsChange> vcsChanges = Collections.singletonList(vcsChange);

        Assert.assertTrue(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(DIRECTORY_RULE), vcsChange, vcsChanges, null, null
        ));
    }

    @Test
    public void testDirectoryRuleFalse() {
        VcsChange vcsChange = new VcsChange(null, null, Collections.singleton("/a/b/d"), null, null);
        List<VcsChange> vcsChanges = Collections.singletonList(vcsChange);

        Assert.assertFalse(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(DIRECTORY_RULE), vcsChange, vcsChanges, null, null
        ));
    }

    @Test
    public void testGlobDirectoryRuleTrue() {
        VcsChange vcsChange = new VcsChange(null, null, Collections.singleton("/a/b/c/d/e/asdf"), null, null);
        List<VcsChange> vcsChanges = Collections.singletonList(vcsChange);

        Assert.assertTrue(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(GLOB_DIRECTORY_RULE), vcsChange, vcsChanges, null, null
        ));
    }

    @Test
    public void testGlobDirectoryRuleFalse() {
        VcsChange vcsChange = new VcsChange(null, null, Collections.singleton("/a/b/c/d/e"), null, null);
        List<VcsChange> vcsChanges = Collections.singletonList(vcsChange);

        Assert.assertFalse(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(GLOB_DIRECTORY_RULE), vcsChange, vcsChanges, null, null
        ));
    }

    @Test
    public void testHasTicketRuleTrue() {
        VcsChange vcsChange = new VcsChange(null, null, VALID_QUEUE + "-123 change", null);
        List<VcsChange> vcsChanges = Collections.singletonList(vcsChange);

        Assert.assertTrue(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(HAS_TICKET_RULE), vcsChange, vcsChanges, null, null
        ));
    }

    @Test
    public void testHasTicketRuleFalse() {
        VcsChange vcsChange = new VcsChange(null, null, "INVALIDQUEUE-123 change", null);
        List<VcsChange> vcsChanges = Collections.singletonList(vcsChange);

        Assert.assertFalse(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(HAS_TICKET_RULE), vcsChange, vcsChanges, null, null
        ));
    }

    @Test
    public void testHaveNumberOfCommitsTrue() {
        VcsChange vcsChange = new VcsChange(null, Instant.ofEpochMilli(3), null, null);
        List<VcsChange> vcsChanges = Arrays.asList(
            new VcsChange(null, Instant.ofEpochMilli(1), null, null),
            new VcsChange(null, Instant.ofEpochMilli(2), null, null),
            vcsChange
        );

        Assert.assertTrue(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(HAVE_NUMBER_OF_COMMITS_RULE), vcsChange, vcsChanges, null, null
        ));
    }

    @Test
    public void testHaveNumberOfCommitsFalse() {
        List<VcsChange> vcsChanges = Arrays.asList(
            new VcsChange(null, Instant.ofEpochMilli(1), null, null),
            new VcsChange(null, Instant.ofEpochMilli(3), null, null)
        );

        Assert.assertFalse(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(HAVE_NUMBER_OF_COMMITS_RULE), null, vcsChanges, null, null
        ));
    }

    @Test
    public void testHoursPassedTrue() {
        Assert.assertTrue(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(HOURS_PASSED_RULE), null, null, VALID_PROJECT_ID,
            DELIVERY_MACHINE_ENTITY.getPipelineId()
        ));
    }

    @Test
    public void testHoursPassedFalse() {
        Assert.assertFalse(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(HOURS_PASSED_RULE), null, null, INVALID_PROJECT_ID,
            DELIVERY_MACHINE_ENTITY.getPipelineId()
        ));
    }

    @Test
    public void testHoursPassedTrueWhenReleaseNull() {
        Assert.assertTrue(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(HOURS_PASSED_RULE), null, null, VALID_PROJECT_ID,
            DELIVERY_MACHINE_ENTITY_WITHOUT_RELEASES.getPipelineId()
        ));
    }

    @Test
    public void testNotHolidayTrue() {
        when(calendarClient.getHolidays(any(), any())).thenReturn(new Holidays());
        Assert.assertTrue(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(NOT_HOLIDAY_RULE), null, null, INVALID_PROJECT_ID,
            DELIVERY_MACHINE_ENTITY.getPipelineId()
        ));
    }

    @Test
    public void testNotHolidayFalse() {
        Holidays holidays = new Holidays();
        holidays.setHolidays(Collections.singletonList(
            new Holidays.Holiday(LocalDate.of(2018, 12, 30), "weekend")
        ));

        when(calendarClient.getHolidays(any(), any())).thenReturn(holidays);
        Assert.assertFalse(launchRuleChecker.checkAllIsTrue(
            Collections.singletonList(NOT_HOLIDAY_RULE), null, null, INVALID_PROJECT_ID,
            DELIVERY_MACHINE_ENTITY.getPipelineId()
        ));

    }

    @Test
    public void testLaunchTimeRule() {
        ZonedDateTime zonedDateTime = Instant.now().atZone(ZoneUtils.MOSCOW_ZONE_ID);
        if (!zonedDateTime.plusMinutes(10).isBefore(zonedDateTime)) {

            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            when(calendarClient.getHolidays(any(), any())).thenReturn(new Holidays());

            List<DayOfWeek> dayOfWeek = new ArrayList<>();
            dayOfWeek.add(zonedDateTime.getDayOfWeek());

            LaunchTimeRule launchTimeRule1 = new LaunchTimeRule(dayOfWeek,
                zonedDateTime.plusMinutes(4).format(timeFormatter),
                zonedDateTime.plusMinutes(9).format(timeFormatter));
            Assert.assertFalse(launchRuleChecker.checkAllIsTrue(
                Collections.singletonList(launchTimeRule1), null, null, INVALID_PROJECT_ID,
                DELIVERY_MACHINE_ENTITY.getPipelineId()
            ));

            LaunchTimeRule launchTimeRule2 = new LaunchTimeRule(dayOfWeek,
                zonedDateTime.minusMinutes(2).format(timeFormatter),
                zonedDateTime.plusMinutes(3).format(timeFormatter));
            Assert.assertTrue(launchRuleChecker.checkAllIsTrue(
                Collections.singletonList(launchTimeRule2), null, null, INVALID_PROJECT_ID,
                DELIVERY_MACHINE_ENTITY.getPipelineId()
            ));

            List<DayOfWeek> anotherDayOfWeek = new ArrayList<>();
            dayOfWeek.add(zonedDateTime.getDayOfWeek().plus(1));

            LaunchTimeRule launchTimeRule3 = new LaunchTimeRule(anotherDayOfWeek,
                zonedDateTime.minusMinutes(2).format(timeFormatter),
                zonedDateTime.plusMinutes(3).format(timeFormatter));
            Assert.assertFalse(launchRuleChecker.checkAllIsTrue(
                Collections.singletonList(launchTimeRule3), null, null, INVALID_PROJECT_ID,
                DELIVERY_MACHINE_ENTITY.getPipelineId()
            ));
        }

    }

    @Test
    public void testPostRulesTrue() {
        List<VcsChange> expectedChanges = Arrays.asList(
            new VcsChange("1", Instant.ofEpochMilli(1), VALID_QUEUE + "-123 message", VALID_PERSON_NAME),
            new VcsChange("2", Instant.ofEpochMilli(2), VALID_QUEUE + "-123 message", VALID_PERSON_NAME),
            new VcsChange("4", Instant.ofEpochMilli(4), VALID_QUEUE + "-123 message", VALID_PERSON_NAME)
        );

        List<VcsChange> vcsChanges = new ArrayList<>(expectedChanges);
        vcsChanges.add(new VcsChange("3", Instant.ofEpochMilli(3), "SOMEQUEUE-123 message", VALID_PERSON_NAME));

        List<? extends VcsChange> filteredChanges = launchRuleChecker.filterChanges(
            vcsChanges,
            new DeliveryMachineEntity(
                DeliveryMachineSettings.builder()
                    .withArcadiaSettings()
                    .withStageGroupId("stage-group-id")
                    .withPipeline(VALID_PIPELINE_ID, "test-pipe", mock(OrdinalTitleProvider.class))
                    .build(), mongoConverter,
                Collections.singletonList(
                    new RuleGroup(Arrays.asList(
                        HAS_TICKET_RULE,
                        DEPARTMENT_RULE,
                        HAVE_NUMBER_OF_COMMITS_RULE
                    ), mongoConverter)
                )
            ), VALID_PROJECT_ID, false
        );

        Assert.assertEquals(expectedChanges, filteredChanges);
    }

    @Test
    public void testPostRulesFalse() {
        List<VcsChange> vcsChanges = Arrays.asList(
            new VcsChange("1", null, VALID_QUEUE + "-123 message", VALID_PERSON_NAME),
            new VcsChange("2", null, VALID_QUEUE + "-123 message", INVALID_PERSON_NAME),
            new VcsChange("4", null, VALID_QUEUE + "-123 message", VALID_PERSON_NAME),
            new VcsChange("3", null, "SOMEQUEUE-123 message", VALID_PERSON_NAME)
        );

        List<? extends VcsChange> filteredChanges = launchRuleChecker.filterChanges(
            vcsChanges,
            new DeliveryMachineEntity(
                DeliveryMachineSettings.builder()
                    .withArcadiaSettings()
                    .withStageGroupId("stage-group-id")
                    .withPipeline(VALID_PIPELINE_ID, "test-pipe", mock(OrdinalTitleProvider.class))
                    .build(), mongoConverter,
                Collections.singletonList(
                    new RuleGroup(Arrays.asList(
                        HAS_TICKET_RULE,
                        DEPARTMENT_RULE,
                        HAVE_NUMBER_OF_COMMITS_RULE
                    ), mongoConverter)
                )
            ), VALID_PROJECT_ID, false
        );

        Assert.assertTrue(filteredChanges.isEmpty());
    }

    @Test
    public void checkTwoRuleGroupsOneWithoutPostRule() {
        List<VcsChange> vcsChanges = Collections.singletonList(
            new VcsChange("1", Instant.ofEpochMilli(1),
                Collections.singletonList("/a/b/c/asdf"),
                "message", VALID_PERSON_NAME)
        );

        List<? extends VcsChange> filteredChanges = launchRuleChecker.filterChanges(
            vcsChanges,
            new DeliveryMachineEntity(
                DeliveryMachineSettings.builder()
                    .withArcadiaSettings()
                    .withStageGroupId("stage-group-id")
                    .withPipeline(VALID_PIPELINE_ID, "test-pipe", mock(OrdinalTitleProvider.class))
                    .build(), mongoConverter,
                Arrays.asList(
                    new RuleGroup(Arrays.asList(
                        DIRECTORY_RULE,
                        HAVE_NUMBER_OF_COMMITS_RULE
                    ), mongoConverter),
                    new RuleGroup(
                        List.of(new DirectoryRule("/some/irrelevant/path/**", PathPatternType.GLOB)),
                        mongoConverter)
                )
            ), VALID_PROJECT_ID, false
        );

        Assert.assertTrue(filteredChanges.isEmpty());
    }
}
