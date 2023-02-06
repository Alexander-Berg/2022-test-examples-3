package ru.yandex.market.hrms.tms.manager.ispring;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.domain.repo.DomainRepo;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeRepo;
import ru.yandex.market.hrms.core.domain.ispring.repo.EmployeeIspringRepo;
import ru.yandex.market.hrms.core.domain.ispring.repo.IspringAccountRepository;
import ru.yandex.market.hrms.core.domain.ispring.repo.IspringGroupInfoRepo;
import ru.yandex.market.hrms.core.domain.ispring.repo.IspringReservePositionFormRepo;
import ru.yandex.market.hrms.core.domain.outstaff.repo.OutstaffEntityRepo;
import ru.yandex.market.hrms.core.domain.position.repo.OutstaffPositionRepo;
import ru.yandex.market.hrms.core.domain.property.TaskPropertyService;
import ru.yandex.market.hrms.core.domain.property.repo.TaskPropertyEntity;
import ru.yandex.market.hrms.core.domain.warehouse.repo.DomainWarehouseRepo;
import ru.yandex.market.hrms.core.service.domain.DomainService;
import ru.yandex.market.hrms.core.service.employee.EmployeeCandidateService;
import ru.yandex.market.hrms.core.service.employee.EmployeeService;
import ru.yandex.market.hrms.core.service.environment.EnvironmentService;
import ru.yandex.market.hrms.core.service.ispring.EmployeeCandidateIspringService;
import ru.yandex.market.hrms.core.service.ispring.EmployeeIspringService;
import ru.yandex.market.hrms.core.service.ispring.ISpringService;
import ru.yandex.market.hrms.core.service.ispring.IspringGroupService;
import ru.yandex.market.hrms.core.service.ispring.IspringMapper;
import ru.yandex.market.hrms.core.service.ispring.OutstaffIspringService;
import ru.yandex.market.hrms.core.service.sms.YaSmsService;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.ispring.ISpringClientMock;
import ru.yandex.market.ispring.pojo.GroupInfo;
import ru.yandex.market.ispring.pojo.GroupToGetDto;
import ru.yandex.market.ispring.pojo.UserToGetDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@DbUnitDataSet(schema = "public", before = "ISpringSyncManagerTest.before.csv")
public class ISpringSyncManagerTest extends AbstractTmsTest {

    private final LocalDate releaseDate = LocalDate.of(2021, 12, 10);

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private OutstaffEntityRepo outstaffEntityRepo;
    @Autowired
    private EmployeeIspringService employeeIspringService;
    @Autowired
    private EmployeeCandidateService candidateService;
    @Autowired
    private OutstaffIspringService outstaffIspringService;
    @Autowired
    private TaskPropertyService taskPropertyService;
    @Autowired
    private DomainWarehouseRepo domainWarehouseRepo;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private IspringMapper ispringMapper;
    @Autowired
    private DomainRepo domainRepo;
    @Autowired
    private DomainService domainService;
    @Autowired
    private OutstaffPositionRepo outstaffPositionRepo;
    @Autowired
    private IspringGroupInfoRepo ispringGroupInfoRepo;
    @Autowired
    private IspringReservePositionFormRepo ispringReservePositionFormRepo;
    @Autowired
    private EmployeeRepo employeeRepo;
    @Autowired
    private EmployeeIspringRepo employeeIspringRepo;
    @Autowired
    private IspringAccountRepository ispringAccountRepository;

    @Autowired
    private EmployeeCandidateIspringService candidateIspringService;

    @MockBean
    private YaSmsService yaSmsService;

    private ISpringSyncManager iSpringSyncManager;
    private final ISpringClientMock iSpringClient = Mockito.spy(new ISpringClientMock());

    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        ISpringService iSpringService = new ISpringService(iSpringClient, environmentService, ispringAccountRepository);
        IspringGroupService ispringGroupService = new IspringGroupService(iSpringClient, environmentService,
                domainWarehouseRepo, ispringReservePositionFormRepo, ispringGroupInfoRepo, employeeRepo,
                employeeIspringRepo, domainRepo, clock);

