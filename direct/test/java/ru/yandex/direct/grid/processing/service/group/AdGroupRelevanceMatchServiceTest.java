package ru.yandex.direct.grid.processing.service.group;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.BigDecimalComparator.BIG_DECIMAL_COMPARATOR;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.CAMPAIGN_TYPE_NOT_SUPPORTED;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupRelevanceMatchServiceTest {

    @Autowired
    Steps steps;
    @Autowired
    AdGroupRepository adGroupRepository;
    @Autowired
    AdGroupMutationService adGroupMutationService;
    @Autowired
    AdGroupRelevanceMatchService adGroupRelevanceMatchService;
    @Autowired
    RelevanceMatchRepository relevanceMatchRepository;

    private ClientId clientId;
    private Long uid;
    private Long adGroupId;
    private Long campaignId;
    private ClientInfo clientInfo;
    private AdGroupInfo adGroupWithEnabledRelevanceMatch;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createClient(defaultClient().withCountryRegionId(Region.RUSSIA_REGION_ID));

        uid = clientInfo.getUid();
        clientId = clientInfo.getClientId();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        adGroupId = adGroupInfo.getAdGroupId();
        campaignId = adGroupInfo.getCampaignId();

        adGroupWithEnabledRelevanceMatch = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        RelevanceMatch relevanceMatch =
                steps.relevanceMatchSteps().getDefaultRelevanceMatch(adGroupWithEnabledRelevanceMatch);
        steps.relevanceMatchSteps()
                .addRelevanceMatchToAdGroup(List.of(relevanceMatch), adGroupWithEnabledRelevanceMatch);
    }

    @Test
    public void enableRelevanceMatch_AllSuccess() {
        List<Long> adGroupIds = List.of(adGroupId, adGroupWithEnabledRelevanceMatch.getAdGroupId());
        ValidationResult<List<Long>, Defect> vr =
                adGroupRelevanceMatchService.changeAdGroupsRelevanceMatch(uid, clientId, adGroupIds, true);

        assertThat(vr.hasAnyErrors()).isFalse();
        assertThat(vr.getValue()).isEqualTo(adGroupIds);

        checkNewRelevanceMatch(adGroupId, campaignId);
        checkNewRelevanceMatch(adGroupWithEnabledRelevanceMatch.getAdGroupId(),
                adGroupWithEnabledRelevanceMatch.getCampaignId());
    }

    @Test
    public void disableRelevanceMatch_AllSuccess() {
        List<Long> adGroupIds = List.of(adGroupId, adGroupWithEnabledRelevanceMatch.getAdGroupId());

        ValidationResult<List<Long>, Defect> vr =
                adGroupRelevanceMatchService.changeAdGroupsRelevanceMatch(uid, clientId, adGroupIds, false);

        assertThat(vr.hasAnyErrors()).isFalse();
        assertThat(vr.getValue()).isEqualTo(adGroupIds);

        checkRelevanceMatchIsEmpty(adGroupIds);
    }

    @Test
    public void enableRelevanceMatch_OneSuccess_OneFailed() {
        Long dynamicTextAdGroupId = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo).getAdGroupId();
        List<Long> adGroupIds = List.of(adGroupId, dynamicTextAdGroupId);

        ValidationResult<List<Long>, Defect> vr =
                adGroupRelevanceMatchService.changeAdGroupsRelevanceMatch(uid, clientId, adGroupIds, true);

        assertThat(vr.getValue()).isEqualTo(adGroupIds);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(1)), CAMPAIGN_TYPE_NOT_SUPPORTED))));

        checkNewRelevanceMatch(adGroupId, campaignId);
        checkRelevanceMatchIsEmpty(List.of(dynamicTextAdGroupId));
    }

    private void checkNewRelevanceMatch(Long adGroupId, Long campaignId) {
        RelevanceMatch expectedRelevanceMatch = new RelevanceMatch()
                .withAdGroupId(adGroupId)
                .withCampaignId(campaignId)
                .withIsDeleted(false)
                .withIsSuspended(false)
                .withStatusBsSynced(StatusBsSynced.NO);

        Map<Long, RelevanceMatch> relevanceMatchMap = relevanceMatchRepository
                .getRelevanceMatchesByAdGroupIds(clientInfo.getShard(), clientInfo.getClientId(),
                        singletonList(adGroupId));

        assertThat(relevanceMatchMap)
                .containsOnlyKeys(adGroupId);

        assertThat(relevanceMatchMap.get(adGroupId))
                .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal.class)
                .isEqualToIgnoringNullFields(expectedRelevanceMatch);
    }

    private void checkRelevanceMatchIsEmpty(Collection<Long> adGroupId) {
        Map<Long, RelevanceMatch> relevanceMatchMap = relevanceMatchRepository
                .getRelevanceMatchesByAdGroupIds(clientInfo.getShard(), clientInfo.getClientId(), adGroupId);

        assertThat(relevanceMatchMap)
                .isEmpty();
    }
}
