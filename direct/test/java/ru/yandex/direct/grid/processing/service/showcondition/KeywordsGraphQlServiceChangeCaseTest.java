package ru.yandex.direct.grid.processing.service.showcondition;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
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

import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.service.KeywordService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.KeywordSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdChangeKeywordsCase;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdChangeKeywordsCaseField;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdChangeKeywordsCaseMode;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdFindAndReplaceKeywordsPayload;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdUpdateKeywordsPayloadItem;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordsGraphQlServiceChangeCaseTest {

    private static final String CHANGE_CASE_MUTATION_NAME = "changeKeywordsCase";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "  rowset {\n" +
            "      id\n" +
            "      keyword\n" +
            "      minusKeywords\n" +
            "    }" +
            "    updatedKeywordIds\n" +
            "    updatedTotalCount"
            + "  }\n"
            + "}";

    private User operator;
    private AdGroupInfo adGroupInfo;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private KeywordSteps keywordSteps;

    @Autowired
    private GridContextProvider contextProvider;

    @Autowired
    private KeywordService keywordService;

    @Before
    public void initTestData() {
        UserInfo userInfo = userSteps.createDefaultUser();
        operator = userInfo.getUser();
        TestAuthHelper.setDirectAuthentication(operator);
        contextProvider.setGridContext(buildContext(operator));

        adGroupInfo = new AdGroupInfo()
                .withClientInfo(userInfo.getClientInfo());
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void changeCase_ActuallyChanged() {
        Keyword keyword = defaultKeyword();

        KeywordInfo keywordInfo = keywordSteps.createKeyword(adGroupInfo, keyword);

        GdChangeKeywordsCase request = new GdChangeKeywordsCase()
                .withCaseMode(GdChangeKeywordsCaseMode.UPPERCASE)
                .withFields(Set.of(GdChangeKeywordsCaseField.KEYWORD))
                .withKeywordIds(List.of(keywordInfo.getId()));

        String query = String.format(MUTATION_TEMPLATE, CHANGE_CASE_MUTATION_NAME,
                graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Assertions.assertThat(data).containsOnlyKeys(CHANGE_CASE_MUTATION_NAME);

        var typeReference = new TypeReference<GdFindAndReplaceKeywordsPayload>() {
        };
        GdFindAndReplaceKeywordsPayload payload = GraphQlJsonUtils.convertValue(data.get(CHANGE_CASE_MUTATION_NAME),
                typeReference);

        GdFindAndReplaceKeywordsPayload expectedPayload = new GdFindAndReplaceKeywordsPayload()
                .withUpdatedKeywordIds(List.of(keywordInfo.getId()))
                .withUpdatedTotalCount(1)
                .withRowset(List.of(new GdUpdateKeywordsPayloadItem()
                        .withId(keywordInfo.getId())
                        .withKeyword(keyword.getPhrase().toUpperCase())
                        .withMinusKeywords(emptyList()))
                );

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
        List<Keyword> keywords =
                keywordService.getKeywords(operator.getClientId(), singletonList(keyword.getId()));
        assertThat(keywords.get(0).getPhrase()).isEqualTo(keyword.getPhrase().toUpperCase());
    }

    @Test
    public void changeCase_NoChange() {
        Keyword keyword = defaultKeyword()
                .withPhrase("КЛЮЧЕВАЯ ФРАЗА");

        KeywordInfo keywordInfo = keywordSteps.createKeyword(adGroupInfo, keyword);

        GdChangeKeywordsCase request = new GdChangeKeywordsCase()
                .withCaseMode(GdChangeKeywordsCaseMode.UPPERCASE)
                .withFields(Set.of(GdChangeKeywordsCaseField.KEYWORD))
                .withKeywordIds(List.of(keywordInfo.getId()));

        String query = String.format(MUTATION_TEMPLATE, CHANGE_CASE_MUTATION_NAME,
                graphQlSerialize(request));

        ExecutionResult result = processor.processQuery(null, query, null, contextProvider.getGridContext());
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        Assertions.assertThat(data).containsOnlyKeys(CHANGE_CASE_MUTATION_NAME);

        var typeReference = new TypeReference<GdFindAndReplaceKeywordsPayload>() {
        };
        GdFindAndReplaceKeywordsPayload payload = GraphQlJsonUtils.convertValue(data.get(CHANGE_CASE_MUTATION_NAME),
                typeReference);

        GdFindAndReplaceKeywordsPayload expectedPayload = new GdFindAndReplaceKeywordsPayload()
                .withUpdatedKeywordIds(emptyList())
                .withUpdatedTotalCount(0)
                .withRowset(emptyList());

        assertThat(payload).is(matchedBy(beanDiffer(expectedPayload)));
        List<Keyword> keywords =
                keywordService.getKeywords(operator.getClientId(), singletonList(keyword.getId()));
        assertThat(keywords.get(0).getPhrase()).isEqualTo(keyword.getPhrase());
    }
}

