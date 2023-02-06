package ru.yandex.autotests.direct.cmd.banners;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.MobileBannerPrimaryAction;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Сохранение названия действия для баннера рмп")
@Stories(TestFeatures.Banners.MOBILE_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag("DIRECT-55077")
public class SaveMobileBannerPrimaryActionTest {

    public static final String CLIENT = "at-direct-backend-b";

    private MobileBannerPrimaryAction action = MobileBannerPrimaryAction.MORE;

    private MobileBannersRule bannersRule = new MobileBannersRule()
            .overrideBannerTemplate(new Banner().withPrimaryAction(action))
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Test
    @Description("Возможно сохранить название действия")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9055")
    public void canSaveBannerPrimaryAction() {
        assertThat("primary action сохранилось", bannersRule.getCurrentGroup().getBanners().get(0).getPrimaryAction(),
                equalTo(action));
    }


}
