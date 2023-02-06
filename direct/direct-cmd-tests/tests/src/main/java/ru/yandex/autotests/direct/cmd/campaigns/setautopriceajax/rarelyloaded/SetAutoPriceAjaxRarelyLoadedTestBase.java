package ru.yandex.autotests.direct.cmd.campaigns.setautopriceajax.rarelyloaded;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.cmd.data.CmdBeansMaps;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.campaigns.SetAutoPriceAjaxRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;


@RunWith(Parameterized.class)
public abstract class SetAutoPriceAjaxRarelyLoadedTestBase {

    private static final String CLIENT = "at-direct-backend-c";
    protected static final Double EXPECTED_PRICE = 100d;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    protected BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    private CampaignTypeEnum campaignType;
    protected SetAutoPriceAjaxRequest autoPriceAjaxRequest;
    protected int shard;

    @Parameterized.Parameters(name = "Проверка отсутствия данных торгов при мало показов. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
        });
    }

    public SetAutoPriceAjaxRarelyLoadedTestBase(CampaignTypeEnum campaignType) {
        this.campaignType = campaignType;
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .overrideCampTemplate(new SaveCampRequest().withJsonStrategy(getCampStrategy()))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    protected abstract CampaignStrategy getCampStrategy();

    @Before
    public void before() {
        shard = TestEnvironment.newDbSteps().shardingSteps().getShardByLogin(CLIENT);
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(),
                BeanLoadHelper.loadCmdBean(CmdBeansMaps.MEDIA_TYPE_TO_GROUP_TEMPLATE.get(campaignType), Group.class)));
        assumeThat("в кампании две группы", cmdRule.cmdSteps().groupsSteps()
                .getGroups(CLIENT, bannersRule.getCampaignId()), hasSize(2));

        TestEnvironment.newDbSteps().useShard(shard).adGroupsSteps()
                .setBsRarelyLoaded(bannersRule.getGroupId(), true);
    }

    protected void setPrice() {
        CommonResponse response = cmdRule.cmdSteps().ajaxCampaignSteps().postSetAutoPriceAjax(autoPriceAjaxRequest);
        assumeThat("сохранение ставки выполнено успешно", response.getSuccess(), equalTo("1"));

        cmdRule.darkSideSteps().getRunScriptSteps().runPpcCampAutoPrice(shard, bannersRule.getCampaignId().intValue());
    }

    protected Phrase getGroupPhrase(Long groupId) {
        return cmdRule.cmdSteps().groupsSteps()
                .getGroup(CLIENT, bannersRule.getCampaignId(), groupId)
                .getPhrases().get(0);
    }

    protected String getSecondGroupId() {
        return cmdRule.cmdSteps().groupsSteps()
                .getGroups(CLIENT, bannersRule.getCampaignId()).stream()
                .filter(t -> !bannersRule.getGroupId().toString().equals(t.getAdGroupID()))
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Ожидалось что в кампании есть другая группа"))
                .getAdGroupID();
    }
}
