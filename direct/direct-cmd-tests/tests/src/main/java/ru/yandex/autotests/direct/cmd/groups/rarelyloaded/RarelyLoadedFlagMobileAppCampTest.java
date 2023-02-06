package ru.yandex.autotests.direct.cmd.groups.rarelyloaded;

import org.junit.Test;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;


@Aqua.Test
@Features(TestFeatures.Groups.RARELY_LOADED_FLAG)
@Stories(TestFeatures.GROUPS)
@Description("Проверка флага мало показов в мобильной кампании")
@Tag(CmdTag.SHOW_CAMP)
@Tag(CmdTag.SHOW_CAMP_MULTI_EDIT)
@Tag(CampTypeTag.MOBILE)
@Tag(TrunkTag.YES)
public class RarelyLoadedFlagMobileAppCampTest extends RarelyLoadedFlagTestBase {

    @Parameterized.Parameters(name = "Параметры : ожидаемый статус is_bs_rarely_loaded = {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {0, 0},
                {1, 1},
        });
    }

    @Override
    public CampaignTypeEnum getCampaignType() {
        return CampaignTypeEnum.MOBILE;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10690")
    public void rarelyLoadedShowCampTest() {
        super.rarelyLoadedShowCampTest();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10689")
    public void rarelyLoadedShowCampMultiEditTest() {
        super.rarelyLoadedShowCampMultiEditTest();
    }
}
