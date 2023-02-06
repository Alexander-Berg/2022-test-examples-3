package ru.yandex.direct.intapi.entity.keywords.controller;

import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.keywords.model.CampaignIdAndBannerIdPair;
import ru.yandex.direct.intapi.entity.keywords.model.GetKeywordsByCidAndBidPairResult;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class KeywordsControllerTest {
    private static final String EXPECTED_CONTROLLER_MAPPING = "/keywords/by-cid-pid-pairs";

    @Autowired
    private KeywordsController controller;

    @Autowired
    private Steps steps;

    private MockMvc mockMvc;
    private CampaignIdAndBannerIdPair param;
    private GetKeywordsByCidAndBidPairResult expected;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner();
        AdGroupInfo adGroupInfo = bannerInfo.getAdGroupInfo();
        KeywordInfo keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo);

        param = new CampaignIdAndBannerIdPair()
                .withCampaignId(adGroupInfo.getCampaignId())
                .withBannerId(bannerInfo.getBannerId());

        expected = new GetKeywordsByCidAndBidPairResult()
                .withKeywordId(keywordInfo.getKeyword().getId())
                .withKeyword(keywordInfo.getKeyword().getPhrase())
                .withBid(param.getBannerId())
                .withCid(param.getCampaignId());
    }

    @Test
    public void testControllerReturnsValidResponse() throws Exception {
        String r = mockMvc
                .perform(post(EXPECTED_CONTROLLER_MAPPING)
                        .content(JsonUtils.toJson(singletonList(param)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();
        GetKeywordsByCidAndBidPairResult[] got = JsonUtils.fromJson(r, GetKeywordsByCidAndBidPairResult[].class);

        assertThat("В ответе верные данные", got[0], beanDiffer(expected));
    }
}
