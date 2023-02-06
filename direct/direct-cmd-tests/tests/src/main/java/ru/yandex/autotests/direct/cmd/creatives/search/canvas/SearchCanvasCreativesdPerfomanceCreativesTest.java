package ru.yandex.autotests.direct.cmd.creatives.search.canvas;

import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.creatives.SearchCreativesResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.PerformanceBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

// таск:
@Aqua.Test
@Description("Поиск perfomance креативов по id через searchCanvasCreative")
@Stories(TestFeatures.Creatives.SEARCH_CREATIVES)
@Features(TestFeatures.CREATIVES)
@Tag(CmdTag.SEARCH_CANVAS_CREATIVES)
@Tag(ObjectTag.CREATIVE)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class SearchCanvasCreativesdPerfomanceCreativesTest {

    private static final String CLIENT = "at-direct-search-creatives1";
    private static final int CREATIVES_NUMBER = 3;

    private static PerformanceBannersRule bannersRule = (PerformanceBannersRule)
            new PerformanceBannersRule().withUlogin(CLIENT);

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule().useAuth(true).withRules(bannersRule);

    @Test
    @Description("ищем perfomance креатив через searchCanvasCreative")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9672")
    public void searchPerfomanceCreativeByCanvasSearch() {
        SearchCreativesResponse actualResponse = defaultClassRule.cmdSteps().creativesSteps()
                .rawSearchCanvasCreatives(bannersRule.getCreativeId());
        assertThat("полученные креативы соотвествуют ожиданиям", actualResponse.getResult(), nullValue());
    }
}
