package ru.yandex.autotests.directintapi.tests.getbannersphrases;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.autotests.directapi.darkside.Logins;
import ru.yandex.autotests.directapi.darkside.connection.Semaphore;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.request.GetBannersPhrasesRequest;
import ru.yandex.autotests.directapi.darkside.datacontainers.http.response.tsv.GetBannersPhrasesResponse;
import ru.yandex.autotests.directapi.darkside.steps.DarkSideSteps;
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
@Aqua.Test(title = "GetBannersPhrases - архивная кампания")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.GET_BANNERS_PHRASES)
public class GetBannersPhrasesArchivedTest {
    Logger log = LogManager.getLogger(GetBannersPhrasesArchivedTest.class);
    private DarkSideSteps darkSideSteps = new DarkSideSteps();
    private Long cid;
    private Long bid;

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @Before
    public void before() {
        log.info("Создаём архивную кампанию для теста");
        cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long pid = api.userSteps.adGroupsSteps().addDefaultGroup(cid);
        bid = api.userSteps.adsSteps().addDefaultTextAd(pid);
        api.userSteps.keywordsSteps().addDefaultKeyword(pid);
        api.userSteps.campaignFakeSteps().makeCampaignReadyForArchive(cid);
        api.userSteps.campaignSteps().campaignsArchive(cid);
    }

    @Test
    public void getBannersPhrasesCountTest() {
        GetBannersPhrasesResponse response = darkSideSteps.getBannersPhrasesSteps().getBannersPhrases(
                new GetBannersPhrasesRequest()
                        .withBanner(cid, bid)
        );

        assertThat("Фразы из архивной кампании попали в ответ",
                response.getPhrases(),
                hasSize(0));
    }

}
