package ru.yandex.autotests.direct.cmd.adjustment.common;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.CampaignErrorResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.allure.annotations.Description;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

public abstract class MultiplierShowsNegativeAtCampTestBase {
    protected final static String VALID_MULTIPLIER = "120";
    private final static String CLIENT = "at-direct-backend-c";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected SaveCampRequest saveCampRequest;
    private CampaignRule campaignRule = new CampaignRule().
            withMediaType(getCampaignType()).
            withUlogin(getClient());
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);

    @Before
    public void before() {
        saveCampRequest = campaignRule.getSaveCampRequest().
                withHierarhicalMultipliers(getHierarchicalMultipliers());
    }

    protected String getClient() {
        return CLIENT;
    }

    protected abstract HierarchicalMultipliers getHierarchicalMultipliers();

    protected abstract String getErrorText();

    protected CampaignTypeEnum getCampaignType() {
        return CampaignTypeEnum.TEXT;
    }

    @Description("Проверяем сохранение некорректных корректировок контроллером saveNewCamp")
    public void checkSaveInvalidMobileMultiplierAtSaveNewCamp() {
        CampaignErrorResponse errorResponse = cmdRule.cmdSteps().campaignSteps().
                postSaveNewCampInvalidData(saveCampRequest);

        assertThat("при создании кампании вернулась ошибка", errorResponse.getCampaignErrors().getError(),
                equalTo(getErrorText()));
    }

    @Description("Проверяем сохранение некорректных корректировок контроллером saveCamp")
    public void checkSaveInvalidMobileMultiplierAtSaveCamp() {
        saveCampRequest.withCid(campaignRule.getCampaignId().toString());
        CampaignErrorResponse errorResponse = cmdRule.cmdSteps().campaignSteps().
                postSaveCampInvalidData(saveCampRequest);

        assertThat("при сохранении кампании вернулась ошибка", errorResponse.getCampaignErrors().getError(),
                equalTo(getErrorText()));
    }
}
