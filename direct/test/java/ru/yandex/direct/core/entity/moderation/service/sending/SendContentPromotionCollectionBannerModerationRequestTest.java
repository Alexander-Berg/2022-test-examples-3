package ru.yandex.direct.core.entity.moderation.service.sending;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.entity.contentpromotion.repository.ContentPromotionRepository;
import ru.yandex.direct.core.entity.moderation.model.BannerLink;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow;
import ru.yandex.direct.core.entity.moderation.model.contentpromotion.ContentPromotionBannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.contentpromotion.ContentPromotionCollectionBannerRequestData;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ContentPromotionBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.moderation.model.BaseModerationData.ASAP_PROPERTY_NAME;
import static ru.yandex.direct.core.testing.data.TestBanners.YET_ANOTHER_DEFAULT_BS_BANNER_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerCollectionType;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SendContentPromotionCollectionBannerModerationRequestTest {

    private static final String COLLECTION_SERP_DATA_FILENAME = "collection_serp_data_example.json";

    @Autowired
    private Steps steps;

    @Autowired
    private ContentPromotionRepository contentPromotionRepository;

    @Autowired
    private ContentPromotionBannerSender contentPromotionBannerSender;

    private int shard;
    private ContentPromotionBannerInfo contentPromotionBannerInfo;
    private Long contentPromotionId;
    private UserInfo userInfo;
    private ClientId clientId;

    @Before
    public void before() throws IOException {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        userInfo = clientInfo.getChiefUserInfo();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultContentPromotionAdGroup(clientInfo,
                ContentPromotionAdgroupType.COLLECTION);

        String collectionSerpData = IOUtils.toString(getClass().getResourceAsStream(COLLECTION_SERP_DATA_FILENAME),
                UTF_8);
        contentPromotionId = contentPromotionRepository.insertContentPromotion(clientId,
                new ContentPromotionContent()
                        .withExternalId("external_id")
                        .withIsInaccessible(false)
                        .withType(ContentPromotionContentType.COLLECTION)
                        .withPreviewUrl("https://avatars.mdst.yandex.net/get-pdb-teasers/69072/dba12043-8607-4e4f" +
                                "-b274-25745dbfeaa0/thumb")
                        .withUrl("https://l7test.yandex.ru/collections/user/yakudzablr/tupye-kartinochki/")
                        .withMetadata(collectionSerpData));

        contentPromotionBannerInfo = steps.bannerSteps().createActiveContentPromotionBanner(
                activeContentPromotionBannerCollectionType(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withStatusModerate(OldBannerStatusModerate.READY)
                        .withStatusPostModerate(OldBannerStatusPostModerate.NO)
                        .withContentPromotionId(contentPromotionId), adGroupInfo);

        shard = contentPromotionBannerInfo.getShard();
    }

    @Test
    public void makeContentPromotionModerationRequests_CollectionType_RequestInfoIsCorrect() {
        List<ContentPromotionBannerModerationRequest> requests =
                makeContentPromotionModerationRequests(shard, singletonList(contentPromotionBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        ContentPromotionBannerModerationRequest actual = requests.get(0);
        ContentPromotionBannerModerationRequest expected = new ContentPromotionBannerModerationRequest();
        expected.setWorkflow(ModerationWorkflow.COMMON);
        expected.setService(ModerationServiceNames.DIRECT_SERVICE);
        expected.setType(ModerationObjectType.getEnumByValue("content_promotion_collection"));
        expected.setUnixtime(actual.getUnixtime());

        assertThat(actual, beanDiffer(expected).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void makeContentPromotionModerationRequests_CollectionType_DataIsCorrect() {
        List<ContentPromotionBannerModerationRequest> requests =
                makeContentPromotionModerationRequests(shard, singletonList(contentPromotionBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        ContentPromotionCollectionBannerRequestData actual =
                (ContentPromotionCollectionBannerRequestData) requests.get(0).getData();

        ContentPromotionCollectionBannerRequestData expected = new ContentPromotionCollectionBannerRequestData();
        expected.setCreativeId(contentPromotionId);
        expected.setLogin(userInfo.getUser().getLogin());
        expected.setLinks(singletonList(new BannerLink().setHref("https://www.yandex.ru/")));
        expected.setContentUrl("https://l7test.yandex.ru/collections/user/yakudzablr/tupye-kartinochki/");
        expected.setCreativePreviewUrl("https://avatars.mdst.yandex.net/get-pdb-teasers/69072/dba12043-8607-4e4f-b274" +
                "-25745dbfeaa0/thumb");
        expected.setUserFlags(emptyList());

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionModerationRequests_CollectionType_MetaIsCorrect() {
        List<ContentPromotionBannerModerationRequest> requests =
                makeContentPromotionModerationRequests(shard, singletonList(contentPromotionBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        BannerModerationMeta actual = requests.get(0).getMeta();

        BannerModerationMeta expected = new BannerModerationMeta();
        expected.setCampaignId(contentPromotionBannerInfo.getCampaignId());
        expected.setAdGroupId(contentPromotionBannerInfo.getAdGroupId());
        expected.setBannerId(contentPromotionBannerInfo.getBannerId());
        expected.setClientId(contentPromotionBannerInfo.getClientId().asLong());
        expected.setUid(userInfo.getUid());
        expected.setBsBannerId(YET_ANOTHER_DEFAULT_BS_BANNER_ID);
        expected.setVersionId(1);

        assertThat("Вернулась правильная мета", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionModerationRequests_CollectionType_ClientWithAsapFlag_DataIsCorrect() {
        steps.clientOptionsSteps().addEmptyClientOptions(shard, clientId);
        steps.clientOptionsSteps().setClientFlags(shard, clientId, "as_soon_as_possible");

        List<ContentPromotionBannerModerationRequest> requests =
                makeContentPromotionModerationRequests(shard, singletonList(contentPromotionBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        ContentPromotionCollectionBannerRequestData actual =
                (ContentPromotionCollectionBannerRequestData) requests.get(0).getData();

        ContentPromotionCollectionBannerRequestData expected = new ContentPromotionCollectionBannerRequestData();
        expected.setCreativeId(contentPromotionId);
        expected.setLogin(userInfo.getUser().getLogin());
        expected.setLinks(singletonList(new BannerLink().setHref("https://www.yandex.ru/")));
        expected.setContentUrl("https://l7test.yandex.ru/collections/user/yakudzablr/tupye-kartinochki/");
        expected.setCreativePreviewUrl("https://avatars.mdst.yandex.net/get-pdb-teasers/69072/dba12043-8607-4e4f-b274" +
                "-25745dbfeaa0/thumb");
        expected.setAsSoonAsPossible(true);
        expected.setUserFlags(emptyList());

        assertThat("Вернулись правильные данные", actual, beanDiffer(expected));
    }

    @Test
    public void makeContentPromotionModerationRequests_CollectionType_ClientWithNoAsapFlag_NoAsapInRequest() {
        List<ContentPromotionBannerModerationRequest> requests =
                makeContentPromotionModerationRequests(shard, singletonList(contentPromotionBannerInfo.getBannerId()));

        assumeThat(requests, hasSize(1));

        ContentPromotionCollectionBannerRequestData actual =
                (ContentPromotionCollectionBannerRequestData) requests.get(0).getData();

        assertThat(toJson(actual), not(containsString(ASAP_PROPERTY_NAME)));
    }

    private List<ContentPromotionBannerModerationRequest> makeContentPromotionModerationRequests(int shard,
                                                                                                 List<Long> bids) {
        Consumer<List<ContentPromotionBannerModerationRequest>> sender = Mockito.mock(Consumer.class);
        ArgumentCaptor<List<ContentPromotionBannerModerationRequest>> requestsCaptor =
                ArgumentCaptor.forClass(List.class);

        contentPromotionBannerSender.send(shard, bids, o -> System.currentTimeMillis(), o -> null, sender);

        Mockito.verify(sender, Mockito.only()).accept(requestsCaptor.capture());
        return requestsCaptor.getValue();
    }
}
