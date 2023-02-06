package ru.yandex.direct.core.entity.banner;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.jooq.DSLContext;

public class FakeBannerAdGroupRelation implements BannerAdGroupRelation {
    private final Map<Long, Long> adGroupIdByBannerId;

    public FakeBannerAdGroupRelation(Map<Long, Long> adGroupIdByBannerId) {
        this.adGroupIdByBannerId = adGroupIdByBannerId;
    }

    @Override
    public Map<Long, Long> getAdGroupIdsByBannerIds(int shard, Collection<Long> bannerIds) {
        return bannerIds.stream()
                .filter(adGroupIdByBannerId::containsKey)
                .collect(Collectors.toMap(bid -> bid, adGroupIdByBannerId::get));
    }

    @Override
    public Map<Long, Long> getAdGroupIdsByBannerIds(DSLContext context, Collection<Long> bannerIds) {
        return getAdGroupIdsByBannerIds(0, bannerIds);
    }
}
