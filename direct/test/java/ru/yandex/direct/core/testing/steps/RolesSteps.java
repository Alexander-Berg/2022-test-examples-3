package ru.yandex.direct.core.testing.steps;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.AgencyClientRelationService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.model.IdmRequiredRole;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.data.TestRoleRelation;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.IdmGroupRoleInfo;
import ru.yandex.direct.core.testing.info.RoleRelationInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestIdmGroups.DEFAULT_IDM_GROUP_ID;
import static ru.yandex.direct.core.testing.steps.ClientSteps.ANOTHER_SHARD;
import static ru.yandex.direct.core.testing.steps.ClientSteps.DEFAULT_SHARD;

public class RolesSteps {

    private UserSteps userSteps;
    private ClientSteps clientSteps;
    private IdmGroupSteps idmGroupSteps;
    private UserService userService;
    private ClientService clientService;
    private AgencyClientRelationService agencyService;
    private RbacService rbacService;
    private TestClientRepository testClientRepository;

    @Autowired
    public RolesSteps(UserSteps userSteps,
                      ClientSteps clientSteps,
                      IdmGroupSteps idmGroupSteps,
                      UserService userService,
                      ClientService clientService,
                      AgencyClientRelationService agencyService,
                      RbacService rbacService,
                      TestClientRepository testClientRepository) {
        this.userSteps = userSteps;
        this.clientSteps = clientSteps;
        this.idmGroupSteps = idmGroupSteps;
        this.userService = userService;
        this.clientService = clientService;
        this.agencyService = agencyService;
        this.rbacService = rbacService;
        this.testClientRepository = testClientRepository;
    }

    public RoleRelationInfo getRoleRelationInfo(TestRoleRelation relation) {
        switch (relation) {
            case AGENCY_AND_SUB_CLIENT:
                return agencyAndSubClient();
            case AGENCY_REP_AND_SUB_CLIENT:
                return agencyRepAndSubClient();
            case CLIENT_AND_OTHER_CLIENT:
                return clientAndOtherClient();
            case CLIENT_CHIEF_AND_SAME_CLIENT:
                return clientAndSameClient();
            case CLIENT_MANAGER_AND_CLIENT:
                return clientManagerAndClient();
            case IDM_MANAGER_AND_CLIENT:
                return idmManagerAndClient();
            case IDM_PRIMARY_MANAGER_AND_CLIENT:
                return idmPrimaryManagerAndClient();
            case REP_AND_CHIEF:
                return repAndChief();
            case SUPER_AND_CLIENT:
                return superAndClient();
            case SUPER_READER_AND_CLIENT:
                return superReaderAndClient();
            case SUPPORT_AND_CLIENT:
                return supportAndClient();
            default:
                throw new UnsupportedOperationException();
        }
    }

    private RoleRelationInfo supportAndClient() {
        UserInfo operatorUserInfo = getOperatorInfo(RbacRole.SUPPORT);
        UserInfo clientUserInfo = getOwnerInfo(RbacRole.CLIENT);
        return new RoleRelationInfo()
                .withOperatorInfo(operatorUserInfo)
                .withOwnerInfo(clientUserInfo);
    }

    private RoleRelationInfo superReaderAndClient() {
        UserInfo operatorUserInfo = getOperatorInfo(RbacRole.SUPERREADER);
        UserInfo clientUserInfo = getOwnerInfo(RbacRole.CLIENT);
        return new RoleRelationInfo()
                .withOperatorInfo(operatorUserInfo)
                .withOwnerInfo(clientUserInfo);
    }

    private RoleRelationInfo superAndClient() {
        UserInfo operatorUserInfo = getOperatorInfo(RbacRole.SUPER);
        UserInfo clientUserInfo = getOwnerInfo(RbacRole.CLIENT);
        return new RoleRelationInfo()
                .withOperatorInfo(operatorUserInfo)
                .withOwnerInfo(clientUserInfo);
    }

    private RoleRelationInfo repAndChief() {
        UserInfo clientUserInfo = getOwnerInfo(RbacRole.CLIENT);
        UserInfo repUserInfo = userSteps.createUser(clientUserInfo.getClientInfo(), RbacRepType.MAIN);
        return new RoleRelationInfo()
                .withOperatorInfo(repUserInfo)
                .withOwnerInfo(clientUserInfo);
    }

    private RoleRelationInfo idmPrimaryManagerAndClient() {
        UserInfo managerUserInfo = getOperatorInfo(RbacRole.MANAGER);
        UserInfo clientUserInfo = getOwnerInfo(RbacRole.CLIENT);
        idmGroupSteps.addIdmPrimaryManager(managerUserInfo, clientUserInfo.getClientInfo());
        return new RoleRelationInfo()
                .withOperatorInfo(managerUserInfo)
                .withOwnerInfo(clientUserInfo);
    }

