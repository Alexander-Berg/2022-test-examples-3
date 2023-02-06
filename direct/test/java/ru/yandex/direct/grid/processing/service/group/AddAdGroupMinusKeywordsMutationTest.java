package ru.yandex.direct.grid.processing.service.group;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupMinusKeywords;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupMinusKeywordsItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupMinusKeywordsPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupMinusKeywordsPayloadItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddAdGroupMinusKeywordsMutationTest {

    private static final String MUTATION_NAME = "addAdGroupMinusKeywords";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    addedItems {\n"
            + "      adGroupId,\n"
            + "      addedMinusKeywords\n"
            + "    }\n"
            + "  }\n"
            + "}";
    private static final long NOT_EXIST_AD_GROUP_ID = Long.MAX_VALUE;
    private static final String INVALID_MINUS_KEYWORD = "---";

    private Long adGroupId;
    private List<String> minusKeywords;
    private User operator;
    private GdAddAdGroupMinusKeywords request;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private AdGroupService adGroupService;

    @Autowired
    private AdGroupMutationService adGroupMutationService;

    @Autowired
    private Steps steps;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void initTestData() {
        minusKeywords = Arrays.asList("first", "second");

        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        adGroupId = adGroupInfo.getAdGroupId();

        operator = UserHelper.getUser(adGroupInfo);
        TestAuthHelper.setDirectAuthentication(operator);

        request = new GdAddAdGroupMinusKeywords()
                .withAddItems(singletonList(new GdAddAdGroupMinusKeywordsItem()
                        .withAdGroupId(adGroupId)
                        .withMinusKeywords(minusKeywords)
                ));
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }


    @Test
    public void checkAddAdGroupMinusKeywords() {
        String query = String.format(MUTATION_TEMPLATE, MUTATION_NAME, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        GdAddAdGroupMinusKeywordsPayload expectedPayload = new GdAddAdGroupMinusKeywordsPayload()
                .withAddedItems(singletonList(
                        new GdAddAdGroupMinusKeywordsPayloadItem()
                                .withAdGroupId(adGroupId)
                                .withAddedMinusKeywords(minusKeywords)
                ));
        Map<String, Object> data = result.getData();
        assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdAddAdGroupMinusKeywordsPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdAddAdGroupMinusKeywordsPayload.class);
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));

        List<AdGroup> adGroups =
                adGroupService.getAdGroups(operator.getClientId(), singletonList(adGroupId));
        assertThat(adGroups)
                .hasSize(1);
        assertThat(adGroups.get(0).getMinusKeywords())
                .containsOnlyElementsOf(minusKeywords);
    }

    @Test
    public void checkAddAdGroupMinusKeywords_RequestValidation() {
        request.setAddItems(Collections.emptyList());
        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(
                hasErrorsWith(gridDefect(GdAddAdGroupMinusKeywords.ADD_ITEMS.name(), CollectionDefects.notEmptyCollection()))));

        adGroupMutationService.addMinusKeywords(request, operator.getUid(), operator.getClientId());
    }

    @Test
    public void checkAddAdGroupMinusKeywords_CoreValidation() {
        request.getAddItems().get(0)
                .setMinusKeywords(Arrays.asList("someKeyword", INVALID_MINUS_KEYWORD));

        GdAddAdGroupMinusKeywordsPayload payload =
                adGroupMutationService.addMinusKeywords(request, operator.getUid(), operator.getClientId());

        Path expectedPath = path(field(GdAddAdGroupMinusKeywords.ADD_ITEMS), index(0),
                field(GdAddAdGroupMinusKeywordsItem.MINUS_KEYWORDS), index(1));
        GdAddAdGroupMinusKeywordsPayload expectedPayload = new GdAddAdGroupMinusKeywordsPayload()
                .withAddedItems(Collections.emptyList())
                .withValidationResult(toGdValidationResult(expectedPath,
                        MinusPhraseDefects.invalidMinusMark(singletonList(INVALID_MINUS_KEYWORD)))
                );

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void checkAddAdGroupMinusKeywords_SuccessResult_WithValidationResult() {
        request = new GdAddAdGroupMinusKeywords()
                .withAddItems(Arrays.asList(
                        new GdAddAdGroupMinusKeywordsItem()
                                .withAdGroupId(adGroupId)
                                .withMinusKeywords(minusKeywords),
                        new GdAddAdGroupMinusKeywordsItem()
                                .withAdGroupId(NOT_EXIST_AD_GROUP_ID)
                                .withMinusKeywords(minusKeywords)
                ));

        GdAddAdGroupMinusKeywordsPayload payload =
                adGroupMutationService.addMinusKeywords(request, operator.getUid(), operator.getClientId());

        Path expectedPath = path(field(GdAddAdGroupMinusKeywords.ADD_ITEMS), index(1),
                field(GdAddAdGroupMinusKeywordsItem.AD_GROUP_ID));
        GdAddAdGroupMinusKeywordsPayload expectedPayload = new GdAddAdGroupMinusKeywordsPayload()
                .withAddedItems(singletonList(
                        new GdAddAdGroupMinusKeywordsPayloadItem()
                                .withAdGroupId(adGroupId)
                                .withAddedMinusKeywords(minusKeywords)
                ))
                .withValidationResult(toGdValidationResult(expectedPath, CommonDefects.objectNotFound()));

        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

}
