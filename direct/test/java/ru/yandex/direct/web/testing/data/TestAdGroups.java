package ru.yandex.direct.web.testing.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.dbschema.ppc.enums.PhrasesAdgroupType;
import ru.yandex.direct.web.core.model.retargeting.CryptaInterestTypeWeb;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupRelevanceMatch;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupRetargeting;
import ru.yandex.direct.web.entity.adgroup.model.WebAutoPrice;
import ru.yandex.direct.web.entity.adgroup.model.WebContentPromotionAdGroup;
import ru.yandex.direct.web.entity.adgroup.model.WebContentPromotionAdGroupType;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroup;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroupRetargeting;
import ru.yandex.direct.web.entity.adgroup.model.WebPageBlock;
import ru.yandex.direct.web.entity.adgroup.model.WebRetargetingGoal;
import ru.yandex.direct.web.entity.adgroup.model.WebRetargetingRule;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_FAMILY_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_INTERESTS_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.CRYPTA_SOCIAL_DEMO_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_AUDIENCE_UPPER_BOUND;
import static ru.yandex.direct.core.entity.retargeting.model.Goal.METRIKA_GOAL_UPPER_BOUND;

public final class TestAdGroups {

    private TestAdGroups() {
    }

    public static WebTextAdGroup randomNameWebAdGroup(Long id, Long campaignId) {
        return new WebTextAdGroup()
                .withId(id)
                .withCampaignId(campaignId)
                .withName(randomAlphabetic(5))
                .withMinusKeywords(singletonList(randomAlphabetic(5)))
                .withGeo("0");
    }

    public static WebCpmAdGroup randomNameWebCpmBannerAdGroup(Long id, Long campaignId) {
        return typelessCpmAdGroup(id, campaignId)
                .withCpmBannersType(PhrasesAdgroupType.cpm_banner.getLiteral())
                .withGeo("0");
    }

    public static WebContentPromotionAdGroup randomNameWebContentPromotionVideoAdGroup(Long id, Long campaignId) {
        return typelessContentPromotionAdGroup(id, campaignId)
                .withGeo("0");
    }

    public static WebContentPromotionAdGroup randomNameWebContentPromotionCollectionAdGroup(Long id, Long campaignId) {
        return typelessContentPromotionAdGroup(id, campaignId)
                .withGeo("0")
                .withAdGroupContentType(WebContentPromotionAdGroupType.COLLECTION);
    }

    public static WebCpmAdGroup randomNameWebCpmVideoAdGroup(Long id, Long campaignId) {
        return typelessCpmAdGroup(id, campaignId)
                .withCpmBannersType(PhrasesAdgroupType.cpm_video.getLiteral())
                .withGeo("0");
    }

    public static WebCpmAdGroup randomNameWebCpmAudioAdGroup(Long id, Long campaignId) {
        return typelessCpmAdGroup(id, campaignId)
                .withCpmBannersType(PhrasesAdgroupType.cpm_audio.getLiteral())
                .withGeo("0");
    }

    public static WebCpmAdGroup randomNameWebCpmOutdoorAdGroup(Long id, Long campaignId, WebPageBlock pageBlock) {
        return typelessCpmAdGroup(id, campaignId)
                .withCpmBannersType(PhrasesAdgroupType.cpm_outdoor.getLiteral())
                .withPageBlocks(singletonList(pageBlock));
    }

    public static WebCpmAdGroup randomNameWebCpmIndoorAdGroup(Long id, Long campaignId, WebPageBlock pageBlock) {
        return typelessCpmAdGroup(id, campaignId)
                .withCpmBannersType(PhrasesAdgroupType.cpm_indoor.getLiteral())
                .withPageBlocks(singletonList(pageBlock));
    }

    public static WebCpmAdGroup randomNameWebCpmYndxFrontpageAdGroup(Long id, Long campaignId) {
        return typelessCpmAdGroup(id, campaignId)
                .withGeo("0");
    }

    public static WebCpmAdGroup randomNameWebCpmGeoproductAdGroup(Long id, Long campaignId) {
        return typelessCpmAdGroup(id, campaignId)
                .withCpmBannersType(PhrasesAdgroupType.cpm_geoproduct.getLiteral())
                .withGeo("0");
    }

    private static WebCpmAdGroup typelessCpmAdGroup(Long id, Long campaignId) {
        return new WebCpmAdGroup()
                .withId(id)
                .withCampaignId(campaignId)
                .withName(randomAlphabetic(5))
                .withGeo("0")
                .withAutoPrice(new WebAutoPrice());
    }

