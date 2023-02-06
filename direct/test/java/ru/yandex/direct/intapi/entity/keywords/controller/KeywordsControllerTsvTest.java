package ru.yandex.direct.intapi.entity.keywords.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.common.converter.directtsv.DirectTsvMessageConverter;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.keywords.model.CampaignIdAndBannerIdPair;
import ru.yandex.direct.intapi.entity.keywords.model.GetKeywordsByCidAndBidPairResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.common.converter.directtsv.DirectTsvMessageConverter.DIRECT_TSV_MEDIA_TYPE;
import static ru.yandex.direct.intapi.entity.keywords.controller.KeywordsController.CID_BID_PAIR_PARAM_NAME;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public class KeywordsControllerTsvTest {
    private static final String EXPECTED_CONTROLLER_MAPPING = "/GetBannersPhrases";

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
                .setMessageConverters(new DirectTsvMessageConverter())
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
    public void testControllerReturnsValidTsvResponse() throws Exception {
        String r = mockMvc
                .perform(get(EXPECTED_CONTROLLER_MAPPING)
                        .param(CID_BID_PAIR_PARAM_NAME,
                                String.format("%s\t%s", param.getCampaignId(), param.getBannerId()))
                        .accept(DIRECT_TSV_MEDIA_TYPE))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat("В ответе верные данные", r, equalTo(String
                .format("#cid\tbid\tbids_id\tphrase\n%s\t%s\t%s\t%s\n#End\n", expected.getCid(), expected.getBid(),
                        expected.getKeywordId(), expected.getKeyword())));
    }

    @Test
    public void testTsvControllerReturnsValidResponse() {
        // Есть проблема с mockMvc и табами, поэтому вызываем напрямую
        List<GetKeywordsByCidAndBidPairResult> result = controller
                .getKeywordsByCidAndBidPairTsv(String.format("%s\t%s", param.getCampaignId(), param.getBannerId()));

        assertThat("В ответе верные данные", result, beanDiffer(Collections.singletonList(expected)));
    }

    @Test
    public void testTSVParser() {
        String s = "#Start\n123\t456\n\n321\t654\n#End\n";
        List<CampaignIdAndBannerIdPair> data = controller.parseRequestTsvData(s);

        List<CampaignIdAndBannerIdPair> expect = Arrays.asList(
                new CampaignIdAndBannerIdPair().withCampaignId(123L).withBannerId(456L),
                new CampaignIdAndBannerIdPair().withCampaignId(321L).withBannerId(654L)
        );
        assertThat("В ответе верные данные", data, beanDiffer(expect));
    }

    @Test(expected = IntApiException.class)
    public void testTSVParserErrorLine() {
        String s = "#Start\n123\n\n321\t654\n#End\n";
        controller.parseRequestTsvData(s);
    }

    @Test(expected = IntApiException.class)
    public void testTSVParserErrorValue() {
        String s = "#Start\n123\t4T56\n\n321\t654\n#End\n";
        controller.parseRequestTsvData(s);
    }
}
