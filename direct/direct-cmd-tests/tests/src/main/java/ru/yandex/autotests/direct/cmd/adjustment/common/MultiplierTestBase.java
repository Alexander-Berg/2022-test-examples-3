package ru.yandex.autotests.direct.cmd.adjustment.common;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.data.Logins;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.qatools.allure.annotations.Description;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public abstract class MultiplierTestBase {

    protected final static String CLIENT = "at-direct-backend-c";
    protected final static String VALID_MULTIPLIER = "120";
    private final static String SUPER = Logins.SUPER;
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected SaveCampRequest saveCampRequest;
    private CampaignRule campaignRule = new CampaignRule().
            withMediaType(getCampaignTypeEnum()).
            withUlogin(getClient());
    private Long campaignIdToRemove;
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);

    @Before
    public void before() {
        saveCampRequest = campaignRule.getSaveCampRequest()
                .withMobileAppId(null)
                .withHierarhicalMultipliers(getHierarchicalMultipliers());
    }

    @After
    public void delete() {
        if (campaignIdToRemove != null) {
            if (getCampaignTypeEnum() == CampaignTypeEnum.DMO) {
                cmdRule.apiAggregationSteps().makeCampaignReadyForDelete(campaignIdToRemove);
                cmdRule.cmdSteps().campaignSteps().deleteCampaign(CLIENT, campaignIdToRemove);
            } else {
                cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, campaignIdToRemove);
            }
        }
    }

    @Description("Проверяем сохранение корректировок ставок контроллером saveNewCamp")
    public void checkSaveMobileMultiplierAtSaveNewCamp() {
        Long cid = cmdRule.cmdSteps().campaignSteps().saveNewCampaign(saveCampRequest);
        campaignIdToRemove = cid;
        check(cid);
    }

    @Description("Проверяем сохранение корректировок ставок контроллером saveCamp")
    public void checkSaveMobileMultiplierAtSaveCamp() {
        saveCampRequest.withCid(campaignRule.getCampaignId().toString());
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(saveCampRequest);
        check(campaignRule.getCampaignId());
    }

    protected void check(Long cid) {
        EditCampResponse editCampResponse = cmdRule.cmdSteps().campaignSteps().getEditCamp(cid, getClient());
        HierarchicalMultipliers hierarchicalMultipliers = editCampResponse.getCampaign()
                .getHierarchicalMultipliers();

        assertThat("корректировка ставок сохранилась", hierarchicalMultipliers,
                beanDiffer(getExpectedHierarchicalMultipliers()).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    protected abstract HierarchicalMultipliers getHierarchicalMultipliers();

    protected HierarchicalMultipliers getExpectedHierarchicalMultipliers() {
        return getHierarchicalMultipliers();
    }

    protected String getClient() {
        return CLIENT;
    }

    protected CampaignTypeEnum getCampaignTypeEnum() {
        return CampaignTypeEnum.TEXT;
    }
}
