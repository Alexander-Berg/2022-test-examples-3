package ru.yandex.direct.internaltools.tools.idm.tool;

import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.idm.model.IdmGroupMember;
import ru.yandex.direct.core.entity.idm.repository.IdmGroupsMembersRepository;
import ru.yandex.direct.core.testing.info.IdmGroupMemberInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.idm.model.IdmGroupMemberParameters;
import ru.yandex.direct.internaltools.tools.idm.model.IntToolIdmGroupMember;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ManageIdmGroupMembersToolTest {

    @Autowired
    private ManageIdmGroupMembersTool tool;
    @Autowired
    private Steps steps;
    @Autowired
    private IdmGroupsMembersRepository membersRepository;

    private IdmGroupMemberInfo startMemberInfo;
    private Long startGroupId;

    @Before
    public void setUp() {
        startMemberInfo = steps.idmGroupSteps().createDefaultIdmGroupMember();
        startGroupId = startMemberInfo.getIdmGroupId();
    }

    @Test
    public void validate_show_success() {
        IdmGroupMemberParameters params = new IdmGroupMemberParameters();
        params.setOperation(IdmGroupMemberParameters.SHOW_OPERATION);

        ValidationResult<IdmGroupMemberParameters, Defect> vr = tool.validate(params);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_add_success() {
        UserInfo newManagerInfo = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER)
                .getChiefUserInfo();
        IdmGroupMemberParameters params = new IdmGroupMemberParameters();
        params.setIdmGroupId(startGroupId);
        params.setLogin(newManagerInfo.getUser().getLogin());
        params.setOperation(IdmGroupMemberParameters.ADD_OPERATION);

        ValidationResult<IdmGroupMemberParameters, Defect> vr = tool.validate(params);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_delete_success() {
        IdmGroupMemberParameters params = new IdmGroupMemberParameters();
        params.setIdmGroupId(startGroupId);
        params.setLogin(startMemberInfo.getPassportLogin());
        params.setOperation(IdmGroupMemberParameters.DELETE_OPERATION);

        ValidationResult<IdmGroupMemberParameters, Defect> vr = tool.validate(params);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void getMassData_success() {
        IntToolIdmGroupMember expected = new IntToolIdmGroupMember()
                .withClientId(startMemberInfo.getClientId())
                .withDomainLogin(startMemberInfo.getDomainLogin())
                .withIdmGroupId(startGroupId)
                .withLogin(startMemberInfo.getPassportLogin())
                .withUid(startMemberInfo.getUid());

        List<IntToolIdmGroupMember> massData = tool.getMassData();

        Assertions.assertThat(massData)
                .as("the expected IntToolIdmGroupMember is in collection")
                .anySatisfy(g -> Assertions.assertThat(g).isEqualToComparingFieldByFieldRecursively(expected));
    }

    @Test
    public void getMassData_show_success() {
        IdmGroupMemberParameters params = new IdmGroupMemberParameters();
        params.setIdmGroupId(startGroupId);
        params.setLogin(startMemberInfo.getPassportLogin());
        params.setOperation(IdmGroupMemberParameters.SHOW_OPERATION);
        IntToolIdmGroupMember expected = new IntToolIdmGroupMember()
                .withClientId(startMemberInfo.getClientId())
                .withDomainLogin(startMemberInfo.getDomainLogin())
                .withIdmGroupId(startGroupId)
                .withLogin(startMemberInfo.getPassportLogin())
                .withUid(startMemberInfo.getUid());

        List<IntToolIdmGroupMember> massData = tool.getMassData(params);

        Assertions.assertThat(massData)
                .as("the expected IntToolIdmGroupMember is in collection")
                .anySatisfy(g -> Assertions.assertThat(g).isEqualToComparingFieldByFieldRecursively(expected));
    }

    @Test
    public void getMassData_add_success() {
        UserInfo newManagerInfo = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER)
                .getChiefUserInfo();
        IdmGroupMemberParameters params = new IdmGroupMemberParameters();
        params.setIdmGroupId(startGroupId);
        params.setLogin(newManagerInfo.getUser().getLogin());
        params.setOperation(IdmGroupMemberParameters.ADD_OPERATION);

        tool.getMassData(params);

        Optional<IdmGroupMember> actual =
                membersRepository.getMember(newManagerInfo.getShard(), newManagerInfo.getClientInfo().getClientId(),
                        startGroupId);
        Assertions.assertThat(actual).isNotEmpty();
    }

    @Test
    public void getMassData_delete_success() {
        IdmGroupMemberParameters params = new IdmGroupMemberParameters();
        params.setIdmGroupId(startGroupId);
        params.setLogin(startMemberInfo.getPassportLogin());
        params.setOperation(IdmGroupMemberParameters.DELETE_OPERATION);

        tool.getMassData(params);

        Optional<IdmGroupMember> actual =
                membersRepository.getMember(startMemberInfo.getShard(), startMemberInfo.getClientId(), startGroupId);
        Assertions.assertThat(actual).isEmpty();
    }

}
