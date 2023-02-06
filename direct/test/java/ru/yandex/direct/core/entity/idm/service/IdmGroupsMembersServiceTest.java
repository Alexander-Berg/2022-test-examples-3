package ru.yandex.direct.core.entity.idm.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.model.IdmGroupMember;
import ru.yandex.direct.core.entity.idm.model.IdmRequiredRole;
import ru.yandex.direct.core.entity.idm.repository.IdmGroupsMembersRepository;
import ru.yandex.direct.core.entity.idm.repository.IdmGroupsRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.idm.container.IdmGroupsMembersQueryFilter.allIdmGroupsMembersFilter;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmGroupsMembersServiceTest {

    private static final long IDM_GROUP_ID = 11L;
    @Autowired
    private Steps steps;
    @Autowired
    private IdmGroupsRepository idmGroupsRepository;
    @Autowired
    private IdmGroupsMembersRepository idmGroupsMembersRepository;
    @Autowired
    private IdmGroupsMembersService idmGroupsMembersService;
    private IdmGroupMember startGroupMember;
    private Integer shard;
    private ClientId startClientId;
    private IdmGroup startIdmGroup;

    @Before
    public void setUp() throws Exception {
        UserInfo startUserInfo =
                steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER).getChiefUserInfo();
        startIdmGroup = new IdmGroup().withIdmGroupId(IDM_GROUP_ID).withRequiredRole(IdmRequiredRole.MANAGER);
        idmGroupsRepository.add(singletonList(startIdmGroup));
        Long startUid = startUserInfo.getUid();
        User startUser = startUserInfo.getUser();
        startClientId = startUserInfo.getClientInfo().getClientId();
        startGroupMember = new IdmGroupMember().withUid(startUid)
                .withClientId(startClientId)
                .withDomainLogin(startUser.getDomainLogin())
                .withIdmGroupId(startIdmGroup.getIdmGroupId())
                .withLogin(startUser.getLogin());
        shard = startUserInfo.getShard();
        idmGroupsMembersRepository.addMembersWhichNotExist(shard, singletonList(startGroupMember));
    }

    @Test
    public void addMembersWhichNotExist_success() {
        UserInfo newManager =
                steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER)
                        .getChiefUserInfo();
        IdmGroupMember newMember = new IdmGroupMember().withUid(newManager.getUid())
                .withClientId(newManager.getClientInfo().getClientId())
                .withDomainLogin(newManager.getUser().getDomainLogin())
                .withIdmGroupId(startIdmGroup.getIdmGroupId())
                .withLogin(newManager.getUser().getLogin());

        idmGroupsMembersService.addMembersWhichNotExist(asList(startGroupMember, newMember));

        List<IdmGroupMember> allMembers = idmGroupsMembersRepository.getMembers(shard, allIdmGroupsMembersFilter());
        assertThat(allMembers).contains(startGroupMember, newMember);
    }

    @Test
    public void removeMembers_success() {
        idmGroupsMembersService.removeMembers(singletonList(startGroupMember));

        List<IdmGroupMember> allMembers = idmGroupsMembersService.getAllMembers();
        assertThat(allMembers).doesNotContain(startGroupMember);
    }

    @Test
    public void removeMember_success() {
        idmGroupsMembersService.removeMember(startClientId, IDM_GROUP_ID);

        List<IdmGroupMember> allMembers = idmGroupsMembersService.getAllMembers();
        assertThat(allMembers).doesNotContain(startGroupMember);
    }

    @Test
    public void getAllMembers_success() {
        List<IdmGroupMember> allMembers = idmGroupsMembersService.getAllMembers();
        assertThat(allMembers).contains(startGroupMember);
    }

    @Test
    public void getMember_success() {
        IdmGroupMember member = idmGroupsMembersService.getMember(startClientId, IDM_GROUP_ID).orElse(null);
        assertThat(member).isEqualTo(startGroupMember);
    }
}
