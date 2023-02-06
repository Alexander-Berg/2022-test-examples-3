package ru.yandex.autotests.direct.cmd.banners;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.SearchBannersResponse;
import ru.yandex.autotests.direct.cmd.data.banners.SearchWhat;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.SmokeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.cmd.data.CmdBeans.BANNER_PARAM_FOR_PREVIEW;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

//TESTIRT-8267
@Aqua.Test
@Description("Необходимые данные для отображение b-banner-preview2 на странице поиска по баннерам")
@Stories(TestFeatures.Banners.SEARCH_BANNERS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SEARCH_BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
@Tag(SmokeTag.YES)
@Tag(TrunkTag.YES)
public class SearchBannersNeedParamsTest {

    private static final String BANNER_ID = "2112194448";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();


    @Before
    public void before() {
    }

    @Test
    @Description("На страницу поиска по баннерам необходимо прокидывать данные кампаний," +
            " баннеры которых присутствуют в выдаче")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9059")
    public void checkCampaignParams() {
        SearchBannersResponse response = cmdRule.cmdSteps().searchBannersSteps().postSearchBanners(
                SearchWhat.NUM.getName(), BANNER_ID);

        SearchBannersResponse expectedParams = BeanLoadHelper.loadCmdBean(BANNER_PARAM_FOR_PREVIEW,
                SearchBannersResponse.class);

        assertThat("Получили нужные параметры для отображения баннера на поиске",
                response, beanDiffer(expectedParams).useCompareStrategy(onlyExpectedFields()));
    }
}
