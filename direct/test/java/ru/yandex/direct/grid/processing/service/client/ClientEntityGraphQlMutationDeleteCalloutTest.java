package ru.yandex.direct.grid.processing.service.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
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
import ru.yandex.direct.core.entity.addition.callout.service.CalloutService;
import ru.yandex.direct.core.entity.addition.callout.service.validation.CalloutDefectIds;
import ru.yandex.direct.core.entity.banner.model.AdditionType;
import ru.yandex.direct.core.entity.banner.model.BannerAddition;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerAdditionsRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.cliententity.mutation.GdDeleteCallouts;
import ru.yandex.direct.grid.processing.model.cliententity.mutation.GdDeleteCalloutsPayload;
import ru.yandex.direct.grid.processing.model.cliententity.mutation.GdSaveCalloutsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.core.IsNull.notNullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class ClientEntityGraphQlMutationDeleteCalloutTest {

    private static final String DELETE_CALLOUTS_MUTATION = "deleteCallouts";
    private static final String DELETE_MUTATION_TEMPLATE = ""
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

    private User operator;
    private Callout callout;
    private GdDeleteCallouts request;
    private ClientInfo clientInfo;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private CalloutService calloutService;

    @Autowired
    private OldBannerAdditionsRepository bannerAdditionsRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void initTestData() {
        clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
        callout = steps.calloutSteps().createDefaultCallout(clientInfo);
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void deleteCallouts() {
        request = new GdDeleteCallouts().withCalloutIds(Collections.singletonList(callout.getId()));
        String query =
                String.format(DELETE_MUTATION_TEMPLATE, DELETE_CALLOUTS_MUTATION, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));

        checkSuccessDeleteCallouts(result);
    }

    @Test
    public void deleteCallouts_validation() {
        Long invalidCalloutId = -1L;
        request = new GdDeleteCallouts().withCalloutIds(Collections.singletonList(invalidCalloutId));
        String query = String.format(DELETE_MUTATION_TEMPLATE, DELETE_CALLOUTS_MUTATION, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .hasSize(1);
        assertThat(result.getErrors().get(0)).isInstanceOf(ExceptionWhileDataFetching.class);
        assertThat(((ExceptionWhileDataFetching) result.getErrors().get(0)).getException())
                .isInstanceOf(GridValidationException.class);
    }

    @Test
    public void deleteCallouts_validationOnAttachedBanners() {
        createBannerWithAttachedCallout();
        request = new GdDeleteCallouts()
                .withCalloutIds(Collections.singletonList(callout.getId()));
        String query = String.format(DELETE_MUTATION_TEMPLATE, DELETE_CALLOUTS_MUTATION, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .hasSize(0);
        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(DELETE_CALLOUTS_MUTATION);

        GdDeleteCalloutsPayload payload = GraphQlJsonUtils
                .convertValue(data.get(DELETE_CALLOUTS_MUTATION), GdDeleteCalloutsPayload.class);
        assertThat(payload.getValidationResult().getErrors())
                .hasSize(1);
        assertThat(payload.getValidationResult().getErrors().get(0).getCode())
                .isEqualTo(CalloutDefectIds.Gen.AD_EXTENSION_IS_IN_USE.getCode());
    }

    @Test
    public void deleteCallouts_withDetach() {
        createBannerWithAttachedCallout();
        request = new GdDeleteCallouts()
                .withCalloutIds(Collections.singletonList(callout.getId()))
                .withDetachBeforeDelete(true);
        String query =
                String.format(DELETE_MUTATION_TEMPLATE, DELETE_CALLOUTS_MUTATION, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));

        checkSuccessDeleteCallouts(result);
    }

    @Test
    public void deleteCallouts_partial() {
        Long unexistingCalloutId = callout.getId() + 1000;
        request = new GdDeleteCallouts().withCalloutIds(ImmutableList.of(callout.getId(), unexistingCalloutId));
        String query =
                String.format(DELETE_MUTATION_TEMPLATE, DELETE_CALLOUTS_MUTATION, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        assertThat(result.getErrors())
                .isEmpty();

        Callout expected = callout.withDeleted(true);
        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(Callout.CREATE_TIME.name()), newPath(Callout.LAST_CHANGE.name()))
                .useMatcher(notNullValue());

        List<Callout> callouts =
                calloutService.getCallouts(operator.getClientId(), new CalloutSelection(), LimitOffset.maxLimited());
        assertThat(callouts)
                .is(matchedBy(contains(Collections.singletonList(beanDiffer(expected)
                        .useCompareStrategy(compareStrategy)))));

        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(DELETE_CALLOUTS_MUTATION);

        GdDeleteCalloutsPayload expectedPayload = new GdDeleteCalloutsPayload()
                .withCalloutIds(Collections.singletonList(callout.getId()));

        GdDeleteCalloutsPayload payload = GraphQlJsonUtils
                .convertValue(data.get(DELETE_CALLOUTS_MUTATION), GdDeleteCalloutsPayload.class);

        CompareStrategy payloadCompareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(GdSaveCalloutsPayload.VALIDATION_RESULT.name(), GdValidationResult.ERRORS.name()))
                .useMatcher(iterableWithSize(1));

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)
                        .useCompareStrategy(payloadCompareStrategy))
                );
    }

    private void checkSuccessDeleteCallouts(ExecutionResult result) {
        assertThat(result.getErrors())
                .isEmpty();

        Callout expected = callout.withDeleted(true);
        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(Callout.CREATE_TIME.name()), newPath(Callout.LAST_CHANGE.name()))
                .useMatcher(notNullValue());

        List<Callout> callouts =
                calloutService.getCallouts(operator.getClientId(), new CalloutSelection(), LimitOffset.maxLimited());
        assertThat(callouts)
                .is(matchedBy(contains(Collections.singletonList(beanDiffer(expected)
                        .useCompareStrategy(compareStrategy)))));

        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(DELETE_CALLOUTS_MUTATION);

        GdDeleteCalloutsPayload expectedPayload = new GdDeleteCalloutsPayload()
                .withCalloutIds(Collections.singletonList(callout.getId()));

        GdDeleteCalloutsPayload payload = GraphQlJsonUtils
                .convertValue(data.get(DELETE_CALLOUTS_MUTATION), GdDeleteCalloutsPayload.class);
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    private TextBannerInfo createBannerWithAttachedCallout() {
        TextBannerInfo banner = steps.bannerSteps().createBanner(activeTextBanner()
                .withCalloutIds(List.of(callout.getId())), clientInfo);
        bannerAdditionsRepository.addOrUpdateBannerAdditions(dslContextProvider.ppc(clientInfo.getShard()), List.of(new BannerAddition()
                        .withAdditionType(AdditionType.CALLOUT)
                        .withBannerId(banner.getBannerId())
                        .withId(callout.getId())
                        .withSequenceNum(1L))
        );

        return banner;
    }

}
