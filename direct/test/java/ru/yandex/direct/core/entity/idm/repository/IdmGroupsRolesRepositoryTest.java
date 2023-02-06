package ru.yandex.direct.core.entity.idm.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.model.IdmGroupRole;
import ru.yandex.direct.core.entity.idm.model.IdmRequiredRole;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmGroupsRolesRepositoryTest {

    private static final long IDM_GROUP_ID = 11L;
    @Autowired
    private Steps steps;
    @Autowired
    private IdmGroupsRepository idmGroupsRepository;
    @Autowired
    private IdmGroupsRolesRepository idmGroupsRolesRepository;
    private IdmGroupRole startGroupRole;
    private Integer shard;
    private ClientId startClientId;

    @Before
    public void setUp() throws Exception {
        ClientInfo startClientInfo = steps.clientSteps().createDefaultClient();
        IdmGroup startIdmGroup = new IdmGroup().withIdmGroupId(IDM_GROUP_ID).withRequiredRole(IdmRequiredRole.MANAGER);
        idmGroupsRepository.add(singletonList(startIdmGroup));
        startClientId = startClientInfo.getClientId();
        startGroupRole = new IdmGroupRole()
                .withClientId(startClientId)
                .withIdmGroupId(startIdmGroup.getIdmGroupId());
        shard = startClientInfo.getShard();
        idmGroupsRolesRepository.addRolesWhichNotExist(shard, singletonList(startGroupRole));
    }

    @Test
    public void getAllRoles_success() {
        List<IdmGroupRole> allRoles = idmGroupsRolesRepository.getAllRoles(shard);
        assertThat(allRoles).contains(startGroupRole);
    }

    @Test
    public void getRole_success() {
        IdmGroupRole actualRole =
                idmGroupsRolesRepository.getRole(shard, startGroupRole.getClientId(), IDM_GROUP_ID).orElse(null);
        assertThat(actualRole)
                .is(matchedBy(beanDiffer(startGroupRole)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addRolesWhichNotExist_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        IdmGroupRole newRole = new IdmGroupRole()
                .withClientId(clientInfo.getClientId())
                .withIdmGroupId(IDM_GROUP_ID);
        idmGroupsRolesRepository.addRolesWhichNotExist(shard, singletonList(newRole));
        IdmGroupRole actualRole = idmGroupsRolesRepository.getRole(shard, clientInfo.getClientId(),
                IDM_GROUP_ID).orElse(null);
        assertThat(actualRole)
                .is(matchedBy(beanDiffer(newRole)
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void removeRoles_success() {
        idmGroupsRolesRepository.removeRoles(shard, singletonList(startGroupRole));
        List<IdmGroupRole> allRoles = idmGroupsRolesRepository.getAllRoles(shard);
        assertThat(allRoles).doesNotContain(startGroupRole);
    }

    @Test
    public void removeRole_success() {
        idmGroupsRolesRepository.removeRole(shard, startClientId, IDM_GROUP_ID);
        List<IdmGroupRole> allRoles = idmGroupsRolesRepository.getAllRoles(shard);
        assertThat(allRoles).doesNotContain(startGroupRole);
    }

}