        iSpringSyncManager = new ISpringSyncManager(employeeService, employeeIspringService,
                outstaffIspringService, environmentService, ispringGroupService, outstaffEntityRepo,
                iSpringService, candidateIspringService, candidateService, ispringMapper, outstaffPositionRepo,
                domainService, taskPropertyService, clock);
        Mockito.clearInvocations(yaSmsService);
    }

    @Test
    @DbUnitDataSet(
            before = "ISpringSyncManagerTest.employeeWithCandidates.csv",
            after = "ISpringSyncManagerTest.syncEmployees.after.csv")
    void syncAllEmployeesTest() {
        var initMap = new HashMap<String, UserToGetDto>();
        initMap.put("78889990077",
                userToGetDto("78889990077", "dep1", "name1", "job1", releaseDate, "test1@yandex-team.ru"));
        initMap.put("79014311223",
                userToGetDto("79014311223", "root_department", "Сергеев", "job2", null, "test1@yandex-team.ru"));
        iSpringClient.setUsers(initMap);
        iSpringSyncManager.syncAllStaffEmployees();

        Assertions.assertThat(iSpringClient.getAddedUsersCount())
                .isEqualTo(2);
        Assertions.assertThat(iSpringClient.getModifiedUsersCount())
                .isEqualTo(0);

        Assertions.assertThat(iSpringClient.getUsers().values())
                .usingElementComparatorOnFields("login", "departmentId")
                .containsExactlyInAnyOrder(
                        userToGetDto("78889990077", "dep1", "name1", "job1", releaseDate, "test1@yandex-team.ru"),
                        userToGetDto("79014311223", "root_department", "Сергеев", "job2", releaseDate, "test2@yandex" +
                                "-team.ru"),
                        userToGetDto("79160806565", "shift2_department",
                                "Тимур", "Кладовщик", LocalDate.of(2021, 2, 1), "timursha@yandex-team.ru"),

                        userToGetDto("79775130017", "shift1_department",
                                "Сергей", "Кладовщик", LocalDate.of(2021, 2, 1), "kashapov-s@yandex-team.ru")
                );
    }

    @Test
    @DbUnitDataSet(
            schema = "public",
            before = "ISpringSyncManagerTest.withOutstaff.csv",
            after = "ISpringSyncManagerTest.syncOutstaff.after.csv")
    void syncOutstaffTest() {
        mockClock(LocalDate.of(2021, 9, 13));

        var initMap = new HashMap<String, UserToGetDto>();
        initMap.put("70001000100", userToGetDto("70001000100", "dev0", "name0", "job0", null, "dev0@yandex.ru"));
        initMap.put("70001000101", userToGetDto("70001000101", "dev1", "name1", "job1", null, "dev1@yandex.ru"));
        initMap.put("70001000102", userToGetDto("70001000102", "dev2", "name2", "job2", null, "dev2@yandex.ru"));
        iSpringClient.setUsers(initMap);

        String groupId = UUID.randomUUID().toString();
        iSpringClient.setGroups(Map.of(
                groupId, groupToGetDto(groupId, "Аутсорс Софьино")
        ));

        iSpringSyncManager.syncOutstaffEmployees();

        Assertions.assertThat(iSpringClient.getUsers().values())
                .usingElementComparatorOnFields("login", "departmentId", "groups")
                .containsExactlyInAnyOrder(
                        userToGetDto("70001000100", "dev0", "name0", "job0", null, "dev0@yandex.ru"),
                        userToGetDto("70001000101", "dev1", "name1", "job1", null, "dev1@yandex.ru"),
                        userToGetDto("70001000102", "dev2", "name2", "job2", null, "dev2@yandex.ru"),
                        userToGetDto("70001000103", "out1_department", "name3", "job3", List.of(groupId), null, "out1" +
                                "@yandex.ru"),
                        userToGetDto("70001000104", "out2_department", "name4", "job4", List.of(groupId), null, "out2" +
                                "@yandex.ru")
                );
        verify(iSpringClient).inactivateUser("userId-70001000105");

        List<String> actualGroupNames = iSpringClient.groups().stream()
                .map(GroupInfo::getName)
                .collect(Collectors.toList());
        assertEquals(List.of("Аутсорс Софьино"), actualGroupNames);
    }

    @Test
    @DbUnitDataSet(
            schema = "public",
            before = "ISpringSyncManagerTest.withEmployeeCandidates.csv",
            after = "ISpringSyncManagerTest.syncEmployeeCandidates.after.csv")
    void syncEmployeeCandidatesTest() {
        mockClock(LocalDate.of(2022, 1, 2));

        var initMap = new HashMap<String, UserToGetDto>();
        initMap.put("79009009092", userToGetDto("79009009092", "dev0", "Старк", "Кладовщик", null, "dev0@yandex.ru"));
        initMap.put("79009009091", userToGetDto("79009009091", "dev1", "Старк", "Кладовщик", null, "dev1@yandex.ru"));
        iSpringClient.setUsers(initMap);

        iSpringSyncManager.syncEmployeeCandidates();

        Assertions.assertThat(iSpringClient.getAddedUsersCount())
                .isEqualTo(1);
        Assertions.assertThat(iSpringClient.getModifiedUsersCount())
                .isEqualTo(0);

        Assertions.assertThat(iSpringClient.getUsers().values())
                .usingElementComparatorOnFields("login", "departmentId")
                .containsExactlyInAnyOrder(
                        userToGetDto("79009009092", "dev0", "Старк", "Кладовщик", null, "dev0@yandex.ru"),
                        userToGetDto("79009009091", "dev1", "Старк", "Кладовщик", null, "dev1@yandex.ru"),

                        userToGetDto("79009009090", "root_department",
                                "Сноу", "Водитель штабелера", LocalDate.of(2022, 2, 15), "jon-snow@yandex-team.ru")
                );
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "ISpringSyncManagerTest.withOutstaff.csv",
                    "ISpringSyncManagerTest.withSms.csv",
                    "ISpringSyncManagerTest.withWhiteList.csv"},
            after = "ISpringSyncManagerTest.withWhiteList.after.csv"
    )
    void shouldSendSmsToWhiteListOnly() {
        iSpringSyncManager.syncOutstaffEmployees();
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "ISpringSyncManagerTest.withOutstaff.csv",
                    "ISpringSyncManagerTest.withSms.csv"
            },
            after = "ISpringSyncManagerTest.withSms.after.csv")
    void shouldSendSmsWhenFlagPresent() {
        iSpringSyncManager.syncOutstaffEmployees();
    }

    @Test
    @DbUnitDataSet(
            before = "ISpringSyncManagerTest.withOutstaff.csv",
            after = "ISpringSyncManagerTest.syncOutstaff.after.csv"
    )
    void shouldNotSendSmsWhenNoFlag() throws Exception {
        mockClock(LocalDate.of(2021, 9, 13));
        Map<String, TaskPropertyEntity> properties = taskPropertyService.loadAllProperties(
                "ispringEmployeeSync");
        iSpringSyncManager.syncOutstaffEmployees();
        verify(yaSmsService, never())
                .sendSms(anyString(), anyString());
    }

    @Test
    @DbUnitDataSet(
            before = "ISpringSyncManagerTest.outstaff.duplicates.before.csv",
            after = "ISpringSyncManagerTest.outstaff.duplicates.after.csv"
    )
    void shouldReuseISpringAccountsForDuplicate() {
        iSpringSyncManager.syncOutstaffEmployees();
    }

    @Test
    @DbUnitDataSet(
            before = "ISpringSyncManagerTest.outstaff.duplicatesanothername.before.csv",
            after = "ISpringSyncManagerTest.outstaff.duplicatesanothername.after.csv"
    )
    void shouldNotReuseISpringAccountsForDuplicateWithAnotherName() {
        iSpringSyncManager.syncOutstaffEmployees();
    }


    @Test
    @DbUnitDataSet(
            before = {"ISpringSyncManagerTest.employee.updatePosition.before.csv",
                    "ISpringSyncManagerTest.employeeWithCandidates.csv"},
            after = "ISpringSyncManagerTest.employee.updatePosition.after.csv"
    )
    void shouldUpdateEmployeePosition() {
        iSpringSyncManager.syncPositions();
    }

    @Test
    @DbUnitDataSet(
            before = {"ISpringSyncManagerTest.withOutstaff.csv",
                    "ISpringSyncManagerTest.outstaff.updatePosition.before.csv",},
            after = "ISpringSyncManagerTest.outstaff.updatePosition.after.csv"
    )
    void shouldUpdateOutstaffPosition() {
        iSpringSyncManager.syncPositions();
    }

    @Test
    @DbUnitDataSet(
            before = {"ISpringSyncManagerTest.withEmployeeCandidates.csv",
                    "ISpringSyncManagerTest.candidates.updatePosition.before.csv",},
            after = "ISpringSyncManagerTest.candidates.updatePosition.after.csv"
    )
    void shouldUpdateCandidatePosition() {
        iSpringSyncManager.syncPositions();
    }

    private UserToGetDto userToGetDto(String login, String departmentId, String name,
                                      String jobTitle, LocalDate releaseDate, String email) {
        return userToGetDto(login, departmentId, name, jobTitle, null, releaseDate, email);
    }

    private UserToGetDto userToGetDto(String login, String departmentId, String name, String jobTitle,
                                      List<String> groupIds, LocalDate releaseDate, String email) {
        var user = new UserToGetDto();
        user.setUserId("userId-" + login);
        user.setDepartmentId(departmentId);
        user.setGroups(groupIds);
        var fields = new ArrayList<UserToGetDto.UserField>();
        fields.add(new UserToGetDto.UserField("LOGIN", login));
        fields.add(new UserToGetDto.UserField("FIRST_NAME", name));
        fields.add(new UserToGetDto.UserField("LAST_NAME", name));
        fields.add(new UserToGetDto.UserField("JOB_TITLE", jobTitle));
        fields.add(new UserToGetDto.UserField("EMAIL", email));
        fields.add(new UserToGetDto.UserField("Release_date",
                Optional.ofNullable(releaseDate)
                        .map(LocalDate::toString)
                        .orElse(null)));

        user.setFields(fields);
        return user;
    }

    private GroupToGetDto groupToGetDto(String groupId, String name) {
        return new GroupToGetDto(groupId, name, 0, List.of());
    }
}
