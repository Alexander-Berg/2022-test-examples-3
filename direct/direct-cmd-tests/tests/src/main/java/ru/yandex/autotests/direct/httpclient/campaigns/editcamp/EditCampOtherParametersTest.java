package ru.yandex.autotests.direct.httpclient.campaigns.editcamp;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.campaigns.SaveCampParameters;
import ru.yandex.autotests.direct.httpclient.data.campaigns.campaignInfo.CampaignInfoCmd;
import ru.yandex.autotests.direct.httpclient.data.campaigns.editcamp.EditCampResponse;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.httpclient.CocaineSteps.getCsrfTokenFromCocaine;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer.BeanDifferMatcher.beanEquivalent;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 14.04.15
 *         https://st.yandex-team.ru/TESTIRT-4958
 */

@Aqua.Test
@Description("Проверка контроллера editCamp")
@Stories(TestFeatures.Campaigns.EDIT_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(CampTypeTag.TEXT)
@Tag(CmdTag.EDIT_CAMP)
@Tag(OldTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class EditCampOtherParametersTest {
    private final List<Integer> ALL_WORLD = Arrays.asList(225, 166, 111, 183, 241, 10002, 10003, 138);
    private final List<Integer> SINGLE_REGION = Arrays.asList(213, 10716, 2, 159);

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private static String CLIENT = "at-direct-backend-c";
    private TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    private SaveCampParameters saveCampParameters;
    private CampaignInfoCmd expectedCampaignInfoCmd;

    @Before
    public void before() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
        saveCampParameters = cmdRule.oldSteps().clientSteps().getDefaultSaveNewCampParameters();
        saveCampParameters.setCid(String.valueOf(bannersRule.getCampaignId()));
        expectedCampaignInfoCmd = new CampaignInfoCmd();
    }

    private void checkEditCampParameter() {
        CSRFToken csrfToken = getCsrfTokenFromCocaine(User.get(CLIENT).getPassportUID());
        cmdRule.oldSteps().onSaveCamp().saveCamp(csrfToken, saveCampParameters);
        EditCampResponse editCampResponse =
                cmdRule.oldSteps().onEditCamp().getEditCampResponse(bannersRule.getCampaignId().toString(), CLIENT);
        String sortedGeo = Arrays.stream(editCampResponse.getCampaign().getGeo().split(","))
                .map(Integer::parseInt)
                .sorted(Comparator.naturalOrder())
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        editCampResponse.getCampaign().setGeo(sortedGeo);
        assertThat("Ответ контроллера совпадает с ожидаемым", editCampResponse.getCampaign(),
                beanEquivalent(expectedCampaignInfoCmd));
    }


    @Test
    @Description("Проверяем единый регион кампании в ответе контроллера editCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10334")
    public void editCampRegionTest() {
        saveCampParameters.setGeo(SINGLE_REGION.stream()
                .sorted(Comparator.naturalOrder())
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        expectedCampaignInfoCmd.setGeo(saveCampParameters.getGeo());
        checkEditCampParameter();
    }

    @Test
    @Description("Проверяем установку значения 'Весь мир' в качетсве единого региона кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10335")
    public void editCampRegionAllWorldTest() {
        saveCampParameters.setGeo(ALL_WORLD.stream()
                .sorted(Comparator.naturalOrder())
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        expectedCampaignInfoCmd.setGeo(saveCampParameters.getGeo());
        checkEditCampParameter();
    }

    @Test
    @Description("Проверяем единый набор минус слов кампании в ответе контроллера editCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10336")
    public void editCampMinusWordsTest() {
        saveCampParameters.setJson_campaign_minus_words(Arrays.asList("минус", "слова"));
        expectedCampaignInfoCmd.setMinusKeywords(saveCampParameters.getJson_campaign_minus_words().getMinusWords());
        checkEditCampParameter();
    }

    @Test
    @Description("Проверяем значение 0% расходов в настройках на тематических площадках editCamp")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10331")
    public void editCampPriceCoefficientZeroPercentTest() {
        saveCampParameters.setContextLimit("0");
        saveCampParameters.setContextPriceCoef("100");
        expectedCampaignInfoCmd.setContextLimitSum(Integer.valueOf(saveCampParameters.getContextLimit()));
        expectedCampaignInfoCmd.setContextPricePercent(Integer.valueOf(saveCampParameters.getContextPriceCoef()));
        checkEditCampParameter();
    }
}
