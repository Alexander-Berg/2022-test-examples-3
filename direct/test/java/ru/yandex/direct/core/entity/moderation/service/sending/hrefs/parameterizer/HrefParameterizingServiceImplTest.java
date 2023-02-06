package ru.yandex.direct.core.entity.moderation.service.sending.hrefs.parameterizer;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.moderation.service.sending.hrefs.parameterizer.implementations.HrefParameterizationRequestImpl;
import ru.yandex.direct.core.entity.moderation.service.sending.hrefs.parameterizer.implementations.HrefParameterizingServiceImpl;
import ru.yandex.direct.core.entity.moderation.service.sending.hrefs.parameterizer.implementations.ReplacingParamsImpl;
import ru.yandex.direct.core.entity.placements.model.Placement;
import ru.yandex.direct.core.entity.placements.repository.PlacementsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestKeywords;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.repository.TestPlacementRepository;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.CPM_YNDX_FRONTPAGE;


@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class HrefParameterizingServiceImplTest {

    @Autowired
    private HrefParameterizingServiceImpl hrefParameterizingService;

    @Autowired
    private PlacementsRepository placementsRepository;

    @Autowired
    private TestPlacementRepository testPlacementRepository;

    @Autowired
    private Steps steps;

    @Test
    public void parameterizeBaseCheck() {
        HrefParameterizationRequest request = HrefParameterizationRequestImpl.builder()
                .withHref("http://some.domain/ya.ru?bid={ad_id}&cid={campaign_id}")
                .withReplacingParams(
                        ReplacingParamsImpl.builder()
                                .withBid(1234L)
                                .withCid(9999L)
                                .build()
                )
                .build();

        List<String> result = hrefParameterizingService.parameterize(1, List.of(request));

        assertThat(result).isNotEmpty();
        assertThat(result.get(0)).isEqualTo("http://some.domain/ya.ru?bid=1234&cid=9999");
    }

    @Test
    public void parameterizeBaseCheckTrailingCharacters() {
        HrefParameterizationRequest request = HrefParameterizationRequestImpl.builder()
                .withHref("http://some.domain/ya.ru?bid={ad_id}&cid={campaign_id}&x=12")
                .withReplacingParams(
                        ReplacingParamsImpl.builder()
                                .withBid(1234L)
                                .withCid(9999L)
                                .build()
                )
                .build();

        List<String> result = hrefParameterizingService.parameterize(1, List.of(request));

        assertThat(result).isNotEmpty();
        assertThat(result.get(0)).isEqualTo("http://some.domain/ya.ru?bid=1234&cid=9999&x=12");
    }

    @Test
    public void parameterizeUnknownTagTest() {
        HrefParameterizationRequest request = HrefParameterizationRequestImpl.builder()
                .withHref("http://some.domain/ya.ru?bid={ad_id}&cid={campaign_id}&x=12&unk={unknown}")
                .withReplacingParams(
                        ReplacingParamsImpl.builder()
                                .withBid(1234L)
                                .withCid(9999L)
                                .withCampaignType(CPM_YNDX_FRONTPAGE)
                                .build()
                )
                .build();

        List<String> result = hrefParameterizingService.parameterize(1, List.of(request));

        assertThat(result).isNotEmpty();
        assertThat(result.get(0)).isEqualTo("http://some.domain/ya.ru?bid=1234&cid=9999&x=12&unk=");
    }

    @Test
    public void parameterizeWithoutParameters() {
        HrefParameterizationRequest request = HrefParameterizationRequestImpl.builder()
                .withHref("http://some.domain/ya.ru?bid=&cid=&x=12")
                .withReplacingParams(
                        ReplacingParamsImpl.builder()
                                .withBid(1234L)
                                .withCid(9999L)
                                .withCampaignType(CPM_YNDX_FRONTPAGE)
                                .build()
                )
                .build();

        List<String> result = hrefParameterizingService.parameterize(1, List.of(request));

        assertThat(result).isNotEmpty();
        assertThat(result.get(0)).isEqualTo("http://some.domain/ya.ru?bid=&cid=&x=12");
    }

    @Test
    public void parameterizeEmptyString() {

        HrefParameterizationRequest request = HrefParameterizationRequestImpl.builder()
                .withHref("")
                .withReplacingParams(
                        ReplacingParamsImpl.builder()
                                .withBid(1234L)
                                .withCid(9999L)
                                .withCampaignType(CPM_YNDX_FRONTPAGE)
                                .build()
                )
                .build();

        List<String> result = hrefParameterizingService.parameterize(1, List.of(request));

        assertThat(result).isNotEmpty();
        assertThat(URLDecoder.decode(result.get(0), StandardCharsets.UTF_8)).isEqualTo("");
    }

    @Test
    public void parameterizeKeyword() {

        KeywordInfo keywordInfo = steps.keywordSteps().createDefaultKeyword();

        HrefParameterizationRequest request = HrefParameterizationRequestImpl.builder()
                .withHref("http://some.domain/ya.ru?bid={ad_id}&cid={campaign_id}&phr={keyword}&x=12")
                .withReplacingParams(
                        ReplacingParamsImpl.builder()
                                .withBid(1234L)
                                .withCid(keywordInfo.getCampaignId())
                                .withPid(keywordInfo.getAdGroupId())
                                .build()
                )
                .build();

        List<String> result = hrefParameterizingService.parameterize(keywordInfo.getAdGroupInfo().getShard(),
                List.of(request));

        assertThat(result).isNotEmpty();
        assertThat(URLDecoder.decode(result.get(0), StandardCharsets.UTF_8)).isEqualTo("http://some.domain/ya" +
                ".ru?bid=1234&cid=" + keywordInfo.getCampaignId() +
                "&phr=" + keywordInfo.getKeyword().getPhrase() + "&x=12");
    }

    @Test
    public void parameterizeKeywordWithoutDbRecord() {
        HrefParameterizationRequest request = HrefParameterizationRequestImpl.builder()
                .withHref("http://some.domain/ya.ru?bid={ad_id}&cid={campaign_id}&phr={keyword}&x=12")
                .withReplacingParams(
                        ReplacingParamsImpl.builder()
                                .withBid(1234L)
                                .withCid(48296L)
                                .withPid(29156L)
                                .withCampaignType(CPM_YNDX_FRONTPAGE)
                                .build()
                )
                .build();

        List<String> result = hrefParameterizingService.parameterize(1, List.of(request));

        assertThat(result).isNotEmpty();
        assertThat(URLDecoder.decode(result.get(0), StandardCharsets.UTF_8)).isEqualTo("http://some.domain/ya" +
                ".ru?bid=1234&cid=48296&phr=&x=12");
    }

    @Test
    public void parameterizeRetargeting() {

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        RetargetingInfo retargetingInfo = steps.retargetingSteps().createDefaultRetargeting(adGroupInfo);

        HrefParameterizationRequest request = HrefParameterizationRequestImpl.builder()
                .withHref("http://some.domain/ya.ru?bid={ad_id}&cid={campaign_id}&phr={retargeting_id}&x=12")
                .withReplacingParams(
                        ReplacingParamsImpl.builder()
                                .withBid(1234L)
                                .withCid(retargetingInfo.getCampaignId())
                                .withPid(retargetingInfo.getAdGroupId())
                                .withCampaignType(CPM_YNDX_FRONTPAGE)
                                .build()
                )
                .build();

        List<String> result = hrefParameterizingService.parameterize(retargetingInfo.getAdGroupInfo().getShard(),
                List.of(request));

        assertThat(result).isNotEmpty();
        assertThat(URLDecoder.decode(result.get(0), StandardCharsets.UTF_8)).isEqualTo("http://some.domain/ya" +
                ".ru?bid=1234&cid=" + retargetingInfo.getCampaignId() +
                "&phr=" + retargetingInfo.getRetargetingId() + "&x=12");
    }

    @Test
    public void parameterizeRetargetingWithoutDbRecord() {

        HrefParameterizationRequest request = HrefParameterizationRequestImpl.builder()
                .withHref("http://some.domain/ya.ru?bid={ad_id}&cid={campaign_id}&phr={retargeting_id}&x=12")
                .withReplacingParams(
                        ReplacingParamsImpl.builder()
                                .withBid(1234L)
                                .withCid(78513L)
                                .withPid(12976L)
                                .withCampaignType(CPM_YNDX_FRONTPAGE)
                                .build()
                )
                .build();

        List<String> result = hrefParameterizingService.parameterize(1, List.of(request));

        assertThat(result).isNotEmpty();
        assertThat(URLDecoder.decode(result.get(0), StandardCharsets.UTF_8)).isEqualTo("http://some.domain/ya" +
                ".ru?bid=1234&cid=78513&phr=&x=12");
    }

    @Test
    public void parameterizeCampType() {

        HrefParameterizationRequest request = HrefParameterizationRequestImpl.builder()
                .withHref("http://some.domain/ya.ru?bid={ad_id}&cid={campaign_id}&phr={campaigntype}&x=12")
                .withReplacingParams(
                        ReplacingParamsImpl.builder()
                                .withBid(1234L)
                                .withCid(15L)
                                .withPid(16L)
                                .withCampaignType(CPM_YNDX_FRONTPAGE)
                                .build()
                )
                .build();

        List<String> result = hrefParameterizingService.parameterize(1, List.of(request));

        assertThat(result).isNotEmpty();
        assertThat(URLDecoder.decode(result.get(0), StandardCharsets.UTF_8)).isEqualTo("http://some.domain/ya" +
                ".ru?bid=1234&cid=15&phr=type12&x=12");
    }

    @Test
    public void parameterizeComplexKeyword() {

        KeywordInfo keywordInfo = steps.keywordSteps().createDefaultKeywordWithText("огонь [трактор большой и  " +
                "красивый] " +
                "!мощный !как !слон -беларусь -лошадь");

        HrefParameterizationRequest request = HrefParameterizationRequestImpl.builder()
                .withHref("http://some.domain/ya.ru?bid={ad_id}&cid={campaign_id}&phr={keyword}&x=12")
                .withReplacingParams(
                        ReplacingParamsImpl.builder()
                                .withBid(1234L)
                                .withCid(keywordInfo.getCampaignId())
                                .withPid(keywordInfo.getAdGroupId())
                                .build()
                )
                .build();

        List<String> result = hrefParameterizingService.parameterize(keywordInfo.getAdGroupInfo().getShard(),
                List.of(request));

        String correctKeyword = "огонь трактор большой и красивый мощный как слон";

        assertThat(result).isNotEmpty();
        assertThat(URLDecoder.decode(result.get(0), StandardCharsets.UTF_8)).isEqualTo("http://some.domain/ya" +
                ".ru?bid=1234&cid=" + keywordInfo.getCampaignId() +
                "&phr=" + correctKeyword + "&x=12");
    }

    @Test
    public void parameterizeKeyword_twoKeywords() {
        KeywordInfo keywordInfo = steps.keywordSteps().createDefaultKeyword();

        HrefParameterizationRequest request = HrefParameterizationRequestImpl.builder()
                .withHref("http://some.domain/ya.ru?bid={ad_id}&cid={campaign_id}&phr={keyword}&x=12")
                .withReplacingParams(
                        ReplacingParamsImpl.builder()
                                .withBid(1234L)
                                .withCid(keywordInfo.getCampaignId())
                                .withPid(keywordInfo.getAdGroupId())
                                .build()
                )
                .build();

        steps.keywordSteps().createKeyword(keywordInfo.getAdGroupInfo());

        List<String> result = hrefParameterizingService.parameterize(keywordInfo.getAdGroupInfo().getShard(),
                List.of(request));

        assertThat(result).isNotEmpty();
    }

    @Test
    public void parameterizeKeyword_twoKeywordsWithHrefParamsAndRetargetings() {
        KeywordInfo keywordInfo = steps.keywordSteps().createKeyword(TestKeywords.fullKeyword());

        HrefParameterizationRequest request = HrefParameterizationRequestImpl.builder()
                .withHref("http://some.domain/ya.ru?bid={ad_id}&cid={campaign_id}&phr={keyword}&x=12&phr={retargeting_id}")
                .withReplacingParams(
                        ReplacingParamsImpl.builder()
                                .withBid(1234L)
                                .withCid(keywordInfo.getCampaignId())
                                .withPid(keywordInfo.getAdGroupId())
                                .build()
                )
                .build();

        steps.keywordSteps().createKeyword(keywordInfo.getAdGroupInfo(), TestKeywords.fullKeyword());
        steps.retargetingSteps().createDefaultRetargeting(keywordInfo.getAdGroupInfo());
        steps.retargetingSteps().createDefaultRetargeting(keywordInfo.getAdGroupInfo());

        List<String> result = hrefParameterizingService.parameterize(keywordInfo.getAdGroupInfo().getShard(),
                List.of(request));

        assertThat(result).isNotEmpty();
    }

    @Test
    public void testParameterizeSource() {
        HrefParameterizationRequest request = HrefParameterizationRequestImpl.builder()
                .withHref("http://some.domain/ya.ru?source={source}")
                .withReplacingParams(
                        ReplacingParamsImpl.builder()
                                .build()
                )
                .build();
        int shard = 1;
        testPlacementRepository.clearPlacements();
        placementsRepository.insertPlacements(singletonList(
                new Placement()
                        .withId(1L)
                        .withDomain("parameterized.ru")
                        .withIsYandexPage(1L)
        ));
        List<String> result = hrefParameterizingService.parameterize(shard, List.of(request));

        assertThat(result).isNotEmpty();
        assertThat(result.get(0)).isEqualTo("http://some.domain/ya.ru?source=parameterized.ru");
    }


    @Test
    public void parameterizeWithEmptyReplacingParams() {
        HrefParameterizationRequest request = HrefParameterizationRequestImpl.builder()
                .withHref("http://some.domain/ya.ru?bid={ad_id}&cid={campaign_id}&pid={adgroup_id}")
                .withReplacingParams(ReplacingParamsImpl.builder().build())
                .build();

        List<String> result = hrefParameterizingService.parameterize(1, List.of(request));

        assertThat(result).isNotEmpty();
        assertThat(result.get(0)).isEqualTo("http://some.domain/ya.ru?bid=&cid=&pid=");
    }

}
