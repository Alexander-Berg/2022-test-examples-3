package ru.yandex.direct.core.entity.adgroup.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class GetAdGroupsWithAdjustedBsRarelyLoadedTest {

    private static final ClientId DEFAULT_CLIENT_ID = ClientId.fromLong(1L);

    private static final int DEFAULT_SHARD = 1;

    private static final Long FIRST_ADGROUP_ID = 1L;
    private static final Long SECOND_ADGROUP_ID = 2L;
    private static final Long THIRD_ADGROUP_ID = 3L;
    private static final Long FOURTH_ADGROUP_ID = 4L;
    private static final Long FIFTH_ADGROUP_ID = 5L;

    private static final Long ARCHIVED_CAMPAIGN_ID = 11L;

    @InjectMocks
    private AdGroupService service;

    @Mock
    private ShardHelper shardHelper;

    @Mock
    private AdGroupRepository adGroupRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Before
    public void setUp() {
        initMocks(this);

        when(shardHelper.getShardByClientIdStrictly(eq(DEFAULT_CLIENT_ID))).thenReturn(DEFAULT_SHARD);

        List<AdGroup> adgroups = asList(
                new TextAdGroup().withId(FIRST_ADGROUP_ID).withCampaignId(10L).withType(AdGroupType.BASE)
                        .withBsRarelyLoaded(true),
                new TextAdGroup().withId(SECOND_ADGROUP_ID).withCampaignId(ARCHIVED_CAMPAIGN_ID)
                        .withType(AdGroupType.BASE).withBsRarelyLoaded(true),
                new DynamicTextAdGroup().withId(THIRD_ADGROUP_ID).withCampaignId(12L).withType(AdGroupType.DYNAMIC)
                        .withBsRarelyLoaded(true),
                new PerformanceAdGroup().withId(FOURTH_ADGROUP_ID).withCampaignId(13L)
                        .withType(AdGroupType.PERFORMANCE).withBsRarelyLoaded(true));

        when(adGroupRepository.getAdGroups(eq(DEFAULT_SHARD), anyList())).thenReturn(adgroups);

        when(campaignRepository.getArchivedCampaigns(eq(DEFAULT_SHARD), anySet())).thenReturn(new HashSet<>(
                singleton(ARCHIVED_CAMPAIGN_ID)));
    }

    @Test
    public void noAdGroupIdsGiven() {
        List<AdGroup> adgroups = service.getAdGroups(DEFAULT_CLIENT_ID, new ArrayList<>());

        assertThat("no adgroup without ids", adgroups, is(empty()));
    }

    @Test
    public void getAdGroupWIthAdjustedBsRarelyLoaded() {
        List<AdGroup> adgroups = service.getAdGroups(DEFAULT_CLIENT_ID,
                asList(FIRST_ADGROUP_ID, SECOND_ADGROUP_ID, THIRD_ADGROUP_ID, FOURTH_ADGROUP_ID, FIFTH_ADGROUP_ID));

        List<Boolean> adgroupsBsRarelyLoaded = adgroups.stream().map(AdGroup::getBsRarelyLoaded).collect(toList());
        assertThat("bsRarelyLoaded adjusted for adgroups", adgroupsBsRarelyLoaded, contains(true, false, false, false));
    }

}
