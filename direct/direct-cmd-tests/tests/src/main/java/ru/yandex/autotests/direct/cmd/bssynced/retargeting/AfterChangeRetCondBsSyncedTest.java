package ru.yandex.autotests.direct.cmd.bssynced.retargeting;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.group.Retargeting;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.singletonList;

@Aqua.Test
@Description("Проверка сброса статуса bsSynced группы при изменении условия ретаргетинга")
@Stories(TestFeatures.Retargeting.AJAX_SAVE_RETARGETING_CONDITIONS)
@Features(TestFeatures.RETARGETING)
@Tag(ObjectTag.RETAGRETING)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class AfterChangeRetCondBsSyncedTest extends AfterChangeRetCondBsSyncedTestBase {

    protected static final String CLIENT = "at-direct-retargeting16";

    public CampaignTypeEnum campaignType;

    public AfterChangeRetCondBsSyncedTest(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        retCondId = addRetargetingCondition();
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        cmdRule.withRules(bannersRule.overrideGroupTemplate(new Group()
                .withRetargetings(singletonList(new Retargeting()
                        .withRetCondId(retCondId)
                        .withPriceContext("3")))));
    }

    @Parameterized.Parameters(name = "Сброс bsSynced группы после изменения условия ретаргетинга. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Override
    protected String getClient() {
        return CLIENT;
    }

    @Test
    @Description("Проверяем сброс статуса bsSynced группы при изменении названия ретаргетинга")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9357")
    public void checkBsSyncedAfterChangeRetargetingCondNameTest() {
        changeRetargeting();

        BsSyncedHelper.checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO);
    }

}
