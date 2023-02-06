package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestAutobudgetAlertRepository;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationAdditionalTasksFreezeAutoBudgetAlertsTest extends KeywordsUpdateOperationBaseTest {

    @Autowired
    private TestAutobudgetAlertRepository testAutobudgetAlertRepository;

    @Test
    public void execute_NoChange_AlertNotFrozen() {
        createOneActiveAdGroup();
        testAutobudgetAlertRepository.addAutobudgetAlert(clientInfo.getShard(), adGroupInfo1.getCampaignId());
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        ModelChanges<Keyword> changesKeyword = keywordModelChanges(keywordIdToUpdate, PHRASE_1);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        testAutobudgetAlertRepository.assertAutobudgetAlertNotFrozen(clientInfo.getShard(),
                adGroupInfo1.getCampaignId());
    }

    @Test
    public void execute_ChangePhrase_AlertFrozen() {
        createOneActiveAdGroup();
        testAutobudgetAlertRepository.addAutobudgetAlert(clientInfo.getShard(), adGroupInfo1.getCampaignId());
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        ModelChanges<Keyword> changesKeyword = keywordModelChanges(keywordIdToUpdate, PHRASE_2);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));

        testAutobudgetAlertRepository.assertAutobudgetAlertFrozen(clientInfo.getShard(), adGroupInfo1.getCampaignId());
    }

    @Test
    public void execute_ChangeIsSuspended_AlertFrozen() {
        createOneActiveAdGroup();
        testAutobudgetAlertRepository.addAutobudgetAlert(clientInfo.getShard(), adGroupInfo1.getCampaignId());
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        ModelChanges<Keyword> changesKeyword = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(true, Keyword.IS_SUSPENDED);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1, true)));

        testAutobudgetAlertRepository.assertAutobudgetAlertFrozen(clientInfo.getShard(), adGroupInfo1.getCampaignId());
    }

    @Test
    public void execute_ChangeAutobudgetPriority_AlertFrozen() {
        createOneActiveAdGroup();
        testAutobudgetAlertRepository.addAutobudgetAlert(clientInfo.getShard(), adGroupInfo1.getCampaignId());
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        ModelChanges<Keyword> changesKeyword = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(5, Keyword.AUTOBUDGET_PRIORITY);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        testAutobudgetAlertRepository.assertAutobudgetAlertFrozen(clientInfo.getShard(), adGroupInfo1.getCampaignId());
    }

    @Test
    public void execute_OneAdGroupWithTwoKeywords_AlertFrozen() {
        createOneActiveAdGroup();
        testAutobudgetAlertRepository.addAutobudgetAlert(clientInfo.getShard(), adGroupInfo1.getCampaignId());
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2).getId();

        String firstPhrase = "слон";
        List<ModelChanges<Keyword>> changesKeywords =
                asList(keywordModelChanges(keywordIdToUpdate1, firstPhrase),
                        keywordModelChanges(keywordIdToUpdate2, PHRASE_3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, firstPhrase),
                isUpdated(keywordIdToUpdate2, PHRASE_3)));

        testAutobudgetAlertRepository.assertAutobudgetAlertFrozen(clientInfo.getShard(), adGroupInfo1.getCampaignId());
    }

    @Test
    public void execute_TwoAdGroupsInOneCampaign_AlertFrozen() {
        createTwoActiveAdGroups();
        testAutobudgetAlertRepository.addAutobudgetAlert(clientInfo.getShard(), adGroupInfo1.getCampaignId());
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_2).getId();

        String firstPhrase = "слон";
        List<ModelChanges<Keyword>> changesKeywords =
                asList(keywordModelChanges(keywordIdToUpdate1, firstPhrase),
                        keywordModelChanges(keywordIdToUpdate2, PHRASE_3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, firstPhrase),
                isUpdated(keywordIdToUpdate2, PHRASE_3)));

        testAutobudgetAlertRepository.assertAutobudgetAlertFrozen(clientInfo.getShard(), adGroupInfo1.getCampaignId());
    }

    @Test
    public void execute_TwoCampaignsOneAdGroupContainsInvalidKeyword_AlertNotFrozenForInvalid() {
        createOneActiveAdGroup();
        adGroupInfo2 = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());
        testAutobudgetAlertRepository.addAutobudgetAlert(clientInfo.getShard(), adGroupInfo1.getCampaignId());
        testAutobudgetAlertRepository.addAutobudgetAlert(clientInfo.getShard(), adGroupInfo2.getCampaignId());

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_2).getId();

        List<ModelChanges<Keyword>> changesKeywords =
                asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_3),
                        keywordModelChanges(keywordIdToUpdate2, INVALID_PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_3), null));

        testAutobudgetAlertRepository.assertAutobudgetAlertFrozen(clientInfo.getShard(), adGroupInfo1.getCampaignId());
        testAutobudgetAlertRepository.assertAutobudgetAlertNotFrozen(clientInfo.getShard(),
                adGroupInfo2.getCampaignId());
    }

    @Test
    public void execute_TwoCampaignsOneAdGroupContainsDuplicateKeyword_AlertNotFrozenForDuplicate() {
        createOneActiveAdGroup();
        adGroupInfo2 = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());
        testAutobudgetAlertRepository.addAutobudgetAlert(clientInfo.getShard(), adGroupInfo1.getCampaignId());
        testAutobudgetAlertRepository.addAutobudgetAlert(clientInfo.getShard(), adGroupInfo2.getCampaignId());

        Long existingKeywordId = createKeyword(adGroupInfo2, PHRASE_1).getId();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_2).getId();

        List<ModelChanges<Keyword>> changesKeywords =
                asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_3),
                        keywordModelChanges(keywordIdToUpdate2, PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_3),
                isNotUpdated(existingKeywordId, PHRASE_1)));

        testAutobudgetAlertRepository.assertAutobudgetAlertFrozen(clientInfo.getShard(), adGroupInfo1.getCampaignId());
        testAutobudgetAlertRepository.assertAutobudgetAlertNotFrozen(clientInfo.getShard(),
                adGroupInfo2.getCampaignId());
    }

}
