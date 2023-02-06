package ru.yandex.autotests.direct.cmd.banners;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.SearchBannersResponse;
import ru.yandex.autotests.direct.cmd.data.banners.SearchWhat;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("поиск баннера по невалидным данным")
@Stories(TestFeatures.Banners.SEARCH_BANNERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SEARCH_BANNERS)
@Tag(ObjectTag.BANNER)
@RunWith(Parameterized.class)
public class SearchBannerByInvalidParamTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(value = 0)
    public SearchWhat searchWhat;


    @Parameterized.Parameters(name = "Поиск баннера по {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {SearchWhat.NUM},
                {SearchWhat.GROUP},
        });
    }

    @Before
    public void before() {
    }

    @Test
    @Description("В строку поиска передаем null")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9056")
    public void searchBannerByNullText() {
        SearchBannersResponse response = cmdRule.cmdSteps().searchBannersSteps().postSearchBanners(
                searchWhat.getName(), null);

        assertThat("Баннер не найден", response.getBanners(), equalTo(null));
    }

    @Test
    @Description("Поиск баннера по пустому текстовому полю")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9057")
    public void searchBannerByEmptyText() {
        String empty_string = "";
        SearchBannersResponse response = cmdRule.cmdSteps().searchBannersSteps().postSearchBanners(
                searchWhat.getName(), empty_string);

        assertThat("Баннер не найден", response.getBanners(), equalTo(null));
    }

    @Test
    @Description("Поиск баннера по текстовому полю")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9058")
    public void searchBannerByLetterText() {
        String abc_string = "abc";
        SearchBannersResponse response = cmdRule.cmdSteps().searchBannersSteps().postSearchBanners(
                searchWhat.getName(), abc_string);

        assertThat("Баннер не найден", response.getBanners(), hasSize(0));
    }
}
