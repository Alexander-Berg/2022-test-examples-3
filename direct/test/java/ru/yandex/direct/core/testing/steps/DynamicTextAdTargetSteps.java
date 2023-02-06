package ru.yandex.direct.core.testing.steps;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.repository.DynamicTextAdTargetRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.DynamicTextAdTargetInfo;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicFeedAdTarget;
import static ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTarget;

public class DynamicTextAdTargetSteps {

    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;

    public DynamicTextAdTargetInfo createDefaultDynamicTextAdTarget(AdGroupInfo adGroupInfo) {
        DynamicTextAdTarget dynamicTextAdTargetToAdd = defaultDynamicTextAdTarget(adGroupInfo);
        return createDynamicTextAdTarget(adGroupInfo, dynamicTextAdTargetToAdd);
    }

    public DynamicTextAdTargetInfo createDynamicTextAdTarget(AdGroupInfo adGroupInfo,
                                                             DynamicTextAdTarget dynamicTextAdTargetToAdd) {
        dynamicTextAdTargetRepository.add(adGroupInfo.getShard(), singletonList(dynamicTextAdTargetToAdd));

        return new DynamicTextAdTargetInfo()
                .withDynamicTextAdTarget(dynamicTextAdTargetToAdd)
                .withAdGroupInfo(adGroupInfo);
    }

    public DynamicFeedAdTarget createDefaultDynamicFeedAdTarget(AdGroupInfo adGroupInfo) {
        DynamicFeedAdTarget dynamicFeedAdTarget = defaultDynamicFeedAdTarget(adGroupInfo);
        return createDynamicFeedAdTarget(adGroupInfo, dynamicFeedAdTarget);
    }

    public DynamicFeedAdTarget createDynamicFeedAdTarget(AdGroupInfo adGroupInfo,
                                                         DynamicFeedAdTarget dynamicFeedAdTarget) {
        dynamicTextAdTargetRepository.add(adGroupInfo.getShard(), List.of(dynamicFeedAdTarget));
        return dynamicFeedAdTarget;
    }
}
