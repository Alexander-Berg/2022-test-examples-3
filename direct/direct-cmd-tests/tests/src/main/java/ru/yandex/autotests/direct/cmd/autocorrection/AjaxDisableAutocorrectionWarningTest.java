package ru.yandex.autotests.direct.cmd.autocorrection;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

@Aqua.Test
@Description("Проверка установки флага before_moderation ручкой ajaxDisableAutocorrectionWarning")
@Stories(TestFeatures.Banners.AUTO_CORRECTION)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.AJAX_DISABLE_AUTOCORRECTION_WARNING)
@Tag(ObjectTag.BANNER)
@Tag(TrunkTag.YES)
public class AjaxDisableAutocorrectionWarningTest extends BeforeModerationTestBase {

    public AjaxDisableAutocorrectionWarningTest(CampaignTypeEnum campaignType) {
        super(campaignType);
    }

    @Override
    public void before() {
        super.before();

    }

    @Test
    @Description("Проверяем флаг showModEditNotice блок before_moderation")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9024")
    public void campaignParametersTest() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
        CommonResponse response = cmdRule.cmdSteps().ajaxDisableAutocorrectionWarningSteps()
                .getAjaxDisableAutocorrectionWarning(bannerId);
        assumeThat("было отключены сообщения об исправленном баннере", response.getResult(), equalTo("ok"));
        ShowCampResponse actualResponse = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, campaignId.toString());
        Banner bannerResponse = actualResponse.getGroups().stream()
                .filter(t -> t.getBid().equals(bannerId))
                .findFirst().orElse(null);

        assumeThat("получили баннер", bannerResponse, notNullValue());
        assumeThat("блок before_moderation существует", bannerResponse.getBeforeModeration(), notNullValue());
        assertThat("данные блока before_moderation соответствуют ожидаемым",
                bannerResponse.getBeforeModeration().getShowModEditNotice(),
                equalTo("No"));
    }
}
