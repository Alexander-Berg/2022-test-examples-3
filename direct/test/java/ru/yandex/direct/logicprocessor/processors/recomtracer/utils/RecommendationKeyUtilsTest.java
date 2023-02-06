package ru.yandex.direct.logicprocessor.processors.recomtracer.utils;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.recommendation.model.RecommendationKey;
import ru.yandex.direct.grid.core.entity.recommendation.model.GdiRecommendation;
import ru.yandex.direct.grid.model.entity.recommendation.GdiRecommendationType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.logicprocessor.processors.recomtracer.utils.RecommendationKeyUtils.getAllPossiblePrefixesFromGdiKeys;
import static ru.yandex.direct.logicprocessor.processors.recomtracer.utils.RecommendationKeyUtils.getKeyPrefixWithLengthFromFullKey;

class RecommendationKeyUtilsTest {

    @Test
    void testGetKeyPrefixWithLengthFromFullKey() {
        Long clientId = 1L;
        Long cid = 2L;
        Long pid = 3L;
        Long bid = 4L;
        Long timestamp = 1234567L;

        GdiRecommendationType type = GdiRecommendationType.dailyBudget;

        RecommendationKey fullRecommendationKey = new RecommendationKey()
                .withClientId(clientId)
                .withType(type.getId())
                .withCampaignId(cid)
                .withAdGroupId(pid)
                .withBannerId(bid)
                .withUserKey1("")
                .withUserKey2("")
                .withUserKey3("")
                .withTimestamp(timestamp);

        RecommendationKey expectedPrefix1 =
                new RecommendationKey().withClientId(clientId).withType(type.getId());

        RecommendationKey expectedPrefix2 =
                new RecommendationKey().withClientId(clientId).withType(type.getId()).withCampaignId(cid);

        RecommendationKey expectedPrefix3 =
                new RecommendationKey().withClientId(clientId).withType(type.getId()).withCampaignId(cid).withAdGroupId(pid);

        RecommendationKey expectedPrefix4 =
                new RecommendationKey().withClientId(clientId).withType(type.getId()).withCampaignId(cid).withAdGroupId(pid).withBannerId(bid);

        assertThatThrownBy(() -> getKeyPrefixWithLengthFromFullKey(fullRecommendationKey, 0)).isInstanceOf(IllegalArgumentException.class);

        RecommendationKey gotPrefix1 = getKeyPrefixWithLengthFromFullKey(fullRecommendationKey, 1);
        assertThat(gotPrefix1).isEqualTo(expectedPrefix1);

        RecommendationKey gotPrefix2 = getKeyPrefixWithLengthFromFullKey(fullRecommendationKey, 2);
        assertThat(gotPrefix2).isEqualTo(expectedPrefix2);

        RecommendationKey gotPrefix3 = getKeyPrefixWithLengthFromFullKey(fullRecommendationKey, 3);
        assertThat(gotPrefix3).isEqualTo(expectedPrefix3);

        RecommendationKey gotPrefix4 = getKeyPrefixWithLengthFromFullKey(fullRecommendationKey, 4);
        assertThat(gotPrefix4).isEqualTo(expectedPrefix4);

    }

