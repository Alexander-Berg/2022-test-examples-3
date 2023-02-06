package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.receiving.CanvasModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewCpmBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.ModReasonsType;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.common.db.PpcPropertyNames.RESTRICTED_CANVAS_TRANSPORT_NEW_MODERATION;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Maybe;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.fullCpmBanner;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveCpmCanvasModerationResponseTest extends OldAbstractBannerModerationResponseTest {

    @Autowired
    private CanvasModerationReceivingService canvasModerationReceivingService;
    @Autowired
    private TestModerationRepository testModerationRepository;
    @Autowired
    private TestBannerRepository testBannerRepository;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private Steps steps;

    protected Long defaultBannerId;
    private CampaignInfo campaignInfo;
    private int shard;

    @Before
    public void setUp() throws Exception {
        disableRestrictedMode();
        campaignInfo = steps.campaignSteps().createActiveCpmBannerCampaign();
        shard = campaignInfo.getShard();
        defaultBannerId = createObjectInDb(getDefaultVersion());
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected ModerationReceivingService<BannerModerationResponse> getReceivingService() {
        return canvasModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        var creative = defaultCanvas(campaignInfo.getClientId(), null);
        var banner = fullCpmBanner(null)
                .withLanguage(Language.RU_)
                .withStatusModerate(BannerStatusModerate.SENT);
        var bannerInfo = steps.cpmBannerSteps().createCpmBanner(
                new NewCpmBannerInfo().withCampaignInfo(campaignInfo).withBanner(banner).withCreative(creative));

        testModerationRepository.createBannerVersion(shard, bannerInfo.getBannerId(), version);

        return bannerInfo.getBannerId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return ModerationObjectType.CANVAS;
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

    protected void disableRestrictedMode() {
        ppcPropertiesSupport.set(RESTRICTED_CANVAS_TRANSPORT_NEW_MODERATION, String.valueOf(false));
    }

    protected void enableRestrictedMode() {
        ppcPropertiesSupport.set(RESTRICTED_CANVAS_TRANSPORT_NEW_MODERATION, String.valueOf(true));
    }

    @Test
    public void moderationResponse_notSavedInDb_whenFullFeaturedTransportDisabled() {
        enableRestrictedMode();
        var response = createResponseForDefaultObject(Yes, Language.EN.name());
        response.getResult().setMinusRegions(singletonList(RUSSIA_REGION_ID));
        response.getResult().setFlags(new HashMap<>(Map.of("age", "age18", "finance", "1")));
        var unknownVerdictCountAndSuccess =
                getReceivingService().processModerationResponses(getShard(), singletonList(response));
        assertThat(unknownVerdictCountAndSuccess.getLeft()).isZero();
        assertThat(unknownVerdictCountAndSuccess.getRight()).hasSize(1);
        checkInDbForRestrictedMode(getDefaultObjectId());
    }
    @Test
    public void unknownVerdict_notSavedInDb_correctReturnValue_whenFullFeaturedTransportDisabled() {
        enableRestrictedMode();
        var response = createResponseForDefaultObject(Maybe, Language.EN.name());
        response.getResult().setMinusRegions(singletonList(RUSSIA_REGION_ID));
        response.getResult().setFlags(new HashMap<>(Map.of("age", "age18", "finance", "1")));
        var unknownVerdictCountAndSuccess = getReceivingService()
                .processModerationResponses(getShard(), singletonList(response));
        assertThat(unknownVerdictCountAndSuccess.getLeft()).isEqualTo(1);
        assertThat(unknownVerdictCountAndSuccess.getRight()).isEmpty();
        checkInDbForRestrictedMode(getDefaultObjectId());
    }

    private void checkInDbForRestrictedMode(Long bid) {
        OldBanner b = getBanner(bid);
        assertThat(b).describedAs("Баннер не изменился")
                .hasFieldOrPropertyWithValue(OldBanner.STATUS_MODERATE.name(), OldBannerStatusModerate.SENT)
                .hasFieldOrPropertyWithValue(OldBanner.STATUS_POST_MODERATE.name(), OldBannerStatusPostModerate.YES)
                .hasFieldOrPropertyWithValue(OldBanner.STATUS_BS_SYNCED.name(), StatusBsSynced.YES)
                .hasFieldOrPropertyWithValue(OldBanner.LANGUAGE.name(), Language.RU_)
                .hasFieldOrPropertyWithValue(OldBanner.FLAGS.name(), null);

        List<String> modReasons = testBannerRepository.getModReasons(shard, bid, ModReasonsType.banner);
        assertThat(modReasons).describedAs("В mod_reasons ничего нет").isEmpty();

        String minusGeo = testBannerRepository.getMinusGeo(shard, bid);
        assertThat(minusGeo).describedAs("Минус регионы не прописали").isNull();
    }
}
