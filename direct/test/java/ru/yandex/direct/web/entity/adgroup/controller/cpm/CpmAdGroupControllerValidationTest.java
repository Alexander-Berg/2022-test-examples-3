package ru.yandex.direct.web.entity.adgroup.controller.cpm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebAutoPrice;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroup;
import ru.yandex.direct.web.entity.adgroup.model.WebCpmAdGroupRetargeting;
import ru.yandex.direct.web.entity.adgroup.model.WebRetargetingGoal;
import ru.yandex.direct.web.entity.adgroup.model.WebRetargetingRule;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.CurrencyAmountDefects.CPM_PRICE_IS_NOT_GREATER_THAN_MIN;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.CurrencyAmountDefects.CPM_PRICE_IS_NOT_SMALLER_THAN_MAX;
import static ru.yandex.direct.web.testing.data.TestAdGroups.cpmAdGroupRetargeting;
import static ru.yandex.direct.web.testing.data.TestAdGroups.defaultCpmAdGroupRetargeting;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmBannerAdGroup;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebCpmVideoAdGroup;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

@DirectWebTest
@RunWith(SpringRunner.class)
public class CpmAdGroupControllerValidationTest extends CpmAdGroupControllerTestBase {

    @Test
    public void addCpmAdGroup_nullAdGroup() {
        addAndExpectError(null, "[0]", DefectIds.CANNOT_BE_NULL.getCode());
    }

    @Test
    public void addCpmAdGroup_RetargetingNullPrice() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmVideoAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(cpmAdGroupRetargeting(null, interestsRetCondId)
                        .withPriceContext(null)));
        addAndExpectError(webCpmAdGroup,
                "[0]." + WebCpmAdGroup.Prop.RETARGETINGS + "[0]." + WebCpmAdGroupRetargeting.Prop.PRICE_CONTEXT,
                DefectIds.CANNOT_BE_NULL.getCode());
    }

    @Test
    public void addCpmAdGroup_RetargetingWrongPrice() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmVideoAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(cpmAdGroupRetargeting(null, interestsRetCondId)
                        .withPriceContext(1.0)));
        addAndExpectError(webCpmAdGroup,
                "[0]." + WebCpmAdGroup.Prop.RETARGETINGS + "[0]." + WebCpmAdGroupRetargeting.Prop.PRICE_CONTEXT,
                CPM_PRICE_IS_NOT_GREATER_THAN_MIN.getCode());
    }

    @Test
    public void addCpmAdGroup_RetargetingNullName() {
        WebRetargetingRule rule = new WebRetargetingRule()
                .withRuleType(RuleType.OR)
                .withGoals(singletonList(new WebRetargetingGoal()
                        .withId(publicGoal.getId())
                        .withGoalType(publicGoal.getType())));

        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(defaultCpmAdGroupRetargeting()
                        .withName(null)
                        .withGroups(singletonList(rule))));

        addAndExpectError(requestAdGroup,
                "[0]." + WebCpmAdGroup.Prop.RETARGETINGS + "[0]." + WebCpmAdGroupRetargeting.Prop.NAME,
                DefectIds.CANNOT_BE_NULL.getCode());
    }

    @Test
    public void addCpmAdGroup_RetargetingWrongGoalId() {
        WebRetargetingRule rule = new WebRetargetingRule()
                .withRuleType(RuleType.OR)
                .withGoals(singletonList(new WebRetargetingGoal()
                        .withId(-1L)
                        .withGoalType(GoalType.SOCIAL_DEMO)));

        WebCpmAdGroup requestAdGroup = randomNameWebCpmBannerAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(defaultCpmAdGroupRetargeting()
                        .withGroups(singletonList(rule))));

        addAndExpectError(requestAdGroup,
                "[0]." + WebCpmAdGroup.Prop.RETARGETINGS + "[0]." + WebCpmAdGroupRetargeting.Prop.GROUPS + "[0]."
                        + WebRetargetingRule.Prop.GOALS + "[0]." + WebRetargetingGoal.Prop.ID,
                DefectIds.MUST_BE_VALID_ID.getCode());
    }

    @Test
    public void addCpmAdGroup_KeywordWrongPrice() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmVideoAdGroup(null, campaignInfo.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(null)))
                .withGeneralPrice(5000.0);
        addAndExpectError(webCpmAdGroup,
                "[0]." + WebCpmAdGroup.Prop.AUTO_PRICE + "." + WebAutoPrice.Prop.GENERAL_PRICE,
                CPM_PRICE_IS_NOT_SMALLER_THAN_MAX.getCode());
    }

    @Test
    public void updateCpmAdGroup_nullAdGroup() {
        updateAndExpectError(null, "[0]", DefectIds.CANNOT_BE_NULL.getCode());
    }

    @Test
    public void updateCpmAdGroup_AndRetargetingNullPrice() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmVideoAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(cpmAdGroupRetargeting(null, interestsRetCondId)
                        .withPriceContext(null)));
        updateAndExpectError(webCpmAdGroup,
                "[0]." + WebCpmAdGroup.Prop.RETARGETINGS + "[0]." + WebCpmAdGroupRetargeting.Prop.PRICE_CONTEXT,
                DefectIds.CANNOT_BE_NULL.getCode());
    }

    @Test
    public void updateCpmAdGroup_RetargetingWrongPrice() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmVideoAdGroup(null, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(cpmAdGroupRetargeting(null, interestsRetCondId)
                        .withPriceContext(1.0)));
        updateAndExpectError(webCpmAdGroup,
                "[0]." + WebCpmAdGroup.Prop.RETARGETINGS + "[0]." + WebCpmAdGroupRetargeting.Prop.PRICE_CONTEXT,
                CPM_PRICE_IS_NOT_GREATER_THAN_MIN.getCode());
    }

    @Test
    public void updateCpmAdGroup_KeywordWrongPrice() {
        WebCpmAdGroup webCpmAdGroup = randomNameWebCpmVideoAdGroup(null, campaignInfo.getCampaignId())
                .withKeywords(singletonList(randomPhraseKeyword(null)))
                .withGeneralPrice(5000.0);
        updateAndExpectError(webCpmAdGroup,
                "[0]." + WebCpmAdGroup.Prop.AUTO_PRICE + "." + WebAutoPrice.Prop.GENERAL_PRICE,
                CPM_PRICE_IS_NOT_SMALLER_THAN_MAX.getCode());
    }

}
