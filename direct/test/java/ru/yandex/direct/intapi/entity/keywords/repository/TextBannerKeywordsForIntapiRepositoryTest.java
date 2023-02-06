package ru.yandex.direct.intapi.entity.keywords.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.keywords.model.CampaignIdAndBannerIdPair;
import ru.yandex.direct.intapi.entity.keywords.model.GetKeywordsByCidAndBidPairResult;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TextBannerKeywordsForIntapiRepositoryTest {
    @Autowired
    private Steps steps;

    @Autowired
    private BannerKeywordsForIntapiRepository repository;

    private GetKeywordsByCidAndBidPairResult result1;
    private GetKeywordsByCidAndBidPairResult result2;
    private Integer shard;

    private GetKeywordsByCidAndBidPairResult generateResult() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner();
        AdGroupInfo adGroupInfo = bannerInfo.getAdGroupInfo();
        KeywordInfo keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo);

        // Сейчас все создается в одном шарде и мы расчитываем на такое поведение
        if (shard != null) {
            assumeThat("группы создаются в одном шарде", shard, equalTo(adGroupInfo.getShard()));
        } else {
            shard = adGroupInfo.getShard();
        }

        return new GetKeywordsByCidAndBidPairResult()
                .withCid(adGroupInfo.getCampaignId())
                .withBid(bannerInfo.getBannerId())
                .withKeywordId(keywordInfo.getKeyword().getId())
                .withKeyword(keywordInfo.getKeyword().getPhrase());
    }

    @Before
    public void beforeClass() {
        result1 = generateResult();
        result2 = generateResult();
    }

    @Test
    public void getOneResult() {
        CampaignIdAndBannerIdPair pair = new CampaignIdAndBannerIdPair()
                .withCampaignId(result1.getCid())
                .withBannerId(result1.getBid());

        List<GetKeywordsByCidAndBidPairResult> results = repository.getPhrases(shard, Collections.singletonList(pair));

        assertThat("получили один результат", results, hasSize(1));
        assertThat("получили ожидаемый результат", results.get(0), beanDiffer(result1));
    }

    @Test
    public void getTwoResults() {
        List<CampaignIdAndBannerIdPair> pairList = new ArrayList<>();
        pairList.add(new CampaignIdAndBannerIdPair()
                .withCampaignId(result1.getCid())
                .withBannerId(result1.getBid()));
        pairList.add(new CampaignIdAndBannerIdPair()
                .withCampaignId(result2.getCid())
                .withBannerId(result2.getBid()));

        List<GetKeywordsByCidAndBidPairResult> results = repository.getPhrases(shard, pairList);

        assertThat("получили два результата", results, hasSize(2));
        assertThat("результаты содержат верные данные", results,
                containsInAnyOrder(Arrays.asList(beanDiffer(result1), beanDiffer(result2))));
    }

    @Test
    public void getEmptyResult() {
        CampaignIdAndBannerIdPair pair = new CampaignIdAndBannerIdPair()
                .withCampaignId(result1.getCid())
                .withBannerId(result2.getBid());

        List<GetKeywordsByCidAndBidPairResult> results = repository.getPhrases(shard, Collections.singletonList(pair));

        assertTrue("получили пустой результат", results.isEmpty());
    }
}
