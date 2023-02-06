package ru.yandex.autotests.direct.cmd.banners;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Поиск перформанс баннера")
@Stories(TestFeatures.Banners.SEARCH_BANNERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SEARCH_BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.PERFORMANCE)
public class SearchPerformanceBannerTest extends SearchBannerTestBase {

    public SearchPerformanceBannerTest() {
        super(new PerformanceBannersRule().withUlogin(CLIENT));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9064")
    public void searchBannerByAdNumber() {
        super.searchBannerByAdNumber();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9065")
    public void searchBannerByGroupNumber() {
        super.searchBannerByGroupNumber();
    }
}
