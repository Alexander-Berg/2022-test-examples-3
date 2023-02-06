package ru.yandex.autotests.directintapi.tests.bannersearch;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bannersearch.BannerSearchRequestData;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bannersearch.BannerSearchResponseData;
import ru.yandex.autotests.directapi.darkside.datacontainers.jsonrpc.bannersearch.Criteria;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directapi.rules.Trashman;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.directintapi.tests.IntapiConstants.ACTIVE_BANNER_ID;
import static ru.yandex.autotests.directintapi.tests.IntapiConstants.ACTIVE_TEXT;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by buhter on 08.11.16.
 * https://st.yandex-team.ru/TESTIRT-10344
 */
@Aqua.Test(title = "advanced_search - поиск по тексту(text).")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Issue("https://st.yandex-team.ru/DIRECT-57459")
@Features(FeatureNames.BANNER_SEARCH)
public class AdvancedSearchByTextTest {

    @ClassRule
    public static ApiSteps api = new ApiSteps();
    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();
    @Rule
    public Trashman trashman = new Trashman(api);

    @Test
    @Title("Поиск баннера по тексту")
    @Description("Делаем запрос с определенным текстом из прода, баннер должен быть в БК. " +
            "Проверяем, что в ответе есть ожидаемый баннер")
    public void bannerSearchByBannerTextTest() {
        //берём текст из прода, т.к. она должна существовать в БК https://st.yandex-team.ru/DIRECT-55112
        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.TEXT.getKey())
                .withValues(Arrays.asList(ACTIVE_TEXT))
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
        );
        assertThat("в ответе есть ожидаемый баннер",
                response.getBanners().stream().map(BannerSearchResponseData.Banner::getBid).collect(toList()),
                hasItem(equalTo(ACTIVE_BANNER_ID))
        );
    }

    @Test
    @Title("Поиск по несуществующему тексту")
    @Description("Делаем запрос с несуществующим текстом. Проверяем, что ответ пустой")
    public void bannerSearchByBannerUnexistingTextTest() {
        List<Criteria> criteria = Arrays.asList(new Criteria()
                .withKey(Criteria.CriteriaKey.TEXT.getKey())
                .withValues(Arrays.asList(RandomUtils.getString(30)))
        );
        BannerSearchResponseData response = api.userSteps.getDarkSideSteps().getBannerSearchSteps().advancedSearch(
                new BannerSearchRequestData()
                        .withCriteria(criteria)
        );
        assertThat("ответ пустой",
                response.getBanners(),
                emptyIterable()
        );
    }
}
