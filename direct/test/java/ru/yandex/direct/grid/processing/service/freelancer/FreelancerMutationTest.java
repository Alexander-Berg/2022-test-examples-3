package ru.yandex.direct.grid.processing.service.freelancer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import graphql.ExecutionResult;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerProject;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerProjectStatus;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.info.FreelancerProjectInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.freelancer.mutation.GdAcceptFlServiceByFreelancerRequest;
import ru.yandex.direct.grid.processing.model.freelancer.mutation.GdCancelFlServiceByClientRequest;
import ru.yandex.direct.grid.processing.model.freelancer.mutation.GdCancelFlServiceByFreelancerRequest;
import ru.yandex.direct.grid.processing.model.freelancer.mutation.GdRequestFlServiceByClientRequest;
import ru.yandex.direct.grid.processing.model.freelancer.mutation.GdUpdateFreelancerCard;
import ru.yandex.direct.grid.processing.model.freelancer.mutation.GdUpdateFreelancerCardItem;
import ru.yandex.direct.grid.processing.model.freelancer.mutation.GdUpdateFreelancerContactsItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.sender.YandexSenderClient;
import ru.yandex.direct.sender.YandexSenderTemplateParams;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancerProject;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.service.freelancer.FreelancerDataService.CLIENT_CLIENT_ID;
import static ru.yandex.direct.grid.processing.service.freelancer.FreelancerDataService.CLIENT_EMAIL;
import static ru.yandex.direct.grid.processing.service.freelancer.FreelancerDataService.CLIENT_LOGIN;
import static ru.yandex.direct.grid.processing.service.freelancer.FreelancerDataService.CLIENT_NAME;
import static ru.yandex.direct.grid.processing.service.freelancer.FreelancerDataService.CLIENT_PHONE_NUMBER;
import static ru.yandex.direct.grid.processing.service.freelancer.FreelancerDataService.FEEDBACK_FORM;
import static ru.yandex.direct.grid.processing.service.freelancer.FreelancerDataService.FREELANCER_NAME;
import static ru.yandex.direct.grid.processing.service.freelancer.FreelancerDataService.LINK_TO_FREELANCER_CARD;
import static ru.yandex.direct.grid.processing.service.freelancer.FreelancerDataService.LINK_TO_INTERFACE;
import static ru.yandex.direct.grid.processing.service.freelancer.FreelancerDataService.REQUESTS;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FreelancerMutationTest {
    private static final String ACCEPT_BY_FREELANCER_MUTATION = "acceptFlServiceByFreelancer";
    private static final String CANCEL_BY_FREELANCER_MUTATION = "cancelFlServiceByFreelancer";
    private static final String CANCEL_BY_CLIENT_MUTATION = "cancelFlServiceByClient";
    private static final String REQUEST_BY_CLIENT_MUTATION = "requestFlServiceByClient";
    private static final String UPDATE_FREELANCER_CARD_MUTATION = "updateFreelancerCard";
    private static final String UPDATE_FREELANCER_STATUS_MUTATION = "updateFreelancerStatus";
    private static final String FL_SERVICE_MUTATION_TEMPLATE = "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    freelancerId\n"
            + "    clientId\n"
            + "    freelancerProject {\n"
            + "      status\n"
            + "      id\n"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final String FREELANCER_PAGE_PREFIX = "https://test-direct.yandex.ru/dna/freelancers/";
    private static final String FREELANCER_REQUESTS = "https://test-direct.yandex.ru/dna/customers?tab=requested";
    private static final String LINK_TO_FEEDBACK_FORM = "https://forms.yandex.ru/surveys/9938/";
    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)

    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;
    @Autowired
    private YandexSenderClient yandexSenderClient;
    @Autowired
    private EnvironmentType environmentType;
    private ClientInfo clientInfo;
    private FreelancerInfo freelancerInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        Mockito.clearInvocations(yandexSenderClient);
        TestAuthHelper.setDirectAuthentication(userService.getUser(clientInfo.getUid()));
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }


    @Test
    public void acceptFlServiceByFreelancer_success() {

        FreelancerProject project = defaultFreelancerProject(clientInfo.getClientId().asLong(),
                freelancerInfo.getClientId().asLong())
                .withStatus(FreelancerProjectStatus.NEW)
                .withUpdatedTime(LocalDateTime.now())
                .withCreatedTime(LocalDateTime.now());

        FreelancerProjectInfo projectInfo = new FreelancerProjectInfo()
                .withProject(project)
                .withFreelancerInfo(freelancerInfo)
                .withClientInfo(clientInfo);

        steps.freelancerSteps().createProject(projectInfo);

        GdAcceptFlServiceByFreelancerRequest request = new GdAcceptFlServiceByFreelancerRequest()
                .withClientId(clientInfo.getClientId().asLong())
                .withProjectId(projectInfo.getProjectId());
        String query = String.format(FL_SERVICE_MUTATION_TEMPLATE, ACCEPT_BY_FREELANCER_MUTATION,
                graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null,
                buildContext(userService.getUser(freelancerInfo.getClientInfo().getUid())));
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result.getErrors()).isEmpty();

        List<YandexSenderTemplateParams> clientFreelancerParams = verifyDoubleCallingAndGetArguments(softly);
        YandexSenderTemplateParams clientParams = clientFreelancerParams.get(0);
        YandexSenderTemplateParams freelancerParams = clientFreelancerParams.get(1);
        User client = getClient(clientInfo);
        User freelancer = getClient(freelancerInfo.getClientInfo());

        softly.assertThat(clientParams.getToEmail()).isEqualTo(client.getEmail());
        softly.assertThat(freelancerParams.getToEmail()).isEqualTo(freelancer.getEmail());

        String clientClientId = client.getClientId().toString();
        String freelancerName = freelancer.getFio();
        String freelancerPage = FREELANCER_PAGE_PREFIX + freelancerInfo.getClientInfo().getLogin();
        softly.assertThat(clientParams.getArgs()).isEqualTo(ImmutableMap.of(
                FREELANCER_NAME, freelancerName,
                LINK_TO_FREELANCER_CARD, freelancerPage,
                CLIENT_CLIENT_ID, clientClientId
        ));
        softly.assertThat(freelancerParams.getArgs()).isEqualTo(ImmutableMap.of(
                FREELANCER_NAME, freelancerName,
                CLIENT_LOGIN, client.getLogin(),
                LINK_TO_INTERFACE, freelancerPage,
                CLIENT_CLIENT_ID, clientClientId
        ));

        softly.assertAll();
    }

    @Test
    public void cancelFlServiceByFreelancer_success() {

        FreelancerProject project = defaultFreelancerProject(clientInfo.getClientId().asLong(),
                freelancerInfo.getClientId().asLong())
                .withStatus(FreelancerProjectStatus.INPROGRESS)
                .withUpdatedTime(LocalDateTime.now())
                .withCreatedTime(LocalDateTime.now());

        FreelancerProjectInfo projectInfo = new FreelancerProjectInfo()
                .withProject(project)
                .withFreelancerInfo(freelancerInfo)
                .withClientInfo(clientInfo);

        steps.freelancerSteps().createProject(projectInfo);

        GdCancelFlServiceByFreelancerRequest request = new GdCancelFlServiceByFreelancerRequest()
                .withClientId(clientInfo.getClientId().asLong())
                .withProjectId(projectInfo.getProjectId());
        String query = String.format(FL_SERVICE_MUTATION_TEMPLATE, CANCEL_BY_FREELANCER_MUTATION,
                graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null,
                buildContext(userService.getUser(freelancerInfo.getClientInfo().getUid())));
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result.getErrors()).isEmpty();

        verifyCallingAndArguments(softly);

        softly.assertAll();
    }

    @Test
    public void cancelFlServiceByClient_success() {

        FreelancerProject project = defaultFreelancerProject(clientInfo.getClientId().asLong(),
                freelancerInfo.getClientId().asLong())
                .withStatus(FreelancerProjectStatus.INPROGRESS)
                .withUpdatedTime(LocalDateTime.now())
                .withCreatedTime(LocalDateTime.now());

        FreelancerProjectInfo projectInfo = new FreelancerProjectInfo()
                .withProject(project)
                .withFreelancerInfo(freelancerInfo)
                .withClientInfo(clientInfo);

        steps.freelancerSteps().createProject(projectInfo);

        GdCancelFlServiceByClientRequest request = new GdCancelFlServiceByClientRequest()
                .withFreelancerId(freelancerInfo.getFreelancer().getFreelancerId())
                .withProjectId(projectInfo.getProjectId());
        String query = String.format(FL_SERVICE_MUTATION_TEMPLATE, CANCEL_BY_CLIENT_MUTATION,
                graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null,
                buildContext(userService.getUser(clientInfo.getUid())));
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result.getErrors()).isEmpty();

        verifyCallingAndArguments(softly);

        softly.assertAll();
    }

    private void verifyCallingAndArguments(SoftAssertions softly) {
        List<YandexSenderTemplateParams> clientFreelancerParams = verifyDoubleCallingAndGetArguments(softly);
        YandexSenderTemplateParams clientParams = clientFreelancerParams.get(0);
        YandexSenderTemplateParams freelancerParams = clientFreelancerParams.get(1);

        User freelancer = getClient(freelancerInfo.getClientInfo());
        User client = getClient(clientInfo);

        softly.assertThat(clientParams.getToEmail()).isEqualTo(client.getEmail());
        softly.assertThat(freelancerParams.getToEmail()).isEqualTo(freelancer.getEmail());

        String freelancerName = freelancer.getFio();
        String clientClientId = client.getClientId().toString();

        softly.assertThat(clientParams.getArgs()).isEqualTo(ImmutableMap.of(
                FREELANCER_NAME, freelancerName,
                FEEDBACK_FORM, LINK_TO_FEEDBACK_FORM,
                CLIENT_CLIENT_ID, clientClientId
        ));
        softly.assertThat(freelancerParams.getArgs()).isEqualTo(ImmutableMap.of(
                FREELANCER_NAME, freelancerName,
                CLIENT_LOGIN, client.getLogin(),
                FEEDBACK_FORM, LINK_TO_FEEDBACK_FORM,
                CLIENT_CLIENT_ID, clientClientId
        ));
    }

    @Test
    public void requestFlServiceByClient_success() {

        GdRequestFlServiceByClientRequest request = new GdRequestFlServiceByClientRequest()
                .withFreelancerId(freelancerInfo.getFreelancer().getFreelancerId());
        String query = String.format(FL_SERVICE_MUTATION_TEMPLATE, REQUEST_BY_CLIENT_MUTATION,
                graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null,
                buildContext(userService.getUser(clientInfo.getUid())));
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(result.getErrors()).isEmpty();

        List<YandexSenderTemplateParams> clientFreelancerParams =
                verifyDoubleCallingAndGetArguments(softly);
        YandexSenderTemplateParams clientParams = clientFreelancerParams.get(0);
        YandexSenderTemplateParams freelancerParams = clientFreelancerParams.get(1);

        User client = getClient(clientInfo);
        User freelancer = getClient(freelancerInfo.getClientInfo());

        softly.assertThat(clientParams.getToEmail()).isEqualTo(client.getEmail());
        softly.assertThat(freelancerParams.getToEmail()).isEqualTo(freelancer.getEmail());

        String clientName = client.getFio();
        String clientClientId = client.getClientId().toString();
        String freelancerName = freelancer.getFio();
        String freelancerPage = FREELANCER_PAGE_PREFIX + freelancer.getLogin();

        softly.assertThat(clientParams.getArgs()).isEqualTo(ImmutableMap.of(
                CLIENT_NAME, clientName,
                FREELANCER_NAME, freelancerName,
                LINK_TO_FREELANCER_CARD, freelancerPage,
                CLIENT_CLIENT_ID, clientClientId
        ));
        softly.assertThat(freelancerParams.getArgs()).isEqualTo(Map.of(
                FREELANCER_NAME, freelancerName,
                CLIENT_NAME, clientName,
                CLIENT_PHONE_NUMBER, client.getPhone(),
                CLIENT_EMAIL, client.getEmail(),
                REQUESTS, FREELANCER_REQUESTS,
                CLIENT_CLIENT_ID, clientClientId
        ));

        softly.assertAll();
    }

    private List<YandexSenderTemplateParams> verifyDoubleCallingAndGetArguments(SoftAssertions softly) {
        ArgumentCaptor<YandexSenderTemplateParams> paramsCaptor =
                ArgumentCaptor.forClass(YandexSenderTemplateParams.class);
        softly.assertThat(yandexSenderClient)
                .satisfies(ysc -> {
                    verify(yandexSenderClient, times(2))
                            .sendTemplate(paramsCaptor.capture());
                });
        return paramsCaptor.getAllValues();
    }

    private User getClient(ClientInfo clientInfo) {
        ClientId clientId = clientInfo.getClientId();
        String login = userService
                .getChiefsLoginsByClientIds(singleton(clientId))
                .get(clientId);
        return userService.getUserByLogin(login);
    }

    @Test
    public void updateFreelancerCard_success() {
        String template = "mutation {\n"
                + "  %s(input: %s) {\n"
                + "    freelancer {\n"
                + "      freelancerId\n"
                + "    }\n"
                + "  }\n"
                + "}";

        GdUpdateFreelancerContactsItem contactsItem = new GdUpdateFreelancerContactsItem()
                .withEmail("ya@ya.ru")
                .withPhone("+74951234567");
        GdUpdateFreelancerCardItem cardItem = new GdUpdateFreelancerCardItem()
                .withAvatarId(1L)
                .withBriefInfo("info")
                .withContacts(contactsItem);
        GdUpdateFreelancerCard request = new GdUpdateFreelancerCard().withCard(cardItem);
        String query = String.format(template, UPDATE_FREELANCER_CARD_MUTATION, graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null,
                buildContext(userService.getUser(freelancerInfo.getClientInfo().getUid())));
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    public void updateFreelancerStatus_success() {
        String template = "mutation {\n"
                + " %s(input:{status:BUSY}) {\n"
                + "    freelancer{\n"
                + "      status\n"
                + "    }\n"
                + "  }\n"
                + "}";

        String query = String.format(template, UPDATE_FREELANCER_STATUS_MUTATION);

        ExecutionResult result = processor.processQuery(null, query, null,
                buildContext(userService.getUser(freelancerInfo.getClientInfo().getUid())));
        assertThat(result.getErrors()).isEmpty();
    }
}