    @Test
    void testGetAllPossiblePrefixesFromGdiKeys() {
        Long clientId = 1L;
        Long cid = 2L;
        Long pid = 3L;
        Long bid1 = 4L;
        Long bid2 = 5L;
        Long timestamp = 1234567L;

        GdiRecommendationType type = GdiRecommendationType.dailyBudget;

        GdiRecommendation gdiRecommendation1 = new GdiRecommendation()
                .withClientId(clientId)
                .withType(type)
                .withCid(cid)
                .withPid(pid)
                .withBid(bid1)
                .withUserKey1("")
                .withUserKey2("")
                .withUserKey3("")
                .withTimestamp(timestamp);

        GdiRecommendation gdiRecommendation2 = new GdiRecommendation()
                .withClientId(clientId)
                .withType(type)
                .withCid(cid)
                .withPid(pid)
                .withBid(bid2)
                .withUserKey1("")
                .withUserKey2("")
                .withUserKey3("")
                .withTimestamp(timestamp);

        RecommendationKey prefixWithCampaignId =
                new RecommendationKey().withClientId(clientId).withType(type.getId()).withCampaignId(cid);

        RecommendationKey prefixWithAdGroupId =
                new RecommendationKey().withClientId(clientId).withType(type.getId()).withCampaignId(cid).withAdGroupId(pid);

        RecommendationKey prefixWithBannerId1 =
                new RecommendationKey().withClientId(clientId).withType(type.getId()).withCampaignId(cid).withAdGroupId(pid).withBannerId(bid1);

        RecommendationKey prefixWithBannerId2 =
                new RecommendationKey().withClientId(clientId).withType(type.getId()).withCampaignId(cid).withAdGroupId(pid).withBannerId(bid2);

        RecommendationKey fullKey1 =
                new RecommendationKey().withClientId(clientId).withType(type.getId()).withCampaignId(cid).withAdGroupId(pid).withBannerId(bid1).withUserKey1("").withUserKey2("").withUserKey3("").withTimestamp(timestamp);

        RecommendationKey fullKey2 =
                new RecommendationKey().withClientId(clientId).withType(type.getId()).withCampaignId(cid).withAdGroupId(pid).withBannerId(bid2).withUserKey1("").withUserKey2("").withUserKey3("").withTimestamp(timestamp);

        Map<RecommendationKey, List<RecommendationKey>> expected = ImmutableMap.of(
                prefixWithCampaignId, ImmutableList.of(fullKey1, fullKey2),
                prefixWithAdGroupId, ImmutableList.of(fullKey1, fullKey2),
                prefixWithBannerId1, ImmutableList.of(fullKey1),
                prefixWithBannerId2, ImmutableList.of(fullKey2)
        );

        Map<RecommendationKey, List<RecommendationKey>> got =
                getAllPossiblePrefixesFromGdiKeys(ImmutableSet.of(gdiRecommendation1,
                        gdiRecommendation2));

        assertThat(got).isNotEmpty().hasSize(4);
        assertThat(got).isEqualTo(expected);
    }

    @Test
    void testGetAllPossiblePrefixesFromGdiKeys_ZeroBid() {
        Long clientId = 1L;
        Long cid = 2L;
        Long pid = 3L;
        Long bid = 0L;
        Long timestamp = 1234567L;

        GdiRecommendationType type = GdiRecommendationType.dailyBudget;

        GdiRecommendation gdiRecommendation = new GdiRecommendation()
                .withClientId(clientId)
                .withType(type)
                .withCid(cid)
                .withPid(pid)
                .withBid(bid)
                .withUserKey1("")
                .withUserKey2("")
                .withUserKey3("")
                .withTimestamp(timestamp);


        RecommendationKey prefixWithCampaignId =
                new RecommendationKey().withClientId(clientId).withType(type.getId()).withCampaignId(cid);

        RecommendationKey prefixWithAdGroupId =
                new RecommendationKey().withClientId(clientId).withType(type.getId()).withCampaignId(cid).withAdGroupId(pid);

        RecommendationKey prefixWithBannerId =
                new RecommendationKey().withClientId(clientId).withType(type.getId()).withCampaignId(cid).withAdGroupId(pid).withBannerId(bid);

        RecommendationKey fullKey =
                new RecommendationKey().withClientId(clientId).withType(type.getId()).withCampaignId(cid).withAdGroupId(pid).withBannerId(bid).withUserKey1("").withUserKey2("").withUserKey3("").withTimestamp(timestamp);

        Map<RecommendationKey, List<RecommendationKey>> expected = ImmutableMap.of(
                prefixWithCampaignId, ImmutableList.of(fullKey),
                prefixWithAdGroupId, ImmutableList.of(fullKey),
                prefixWithBannerId, ImmutableList.of(fullKey)
        );

        Map<RecommendationKey, List<RecommendationKey>> got =
                getAllPossiblePrefixesFromGdiKeys(ImmutableSet.of(gdiRecommendation));

        assertThat(got).isNotEmpty().hasSize(3);
        assertThat(got).isEqualTo(expected);
    }
}
