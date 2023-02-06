package ru.yandex.direct.core.entity.minuskeywordspack.service;

import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.MinusKeywordsPackSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.minusWordsPackNotFound;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;
import static ru.yandex.direct.operation.Applicability.PARTIAL;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.unableToDelete;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MinusKeywordsPacksDeleteOperationTest {

    private static final String MUST_BE_DELETED = "Набор должен быть удален из бд";
    private static final String CANNOT_DELETE_LINKED_PACK = "Нельзя удалять привязанный набор";
    private static final String CANNOT_DELETE_PACK_OF_ANOTHER_USER = "Нельзя удалять набор другого пользователя";

    @Autowired
    protected Steps steps;
    @Autowired
    private MinusKeywordsPacksDeleteOperationFactory factory;
    @Autowired
    private TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;

    private int shard;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private ClientInfo otherClientInfo;
    private Long defaultPackId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        otherClientInfo = steps.clientSteps().createDefaultClient();

        defaultPackId = createLibraryPack(clientInfo);
    }

    @Test
    public void execute_OneValidPack_Success() {
        MassResult<Long> result = executeDeleteOperation(defaultPackId);

        assertThat(result, isSuccessful(true));
        assertDeleting(defaultPackId, MUST_BE_DELETED, is(emptyList()));
    }

    @Test
    public void execute_SeveralValidPack_AllSuccess() {
        Long packId2 = createLibraryPack(clientInfo);

        MassResult<Long> result = executeDeleteOperation(defaultPackId, packId2);

        assertThat(result, isSuccessful(true, true));
        assertDeleting(defaultPackId, MUST_BE_DELETED, is(emptyList()));
        assertDeleting(packId2, MUST_BE_DELETED, is(emptyList()));
    }

    @Test
    public void execute_OnePackOfAnotherClient_MinusWordsPackNotFound() {
        Long packIdOfOtherClient = createLibraryPack(otherClientInfo);

        MassResult<Long> result = executeDeleteOperation(packIdOfOtherClient);

        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0)), minusWordsPackNotFound())));
        assertDeleting(packIdOfOtherClient, CANNOT_DELETE_PACK_OF_ANOTHER_USER, not(emptyList()));
    }

    @Test
    public void execute_DeletingAlreadyDeleted_MinusWordsPackNotFound() {
        MassResult<Long> result = executeDeleteOperation(defaultPackId);
        checkState(result.get(0).isSuccessful());


        MassResult<Long> reDeleteResult = executeDeleteOperation(defaultPackId);

        assertThat(reDeleteResult.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0)), minusWordsPackNotFound())));
        assertDeleting(defaultPackId, MUST_BE_DELETED, is(emptyList()));
    }

    @Test
    public void execute_OneValidAndOneInvalid_MinusWordsPackNotFoundAndNoDefect() {
        Long packIdOfOtherClient = createLibraryPack(otherClientInfo);

        MassResult<Long> result = executeDeleteOperation(packIdOfOtherClient, defaultPackId);

        assertThat(result, isSuccessful(false, true));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0)), minusWordsPackNotFound())));

        assertDeleting(packIdOfOtherClient, CANNOT_DELETE_PACK_OF_ANOTHER_USER, not(emptyList()));
        assertDeleting(defaultPackId, MUST_BE_DELETED, is(emptyList()));
    }

    @Test
    public void execute_TwoInvalid_MinusWordsPackNotFoundAndUnableToDelete() {
        linkToAnyAdGroup(defaultPackId);
        Long packIdOfOtherClient = createLibraryPack(otherClientInfo);

        MassResult<Long> result = executeDeleteOperation(packIdOfOtherClient, defaultPackId);

        assertThat(result, isSuccessful(false, false));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(0)), minusWordsPackNotFound())));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(path(index(1)), unableToDelete())));

        assertDeleting(packIdOfOtherClient, CANNOT_DELETE_PACK_OF_ANOTHER_USER, not(emptyList()));
        assertDeleting(defaultPackId, CANNOT_DELETE_LINKED_PACK, not(emptyList()));
    }

    private void assertDeleting(Long packId, String errorMessage, Matcher<List> matcher) {
        List<MinusKeywordsPack> actualPacks = testMinusKeywordsPackRepository.get(shard, singleton(packId));
        assertThat(errorMessage, actualPacks, matcher);
    }

    private void linkToAnyAdGroup(Long packId) {
        Long adGroupId = steps.adGroupSteps().createDefaultAdGroup().getAdGroupId();
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(shard, packId, adGroupId);
    }

    private MassResult<Long> executeDeleteOperation(Long... mwIds) {
        MinusKeywordsPacksDeleteOperation operation = factory.newInstance(PARTIAL, asList(mwIds), clientId, shard);
        return operation.prepareAndApply();
    }

    private Long createLibraryPack(ClientInfo clientInfo) {
        MinusKeywordsPackSteps packSteps = steps.minusKeywordsPackSteps();
        MinusKeywordsPackInfo pack = packSteps.createMinusKeywordsPack(libraryMinusKeywordsPack(), clientInfo);
        return pack.getMinusKeywordPackId();
    }
}
