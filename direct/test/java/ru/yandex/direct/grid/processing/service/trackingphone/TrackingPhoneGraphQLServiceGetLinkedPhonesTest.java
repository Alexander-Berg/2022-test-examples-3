package ru.yandex.direct.grid.processing.service.trackingphone;

import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.trackingphone.GdGetLinkedPhones;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TrackingPhoneGraphQLServiceGetLinkedPhonesTest {

    private static final String FILTERS_QUERY_TEMPLATE = "{\n" +
            "  client(searchBy: {login: \"%s\"}) {\n" +
            "    linkedPhones(input: %s) {\n" +
            "      phoneId\n" +
            "      formattedPhone {\n" +
            "        cityCode\n" +
            "        countryCode\n" +
            "        extension\n" +
            "        phoneNumber\n" +
            "      }\n" +
            "    }\n" +
            "  }" +
            "}";

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;

    private GridGraphQLContext context;
    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        User user = userService.getUser(clientInfo.getUid());
        context = ContextHelper.buildContext(user);
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void getLinkedPhones() {
        var clientId = clientInfo.getClientId();
        var phoneNumber = new PhoneNumber().withPhone("+79991112233").withExtension(1L);
        var clientPhone = steps.clientPhoneSteps().addClientManualPhone(clientId, phoneNumber);
        var defaultAdGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        var bannerInfoOne = steps.bannerSteps().createDefaultBanner(defaultAdGroup);
        var bannerId = bannerInfoOne.getBannerId();

        addOrganizationAndPhoneToBanner(clientInfo.getShard(), clientId, bannerId, clientPhone.getId());

        var input = new GdGetLinkedPhones().withAdIds(List.of(bannerId));
        var query = String.format(FILTERS_QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        assumeThat(result.getErrors(), hasSize(0));
        Map<String, Object> data = result.getData();
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(data).isNotEmpty();
            List<Map<String, Object>> linkedPhones = getDataValue(data, "client/linkedPhones/");
            Map<String, Object> linked = linkedPhones.get(0);
            sa.assertThat(linked.get("phoneId")).isEqualTo(clientPhone.getId());
            Map<String, Object> formattedPhone = getDataValue(data, "client/linkedPhones/0/formattedPhone");
            sa.assertThat(formattedPhone.get("countryCode")).isEqualTo("+7");
            sa.assertThat(formattedPhone.get("cityCode")).isEqualTo("999");
            sa.assertThat(formattedPhone.get("phoneNumber")).isEqualTo("111-22-33");
            sa.assertThat(formattedPhone.get("extension")).isEqualTo("1");
        });
    }

    private void addOrganizationAndPhoneToBanner(int shard, ClientId clientId, Long bannerId, Long clientPhoneId) {
        long permalinkId = RandomNumberUtils.nextPositiveLong();
        steps.organizationSteps().createClientOrganization(clientId, permalinkId);
        steps.organizationSteps().linkOrganizationToBanner(clientId, permalinkId, bannerId);
        steps.clientPhoneSteps().linkPhoneIdToBanner(shard, bannerId, clientPhoneId);
    }
}
