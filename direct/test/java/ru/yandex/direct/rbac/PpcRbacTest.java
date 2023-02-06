package ru.yandex.direct.rbac;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.ClientsRole;
import ru.yandex.direct.dbschema.ppc.enums.ClientsSubrole;
import ru.yandex.direct.dbschema.ppc.enums.UsersRepType;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.model.ClientsRelationType;
import ru.yandex.direct.rbac.model.RbacAccessType;
import ru.yandex.direct.rbac.model.Representative;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.dbschema.ppc.Tables.AGENCY_MANAGERS;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENTS_RELATIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENT_MANAGERS;
import static ru.yandex.direct.dbschema.ppc.Tables.USERS;
import static ru.yandex.direct.dbschema.ppc.tables.Clients.CLIENTS;
import static ru.yandex.direct.rbac.model.RbacAccessType.NONE;
import static ru.yandex.direct.rbac.model.RbacAccessType.READONLY;
import static ru.yandex.direct.rbac.model.RbacAccessType.READ_WRITE;

@RunWith(SpringJUnit4ClassRunner.class)
@CoreTest
public class PpcRbacTest {

    @Autowired
    PpcRbac ppcRbac;

    @Autowired
    RbacService rbacService;

    @Autowired
    Steps steps;

    @Autowired
    DslContextProvider dslContextProvider;

    @Autowired
    RbacClientsRelations rbacClientsRelations;

    @Autowired
    TestClientRepository testClientRepository;

    private static ClientInfo superUser;
    private static ClientInfo superreader;
    private static ClientInfo teamleader;
    private static ClientInfo teamManager1;
    private static ClientInfo teamManager2;
    private static ClientInfo anotherManager;
    private static ClientInfo clientSelfServ;
    private static ClientInfo clientWithReps;
    private static UserInfo clientMainRep;
    private static UserInfo clientLimitedRep;
    private static ClientInfo agency;
    private static ClientInfo managersAgency;
    private static FreelancerInfo freelancer;
    private static FreelancerInfo freelancerServedByLimitedSupport;
    private static ClientInfo internalAdAdmin;
    private static ClientInfo internalAdSuperreader;
    private static ClientInfo internalAdProduct;
    private static ClientInfo internalAdManagerWithoutAccessToProduct;
    private static ClientInfo internalAdManagerWithReadonlyAccessToProduct;
    private static ClientInfo internalAdManagerWithFullAccessToProduct;
    private static ClientInfo clientFrServ;
    private static ClientInfo clientFrServ2;
    private static ClientInfo clientServedByLimitedSupportViaFreelancer;
    private static UserInfo limitedAgency;
    private static UserInfo limitedAgency2;
    private static ClientInfo agClient;
    private static ClientInfo agClientSuperSub;
    private static ClientInfo managersAgencyClient;
    private static ClientInfo support;
    private static ClientInfo limitedSupport;
    private static ClientInfo placer;
    private static ClientInfo media;
    private static ClientInfo servicedClient;
    private static ClientInfo limitedSupportClient;
    private static ClientInfo limitedSupportAgency;
    private static ClientInfo limitedSupportAgencyClient;
    private static ClientInfo controlMccClient;
    private static ClientInfo managedMccClient;
    private static ClientInfo notManagedMccClient;
    private static ClientInfo managedMccAgencySubClient;
    private static ClientInfo managedMccAgencySuperSubClient;
    private static ClientInfo notManagedMccAgencySubClient;
    private static ClientInfo notManagedMccAgencySuperSubClient;
    private static UserInfo controlMccClientReadonlyRep;

    /**
     * For getting AgencySubclients
     **/

    private static ClientInfo agencyForSubclients;
    private static UserInfo chiefAgencyForSubclients;
    private static UserInfo limitedAgencyForSubclients;
    private static UserInfo notChiefSubclient;
    private static ClientInfo clientForAgenceSubclients;
    private static ClientInfo clientForAgenceSubclientsShard2;
    private static ClientInfo clientForAgenceSubclientsLimited;
    private static ClientInfo notAgency;
    private static UserInfo agencyForSubclientsWithNoSubclients;

    // для каждого теста создавать всех пользователей - очень дорого,
    // поэтому пользователи - в статических полях
    private static boolean inited = false;

    @Before
    public void setUp() throws Exception {
        if (inited) {
            return;
        }
        superUser = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);

