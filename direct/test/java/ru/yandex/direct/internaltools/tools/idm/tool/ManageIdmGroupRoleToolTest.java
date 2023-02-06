package ru.yandex.direct.internaltools.tools.idm.tool;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.idm.model.IdmGroupRole;
import ru.yandex.direct.core.entity.idm.repository.IdmGroupsRolesRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.IdmGroupRoleInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.idm.model.IdmGroupRoleParameters;
import ru.yandex.direct.internaltools.tools.idm.model.IntToolIdmGroupRole;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ManageIdmGroupRoleToolTest {

    @Autowired
    private ManageIdmGroupRolesTool tool;
    @Autowired
    private Steps steps;
    @Autowired
    private IdmGroupsRolesRepository rolesRepository;

    private IdmGroupRoleInfo startRoleInfo;
    private Long startGroupId;

    @Before
    public void setUp() {
        startRoleInfo = steps.idmGroupSteps().createDefaultIdmGroupRole();
        startGroupId = startRoleInfo.getIdmGroupId();
    }

    @Test
    public void validate_show_success() {
        IdmGroupRoleParameters params = new IdmGroupRoleParameters();
        params.setOperation(IdmGroupRoleParameters.SHOW_OPERATION);

        ValidationResult<IdmGroupRoleParameters, Defect> vr = tool.validate(params);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_add_success() {
        ClientInfo newSubjectInfo = steps.clientSteps().createDefaultClient();
        IdmGroupRoleParameters params = new IdmGroupRoleParameters();
        params.setIdmGroupId(startGroupId);
        params.setClientId(newSubjectInfo.getClientId().asLong());
        params.setOperation(IdmGroupRoleParameters.ADD_OPERATION);

        ValidationResult<IdmGroupRoleParameters, Defect> vr = tool.validate(params);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_delete_success() {
        IdmGroupRoleParameters params = new IdmGroupRoleParameters();
        params.setIdmGroupId(startGroupId);
        params.setClientId(startRoleInfo.getClientId().asLong());
        params.setOperation(IdmGroupRoleParameters.DELETE_OPERATION);

        ValidationResult<IdmGroupRoleParameters, Defect> vr = tool.validate(params);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void getMassData_success_returnEmptyList() {
        List<IntToolIdmGroupRole> massData = tool.getMassData();

        Assertions.assertThat(massData)
                .isEmpty();
    }

    @Test
    public void getMassData_show_success() {
        IdmGroupRoleParameters params = new IdmGroupRoleParameters();
        params.setIdmGroupId(startGroupId);
        params.setClientId(startRoleInfo.getClientId().asLong());
        params.setOperation(IdmGroupRoleParameters.SHOW_OPERATION);
        IntToolIdmGroupRole expected = new IntToolIdmGroupRole()
                .withClientId(startRoleInfo.getClientId())
                .withIdmGroupId(startGroupId)
                .withName(startRoleInfo.getClientInfo().getClient().getName());

        List<IntToolIdmGroupRole> massData = tool.getMassData(params);

        Assertions.assertThat(massData)
                .as("the expected IntToolIdmGroupRole is in collection")
                .anySatisfy(g -> Assertions.assertThat(g).isEqualToComparingFieldByFieldRecursively(expected));
    }

    @Test
    public void getMassData_add_success() {
        ClientInfo newSubjectInfo = steps.clientSteps().createDefaultClient();
        IdmGroupRoleParameters params = new IdmGroupRoleParameters();
        params.setIdmGroupId(startGroupId);
        params.setClientId(newSubjectInfo.getClientId().asLong());
        params.setOperation(IdmGroupRoleParameters.ADD_OPERATION);

        tool.getMassData(params);

        Optional<IdmGroupRole> actual =
                rolesRepository.getRole(newSubjectInfo.getShard(), newSubjectInfo.getClientId(), startGroupId);
        Assertions.assertThat(actual).isNotEmpty();
    }

    @Test
    public void getMassData_delete_success() {
        IdmGroupRoleParameters params = new IdmGroupRoleParameters();
        params.setIdmGroupId(startGroupId);
        params.setClientId(startRoleInfo.getClientId().asLong());
        params.setOperation(IdmGroupRoleParameters.DELETE_OPERATION);

        tool.getMassData(params);

        Optional<IdmGroupRole> actual =
                rolesRepository.getRole(startRoleInfo.getShard(), startRoleInfo.getClientId(), startGroupId);
        Assertions.assertThat(actual).isEmpty();
    }

}
