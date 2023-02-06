package ru.yandex.autotests.direct.httpclient.banners.searchbanners;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.banners.searchbanners.SearchBannersResponseBean;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.autotests.directapi.model.Logins;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by shmykov on 16.06.15.
 * TESTIRT-5018
 */
@Aqua.Test
@Description("Тесты контроллера SearchBanners")
@Stories(TestFeatures.Banners.SEARCH_BANNERS)
@Features(TestFeatures.BANNERS)
@Tag(ObjectTag.BANNER)
@Tag(CmdTag.SEARCH_BANNERS)
@Tag(TrunkTag.YES)
@Tag(OldTag.YES)
public class SearchBannersTest extends SearchBannersTestBase {

    public SearchBannersTest() {
        super(Logins.SUPER_LOGIN);
    }

    @Test
    @Description("Проверка ответа контроллера SearchBanners")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10273")
    public void searchBannersResponseTest() {
        response = cmdRule.oldSteps().searchBannersSteps().searchBanners(requestParams, csrfToken);
        SearchBannersResponseBean responseBean =
                JsonPathJSONPopulater.evaluateResponse(response, new SearchBannersResponseBean());
        assertThat("найденные баннеры в ответе соответствуют ожиданиям", responseBean.getBids(),
                containsInAnyOrder(bannersRule.getBannerId().toString()));
    }

    @Test
    @Description("Проверка ответа контроллера SearchBanners в случае отсутствия найденных баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10274")
    public void searchBannersEmptyResponseTest() {
        requestParams.setTextSearch("abc");
        response = cmdRule.oldSteps().searchBannersSteps().searchBanners(requestParams, csrfToken);
        SearchBannersResponseBean responseBean =
                JsonPathJSONPopulater.evaluateResponse(response, new SearchBannersResponseBean());
        assertThat("в ответе нет найденных баннеров", responseBean.getBids(), emptyCollectionOf(String.class));
    }

    @Test
    @Description("Проверка отсутствия найденных баннеров при неверном параметре what")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10275")
    public void wrongWhatTypeTest() {
        requestParams.setWhat("abc");
        response = cmdRule.oldSteps().searchBannersSteps().searchBanners(requestParams, csrfToken);
        SearchBannersResponseBean responseBean =
                JsonPathJSONPopulater.evaluateResponse(response, new SearchBannersResponseBean());
        assertThat("в ответе нет найденных баннеров", responseBean.getBids(), emptyCollectionOf(String.class));
    }
}
