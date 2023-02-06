package ru.yandex.market.hrms.core.service.isrping;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.ispring.entity.DepartmentIspring;
import ru.yandex.market.hrms.core.domain.ispring.entity.EmployeeIspring;
import ru.yandex.market.hrms.core.domain.ispring.repo.DepartmentIspringRepo;
import ru.yandex.market.hrms.core.domain.ispring.repo.EmployeeIspringRepo;
import ru.yandex.market.hrms.core.domain.ispring.repo.IspringGroupInfoRepo;
import ru.yandex.market.hrms.core.service.ispring.IspringGroupService;
import ru.yandex.market.ispring.ISpringClient;
import ru.yandex.market.ispring.pojo.UserToAddDto;

@DbUnitDataSet(schema = "public")
public class IspringGroupServiceTest extends AbstractCoreTest {

    @Autowired
    private IspringGroupService service;
    @Autowired
    private IspringGroupInfoRepo ispringGroupInfoRepo;
    @Autowired
    private DepartmentIspringRepo departmentIspringRepo;
    @Autowired
    private EmployeeIspringRepo employeeIspringRepo;
    @Autowired
    private ISpringClient ispringClient;

    @Test
    @DbUnitDataSet(before = "IspringGroupServiceTest.shouldAssignReservePositionsFromForms.before.csv")
    void shouldAssignReservePositionsFromForms() {
        String depId = ispringClient.addDepartment("ФФЦ Софьино", "", null).getId();

        String grId1 = ispringClient.addGroup("Кладовщик").getId();
        String grId2 = ispringClient.addGroup("Диспетчер").getId();

        UserToAddDto user1 = new UserToAddDto();
        user1.setLogin("login1");
        user1.setLoginField("login1");
        user1.setDepartmentId(depId);
        user1.setGroups(List.of(grId1));
        String uId1 = ispringClient.addUser(user1).getId();

        UserToAddDto user2 = new UserToAddDto();
        user2.setLogin("login2");
        user2.setLoginField("login2");
        user2.setDepartmentId(depId);
        user2.setGroups(List.of(grId1));
        String uId2 = ispringClient.addUser(user2).getId();

        ispringGroupInfoRepo.saveNewMappings(
                Map.of("Кладовщик", grId1,
                        "Диспетчер", grId2)
        );
        departmentIspringRepo.save(
                DepartmentIspring.builder()
                        .departmentId(depId)
                        .name("ФФЦ Софьино")
                        .build()
        );
        employeeIspringRepo.save(
                EmployeeIspring.builder()
                        .employeeId(1L)
                        .ispringId(uId1)
                        .departmentId(depId)
                        .build()
        );
        employeeIspringRepo.save(
                EmployeeIspring.builder()
                        .employeeId(2L)
                        .ispringId(uId2)
                        .departmentId(depId)
                        .build()
        );

        service.assignReservePositionsFromForms();

        String grId3 = ispringGroupInfoRepo.getMap().get("Специалист по операционным инцидентам");
        Assertions.assertNotNull(grId3);
        Assertions.assertNotNull(ispringClient.user(uId1).getGroups());
        Assertions.assertEquals(ispringClient.user(uId1).getGroups().size(), 2);
        Assertions.assertTrue(ispringClient.user(uId1).getGroups().contains(grId2));
        Assertions.assertNotNull(ispringClient.user(uId2).getGroups());
        Assertions.assertEquals(ispringClient.user(uId2).getGroups().size(), 2);
        Assertions.assertTrue(ispringClient.user(uId2).getGroups().contains(grId3));
    }
}