    private RoleRelationInfo idmManagerAndClient() {
        UserInfo managerUserInfo = getOperatorInfo(RbacRole.MANAGER);
        UserInfo clientUserInfo = getOwnerInfo(RbacRole.CLIENT);
        IdmGroup idmGroup = idmGroupSteps.addIfNotExistIdmGroup(DEFAULT_IDM_GROUP_ID, IdmRequiredRole.MANAGER);
        idmGroupSteps.addIdmGroupRole(
                new IdmGroupRoleInfo()
                        .withClientInfo(clientUserInfo.getClientInfo())
                        .withIdmGroup(idmGroup));
        idmGroupSteps.addGroupMembership(idmGroup.getIdmGroupId(), managerUserInfo.getClientId());
        return new RoleRelationInfo()
                .withOperatorInfo(managerUserInfo)
                .withOwnerInfo(clientUserInfo);
    }

    private RoleRelationInfo agencyRepAndSubClient() {
        UserInfo agencyUserInfo = getOperatorInfo(RbacRole.AGENCY);
        UserInfo agencyRepInfo = userSteps.createRepresentative(agencyUserInfo.getClientInfo(), RbacRepType.MAIN);
        UserInfo clientUserInfo = getOwnerInfo(RbacRole.CLIENT);
        agencyService.bindClients(agencyRepInfo.getClientId(), singleton(clientUserInfo.getClientId()));
        rbacService.bindClientsToAgency(agencyRepInfo.getUid(), singleton(agencyRepInfo.getUidAndClientId()),
                singleton(clientUserInfo.getClientId()));
        return new RoleRelationInfo()
                .withOperatorInfo(agencyRepInfo)
                .withOwnerInfo(clientUserInfo);
    }

    private RoleRelationInfo agencyAndSubClient() {
        UserInfo agencyUserInfo = getOperatorInfo(RbacRole.AGENCY);
        UserInfo clientUserInfo = getOwnerInfo(RbacRole.CLIENT);
        agencyService.bindClients(agencyUserInfo.getClientId(), singleton(clientUserInfo.getClientId()));
        rbacService.bindClientsToAgency(agencyUserInfo.getUid(), singleton(agencyUserInfo.getUidAndClientId()),
                singleton(clientUserInfo.getClientId()));
        return new RoleRelationInfo()
                .withOperatorInfo(agencyUserInfo)
                .withOwnerInfo(clientUserInfo);
    }

    private RoleRelationInfo clientManagerAndClient() {
        UserInfo managerUserInfo = getOperatorInfo(RbacRole.MANAGER);
        UserInfo clientUserInfo = getOwnerInfo(RbacRole.CLIENT);
        clientService.bindClientsToManager(managerUserInfo.getUid(), singletonList(clientUserInfo.getClientId()));
        testClientRepository.setManagerToClient(clientUserInfo.getShard(), clientUserInfo.getClientId(),
                managerUserInfo.getUid());
        return new RoleRelationInfo()
                .withOperatorInfo(managerUserInfo)
                .withOwnerInfo(clientUserInfo);
    }

    private RoleRelationInfo clientAndSameClient() {
        UserInfo clientUserInfo = getOperatorInfo(RbacRole.CLIENT);
        Client client = clientService.getClient(clientUserInfo.getClientId());
        clientUserInfo.getClientInfo().setClient(client);
        return new RoleRelationInfo()
                .withOperatorInfo(clientUserInfo)
                .withOwnerInfo(clientUserInfo);
    }

    private RoleRelationInfo clientAndOtherClient() {
        return new RoleRelationInfo()
                .withOperatorInfo(getOperatorInfo(RbacRole.CLIENT))
                .withOwnerInfo(getOwnerInfo(RbacRole.CLIENT));
    }

    private UserInfo getOperatorInfo(RbacRole role) {
        return getClientUserInfo(role, DEFAULT_SHARD);
    }

    private UserInfo getOwnerInfo(RbacRole role) {
        return getClientUserInfo(role, ANOTHER_SHARD);
    }

    private UserInfo getClientUserInfo(RbacRole role, int shard) {
        ClientInfo clientInfo = clientSteps.createClient(new ClientInfo()
                .withShard(shard)
                .withClient(defaultClient(role)));
        User user = clientInfo.getChiefUserInfo().getUser();
        clientSteps.mockClientInExternalClients(clientInfo.getClient(), singletonList(user));
        userSteps.mockUserInExternalClients(user);
        return new UserInfo()
                .withClientInfo(clientInfo)
                .withUser(user);
    }

}
