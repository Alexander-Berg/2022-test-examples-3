package ru.yandex.direct.core.testing.data;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTargetTab;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleKind;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleType;
import ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.FeedType;
import ru.yandex.direct.core.entity.performancefilter.model.Operator;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static ru.yandex.direct.core.entity.dynamictextadtarget.utils.DynamicTextAdTargetHashUtils.getHashForDynamicFeedRules;

public class TestDynamicTextAdTargets {

    private TestDynamicTextAdTargets() {

    }

    public static DynamicTextAdTarget defaultDynamicTextAdTargetWithRules(AdGroupInfo adGroup,
                                                                          List<WebpageRule> condition) {
        return new DynamicTextAdTarget()
                .withId(null)
                .withDynamicConditionId(null)
                .withAdGroupId(adGroup.getAdGroupId())
                .withCampaignId(adGroup.getCampaignId())
                .withPrice(BigDecimal.valueOf(100))
                .withPriceContext(BigDecimal.valueOf(100))
                .withAutobudgetPriority(3)
                .withIsSuspended(false)
                .withConditionName("default test dynamic condition " + RandomStringUtils.randomNumeric(7))
                .withCondition(condition)
                .withConditionHash(DynamicTextAdTargetHashUtils.getHash(condition))
                .withConditionUniqHash(DynamicTextAdTargetHashUtils.getUniqHash(condition))
                .withTab(DynamicAdTargetTab.CONDITION);
    }

    public static DynamicTextAdTarget defaultDynamicTextAdTarget(AdGroupInfo adGroup) {
        return defaultDynamicTextAdTargetWithRules(adGroup, defaultRules());
    }

    public static DynamicTextAdTarget defaultDynamicTextAdTargetWithRandomRules(AdGroupInfo adGroup) {
        return defaultDynamicTextAdTargetWithRules(adGroup, randomRules());
    }

    public static List<WebpageRule> defaultRules() {
        return Collections.singletonList(new WebpageRule()
                .withType(WebpageRuleType.ANY));
    }

    private static List<WebpageRule> randomRules() {
        return Collections.singletonList(new WebpageRule()
                .withType(WebpageRuleType.CONTENT)
                .withKind(WebpageRuleKind.EXACT)
                .withValue(Collections.singletonList(RandomStringUtils.randomAlphabetic(7))));
    }

    public static DynamicFeedAdTarget defaultDynamicFeedAdTarget(@Nullable Long adGroupId) {
        return dynamicFeedAdTargetWithRules(defaultDynamicFeedRules())
                .withAdGroupId(adGroupId);
    }

    public static DynamicFeedAdTarget defaultDynamicFeedAdTarget(AdGroupInfo adGroupInfo) {
        return dynamicFeedAdTargetWithRules(defaultDynamicFeedRules())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId());
    }

    public static DynamicFeedAdTarget dynamicFeedAdTargetWithRandomRules(AdGroupInfo adGroupInfo) {
        return dynamicFeedAdTargetWithRules(randomDynamicFeedRules())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId());
    }

    private static DynamicFeedAdTarget dynamicFeedAdTargetWithRules(List<DynamicFeedRule> rules) {
        return new DynamicFeedAdTarget()
                .withPrice(BigDecimal.valueOf(10L))
                .withPriceContext(BigDecimal.valueOf(5L))
                .withAutobudgetPriority(null)
                .withIsSuspended(false)
                .withConditionName("test dynamic feed condition")
                .withCondition(rules)
                .withConditionHash(getHashForDynamicFeedRules(rules))
                .withBusinessType(BusinessType.RETAIL)
                .withFeedType(FeedType.YANDEX_MARKET)
                .withTab(DynamicAdTargetTab.TREE);
    }

    private static List<DynamicFeedRule> defaultDynamicFeedRules() {
        DynamicFeedRule<List<Long>> dynamicFeedRule =
                new DynamicFeedRule<>("categoryId", Operator.EQUALS, "[\"1\",\"2\"]");
        dynamicFeedRule.setParsedValue(List.of(1L, 2L));
        return List.of(dynamicFeedRule);
    }

    private static List<DynamicFeedRule> randomDynamicFeedRules() {
        Long value = RandomNumberUtils.nextPositiveLong();
        DynamicFeedRule<List<Long>> dynamicFeedRule =
                new DynamicFeedRule<>("categoryId", Operator.EQUALS, "[\"" + value + "\"]");
        dynamicFeedRule.setParsedValue(List.of(value));
        return List.of(dynamicFeedRule);
    }
}
