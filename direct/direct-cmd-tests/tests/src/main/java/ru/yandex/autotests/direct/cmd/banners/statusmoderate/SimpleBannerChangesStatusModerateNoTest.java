package ru.yandex.autotests.direct.cmd.banners.statusmoderate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Статус модерации после незначительного изменения баннера со statusModerate=NO")
@Stories(TestFeatures.Banners.BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag("TESTIRT-9435")
@RunWith(Parameterized.class)
public class SimpleBannerChangesStatusModerateNoTest extends SimpleBannerChangesStatusModerateTestBase {

    public SimpleBannerChangesStatusModerateNoTest(CampaignTypeEnum campaignType) {
        super(campaignType);
    }

    @Override
    protected String getStatusModerate() {
        return StatusModerate.NO.toString();
    }

    @Override
    protected String getExpectedStatusModerate() {
        return StatusModerate.READY.toString();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10742")
    public void addSpaceAfterCommaBannerTitle() {
        super.addSpaceAfterCommaBannerTitle();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10743")
    public void addSpaceAfterCommaBannerBody() {
        super.addSpaceAfterCommaBannerBody();
    }
}
