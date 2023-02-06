package ru.yandex.direct.grid.processing.service.minuskeywordspack;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import graphql.ExecutionResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.minuskeywordspack.mutation.GdDeleteMinusKeywordsPacks;
import ru.yandex.direct.grid.processing.model.minuskeywordspack.mutation.GdDeleteMinusKeywordsPacksPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.minusWordsPackNotFound;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.model.minuskeywordspack.mutation.GdDeleteMinusKeywordsPacks.MINUS_KEYWORD_PACK_IDS;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.defect.CommonDefects.unableToDelete;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class MinusKeywordsPackGraphQlServiceDeleteMinusKeywordsPacksTest {

    private static final String MUTATION_HANDLE = "deleteMinusKeywordsPacks";
    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s(input: %s) {\n"
            + "    deletedPackIds\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "      warnings {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
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

    private int shard;
    private User operator;

    private Long otherClientPackId;
    private Long currentClientPackId1;
    private Long currentClientPackId2;
    private Long privatePackId;
    private Long linkedPackId;

    @Before
    public void initTestData() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        operator = UserHelper.getUser(clientInfo.getClient());
        shard = clientInfo.getShard();

        ClientInfo otherClientInfo = steps.clientSteps().createDefaultClient();

        otherClientPackId = createLibraryPack(otherClientInfo);
        currentClientPackId1 = createLibraryPack(clientInfo);
        currentClientPackId2 = createLibraryPack(clientInfo);
        linkedPackId = createLibraryPack(clientInfo);
        linkToAnyAdGroup(linkedPackId);
        privatePackId =
                steps.minusKeywordsPackSteps().createPrivateMinusKeywordsPack(clientInfo).getMinusKeywordPackId();

        TestAuthHelper.setDirectAuthentication(operator);
    }

    @After
    public void afterTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void deleteMinusKeywordsPacks_OwnLibraryPacks_Success() {
        List<Long> mwIds = asList(currentClientPackId1, currentClientPackId2);

        GdDeleteMinusKeywordsPacksPayload payload = deleteMinusKeywordsPacksGraphQl(mwIds);
        List<Long> existingPackIds = testMinusKeywordsPackRepository.getExistingPackIds(shard, mwIds);

        assertSoftly(assertions -> {
            assertions.assertThat(payload.getValidationResult()).isNull();
            assertions.assertThat(existingPackIds).isEmpty();
        });
    }

    @Test
    public void deleteMinusKeywordsPacks_OneInvalidPack_MinusWordsPackNotFound() {
        List<Long> mwIds = singletonList(otherClientPackId);

        GdDeleteMinusKeywordsPacksPayload payload = deleteMinusKeywordsPacksGraphQl(mwIds);
        List<Long> existingPackIds = testMinusKeywordsPackRepository.getExistingPackIds(shard, mwIds);

        GdDefect gdDefect1 = getGdDefect(0, minusWordsPackNotFound());
        GdValidationResult expectedGdValidationResult = toGdValidationResult(gdDefect1);

        assertSoftly(assertions -> {
            assertions.assertThat(payload.getValidationResult()).is(matchedBy(beanDiffer(expectedGdValidationResult)));
            assertions.assertThat(existingPackIds).containsOnly(otherClientPackId);
        });
    }

    @Test
    public void deleteMinusKeywordsPacks_LinkedAndPrivateAndValidAndOtherClientPack_PartiallyDelete() {
        List<Long> mwIds = asList(linkedPackId, privatePackId, currentClientPackId1, otherClientPackId);

        GdDeleteMinusKeywordsPacksPayload payload = deleteMinusKeywordsPacksGraphQl(mwIds);
        List<Long> existingPackIds = testMinusKeywordsPackRepository.getExistingPackIds(shard, mwIds);

        GdDefect gdDefect1 = getGdDefect(0, unableToDelete());
        GdDefect gdDefect2 = getGdDefect(1, minusWordsPackNotFound());
        GdDefect gdDefect3 = getGdDefect(3, minusWordsPackNotFound());

        GdValidationResult expectedGdValidationResult = toGdValidationResult(gdDefect1, gdDefect2, gdDefect3);

        assertSoftly(assertions -> {
            assertions.assertThat(payload.getValidationResult()).is(matchedBy(beanDiffer(expectedGdValidationResult)));
            assertions.assertThat(existingPackIds).containsOnly(linkedPackId, privatePackId, otherClientPackId);
        });
    }

    private GdDefect getGdDefect(int i, Defect defect) {
        return toGdDefect(path(field(MINUS_KEYWORD_PACK_IDS), index(i)), defect);
    }

    private Long createLibraryPack(ClientInfo client) {
        return steps.minusKeywordsPackSteps().createLibraryMinusKeywordsPack(client).getMinusKeywordPackId();
    }

    private void linkToAnyAdGroup(Long packId) {
        Long adGroupId = steps.adGroupSteps().createDefaultAdGroup().getAdGroupId();
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(shard, packId, adGroupId);
    }

    private GdDeleteMinusKeywordsPacksPayload deleteMinusKeywordsPacksGraphQl(List<Long> mwIds) {
        GdDeleteMinusKeywordsPacks request = new GdDeleteMinusKeywordsPacks().withMinusKeywordPackIds(mwIds);

        String query = String.format(MUTATION_TEMPLATE, MUTATION_HANDLE, graphQlSerialize(request));
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors())
                .isEmpty();

        Map<String, Object> data = result.getData();
        assertThat(data).containsOnlyKeys(MUTATION_HANDLE);

        return GraphQlJsonUtils.convertValue(data.get(MUTATION_HANDLE), GdDeleteMinusKeywordsPacksPayload.class);
    }
}
