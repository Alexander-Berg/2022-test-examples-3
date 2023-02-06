package ru.yandex.direct.core.entity.campaign.service.validation;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.ClientPerm;
import ru.yandex.direct.rbac.RbacClientsRelations;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacSubrole;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.dbschema.ppc.Tables.AGENCY_MANAGERS;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENT_MANAGERS;
import static ru.yandex.direct.dbschema.ppc.tables.Clients.CLIENTS;

@CoreTest
@RunWith(Parameterized.class)
public class AddRestrictedCampaignPreValidationServiceRoleTest {
    private static boolean dataLoaded = false;
    private static final User COMMON_CLIENT = getCommonClient();
    private static final User ANOTHER_COMMON_CLIENT = getCommonClient();
    public static final User MANAGER = generateNewUser()
            .withRole(RbacRole.MANAGER);
    private static final User MANAGER_CLIENT = generateNewUser();
    private static final User AGENCY_1 = getAgency();
    private static final User AGENCY_1_CLIENT = generateNewUser();
    private static final User AGENCY_2 = getAgency();
    private static final User AGENCY_2_CLIENT = generateNewUser();

    private static final User MANAGER_TEAM_LEADER = generateNewUser()
            .withRole(RbacRole.MANAGER)
            .withSubRole(RbacSubrole.TEAMLEADER);

    private static final User MANAGER_TEAM_LEADERS_CLIENT = generateNewUser();

    private static final User SUB_MANAGER_OF_TEAM_LEADER = generateNewUser()
            .withRole(RbacRole.MANAGER);

    private static final User SUB_MANAGER_AGENCY = getAgency();

    private static final User SUB_MANAGER_AGENCY_CLIENT = generateNewUser();

    private static final User MAIN_AGENCY_REPRESENTATIVE = generateNewUser()
            .withRole(RbacRole.AGENCY)
            .withRepType(RbacRepType.MAIN);
    private static final User AGENCY_CLIENT_WITHOUT_PERMISSION = generateNewUser();
    private static final User AGENCY_ADMIN_REPRESENTATIVE = generateNewUser()
            .withRole(RbacRole.AGENCY)
            .withRepType(RbacRepType.CHIEF);

    private static final User AGENCY_LIMITED_REPRESENTATIVE = generateNewUser()
            .withRole(RbacRole.AGENCY)
            .withRepType(RbacRepType.LIMITED);

    private static final User AGENCY_LIMITED_REPRESENTATIVE_CLIENT = generateNewUser();

    private static final User FREELANCER_1 = generateNewUser();
    private static final User FREELANCER_1_CLIENT = generateNewUser();
    private static final User FREELANCER_2 = generateNewUser();
    private static final User FREELANCER_2_CLIENT = generateNewUser();
    private static final User MCC_CONTROL = generateNewUser();
    private static final User MCC_MANAGED_CLIENT = generateNewUser();
    private static final User MCC_MANAGED_SUBCLIENT = generateNewUser();
    private static final User MCC_MANAGED_SUPERSUBCLIENT = generateNewUser();
    public static final User SUPER = generateNewUser()
            .withRole(RbacRole.SUPER);
    private static final User SUPER_READER = generateNewUser()
            .withRole(RbacRole.SUPERREADER);
    public static final User SUPPORT = generateNewUser()
            .withRole(RbacRole.SUPPORT);
    public static final User PLACER = generateNewUser()
            .withRole(RbacRole.PLACER);
    private static final User MEDIA_PLANER = generateNewUser()
            .withRole(RbacRole.MEDIA);

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    public Steps steps;

    @Autowired
    public DslContextProvider dslContextProvider;

    @Autowired
    RbacClientsRelations rbacClientsRelations;

    @Autowired
    public AddRestrictedCampaignValidationService addRestrictedCampaignValidationService;

    @Autowired
    public TestClientRepository testClientRepository;

    @Parameterized.Parameter()
    public String name;

    @Parameterized.Parameter(1)
    public User operatorUser;

    @Parameterized.Parameter(2)
    public User clientUser;

