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

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
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
public class UpdateAdGroupMutationRelevanceMatchAddTest extends UpdateAdGroupMutationBaseTest {

    @Before
    public void init() {
        super.initTestData();
    }

    @Test
    public void checkUpdateAdGroupRelevanceMatch_add() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .setGeneralPrice(BigDecimal.ONE)
                .setRelevanceMatch(true, null)
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

        RelevanceMatch expectedNewRelevanceMatch = new RelevanceMatch()
                .withAdGroupId(adGroupId)
                .withCampaignId(textAdGroupInfo.getCampaignId())
                .withIsDeleted(false)
                .withIsSuspended(false)
                .withStatusBsSynced(StatusBsSynced.NO);

        checkNewRelevanceMatch(adGroupId, expectedNewRelevanceMatch);

        softAssertions.assertAll();
    }

    @Test
    public void checkUpdateAdGroupRelevanceMatch_stayOff() {
        GdUpdateTextAdGroup request = requestBuilder()
                .getRequestFromOriginalAdGroupParams()
                .build();

        ExecutionResult result = processor.processQuery(null, getQuery(request), null, buildContext(operator));
        softAssertions.assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        softAssertions.assertThat(data)
                .containsOnlyKeys(MUTATION_NAME);

        GdUpdateAdGroupPayload payload =
                GraphQlJsonUtils.convertValue(data.get(MUTATION_NAME), GdUpdateAdGroupPayload.class);
        softAssertions.assertThat(payload)
                .isEqualToComparingFieldByFieldRecursively(payload);

        checkKeywordDbState(mapList(Arrays.asList(keywordInfo), KeywordInfo::getKeyword));

        checkRelevanceMatchIsDeleted(Collections.singletonList(textAdGroupInfo.getAdGroupId()));

        softAssertions.assertAll();
    }

}
