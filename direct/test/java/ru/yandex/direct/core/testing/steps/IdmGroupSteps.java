package ru.yandex.direct.core.testing.steps;

import java.util.Objects;

import com.google.common.collect.Iterables;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.model.ClientPrimaryManager;
import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.model.IdmGroupMember;
import ru.yandex.direct.core.entity.idm.model.IdmGroupRole;
import ru.yandex.direct.core.entity.idm.model.IdmRequiredRole;
import ru.yandex.direct.core.entity.idm.repository.IdmGroupsMembersRepository;
import ru.yandex.direct.core.entity.idm.repository.IdmGroupsRepository;
import ru.yandex.direct.core.entity.idm.repository.IdmGroupsRolesRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ClientPrimaryManagerInfo;
import ru.yandex.direct.core.testing.info.IdmGroupMemberInfo;
import ru.yandex.direct.core.testing.info.IdmGroupRoleInfo;
import ru.yandex.direct.core.testing.info.SupportForClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.dbschema.ppcdict.enums.IdmGroupsRole;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacClientsRelations;
import ru.yandex.direct.rbac.RbacClientsRelationsStorage;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.model.ClientsRelationType;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestIdmGroups.DEFAULT_IDM_GROUP_ID;
import static ru.yandex.direct.core.testing.data.TestIdmGroups.defaultIdmGroup;
import static ru.yandex.direct.dbschema.ppc.tables.IdmGroupRoles.IDM_GROUP_ROLES;
import static ru.yandex.direct.dbschema.ppc.tables.IdmGroupsMembers.IDM_GROUPS_MEMBERS;
import static ru.yandex.direct.dbschema.ppcdict.tables.IdmGroups.IDM_GROUPS;

public class IdmGroupSteps {

    @Autowired
    private ClientSteps clientSteps;
    @Autowired
    private IdmGroupsRepository idmGroupsRepository;
    @Autowired
    private IdmGroupsMembersRepository membersRepository;
    @Autowired
    private IdmGroupsRolesRepository rolesRepository;
    @Autowired
    DslContextProvider dslContextProvider;
    @Autowired
    ShardHelper shardHelper;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private RbacClientsRelations rbacClientsRelations;
    @Autowired
    private RbacClientsRelationsStorage rbacClientsRelationsStorage;
    @Autowired
    private TestClientRepository testClientRepository;

    public IdmGroupMemberInfo createDefaultIdmGroupMember() {
        return addIdmGroupMember(new IdmGroupMemberInfo());
    }

