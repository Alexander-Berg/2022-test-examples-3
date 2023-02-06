package ru.yandex.direct.core.testing.steps;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.testing.repository.TestBannerModerationVersionsRepository;

public class BannerModerationVersionSteps {

    @Autowired
    private TestBannerModerationVersionsRepository testBannerModerationVersionsRepository;

    public void addBannerModerationVersion(int shard, Long bannerId, Long version) {
        testBannerModerationVersionsRepository.addVersion(shard, bannerId, version);
    }

    public Map<Long, Long> getBannerModerationVersionByBannerId(int shard, Collection<Long> bannerIds) {
        return testBannerModerationVersionsRepository.getVersion(shard, bannerIds);
    }
}
