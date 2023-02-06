package ru.yandex.autotests.direct.cmd.banners.flags;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.banners.AdWarningFlag;
import ru.yandex.autotests.direct.cmd.data.banners.ChangeFlagsAjaxRequest;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка сохрания флагов на баннер")
@Stories(TestFeatures.Banners.BANNER_FLAGS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SEARCH_BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class SetBannerFlagsTest {

    public static String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    public BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule).as(CLIENT);

    @Test
    @Description("Клиент не может сохранить флаг project_declaration")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9145")
    public void cantSetProjectDeclarationFlag() {
        AdWarningFlag flag = AdWarningFlag.PROJECT_DECLARATION;

        cmdRule.cmdSteps().bannerSteps().changeBannerFlag(new ChangeFlagsAjaxRequest()
                .withBid(bannersRule.getBannerId().toString())
                .addFlag(AdWarningFlag.PROJECT_DECLARATION));

        Banner banner = bannersRule.getCurrentGroup().getBanners().get(0);
        assertThat("Флаг " + flag + " не установлен на баннер", banner.getFlags(), isEmptyOrNullString());
    }


}