    @Parameterized.Parameter(3)
    public Boolean hasError;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
        return List.of(
                toObjectArray("Common client can add new Campaign to him", COMMON_CLIENT, COMMON_CLIENT, false),
                toObjectArray("Common client can not add Campaign to another client", COMMON_CLIENT,
                        ANOTHER_COMMON_CLIENT, true),
                toObjectArray("Manager can create Campaign to his client", MANAGER, MANAGER_CLIENT, false),
                toObjectArray("Manager can create Campaign to client of his agency", MANAGER, AGENCY_1_CLIENT, false),
                toObjectArray("Manager can not create Campaign to client of other agency", MANAGER, AGENCY_2_CLIENT,
                        true),
                toObjectArray("Manager can not create Campaign to common client", MANAGER, COMMON_CLIENT, true),
                toObjectArray("Manager-team-leader can create Campaign to his client", MANAGER_TEAM_LEADER,
                        MANAGER_TEAM_LEADERS_CLIENT, false),
                toObjectArray("Manager-team-leader can create Campaign to client of his managers' agency",
                        MANAGER_TEAM_LEADER, SUB_MANAGER_AGENCY_CLIENT, false),
                toObjectArray("Manager-team-leader can not create Campaign to common client", MANAGER_TEAM_LEADER,
                        COMMON_CLIENT, true),
                toObjectArray("Manager's client can create Campaign to him", MANAGER_CLIENT, MANAGER_CLIENT, false),
                toObjectArray("Main agency's representative can create Campaign to client of agency",
                        MAIN_AGENCY_REPRESENTATIVE, SUB_MANAGER_AGENCY_CLIENT, false),
                toObjectArray("Main agency's representative can not create Campaign to client of other agency",
                        MAIN_AGENCY_REPRESENTATIVE, AGENCY_1_CLIENT,
                        true),
                toObjectArray("Agency's admin-representative can create Campaign to client of agency",
                        AGENCY_ADMIN_REPRESENTATIVE, SUB_MANAGER_AGENCY_CLIENT, false),
                toObjectArray("Agency's admin-representative can not create Campaign to client of other agency",
                        AGENCY_ADMIN_REPRESENTATIVE, AGENCY_1_CLIENT,
                        true),
                toObjectArray("Agency's representative can create Campaign to his client",
                        AGENCY_LIMITED_REPRESENTATIVE, AGENCY_LIMITED_REPRESENTATIVE_CLIENT, false),
                toObjectArray("Agency's representative can not create Campaign to not his client of his agency",
                        AGENCY_LIMITED_REPRESENTATIVE, SUB_MANAGER_AGENCY_CLIENT,
                        true),
                toObjectArray("Agency's client with permissions can create Campaign to him",
                        SUB_MANAGER_AGENCY_CLIENT,
                        SUB_MANAGER_AGENCY_CLIENT, false),
                toObjectArray("Agency's client without permissions can not create Campaign to him",
                        AGENCY_CLIENT_WITHOUT_PERMISSION, AGENCY_CLIENT_WITHOUT_PERMISSION, true),
                toObjectArray("Freelancer can create Campaign to his client", FREELANCER_1, FREELANCER_1_CLIENT, false),
                toObjectArray("Freelancer can create Campaign to him", FREELANCER_1, FREELANCER_1, false),
                toObjectArray("Freelancer can not create Campaign to client of another freelancer", FREELANCER_1,
                        FREELANCER_2_CLIENT, true),
                toObjectArray("Super can create Campaign to common client", SUPER, COMMON_CLIENT, false),
                toObjectArray("Super cannot create Campaign to him", SUPER, SUPER, true),
                toObjectArray("Super-reader cannot create Campaign to common client ", SUPER_READER, COMMON_CLIENT,
                        true),
                toObjectArray("Super-reader cannot create Campaign to him ", SUPER_READER, SUPER_READER, true),
                toObjectArray("Support can create Campaign to common client ", SUPPORT, COMMON_CLIENT, false),
                toObjectArray("Support can not create Campaign to him", SUPPORT, SUPPORT, true),
                toObjectArray("Placer can not create Campaign to common client", PLACER, COMMON_CLIENT, true),
                toObjectArray("Placer can not create Campaign to him", PLACER, PLACER, true),
                toObjectArray("Mediaplaner cannot create Campaign to common client ", MEDIA_PLANER, COMMON_CLIENT,
                        true),
                toObjectArray("Mediaplaner cannot create Campaign to him ", MEDIA_PLANER, MEDIA_PLANER, true),
                toObjectArray("Mcc control client can create Campaign to managed client ", MCC_CONTROL, MCC_MANAGED_CLIENT, false),
                toObjectArray("Mcc control client can create Campaign to managed supersubclient ", MCC_CONTROL, MCC_MANAGED_SUPERSUBCLIENT, false),
                toObjectArray("Mcc control client cannot create Campaign to managed subclient ", MCC_CONTROL, MCC_MANAGED_SUBCLIENT, true)
        );
    }

    private static User getCommonClient() {
        return generateNewUser();
    }

    private static User getAgency() {
        return generateNewUser()
                .withRole(RbacRole.AGENCY);
    }

    private static Integer defaultShard;

    @Before
    public void before() throws JsonProcessingException {
        if (!dataLoaded) {
            ClientInfo defaultClientAndUser = steps.clientSteps().createDefaultClient(COMMON_CLIENT);
            defaultShard = defaultClientAndUser.getShard();
            steps.clientSteps().createDefaultClient(ANOTHER_COMMON_CLIENT);
            steps.clientSteps().createDefaultClient(MANAGER);
            steps.clientSteps().createDefaultClient(MANAGER_CLIENT
                    .withManagerUserId(MANAGER.getUid()));

            addToClientManagers(defaultShard, MANAGER_CLIENT.getClientId().asLong(), MANAGER.getUid());

            steps.clientSteps().createDefaultClient(AGENCY_1);
            steps.clientSteps().createDefaultClient(AGENCY_1_CLIENT
                    .withAgencyUserId(AGENCY_1.getUid())
                    .withAgencyClientId(AGENCY_1.getClientId().asLong()));
            addManagerToAgency(defaultShard, AGENCY_1.getClientId().asLong(), MANAGER.getClientId().asLong(),
                    MANAGER.getUid());

            steps.clientSteps().createDefaultClient(AGENCY_2);
            steps.clientSteps().createDefaultClient(AGENCY_2_CLIENT
                    .withAgencyUserId(AGENCY_2.getUid())
                    .withAgencyClientId(AGENCY_2.getClientId().asLong()));

            ClientInfo managerTeamLeaderClientInfo =
                    steps.clientSteps().createDefaultClient(MANAGER_TEAM_LEADER);
            steps.clientSteps().createDefaultClient(MANAGER_TEAM_LEADERS_CLIENT
                    .withManagerUserId(MANAGER_TEAM_LEADER.getUid()));
            addToClientManagers(defaultShard, MANAGER_TEAM_LEADERS_CLIENT.getClientId().asLong(),
                    MANAGER_TEAM_LEADER.getUid());

            ClientInfo subManagerClientInfo =
                    steps.clientSteps().createDefaultClient(SUB_MANAGER_OF_TEAM_LEADER);
            testClientRepository.setManagerHierarchy(managerTeamLeaderClientInfo,
                    Collections.singletonList(subManagerClientInfo));
            ClientInfo subManagerAgencyClientInfo = steps.clientSteps().createDefaultClient(SUB_MANAGER_AGENCY);

            addManagerToAgency(defaultShard, SUB_MANAGER_AGENCY.getClientId().asLong(),
                    SUB_MANAGER_OF_TEAM_LEADER.getClientId().asLong(),
                    SUB_MANAGER_OF_TEAM_LEADER.getUid());

            ClientInfo subManagerAgencyClient = steps.clientSteps().createDefaultClient(SUB_MANAGER_AGENCY_CLIENT
                    .withAgencyUserId(SUB_MANAGER_AGENCY.getUid())
                    .withAgencyClientId(SUB_MANAGER_AGENCY.getClientId().asLong()));
            setPerms(subManagerAgencyClient, Set.of(ClientPerm.SUPER_SUBCLIENT));

            steps.userSteps().createUser(subManagerAgencyClientInfo, MAIN_AGENCY_REPRESENTATIVE, RbacRepType.MAIN);

            steps.userSteps().createUser(subManagerAgencyClientInfo, AGENCY_ADMIN_REPRESENTATIVE, RbacRepType.CHIEF);
            steps.clientSteps().createDefaultClient(AGENCY_CLIENT_WITHOUT_PERMISSION
                    .withAgencyUserId(SUB_MANAGER_AGENCY.getUid())
                    .withAgencyClientId(SUB_MANAGER_AGENCY.getClientId().asLong()));

            steps.userSteps().createUser(subManagerAgencyClientInfo, AGENCY_LIMITED_REPRESENTATIVE,
                    RbacRepType.LIMITED);

            steps.clientSteps().createDefaultClient(AGENCY_LIMITED_REPRESENTATIVE_CLIENT
                    .withAgencyUserId(AGENCY_LIMITED_REPRESENTATIVE.getUid())
                    .withAgencyClientId(AGENCY_LIMITED_REPRESENTATIVE.getClientId().asLong()));

            FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
            FREELANCER_1.withClientId(freelancerInfo.getClientId())
                    .withUid(freelancerInfo.getClientInfo().getUid());

            steps.clientSteps().createDefaultClient(FREELANCER_1_CLIENT);
            rbacClientsRelations.addFreelancerRelation(FREELANCER_1_CLIENT.getClientId(), FREELANCER_1.getClientId());

            steps.clientSteps().createDefaultClient(FREELANCER_2);
            steps.clientSteps().createDefaultClient(FREELANCER_2_CLIENT);
            rbacClientsRelations.addFreelancerRelation(FREELANCER_2_CLIENT.getClientId(), FREELANCER_2.getClientId());

            steps.clientSteps().createDefaultClient(MCC_CONTROL);
            steps.clientSteps().createDefaultClient(MCC_MANAGED_CLIENT);
            steps.clientMccSteps().createClientMccLink(MCC_CONTROL.getClientId(), MCC_MANAGED_CLIENT.getClientId());
            steps.clientSteps().createDefaultClient(MCC_MANAGED_SUBCLIENT
                    .withAgencyUserId(SUB_MANAGER_AGENCY.getUid())
                    .withAgencyClientId(SUB_MANAGER_AGENCY.getClientId().asLong()));
            steps.clientMccSteps().createClientMccLink(MCC_CONTROL.getClientId(), MCC_MANAGED_SUBCLIENT.getClientId());
            var mccManagedSupersubclient = steps.clientSteps().createDefaultClient(MCC_MANAGED_SUPERSUBCLIENT
                    .withAgencyUserId(SUB_MANAGER_AGENCY.getUid())
                    .withAgencyClientId(SUB_MANAGER_AGENCY.getClientId().asLong()));
            steps.clientMccSteps().createClientMccLink(MCC_CONTROL.getClientId(), MCC_MANAGED_SUPERSUBCLIENT.getClientId());
            setPerms(mccManagedSupersubclient, Set.of(ClientPerm.SUPER_SUBCLIENT));

            steps.clientSteps().createDefaultClient(SUPER);
            steps.clientSteps().createDefaultClient(SUPER_READER);
            steps.clientSteps().createDefaultClient(SUPPORT);
            steps.clientSteps().createDefaultClient(PLACER);
            steps.clientSteps().createDefaultClient(MEDIA_PLANER);
            dataLoaded = true;
        }
    }

    private static Object[] toObjectArray(String name, User clientUser, User operatorUser, Boolean hasErrors) {
        return new Object[]{name, clientUser, operatorUser, hasErrors};
    }

    private void addToClientManagers(int shard, Long clientId, Long managerUid) {
        dslContextProvider.ppc(shard)
                .insertInto(CLIENT_MANAGERS)
                .values(clientId, managerUid)
                .execute();
    }

    private void addManagerToAgency(int shard, Long agencyClientId, Long managerClientId, Long managerUid) {
        dslContextProvider.ppc(shard)
                .insertInto(AGENCY_MANAGERS)
                .values(agencyClientId, managerClientId, managerUid)
                .execute();
    }

    private void setPerms(ClientInfo clientInfo, Set<ClientPerm> perms) {
        dslContextProvider.ppc(clientInfo.getShard())
                .update(CLIENTS)
                .set(CLIENTS.PERMS, ClientPerm.format(perms))
                .where(CLIENTS.CLIENT_ID.eq(clientInfo.getClientId().asLong()))
                .execute();
    }


    @Test
    public void preValidate() {
        ValidationResult<? extends List<? extends BaseCampaign>, Defect> defectValidationResult =
                addRestrictedCampaignValidationService.preValidate(CampaignValidationContainer.create(defaultShard,
                        operatorUser.getUid(), clientUser.getClientId()), clientUser.getUid(), List.of());

        assertThat(defectValidationResult.hasAnyErrors()).isEqualTo(hasError);
    }

}
