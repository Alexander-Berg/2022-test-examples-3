package ru.yandex.direct.core.entity.idm.repository;


import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.model.IdmGroupMember;
import ru.yandex.direct.core.entity.idm.model.IdmRequiredRole;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.idm.container.IdmGroupsMembersQueryFilter.allIdmGroupsMembersFilter;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmGroupsMembersRepositoryTest {

    private static final long IDM_GROUP_ID = 11L;
    @Autowired
    private Steps steps;
    @Autowired
    private IdmGroupsRepository idmGroupsRepository;
    @Autowired
    private IdmGroupsMembersRepository idmGroupsMembersRepository;
    private IdmGroupMember startGroupMember;
    private Integer shard;
    private ClientId startClientId;
    private IdmGroup startIdmGroup;

    @Before
    public void setUp() throws Exception {
        UserInfo startUserInfo =
                steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER).getChiefUserInfo();
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
    public void getMembers_getAllMembers_success() {
        List<IdmGroupMember> allMembers = idmGroupsMembersRepository.getMembers(shard, allIdmGroupsMembersFilter());
        assertThat(allMembers).contains(startGroupMember);
    }

    @Test
    public void getMember_success() {
        IdmGroupMember member =
                idmGroupsMembersRepository.getMember(shard, startClientId, IDM_GROUP_ID).orElse(null);
        assertThat(member).isEqualTo(startGroupMember);
    }

    @Test
    public void addMembersWhichNotExist() {
        UserInfo newManager = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER).getChiefUserInfo();
        IdmGroupMember newMember = new IdmGroupMember().withUid(newManager.getUid())
                .withClientId(newManager.getClientInfo().getClientId())
                .withDomainLogin(newManager.getUser().getDomainLogin())
                .withIdmGroupId(startIdmGroup.getIdmGroupId())
                .withLogin(newManager.getUser().getLogin());

        idmGroupsMembersRepository.addMembersWhichNotExist(shard, asList(startGroupMember, newMember));

        List<IdmGroupMember> allMembers = idmGroupsMembersRepository.getMembers(shard, allIdmGroupsMembersFilter());
        assertThat(allMembers).contains(startGroupMember, newMember);
    }

    @Test
    public void removeMembers_success() {
        UserInfo newManager = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER).getChiefUserInfo();
        IdmGroupMember newMember = new IdmGroupMember().withUid(newManager.getUid())
                .withClientId(newManager.getClientInfo().getClientId())
                .withDomainLogin(newManager.getUser().getDomainLogin())
                .withIdmGroupId(startIdmGroup.getIdmGroupId())
                .withLogin(newManager.getUser().getLogin());
        idmGroupsMembersRepository.addMembersWhichNotExist(shard, asList(startGroupMember, newMember));
        ClientId clientId = newMember.getClientId();
        IdmGroupMember member = idmGroupsMembersRepository.getMember(shard, clientId, IDM_GROUP_ID).orElse(null);
        assumeThat(member, is(newMember));

        idmGroupsMembersRepository.removeMembers(shard, singletonList(newMember));

        IdmGroupMember actualMember = idmGroupsMembersRepository.getMember(shard, clientId, IDM_GROUP_ID).orElse(null);
        assertThat(actualMember).isNull();
    }
}


