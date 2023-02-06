package ru.yandex.direct.grid.processing.service.group;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import graphql.ExecutionResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayloadItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroup;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;

import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateAdGroupMutationRelevanceMatchTest extends UpdateAdGroupMutationBaseTest {

    @Before
    public void init() {
        super.initTestData();
        relevanceMatch = steps.relevanceMatchSteps().addDefaultRelevanceMatch(textAdGroupInfo);
    }

    @Test
    public void checkUpdateAdGroupRelevanceMatch_stayOn() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(Collections.singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);

        checkKeywordDbState(mapList(Arrays.asList(keywordInfo), KeywordInfo::getKeyword));

        checkRelevanceMatchDbState(relevanceMatch);

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroupRelevanceMatch_unchangedPrice() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setGeneralPrice(BigDecimal.valueOf(3.14))  //проверяем что ставка не изменится
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(Collections.singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);

        checkKeywordDbState(mapList(Arrays.asList(keywordInfo), KeywordInfo::getKeyword));

        checkRelevanceMatchDbState(relevanceMatch);

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroupRelevanceMatch_changePrice() {
        BigDecimal testGeneralPrice = BigDecimal.valueOf(3.14);

        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setGeneralPrice(testGeneralPrice)
                .setRelevanceMatch(true, null) //при сбросе id обновится ставка автотаргетинга
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(Collections.singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);

        checkKeywordDbState(mapList(Arrays.asList(keywordInfo), KeywordInfo::getKeyword));

        relevanceMatch
                .withPrice(testGeneralPrice)
                .withPriceContext(testGeneralPrice)
                .withLastChangeTime(null)
                .withAutobudgetPriority(null)
                .withHrefParam1(null)
                .withHrefParam2(null);

        checkRelevanceMatchDbState(relevanceMatch);

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroupRelevanceMatch_switchOff() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .disableRelevanceMatch()
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(Collections.singletonList(
                        new GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)));

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(expectedPayload);

        checkKeywordDbState(mapList(Arrays.asList(keywordInfo), KeywordInfo::getKeyword));

        checkRelevanceMatchIsDeleted(Collections.singletonList(adGroupId));

        softAssertions.assertAll();
    }

}