    private static WebContentPromotionAdGroup typelessContentPromotionAdGroup(Long id, Long campaignId) {
        return new WebContentPromotionAdGroup()
                .withId(id)
                .withCampaignId(campaignId)
                .withName(randomAlphabetic(5));
    }

    public static WebCpmAdGroupRetargeting cpmAdGroupRetargeting(Long retargetingId, Long retConditionId) {
        return new WebCpmAdGroupRetargeting()
                .withId(retargetingId)
                .withRetargetingConditionId(retConditionId);
    }

    public static WebCpmAdGroupRetargeting defaultCpmAdGroupRetargeting() {
        return getWebCpmAdGroupRetargeting(defaultWebRetargetingRules());
    }

    public static WebCpmAdGroupRetargeting defaultCpmIndoorRetargeting() {
        return getWebCpmAdGroupRetargeting(cpmIndoorWebRetargetingRules());
    }

    private static WebCpmAdGroupRetargeting getWebCpmAdGroupRetargeting(List<WebRetargetingRule> retargetingRules) {
        return new WebCpmAdGroupRetargeting()
                .withId(null)
                .withRetargetingConditionId(null)
                .withPriceContext(100.0)
                .withConditionType(ConditionType.interests)
                .withName("retargeting condition " + RandomStringUtils.randomNumeric(5))
                .withDescription("retargeting condition description " + RandomStringUtils.randomNumeric(5))
                .withGroups(retargetingRules);
    }


    private static List<WebRetargetingRule> cpmIndoorWebRetargetingRules() {
        List<WebRetargetingRule> rules = new ArrayList<>();
        //crypta social demo
        rules.add(new WebRetargetingRule()
                .withRuleType(RuleType.OR)
                .withGoals(singletonList(new WebRetargetingGoal()
                        .withId(RandomUtils.nextLong(METRIKA_AUDIENCE_UPPER_BOUND + 1, METRIKA_AUDIENCE_UPPER_BOUND + 9))
                        .withGoalType(GoalType.SOCIAL_DEMO))
                ));
        return rules;
    }

    private static List<WebRetargetingRule> defaultWebRetargetingRules() {
        List<WebRetargetingRule> rules = new ArrayList<>();
        //crypta social demo
        rules.add(new WebRetargetingRule()
                .withRuleType(RuleType.OR)
                .withGoals(singletonList(new WebRetargetingGoal()
                        .withId(RandomUtils.nextLong(METRIKA_AUDIENCE_UPPER_BOUND, CRYPTA_SOCIAL_DEMO_UPPER_BOUND)))));
        //metrika goal
        rules.add(new WebRetargetingRule()
                .withRuleType(RuleType.NOT)
                .withGoals(singletonList(new WebRetargetingGoal()
                        .withId(RandomUtils.nextLong(1, METRIKA_GOAL_UPPER_BOUND))
                        .withGoalType(GoalType.GOAL)
                        .withTime(90))));
        //crypta family
        rules.add(new WebRetargetingRule()
                .withRuleType(RuleType.OR)
                .withGoals(singletonList(new WebRetargetingGoal()
                        .withId(RandomUtils.nextLong(CRYPTA_SOCIAL_DEMO_UPPER_BOUND, CRYPTA_FAMILY_UPPER_BOUND)))));
        //crypta interests
        rules.add(new WebRetargetingRule()
                .withRuleType(RuleType.OR)
                .withInterestType(CryptaInterestTypeWeb.long_term)
                .withGoals(singletonList(new WebRetargetingGoal()
                        .withId(RandomUtils.nextLong(CRYPTA_FAMILY_UPPER_BOUND, CRYPTA_INTERESTS_UPPER_BOUND)))));
        return rules;
    }

    public static WebAdGroupRetargeting adGroupRetargeting(Long retConditionId) {
        return adGroupRetargeting(null, retConditionId);
    }

    public static WebAdGroupRetargeting adGroupRetargeting(Long retargetingId, Long retConditionId) {
        return new WebAdGroupRetargeting()
                .withId(retargetingId)
                .withRetargetingConditionId(retConditionId);
    }


    public static WebAdGroupRelevanceMatch webAdGroupRelevanceMatch(Long id) {
        return new WebAdGroupRelevanceMatch()
                .withId(id);
    }
}
