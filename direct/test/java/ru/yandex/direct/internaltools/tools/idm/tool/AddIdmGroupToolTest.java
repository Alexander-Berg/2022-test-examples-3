package ru.yandex.direct.internaltools.tools.idm.tool;

import java.util.List;

import one.util.streamex.StreamEx;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.repository.IdmGroupsRepository;
import ru.yandex.direct.core.testing.info.IdmGroupMemberInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.idm.model.IdmGroupAddParameters;
import ru.yandex.direct.internaltools.tools.idm.model.IntToolIdmGroup;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestIdmGroups.DEFAULT_IDM_GROUP_ID;
import static ru.yandex.direct.core.testing.data.TestIdmGroups.DEFAULT_REQUIRED_ROLE;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddIdmGroupToolTest {

    @Autowired
    private AddIdmGroupTool tool;
    @Autowired
    private Steps steps;
    @Autowired
    private IdmGroupsRepository idmGroupsRepository;


    private IdmGroup startIdmGroup;

    @Before
    public void setUp() {
        IdmGroupMemberInfo idmGroupMember = steps.idmGroupSteps().createDefaultIdmGroupMember();
        startIdmGroup = idmGroupMember.getIdmGroup();
    }

    @Test
    public void validate_success() {
        IdmGroupAddParameters params = new IdmGroupAddParameters();
        params.setId(getNotExistGroupId());
        params.setRoleName(DEFAULT_REQUIRED_ROLE.getTypedValue());

        ValidationResult<IdmGroupAddParameters, Defect> vr = tool.validate(params);

        AssertionsForClassTypes.assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void getMassData_success() {
        IntToolIdmGroup expected = new IntToolIdmGroup()
                .withId(startIdmGroup.getIdmGroupId())
                .withRoleName(startIdmGroup.getRequiredRole().getTypedValue());

        List<IntToolIdmGroup> massData = tool.getMassData();

        assertThat(massData)
                .as("the expected IdmGroup is in collection")
                .anySatisfy(g -> assertThat(g).isEqualToComparingFieldByFieldRecursively(expected));
    }

    @Test
    public void addMassData_success() {
        Long groupId = getNotExistGroupId();
        String roleName = DEFAULT_REQUIRED_ROLE.getTypedValue();
        IdmGroupAddParameters params = new IdmGroupAddParameters();
        params.setId(groupId);
        params.setRoleName(roleName);

        tool.getMassData(params);

        IdmGroup actual = idmGroupsRepository.getGroups(singletonList(groupId)).get(0);
        assertThat(actual.getRequiredRole()).isEqualTo(DEFAULT_REQUIRED_ROLE);
    }

    private Long getNotExistGroupId() {
        Long maxOrDefault = StreamEx.of(idmGroupsRepository.getAllGroupsRoles())
                .map(IdmGroup::getIdmGroupId)
                .maxByLong(l -> l)
                .orElse(DEFAULT_IDM_GROUP_ID);
        return maxOrDefault + 1;
    }

}
