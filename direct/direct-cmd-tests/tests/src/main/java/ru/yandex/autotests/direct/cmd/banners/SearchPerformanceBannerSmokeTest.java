package ru.yandex.autotests.direct.cmd.banners;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.SearchBannersResponse;
import ru.yandex.autotests.direct.cmd.data.banners.SearchWhat;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Поиск перформанс баннера")
@Stories(TestFeatures.Banners.SEARCH_BANNERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SEARCH_BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
public class SearchPerformanceBannerSmokeTest {

    private static final String CLIENT = "at-backend-perf-8";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    public PerformanceBannersRule bannersRule = (PerformanceBannersRule)
            new PerformanceBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);


    @Before
    public void before() {
    }

    @Test
    @Description("Поиск перформанс баннера по номеру объявления в группе")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9062")
    public void searchBannerByAdNumber() {
        SearchBannersResponse response = cmdRule.cmdSteps().searchBannersSteps().postSearchBanners(
                SearchWhat.NUM.getName(), bannersRule.getBannerId().toString());

        assertThat("Нашли перформанс баннер по номеру объявления в группе",
                response.getBanners(), hasSize(greaterThan(0)));
    }

    @Test
    @Description("Поиск перформанс баннера по номеру группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9063")
    public void searchBannerByGroupNumber() {
        SearchBannersResponse response = cmdRule.cmdSteps().searchBannersSteps().postSearchBanners(
                SearchWhat.GROUP.getName(), bannersRule.getGroupId().toString());

        assertThat("Нашли перформанс баннер по номеру группы", response.getBanners(), hasSize(greaterThan(0)));
    }
}
