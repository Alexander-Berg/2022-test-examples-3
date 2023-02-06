package ru.yandex.direct.grid.processing.service.trackingphone;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.clientphone.ClientPhoneTestUtils;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhoneType;
import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.trackingphone.GdGetPhonesInput;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.core.entity.clientphone.ClientPhoneTestUtils.getUniqPhone;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TrackingPhoneGraphQLServiceTest {

    private static final String FILTERS_QUERY_TEMPLATE = "{\n" +
            "  client(searchBy: {login: \"%s\"}) {\n" +
            "    phones(input: %s ) {\n" +
            "      manualPhones {\n" +
            "        phoneId\n" +
            "        phoneType\n" +
            "        comment\n" +
            "        phone\n" +
            "        extension\n" +
            "        formattedPhone {\n" +
            "           countryCode\n" +
            "           cityCode\n" +
            "           phoneNumber\n" +
            "           extension\n" +
            "        }\n" +
            "      }\n" +
            "      organizationPhones {\n" +
            "        permalinkId\n" +
            "        rowset {\n" +
            "          phoneId\n" +
            "          phoneType\n" +
            "          phone\n" +
            "          extension\n" +
            "        }\n" +
            "      }\n" +
            "      telephonyPhones {\n" +
            "        permalinkId\n" +
            "        rowset {\n" +
            "          phoneId\n" +
            "          phoneType\n" +
            "          phone\n" +
            "          extension\n" +
            "          redirectPhone\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
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
    @Autowired
    private OrganizationsClientStub organizationsClientStub;

    private GridGraphQLContext context;
    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        User user = userService.getUser(clientInfo.getUid());
        TestAuthHelper.setDirectAuthentication(user);
        context = ContextHelper.buildContext(user);
        gridContextProvider.setGridContext(context);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.TELEPHONY_ALLOWED, true);
    }

    @Test
    public void getPhones() {
        PhoneNumber phoneNumber = new PhoneNumber().withPhone("+79994547890").withExtension(200L);
        ClientPhone clientPhone = steps.clientPhoneSteps().addClientManualPhone(clientInfo.getClientId(), phoneNumber);
        GdGetPhonesInput input = new GdGetPhonesInput()
                //Список должен быть пустым, иначе запрос уходит в API Справочника
                .withPermalinkIds(Collections.emptyList());
        String query = String.format(FILTERS_QUERY_TEMPLATE, context.getOperator().getLogin(),
                graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        assumeThat(result.getErrors(), hasSize(0));
        Map<String, Object> data = result.getData();
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(data).isNotEmpty();
            List<Map<String, Object>> manualPhones = getDataValue(data, "client/phones/manualPhones/");
            List<Map<String, Object>> organizationPhones = getDataValue(data, "client/phones/organizationPhones/");
            sa.assertThat(organizationPhones).isEmpty();
            Map<String, Object> manualPhone = manualPhones.get(0);
            sa.assertThat(manualPhone.get("phoneId")).isEqualTo(clientPhone.getId());
            sa.assertThat(manualPhone.get("phone")).isEqualTo("+79994547890");
            sa.assertThat(manualPhone.get("extension")).isEqualTo(200L);
            sa.assertThat(manualPhone.get("comment")).isEqualTo(clientPhone.getComment());
            Map<String, Object> formattedPhone = getDataValue(data, "client/phones/manualPhones/0/formattedPhone");
            sa.assertThat(formattedPhone.get("countryCode")).isEqualTo("+7");
            sa.assertThat(formattedPhone.get("cityCode")).isEqualTo("999");
            sa.assertThat(formattedPhone.get("phoneNumber")).isEqualTo("454-78-90");
            sa.assertThat(formattedPhone.get("extension")).isEqualTo("200");
        });
    }

    @Test
    public void getPhones_withSpravAndTelephony() {
        Long permalinkId = RandomUtils.nextLong();
        Long metrikaCounterId = RandomUtils.nextLong();
        GdGetPhonesInput input = new GdGetPhonesInput().withPermalinkIds(Collections.singletonList(permalinkId));

        ClientPhone clientTelephonyPhone = new ClientPhone()
                .withClientId(clientInfo.getClientId())
                .withPhoneNumber(new PhoneNumber().withPhone(getUniqPhone()))
                .withPermalinkId(permalinkId)
                .withPhoneType(ClientPhoneType.TELEPHONY)
                .withIsDeleted(false);

        steps.clientPhoneSteps().addPhone(clientInfo.getClientId(), clientTelephonyPhone);

        List<Long> uids = List.of(clientInfo.getUid());
        organizationsClientStub.addUidsAndCounterIdsByPermalinkId(permalinkId, uids, metrikaCounterId);

        String query = String.format(FILTERS_QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(input));

        ExecutionResult result = processor.processQuery(null, query, null, context);

        assumeThat(result.getErrors(), hasSize(0));
        Map<String, Object> data = result.getData();
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(data).isNotEmpty();
            List<Map<String, Object>> manualPhones = getDataValue(data, "client/phones/manualPhones/");
            List<Map<String, Object>> organizationPhones = getDataValue(data, "client/phones/organizationPhones/");
            List<Map<String, Object>> telephonyPhones = getDataValue(data, "client/phones/telephonyPhones/");
            sa.assertThat(manualPhones).isEmpty();
            sa.assertThat(organizationPhones).hasSize(1);
            sa.assertThat(organizationPhones.get(0).get("permalinkId")).isEqualTo(permalinkId);
            sa.assertThat(telephonyPhones).hasSize(1);
            sa.assertThat(telephonyPhones.get(0).get("permalinkId")).isEqualTo(permalinkId);
            Map<String, Object> telephonyPhone =
                    ((List<Map<String, Object>>) telephonyPhones.get(0).get("rowset")).get(0);
            sa.assertThat(telephonyPhone.get("phoneId")).isNotNull();
            sa.assertThat(telephonyPhone.get("redirectPhone")).isNotNull();
        });
    }

    @Test
    public void getPhones_withSpravWithoutOrgPhone() {
        Long permalinkId = RandomUtils.nextLong();
        GdGetPhonesInput input = new GdGetPhonesInput().withPermalinkIds(Collections.singletonList(permalinkId));

        organizationsClientStub.addUidsWithPhonesByPermalinkId(permalinkId, List.of(clientInfo.getUid()), null);

        String query = String.format(FILTERS_QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(input));

        ExecutionResult result = processor.processQuery(null, query, null, context);

        assumeThat(result.getErrors(), hasSize(0));
        Map<String, Object> data = result.getData();
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(data).isNotEmpty();
            List<Map<String, Object>> manualPhones = getDataValue(data, "client/phones/manualPhones/");
            List<Map<String, Object>> organizationPhones = getDataValue(data, "client/phones/organizationPhones/");
            List<Map<String, Object>> telephonyPhones = getDataValue(data, "client/phones/telephonyPhones/");
            sa.assertThat(manualPhones).isEmpty();
            sa.assertThat(telephonyPhones).isEmpty();
            sa.assertThat(organizationPhones).hasSize(1);
            Map<String, Object> organization = organizationPhones.get(0);
            sa.assertThat((long) organization.get("permalinkId")).isEqualTo(permalinkId);
            List<Map<String, Object>> rowset = (List<Map<String, Object>>) organization.get("rowset");
            sa.assertThat(rowset).isEmpty();
        });
    }

    @Test
    public void getPhones_withManySpravPhones() {
        Long permalinkId = RandomUtils.nextLong();
        GdGetPhonesInput input = new GdGetPhonesInput().withPermalinkIds(Collections.singletonList(permalinkId));

        String firstOrgPhone = ClientPhoneTestUtils.getUniqPhone();
        String secondOrgPhone = ClientPhoneTestUtils.getUniqPhone();
        organizationsClientStub.addUidsWithPhonesByPermalinkId(
                permalinkId,
                List.of(clientInfo.getUid()),
                List.of(firstOrgPhone, secondOrgPhone)
        );

        String query = String.format(FILTERS_QUERY_TEMPLATE, context.getOperator().getLogin(), graphQlSerialize(input));

        ExecutionResult result = processor.processQuery(null, query, null, context);

        assumeThat(result.getErrors(), hasSize(0));
        Map<String, Object> data = result.getData();
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(data).isNotEmpty();
            List<Map<String, Object>> manualPhones = getDataValue(data, "client/phones/manualPhones/");
            List<Map<String, Object>> organizationPhones = getDataValue(data, "client/phones/organizationPhones/");
            sa.assertThat(manualPhones).isEmpty();
            sa.assertThat(organizationPhones).hasSize(1);
            sa.assertThat(organizationPhones.get(0).get("permalinkId")).isEqualTo(permalinkId);
            List<Map<String, Object>> rowset = (List<Map<String, Object>>) organizationPhones.get(0).get("rowset");
            sa.assertThat(rowset).hasSize(2);
            Map<String, Object> orgPhone0 = rowset.get(0);
            // Проверяем, что первым в списке окажется первый телефон справочника
            sa.assertThat(orgPhone0.get("phone")).isEqualTo(firstOrgPhone);
        });
    }

}
