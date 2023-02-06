package ru.yandex.autotests.direct.cmd.campaigns.save;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.campaigns.CampaignErrors;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.CampaignErrorResponse;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.textresource.TextResourceFormatter;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by aliho on 10.10.17.
 */
@Aqua.Test
@Description("Валидация параметра аттрибуционной модели при создании мобильной кампании")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class SaveMobileCampAttributionModelNegativeTest {

    private final static String CLIENT = "at-direct-attr-model";

    private static MobileBannersRule bannersRule = new MobileBannersRule().withUlogin(CLIENT);

    private static final String MODEL_LAST_CLICK = "last_click";                           // at BK AttributionType = 1
    private static final String MODEL_FIRST_CLICK = "first_click";                         // at BK AttributionType = 3
    private static final String MODEL_LAST_SIGNIFICIANT_CLICK = "last_significant_click";  // at BK AttributionType = 2

    @Parameterized.Parameter(value = 0)
    public static String attributionModel;

    @Parameterized.Parameters(name = "Модель аттрибуции = {0}, ожидаем вывод ошибки WRONG_ATTRIBUTION_MODEL_FOR_MOBILE_CONTENT_CAMPAIGN")
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][]{
                {MODEL_LAST_CLICK},
                {MODEL_FIRST_CLICK},
                {MODEL_LAST_SIGNIFICIANT_CLICK}
        };
        return Arrays.asList(data);
    }

    @ClassRule
    public static DirectCmdRule cmdRule = DirectCmdRule.
            defaultClassRule().
            useAuth(true).
            as(CLIENT).
            withRules(bannersRule);

    private SaveCampRequest request;

    @Before
    public void before() {
        request = BeanLoadHelper.loadCmdBean(CmdBeans.SAVE_MOBILE_CAMP_FULL, SaveCampRequest.class);
        request.withCid(String.valueOf(bannersRule.getCampaignId())).
                withUlogin(CLIENT);
    }

    @Test
    @Description("Сохранение аттрибуционной модели для мобильной кампании")
    @TestCaseId("11006")
    public void testSaveCampLastClickAttributionModel() {
        request.withAttributionModel(attributionModel);
        sendAndCheckCampaignErrors(TextResourceFormatter.resource(CampaignErrors.WRONG_ATTRIBUTION_MODEL_FOR_MOBILE_CONTENT_CAMPAIGN).toString());
    }

    private void sendAndCheckCampaignErrors(String error) {
        CampaignErrorResponse response = cmdRule.cmdSteps().campaignSteps().postSaveCampInvalidData(request);
        assertThat("ошибка соответствует ожидаемой", response.getCampaignErrors().getError(), equalTo(error));
    }

}
