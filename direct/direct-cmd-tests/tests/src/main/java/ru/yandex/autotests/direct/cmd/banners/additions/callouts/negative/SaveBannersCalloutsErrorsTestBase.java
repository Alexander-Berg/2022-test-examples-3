package ru.yandex.autotests.direct.cmd.banners.additions.callouts.negative;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.cmd.banners.additions.callouts.CalloutsTestHelper;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/*
* todo javadoc
*/
@RunWith(Parameterized.class)
public abstract class SaveBannersCalloutsErrorsTestBase {
    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();
    public String[] callouts;
    public String errorText;
    @Rule
    public DirectCmdRule cmdRule;
    protected CampaignRule campaignRule;
    protected CalloutsTestHelper helper;

    public SaveBannersCalloutsErrorsTestBase(String[] callouts, String errorText) {
        this.callouts = callouts;
        this.errorText = errorText;
        campaignRule = new CampaignRule().withMediaType(getCampType()).withUlogin(getUlogin());
        cmdRule = DirectCmdRule.defaultRule().withRules(campaignRule);
    }

    protected abstract String getUlogin();

    protected abstract CampaignTypeEnum getCampType();

    @Before
    public void setUp() {
        helper = new CalloutsTestHelper(getUlogin(), cmdRule.cmdSteps(),
                campaignRule.getCampaignId().toString());
        helper.clearCalloutsForClient();
    }

    @Description("Ошибка при сохранении текстового дополнения")
    public void errorOnSave() {
        String error = saveAndGetError(callouts);
        assertThat("получили ошибку", error, containsString(errorText));
    }

    @Description("Текстовое дополнение не сохраняется в базу в случае ошибки")
    public void calloutsNotSaved() {
        saveAndGetError(callouts);
        List<String> callouts = cmdRule.cmdSteps().bannersAdditionsSteps().getCalloutsList(getUlogin());
        assertThat("ни одного дополнения не сохранилось", callouts, hasSize(0));
    }


    protected abstract String saveAndGetError(String... callouts);
}
