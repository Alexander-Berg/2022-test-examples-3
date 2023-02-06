package ru.yandex.direct.logicprocessor.processors.campaignlastchange;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.banner.type.image.BannerImageRepository;
import ru.yandex.direct.ess.common.utils.TablesEnum;
import ru.yandex.direct.ess.logicobjects.campaignlastchange.CampAggregatedLastchangeObject;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CampAggregatedLastchangeServiceTest {

    private static final int SHARD = 1;
    private static final Long CAMPAIGN_ID = 10L;
    private static final LocalDateTime MAX_LAST_CHANGE = LocalDateTime.of(2019, 1, 1, 0, 0);
    private CampAggregatedLastchangeService campAggregatedLastchangeService;
    private BannerRelationsRepository bannerRelationsRepository;
    private BannerImageRepository bannerImageRepository;
    private PpcProperty<Boolean> grutProperty;

    @BeforeEach
    void before() {
        bannerRelationsRepository = mock(BannerRelationsRepository.class);
        bannerImageRepository = mock(BannerImageRepository.class);
        PpcPropertiesSupport ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        grutProperty = mock(PpcProperty.class);
        when(ppcPropertiesSupport.get(PpcPropertyNames.FETCH_DATA_FROM_GRUT_FOR_CAMP_AGGREGATED_LASTCHANGE, Duration.ofMinutes(5)))
                .thenReturn(grutProperty);
        when(grutProperty.getOrDefault(false)).thenReturn(false);

        campAggregatedLastchangeService = new CampAggregatedLastchangeService(bannerRelationsRepository,
                bannerImageRepository, ppcPropertiesSupport);
    }

    @Test
    void testGetLastChangesByCids_BannerTableLastChange() {
        int idx = 0;
        long bannerImagePrimaryKey = 1L;
        long bannerPrimaryKey = 2L;
        long phrasePrimaryKey = 3L;
        CampAggregatedLastchangeObject bannerObject =
                new CampAggregatedLastchangeObject(TablesEnum.BANNERS, bannerPrimaryKey, CAMPAIGN_ID, MAX_LAST_CHANGE);

        CampAggregatedLastchangeObject phraseObject =
                new CampAggregatedLastchangeObject(TablesEnum.PHRASES, phrasePrimaryKey, CAMPAIGN_ID,
                        MAX_LAST_CHANGE.minusHours(++idx));

        CampAggregatedLastchangeObject bannerImageObject =
                new CampAggregatedLastchangeObject(TablesEnum.BANNER_IMAGES, bannerImagePrimaryKey, null,
                        MAX_LAST_CHANGE.minusHours(++idx));

        when(bannerImageRepository.getCampaignIdsByImageIdsForShard(eq(SHARD), eq(singletonList(bannerImagePrimaryKey))))
                .thenReturn(
                        ImmutableMap.of(bannerImagePrimaryKey, CAMPAIGN_ID));

        List<CampAggregatedLastchangeObject> campAggregatedLastchangeObjectList =
                Arrays.asList(bannerObject, phraseObject, bannerImageObject);
        Map<Long, LocalDateTime> expected = ImmutableMap.of(CAMPAIGN_ID, MAX_LAST_CHANGE);
        Map<Long, LocalDateTime> got =
                campAggregatedLastchangeService.getLastChangesByCids(SHARD, campAggregatedLastchangeObjectList);

        assertThat(got).isEqualTo(expected);
    }

    @Test
    void testGetLastChangesByCids_PhrasesTableLastChange() {
        int idx = 0;
        long bannerImagePrimaryKey = 1L;
        long bannerPrimaryKey = 2L;
        long phrasePrimaryKey = 3L;

        CampAggregatedLastchangeObject phraseObject =
                new CampAggregatedLastchangeObject(TablesEnum.PHRASES, phrasePrimaryKey, CAMPAIGN_ID, MAX_LAST_CHANGE);

        CampAggregatedLastchangeObject bannerObject =
                new CampAggregatedLastchangeObject(TablesEnum.BANNERS, bannerPrimaryKey, CAMPAIGN_ID,
                        MAX_LAST_CHANGE.minusHours(++idx));

        CampAggregatedLastchangeObject bannerImageObject =
                new CampAggregatedLastchangeObject(TablesEnum.BANNER_IMAGES, bannerImagePrimaryKey, null,
                        MAX_LAST_CHANGE.minusHours(++idx));

        when(bannerImageRepository.getCampaignIdsByImageIdsForShard(eq(SHARD), eq(singletonList(bannerImagePrimaryKey))))
                .thenReturn(
                        ImmutableMap.of(bannerImagePrimaryKey, CAMPAIGN_ID));

        List<CampAggregatedLastchangeObject> campAggregatedLastchangeObjectList =
                Arrays.asList(bannerObject, phraseObject, bannerImageObject);
        Map<Long, LocalDateTime> expected = ImmutableMap.of(CAMPAIGN_ID, MAX_LAST_CHANGE);
        Map<Long, LocalDateTime> got =
                campAggregatedLastchangeService.getLastChangesByCids(SHARD, campAggregatedLastchangeObjectList);

        assertThat(got).isEqualTo(expected);
    }

    @Test
    void testGetLastChangesByCids_BannerImagesTableLastChange() {
        int idx = 0;
        long bannerImagePrimaryKey = 1L;
        long bannerPrimaryKey = 2L;
        long phrasePrimaryKey = 3L;

        CampAggregatedLastchangeObject bannerImageObject =
                new CampAggregatedLastchangeObject(TablesEnum.BANNER_IMAGES, bannerImagePrimaryKey, null,
                        MAX_LAST_CHANGE);

        CampAggregatedLastchangeObject bannerObject =
                new CampAggregatedLastchangeObject(TablesEnum.BANNERS, bannerPrimaryKey, CAMPAIGN_ID,
                        MAX_LAST_CHANGE.minusHours(++idx));

        CampAggregatedLastchangeObject phraseObject =
                new CampAggregatedLastchangeObject(TablesEnum.PHRASES, phrasePrimaryKey, CAMPAIGN_ID,
                        MAX_LAST_CHANGE.minusHours(++idx));


        when(bannerImageRepository.getCampaignIdsByImageIdsForShard(eq(SHARD), eq(singletonList(bannerImagePrimaryKey))))
                .thenReturn(
                        ImmutableMap.of(bannerImagePrimaryKey, CAMPAIGN_ID));

        List<CampAggregatedLastchangeObject> campAggregatedLastchangeObjectList =
                Arrays.asList(bannerObject, phraseObject, bannerImageObject);
        Map<Long, LocalDateTime> expected = ImmutableMap.of(CAMPAIGN_ID, MAX_LAST_CHANGE);
        Map<Long, LocalDateTime> got =
                campAggregatedLastchangeService.getLastChangesByCids(SHARD, campAggregatedLastchangeObjectList);

        assertThat(got).isEqualTo(expected);
    }

    @Test
    void testGetLastChangesByCids_DuplicateLastChange() {
        long bannerPrimaryKey = 2L;
        long phrasePrimaryKey = 3L;
        CampAggregatedLastchangeObject bannerObject =
                new CampAggregatedLastchangeObject(TablesEnum.BANNERS, bannerPrimaryKey, CAMPAIGN_ID, MAX_LAST_CHANGE);

        CampAggregatedLastchangeObject phraseObject =
                new CampAggregatedLastchangeObject(TablesEnum.PHRASES, phrasePrimaryKey, CAMPAIGN_ID, MAX_LAST_CHANGE);


        List<CampAggregatedLastchangeObject> campAggregatedLastchangeObjectList =
                Arrays.asList(bannerObject, phraseObject);

        Map<Long, LocalDateTime> expected = ImmutableMap.of(CAMPAIGN_ID, MAX_LAST_CHANGE);
        Map<Long, LocalDateTime> got =
                campAggregatedLastchangeService.getLastChangesByCids(SHARD, campAggregatedLastchangeObjectList);

        assertThat(got).isEqualTo(expected);
    }

    @Test
    void testGetLastChangesByCids_DifferentCids() {
        long bannerPrimaryKey = 2L;
        long phrasePrimaryKey = 3L;
        CampAggregatedLastchangeObject bannerObject =
                new CampAggregatedLastchangeObject(TablesEnum.BANNERS, bannerPrimaryKey, CAMPAIGN_ID, MAX_LAST_CHANGE);

        CampAggregatedLastchangeObject phraseObject =
                new CampAggregatedLastchangeObject(TablesEnum.PHRASES, phrasePrimaryKey, CAMPAIGN_ID + 1,
                        MAX_LAST_CHANGE);


        List<CampAggregatedLastchangeObject> campAggregatedLastchangeObjectList =
                Arrays.asList(bannerObject, phraseObject);

        Map<Long, LocalDateTime> expected = ImmutableMap.of(CAMPAIGN_ID, MAX_LAST_CHANGE,
                CAMPAIGN_ID + 1, MAX_LAST_CHANGE);
        Map<Long, LocalDateTime> got =
                campAggregatedLastchangeService.getLastChangesByCids(SHARD, campAggregatedLastchangeObjectList);

        assertThat(got).isEqualTo(expected);
    }

    @Test
    void testGetLastChangesByCids_BannerImageTable_DifferentCidSources() {
        long primaryKey = 1L;
        long bid = 3L;
        CampAggregatedLastchangeObject bannerImageObjectWithImageId =
                new CampAggregatedLastchangeObject(TablesEnum.BANNER_IMAGES, primaryKey, null, MAX_LAST_CHANGE);
        CampAggregatedLastchangeObject bannerImageObjectWithBid =
                new CampAggregatedLastchangeObject(TablesEnum.BANNER_IMAGES, primaryKey + 1, bid, MAX_LAST_CHANGE);

        when(bannerImageRepository.getCampaignIdsByImageIdsForShard(eq(SHARD), eq(singletonList(primaryKey)))).thenReturn(
                ImmutableMap.of(primaryKey, CAMPAIGN_ID));

        when(bannerRelationsRepository.getCampaignIdsByBannerIdsForShard(eq(SHARD), eq(singletonList(bid)))).thenReturn(
                ImmutableMap.of(bid, CAMPAIGN_ID + 1));

        List<CampAggregatedLastchangeObject> campAggregatedLastchangeObjectList =
                Arrays.asList(bannerImageObjectWithImageId, bannerImageObjectWithBid);

        Map<Long, LocalDateTime> expected = ImmutableMap.of(CAMPAIGN_ID, MAX_LAST_CHANGE,
                CAMPAIGN_ID + 1, MAX_LAST_CHANGE);

        Map<Long, LocalDateTime> got =
                campAggregatedLastchangeService.getLastChangesByCids(SHARD, campAggregatedLastchangeObjectList);

        assertThat(got).isEqualTo(expected);
    }

    @Test
    void testGetLastChangesByCids_EmptyObjectList() {
        List<CampAggregatedLastchangeObject> campAggregatedLastchangeObjectList = emptyList();
        Map<Long, LocalDateTime> expected = emptyMap();
        Map<Long, LocalDateTime> got =
                campAggregatedLastchangeService.getLastChangesByCids(SHARD, campAggregatedLastchangeObjectList);

        assertThat(got).isEqualTo(expected);
    }

    @Test
    void testGetLastChangesByCids_NullCid() {
        CampAggregatedLastchangeObject bannerObject =
                new CampAggregatedLastchangeObject(TablesEnum.BANNERS, 1L, null, MAX_LAST_CHANGE);
        List<CampAggregatedLastchangeObject> campAggregatedLastchangeObjectList =
                singletonList(bannerObject);
        Map<Long, LocalDateTime> expected = emptyMap();
        Map<Long, LocalDateTime> got =
                campAggregatedLastchangeService.getLastChangesByCids(SHARD, campAggregatedLastchangeObjectList);

        assertThat(got).isEqualTo(expected);
    }

    @Test
    void testGetLastChangesByCids_NullLastChange() {
        CampAggregatedLastchangeObject bannerObject =
                new CampAggregatedLastchangeObject(TablesEnum.BANNERS, 1L, CAMPAIGN_ID, null);
        List<CampAggregatedLastchangeObject> campAggregatedLastchangeObjectList =
                singletonList(bannerObject);
        Map<Long, LocalDateTime> expected = emptyMap();
        Map<Long, LocalDateTime> got =
                campAggregatedLastchangeService.getLastChangesByCids(SHARD, campAggregatedLastchangeObjectList);

        assertThat(got).isEqualTo(expected);
    }

    @Test
    void testGetLastChangesByCids_BannerCandidateTableLastChangeEnabled() {
        when(grutProperty.getOrDefault(false)).thenReturn(true);

        long bannerPrimaryKey = 2L;
        CampAggregatedLastchangeObject bannerObject =
                new CampAggregatedLastchangeObject(TablesEnum.BANNERS, bannerPrimaryKey, CAMPAIGN_ID,
                        MAX_LAST_CHANGE.minusHours(1));
        CampAggregatedLastchangeObject bannerCandidateObject =
                new CampAggregatedLastchangeObject(TablesEnum.BANNER_CANDIDATES, bannerPrimaryKey, CAMPAIGN_ID,
                        MAX_LAST_CHANGE);
        List<CampAggregatedLastchangeObject> campAggregatedLastchangeObjectList = List.of(bannerObject, bannerCandidateObject);

        Map<Long, LocalDateTime> expected = ImmutableMap.of(CAMPAIGN_ID, MAX_LAST_CHANGE);
        Map<Long, LocalDateTime> got = campAggregatedLastchangeService.getLastChangesByCids(SHARD, campAggregatedLastchangeObjectList);

        assertThat(got).isEqualTo(expected);
    }

    @Test
    void testGetLastChangesByCids_BannerCandidateTableLastChangeNotEnabled() {
        when(grutProperty.getOrDefault(false)).thenReturn(false);

        long bannerPrimaryKey = 2L;
        CampAggregatedLastchangeObject bannerObject =
                new CampAggregatedLastchangeObject(TablesEnum.BANNERS, bannerPrimaryKey, CAMPAIGN_ID,
                        MAX_LAST_CHANGE.minusHours(1));
        CampAggregatedLastchangeObject bannerCandidateObject =
                new CampAggregatedLastchangeObject(TablesEnum.BANNER_CANDIDATES, bannerPrimaryKey, CAMPAIGN_ID,
                        MAX_LAST_CHANGE);
        List<CampAggregatedLastchangeObject> campAggregatedLastchangeObjectList = List.of(bannerObject, bannerCandidateObject);

        Map<Long, LocalDateTime> expected = ImmutableMap.of(CAMPAIGN_ID, MAX_LAST_CHANGE.minusHours(1));
        Map<Long, LocalDateTime> got = campAggregatedLastchangeService.getLastChangesByCids(SHARD, campAggregatedLastchangeObjectList);

        assertThat(got).isEqualTo(expected);
    }
}
