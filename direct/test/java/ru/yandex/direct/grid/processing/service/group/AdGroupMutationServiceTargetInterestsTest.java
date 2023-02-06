package ru.yandex.direct.grid.processing.service.group;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionShortcutService.NOT_BOUNCE_SHORTCUT_DEFAULT_ID;

@RunWith(MockitoJUnitRunner.class)
public class AdGroupMutationServiceTargetInterestsTest {
    private static final Long NOT_SHORTCUT_RETARGETING_CONDITION_ID = 50L;
    private static final Long DEFAULT_SHORTCUT_RETARGETING_CONDITION_ID = NOT_BOUNCE_SHORTCUT_DEFAULT_ID;
    private static final Long NEW_SHORTCUT_RETARGETING_CONDITION_ID = 200L;
    private static final Long CAMPAIGN_ID = 123456L;
    private static final Long AD_GROUP_ID = 654321L;

    @Mock
    private FeatureService featureService;

    @Mock
    private RetargetingConditionService retargetingConditionService;

    @Mock
    private AdGroupRepository adGroupRepository;

    @InjectMocks
    private AdGroupMutationService adGroupMutationService;

    private ClientId clientId;
    private int shard;
    List<TargetInterest> targetInterests;

    @Before
    public void before() {
        clientId = ClientId.fromLong(2L);
        shard = 3;

        doReturn(Map.of(CAMPAIGN_ID, Map.of(DEFAULT_SHORTCUT_RETARGETING_CONDITION_ID,
                NEW_SHORTCUT_RETARGETING_CONDITION_ID))).
                when(retargetingConditionService).findOrCreateRetargetingConditionShortcuts(eq(shard), eq(clientId),
                        eq(Map.of(CAMPAIGN_ID, List.of(DEFAULT_SHORTCUT_RETARGETING_CONDITION_ID))));
    }

    @Test
    public void updateTargetInterests_empty() {
        targetInterests = List.of();
        adGroupMutationService.updateDefaultRetargetingConditionShortcutIds(shard, clientId, targetInterests, true);
        assertThat(targetInterests).isEmpty();
    }

    @Test
    public void updateTargetInterests_noShortcutEntries() {
        targetInterests = List.of(new TargetInterest()
                .withCampaignId(CAMPAIGN_ID)
                .withRetargetingConditionId(NOT_SHORTCUT_RETARGETING_CONDITION_ID));
        var expectedTargetInterest = new TargetInterest()
                .withCampaignId(CAMPAIGN_ID)
                .withRetargetingConditionId(NOT_SHORTCUT_RETARGETING_CONDITION_ID);

        adGroupMutationService.updateDefaultRetargetingConditionShortcutIds(shard, clientId, targetInterests, true);
        assertThat(targetInterests).hasSize(1)
                .containsExactly(expectedTargetInterest);
    }

    @Test
    public void updateTargetInterests_newAdGroup() {
        targetInterests = List.of(new TargetInterest()
                .withCampaignId(CAMPAIGN_ID)
                .withRetargetingConditionId(DEFAULT_SHORTCUT_RETARGETING_CONDITION_ID));
        var expectedTargetInterest = new TargetInterest()
                .withCampaignId(CAMPAIGN_ID)
                .withRetargetingConditionId(NEW_SHORTCUT_RETARGETING_CONDITION_ID);

        adGroupMutationService.updateDefaultRetargetingConditionShortcutIds(shard, clientId, targetInterests, true);
        assertThat(targetInterests).hasSize(1)
                .containsExactly(expectedTargetInterest);
    }

    @Test
    public void updateTargetInterests_existingAdGroup() {
        doReturn(Map.of(AD_GROUP_ID, CAMPAIGN_ID))
                .when(adGroupRepository).getCampaignIdsByAdGroupIds(shard, clientId, List.of(AD_GROUP_ID));

        targetInterests = List.of(new TargetInterest()
                .withAdGroupId(AD_GROUP_ID)
                .withRetargetingConditionId(DEFAULT_SHORTCUT_RETARGETING_CONDITION_ID));
        var expectedTargetInterest = new TargetInterest()
                .withAdGroupId(AD_GROUP_ID)
                .withRetargetingConditionId(NEW_SHORTCUT_RETARGETING_CONDITION_ID);

        adGroupMutationService.updateDefaultRetargetingConditionShortcutIds(shard, clientId, targetInterests, false);
        assertThat(targetInterests).hasSize(1)
                .containsExactly(expectedTargetInterest);
    }
}
