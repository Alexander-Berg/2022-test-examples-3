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
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.minuskeywordspack.mutation.GdUpdateMinusKeywordsPacks;
import ru.yandex.direct.grid.processing.model.minuskeywordspack.mutation.GdUpdateMinusKeywordsPacksItem;
import ru.yandex.direct.grid.processing.model.minuskeywordspack.mutation.GdUpdateMinusKeywordsPacksPayload;
import ru.yandex.direct.grid.processing.model.minuskeywordspack.mutation.GdUpdateMinusKeywordsPacksPayloadItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.minusWordsPackNotFound;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.nestedOrEmptySquareBrackets;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.privateMinusKeywordsPack;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class MinusKeywordsPackGraphQlServiceUpdateMinusKeywordsPacksTest {

    private static final String MUTATION_HANDLE = "updateMinusKeywordsPacks";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    updatedItems {\n"
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
    private MinusKeywordsPackInfo minusKeywordsPack1;
    private MinusKeywordsPackInfo minusKeywordsPack2;

    @Before
    public void initTestData() {
        clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());

        minusKeywordsPack1 = steps.minusKeywordsPackSteps()
                .createMinusKeywordsPack(libraryMinusKeywordsPack().withName("MK Pack #1"), clientInfo);
        minusKeywordsPack2 = steps.minusKeywordsPackSteps()
                .createMinusKeywordsPack(libraryMinusKeywordsPack().withName("MK Pack #2"), clientInfo);

        TestAuthHelper.setDirectAuthentication(operator);
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void updateMinusKeywordsPacks_Success() {
        List<String> mkWords = asList("word1", "word2");
        String mkPackNewName1 = "New MK Pack #1";
        String mkPackNewName2 = "New MK Pack #2";
        List<GdUpdateMinusKeywordsPacksItem> mkPacks = asList(
                new GdUpdateMinusKeywordsPacksItem()
                        .withId(minusKeywordsPack1.getMinusKeywordPackId())
                        .withName(mkPackNewName1)
                        .withMinusKeywords(mkWords),
                new GdUpdateMinusKeywordsPacksItem()
                        .withId(minusKeywordsPack2.getMinusKeywordPackId())
                        .withName(mkPackNewName2)
                        .withMinusKeywords(mkWords));

        GdUpdateMinusKeywordsPacksPayload payload = updateMinusKeywordsPacksGraphQl(mkPacks);

        List<MinusKeywordsPack> expectedMkPacks = asList(
                new MinusKeywordsPack()
                        .withId(minusKeywordsPack1.getMinusKeywordPackId())
                        .withName(mkPackNewName1)
                        .withMinusKeywords(mkWords),
                new MinusKeywordsPack()
                        .withId(minusKeywordsPack2.getMinusKeywordPackId())
                        .withName(mkPackNewName2)
                        .withMinusKeywords(mkWords));
        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields();
        List<MinusKeywordsPack> actualMkPacks =
                testMinusKeywordsPackRepository.getClientPacks(clientInfo.getShard(), clientInfo.getClientId());
        assertThat(actualMkPacks)
                .is(matchedBy(beanDiffer(expectedMkPacks).useCompareStrategy(compareStrategy)));

        GdUpdateMinusKeywordsPacksPayload expectedPayload = new GdUpdateMinusKeywordsPacksPayload()
                .withUpdatedItems(asList(
                        new GdUpdateMinusKeywordsPacksPayloadItem()
                                .withId(minusKeywordsPack1.getMinusKeywordPackId())
                                .withName(mkPackNewName1)
                                .withMinusKeywords(mkWords),
                        new GdUpdateMinusKeywordsPacksPayloadItem()
                                .withId(minusKeywordsPack2.getMinusKeywordPackId())
                                .withName(mkPackNewName2)
                                .withMinusKeywords(mkWords)
                ));
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void updateMinusKeywordsPacks_ValidationError() {
        List<GdUpdateMinusKeywordsPacksItem> mkPacks = asList(
                new GdUpdateMinusKeywordsPacksItem()
                        .withId(minusKeywordsPack1.getMinusKeywordPackId())
                        .withName("MK Pack")
                        .withMinusKeywords(singletonList("[]")));

        GdUpdateMinusKeywordsPacksPayload payload = updateMinusKeywordsPacksGraphQl(mkPacks);

        GdUpdateMinusKeywordsPacksPayload expectedPayload = new GdUpdateMinusKeywordsPacksPayload()
                .withUpdatedItems(singletonList(null))
                .withValidationResult(toGdValidationResult(
                        toGdDefect(path(field(GdUpdateMinusKeywordsPacks.UPDATE_ITEMS), index(0),
                                field(GdUpdateMinusKeywordsPacksItem.MINUS_KEYWORDS), index(0)),
                                nestedOrEmptySquareBrackets(singletonList("[]")))));
        DefaultCompareStrategy compareStrategy = DefaultCompareStrategies
                .allFieldsExcept(newPath("validationResult", "errors", "0", "params"));
        Assertions.assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void updateMinusKeywordsPacks_OneValidOneInvalid() {
        List<String> mkWords = asList("word1", "word2");
        String mkNameValid = "MK Pack";
        String mkNameInvalid = "";
        List<GdUpdateMinusKeywordsPacksItem> mkPacks = asList(
                new GdUpdateMinusKeywordsPacksItem()
                        .withId(minusKeywordsPack1.getMinusKeywordPackId())
                        .withName(mkNameValid)
                        .withMinusKeywords(mkWords),
                new GdUpdateMinusKeywordsPacksItem()
                        .withId(minusKeywordsPack2.getMinusKeywordPackId())
                        .withName(mkNameInvalid)
                        .withMinusKeywords(mkWords));

        GdUpdateMinusKeywordsPacksPayload payload = updateMinusKeywordsPacksGraphQl(mkPacks);

        List<MinusKeywordsPack> expectedMkPacks = asList(
                minusKeywordsPack1.getMinusKeywordsPack()
                        .withName(mkNameValid)
                        .withMinusKeywords(mkWords),
                minusKeywordsPack2.getMinusKeywordsPack());
        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath(MinusKeywordsPack.HASH.name())).useMatcher(notNullValue());
        List<MinusKeywordsPack> actualMkPacks =
                testMinusKeywordsPackRepository.getClientPacks(clientInfo.getShard(), clientInfo.getClientId());
        assertThat(actualMkPacks)
                .is(matchedBy(containsInAnyOrder(
                        mapList(expectedMkPacks, expected -> beanDiffer(expected).useCompareStrategy(compareStrategy))
                )));

        GdUpdateMinusKeywordsPacksPayload expectedPayload = new GdUpdateMinusKeywordsPacksPayload()
                .withUpdatedItems(asList(
                        new GdUpdateMinusKeywordsPacksPayloadItem()
                                .withId(minusKeywordsPack1.getMinusKeywordPackId())
                                .withName(mkNameValid)
                                .withMinusKeywords(mkWords),
                        null))
                .withValidationResult(toGdValidationResult(
                        toGdDefect(path(field(GdUpdateMinusKeywordsPacks.UPDATE_ITEMS), index(1),
                                field(GdUpdateMinusKeywordsPacksItem.NAME)),
                                notEmptyString())));
        DefaultCompareStrategy validationCompareStrategy = DefaultCompareStrategies
                .allFieldsExcept(newPath("validationResult", "errors", "0", "params"));
        Assertions.assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload).useCompareStrategy(validationCompareStrategy)));
    }

    @Test
    public void updateMinusKeywordsPacks_PrivatePack_ValidationError() {
        MinusKeywordsPackInfo minusKeywordsPackPrivate =
                steps.minusKeywordsPackSteps().createMinusKeywordsPack(privateMinusKeywordsPack(), clientInfo);

        List<GdUpdateMinusKeywordsPacksItem> mkPacks = asList(
                new GdUpdateMinusKeywordsPacksItem()
                        .withId(minusKeywordsPackPrivate.getMinusKeywordPackId())
                        .withName("MK Pack")
                        .withMinusKeywords(asList("word1", "word2")));

        GdUpdateMinusKeywordsPacksPayload payload = updateMinusKeywordsPacksGraphQl(mkPacks);

        GdUpdateMinusKeywordsPacksPayload expectedPayload = new GdUpdateMinusKeywordsPacksPayload()
                .withUpdatedItems(singletonList(null))
                .withValidationResult(toGdValidationResult(
                        toGdDefect(path(field(GdUpdateMinusKeywordsPacks.UPDATE_ITEMS), index(0),
                                field(GdUpdateMinusKeywordsPacksItem.ID)),
                                minusWordsPackNotFound())));
        DefaultCompareStrategy compareStrategy = DefaultCompareStrategies
                .allFieldsExcept(newPath("validationResult", "errors", "0", "params"));
        Assertions.assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload).useCompareStrategy(compareStrategy)));
    }

    @Test
    public void updateMinusKeywordsPacks_UpdateOnlyName_Success() {
        String mkPackNewName = "New MK Pack #1";
        List<GdUpdateMinusKeywordsPacksItem> mkPacks = asList(
                new GdUpdateMinusKeywordsPacksItem()
                        .withId(minusKeywordsPack1.getMinusKeywordPackId())
                        .withName(mkPackNewName));

        GdUpdateMinusKeywordsPacksPayload payload = updateMinusKeywordsPacksGraphQl(mkPacks);

        MinusKeywordsPack expectedMkPack = new MinusKeywordsPack()
                .withId(minusKeywordsPack1.getMinusKeywordPackId())
                .withName(mkPackNewName)
                .withMinusKeywords(minusKeywordsPack1.getMinusKeywordsPack().getMinusKeywords());
        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields();
        MinusKeywordsPack actualMkPack = testMinusKeywordsPackRepository
                .get(clientInfo.getShard(), singletonList(minusKeywordsPack1.getMinusKeywordPackId())).get(0);
        assertThat(actualMkPack)
                .is(matchedBy(beanDiffer(expectedMkPack).useCompareStrategy(compareStrategy)));

        GdUpdateMinusKeywordsPacksPayload expectedPayload = new GdUpdateMinusKeywordsPacksPayload()
                .withUpdatedItems(asList(
                        new GdUpdateMinusKeywordsPacksPayloadItem()
                                .withId(minusKeywordsPack1.getMinusKeywordPackId())
                                .withName(mkPackNewName)
                                .withMinusKeywords(minusKeywordsPack1.getMinusKeywordsPack().getMinusKeywords())
                ));
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void updateMinusKeywordsPacks_UpdateOnlyMinusWords_Success() {
        List<String> mkPackNewMinusKeywords = asList("word1", "word2");
        List<GdUpdateMinusKeywordsPacksItem> mkPacks = asList(
                new GdUpdateMinusKeywordsPacksItem()
                        .withId(minusKeywordsPack1.getMinusKeywordPackId())
                        .withMinusKeywords(mkPackNewMinusKeywords));

        GdUpdateMinusKeywordsPacksPayload payload = updateMinusKeywordsPacksGraphQl(mkPacks);

        MinusKeywordsPack expectedMkPack = new MinusKeywordsPack()
                .withId(minusKeywordsPack1.getMinusKeywordPackId())
                .withName(minusKeywordsPack1.getMinusKeywordsPack().getName())
                .withMinusKeywords(mkPackNewMinusKeywords);
        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields();
        MinusKeywordsPack actualMkPack = testMinusKeywordsPackRepository
                .get(clientInfo.getShard(), singletonList(minusKeywordsPack1.getMinusKeywordPackId())).get(0);
        assertThat(actualMkPack)
                .is(matchedBy(beanDiffer(expectedMkPack).useCompareStrategy(compareStrategy)));

        GdUpdateMinusKeywordsPacksPayload expectedPayload = new GdUpdateMinusKeywordsPacksPayload()
                .withUpdatedItems(asList(
                        new GdUpdateMinusKeywordsPacksPayloadItem()
                                .withId(minusKeywordsPack1.getMinusKeywordPackId())
                                .withName(minusKeywordsPack1.getMinusKeywordsPack().getName())
                                .withMinusKeywords(mkPackNewMinusKeywords)
                ));
        assertThat(payload)
                .is(matchedBy(beanDiffer(expectedPayload)));
    }

    private GdUpdateMinusKeywordsPacksPayload updateMinusKeywordsPacksGraphQl(
            List<GdUpdateMinusKeywordsPacksItem> mkPacks) {
        GdUpdateMinusKeywordsPacks request = new GdUpdateMinusKeywordsPacks().withUpdateItems(mkPacks);
        String query = String.format(MUTATION_TEMPLATE, MUTATION_HANDLE, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_HANDLE);

        return GraphQlJsonUtils.convertValue(data.get(MUTATION_HANDLE), GdUpdateMinusKeywordsPacksPayload.class);
    }
}
