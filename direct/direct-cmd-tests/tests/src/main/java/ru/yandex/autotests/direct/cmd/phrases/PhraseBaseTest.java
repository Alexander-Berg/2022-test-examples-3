package ru.yandex.autotests.direct.cmd.phrases;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.qatools.allure.annotations.Description;

public abstract class PhraseBaseTest {
    protected static final String CLIENT =  "at-backend-phrase-1";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Parameterized.Parameter(value = 0)
    public Group group;
    @Parameterized.Parameter(value = 1)
    public String description;
    protected CampaignRule campaignRule = new CampaignRule()
            .withUlogin(CLIENT)
            .withMediaType(CampaignTypeEnum.TEXT).overrideCampTemplate(
                    new SaveCampRequest()
                            .withJsonStrategy(
                                    CmdStrategyBeans.getStrategyBean(Strategies.WEEKLY_BUDGET_MAX_CLICKS_DEFAULT)
                            )
            );
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().as(CLIENT).withRules(campaignRule);

    @Before
    public void before() {
    }

    @Description("сохраняем группу")
    public abstract void saveGroup();
}
