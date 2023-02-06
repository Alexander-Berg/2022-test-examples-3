package ru.yandex.direct.logicprocessor.processors.recomtracer;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.recommendation.model.RecommendationKey;
import ru.yandex.direct.core.entity.recomtracer.container.BannersLoadedObject;
import ru.yandex.direct.core.entity.recomtracer.container.CampaignsLoadedObject;
import ru.yandex.direct.core.entity.recomtracer.container.LoadedObject;
import ru.yandex.direct.core.entity.recomtracer.container.PhrasesLoadedObject;
import ru.yandex.direct.ess.common.utils.TablesEnum;
import ru.yandex.direct.ess.logicobjects.recomtracer.RecomTracerLogicObject;
import ru.yandex.direct.ess.logicobjects.recomtracer.RecommendationKeyIdentifier;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.logicprocessor.processors.recomtracer.RecomTracerService.getRecommendationKeyPrefixFromRecomTracerObject;

class RecomTracerServiceGetRecommendationKeyPrefixTest {
    @Test
    void testGetRecommendationKeyPrefixFromRecomTracerObject_Campaigns_WithoutLoad() {
        long clientId = 1L;
        long cid = 2L;
        RecomTracerLogicObject recomTracerLogicObject = new RecomTracerLogicObject.Builder()
                .withTableToLoad(TablesEnum.CAMPAIGNS)
                .withRecommendationTypeId(1L)
                .withPrimaryKey(cid)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CID, cid)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CLIENT_ID, clientId)
                .build();

        RecommendationKey expectedKeyPrefix = new RecommendationKey()
                .withType(1L)
                .withClientId(clientId)
                .withCampaignId(cid);

        RecommendationKey gotKeyPrefix =
                getRecommendationKeyPrefixFromRecomTracerObject(recomTracerLogicObject);

        assertThat(gotKeyPrefix).isEqualTo(expectedKeyPrefix);

    }

    @Test
    void testGetRecommendationKeyPrefixFromRecomTracerObject_Campaigns() {
        long clientId = 1L;
        long cid = 2L;
        long pid = 3L;
        long bid = 4L;
        RecomTracerLogicObject recomTracerLogicObject = new RecomTracerLogicObject.Builder()
                .withTableToLoad(TablesEnum.CAMPAIGNS)
                .withRecommendationTypeId(1L)
                .withPrimaryKey(cid)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.CID, cid)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.PID, pid)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.BID, bid)
                .build();

        LoadedObject loadedObject = new CampaignsLoadedObject(cid, clientId);
        RecommendationKey expectedKeyPrefix = new RecommendationKey()
                .withType(1L)
                .withClientId(clientId)
                .withCampaignId(cid)
                .withAdGroupId(pid)
                .withBannerId(bid);

        RecommendationKey gotKeyPrefix =
                getRecommendationKeyPrefixFromRecomTracerObject(loadedObject,
                        recomTracerLogicObject);

        assertThat(gotKeyPrefix).isEqualTo(expectedKeyPrefix);

    }

    @Test
    void testGetRecommendationKeyPrefixFromRecomTracerObject_Banners() {
        long clientId = 1L;
        long cid = 2L;
        long pid = 3L;
        long bid = 4L;

        RecomTracerLogicObject recomTracerLogicObject = new RecomTracerLogicObject.Builder()
                .withTableToLoad(TablesEnum.BANNERS)
                .withRecommendationTypeId(1L)
                .withPrimaryKey(bid)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.BID, bid)
                .build();

        LoadedObject loadedObject = new BannersLoadedObject(bid, pid, cid, clientId);
        RecommendationKey expectedKeyPrefix = new RecommendationKey()
                .withType(1L)
                .withClientId(clientId)
                .withCampaignId(cid)
                .withAdGroupId(pid)
                .withBannerId(bid);

        RecommendationKey gotKeyPrefix =
                getRecommendationKeyPrefixFromRecomTracerObject(loadedObject,
                        recomTracerLogicObject);

        assertThat(gotKeyPrefix).isEqualTo(expectedKeyPrefix);

    }

    @Test
    void testGetRecommendationKeyPrefixFromRecomTracerObject_Phrases() {
        long clientId = 1L;
        long cid = 2L;
        long pid = 3L;
        long bid = 4L;

        RecomTracerLogicObject recomTracerLogicObject = new RecomTracerLogicObject.Builder()
                .withTableToLoad(TablesEnum.PHRASES)
                .withRecommendationTypeId(1L)
                .withPrimaryKey(bid)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.PID, pid)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.BID, bid)
                .build();

        LoadedObject loadedObject = new PhrasesLoadedObject(pid, cid, clientId, false);
        RecommendationKey expectedKeyPrefix = new RecommendationKey()
                .withType(1L)
                .withClientId(clientId)
                .withCampaignId(cid)
                .withAdGroupId(pid)
                .withBannerId(bid);

        RecommendationKey gotKeyPrefix =
                getRecommendationKeyPrefixFromRecomTracerObject(loadedObject,
                        recomTracerLogicObject);

        assertThat(gotKeyPrefix).isEqualTo(expectedKeyPrefix);
    }

    @Test
    void testGetRecommendationKeyPrefixFromRecomTracerObject_Phrases_AllBannersArchived() {
        long clientId = 1L;
        long cid = 2L;
        long pid = 3L;
        long bid = 4L;

        RecomTracerLogicObject recomTracerLogicObject = new RecomTracerLogicObject.Builder()
                .withTableToLoad(TablesEnum.PHRASES)
                .withRecommendationTypeId(1L)
                .withPrimaryKey(bid)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.PID, pid)
                .addRecommendationKeyIdentifier(RecommendationKeyIdentifier.BID, bid)
                .build();

        LoadedObject loadedObject = new PhrasesLoadedObject(pid, cid, clientId, true);
        RecommendationKey expectedKeyPrefix = new RecommendationKey()
                .withType(1L)
                .withClientId(clientId)
                .withCampaignId(cid)
                .withAdGroupId(pid);

        RecommendationKey gotKeyPrefix =
                getRecommendationKeyPrefixFromRecomTracerObject(loadedObject,
                        recomTracerLogicObject);

        assertThat(gotKeyPrefix).isEqualTo(expectedKeyPrefix);
    }
}
