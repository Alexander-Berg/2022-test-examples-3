package ru.yandex.direct.intapi.entity.showconditions.controller;


import java.math.BigDecimal;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.client.model.ClientLimits;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.showconditions.model.request.KeywordAddItem;
import ru.yandex.direct.intapi.entity.showconditions.model.request.KeywordModificationContainer;
import ru.yandex.direct.intapi.entity.showconditions.model.request.RetargetingItem;
import ru.yandex.direct.intapi.entity.showconditions.model.request.RetargetingModificationContainer;
import ru.yandex.direct.intapi.entity.showconditions.model.request.ShowConditionsRequest;
import ru.yandex.direct.intapi.entity.showconditions.model.response.ResponseItem;
import ru.yandex.direct.intapi.entity.showconditions.model.response.ShowConditionsResponse;
import ru.yandex.direct.pokazometer.PokazometerClient;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class ShowConditionsControllerModifyKeywordOversizeTest {

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private TestCampaignRepository campaignRepository;

    @Autowired
    private ShowConditionsController showConditionsController;

    @Autowired
    private Steps steps;

    @Autowired
    private PokazometerClient pokazometerClient;

    private long campaignId;
    private long adGroupId;
    private AdGroupInfo adGroupInfo;

    private int shard;
    private long uid;
    private long clientId;

    private static final String NEW_PHRASE = "new phrase";
    private static final BigDecimal DEFAULT_PRICE = BigDecimal.valueOf(10);


    @Before
    public void before() {
        KeywordInfo keywordInfo = steps.keywordSteps().createDefaultKeywordWithText("keyword phrase 1");
        adGroupInfo = keywordInfo.getAdGroupInfo();
        steps.relevanceMatchSteps().addDefaultRelevanceMatchToAdGroup(adGroupInfo);

        campaignId = adGroupInfo.getCampaignId();
        adGroupId = adGroupInfo.getAdGroupId();

        shard = adGroupInfo.getShard();
        uid = adGroupInfo.getUid();
        clientId = adGroupInfo.getClientId().asLong();


        updateKeywordLimit(1L);

        when(pokazometerClient.get(any())).thenReturn(new IdentityHashMap<>());
    }

    @Test
    public void update_AddKeywordsWithCopyOversize_SuccessOperation() {
        ShowConditionsResponse response = doUpdateWithCopyOversize(adGroupId, singletonList(keyword()));

        List<Keyword> keywords = keywordRepository.getKeywordsByAdGroupId(shard, adGroupId);
        assumeThat(keywords, hasSize(1));

        keywords = keywordRepository.getKeywordsByCampaignId(shard, campaignId);
        assumeThat(keywords, hasSize(2));

        assertCopiedFromPid(response, adGroupId);
    }

    @Test
    public void update_ComplexOperationNotValid_NewAdGroupsNotSavedInCampaign() {
        Map<Long, RetargetingModificationContainer> retargetingsMap = buildInvalidEditRetargetings(adGroupId);
        updateKeywordLimit(1L);

        ShowConditionsRequest request = buildAddRequest(adGroupId,
                singletonList(keyword()))
                .withRetargetings(retargetingsMap)
                .withCopyOversizeGroupFlag(true);

        Long expectedAdGroupsCount = adGroupsCountInCampaign(campaignId);
        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assumeFalse(response.isSuccessful());

        Long adGroupsCount = adGroupsCountInCampaign(campaignId);
        assertThat(adGroupsCount, is(expectedAdGroupsCount));
    }

    @Test
    public void update_AddInArchivedCampaign_CampaignStatusArchivedOnDelete() {
        archiveCampaign(campaignId, shard);

        ShowConditionsRequest request = buildAddRequest(adGroupId, singletonList(keyword()));
        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertThat(response.getItems().get(adGroupId).getErrors(), notNullValue());
        assertThat(response.getItems().get(adGroupId).getErrorsByPhrases(), not(emptyMap()));
    }

    @Test
    public void update_CopyOnOversizeRmpGroup_OperationFailed() {
        AdGroupInfo rmpAdGroup = steps.adGroupSteps().createActiveMobileContentAdGroup(adGroupInfo.getClientInfo());
        steps.keywordSteps().createKeywordWithText("keyword phrase 1", rmpAdGroup);
        updateKeywordLimit(1L);

        ShowConditionsRequest request = buildAddRequest(rmpAdGroup.getAdGroupId(), singletonList(keyword()))
                .withCopyOversizeGroupFlag(true);
        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertThat(response.getErrors(), not(empty()));
    }

    public KeywordAddItem keyword() {
        return new KeywordAddItem().withPhrase(NEW_PHRASE).withPrice(DEFAULT_PRICE).withPriceContext(DEFAULT_PRICE);
    }

    private ShowConditionsRequest buildAddRequest(Map<Long, List<KeywordAddItem>> items) {
        ShowConditionsRequest request = new ShowConditionsRequest();

        items.forEach((adGroupId, keywords) -> {
            KeywordModificationContainer container = new KeywordModificationContainer();
            container.getAdded().addAll(keywords);
            request.getKeywords().put(adGroupId, container);
        });

        return request;
    }

    private ShowConditionsRequest buildAddRequest(Long adGroupId, List<KeywordAddItem> items) {
        return buildAddRequest(singletonMap(adGroupId, items));
    }

    private ShowConditionsResponse doUpdateWithCopyOversize(Long adGroupId, List<KeywordAddItem> keywords) {
        return doUpdateWithCopyOversize(singletonMap(adGroupId, keywords));
    }

    private ShowConditionsResponse doUpdateWithCopyOversize(Map<Long, List<KeywordAddItem>> items) {
        ShowConditionsRequest request = buildAddRequest(items)
                .withCopyOversizeGroupFlag(true);
        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assumeTrue(response.isSuccessful());
        return response;
    }

    private long adGroupsCountInCampaign(long campaignId) {
        return adGroupRepository.getAdGroupCountByCampaignIds(shard, singletonList(campaignId))
                .get(campaignId);
    }

    private Map<Long, RetargetingModificationContainer> buildInvalidEditRetargetings(Long adGroupId) {
        RetargetingInfo retargetingInfo = steps.retargetingSteps().createDefaultRetargeting(adGroupInfo);

        RetargetingItem invalidRetargetingItem = new RetargetingItem().withPriceContext(BigDecimal.valueOf(100000));

        RetargetingModificationContainer retargetingContainer = new RetargetingModificationContainer()
                .withEdited(singletonMap(retargetingInfo.getRetargetingId(), invalidRetargetingItem));

        return singletonMap(adGroupId, retargetingContainer);
    }

    private void assertCopiedFromPid(ShowConditionsResponse response, Long... adGroupIds) {
        List<Long> copiedFromPid =
                filterList(mapList(response.getItems().values(), ResponseItem::getCopiedFromAdGroupId),
                        Objects::nonNull);

        assertThat(copiedFromPid, hasSize(adGroupIds.length));
        assertThat(copiedFromPid, containsInAnyOrder(adGroupIds));
    }

    private void updateKeywordLimit(long limit) {
        steps.clientSteps().updateClientLimits(adGroupInfo.getClientInfo()
                .withClientLimits(
                        (ClientLimits) new ClientLimits().withClientId(ClientId.fromLong(clientId))
                                .withKeywordsCountLimit(limit)));
    }

    private void archiveCampaign(long campaignId, int shard) {
        campaignRepository.archiveCampaign(shard, campaignId);
    }
}
