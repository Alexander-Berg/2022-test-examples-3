package ru.yandex.direct.core.entity.banner.type.turbolanding;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTurboLanding.TURBO_LANDING_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTurboLandingUpdateClearModerationTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithTurboLanding> {

    @Autowired
    private TestModerationRepository moderationRepository;
    private int shard;
    private ClientId clientId;

    @Before
    public void setUp() {
        ClientInfo defaultClient = steps.clientSteps().createDefaultClient();
        clientId = defaultClient.getClientId();
        shard = defaultClient.getShard();

        var turboLanding = steps.turboLandingSteps().createDefaultBannerTurboLanding(clientId);
        bannerInfo = steps.bannerSteps().createBanner(
                activeTextBanner(null, null)
                        .withTurboLandingId(turboLanding.getId())
                        .withTurboLandingStatusModerate(OldBannerTurboLandingStatusModerate.YES), defaultClient);
        moderationRepository.addModObjectVersionTurboLanding(shard, bannerInfo.getBannerId());
        moderationRepository.addModReasonTurboLanding(shard, bannerInfo.getBannerId());
    }

    @Test
    public void updateTextBanner_NoChange_ModerationDataNotDeleted() {
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class);

        Long id = prepareAndApplyValid(modelChanges);

        var modObjectVersionId = moderationRepository.getModObjectVersionTurboLandingByBannerId(shard, id);
        var modReasonId = moderationRepository.getModReasonTurboLandingByBannerId(shard, id);
        assertSoftly(softly -> {
            softly.assertThat(modObjectVersionId).isNotNull();
            softly.assertThat(modReasonId).isNotNull();
        });
    }

    @Test
    public void updateTextBanner_ChangeTurboLanding_StatusBsSyncedNo() {
        var newTurboLanding = steps.turboLandingSteps()
                .createDefaultBannerTurboLanding(clientId);
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(newTurboLanding.getId(), TURBO_LANDING_ID);

        Long id = prepareAndApplyValid(modelChanges);

        var modObjectVersionId = moderationRepository.getModObjectVersionTurboLandingByBannerId(shard, id);
        var modReasonId = moderationRepository.getModReasonTurboLandingByBannerId(shard, id);
        assertSoftly(softly -> {
            softly.assertThat(modObjectVersionId).isNotNull();
            softly.assertThat(modReasonId).isNull();
        });
    }

    @Test
    public void updateTextBanner_DeleteTurboLanding_StatusBsSyncedNo() {
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(null, TURBO_LANDING_ID);

        Long id = prepareAndApplyValid(modelChanges);

        var modObjectVersionId = moderationRepository.getModObjectVersionTurboLandingByBannerId(shard, id);
        var modReasonId = moderationRepository.getModReasonTurboLandingByBannerId(shard, id);
        assertSoftly(softly -> {
            softly.assertThat(modObjectVersionId).isNull();
            softly.assertThat(modReasonId).isNull();
        });
    }
}
