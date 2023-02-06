package ru.yandex.market.hrms.tms.manager.ispring;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.ispring.repo.DepartmentIspringRepo;
import ru.yandex.market.hrms.core.domain.ispring.repo.IspringAccountRepository;
import ru.yandex.market.hrms.core.service.domain.DomainService;
import ru.yandex.market.hrms.core.service.environment.EnvironmentService;
import ru.yandex.market.hrms.core.service.ispring.GroupDepartmentIspringService;
import ru.yandex.market.hrms.core.service.ispring.ISpringService;
import ru.yandex.market.hrms.core.service.ispring.IspringStructureService;
import ru.yandex.market.hrms.core.service.sms.YaSmsService;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.ispring.ISpringClientMock;
import ru.yandex.market.ispring.pojo.DepartmentDto;

@DbUnitDataSet(schema = "public", before = "ISpringSyncManagerTest.before.csv")
public class ISpringSyncStructureManagerTest extends AbstractTmsTest {

    @Autowired
    private DepartmentIspringRepo departmentIspringRepo;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private IspringStructureService structureService;
    @Autowired
    private GroupDepartmentIspringService groupDepartmentIspringService;
    @Autowired
    private DomainService domainService;
    @Autowired
    private IspringAccountRepository ispringAccountRepository;

    @MockBean
    private YaSmsService yaSmsService;

    private ISpringStructureSyncManager structureSyncManager;

    private final ISpringClientMock iSpringClient = Mockito.spy(new ISpringClientMock());

    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        ISpringService iSpringService = new ISpringService(iSpringClient, environmentService, ispringAccountRepository);

        structureSyncManager = new ISpringStructureSyncManager(departmentIspringRepo,
                iSpringService, structureService, groupDepartmentIspringService, domainService, clock);
        Mockito.clearInvocations(yaSmsService);
    }

    @Test
    @DbUnitDataSet(after = "ISpringSyncManagerTest.syncDomains.after.csv")
    void shouldSyncDomainStructure() {
        mockClock(LocalDate.of(2021, 9, 13));

        Map<String, DepartmentDto> departmentDtos = new HashMap<>();
        departmentDtos.put("root_department", departmentDto("root_department", "dep1", null));
        departmentDtos.put("shift1_department", departmentDto("shift1_department", "shift_1_group", "root_department"));
        departmentDtos.put("shift2_department", departmentDto("shift2_department", "shift_2_group", "root_department"));
        departmentDtos.put("sof_root_department", departmentDto("sof_root_department", "dep_sof", null));
        iSpringClient.setDepartments(departmentDtos);

        structureSyncManager.syncAllDomains();
    }

    @Test
    @DbUnitDataSet(before = "ISpringSyncManagerTest.syncGroup.before.csv",
            after = "ISpringSyncManagerTest.syncGroup.after.csv")
    void shouldSyncEmployeeGroups() {
        mockClock(LocalDate.of(2021, 2, 4));
        structureSyncManager.syncGroupTree();
    }

    @Test
    @DbUnitDataSet(before = {"ISpringSyncManagerTest.withOutstaff.csv",
            "ISpringSyncManagerTest.withOutstaffIspring.csv"},
            after = "ISpringSyncManagerTest.synOutstaffGroup.after.csv")
    void shouldSyncOutstaffGroups() {
        mockClock(LocalDate.of(2021, 2, 4));
        structureSyncManager.syncGroupTree();
    }

    @Test
    @DbUnitDataSet(before = "ISpringSyncManagerTest.withEmployeeCandidates.csv",
            after = "ISpringSyncManagerTest.withEmployeeCandidates.after.csv")
    void shouldSyncCandidatesGroups() {
        mockClock(LocalDate.of(2022, 1, 4));
        structureSyncManager.syncGroupTree();
    }

    private DepartmentDto departmentDto(String departmentId, String name, String parentDepartment) {
        return createDepartmentDto(departmentId, name, parentDepartment);
    }

    private DepartmentDto createDepartmentDto(String departmentId, String name, String parentDepartment) {
        var department = new DepartmentDto();

        department.setDepartmentId(departmentId);
        department.setName(name);
        department.setParentDepartmentId(parentDepartment);
        department.setCode(name);

        return department;
    }
}
