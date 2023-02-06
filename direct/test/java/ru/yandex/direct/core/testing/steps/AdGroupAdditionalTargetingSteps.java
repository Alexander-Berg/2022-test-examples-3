package ru.yandex.direct.core.testing.steps;

import java.util.List;

import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.repository.AdGroupAdditionalTargetingRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;

import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class AdGroupAdditionalTargetingSteps {
    private final AdGroupAdditionalTargetingRepository additionalTargetingRepository;

    public AdGroupAdditionalTargetingSteps(AdGroupAdditionalTargetingRepository additionalTargetingRepository) {
        this.additionalTargetingRepository = additionalTargetingRepository;
    }

    public void addValidTargetingsToAdGroup(AdGroupInfo adGroupInfo, List<AdGroupAdditionalTargeting> validTargetings) {
        List<AdGroupAdditionalTargeting> targetings = mapList(
                validTargetings, t -> t.withAdGroupId(adGroupInfo.getAdGroupId()));
        additionalTargetingRepository.add(adGroupInfo.getShard(), adGroupInfo.getClientId(), targetings);
    }

    public void deleteTargetingFromMySql(Integer shard, Long id) {
        additionalTargetingRepository.deleteByIds(shard, List.of(id));
    }

}
