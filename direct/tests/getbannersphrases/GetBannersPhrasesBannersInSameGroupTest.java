package ru.yandex.autotests.directintapi.tests.getbannersphrases;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
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

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.directapi.matchers.beans.EveryItem.everyItem;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by omaz on 09.07.14.
 */
@Aqua.Test(title = "GetBannersPhrases - баннеры из одной группы")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.GET_BANNERS_PHRASES)
public class GetBannersPhrasesBannersInSameGroupTest {
    static Logger log = LogManager.getLogger(GetBannersPhrasesBannersInSameGroupTest.class);
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    private static GetBannersPhrasesResponse response;
    private static Long bidsId;
    private static final String PHRASE = "кофе";

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @BeforeClass
    public static void before() {
        log.info("Создаём два баннера для теста");
        Long cid = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long adGroupId = api.userSteps.adGroupsSteps().addDefaultGroup(cid);
        Long bid1 = api.userSteps.adsSteps().addDefaultTextAd(adGroupId);
        Long bid2 = api.userSteps.adsSteps().addDefaultTextAd(adGroupId);
        bidsId = api.userSteps.keywordsSteps().addKeyword(adGroupId, PHRASE);

        response = darkSideSteps.getBannersPhrasesSteps().getBannersPhrases(
                new GetBannersPhrasesRequest()
                        .withBanner(cid, bid1)
                        .withBanner(cid, bid2)
        );
    }

    @Test
    public void getBannersPhrasesCountTest() {
        assertThat("Неправильное количество фраз в ответе",
                response.getPhrases(),
                hasSize(2));
    }

    @Test
    public void getBannersPhrasesCorrectCid() {
        assertThat("У обоих баннеров в ответе должна быть одна и та же фраза",
                response.getPhrases().toArray(),
                everyItem(
                        allOf(
                                having(on(GetBannersPhrasesResponse.Phrase.class).getBidsId(), equalTo(bidsId)),
                                having(on(GetBannersPhrasesResponse.Phrase.class).getPhrase(), equalTo(PHRASE))
                        )
                )
        );
    }

}
