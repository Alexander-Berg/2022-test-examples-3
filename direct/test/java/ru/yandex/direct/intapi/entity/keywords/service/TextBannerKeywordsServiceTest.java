package ru.yandex.direct.intapi.entity.keywords.service;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.keywords.model.CampaignIdAndBannerIdPair;
import ru.yandex.direct.intapi.entity.keywords.model.GetKeywordsByCidAndBidPairResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TextBannerKeywordsServiceTest {

    @Autowired
    private BannerKeywordsService bannerKeywordsService;

    @Autowired
    private Steps steps;

    @Test
    public void getPhrases() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner();
        AdGroupInfo adGroupInfo = bannerInfo.getAdGroupInfo();
        KeywordInfo keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo);

        CampaignIdAndBannerIdPair param = new CampaignIdAndBannerIdPair()
                .withCampaignId(adGroupInfo.getCampaignId())
                .withBannerId(bannerInfo.getBannerId());

        List<GetKeywordsByCidAndBidPairResult> results =
                bannerKeywordsService.getKeywordsByCidAndBidPairs(Collections.singletonList(param));
        assumeThat("В списке результатов 1 элемент", results, hasSize(1));

        GetKeywordsByCidAndBidPairResult expected = new GetKeywordsByCidAndBidPairResult()
                .withKeywordId(keywordInfo.getKeyword().getId())
                .withKeyword(keywordInfo.getKeyword().getPhrase())
                .withBid(param.getBannerId())
                .withCid(param.getCampaignId());

        assertThat("В ответе верные данные", results.get(0), beanDiffer(expected));
    }

    @Test
    public void getPhrasesNoPhrases() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner();
        AdGroupInfo adGroupInfo = bannerInfo.getAdGroupInfo();

        CampaignIdAndBannerIdPair param = new CampaignIdAndBannerIdPair()
                .withCampaignId(adGroupInfo.getCampaignId())
                .withBannerId(bannerInfo.getBannerId());

        List<GetKeywordsByCidAndBidPairResult> results =
                bannerKeywordsService.getKeywordsByCidAndBidPairs(Collections.singletonList(param));
        assumeThat("В списке результатов нет элементов", results, hasSize(0));
    }

    @Test(expected = IntApiException.class)
    public void throwsExceptionWhenValidationFails() {
        bannerKeywordsService.getKeywordsByCidAndBidPairs(null);
    }
}
