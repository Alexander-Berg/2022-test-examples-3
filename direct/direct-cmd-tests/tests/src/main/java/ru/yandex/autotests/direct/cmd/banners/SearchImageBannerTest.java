package ru.yandex.autotests.direct.cmd.banners;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.ImageBannerRule;
import ru.yandex.autotests.direct.cmd.steps.images.NewImagesUploadHelper;
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
@Description("Поиск графического баннера из ТГО и РМП")
@Stories(TestFeatures.Banners.SEARCH_BANNERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SEARCH_BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@RunWith(Parameterized.class)
public class SearchImageBannerTest extends SearchBannerTestBase {

    public SearchImageBannerTest(CampaignTypeEnum campaignType) {
        super(new ImageBannerRule(campaignType)
                .withImageUploader(new NewImagesUploadHelper())
                .withUlogin(CLIENT)
        );
    }

    @Parameterized.Parameters(name = "Поиск графического баннера. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE}
        });
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9060")
    public void searchBannerByAdNumber() {
        super.searchBannerByAdNumber();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9061")
    public void searchBannerByGroupNumber() {
        super.searchBannerByGroupNumber();
    }
}