    private IdmGroupMemberInfo addIdmGroupMember(IdmGroupMemberInfo idmGroupMemberInfo) {
        IdmGroupMember idmGroupMember = idmGroupMemberInfo.getIdmGroupMember();
        if (idmGroupMember == null) {
            UserInfo userInfo = idmGroupMemberInfo.getUserInfo();
            if (userInfo == null || userInfo.getUser() == null || userInfo.getUser().getUid() == null) {
                userInfo = clientSteps.createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER).getChiefUserInfo();
                idmGroupMemberInfo.withUserInfo(userInfo);
            }
            IdmGroup idmGroup = idmGroupMemberInfo.getIdmGroup();
            if (idmGroup == null || idmGroup.getIdmGroupId() == null) {
                idmGroup = Iterables.getFirst(idmGroupsRepository.getGroups(singleton(DEFAULT_IDM_GROUP_ID)), null);
                if (idmGroup == null) {
                    idmGroup = defaultIdmGroup();
                    idmGroupsRepository.add(singletonList(idmGroup));
                }
                idmGroupMemberInfo.withIdmGroup(idmGroup);
            }
            idmGroupMember = new IdmGroupMember()
                    .withUid(userInfo.getUid())
                    .withClientId(userInfo.getClientInfo().getClientId())
                    .withDomainLogin(userInfo.getUser().getDomainLogin())
                    .withIdmGroupId(idmGroup.getIdmGroupId())
                    .withLogin(userInfo.getUser().getLogin());
            idmGroupMemberInfo.withIdmGroupMember(idmGroupMember);
        }
        int shard = idmGroupMemberInfo.getShard();
        membersRepository.addMembersWhichNotExist(shard, singletonList(idmGroupMemberInfo.getIdmGroupMember()));
        return idmGroupMemberInfo;
    }

    public IdmGroup addIfNotExistIdmGroup(long groupId, IdmRequiredRole newRequiredRole) {
        IdmGroup existIdmGroup = Iterables.getFirst(idmGroupsRepository.getGroups(singleton(groupId)), null);
        if (existIdmGroup != null) {
            IdmRequiredRole existRequiredRoleRole = existIdmGroup.getRequiredRole();
            checkState(Objects.equals(existRequiredRoleRole, newRequiredRole),
                    "The group with id=%s already has the role '%s', the role '%s' cannot be set",
                    groupId, existRequiredRoleRole, newRequiredRole);
            return existIdmGroup;
        }
        IdmGroup newIdmGroup = new IdmGroup().withIdmGroupId(groupId).withRequiredRole(newRequiredRole);
        idmGroupsRepository.add(singletonList(newIdmGroup));
        return newIdmGroup;
    }

    public IdmGroupRoleInfo createDefaultIdmGroupRole() {
        return addIdmGroupRole(new IdmGroupRoleInfo());
    }

    public IdmGroupRoleInfo addIdmGroupRole(IdmGroupRoleInfo idmGroupRoleInfo) {
        IdmGroupRole idmGroupRole = idmGroupRoleInfo.getIdmGroupRole();
        if (idmGroupRole == null) {
            ClientInfo clientInfo = idmGroupRoleInfo.getClientInfo();
            if (clientInfo == null || clientInfo.getClient() == null || clientInfo.getClient().getClientId() == null) {
                clientInfo = clientSteps.createDefaultClient();
                idmGroupRoleInfo.withClientInfo(clientInfo);
            }
            IdmGroup idmGroup = idmGroupRoleInfo.getIdmGroup();
            if (idmGroup == null || idmGroup.getIdmGroupId() == null) {
                idmGroup = Iterables.getFirst(idmGroupsRepository.getGroups(singleton(DEFAULT_IDM_GROUP_ID)), null);
                if (idmGroup == null) {
                    idmGroup =
                            new IdmGroup().withIdmGroupId(DEFAULT_IDM_GROUP_ID).withRequiredRole(IdmRequiredRole.MANAGER);
                    idmGroupsRepository.add(singletonList(idmGroup));
                }
                idmGroupRoleInfo.withIdmGroup(idmGroup);
            }
            idmGroupRole = new IdmGroupRole()
                    .withClientId(clientInfo.getClientId())
                    .withIdmGroupId(idmGroup.getIdmGroupId());
            idmGroupRoleInfo.withIdmGroupRole(idmGroupRole);
        }
        int shard = idmGroupRoleInfo.getShard();
        rolesRepository.addRolesWhichNotExist(shard, singletonList(idmGroupRoleInfo.getIdmGroupRole()));
        return idmGroupRoleInfo;
    }

    public ClientPrimaryManagerInfo createIdmPrimaryManager() {
        UserInfo managerInfo =
                clientSteps.createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER).getChiefUserInfo();
        ClientInfo clientInfo = clientSteps.createDefaultClient();
        return addIdmPrimaryManager(managerInfo, clientInfo);
    }

    public ClientPrimaryManagerInfo addIdmPrimaryManager(UserInfo managerInfo, ClientInfo clientInfo) {
        clientSteps.setClientProperty(clientInfo, Client.PRIMARY_MANAGER_UID, managerInfo.getUid());
        clientSteps.setClientProperty(clientInfo, Client.IS_IDM_PRIMARY_MANAGER, true);

        campaignRepository.setManagerForAllClientCampaigns(clientInfo.getShard(), clientInfo.getClientId(),
                managerInfo.getUid());
        ClientPrimaryManager clientPrimaryManager = new ClientPrimaryManager()
                .withIsIdmPrimaryManager(true)
                .withPrimaryManagerUid(managerInfo.getUid())
                .withSubjectClientId(clientInfo.getClientId())
                .withDomainLogin(managerInfo.getUser().getDomainLogin())
                .withPassportLogin(managerInfo.getUser().getLogin());
        return new ClientPrimaryManagerInfo()
                .withClientPrimaryManager(clientPrimaryManager)
                .withSubjectClientInfo(clientInfo)
                .withManagerInfo(managerInfo);
    }

    public SupportForClientInfo createSupportForClientInfo() {
        UserInfo operatorInfo =
                clientSteps.createDefaultClientWithRoleInAnotherShard(RbacRole.SUPPORT).getChiefUserInfo();
        ClientInfo clientInfo = clientSteps.createDefaultClient();
        return addSupportForClient(operatorInfo, clientInfo);
    }

    public SupportForClientInfo addSupportForClient(UserInfo operatorInfo, ClientInfo subjectClientInfo) {
        rbacClientsRelations.addSupportRelation(subjectClientInfo.getClientId(), operatorInfo.getClientId());
        int shard = shardHelper.getShardByClientIdStrictly(subjectClientInfo.getClientId());
        Long relationId = rbacClientsRelationsStorage.getRelationId(shard, operatorInfo.getClientId(),
                subjectClientInfo.getClientId(), ClientsRelationType.SUPPORT_FOR_CLIENT);
        return new SupportForClientInfo()
                .withRelationId(relationId)
                .withOperatorInfo(operatorInfo)
                .withSubjectClientInfo(subjectClientInfo);
    }

    /**
     * Создаёт запись о фейковой Idm-группе менеджеров в {@code ppcdict.idm_groups}
     */
    public synchronized long createManagerIdmGroup() {
        long id = nextGroupId();
        dslContextProvider.ppcdict()
                .insertInto(IDM_GROUPS)
                .set(IDM_GROUPS.IDM_GROUP_ID, id)
                .set(IDM_GROUPS.ROLE, IdmGroupsRole.manager)
                .execute();
        return id;
    }

    private long nextGroupId() {
        Field<Long> nextId = DSL.nvl(DSL.max(IDM_GROUPS.IDM_GROUP_ID), 0L).plus(1L).as("nextId");
        return dslContextProvider.ppcdict().select(nextId).from(IDM_GROUPS).fetchOne(nextId);
    }

    /**
     * Добавляет групповой доступ к клиенту {@code subjectClientId}
     */
    public void addGroupAccess(Long groupId, ClientId subjectClientId) {
        int shard = shardHelper.getShardByClientIdStrictly(subjectClientId);
        dslContextProvider.ppc(shard)
                .insertInto(IDM_GROUP_ROLES)
                .set(IDM_GROUP_ROLES.IDM_GROUP_ID, groupId)
                .set(IDM_GROUP_ROLES.SUBJECT_CLIENT_ID, subjectClientId.asLong())
                .onDuplicateKeyIgnore()
                .execute();
    }

    /**
     * Добавляет пользователя {@code managerClientId} в группу
     */
    public void addGroupMembership(Long groupId, ClientId managerClientId) {
        int shard = shardHelper.getShardByClientIdStrictly(managerClientId);
        dslContextProvider.ppc(shard)
                .insertInto(IDM_GROUPS_MEMBERS)
                .set(IDM_GROUPS_MEMBERS.IDM_GROUP_ID, groupId)
                .set(IDM_GROUPS_MEMBERS.CLIENT_ID, managerClientId.asLong())
                .onDuplicateKeyIgnore()
                .execute();
    }

}
