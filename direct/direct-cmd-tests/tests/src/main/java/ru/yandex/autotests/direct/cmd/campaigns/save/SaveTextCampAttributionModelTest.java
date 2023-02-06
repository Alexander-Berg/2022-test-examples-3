package ru.yandex.autotests.direct.cmd.campaigns.save;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.editcamp.EditCampResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение аттрибуционной модели в текстовой кампании")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
@Tag("sb_test")
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class SaveTextCampAttributionModelTest  {

    private final static String CLIENT = "at-direct-attr-model";

    private static TextBannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    private static final String MODEL_NULL_VALUE = null;                                   // NULL value
    private static final String MODEL_LAST_CLICK = "last_click";                           // at BK AttributionType = 1
    private static final String MODEL_FIRST_CLICK = "first_click";                         // at BK AttributionType = 3
    private static final String MODEL_LAST_SIGNIFICIANT_CLICK = "last_significant_click";  // at BK AttributionType = 2
    private static final String MODEL_LAST_YANDEX_DIRECT_CLICK = "last_yandex_direct_click";  // at BK AttributionType = 4

    @Parameterized.Parameter(value = 0)
    public static String attributionModel;

    @Parameterized.Parameter(value = 1)
    public static String expectedAttributionModel;

    @Parameterized.Parameters(name = "Модель аттрибуции = {0}, ожидаем = {1}")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {MODEL_NULL_VALUE, MODEL_LAST_YANDEX_DIRECT_CLICK},                  // если не передали поле то будет дефолтное поведение БК https://st.yandex-team.ru/BSDEV-55630, изменили в https://st.yandex-team.ru/DIRECT-80662
                {MODEL_LAST_CLICK, MODEL_LAST_CLICK},
                {MODEL_FIRST_CLICK, MODEL_FIRST_CLICK},
                {MODEL_LAST_SIGNIFICIANT_CLICK, MODEL_LAST_SIGNIFICIANT_CLICK}
        };
        return Arrays.asList(data);
    }

    @ClassRule
    public static DirectCmdRule cmdRule = DirectCmdRule.
            defaultClassRule().
            useAuth(true).
            as(CLIENT).
            withRules(bannersRule);

    private static SaveCampRequest request;
    private static EditCampResponse response;

    @Before
    public void beforeClass() {
        request = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_TEXT_CAMP_FULL, SaveCampRequest.class);
        request.withCid(String.valueOf(bannersRule.getCampaignId()))
                .withUlogin(CLIENT);
        request.withAttributionModel(attributionModel);
    }

    @Test
    @Description("Сохранение аттрибуционной модели для текстовой кампании")
    @TestCaseId("11009")
    public void testSaveCampAttributionModel() {
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);
        response = cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT);
        assertThat("аттрибуционная модель на кампании соответствует ожидаемой: ",
                response.getCampaign().getAttributionModel(), equalTo(expectedAttributionModel));
    }
}
