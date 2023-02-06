package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.receiving.Html5ModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewImageBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.ModReasonsType;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.common.db.PpcPropertyNames.RESTRICTED_HTML5_TRANSPORT_NEW_MODERATION;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Maybe;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5;
import static ru.yandex.direct.core.testing.data.TestNewImageBanners.fullImageBannerWithCreative;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;

@CoreTest
@RunWith(SpringRunner.class)
public class ReceiveCpcHtml5ModerationResponseTest extends OldAbstractBannerModerationResponseTest {

    @Autowired
    private Html5ModerationReceivingService html5ModerationReceivingService;
    @Autowired
    private TestModerationRepository testModerationRepository;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private Steps steps;

    private CampaignInfo campaignInfo;
    private int shard;
    private Long defaultBannerId;

    @Before
    public void setUp() {
        disableRestrictedTransport();
        campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        shard = campaignInfo.getShard();
        defaultBannerId = createObjectInDb(getDefaultVersion());
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected ModerationReceivingService<BannerModerationResponse> getReceivingService() {
        return html5ModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        var creative = defaultHtml5(campaignInfo.getClientId(), null);
        var banner = fullImageBannerWithCreative(null).withLanguage(Language.RU_)
                .withStatusModerate(BannerStatusModerate.SENT);
        var bannerInfo = steps.imageBannerSteps().createImageBanner(
                new NewImageBannerInfo().withCampaignInfo(campaignInfo).withBanner(banner).withCreative(creative));

        testModerationRepository.createBannerVersion(shard, bannerInfo.getBannerId(), version);

        return bannerInfo.getBannerId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return ModerationObjectType.HTML5;
    }

    @Override
    protected long getDefaultVersion() {
        return 5000L;
    }

    @Override
    protected long getDefaultObjectId() {
        return defaultBannerId;
    }

    @Override
    protected ClientInfo getDefaultObjectClientInfo() {
        return campaignInfo.getClientInfo();
    }

    protected void disableRestrictedTransport() {
        ppcPropertiesSupport.set(RESTRICTED_HTML5_TRANSPORT_NEW_MODERATION, String.valueOf(false));
    }

    protected void enableRestrictedTransport() {
        ppcPropertiesSupport.set(RESTRICTED_HTML5_TRANSPORT_NEW_MODERATION, String.valueOf(true));
    }

    @Test
    public void moderationResponse_notSavedInDb_whenFullFeaturedTransportDisabled() {
        enableRestrictedTransport();
        var response = createResponseForDefaultObject(Yes, Language.EN.name());
        response.getResult().setMinusRegions(singletonList(RUSSIA_REGION_ID));
        response.getResult().setFlags(new HashMap<>(Map.of("age", "age18", "finance", "1")));

        var unknownVerdictCountAndSuccess =
                getReceivingService().processModerationResponses(getShard(), singletonList(response));

        assertThat(unknownVerdictCountAndSuccess.getLeft()).isZero();
        assertThat(unknownVerdictCountAndSuccess.getRight()).hasSize(1);
        checkInDbForRestrictedMode(defaultBannerId);
    }

    @Test
    public void unknownVerdict_notSavedInDb_correctReturnValue_whenFullFeaturedTransportDisabled() {
        enableRestrictedTransport();
        var response = createResponseForDefaultObject(Maybe, Language.EN.name());
        response.getResult().setMinusRegions(singletonList(RUSSIA_REGION_ID));
        response.getResult().setFlags(new HashMap<>(Map.of("age", "age18", "finance", "1")));

        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));

        assertThat(unknownVerdictCountAndSuccess.getLeft()).isEqualTo(1);
        assertThat(unknownVerdictCountAndSuccess.getRight()).isEmpty();
        checkInDbForRestrictedMode(defaultBannerId);
    }

    private void checkInDbForRestrictedMode(Long bid) {
        OldBanner b = getBanner(bid);
        List<String> modReasons = testBannerRepository.getModReasons(shard, bid, ModReasonsType.banner);
        String minusGeo = testBannerRepository.getMinusGeo(shard, bid);

        assertThat(b).describedAs("Баннер не изменился")
                .hasFieldOrPropertyWithValue(OldBanner.STATUS_MODERATE.name(), OldBannerStatusModerate.SENT)
                .hasFieldOrPropertyWithValue(OldBanner.STATUS_POST_MODERATE.name(), OldBannerStatusPostModerate.YES)
                .hasFieldOrPropertyWithValue(OldBanner.STATUS_BS_SYNCED.name(), StatusBsSynced.YES)
                .hasFieldOrPropertyWithValue(OldBanner.LANGUAGE.name(), Language.RU_)
                .hasFieldOrPropertyWithValue(OldBanner.FLAGS.name(), null);
        assertThat(modReasons).describedAs("В mod_reasons ничего нет").isEmpty();
        assertThat(minusGeo).describedAs("Минус регионы не прописали").isNull();
    }

}
