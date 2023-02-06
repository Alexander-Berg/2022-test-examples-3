package ru.yandex.direct.grid.processing.service.minuskeywordspack;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.minuskeywordspack.GdGetMinusKeywordsPacks;
import ru.yandex.direct.grid.processing.model.minuskeywordspack.GdGetMinusKeywordsPacksItem;
import ru.yandex.direct.grid.processing.model.minuskeywordspack.GdGetMinusKeywordsPacksPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class MinusKeywordsPackGraphQlServiceGetLibraryMinusKeywordsPacksTest {

    private static final String QUERY_HANDLE = "getLibraryMinusKeywordsPacks";
    private static final String QUERY_TEMPLATE = ""
            + "query {\n"
            + "  %s(input: %s) {\n"
            + "    rowset{\n"
            + "      id\n"
            + "      name\n"
            + "      minusKeywords\n"
            + "      linkedAdGroupsCount\n"
            + "    }\n"
            + "    cacheKey\n"
            + "    totalCount\n"
            + "  }\n"
            + "}\n";

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;

    private ClientInfo clientInfo;
    private User operator;

    private GdGetMinusKeywordsPacks input;
    private List<GdGetMinusKeywordsPacksItem> allExpectedPacks;

    @Before
    public void initTestData() {
        clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
        input = new GdGetMinusKeywordsPacks();

        allExpectedPacks = createPacksAndGetExpectedList();
    }

    private List<GdGetMinusKeywordsPacksItem> createPacksAndGetExpectedList() {
        // частный набор и библиотечный набор другого клиента
        steps.minusKeywordsPackSteps().createPrivateMinusKeywordsPack(clientInfo);
        createNewLibraryPack(new ClientInfo());

        MinusKeywordsPack notLinkedLibraryPack = createNewLibraryPack(clientInfo);

        MinusKeywordsPack linkedLibraryPack1 = createNewLibraryPack(clientInfo);
        linkPackToNewAdGroup(linkedLibraryPack1.getId());

        MinusKeywordsPack linkedLibraryPack2 = createNewLibraryPack(clientInfo);
        linkPackToNewAdGroup(linkedLibraryPack2.getId());
        linkPackToNewAdGroup(linkedLibraryPack2.getId());

        return asList(convertToGdItem(notLinkedLibraryPack, 0L),
                convertToGdItem(linkedLibraryPack1, 1L),
                convertToGdItem(linkedLibraryPack2, 2L));
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void getLibraryMinusKeywordsPacks_WithFilter_EmptyResult() {
        long nonExistentId = RandomUtils.nextLong(100L, Long.MAX_VALUE);
        input.setPackIdIn(singleton(nonExistentId));
        GdGetMinusKeywordsPacksPayload payload = getLibraryMinusKeywordsPacksGraphQl(input);
        assertThat(payload.getRowset()).isEmpty();
        assertThat(payload.getTotalCount()).isZero();
    }

    @Test
    public void getLibraryMinusKeywordsPacks_WithoutFilters_FoundAllPacks() {
        GdGetMinusKeywordsPacksPayload payload = getLibraryMinusKeywordsPacksGraphQl(input);

        List<GdGetMinusKeywordsPacksItem> actualRowset = payload.getRowset();
        sortRowset(actualRowset);

        assertThat(actualRowset).isEqualTo(allExpectedPacks);
        assertThat(payload.getTotalCount()).isEqualTo(allExpectedPacks.size());
    }

    @Test
    public void getLibraryMinusKeywordsPacks_PackIdFilter_PackFound() {
        GdGetMinusKeywordsPacksItem expectedItem = allExpectedPacks.get(1);
        input.setPackIdIn(singleton(expectedItem.getId()));

        List<GdGetMinusKeywordsPacksItem> actualRowset = getLibraryMinusKeywordsPacksGraphQl(input).getRowset();

        assertThat(actualRowset).isEqualTo(singletonList(expectedItem));
    }

    private MinusKeywordsPack createNewLibraryPack(ClientInfo clientInfo) {
        return steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack(clientInfo).getMinusKeywordsPack();
    }

    private void linkPackToNewAdGroup(Long packId) {
        Long adGroupId = steps.adGroupSteps().createActiveTextAdGroup(clientInfo).getAdGroupId();
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(clientInfo.getShard(), packId, adGroupId);
    }

    private GdGetMinusKeywordsPacksItem convertToGdItem(MinusKeywordsPack pack, Long linksCount) {
        return new GdGetMinusKeywordsPacksItem()
                .withId(pack.getId())
                .withName(pack.getName())
                .withMinusKeywords(pack.getMinusKeywords())
                .withLinkedAdGroupsCount(linksCount);
    }

    private void sortRowset(List<GdGetMinusKeywordsPacksItem> rowset) {
        rowset.sort(Comparator.comparingLong(GdGetMinusKeywordsPacksItem::getId));
    }

    private GdGetMinusKeywordsPacksPayload getLibraryMinusKeywordsPacksGraphQl(GdGetMinusKeywordsPacks input) {
        String query = String.format(QUERY_TEMPLATE, QUERY_HANDLE, graphQlSerialize(input));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(QUERY_HANDLE);

        return GraphQlJsonUtils.convertValue(data.get(QUERY_HANDLE), GdGetMinusKeywordsPacksPayload.class);
    }
}
