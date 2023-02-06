package ru.yandex.autotests.directintapi.tests.getbannersphrases;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.GetBannersPhrasesRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.tsv.GetBannersPhrasesResponse;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
import ru.yandex.autotests.directapi.darkside.steps.GetBannersPhrasesSteps;
import ru.yandex.autotests.directapi.rules.ApiSteps;
import ru.yandex.autotests.directintapi.utils.FeatureNames;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by omaz on 09.07.14.
 */
@Aqua.Test(title = "GetBannersPhrases - негативные тесты")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.GET_BANNERS_PHRASES)
public class GetBannersPhrasesNegativeTest {
    Logger log = LogManager.getLogger(GetBannersPhrasesNegativeTest.class);
    private DarkSideSteps darkSideSteps = new DarkSideSteps();
    private GetBannersPhrasesResponse response;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Test
    public void getBannersPhrasesEmptyRequestTest() {
        darkSideSteps.getBannersPhrasesSteps().executeMethodExpectError(
                GetBannersPhrasesSteps.GET_BANNERS_PHRASES,
                new GetBannersPhrasesRequest(),
                400,
                "\"code\":\"BAD_PARAM\""
        );
    }

    @Test
    public void getBannersPhrasesDeletedBannerTest() {
        log.info("Создаём кампанию и баннер в другой кампании");
        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long cid2 = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(cid2);
        Long bid = api.userSteps.adsSteps().addDefaultTextAd(pid);
        response = darkSideSteps.getBannersPhrasesSteps().getBannersPhrases(
                new GetBannersPhrasesRequest()
                        .withBanner(cid, bid)
        );
        assertThat("Ответ ожидался пустой",
                response.getPhrases(),
                hasSize(0));
    }

}
