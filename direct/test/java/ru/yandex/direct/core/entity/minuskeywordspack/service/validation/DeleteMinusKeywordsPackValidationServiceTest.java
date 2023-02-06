package ru.yandex.direct.core.entity.minuskeywordspack.service.validation;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo;
import ru.yandex.direct.core.testing.repository.TestMinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.steps.MinusKeywordsPackSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.minusWordsPackNotFound;
import static ru.yandex.direct.core.testing.data.TestMinusKeywordsPacks.libraryMinusKeywordsPack;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.unableToDelete;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DeleteMinusKeywordsPackValidationServiceTest {

    @Autowired
    protected Steps steps;
    @Autowired
    private DeleteMinusKeywordsPackValidationService validationService;
    @Autowired
    private TestMinusKeywordsPackRepository testMinusKeywordsPackRepository;


    private int shard;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private ClientInfo otherClientInfo;
    private Long defaultLibraryPackId;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        defaultLibraryPackId = createLibraryPack(clientInfo);
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        otherClientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void validateDelete_OneValidPack_Success() {
        ValidationResult<List<Long>, Defect> validationResult = validate(defaultLibraryPackId);
        assertThat(validationResult).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateDelete_OneOtherClientPack_MinusWordsPackNotFound() {
        Long packIdOfOtherClient = createLibraryPack(otherClientInfo);

        ValidationResult<List<Long>, Defect> validationResult = validate(packIdOfOtherClient);

        assertThat(validationResult).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(0)), minusWordsPackNotFound()))));
    }

    @Test
    public void validateDelete_PrivatePack_MinusWordsPackNotFound() {
        Long privatePackId = createPrivatePack(clientInfo);

        ValidationResult<List<Long>, Defect> validationResult = validate(privatePackId);

        assertThat(validationResult).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(0)), minusWordsPackNotFound()))));
    }

    @Test
    public void validateDelete_PackLinkedToCampaign_UnableToDelete() {
        linkToAnyCampaign(defaultLibraryPackId);

        ValidationResult<List<Long>, Defect> validationResult = validate(defaultLibraryPackId);

        assertThat(validationResult).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(0)), unableToDelete()))));
    }

    @Test
    public void validateDelete_OneValidAndOneInvalid_MinusWordsPackNotFoundAndNoDefect() {
        Long packIdOfOtherClient = createLibraryPack(otherClientInfo);

        ValidationResult<List<Long>, Defect> validationResult = validate(packIdOfOtherClient, defaultLibraryPackId);

        assertThat(validationResult.getSubResults().get(index(1))).is(matchedBy(hasNoDefectsDefinitions()));
        assertThat(validationResult).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(0)), minusWordsPackNotFound()))));
    }

    @Test
    public void validateDelete_TwoInvalid_MinusWordsPackNotFoundAndUnableToDelete() {
        linkToAnyAdGroup(defaultLibraryPackId);
        Long packIdOfOtherClient = createLibraryPack(otherClientInfo);

        ValidationResult<List<Long>, Defect> validationResult = validate(packIdOfOtherClient, defaultLibraryPackId);

        assertThat(validationResult).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(0)), minusWordsPackNotFound()))));
        assertThat(validationResult).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(index(1)), unableToDelete()))));
    }

    private void linkToAnyAdGroup(Long packId) {
        Long adGroupId = steps.adGroupSteps().createDefaultAdGroup().getAdGroupId();
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToAdGroup(shard, packId, adGroupId);
    }

    private void linkToAnyCampaign(Long packId) {
        Long campaignId = steps.campaignSteps().createDefaultCampaign().getCampaignId();
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToCampaign(shard, packId, campaignId);
    }

    private ValidationResult<List<Long>, Defect> validate(Long... mwIds) {
        return validationService.validateDelete(shard, clientId, Arrays.asList(mwIds));
    }

    private Long createLibraryPack(ClientInfo clientInfo) {
        MinusKeywordsPackSteps packSteps = steps.minusKeywordsPackSteps();
        MinusKeywordsPackInfo pack = packSteps.createMinusKeywordsPack(libraryMinusKeywordsPack(), clientInfo);
        return pack.getMinusKeywordPackId();
    }

    private Long createPrivatePack(ClientInfo clientInfo) {
        MinusKeywordsPackSteps packSteps = steps.minusKeywordsPackSteps();
        MinusKeywordsPackInfo pack = packSteps.createPrivateMinusKeywordsPack(clientInfo);
        return pack.getMinusKeywordPackId();
    }
}
