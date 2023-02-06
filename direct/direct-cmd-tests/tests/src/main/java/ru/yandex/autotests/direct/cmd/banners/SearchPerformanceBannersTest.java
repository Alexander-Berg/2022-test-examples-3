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
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Поиск сразу трех перформанс баннеров")
@Stories(TestFeatures.Banners.SEARCH_BANNERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SEARCH_BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.PERFORMANCE)
public class SearchPerformanceBannersTest {

    private static final String CLIENT = "at-backend-perf-8";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    public PerformanceBannersRule bannersRule1 = (PerformanceBannersRule)
            new PerformanceBannersRule().withUlogin(CLIENT);

    public PerformanceBannersRule bannersRule2 = (PerformanceBannersRule)
            new PerformanceBannersRule().withUlogin(CLIENT);

    public PerformanceBannersRule bannersRule3 = (PerformanceBannersRule)
            new PerformanceBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule1, bannersRule2, bannersRule3);


    @Before
    public void before() {
    }

    @Test
    @Description("Поиск трех перформанс баннеров по номерам объявлений")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9066")
    public void searchBannerByThreeAdNumber() {
        SearchBannersResponse response = cmdRule.cmdSteps().searchBannersSteps().postSearchBanners(
                SearchWhat.NUM.getName(),
                bannersRule1.getBannerId().toString(),
                bannersRule2.getBannerId().toString(),
                bannersRule3.getBannerId().toString());

        assertThat("Нашли три перформанс баннера по номерам объявлений",
                response.getBanners(), hasSize(3));
    }

    @Test
    @Description("Поиск трех перформанс баннеров по номерам групп")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9067")
    public void searchBannerByThreeGroupNumber() {
        SearchBannersResponse response = cmdRule.cmdSteps().searchBannersSteps().postSearchBanners(
                SearchWhat.GROUP.getName(),
                bannersRule1.getGroupId().toString(),
                bannersRule2.getGroupId().toString(),
                bannersRule3.getGroupId().toString());

        assertThat("Нашли три перформанс баннера по номерам групп",
                response.getBanners(), hasSize(3));
    }
}
