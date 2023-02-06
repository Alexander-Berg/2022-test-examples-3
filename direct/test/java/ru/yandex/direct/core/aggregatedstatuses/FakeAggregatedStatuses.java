package ru.yandex.direct.core.aggregatedstatuses;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AggregatedStatusAdData;
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AggregatedStatusAdGroupData;
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData;
import ru.yandex.direct.core.entity.aggregatedstatuses.keyword.AggregatedStatusKeywordData;
import ru.yandex.direct.core.entity.aggregatedstatuses.retargeting.AggregatedStatusRetargetingData;
import ru.yandex.direct.dbutil.model.ClientId;

public class FakeAggregatedStatuses implements AggregatedStatuses {
    private final Map<Long, AggregatedStatusCampaignData> campaignById;
    private final Map<Long, Map<Long, AggregatedStatusAdGroupData>> adGroupByCampaignIdData;
    private final Map<Long, AggregatedStatusAdGroupData> adGroupByIdData;
    private final Map<Long, Map<Long, AggregatedStatusAdData>> adByAdGroupIdData;
    private final Map<Long, Map<Long, AggregatedStatusKeywordData>> keywordByAdGroupIdData;
    private final Map<Long, Map<Long, AggregatedStatusRetargetingData>> retargetingByAdGroupIdData;

    public FakeAggregatedStatuses(
            Map<Long, AggregatedStatusCampaignData> campaignById,
            Map<Long, Map<Long, AggregatedStatusAdGroupData>> adGroupByCampaignIdData,
            Map<Long, AggregatedStatusAdGroupData> adGroupByIdData,
            Map<Long, Map<Long, AggregatedStatusAdData>> adByAdGroupIdData,
            Map<Long, Map<Long, AggregatedStatusKeywordData>> keywordByAdGroupIdData,
            Map<Long, Map<Long, AggregatedStatusRetargetingData>> retargetingByAdGroupIdData
    ) {
        this.campaignById = campaignById;
        this.adGroupByCampaignIdData = adGroupByCampaignIdData;
        this.adGroupByIdData = adGroupByIdData;
        this.adByAdGroupIdData = adByAdGroupIdData;
        this.keywordByAdGroupIdData = keywordByAdGroupIdData;
        this.retargetingByAdGroupIdData = retargetingByAdGroupIdData;
    }

    @Override
    public Map<Long, AggregatedStatusCampaignData> getCampaignStatusesByIds(int shard, Collection<Long> cids) {
        return cids.stream().filter(campaignById::containsKey).collect(Collectors.toMap(Function.identity(), campaignById::get));
    }

    @Override
    public Map<Long, AggregatedStatusAdData> getAdStatusesByIds(int shard, Set<Long> bids) {
        final var allBannersData = adByAdGroupIdData.values().stream().reduce(new HashMap<>(), (a, b) -> {
            a.putAll(b);

            return a;
        });

        final var result = new HashMap<Long, AggregatedStatusAdData>();
        for (var bid : bids) {
            if (allBannersData.containsKey(bid)) {
                result.put(bid, allBannersData.get(bid));
            }
        }

        return result;
    }

    @Override
    public Map<Long, AggregatedStatusAdGroupData> getAdGroupStatusesByCampaignId(int shard, ClientId clientId,
                                                                                 Long campaignId) {
        return adGroupByCampaignIdData.getOrDefault(campaignId, new HashMap<>());
    }

    @Override
    public Map<Long, AggregatedStatusAdData> getAdStatusesByAdgroupIds(int shard, ClientId clientId,
                                                                       Collection<Long> adgroupIds) {
        return adgroupIds.stream().map(adByAdGroupIdData::get).filter(Objects::nonNull).reduce(new HashMap<>(), (a, b) -> {
            a.putAll(b);
            return a;
        });
    }

    @Override
    public Map<Long, AggregatedStatusKeywordData> getKeywordStatusesByAdgroupIds(int shard, ClientId clientId,
                                                                                Collection<Long> adgroupIds) {
        return adgroupIds.stream().map(keywordByAdGroupIdData::get).filter(Objects::nonNull).reduce(new HashMap<>(), (a, b) -> {
            a.putAll(b);
            return a;
        });
    }

    @Override
    public Map<Long, AggregatedStatusRetargetingData> getRetargetingStatusesByAdgroupIds(int shard, ClientId clientId,
                                                                                        Collection<Long> adgroupIds) {
        return adgroupIds.stream().map(retargetingByAdGroupIdData::get).filter(Objects::nonNull).reduce(new HashMap<>(), (a, b) -> {
            a.putAll(b);
            return a;
        });
    }

    @Override
    public Map<Long, AggregatedStatusAdGroupData> getAdGroupStatusesByIds(int shard, Set<Long> pids) {
        return pids.stream().filter(adGroupByIdData::containsKey).collect(Collectors.toMap(Function.identity(), adGroupByIdData::get));
    }
}