        superreader = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER);

        support = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPPORT);
        setRole(support, ClientsRole.support);

        limitedSupport = steps.clientSteps().createDefaultClientWithRole(RbacRole.LIMITED_SUPPORT);

        placer = steps.clientSteps().createDefaultClientWithRole(RbacRole.PLACER);

        media = steps.clientSteps().createDefaultClientWithRole(RbacRole.MEDIA);

        teamManager1 = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);

        servicedClient = steps.clientSteps().createDefaultClient();
        setManagerForClient(servicedClient, teamManager1);

        anotherManager = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);

        teamManager2 = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);

        teamleader = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);
        setSubrole(teamleader, ClientsSubrole.teamleader);
        testClientRepository.setManagerHierarchy(teamleader, Arrays.asList(teamManager1, teamManager2));

        clientSelfServ = steps.clientSteps().createDefaultClient();

        clientWithReps = steps.clientSteps().createDefaultClient();

        clientMainRep = steps.userSteps().createRepresentative(clientWithReps, RbacRepType.MAIN);
        clientLimitedRep = steps.userSteps().createRepresentative(clientWithReps, RbacRepType.LIMITED);

        freelancer = steps.freelancerSteps().addDefaultFreelancer();
        setRole(freelancer.getClientInfo(), ClientsRole.client);

        freelancerServedByLimitedSupport = steps.freelancerSteps().addDefaultFreelancer();
        setRole(freelancerServedByLimitedSupport.getClientInfo(), ClientsRole.client);
        rbacClientsRelations.addSupportRelation(freelancerServedByLimitedSupport.getClientId(), limitedSupport.getClientId());

        internalAdAdmin = steps.clientSteps().createDefaultClientWithRole(RbacRole.INTERNAL_AD_ADMIN);
        internalAdProduct = steps.internalAdProductSteps().createDefaultInternalAdProduct();

        internalAdManagerWithoutAccessToProduct = steps.clientSteps()
                .createDefaultClientWithRole(RbacRole.INTERNAL_AD_MANAGER);

        internalAdManagerWithReadonlyAccessToProduct = steps.clientSteps()
                .createDefaultClientWithRole(RbacRole.INTERNAL_AD_MANAGER);
        rbacClientsRelations.addInternalAdProductRelation(
                internalAdManagerWithReadonlyAccessToProduct.getClientId(),
                internalAdProduct.getClientId(),
                ClientsRelationType.INTERNAL_AD_READER);

        internalAdManagerWithFullAccessToProduct = steps.clientSteps()
                .createDefaultClientWithRole(RbacRole.INTERNAL_AD_MANAGER);
        rbacClientsRelations.addInternalAdProductRelation(
                internalAdManagerWithFullAccessToProduct.getClientId(),
                internalAdProduct.getClientId(),
                ClientsRelationType.INTERNAL_AD_PUBLISHER);

        internalAdSuperreader = steps.clientSteps().createDefaultClientWithRole(RbacRole.INTERNAL_AD_SUPERREADER);

        clientFrServ = steps.clientSteps().createDefaultClient();
        rbacClientsRelations.addFreelancerRelation(clientFrServ.getClientId(), freelancer.getClientId());

        clientFrServ2 = steps.clientSteps().createDefaultClient();
        rbacClientsRelations.addFreelancerRelation(clientFrServ2.getClientId(), freelancer.getClientId());

        clientServedByLimitedSupportViaFreelancer = steps.clientSteps().createDefaultClient();
        rbacClientsRelations.addFreelancerRelation(clientServedByLimitedSupportViaFreelancer.getClientId(),
                freelancerServedByLimitedSupport.getClientId());


        agency = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);

        limitedAgency = steps.userSteps().createRepresentative(agency, RbacRepType.LIMITED);
        limitedAgency2 = steps.userSteps().createRepresentative(agency, RbacRepType.LIMITED);

        agClient = steps.clientSteps().createDefaultClient();
        setAgency(agClient, limitedAgency2);

        agClientSuperSub = steps.clientSteps().createDefaultClient();
        setAgency(agClientSuperSub, limitedAgency2);
        setPerms(agClientSuperSub, Set.of(ClientPerm.SUPER_SUBCLIENT));

        managersAgency = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        setManagerForAgency(teamManager1, managersAgency);

        managersAgencyClient = steps.clientSteps().createDefaultClient();
        UserInfo managersAgencyUser = steps.userSteps().createRepresentative(managersAgency, RbacRepType.LIMITED);
        setAgency(managersAgencyClient, managersAgencyUser);

        limitedSupportClient = steps.clientSteps().createDefaultClient();
        rbacClientsRelations.addSupportRelation(limitedSupportClient.getClientId(), limitedSupport.getClientId());

        limitedSupportAgency = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        rbacClientsRelations.addSupportRelation(limitedSupportAgency.getClientId(), limitedSupport.getClientId());

        limitedSupportAgencyClient = steps.clientSteps().createDefaultClient();
        UserInfo limitedSupportAgencyUser = steps.userSteps()
                .createRepresentative(limitedSupportAgency, RbacRepType.LIMITED);
        setAgency(limitedSupportAgencyClient, limitedSupportAgencyUser);

        //For getting AgencySubclients

        agencyForSubclients = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);

        chiefAgencyForSubclients = agencyForSubclients.getChiefUserInfo();
        limitedAgencyForSubclients = steps.userSteps().createRepresentative(agencyForSubclients, RbacRepType.LIMITED);
        agencyForSubclientsWithNoSubclients = steps.userSteps()
                .createRepresentative(agencyForSubclients, RbacRepType.LIMITED);

        clientForAgenceSubclients = steps.clientSteps().createDefaultClient();
        clientForAgenceSubclientsLimited = steps.clientSteps().createDefaultClient();

        clientForAgenceSubclientsShard2 =
                steps.clientSteps().createDefaultClientAnotherShard();

        notAgency = steps.clientSteps().createDefaultClient();

        setAgency(clientForAgenceSubclients, chiefAgencyForSubclients);
        setAgency(clientForAgenceSubclientsShard2, chiefAgencyForSubclients);
        setAgency(clientForAgenceSubclientsLimited, limitedAgencyForSubclients);

        User user = new User().withClientId(clientForAgenceSubclients.getClientId());
        notChiefSubclient = steps.userSteps().createUser(user);

        controlMccClient = steps.clientSteps().createDefaultClient();
        controlMccClientReadonlyRep = steps.userSteps().createReadonlyRepresentative(controlMccClient);
        managedMccClient = steps.clientSteps().createDefaultClient();
        steps.clientSteps().addClientToMcc(controlMccClient, managedMccClient);
        notManagedMccClient = steps.clientSteps().createDefaultClient();
        managedMccAgencySubClient = steps.clientSteps().createDefaultClientUnderAgency(agencyForSubclients);
        steps.clientSteps().addClientToMcc(controlMccClient, managedMccAgencySubClient);
        managedMccAgencySuperSubClient = steps.clientSteps().createDefaultClientUnderAgency(agencyForSubclients);
        setPerms(managedMccAgencySuperSubClient, Set.of(ClientPerm.SUPER_SUBCLIENT));
        steps.clientSteps().addClientToMcc(controlMccClient, managedMccAgencySuperSubClient);
        notManagedMccAgencySubClient = steps.clientSteps().createDefaultClientUnderAgency(agencyForSubclients);
        notManagedMccAgencySuperSubClient = steps.clientSteps().createDefaultClientUnderAgency(agencyForSubclients);
        setPerms(notManagedMccAgencySuperSubClient, Set.of(ClientPerm.SUPER_SUBCLIENT));

        steps.clientSteps().addClientToMcc(freelancer.getClientInfo(), managedMccClient);

        rbacClientsRelations.addSupportRelation(controlMccClient.getClientId(), limitedSupport.getClientId());

        inited = true;
    }

    @Test
    public void getAgencySubclientsSeveralShardsTest() {
        List<Long> subclients = ppcRbac.getAgencySubclients(chiefAgencyForSubclients.getUid());
        List<Long> expected =
                Arrays.asList(clientForAgenceSubclients.getUid(), clientForAgenceSubclientsLimited.getUid(),
                        clientForAgenceSubclientsShard2.getUid(), notChiefSubclient.getUid(),
                        managedMccAgencySubClient.getUid(), notManagedMccAgencySubClient.getUid(),
                        managedMccAgencySuperSubClient.getUid(), notManagedMccAgencySuperSubClient.getUid());
        assertThat(subclients).containsExactlyInAnyOrder(expected.toArray(new Long[0]));
    }

    @Test
    public void getAccessibleAgencySubclientsSeveralShardsTest() {
        List<Long> subclientsForFind =
                Arrays.asList(clientForAgenceSubclients.getUid(), clientForAgenceSubclientsLimited.getUid(),
                        clientForAgenceSubclientsShard2.getUid(), notChiefSubclient.getUid());
        List<Long> expected = subclientsForFind;
        List<Long> subclients =
                ppcRbac.getAccessibleAgencySubclients(chiefAgencyForSubclients.getUid(), subclientsForFind);
        assertThat(subclients).containsExactlyInAnyOrder(expected.toArray(new Long[0]));
    }

    @Test
    public void getAgencySubclientsLimitedTest() {
        List<Long> subclients = ppcRbac.getAgencySubclients(limitedAgencyForSubclients.getUid());
        List<Long> expected = Arrays.asList(clientForAgenceSubclientsLimited.getUid());
        assertThat(subclients).containsExactlyInAnyOrder(expected.toArray(new Long[0]));
    }

    @Test
    public void getAccessibleAgencySubclientsLimitedTest() {
        List<Long> subclientsForFind =
                Arrays.asList(clientForAgenceSubclientsLimited.getUid(), clientForAgenceSubclients.getUid());
        List<Long> expected = singletonList(clientForAgenceSubclientsLimited.getUid());
        List<Long> subclients =
                ppcRbac.getAccessibleAgencySubclients(limitedAgencyForSubclients.getUid(), subclientsForFind);
        assertThat(subclients).containsExactlyInAnyOrder(expected.toArray(new Long[0]));
    }

    @Test
    public void getAgencySubclientsNotAgencyTest() {
        List<Long> subclients = ppcRbac.getAgencySubclients(notAgency.getUid());
        assertThat(subclients).isEmpty();
    }

    @Test
    public void getAccessibleAgencySubclientsNotAgencyTest() {
        List<Long> subclients = ppcRbac.getAccessibleAgencySubclients(notAgency.getUid(), null);
        assertThat(subclients).isEmpty();
    }

    @Test
    public void getAgencySubclientsWithNoSubclientsTest() {
        List<Long> subclients = ppcRbac.getAgencySubclients(agencyForSubclientsWithNoSubclients.getUid());
        assertThat(subclients).isEmpty();
    }

    @Test
    public void testGetUserPerminfo() {
        UserPerminfo ret = ppcRbac.getUserPerminfo(agClient.getUid()).get();
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(ret.clientId()).isEqualTo(agClient.getClientId()).as("clientid");
        soft.assertThat(ret.chiefUid()).isEqualTo(agClient.getUid()).as("chief");
        soft.assertThat(ret.role()).isEqualTo(RbacRole.CLIENT).as("role");
        soft.assertThat(ret.agencyClientId()).isEqualTo(limitedAgency2.getClientInfo().getClientId())
                .as("agencyClientId");
        soft.assertThat(ret.agencyUids()).hasSize(1).as("agencyUids");
        soft.assertThat(ret.agencyUids().iterator().next()).isEqualTo(limitedAgency2.getUid()).as("agencyUid");
        soft.assertThat(ret.managerUid()).isNull();
        soft.assertThat(ret.repType()).isSameAs(RbacRepType.CHIEF);
        soft.assertAll();
    }

    @Test
    public void testGetUsersPerminfoForMccControlClient() {
        var result = ppcRbac.getUsersPerminfo(
                Set.of(controlMccClient.getUid(), controlMccClientReadonlyRep.getUid())
        );

        assertSoftly(sa -> {
            assertThat(result.get(controlMccClient.getUid()).get().mccClientIds())
                    .containsExactlyInAnyOrder(
                            managedMccClient.getClientId().asLong(),
                            managedMccAgencySubClient.getClientId().asLong(),
                            managedMccAgencySuperSubClient.getClientId().asLong()

                    );
            assertThat(result.get(controlMccClientReadonlyRep.getUid()).get().mccClientIds())
                    .containsExactlyInAnyOrder(
                            managedMccClient.getClientId().asLong(),
                            managedMccAgencySubClient.getClientId().asLong(),
                            managedMccAgencySuperSubClient.getClientId().asLong()

                    );
        });
    }

    @Test
    public void testGetClientPerminfo() {
        ClientPerminfo ret = ppcRbac.getClientPerminfo(agClient.getClientId()).get();
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(ret.clientId()).isEqualTo(agClient.getClientId()).as("clientid");
        soft.assertThat(ret.chiefUid()).isEqualTo(agClient.getUid()).as("chief");
        soft.assertThat(ret.role()).isEqualTo(RbacRole.CLIENT).as("role");
        soft.assertThat(ret.agencyClientId()).isEqualTo(limitedAgency2.getClientInfo().getClientId())
                .as("agencyClientId");
        soft.assertThat(ret.agencyUids()).hasSize(1).as("agencyUids");
        soft.assertThat(ret.agencyUids().iterator().next()).isEqualTo(limitedAgency2.getUid()).as("agencyUid");
        soft.assertThat(ret.managerUid()).isNull();
        soft.assertAll();
    }

    @Test
    public void testCanRead() {
        SoftAssertions soft = new SoftAssertions();
        //super can read
        for (ClientInfo clientInfo : asList(
                superUser, superreader, agency, clientSelfServ, agClient, freelancer.getClientInfo(), clientFrServ
        )) {
            soft.assertThat(rbacService.canRead(superUser.getUid(), clientInfo.getUid())).isTrue();
        }
        //superreader can read
        for (ClientInfo clientInfo : asList(
                superUser, superreader, agency, clientSelfServ, agClient, freelancer.getClientInfo(), clientFrServ
        )) {
            soft.assertThat(rbacService.canRead(superreader.getUid(), clientInfo.getUid())).isTrue();
        }
        //mediaplanner can't read
        for (ClientInfo clientInfo : asList(
                superUser, superreader
        )) {
            soft.assertThat(rbacService.canRead(media.getUid(), clientInfo.getUid())).isFalse();
        }
        //mediaplanner can read
        for (ClientInfo clientInfo : asList(
                agency, clientSelfServ, agClient, clientFrServ, freelancer.getClientInfo()
        )) {
            soft.assertThat(rbacService.canRead(media.getUid(), clientInfo.getUid())).isTrue();
        }
        //support can't read
        for (ClientInfo clientInfo : asList(
                superUser, superreader
        )) {
            soft.assertThat(rbacService.canRead(support.getUid(), clientInfo.getUid())).isFalse();
        }
        //support can read
        for (ClientInfo clientInfo : asList(
                agency, clientSelfServ, agClient, freelancer.getClientInfo(), clientFrServ
        )) {
            soft.assertThat(rbacService.canRead(support.getUid(), clientInfo.getUid())).isTrue();
        }
        //limitedSupport can't read
        for (ClientInfo clientInfo : asList(
                superUser, superreader, agency, clientSelfServ, agClient, freelancer.getClientInfo(), clientFrServ
        )) {
            soft.assertThat(rbacService.canRead(limitedSupport.getUid(), clientInfo.getUid()))
                    .as("limitedSupport can't read " + clientInfo.getLogin())
                    .isFalse();
        }
        //limitedSupport can read
        for (ClientInfo clientInfo : asList(
                limitedSupportAgency, limitedSupportClient, limitedSupportAgencyClient
        )) {
            soft.assertThat(rbacService.canRead(limitedSupport.getUid(), clientInfo.getUid()))
                    .as("limitedSupport can read " + clientInfo.getLogin())
                    .isTrue();
        }
        //placer can't read
        for (ClientInfo clientInfo : asList(
                superUser, superreader
        )) {
            soft.assertThat(rbacService.canRead(placer.getUid(), clientInfo.getUid())).isFalse();
        }
        //placer can read
        for (ClientInfo clientInfo : asList(
                agency, clientSelfServ, agClient, clientFrServ, freelancer.getClientInfo()
        )) {
            soft.assertThat(rbacService.canRead(placer.getUid(), clientInfo.getUid())).isTrue();
        }

        soft.assertThat(rbacService.canRead(agency.getUid(), clientSelfServ.getUid())).isFalse();
        soft.assertThat(rbacService.canRead(agency.getUid(), agClient.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(limitedAgency.getUid(), agency.getUid())).isFalse();
        soft.assertThat(rbacService.canRead(limitedAgency.getUid(), agClient.getUid())).isFalse();
        soft.assertThat(rbacService.canRead(limitedAgency2.getUid(), agClient.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(freelancer.getClientInfo().getUid(), clientFrServ.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(freelancer.getClientInfo().getUid(), clientSelfServ.getUid())).isFalse();

        soft.assertThat(rbacService.canRead(teamManager1.getUid(), servicedClient.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(teamManager1.getUid(), managersAgency.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(teamManager1.getUid(), managersAgencyClient.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(teamManager1.getUid(), agency.getUid())).isFalse();
        soft.assertThat(rbacService.canRead(teamManager1.getUid(), support.getUid())).isFalse();
        soft.assertThat(rbacService.canRead(teamManager1.getUid(), clientFrServ.getUid())).isFalse();
        soft.assertThat(rbacService.canRead(teamManager2.getUid(), servicedClient.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(teamManager2.getUid(), agency.getUid())).isFalse();
        soft.assertThat(rbacService.canRead(teamManager2.getUid(), managersAgency.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(anotherManager.getUid(), servicedClient.getUid())).isFalse();
        soft.assertThat(rbacService.canRead(anotherManager.getUid(), agency.getUid())).isFalse();

        soft.assertThat(rbacService.canRead(internalAdAdmin.getUid(), internalAdProduct.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(internalAdManagerWithoutAccessToProduct.getUid(),
                internalAdProduct.getUid())).isFalse();
        soft.assertThat(rbacService.canRead(internalAdManagerWithReadonlyAccessToProduct.getUid(),
                internalAdProduct.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(internalAdManagerWithFullAccessToProduct.getUid(),
                internalAdProduct.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(internalAdSuperreader.getUid(), internalAdProduct.getUid())).isTrue();

        soft.assertThat(rbacService.canRead(clientWithReps.getUid(), clientWithReps.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(clientMainRep.getUid(), clientWithReps.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(clientLimitedRep.getUid(), clientWithReps.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(controlMccClient.getUid(), managedMccClient.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(controlMccClientReadonlyRep.getUid(), managedMccClient.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(controlMccClient.getUid(), managedMccAgencySubClient.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(controlMccClient.getUid(), managedMccAgencySuperSubClient.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(controlMccClient.getUid(), notManagedMccClient.getUid())).isFalse();
        soft.assertThat(rbacService.canRead(controlMccClient.getUid(), notManagedMccAgencySubClient.getUid())).isFalse();
        soft.assertThat(rbacService.canRead(controlMccClient.getUid(), notManagedMccAgencySuperSubClient.getUid())).isFalse();
        soft.assertThat(rbacService.canRead(controlMccClientReadonlyRep.getUid(), managedMccAgencySubClient.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(limitedSupport.getUid(), managedMccClient.getUid())).isTrue();
        soft.assertThat(rbacService.canRead(limitedSupport.getUid(), notManagedMccClient.getUid())).isFalse();

        soft.assertAll();
    }

    @Test
    public void testCanWrite() {
        SoftAssertions soft = new SoftAssertions();
        for (ClientInfo clientInfo : asList(
                superUser, superreader, agency, clientSelfServ, agClient, freelancer.getClientInfo(), clientFrServ
        )) {
            soft.assertThat(rbacService.canWrite(superUser.getUid(), clientInfo.getUid())).isTrue()
                    .as("super can write " + clientInfo.getLogin());
        }
        for (ClientInfo clientInfo : asList(
                superUser, agency, clientSelfServ, agClient, freelancer.getClientInfo(), clientFrServ
        )) {
            soft.assertThat(rbacService.canWrite(superreader.getUid(), clientInfo.getUid())).isFalse()
                    .as("superreader can't write " + clientInfo.getLogin());
        }
        for (ClientInfo clientInfo : asList(
                superUser, agency, clientSelfServ, agClient, freelancer.getClientInfo(), clientFrServ,
                limitedSupportAgency, limitedSupportClient, limitedSupportAgencyClient
        )) {
            soft.assertThat(rbacService.canWrite(limitedSupport.getUid(), clientInfo.getUid()))
                    .as("limitedSupport can't write " + clientInfo.getLogin())
                    .isFalse();
        }
        soft.assertThat(rbacService.canWrite(agency.getUid(), clientSelfServ.getUid())).isFalse();
        soft.assertThat(rbacService.canWrite(agency.getUid(), agClient.getUid())).isTrue();
        soft.assertThat(rbacService.canWrite(limitedAgency.getUid(), agClient.getUid())).isFalse();
        soft.assertThat(rbacService.canWrite(limitedAgency2.getUid(), agClient.getUid())).isTrue();
        soft.assertThat(rbacService.canWrite(freelancer.getClientInfo().getUid(), clientFrServ.getUid())).isTrue();
        soft.assertThat(rbacService.canWrite(freelancer.getClientInfo().getUid(), clientSelfServ.getUid()))
                .isFalse();

        soft.assertThat(rbacService.canWrite(clientSelfServ.getUid(), clientSelfServ.getUid())).isTrue();
        soft.assertThat(rbacService.canWrite(agClient.getUid(), agClient.getUid())).isFalse();
        soft.assertThat(rbacService.canWrite(agClientSuperSub.getUid(), agClientSuperSub.getUid())).isTrue();
        soft.assertThat(rbacService.canWrite(limitedAgency.getUid(), agClient.getUid())).isFalse();
        soft.assertThat(rbacService.canWrite(limitedAgency.getUid(), agency.getUid()))
                .as("limited is owner for chief")
                .isFalse();
        soft.assertThat(rbacService.canWrite(limitedAgency.getUid(), limitedAgency2.getUid()))
                .as("limited is owner for other limited")
                .isFalse();

        soft.assertThat(rbacService.canWrite(clientWithReps.getUid(), clientWithReps.getUid()))
                .as("chief can write")
                .isTrue();
        soft.assertThat(rbacService.canWrite(clientMainRep.getUid(), clientWithReps.getUid()))
                .as("main rep can write")
                .isTrue();
        soft.assertThat(rbacService.canWrite(clientLimitedRep.getUid(), clientWithReps.getUid()))
                .as("limited rep can't write")
                .isFalse();
        soft.assertThat(rbacService.canWrite(controlMccClient.getUid(), managedMccClient.getUid()))
                .as("control MCC client can write")
                .isTrue();
        soft.assertThat(rbacService.canWrite(controlMccClientReadonlyRep.getUid(), managedMccClient.getUid()))
                .as("control MCC client readonly rep can't write")
                .isFalse();
        soft.assertThat(rbacService.canWrite(controlMccClient.getUid(), managedMccAgencySubClient.getUid()))
                .as("control MCC client can't write on agency subclient")
                .isFalse();
        soft.assertThat(rbacService.canWrite(controlMccClient.getUid(), managedMccAgencySuperSubClient.getUid()))
                .as("control MCC client can write on agency super subclient")
                .isTrue();
        soft.assertThat(rbacService.canWrite(controlMccClientReadonlyRep.getUid(), managedMccAgencySubClient.getUid()))
                .as("control MCC client readonly rep can't write on agency subclient")
                .isFalse();
        soft.assertThat(rbacService.canWrite(controlMccClient.getUid(), notManagedMccClient.getUid()))
                .as("control MCC client can't write on not managed client")
                .isFalse();
        soft.assertThat(rbacService.canWrite(controlMccClient.getUid(), notManagedMccAgencySubClient.getUid()))
                .as("control MCC client can't write on not managed agency subclient")
                .isFalse();
        soft.assertThat(rbacService.canWrite(controlMccClient.getUid(), notManagedMccAgencySuperSubClient.getUid()))
                .as("control MCC client can't write on not managed agency super subclient")
                .isFalse();
        soft.assertThat(rbacService.canWrite(limitedSupport.getUid(), managedMccClient.getUid()))
                .as("limited support can't write on managed MCC client")
                .isFalse();
        soft.assertAll();
    }

    @Test
    public void testIsOwner() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(rbacService.isOwner(clientSelfServ.getUid(), clientSelfServ.getUid())).isTrue();
        // Ограниченный представитель не владеет клиентом другого представителя
        soft.assertThat(rbacService.isOwner(limitedAgency.getUid(), agClient.getUid())).isFalse();
        // Ограниченный представитель не владеет другими представителями
        soft.assertThat(rbacService.isOwner(limitedAgency.getUid(), agency.getUid()))
                .as("limited is owner for chief")
                .isFalse();
        soft.assertThat(rbacService.isOwner(limitedAgency.getUid(), limitedAgency2.getUid()))
                .as("limited is owner for other limited")
                .isFalse();
        soft.assertThat(rbacService.isOwner(freelancer.getClientInfo().getUid(), clientFrServ.getUid())).isTrue();
        soft.assertThat(rbacService.isOwner(freelancer.getClientInfo().getUid(), clientSelfServ.getUid())).isFalse();
        soft.assertThat(rbacService.isOwner(controlMccClient.getUid(), managedMccClient.getUid())).isTrue();
        soft.assertThat(rbacService.isOwner(controlMccClientReadonlyRep.getUid(), managedMccClient.getUid())).isTrue();
        soft.assertThat(rbacService.isOwner(controlMccClient.getUid(), managedMccAgencySubClient.getUid())).isTrue();
        soft.assertThat(rbacService.isOwner(controlMccClientReadonlyRep.getUid(), managedMccAgencySubClient.getUid())).isTrue();
        soft.assertThat(rbacService.isOwner(limitedSupport.getUid(), managedMccClient.getUid())).isTrue();
        soft.assertThat(rbacService.isOwner(limitedSupport.getUid(), notManagedMccClient.getUid())).isFalse();
        soft.assertAll();
    }

    @Test
    public void testGetAccessType_OperatorSuper() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(getAccessType(superUser.getUid(), clientSelfServ.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(superUser.getUid(), agency.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(superUser.getUid(), teamManager1.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(superUser.getUid(), superreader.getUid())).isEqualTo(READ_WRITE);
        soft.assertAll();
    }

    @Test
    public void testGetAccessType_OperatorSuperreader() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(getAccessType(superreader.getUid(), clientSelfServ.getUid())).isEqualTo(READONLY);
        soft.assertThat(getAccessType(superreader.getUid(), agency.getUid())).isEqualTo(READONLY);
        soft.assertThat(getAccessType(superreader.getUid(), teamManager1.getUid())).isEqualTo(READONLY);
        soft.assertThat(getAccessType(superreader.getUid(), superUser.getUid())).isEqualTo(READONLY);
        soft.assertAll();
    }

    @Test
    public void testGetAccessType_OperatorSupport() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(getAccessType(support.getUid(), clientSelfServ.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(support.getUid(), agency.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(support.getUid(), teamManager1.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(support.getUid(), superUser.getUid())).isEqualTo(NONE);
        soft.assertAll();
    }

    @Test
    public void testGetAccessType_OperatorPlacer() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(getAccessType(placer.getUid(), clientSelfServ.getUid())).isEqualTo(READONLY);
        soft.assertThat(getAccessType(placer.getUid(), agency.getUid())).isEqualTo(READONLY);
        soft.assertThat(getAccessType(placer.getUid(), teamManager1.getUid())).isEqualTo(NONE);
        soft.assertThat(getAccessType(placer.getUid(), superUser.getUid())).isEqualTo(NONE);
        soft.assertAll();
    }

    @Test
    public void testGetAccessType_OperatorMedia() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(getAccessType(media.getUid(), clientSelfServ.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(media.getUid(), agency.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(media.getUid(), teamManager1.getUid())).isEqualTo(NONE);
        soft.assertThat(getAccessType(media.getUid(), superUser.getUid())).isEqualTo(NONE);
        soft.assertAll();
    }

    @Test
    public void testGetAccessType_OperatorManager() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(getAccessType(teamManager1.getUid(), clientSelfServ.getUid())).isEqualTo(NONE);
        soft.assertThat(getAccessType(teamManager1.getUid(), agency.getUid())).isEqualTo(NONE);
        soft.assertThat(getAccessType(teamManager1.getUid(), teamleader.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(teamManager1.getUid(), teamManager2.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(teamleader.getUid(), teamManager1.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(teamManager1.getUid(), servicedClient.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(teamManager2.getUid(), servicedClient.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(teamleader.getUid(), servicedClient.getUid())).isEqualTo(READ_WRITE);
        soft.assertAll();
    }

    @Test
    public void testGetAccessType_OperatorAgency() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(getAccessType(chiefAgencyForSubclients.getUid(), clientForAgenceSubclients.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(limitedAgencyForSubclients.getUid(), clientForAgenceSubclients.getUid())).isEqualTo(NONE);
        soft.assertThat(getAccessType(limitedAgencyForSubclients.getUid(), clientForAgenceSubclientsLimited.getUid())).isEqualTo(READ_WRITE);
        soft.assertAll();
    }

    @Test
    public void testGetAccessType_OperatorClient() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(getAccessType(clientFrServ.getUid(), clientFrServ2.getUid())).isEqualTo(NONE);
        soft.assertAll();
    }

    @Test
    public void testGetAccessType_OperatorFreelancer() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(getAccessType(freelancer.getClientInfo().getUid(), clientFrServ.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(freelancer.getClientInfo().getUid(), clientSelfServ.getUid())).isEqualTo(NONE);
        soft.assertThat(getAccessType(freelancer.getClientInfo().getUid(), managedMccClient.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(freelancer.getClientInfo().getUid(), notManagedMccClient.getUid())).isEqualTo(NONE);
        soft.assertAll();
    }

    @Test
    public void testGetAccessType_OperatorMCC() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(getAccessType(controlMccClient.getUid(), managedMccClient.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(controlMccClientReadonlyRep.getUid(), managedMccClient.getUid())).isEqualTo(READONLY);
        soft.assertThat(getAccessType(controlMccClient.getUid(), managedMccAgencySubClient.getUid())).isEqualTo(READONLY);
        soft.assertThat(getAccessType(controlMccClientReadonlyRep.getUid(), managedMccAgencySubClient.getUid())).isEqualTo(READONLY);
        soft.assertThat(getAccessType(controlMccClient.getUid(), managedMccAgencySuperSubClient.getUid())).isEqualTo(READ_WRITE);
        soft.assertThat(getAccessType(controlMccClientReadonlyRep.getUid(), managedMccAgencySuperSubClient.getUid())).isEqualTo(READONLY);
        Stream.of(controlMccClient.getUid(), controlMccClientReadonlyRep.getUid()).forEach(
                uid -> {
                    soft.assertThat(getAccessType(uid, notManagedMccClient.getUid())).isEqualTo(NONE);
                    soft.assertThat(getAccessType(uid, notManagedMccAgencySubClient.getUid())).isEqualTo(NONE);
                    soft.assertThat(getAccessType(uid, notManagedMccAgencySuperSubClient.getUid())).isEqualTo(NONE);
                }
        );
        soft.assertAll();
    }

    @Test
    public void testGetAccessType_OperatorLimitedSupport() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(getAccessType(limitedSupport.getUid(), limitedSupportClient.getUid())).isEqualTo(READONLY);
        soft.assertThat(getAccessType(limitedSupport.getUid(), clientServedByLimitedSupportViaFreelancer.getUid())).isEqualTo(READONLY);
        soft.assertThat(getAccessType(limitedSupport.getUid(), limitedSupportAgency.getUid())).isEqualTo(READONLY);
        soft.assertThat(getAccessType(limitedSupport.getUid(), limitedSupportAgencyClient.getUid())).isEqualTo(READONLY);
        soft.assertThat(getAccessType(limitedSupport.getUid(), clientFrServ.getUid())).isEqualTo(NONE);
        soft.assertThat(getAccessType(limitedSupport.getUid(), clientSelfServ.getUid())).isEqualTo(NONE);
        soft.assertThat(getAccessType(limitedSupport.getUid(), agency.getUid())).isEqualTo(NONE);
        soft.assertThat(getAccessType(limitedSupport.getUid(), teamManager1.getUid())).isEqualTo(NONE);
        soft.assertThat(getAccessType(limitedSupport.getUid(), superUser.getUid())).isEqualTo(NONE);
        soft.assertThat(getAccessType(limitedSupport.getUid(), controlMccClient.getUid())).isEqualTo(READONLY);
        soft.assertThat(getAccessType(limitedSupport.getUid(), managedMccClient.getUid())).isEqualTo(READONLY);
        soft.assertThat(getAccessType(limitedSupport.getUid(), notManagedMccClient.getUid())).isEqualTo(NONE);
        soft.assertAll();
    }

    @Test
    public void testGetAccessType_OperatorInternalAdAdmin() {
        assertThat(getAccessType(internalAdAdmin.getUid(), internalAdProduct.getUid())).isEqualTo(READ_WRITE);
    }

    @Test
    public void testGetAccessType_OperatorInternalAdManager_NoAccess() {
        assertThat(getAccessType(internalAdManagerWithoutAccessToProduct.getUid(), internalAdProduct.getUid()))
                .isEqualTo(NONE);
    }

    @Test
    public void testGetAccessType_OperatorInternalAdManager_ReadonlyAccess() {
        assertThat(getAccessType(internalAdManagerWithReadonlyAccessToProduct.getUid(), internalAdProduct.getUid()))
                .isEqualTo(READONLY);
    }

    @Test
    public void testGetAccessType_OperatorInternalAdManager_FullAccess() {
        assertThat(getAccessType(internalAdManagerWithFullAccessToProduct.getUid(), internalAdProduct.getUid()))
                .isEqualTo(READ_WRITE);
    }

    @Test
    public void testGetAccessType_OperatorInternalAdSuperreader() {
        assertThat(getAccessType(internalAdSuperreader.getUid(), internalAdProduct.getUid())).isEqualTo(READONLY);
    }

    @Test
    public void getRelatedClientsChiefs_success() {
        Long[] expectedClientsChiefs = {clientFrServ.getUid(), clientFrServ2.getUid(), managedMccClient.getUid()};
        List<Long> relatedClientsChiefs = rbacService.getRelatedClientsChiefs(freelancer.getClientId());
        assertThat(relatedClientsChiefs).containsExactlyInAnyOrder(expectedClientsChiefs);
    }

    @Test
    public void getAccessibleRelatedClients_success() {
        //подготавливаем данные
        ClientInfo freelancerClientInfo = freelancer.getClientInfo();
        ClientInfo[] allClients = {superUser, superreader, teamleader, teamManager1, teamManager2, clientSelfServ,
                agency,
                clientFrServ, clientFrServ2, agClient, agencyForSubclients, clientForAgenceSubclients,
                clientForAgenceSubclientsShard2, clientForAgenceSubclientsLimited, notAgency, freelancerClientInfo};
        List<Long> allUids = StreamEx.of(allClients)
                .map(ClientInfo::getUid)
                .toList();
        long freelancerUid = freelancerClientInfo.getUid();

        //ожидаемый результат
        Long[] expectedClientsChiefs = {clientFrServ.getUid(), clientFrServ2.getUid(), freelancerUid};

        //проверяем метод
        List<Long> relatedClientsChiefs = rbacService.getAccessibleRelatedClients(freelancerUid, allUids);
        assertThat(relatedClientsChiefs).containsExactlyInAnyOrder(expectedClientsChiefs);
    }

    @Test
    public void massGetClientRepresentatives() {
        Collection<Representative> representatives =
                ppcRbac.massGetClientRepresentatives(singleton(agency.getClientId()));
        assertThat(representatives).containsExactlyInAnyOrder(
                rep(agency), rep(limitedAgency), rep(limitedAgency2)
        );
    }

    @Test
    public void getSupervisorsSubordinates_getCorrectUid_returnOne() {
        Map<Long, List<Long>> result = ppcRbac.getSupervisorsSubordinates(Collections.singleton(teamleader.getUid()));
        assertThat(result).containsKeys(teamleader.getUid());
        assertThat(result.get(teamleader.getUid())).isEqualTo(Arrays.asList(teamManager1.getUid(),
                teamManager2.getUid()));
    }

    @Test
    public void getSupervisorsSubordinates_getNonexistentUid_returnEmptyList() {
        assertThat(ppcRbac.getSupervisorsSubordinates(Collections.singleton(-1L)))
                .isEqualTo(singletonMap(-1L, emptyList()));
    }

    @QueryWithoutIndex("Используется только в тестах")
    @Test
    public void addRelation_fillRelationId() {
        // Проверяем, что никто не заполнил CLIENTS_RELATION без RELATION_ID
        // Тест может шуметь, но проверяет широкий спектр сценариев
        // После смены PK в DIRECT-102740 тест можно убрать
        int absentRelationIdCount = dslContextProvider.ppc(clientFrServ.getShard())
                .selectCount()
                .from(CLIENTS_RELATIONS)
                .where(CLIENTS_RELATIONS.RELATION_ID.eq(0L))
                .fetchOne().value1();
        assertThat(absentRelationIdCount)
                .as("количество записей clients_relations с relation_id=0")
                .isZero();
    }

    private Representative rep(ClientInfo clientInfo) {
        return Representative.create(
                clientInfo.getClientId(),
                clientInfo.getUid(),
                RbacRepType.CHIEF
        );
    }

    private Representative rep(UserInfo userInfo) {
        return Representative.create(
                userInfo.getClientInfo().getClientId(),
                userInfo.getUid(),
                userInfo.getUser().getRepType()
        );
    }

    private void setAgency(ClientInfo client, UserInfo agency) {
        dslContextProvider.ppc(client.getShard())
                .update(CLIENTS)
                .set(CLIENTS.AGENCY_CLIENT_ID, agency.getClientInfo().getClientId().asLong())
                .set(CLIENTS.AGENCY_UID, agency.getUid())
                .where(CLIENTS.CLIENT_ID.eq(client.getClientId().asLong()))
                .execute();
    }

    private void setManagerForClient(ClientInfo client, ClientInfo manager) {
        dslContextProvider.ppc(client.getShard())
                .update(CLIENTS)
                .set(CLIENTS.PRIMARY_MANAGER_UID, manager.getUid())
                .where(CLIENTS.CLIENT_ID.eq(client.getClientId().asLong()))
                .execute();
        dslContextProvider.ppc(client.getShard())
                .insertInto(CLIENT_MANAGERS)
                .values(client.getClientId().asLong(), manager.getUid())
                .execute();
    }

    private void setManagerForAgency(ClientInfo manager, ClientInfo agency) {
        dslContextProvider.ppc(agency.getShard())
                .insertInto(AGENCY_MANAGERS)
                .values(agency.getClientId().asLong(), manager.getClientId().asLong(), manager.getUid())
                .execute();
    }

    private void setRepType(UserInfo user, UsersRepType repType) {
        dslContextProvider.ppc(user.getShard())
                .update(USERS)
                .set(USERS.REP_TYPE, repType)
                .where(USERS.UID.eq(user.getUid()))
                .execute();
    }

    private void setRole(ClientInfo client, ClientsRole role) {
        dslContextProvider.ppc(client.getShard())
                .update(CLIENTS)
                .set(CLIENTS.ROLE, role)
                .where(CLIENTS.CLIENT_ID.eq(client.getClientId().asLong()))
                .execute();
    }

    private void setSubrole(ClientInfo client, ClientsSubrole subrole) {
        dslContextProvider.ppc(client.getShard())
                .update(CLIENTS)
                .set(CLIENTS.SUBROLE, subrole)
                .where(CLIENTS.CLIENT_ID.eq(client.getClientId().asLong()))
                .execute();
    }

    private void setPerms(ClientInfo client, Set<ClientPerm> perms) {
        dslContextProvider.ppc(client.getShard())
                .update(CLIENTS)
                .set(CLIENTS.PERMS, ClientPerm.format(perms))
                .where(CLIENTS.CLIENT_ID.eq(client.getClientId().asLong()))
                .execute();
    }

    private RbacAccessType getAccessType(Long operatorUid, Long clientUid) {
        return ppcRbac.getAccessTypes(operatorUid, singletonList(clientUid)).get(clientUid);
    }
}
