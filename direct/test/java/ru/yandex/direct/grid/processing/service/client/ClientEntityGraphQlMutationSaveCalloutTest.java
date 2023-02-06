package ru.yandex.direct.grid.processing.service.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.addition.callout.container.CalloutSelection;
import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.model.CalloutsStatusModerate;
import ru.yandex.direct.core.entity.addition.callout.service.CalloutService;
import ru.yandex.direct.core.entity.addition.callout.service.validation.CalloutConstants;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.cliententity.mutation.GdSaveCallouts;
import ru.yandex.direct.grid.processing.model.cliententity.mutation.GdSaveCalloutsItem;
import ru.yandex.direct.grid.processing.model.cliententity.mutation.GdSaveCalloutsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class ClientEntityGraphQlMutationSaveCalloutTest {

    private static final String SAVE_CALLOUTS_MUTATION = "saveCallouts";
    private static final String SAVE_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    calloutIds\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        params\n"
            + "        path\n"
            + "      }\n"
            + "      warnings {\n"
            + "        code\n"
            + "        params\n"
            + "        path\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final String CALLOUT_TEXT = "уточнение";
    private static final String CALLOUT_INCORRECT_TEXT =
            RandomStringUtils.randomAlphabetic(CalloutConstants.MAX_CALLOUT_TEXT_LENGTH + 1);

    private User operator;
    private GdSaveCallouts request;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private CalloutService calloutService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void initTestData() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void saveCallouts_success() {
        request = new GdSaveCallouts().withSaveItems(
                Collections.singletonList(new GdSaveCalloutsItem().withText(CALLOUT_TEXT))
        );
        String query = String.format(SAVE_MUTATION_TEMPLATE, SAVE_CALLOUTS_MUTATION, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        Callout expected = new Callout().withClientId(operator.getClientId().asLong())
                .withText(CALLOUT_TEXT)
                .withStatusModerate(CalloutsStatusModerate.READY)
                .withDeleted(false);
        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(Callout.ID.name()))
                .useMatcher(notNullValue());

        List<Callout> callouts =
                calloutService.getCallouts(operator.getClientId(), new CalloutSelection(), LimitOffset.maxLimited());
        assertThat(callouts)
                .is(matchedBy(beanDiffer(Collections.singletonList(expected))
                        .useCompareStrategy(compareStrategy))
                );

        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(SAVE_CALLOUTS_MUTATION);

        GdSaveCalloutsPayload expectedPayload = new GdSaveCalloutsPayload()
                .withCalloutIds(Collections.singletonList(callouts.get(0).getId()));

        GdSaveCalloutsPayload payload = GraphQlJsonUtils
                .convertValue(data.get(SAVE_CALLOUTS_MUTATION), GdSaveCalloutsPayload.class);
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void saveCallouts_validation() {
        request = new GdSaveCallouts().withSaveItems(
                Collections.singletonList(new GdSaveCalloutsItem().withText(""))
        );
        String query = String.format(SAVE_MUTATION_TEMPLATE, SAVE_CALLOUTS_MUTATION, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .hasSize(1);
        assertThat(result.getErrors().get(0)).isInstanceOf(ExceptionWhileDataFetching.class);
        assertThat(((ExceptionWhileDataFetching) result.getErrors().get(0)).getException())
                .isInstanceOf(GridValidationException.class);
    }

    @Test
    public void saveCallouts_partial() {
        request = new GdSaveCallouts().withSaveItems(ImmutableList.of(
                new GdSaveCalloutsItem().withText(CALLOUT_TEXT),
                new GdSaveCalloutsItem().withText(CALLOUT_INCORRECT_TEXT))
        );
        String query = String.format(SAVE_MUTATION_TEMPLATE, SAVE_CALLOUTS_MUTATION, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        Callout expected = new Callout().withClientId(operator.getClientId().asLong())
                .withText(CALLOUT_TEXT)
                .withStatusModerate(CalloutsStatusModerate.READY)
                .withDeleted(false);
        CompareStrategy calloutCompareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(Callout.ID.name()))
                .useMatcher(notNullValue());

        List<Callout> callouts =
                calloutService.getCallouts(operator.getClientId(), new CalloutSelection(), LimitOffset.maxLimited());
        assertThat(callouts)
                .is(matchedBy(beanDiffer(Collections.singletonList(expected))
                        .useCompareStrategy(calloutCompareStrategy))
                );

        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(SAVE_CALLOUTS_MUTATION);

        GdSaveCalloutsPayload expectedPayload = new GdSaveCalloutsPayload()
                .withCalloutIds(Collections.singletonList(callouts.get(0).getId()));

        GdSaveCalloutsPayload payload = GraphQlJsonUtils
                .convertValue(data.get(SAVE_CALLOUTS_MUTATION), GdSaveCalloutsPayload.class);
        CompareStrategy payloadCompareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(GdSaveCalloutsPayload.VALIDATION_RESULT.name(), GdValidationResult.ERRORS.name()))
                .useMatcher(iterableWithSize(1));

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(payloadCompareStrategy))
                );
    }
}
