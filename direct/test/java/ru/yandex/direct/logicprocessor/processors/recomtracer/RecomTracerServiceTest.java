package ru.yandex.direct.logicprocessor.processors.recomtracer;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.recommendation.model.RecommendationKey;
import ru.yandex.direct.core.entity.recomtracer.container.BannersLoadedObject;
import ru.yandex.direct.core.entity.recomtracer.container.CampaignsLoadedObject;
import ru.yandex.direct.core.entity.recomtracer.container.LoadedObject;
import ru.yandex.direct.core.entity.recomtracer.container.PhrasesLoadedObject;
import ru.yandex.direct.core.entity.recomtracer.repository.RecomTracerRepository;
import ru.yandex.direct.ess.common.utils.TablesEnum;
import ru.yandex.direct.ess.logicobjects.recomtracer.RecomTracerLogicObject;
import ru.yandex.direct.ess.logicobjects.recomtracer.RecommendationKeyIdentifier;
import ru.yandex.direct.grid.core.entity.recommendation.repository.GridRecommendationYtRepository;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecomTracerServiceTest {

    private static final int SHARD = 0;
    private static final long recommendationType = 1L;
    private RecomTracerRepository recomTracerRepository;
    private RecomTracerService recomTracerService;

    @BeforeEach
    void before() {
        recomTracerRepository = mock(RecomTracerRepository.class);
        GridRecommendationYtRepository recommendationYtRepository = mock(GridRecommendationYtRepository.class);
        recomTracerService = new RecomTracerService(recomTracerRepository, recommendationYtRepository, emptyList());
    }


    @Test
    void testGetRecommendationKeysPrefixForObjects_SamePrimaryKeyForLoadAndWithoutLoad() {

        long pid = 5L;
        long cid = 2L;
        long clientId = 4L;

        RecomTracerLogicObject recomTracerLogicObjectWithoutLoad = new RecomTracerLogicObject.Builder()
                .withRecommendationTypeId(recommendationType)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CLIENT_ID, clientId)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CID, cid)
                .build();

        RecomTracerLogicObject recomTracerLogicObjectNeedLoad = createCampaignLogicObject(cid, pid);

        LoadedObject loadedObject = new CampaignsLoadedObject(cid, clientId);
        when(recomTracerRepository.loadCampaignsEntities(eq(SHARD), eq(ImmutableSet.of(cid)))).thenReturn(
                ImmutableMap.of(cid, loadedObject)
        );

        RecommendationKey expectedRecommendationKeyPrefixWithoutLoad = createKey(clientId, cid);

        RecommendationKey expectedRecommendationKeyPrefixWithLoad = createKey(clientId, cid, pid);

        Map<RecomTracerLogicObject, RecommendationKey> expected = ImmutableMap.of(
                recomTracerLogicObjectWithoutLoad, expectedRecommendationKeyPrefixWithoutLoad,
                recomTracerLogicObjectNeedLoad, expectedRecommendationKeyPrefixWithLoad
        );

        Map<RecomTracerLogicObject, RecommendationKey> got =
                recomTracerService.getRecommendationKeysPrefixForObjects(SHARD,
                        ImmutableSet.of(recomTracerLogicObjectWithoutLoad, recomTracerLogicObjectNeedLoad));

        assertThat(got).hasSize(2);
        assertThat(got).isEqualTo(expected);

    }

    @Test
    void testGetRecommendationKeysPrefixForObjectsWithoutLoad_EmptyObjectsSet() {
        Map<RecomTracerLogicObject, RecommendationKey> got =
                recomTracerService.getRecommendationKeysPrefixForObjects(SHARD, emptySet());
        assertThat(got).isEmpty();
    }


    @Test
    void testGetRecommendationKeysPrefixForObjects_SameTableAndPrimaryKey() {
        long cid = 1L;
        long pid1 = 2L;
        long pid2 = 3L;
        long clientId = 5L;

        CampaignsLoadedObject campaignsLoadedObject = new CampaignsLoadedObject(cid, clientId);
        when(recomTracerRepository.loadCampaignsEntities(eq(SHARD), eq(ImmutableSet.of(cid)))).thenReturn(
                ImmutableMap.of(cid, campaignsLoadedObject)
        );
        RecomTracerLogicObject recomTracerLogicObject1 = createCampaignLogicObject(cid, pid1);

        RecomTracerLogicObject recomTracerLogicObject2 = createCampaignLogicObject(cid, pid2);

        RecommendationKey expectedRecommendationKeyPrefix1 = createKey(clientId, cid, pid1);

        RecommendationKey expectedRecommendationKeyPrefix2 = createKey(clientId, cid, pid2);

        Map<RecomTracerLogicObject, RecommendationKey> expected = ImmutableMap.of(
                recomTracerLogicObject1, expectedRecommendationKeyPrefix1,
                recomTracerLogicObject2, expectedRecommendationKeyPrefix2
        );

        Map<RecomTracerLogicObject, RecommendationKey> got =
                recomTracerService.getRecommendationKeysPrefixForObjects(SHARD,
                        ImmutableSet.of(recomTracerLogicObject1,
                                recomTracerLogicObject2));

        assertThat(got).hasSize(2);
        assertThat(got).isEqualTo(expected);
    }

    @Test
    void testGetRecommendationKeysPrefixForObjects_SameTableDifferentPrimaryKey() {
        long cid1 = 1L;
        long cid2 = 4L;
        long pid1 = 2L;
        long pid2 = 2L;
        long clientId = 5L;
        CampaignsLoadedObject campaignsLoadedObject1 = new CampaignsLoadedObject(cid1, clientId);
        CampaignsLoadedObject campaignsLoadedObject2 = new CampaignsLoadedObject(cid2, clientId);
        when(recomTracerRepository.loadCampaignsEntities(eq(SHARD), eq(ImmutableSet.of(cid1, cid2)))).thenReturn(
                ImmutableMap.of(
                        cid1, campaignsLoadedObject1,
                        cid2, campaignsLoadedObject2)
        );
        RecomTracerLogicObject recomTracerLogicObject1 = createCampaignLogicObject(cid1, pid1);

        RecomTracerLogicObject recomTracerLogicObject2 = createCampaignLogicObject(cid2, pid2);

        RecommendationKey recommendationKey1 = createKey(clientId, cid1, pid1);
        RecommendationKey recommendationKey2 = createKey(clientId, cid2, pid2);

        Map<RecomTracerLogicObject, RecommendationKey> expected = ImmutableMap.of(
                recomTracerLogicObject1, recommendationKey1,
                recomTracerLogicObject2, recommendationKey2);

        Map<RecomTracerLogicObject, RecommendationKey> got =
                recomTracerService.getRecommendationKeysPrefixForObjects(SHARD,
                        ImmutableSet.of(recomTracerLogicObject1, recomTracerLogicObject2));

        assertThat(got).hasSize(2);
        assertThat(got).isEqualTo(expected);
    }

    @Test
    void testGetRecommendationKeysPrefixForObjects_AllBannersArchivedForGroup() {
        long cid = 1L;
        long pid1 = 2L;
        long pid2 = 3L;
        long bid1 = 8L;
        long bid2 = 9L;
        long clientId = 5L;

        RecomTracerLogicObject recomTracerLogicObject1 = new RecomTracerLogicObject.Builder()
                .withRecommendationTypeId(recommendationType)
                .withTableToLoad(TablesEnum.PHRASES)
                .withPrimaryKey(pid1)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.PID, pid1)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.BID, bid1)
                .build();

        RecomTracerLogicObject recomTracerLogicObject2 = new RecomTracerLogicObject.Builder()
                .withRecommendationTypeId(recommendationType)
                .withTableToLoad(TablesEnum.PHRASES)
                .withPrimaryKey(pid2)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.PID, pid2)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.BID, bid2)
                .build();

        PhrasesLoadedObject phrasesLoadedObject1 = new PhrasesLoadedObject(pid1, cid, clientId, true);
        PhrasesLoadedObject phrasesLoadedObject2 = new PhrasesLoadedObject(pid2, cid, clientId, false);

        when(recomTracerRepository.loadPhrasesEntities(eq(SHARD), eq(ImmutableSet.of(pid1, pid2)))).thenReturn(
                ImmutableMap.of(
                        pid1, phrasesLoadedObject1,
                        pid2, phrasesLoadedObject2)
        );

        RecommendationKey recommendationKey1 = createKey(clientId, cid, pid1);
        RecommendationKey recommendationKey2 = createKey(clientId, cid, pid2, bid2);

        Map<RecomTracerLogicObject, RecommendationKey> expected = ImmutableMap.of(
                recomTracerLogicObject1, recommendationKey1,
                recomTracerLogicObject2, recommendationKey2
        );

        Map<RecomTracerLogicObject, RecommendationKey> got =
                recomTracerService.getRecommendationKeysPrefixForObjects(SHARD,
                        ImmutableSet.of(recomTracerLogicObject1, recomTracerLogicObject2));

        assertThat(got).hasSize(2);
        assertThat(got).isEqualTo(expected);


    }

    @Test
    void testGetRecommendationKeysPrefixForObjects_DifferentTables() {
        long cid = 1L;
        long pid = 2L;
        long bid = 8L;
        long clientId = 5L;

        RecomTracerLogicObject recomTracerLogicObject1 = new RecomTracerLogicObject.Builder()
                .withRecommendationTypeId(recommendationType)
                .withTableToLoad(TablesEnum.CAMPAIGNS)
                .withPrimaryKey(cid)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CID, cid)
                .build();

        RecomTracerLogicObject recomTracerLogicObject2 = new RecomTracerLogicObject.Builder()
                .withRecommendationTypeId(recommendationType)
                .withTableToLoad(TablesEnum.PHRASES)
                .withPrimaryKey(pid)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.PID, pid)
                .build();

        RecomTracerLogicObject recomTracerLogicObject3 = new RecomTracerLogicObject.Builder()
                .withRecommendationTypeId(recommendationType)
                .withTableToLoad(TablesEnum.BANNERS)
                .withPrimaryKey(bid)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.BID, bid)
                .build();

        CampaignsLoadedObject campaignsLoadedObject = new CampaignsLoadedObject(cid, clientId);
        PhrasesLoadedObject phrasesLoadedObject = new PhrasesLoadedObject(pid, cid, clientId, false);
        BannersLoadedObject bannersLoadedObject = new BannersLoadedObject(bid, pid, cid, clientId);

        when(recomTracerRepository.loadCampaignsEntities(eq(SHARD), eq(ImmutableSet.of(cid)))).thenReturn(
                ImmutableMap.of(cid, campaignsLoadedObject)
        );

        when(recomTracerRepository.loadPhrasesEntities(eq(SHARD), eq(ImmutableSet.of(pid)))).thenReturn(
                ImmutableMap.of(
                        pid, phrasesLoadedObject)
        );

        when(recomTracerRepository.loadBannerEntities(eq(SHARD), eq(ImmutableSet.of(bid)))).thenReturn(
                ImmutableMap.of(
                        bid, bannersLoadedObject)
        );

        RecommendationKey recommendationKey1 = createKey(clientId, cid);
        RecommendationKey recommendationKey2 = createKey(clientId, cid, pid);
        RecommendationKey recommendationKey3 = createKey(clientId, cid, pid, bid);

        Map<RecomTracerLogicObject, RecommendationKey> expected = ImmutableMap.of(
                recomTracerLogicObject1, recommendationKey1,
                recomTracerLogicObject2, recommendationKey2,
                recomTracerLogicObject3, recommendationKey3
        );

        Map<RecomTracerLogicObject, RecommendationKey> got =
                recomTracerService.getRecommendationKeysPrefixForObjects(SHARD,
                        ImmutableSet.of(recomTracerLogicObject1,
                                recomTracerLogicObject2, recomTracerLogicObject3));

        assertThat(got).hasSize(3);
        assertThat(got).isEqualTo(expected);
    }

    @Test
    void testGetRecommendationKeysPrefixForObjects_SomeObjectsNotFoundInMysql() {
        long cid1 = 1L;
        long cid2 = 4L;
        long pid1 = 2L;
        long pid2 = 2L;
        long clientId = 5L;
        CampaignsLoadedObject campaignsLoadedObject = new CampaignsLoadedObject(cid1, clientId);
        when(recomTracerRepository.loadCampaignsEntities(eq(SHARD), eq(ImmutableSet.of(cid1, cid2)))).thenReturn(
                ImmutableMap.of(
                        cid1, campaignsLoadedObject)
        );
        RecomTracerLogicObject recomTracerLogicObjectFound = createCampaignLogicObject(cid1, pid1);

        RecomTracerLogicObject recomTracerLogicObjectNotFound = createCampaignLogicObject(cid2, pid2);

        RecommendationKey recommendationKey = createKey(clientId, cid1, pid1);

        Map<RecomTracerLogicObject, RecommendationKey> expected = ImmutableMap.of(
                recomTracerLogicObjectFound, recommendationKey);

        Map<RecomTracerLogicObject, RecommendationKey> got =
                recomTracerService.getRecommendationKeysPrefixForObjects(SHARD,
                        ImmutableSet.of(recomTracerLogicObjectFound, recomTracerLogicObjectNotFound));

        assertThat(got).hasSize(1);
        assertThat(got).isEqualTo(expected);
    }

    private RecomTracerLogicObject createCampaignLogicObject(long cid, long pid) {
        return new RecomTracerLogicObject.Builder()
                .withRecommendationTypeId(recommendationType)
                .withTableToLoad(TablesEnum.CAMPAIGNS)
                .withPrimaryKey(cid)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CID, cid)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.PID, pid)
                .build();
    }

    private RecommendationKey createKey(long clientId, long cid) {
        return new RecommendationKey().withType(recommendationType).withClientId(clientId).withCampaignId(cid);
    }

    private RecommendationKey createKey(long clientId, long cid, long pid) {
        return createKey(clientId, cid).withAdGroupId(pid);
    }

    private RecommendationKey createKey(long clientId, long cid, long pid, long bid) {
        return createKey(clientId, cid, pid).withBannerId(bid);
    }
}
