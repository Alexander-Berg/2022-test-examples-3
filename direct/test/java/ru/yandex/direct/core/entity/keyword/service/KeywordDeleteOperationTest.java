package ru.yandex.direct.core.entity.keyword.service;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.log.service.LogPriceService;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.keyword.container.CampaignIdAndKeywordIdPair;
import ru.yandex.direct.core.entity.keyword.container.KeywordDeleteInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.entity.keyword.service.validation.DeleteKeywordValidationService;
import ru.yandex.direct.core.entity.mailnotification.model.KeywordEvent;
import ru.yandex.direct.core.entity.mailnotification.model.MailNotificationEvent;
import ru.yandex.direct.core.entity.mailnotification.service.MailNotificationEventService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.repository.TestMailNotificationEventRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.adgroup.model.StatusModerate.NEW;
import static ru.yandex.direct.core.entity.adgroup.model.StatusModerate.READY;
import static ru.yandex.direct.core.entity.mailnotification.model.KeywordEvent.deletedKeywordEvent;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class KeywordDeleteOperationTest extends KeywordsBaseTest {

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private MailNotificationEventService mailNotificationEventService;

    @Autowired
    private TestMailNotificationEventRepository testMailNotificationEventRepository;

    @Autowired
    private DeleteKeywordValidationService deleteKeywordValidationService;

    @Autowired
    private LogPriceService logPriceService;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private Steps steps;

    private KeywordInfo keyword;
    private long operatorUid;
    private ClientId clientId;
    private int shard;

    @Before
    public void before() {
        super.before();
        adGroupInfo1 = steps.adGroupSteps()
                .createAdGroup(defaultTextAdGroup(null).withLastChange(LocalDateTime.now().minusHours(3L)));
        keyword = createFullKeywordAndCheckRecordsPresentInAllTables(adGroupInfo1, PHRASE_1);
        operatorUid = keyword.getAdGroupInfo().getUid();
        clientId = keyword.getAdGroupInfo().getClientId();
        shard = keyword.getShard();
    }

    @Test
    public void prepareAndApply_InvalidId() {
        KeywordDeleteOperation keywordDeleteOperation = createKeywordDeleteOperation(asList(keyword.getId(), -1L));
        MassResult<Long> result = keywordDeleteOperation.prepareAndApply();
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(path(index(1)), validId())));
    }

    @Test
    public void prepareAndApply_CheckKeywordDeleted() {
        long id = keyword.getId();
        KeywordDeleteOperation keywordDeleteOperation = createKeywordDeleteOperation(singletonList(id));
        MassResult<Long> result = keywordDeleteOperation.prepareAndApply();
        assumeThat(result, isFullySuccessful());
        assertThat(isBidsBaseRecordNotPresent(keyword), is(true));
        assertThat(isBidsRecordPresent(keyword), is(false));
        assertThat(isBidsHrefParamsRecordPresent(keyword), is(false));
        assertThat(isBidsPhraseIdHistoryRecordPresent(keyword), is(false));
        assertThat(isBidsManualPricesRecordPresent(keyword), is(false));
    }

    /**
     * Проверяем, что при заданных существующих ключевых словах, id валидируются с их помощью,
     * а не с помощью слов из базы
     */
    @Test
    public void prepareAndApply_CheckWorkWithExistingKeywords() {
        KeywordInfo keyword2 = steps.keywordSteps().createKeyword(keyword.getAdGroupInfo());
        KeywordDeleteOperation keywordDeleteOperation = createKeywordDeleteOperation(
                asList(keyword.getId(), keyword2.getId()), singletonList(keyword2.getKeyword()));
        MassResult<Long> result = keywordDeleteOperation.prepareAndApply();
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), objectNotFound())));
    }

    @Test
    public void prepareAndApply_CheckAdGroupUpdated() {
        KeywordDeleteOperation keywordDeleteOperation = createKeywordDeleteOperation(singletonList(keyword.getId()));
        AdGroup adGroupBefore = adGroupRepository.getAdGroups(shard, singletonList(keyword.getAdGroupId())).get(0);
        MassResult<Long> result = keywordDeleteOperation.prepareAndApply();
        assumeThat(result, isFullySuccessful());
        AdGroup adGroupAfter = adGroupRepository.getAdGroups(shard, singletonList(keyword.getAdGroupId())).get(0);
        checkAdGroupUpdated(adGroupBefore, adGroupAfter);
    }

    @Test
    public void prepareAndApply_emptyAdGroupsNotModerated_CheckAdGroupUpdated() {
        Long adGroupId = keyword.getAdGroupId();
        AdGroup adGroupBefore = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        AppliedChanges<AdGroup> appliedChanges = ModelChanges
                .build(adGroupId, AdGroup.class, AdGroup.STATUS_POST_MODERATE, StatusPostModerate.NO)
                .applyTo(adGroupBefore);
        adGroupRepository.updateAdGroups(shard, clientId, singleton(appliedChanges));
        KeywordDeleteOperation keywordDeleteOperation = createKeywordDeleteOperation(singletonList(keyword.getId()));
        MassResult<Long> result = keywordDeleteOperation.prepareAndApply();
        assumeThat(result, isFullySuccessful());
        AdGroup adGroupAfter = adGroupRepository.getAdGroups(shard, singletonList(keyword.getAdGroupId())).get(0);
        checkAdGroupUpdated(adGroupBefore, adGroupAfter);
    }

    @Test
    public void prepareAndApply_emptyAdGroupsModerated_CheckAdGroupNotUpdated() {
        Long adGroupId = keyword.getAdGroupId();
        AdGroup adGroupBefore = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        assumeThat(adGroupBefore.getStatusPostModerate(), is(StatusPostModerate.YES));
        KeywordDeleteOperation keywordDeleteOperation = createKeywordDeleteOperation(singletonList(keyword.getId()));
        MassResult<Long> result = keywordDeleteOperation.prepareAndApply();
        assumeThat(result, isFullySuccessful());
        AdGroup adGroupAfter = adGroupRepository.getAdGroups(shard, singletonList(keyword.getAdGroupId())).get(0);
        checkAdGroupUpdated(adGroupBefore, adGroupAfter);
    }

    @Test
    public void prepareAndApply_CheckDraftAdGroupUpdated() {
        Long adGroupId = keyword.getAdGroupId();
        AdGroup adGroupBefore = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        AppliedChanges<AdGroup> appliedChanges = ModelChanges
                .build(adGroupId, AdGroup.class, AdGroup.STATUS_MODERATE, NEW)
                .applyTo(adGroupBefore);
        adGroupRepository.updateAdGroups(shard, clientId, singleton(appliedChanges));
        KeywordDeleteOperation keywordDeleteOperation = createKeywordDeleteOperation(singletonList(keyword.getId()));
        MassResult<Long> result = keywordDeleteOperation.prepareAndApply();
        assumeThat(result, isFullySuccessful());
        AdGroup adGroupAfter = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        checkAdGroupUpdated(adGroupBefore, adGroupAfter);
    }

    @Test
    public void prepareAndApply_AdGroupsUpdatedOnlyForValidIds() {
        AdGroupInfo adGroupWithDeletedKeyword = steps.adGroupSteps().createActiveTextAdGroup();
        Long deletedKeywordId = createAndDeleteKeyword(adGroupWithDeletedKeyword);

        AdGroup adGroupBeforeWithInvalidKeyword =
                adGroupRepository.getAdGroups(adGroupWithDeletedKeyword.getShard(),
                        singletonList(adGroupWithDeletedKeyword.getAdGroupId())
                ).get(0);
        AdGroup adGroupBefore = adGroupRepository
                .getAdGroups(adGroupWithDeletedKeyword.getShard(), singletonList(keyword.getAdGroupId())
                ).get(0);
        KeywordDeleteOperation keywordDeleteOperation =
                createKeywordDeleteOperation(asList(keyword.getId(), deletedKeywordId));
        MassResult<Long> result = keywordDeleteOperation.prepareAndApply();
        assumeThat(result, isSuccessfulWithMatchers(equalTo(keyword.getId()), null));
        AdGroup adGroupAfterWithInvalidKeyword =
                adGroupRepository.getAdGroups(adGroupWithDeletedKeyword.getShard(),
                        singletonList(adGroupWithDeletedKeyword.getAdGroupId())
                ).get(0);
        AdGroup adGroupAfter = adGroupRepository
                .getAdGroups(adGroupWithDeletedKeyword.getShard(), singletonList(keyword.getAdGroupId())
                ).get(0);
        assertThat("группа с невалидной кф не изменилась", adGroupAfterWithInvalidKeyword,
                beanDiffer(adGroupBeforeWithInvalidKeyword));
        //а с валидной изменилась
        checkAdGroupUpdated(adGroupBefore, adGroupAfter);
    }

    @Test
    public void prepareAndApply_ByManagerRole_NotificationsAdded() {
        var managerClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);
        clientInfo = steps.clientSteps().createDefaultClientUnderManager(managerClientInfo);
        operatorUid = managerClientInfo.getUid();
        clientId = clientInfo.getClientId();
        var adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withManagerUid(managerClientInfo.getUid())));
        keyword = createFullKeywordAndCheckRecordsPresentInAllTables(adGroupInfo, PHRASE_1);

        KeywordDeleteOperation keywordDeleteOperation = createKeywordDeleteOperation(singletonList(keyword.getId()));
        MassResult<Long> result = keywordDeleteOperation.prepareAndApply();
        assumeThat(result, isFullySuccessful());

        KeywordEvent<String> expectedEvent = getExpectedEvent(keyword);
        List<MailNotificationEvent> events = testMailNotificationEventRepository
                .getEventsByOwnerUids(shard, singletonList(keyword.getAdGroupInfo().getUid()));
        assertThat(events, hasSize(1));
        assertThat(events.get(0), beanDiffer(expectedEvent));
    }

    @Test
    public void prepareAndApply_NotificationsAddedOnlyForValidIds() {
        var managerClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);
        clientInfo = steps.clientSteps().createDefaultClientUnderManager(managerClientInfo);
        operatorUid = managerClientInfo.getUid();
        clientId = clientInfo.getClientId();
        var adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(activeTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withManagerUid(managerClientInfo.getUid())));
        keyword = createFullKeywordAndCheckRecordsPresentInAllTables(adGroupInfo, PHRASE_1);

        Long deletedKeywordId = createAndDeleteKeyword(keyword.getAdGroupInfo());

        KeywordDeleteOperation keywordDeleteOperation =
                createKeywordDeleteOperation(asList(keyword.getId(), deletedKeywordId));
        MassResult<Long> result = keywordDeleteOperation.prepareAndApply();
        assumeThat(result, isSuccessfulWithMatchers(equalTo(keyword.getId()), null));

        KeywordEvent<String> expectedEvent = getExpectedEvent(keyword);
        List<MailNotificationEvent> events = testMailNotificationEventRepository
                .getEventsByOwnerUids(shard, singletonList(keyword.getAdGroupInfo().getUid()));
        assertThat("только 1 событие создалось", events, hasSize(1));
        assertThat("создалось событие только для валидной кф", events.get(0), beanDiffer(expectedEvent));
    }

    @Test
    public void prepareAndApply_CheckCampaignsUpdated() {
        KeywordInfo keywordInfo = getKeywordInfoForCampaignChecks();
        KeywordDeleteOperation keywordDeleteOperation =
                createKeywordDeleteOperation(singletonList(keywordInfo.getId()));
        MassResult<Long> result = keywordDeleteOperation.prepareAndApply();
        assumeThat(result, isFullySuccessful());

        List<Campaign> campaigns = campaignRepository.getCampaigns(shard, singletonList(keywordInfo.getCampaignId()));
        assumeThat(campaigns, hasSize(1));
        Campaign campaignAfter = campaigns.get(0);
        assertThat(campaignAfter.getAutobudgetForecastDate(), nullValue());
    }

    @Test
    public void prepareAndApply_CheckCampaignsUpdatedOnlyForValidIds() {
        KeywordInfo validKeyword = getKeywordInfoForCampaignChecks();
        KeywordInfo keywordForDelete = getKeywordInfoForCampaignChecks();
        deleteKeyword(keywordForDelete);

        KeywordDeleteOperation keywordDeleteOperation =
                createKeywordDeleteOperation(asList(validKeyword.getId(), keywordForDelete.getId()));
        MassResult<Long> result = keywordDeleteOperation.prepareAndApply();
        assumeThat(result, isSuccessfulWithMatchers(equalTo(validKeyword.getId()), null));


        List<Campaign> campaigns = campaignRepository.getCampaigns(shard,
                asList(validKeyword.getCampaignId(), keywordForDelete.getCampaignId()));
        assumeThat(campaigns, hasSize(2));

        Campaign campaignWithRemoved = campaigns.get(0);
        Campaign campaignWithoutRemoved = campaigns.get(1);

        assertThat(campaignWithoutRemoved.getAutobudgetForecastDate(), notNullValue());
        assertThat(campaignWithRemoved.getAutobudgetForecastDate(), nullValue());
    }

    private KeywordDeleteOperation createKeywordDeleteOperation(List<Long> ids) {
        return new KeywordDeleteOperation(Applicability.PARTIAL, ids, keywordRepository, adGroupRepository,
                campaignRepository,
                mailNotificationEventService, deleteKeywordValidationService, logPriceService, dslContextProvider,
                operatorUid, clientId, shard);
    }

    private KeywordDeleteOperation createKeywordDeleteOperation(List<Long> ids, List<Keyword> keywords) {
        return new KeywordDeleteOperation(Applicability.PARTIAL, ids, keywordRepository, adGroupRepository,
                campaignRepository,
                mailNotificationEventService, deleteKeywordValidationService, logPriceService, dslContextProvider,
                operatorUid, clientId, shard, keywords);
    }

    /**
     * Создает ключевую фразу в группе и удаляет ее.
     *
     * @param adGroupInfo группа, в которой надо создать и удалить ключевую фразу
     * @return id удаленной ключевой фразы
     */
    private Long createAndDeleteKeyword(AdGroupInfo adGroupInfo) {
        KeywordInfo keywordForDelete = steps.keywordSteps().createKeyword(adGroupInfo);
        deleteKeyword(keywordForDelete);
        return keywordForDelete.getId();
    }

    private void deleteKeyword(KeywordInfo keywordInfo) {
        keywordRepository.deleteKeywords(dslContextProvider.ppc(shard).configuration(),
                singletonList(new CampaignIdAndKeywordIdPair(keywordInfo.getCampaignId(), keywordInfo.getId())));
    }

    private KeywordEvent<String> getExpectedEvent(KeywordInfo keyword) {
        AdGroupInfo adGroup = keyword.getAdGroupInfo();
        KeywordDeleteInfo keywordDeleteInfo = new KeywordDeleteInfo()
                .withCampaignId(adGroup.getCampaignId())
                .withOwnerUid(adGroup.getUid())
                .withAdGroupId(adGroup.getAdGroupId())
                .withAdGroupName(adGroup.getAdGroup().getName());
        return deletedKeywordEvent(operatorUid, keyword.getAdGroupId(), keywordDeleteInfo,
                keyword.getKeyword().getPhrase());
    }

    private void checkAdGroupUpdated(AdGroup beforeOperation, AdGroup afterOperation) {
        assertThat(afterOperation.getStatusBsSynced(), is(StatusBsSynced.NO));
        assertThat(afterOperation.getLastChange(), LocalDateTimeMatcher.isAfter(beforeOperation.getLastChange()));
        if (beforeOperation.getStatusModerate() != NEW
                && beforeOperation.getStatusPostModerate() != StatusPostModerate.YES) {
            assertThat(afterOperation.getStatusModerate(), is(READY));
            assertThat(afterOperation.getStatusPostModerate(), is(StatusPostModerate.REJECTED));
        } else {
            assertThat(afterOperation.getStatusModerate(), is(beforeOperation.getStatusModerate()));
            assertThat(afterOperation.getStatusPostModerate(), is(beforeOperation.getStatusPostModerate()));
        }
    }

    private KeywordInfo getKeywordInfoForCampaignChecks() {
        CampaignInfo campaign =
                steps.campaignSteps().createCampaign(newTextCampaign(clientId, operatorUid)
                        .withAutobudgetForecastDate(LocalDateTime.now()), keyword.getAdGroupInfo().getClientInfo());
        AdGroupInfo defaultAdGroup = steps.adGroupSteps().createDefaultAdGroup(campaign);
        return steps.keywordSteps().createKeyword(defaultAdGroup);
    }
}
