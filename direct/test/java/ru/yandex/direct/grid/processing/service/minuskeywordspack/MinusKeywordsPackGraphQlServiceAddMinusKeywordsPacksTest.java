package ru.yandex.direct.grid.processing.service.minuskeywordspack;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.minuskeywordspack.mutation.GdAddMinusKeywordsPacks;
import ru.yandex.direct.grid.processing.model.minuskeywordspack.mutation.GdAddMinusKeywordsPacksItem;
import ru.yandex.direct.grid.processing.model.minuskeywordspack.mutation.GdAddMinusKeywordsPacksPayload;
import ru.yandex.direct.grid.processing.model.minuskeywordspack.mutation.GdAddMinusKeywordsPacksPayloadItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.nestedOrEmptySquareBrackets;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class MinusKeywordsPackGraphQlServiceAddMinusKeywordsPacksTest {

    private static final String MUTATION_HANDLE = "addMinusKeywordsPacks";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    addedItems {\n"
            + "      id\n"
            + "      name\n"
            + "      minusKeywords\n"
            + "    }\n"
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

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;

    private ClientInfo clientInfo;
    private User operator;

    @Before
    public void initTestData() {
        clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void addMinusKeywordsPacks_Success_MkpackCreatedEvenDuplicates() {
        List<String> mkWords = asList("word1", "word2");
        String mkPackName1 = "MK Pack #1";
        String mkPackName2 = "MK Pack #2";
        List<GdAddMinusKeywordsPacksItem> mkPacks = asList(
                new GdAddMinusKeywordsPacksItem()
                        .withName(mkPackName1)
                        .withMinusKeywords(mkWords),
                new GdAddMinusKeywordsPacksItem()
                        .withName(mkPackName2)
                        .withMinusKeywords(mkWords));

        GdAddMinusKeywordsPacksPayload payload = addMinusKeywordsPacksGraphQl(mkPacks);

        List<MinusKeywordsPack> expectedMkPacks = asList(
                new MinusKeywordsPack()
                        .withName(mkPackName1)
                        .withMinusKeywords(mkWords),
                new MinusKeywordsPack()
                        .withName(mkPackName2)
                        .withMinusKeywords(mkWords));
        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(MinusKeywordsPack.ID.name()))
                .useMatcher(notNullValue());
        List<MinusKeywordsPack> actualMkPacks =
                testMinusKeywordsPackRepository.getClientPacks(clientInfo.getShard(), clientInfo.getClientId());
        assertThat(actualMkPacks)
                .is(matchedBy(beanDiffer(expectedMkPacks).useCompareStrategy(compareStrategy)));

        GdAddMinusKeywordsPacksPayload expectedPayload = new GdAddMinusKeywordsPacksPayload()
                .withAddedItems(asList(
                        new GdAddMinusKeywordsPacksPayloadItem()
                                .withId(actualMkPacks.get(0).getId())
                                .withName(mkPackName1)
                                .withMinusKeywords(mkWords),
                        new GdAddMinusKeywordsPacksPayloadItem()
                                .withId(actualMkPacks.get(1).getId())
                                .withName(mkPackName2)
                                .withMinusKeywords(mkWords)
                ));
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void addMinusKeywordsPacks_ValidationError() {
        List<GdAddMinusKeywordsPacksItem> mkPacks = asList(
                new GdAddMinusKeywordsPacksItem()
                        .withName("MK Pack")
                        .withMinusKeywords(singletonList("[]")));

        GdAddMinusKeywordsPacksPayload payload = addMinusKeywordsPacksGraphQl(mkPacks);

        GdAddMinusKeywordsPacksPayload expectedPayload = new GdAddMinusKeywordsPacksPayload()
                .withAddedItems(singletonList(null))
                .withValidationResult(toGdValidationResult(
                        toGdDefect(path(field(GdAddMinusKeywordsPacks.ADD_ITEMS), index(0),
                                field(GdAddMinusKeywordsPacksItem.MINUS_KEYWORDS), index(0)),
                                nestedOrEmptySquareBrackets(singletonList("[]")))));
        DefaultCompareStrategy compareStrategy = DefaultCompareStrategies
                .allFieldsExcept(newPath("validationResult", "errors", "0", "params"));
        Assertions.assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void addMinusKeywordsPacks_OneValidOneInvalid() {
        List<String> mkWords = asList("word1", "word2");
        String mkNameValid = "MK Pack";
        String mkNameInvalid = "";
        List<GdAddMinusKeywordsPacksItem> mkPacks = asList(
                new GdAddMinusKeywordsPacksItem()
                        .withName(mkNameValid)
                        .withMinusKeywords(mkWords),
                new GdAddMinusKeywordsPacksItem()
                        .withName(mkNameInvalid)
                        .withMinusKeywords(mkWords));

        GdAddMinusKeywordsPacksPayload payload = addMinusKeywordsPacksGraphQl(mkPacks);

        List<MinusKeywordsPack> expectedMkPacks = asList(
                new MinusKeywordsPack()
                        .withName(mkNameValid)
                        .withMinusKeywords(mkWords));
        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(MinusKeywordsPack.ID.name()))
                .useMatcher(notNullValue());
        List<MinusKeywordsPack> actualMkPacks =
                testMinusKeywordsPackRepository.getClientPacks(clientInfo.getShard(), clientInfo.getClientId());
        assertThat(actualMkPacks)
                .is(matchedBy(beanDiffer(expectedMkPacks).useCompareStrategy(compareStrategy)));

        GdAddMinusKeywordsPacksPayload expectedPayload = new GdAddMinusKeywordsPacksPayload()
                .withAddedItems(asList(
                        new GdAddMinusKeywordsPacksPayloadItem()
                                .withId(actualMkPacks.get(0).getId())
                                .withName(mkNameValid)
                                .withMinusKeywords(mkWords),
                        null))
                .withValidationResult(toGdValidationResult(
                        toGdDefect(path(field(GdAddMinusKeywordsPacks.ADD_ITEMS), index(1),
                                field(GdAddMinusKeywordsPacksItem.NAME)),
                                notEmptyString())));
        DefaultCompareStrategy validationCompareStrategy = DefaultCompareStrategies
                .allFieldsExcept(newPath("validationResult", "errors", "0", "params"));
        Assertions.assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload).useCompareStrategy(validationCompareStrategy)));
    }

    private GdAddMinusKeywordsPacksPayload addMinusKeywordsPacksGraphQl(List<GdAddMinusKeywordsPacksItem> mkPacks) {
        GdAddMinusKeywordsPacks request = new GdAddMinusKeywordsPacks().withAddItems(mkPacks);
        String query = String.format(MUTATION_TEMPLATE, MUTATION_HANDLE, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_HANDLE);

        return GraphQlJsonUtils.convertValue(data.get(MUTATION_HANDLE), GdAddMinusKeywordsPacksPayload.class);
    }
}
