package ru.yandex.autotests.direct.cmd.banners.canvas;

import java.util.List;

import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.CreativeBannerRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersPerformanceStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatuspostmoderate;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Статус модерации canvas баннера в РМП кампаниях")
@Stories(TestFeatures.Banners.CANVAS_BANNERS_PARAMETERS)
@Features(TestFeatures.BANNERS)
@Tag(CampTypeTag.MOBILE)
@Tag(ObjectTag.BANNER)
public class StatusModerationForCanvasMobileBannerTest extends StatusModerationForCanvasBannersBaseTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private static CreativeBannerRule bannerRule = new CreativeBannerRule(CampaignTypeEnum.MOBILE).withUlogin(CLIENT);

    @ClassRule
    public static DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannerRule);

    public StatusModerationForCanvasMobileBannerTest(BannersPerformanceStatusmoderate creativeModerateStatus,
                                                     BannersStatusmoderate bannerModerateStatus,
                                                     BannersStatuspostmoderate bannerPostModerateStatus,
                                                     List expectedStatuses) {
        super(creativeModerateStatus, bannerModerateStatus, bannerPostModerateStatus, expectedStatuses);
    }

    @Override
    protected CreativeBannerRule getBannerRule() {
        return bannerRule;
    }

    @Override
    protected DirectCmdRule getCmdRule() {
        return cmdRule;
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9143")
    public void checkStatus() {
        super.checkStatus();
    }
}
