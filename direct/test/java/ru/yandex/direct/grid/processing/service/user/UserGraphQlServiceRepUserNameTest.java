package ru.yandex.direct.grid.processing.service.user;

import java.util.Map;

import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UserGraphQlServiceRepUserNameTest {

    private static final String QUERY_TEMPLATE = ""
            + "query repUserName {\n" +
            "    repUserName\n" +
            "}";
    public static final String CLIENT_USER_FIO = "client_user fio";
    public static final String REPR_USER_FIO = "rep_user fio";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserService userService;

    @Autowired
    private Steps steps;

    /**
     * Клиент А
     */
    private ClientInfo clientInfo;
    /**
     * Представитель клиента Б
     */
    private User repUser;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createClient(
                new ClientInfo()
                        .withChiefUserInfo(new UserInfo().withUser(generateNewUser().withFio(CLIENT_USER_FIO))));

        var repUserInfo = new UserInfo()
                .withClientInfo(clientInfo)
                .withUser(
                        generateNewUser().withClientId(clientInfo.getClientId()).withFio(REPR_USER_FIO)
                );
        repUser = steps.userSteps().createUser(repUserInfo).getUser();
    }

    /**
     * Б логинится, в ulogin'е — Б
     */
    @Test
    public void getReprUserName_repInOperatorAndUlogin_success() {
        TestAuthHelper.setDirectAuthentication(repUser, repUser);
        GridGraphQLContext operatorContext = new GridGraphQLContext(repUser, repUser);

        ExecutionResult result = processor.processQuery(null, QUERY_TEMPLATE, null, operatorContext);

        Map<String, Object> actualData = result.getData();
        Map<String, Object> expectedData = map("repUserName", REPR_USER_FIO);
        assertThat(actualData).is(matchedBy(beanDiffer(expectedData)));
    }

    /**
     * А логинится, кидает ссылку Б, при открытии по ссылке в ulogin'е — А
     */
    @Test
    public void getReprUserName_repInOperatorAndClientInUlogin_success() {
        User clientUser = userService.getUser(clientInfo.getUid());
        //noinspection ConstantConditions
        TestAuthHelper.setDirectAuthentication(repUser, clientUser);
        GridGraphQLContext operatorContext = new GridGraphQLContext(repUser, clientUser);

        ExecutionResult result = processor.processQuery(null, QUERY_TEMPLATE, null, operatorContext);

        Map<String, Object> actualData = result.getData();
        Map<String, Object> expectedData = map("repUserName", REPR_USER_FIO);
        assertThat(actualData).is(matchedBy(beanDiffer(expectedData)));
    }

    /**
     * A логинится, в ulogin'е — A
     */
    @Test
    public void getReprUserName_clientInOperatorAndInUlogin_success() {
        User clientUser = userService.getUser(clientInfo.getUid());
        //noinspection ConstantConditions
        TestAuthHelper.setDirectAuthentication(clientUser, clientUser);
        GridGraphQLContext operatorContext = new GridGraphQLContext(clientUser, clientUser);

        ExecutionResult result = processor.processQuery(null, QUERY_TEMPLATE, null, operatorContext);

        Map<String, Object> actualData = result.getData();
        Map<String, Object> expectedData = map("repUserName", CLIENT_USER_FIO);
        assertThat(actualData).is(matchedBy(beanDiffer(expectedData)));
    }
}
