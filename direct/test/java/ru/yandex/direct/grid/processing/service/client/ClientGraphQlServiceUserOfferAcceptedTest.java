package ru.yandex.direct.grid.processing.service.client;

import java.util.Map;

import graphql.ExecutionResult;
import org.assertj.core.api.SoftAssertions;
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
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.feature.FeatureName.MODERATION_OFFER_ENABLED_FOR_DNA;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.checkErrors;

/**
 * Тесты на значения поля userOfferEnabled для текущего представителя клиента.
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientGraphQlServiceUserOfferAcceptedTest {
    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "    isOfferAccepted\n"
            + "}\n";
    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private GridContextProvider gridContextProvider;
    @Autowired
    private UserService userService;
    @Autowired
    private Steps steps;

    @Test
    public void isOfferAccepted_whenFeatureEnabledAndOnlyOneRepresentative() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), MODERATION_OFFER_ENABLED_FOR_DNA, true);

        Map<String, Object> data = sendRequest(clientInfo.getUid());

        Boolean isOfferAccepted = GraphQLUtils.getDataValue(data, "isOfferAccepted");
        assertThat(isOfferAccepted).isTrue();
    }

    @Test
    public void isOfferAccepted_whenFeatureEnabledButSeveralRepresentatives() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.userSteps().createRepresentative(clientInfo, RbacRepType.MAIN);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), MODERATION_OFFER_ENABLED_FOR_DNA, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), MODERATION_OFFER_ENABLED_FOR_DNA, true);

        Map<String, Object> data = sendRequest(clientInfo.getUid());

        Boolean isOfferAccepted = GraphQLUtils.getDataValue(data, "isOfferAccepted");
        assertThat(isOfferAccepted).isFalse();
    }

    @Test
    public void isOfferAccepted_whenFeatureEnabledButDeletedRepresentative() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.userSteps().createDeletedUser(clientInfo);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), MODERATION_OFFER_ENABLED_FOR_DNA, true);
        Map<String, Object> data = sendRequest(clientInfo.getUid());
        Boolean isOfferAccepted = GraphQLUtils.getDataValue(data, "isOfferAccepted");
        assertThat(isOfferAccepted).isFalse();
    }

    @Test
    public void isOfferAccepted_whenSeveralRepresentativesButOfferAlreadyAssepted() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        UserInfo secondRepInfo = steps.userSteps().createUser(clientInfo, RbacRepType.MAIN);
        steps.userSteps().setUserProperty(secondRepInfo, User.IS_OFFER_ACCEPTED, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), MODERATION_OFFER_ENABLED_FOR_DNA, true);

        Map<String, Object> firstRepData = sendRequest(clientInfo.getUid());
        Map<String, Object> secondRepData = sendRequest(secondRepInfo.getUid());

        Boolean firstAccepted = GraphQLUtils.getDataValue(firstRepData, "isOfferAccepted");
        Boolean secondAccepted = GraphQLUtils.getDataValue(secondRepData, "isOfferAccepted");
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(firstAccepted).as("firstAccepted").isFalse();
            soft.assertThat(secondAccepted).as("secondAccepted").isTrue();
        });
    }

    @Test
    public void isOfferAccepted_whenFeatureDisabledAndOnlyOneRepresentative() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), MODERATION_OFFER_ENABLED_FOR_DNA, false);

        Map<String, Object> data = sendRequest(clientInfo.getUid());

        Boolean isOfferAccepted = GraphQLUtils.getDataValue(data, "isOfferAccepted");
        assertThat(isOfferAccepted).isFalse();
    }

    @Test
    public void isOfferAccepted_whenFeatureEnabledButAgencySubclient() {
        ClientInfo agency = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        ClientInfo subclient = steps.clientSteps().createDefaultClientUnderAgency(agency);

        steps.featureSteps().addClientFeature(subclient.getClientId(), MODERATION_OFFER_ENABLED_FOR_DNA, true);
        Map<String, Object> data = sendRequest(subclient.getUid());

        Boolean isOfferAccepted = GraphQLUtils.getDataValue(data, "isOfferAccepted");
        assertThat(isOfferAccepted).isFalse();
    }

    private Map<String, Object> sendRequest(Long subjectUserId) {
        User user = userService.getUser(subjectUserId);
        GridGraphQLContext context = ContextHelper.buildContext(user).withFetchedFieldsReslover(null);
        gridContextProvider.setGridContext(context);
        ExecutionResult result = processor.processQuery(null, QUERY_TEMPLATE, null, context);
        checkErrors(result.getErrors());
        return result.getData();
    }
}
