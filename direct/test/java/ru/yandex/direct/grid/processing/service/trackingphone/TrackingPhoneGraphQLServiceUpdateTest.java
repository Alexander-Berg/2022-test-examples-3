package ru.yandex.direct.grid.processing.service.trackingphone;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.trackingphone.mutation.GdUpdateTelephonyRedirectPhone;
import ru.yandex.direct.grid.processing.model.trackingphone.mutation.GdUpdateTelephonyRedirectPhonePayload;
import ru.yandex.direct.grid.processing.model.trackingphone.mutation.GdUpdateTelephonyRedirectPhones;
import ru.yandex.direct.grid.processing.model.trackingphone.mutation.GdUpdateTelephonyRedirectPhonesPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TrackingPhoneGraphQLServiceUpdateTest {

    private static final String UPDATE_PHONE_TEMPLATE = "mutation {\n" +
            "  updateTelephonyRedirectPhone(input: %s) {\n" +
            "    phoneId\n" +
            "    validationResult {\n" +
            "      errors {\n" +
            "        path\n" +
            "        code\n" +
            "        params\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String UPDATE_PHONES_TEMPLATE = "mutation {\n" +
            "  updateTelephonyRedirectPhones(input: %s) {\n" +
            "    items {" +
            "      phoneId\n" +
            "    }" +
            "    validationResult {\n" +
            "      errors {\n" +
            "        path\n" +
            "        code\n" +
            "        params\n" +
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

    private GridGraphQLContext context;
    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        User user = userService.getUser(clientInfo.getUid());
        TestAuthHelper.setDirectAuthentication(user);
        context = ContextHelper.buildContext(user);
        gridContextProvider.setGridContext(context);
    }

    @Test
    public void updatePhone() {
        Long addedId = steps.clientPhoneSteps()
                .addClientTelephonyPhone(clientInfo.getClientId(), new PhoneNumber().withPhone("+79994500001")).getId();

        GdUpdateTelephonyRedirectPhone input = new GdUpdateTelephonyRedirectPhone()
                .withPhoneId(addedId)
                .withRedirectPhone("+79994500002");

        String query = String.format(UPDATE_PHONE_TEMPLATE, graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        Map<String, Object> data = result.getData();
        GdUpdateTelephonyRedirectPhonePayload payload = convertValue(data.get("updateTelephonyRedirectPhone"),
                GdUpdateTelephonyRedirectPhonePayload.class);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(result.getErrors()).isEmpty();
            sa.assertThat(payload.getValidationResult()).isNull();
            sa.assertThat(payload.getPhoneId()).isEqualTo(addedId);
        });
    }

    @Test
    public void updatePhone_invalid() {
        Long addedId = steps.clientPhoneSteps()
                .addClientTelephonyPhone(clientInfo.getClientId(), new PhoneNumber().withPhone("+79994500003")).getId();

        GdUpdateTelephonyRedirectPhone input = new GdUpdateTelephonyRedirectPhone()
                .withPhoneId(addedId)
                .withRedirectPhone("+799945000031");

        String query = String.format(UPDATE_PHONE_TEMPLATE, graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        Map<String, Object> data = result.getData();
        GdUpdateTelephonyRedirectPhonePayload payload = convertValue(data.get("updateTelephonyRedirectPhone"),
                GdUpdateTelephonyRedirectPhonePayload.class);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(result.getErrors()).isEmpty();
            sa.assertThat(payload.getValidationResult()).isNotNull();
            sa.assertThat(payload.getPhoneId()).isNull();
        });
    }

    @Test
    public void updatePhones() {
        Long firstAddedId = steps.clientPhoneSteps()
                .addClientTelephonyPhone(clientInfo.getClientId(), new PhoneNumber().withPhone("+79994500004")).getId();
        Long secondAddedId = steps.clientPhoneSteps()
                .addClientTelephonyPhone(clientInfo.getClientId(), new PhoneNumber().withPhone("+79994500005")).getId();

        GdUpdateTelephonyRedirectPhones input = new GdUpdateTelephonyRedirectPhones()
                .withUpdateItems(List.of(
                        new GdUpdateTelephonyRedirectPhone()
                                .withPhoneId(firstAddedId)
                                .withRedirectPhone("+79994500006"),
                        new GdUpdateTelephonyRedirectPhone()
                                .withPhoneId(secondAddedId)
                                .withRedirectPhone("+79994500007")
                ));

        String query = String.format(UPDATE_PHONES_TEMPLATE, graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        Map<String, Object> data = result.getData();
        GdUpdateTelephonyRedirectPhonesPayload payload = convertValue(data.get("updateTelephonyRedirectPhones"),
                GdUpdateTelephonyRedirectPhonesPayload.class);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(result.getErrors()).isEmpty();
            sa.assertThat(payload.getValidationResult().getErrors()).isEmpty();
            sa.assertThat(payload.getItems()).hasSize(2);
            sa.assertThat(payload.getItems().stream()
                    .map(GdUpdateTelephonyRedirectPhonePayload::getPhoneId).collect(Collectors.toSet()))
                    .isEqualTo(Set.of(firstAddedId, secondAddedId));
        });
    }

    @Test
    public void updatePhones_onlyOneValid() {
        Long firstAddedId = steps.clientPhoneSteps()
                .addClientTelephonyPhone(clientInfo.getClientId(), new PhoneNumber().withPhone("+79994500011")).getId();
        Long secondAddedId = steps.clientPhoneSteps()
                .addClientTelephonyPhone(clientInfo.getClientId(), new PhoneNumber().withPhone("+79994500012")).getId();

        GdUpdateTelephonyRedirectPhones input = new GdUpdateTelephonyRedirectPhones()
                .withUpdateItems(List.of(
                        new GdUpdateTelephonyRedirectPhone()
                                .withPhoneId(firstAddedId)
                                .withRedirectPhone("+79994500013"),
                        new GdUpdateTelephonyRedirectPhone()
                                .withPhoneId(secondAddedId)
                                .withRedirectPhone("+799945000121")
                ));

        String query = String.format(UPDATE_PHONES_TEMPLATE, graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        Map<String, Object> data = result.getData();
        GdUpdateTelephonyRedirectPhonesPayload payload = convertValue(data.get("updateTelephonyRedirectPhones"),
                GdUpdateTelephonyRedirectPhonesPayload.class);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(result.getErrors()).isEmpty();
            sa.assertThat(payload.getValidationResult()).isNotNull();
            sa.assertThat(payload.getValidationResult().getErrors()).hasSize(1);
            sa.assertThat(payload.getItems()).hasSize(1);
            sa.assertThat(payload.getItems().get(0).getPhoneId()).isEqualTo(firstAddedId);
        });
    }

    @Test
    public void updatePhones_invalid() {
        Long firstAddedId = steps.clientPhoneSteps()
                .addClientTelephonyPhone(clientInfo.getClientId(), new PhoneNumber().withPhone("+79994500015")).getId();
        Long secondAddedId = steps.clientPhoneSteps()
                .addClientTelephonyPhone(clientInfo.getClientId(), new PhoneNumber().withPhone("+79994500016")).getId();

        GdUpdateTelephonyRedirectPhones input = new GdUpdateTelephonyRedirectPhones()
                .withUpdateItems(List.of(
                        new GdUpdateTelephonyRedirectPhone()
                                .withPhoneId(firstAddedId)
                                .withRedirectPhone("+799945000151"),
                        new GdUpdateTelephonyRedirectPhone()
                                .withPhoneId(secondAddedId)
                                .withRedirectPhone("+799945000161")
                ));

        String query = String.format(UPDATE_PHONES_TEMPLATE, graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        Map<String, Object> data = result.getData();
        GdUpdateTelephonyRedirectPhonesPayload payload = convertValue(data.get("updateTelephonyRedirectPhones"),
                GdUpdateTelephonyRedirectPhonesPayload.class);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(result.getErrors()).isEmpty();
            sa.assertThat(payload.getValidationResult()).isNotNull();
            sa.assertThat(payload.getValidationResult().getErrors()).hasSize(2);
            sa.assertThat(payload.getItems()).hasSize(0);
        });
    }

}
