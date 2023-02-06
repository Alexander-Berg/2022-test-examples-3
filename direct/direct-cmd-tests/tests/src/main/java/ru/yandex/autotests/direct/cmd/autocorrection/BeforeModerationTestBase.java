package ru.yandex.autotests.direct.cmd.autocorrection;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.BeforeModeration;
import ru.yandex.autotests.direct.cmd.data.commons.ModEdit;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ModEditRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;

@RunWith(Parameterized.class)
public abstract class BeforeModerationTestBase {

    protected static final String CLIENT = "at-direct-misspells";

    public CampaignTypeEnum campaignType;
    public BannersRule campaignRule;
    @Rule
    public DirectCmdRule cmdRule;
    protected Long campaignId;
    protected Long bannerId;
    protected BeforeModeration expectedBean;

    public BeforeModerationTestBase(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        this.campaignRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);
    }

    @Parameterized.Parameters(name = "тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DTO}
        });
    }

    @Before
    public void before() {
        cmdRule.getApiStepsRule().as(Logins.SUPER);

        campaignId = campaignRule.getCampaignId();
        bannerId = campaignRule.getBannerId();
        ModEditRecord modEditRecord = getModEditRecord();
        expectedBean = BeforeModeration.fromModEditRecord(modEditRecord);

        cmdRule.apiSteps().adsSteps().adsModerate(CLIENT, bannerId);
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).modEditSteps().addModEdit(modEditRecord);
    }

    private ModEditRecord getModEditRecord() {
        ModEditRecord modEditRecord = BeanLoadHelper.loadCmdBean(
                CmdBeans.MOD_EDIT_TEMPLATE, ModEdit.class).createModEditRecord();
        modEditRecord.setCreatetime(Timestamp.valueOf(LocalDateTime.now()));
        modEditRecord.setId(bannerId);
        return modEditRecord;
    }
}
