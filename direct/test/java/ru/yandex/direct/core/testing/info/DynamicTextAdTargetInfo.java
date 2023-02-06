package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;

public class DynamicTextAdTargetInfo {

    private DynamicTextAdTarget dynamicTextAdTarget;
    private AdGroupInfo adGroupInfo;

    public DynamicTextAdTarget getDynamicTextAdTarget() {
        return dynamicTextAdTarget;
    }

    public DynamicTextAdTargetInfo withDynamicTextAdTarget(
            DynamicTextAdTarget dynamicTextAdTarget) {
        this.dynamicTextAdTarget = dynamicTextAdTarget;
        return this;
    }

    public AdGroupInfo getAdGroupInfo() {
        return adGroupInfo;
    }

    public DynamicTextAdTargetInfo withAdGroupInfo(AdGroupInfo adGroupInfo) {
        this.adGroupInfo = adGroupInfo;
        return this;
    }

    public Long getDynamicConditionId() {
        return dynamicTextAdTarget.getDynamicConditionId();
    }
}
