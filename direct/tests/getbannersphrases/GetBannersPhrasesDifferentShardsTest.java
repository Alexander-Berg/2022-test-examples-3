package ru.yandex.autotests.directintapi.tests.getbannersphrases;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.utils.model.ShardNumbers;
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
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.direct.utils.matchers.BeanEquals.beanEquals;
import static ru.yandex.autotests.directapi.matchers.beans.EveryItem.everyItem;
import static ru.yandex.autotests.directintapi.utils.ListUtils.selectFirstNotNull;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by omaz on 09.07.14.
 */
@Aqua.Test(title = "GetBannersPhrases - баннеры в разных шардах")
@Tag(TagDictionary.RELEASE)
@Tag(TagDictionary.TRUNK)
@Features(FeatureNames.GET_BANNERS_PHRASES)
public class GetBannersPhrasesDifferentShardsTest {
    private static Logger log = LogManager.getLogger(GetBannersPhrasesDifferentShardsTest.class);
    private static DarkSideSteps darkSideSteps = new DarkSideSteps();
    private static GetBannersPhrasesResponse response;
    private static Long cid1;
    private static Long cid2;
    private static Long bid1;
    private static Long bid2;
    private static Long bidsId1;
    private static Long bidsId2;
    private static final String PHRASE_1 = "подарки";
    private static final String PHRASE_2 = "окна";

    @ClassRule
    public static ApiSteps api = new ApiSteps().as(Logins.LOGIN_MAIN);

    @ClassRule
    public static SemaphoreRule semaphore = Semaphore.getSemaphore();

    @BeforeClass
    public static void before() {
        log.info("Создаём два баннера в разных шардах для теста");
        cid1 = api.userSteps.campaignSteps().addDefaultTextCampaign();
        Long pid1 = api.userSteps.adGroupsSteps().addDefaultGroup(cid1);
        bid1 = api.userSteps.adsSteps().addDefaultTextAd(pid1);
        bidsId1 = api.userSteps.keywordsSteps().addKeyword(pid1, PHRASE_1);

        api.as(Logins.LOGIN_SUPER).userSteps.clientFakeSteps()
                .reshardUser(Logins.LOGIN_SHARDING, ShardNumbers.EXTRA_SHARD);
        cid2 = api.as(Logins.LOGIN_SHARDING).userSteps.campaignSteps().addDefaultTextCampaign();
        Long pid2 = api.userSteps.adGroupsSteps().addDefaultGroup(cid2);
        bid2 = api.userSteps.adsSteps().addDefaultTextAd(pid2);
        bidsId2 = api.userSteps.keywordsSteps().addKeyword(pid2, PHRASE_2);


        response = darkSideSteps.getBannersPhrasesSteps().getBannersPhrases(
                new GetBannersPhrasesRequest()
                        .withBanner(cid1, bid1)
                        .withBanner(cid2, bid2)
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
        assertThat("У всех фраз в ответе должен быть cid из запроса",
                response.getPhrases().toArray(),
                everyItem(having(on(GetBannersPhrasesResponse.Phrase.class).getCid(), isIn(new Long[]{cid1, cid2}))));
    }

    @Test
    public void getBannersPhrasesCorrectBids() {
        assertThat("У всех фраз в ответе должен быть bid из запроса",
                response.getPhrases().toArray(),
                everyItem(having(on(GetBannersPhrasesResponse.Phrase.class).getBid(), isIn(new Long[]{bid1, bid2}))));
    }

    @Test
    public void getBannersPhrasesCorrectPhrases() {
        GetBannersPhrasesResponse.Phrase responsePhrase1 =
                selectFirstNotNull(response.getPhrases(),
                        having(on(GetBannersPhrasesResponse.Phrase.class).getBid(), equalTo(bid1)));
        GetBannersPhrasesResponse.Phrase responsePhrase2 =
                selectFirstNotNull(response.getPhrases(),
                        having(on(GetBannersPhrasesResponse.Phrase.class).getBid(), equalTo(bid2)));
        GetBannersPhrasesResponse.Phrase expectedPhrase1 = new GetBannersPhrasesResponse.Phrase();
        expectedPhrase1.setBidsId(bidsId1);
        expectedPhrase1.setPhrase(PHRASE_1);
        GetBannersPhrasesResponse.Phrase expectedPhrase2 = new GetBannersPhrasesResponse.Phrase();
        expectedPhrase2.setBidsId(bidsId2);
        expectedPhrase2.setPhrase(PHRASE_2);
        assertThat("Фраза из первого баннера содержит неправильные данные",
                responsePhrase1,
                beanEquals(expectedPhrase1));
        assertThat("Фраза из второго баннера содержит неправильные данные",
                responsePhrase2,
                beanEquals(expectedPhrase2));
    }
}
