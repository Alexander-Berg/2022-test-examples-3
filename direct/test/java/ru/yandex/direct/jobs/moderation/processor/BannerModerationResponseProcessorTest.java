package ru.yandex.direct.jobs.moderation.processor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.log.service.ModerationLogService;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerModerationVersionsRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.jobs.moderation.BannerModerationResponseTestUtils;
import ru.yandex.direct.jobs.moderation.ModerationReadMonitoring;
import ru.yandex.direct.jobs.moderation.processor.handlers.ModerationResponseHandler;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Maybe;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.AUDIO_CREATIVE;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.CANVAS;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.CONTENT_PROMOTION_COLLECTION;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.CONTENT_PROMOTION_VIDEO;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.FIXCPM_YNDX_FRONTPAGE_CREATIVE;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.GEO_PIN_CREATIVE;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.HTML5;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.UNKNOWN;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.YNDX_FRONTPAGE_CREATIVE;
import static ru.yandex.direct.jobs.moderation.BannerModerationResponseTestUtils.getBannerModerationResponse;

@JobsTest
@ExtendWith(SpringExtension.class)
class BannerModerationResponseProcessorTest {

    @Autowired
    protected Steps steps;

    @Autowired
    protected ShardHelper shardHelper;

    @Autowired
    protected OldBannerRepository bannerRepository;

    @Autowired
    protected List<ModerationResponseHandler> handlers;

    @Autowired
    protected TestBannerModerationVersionsRepository testBannerModerationVersionsRepository;

    private ModerationResponseProcessor processor;

    private static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {AUDIO_CREATIVE},
                {YNDX_FRONTPAGE_CREATIVE},
                {FIXCPM_YNDX_FRONTPAGE_CREATIVE},
                {CONTENT_PROMOTION_COLLECTION},
                {CONTENT_PROMOTION_VIDEO},
                {GEO_PIN_CREATIVE},
                {HTML5},
                {CANVAS},
        });
    }

    @BeforeEach
    void before() {
        ModerationReadMonitoring readMonitoring = mock(ModerationReadMonitoring.class);
        processor = new ModerationResponseProcessor(shardHelper, ModerationResponseProcessorFilter.doNothing(),
                readMonitoring, handlers,
                mock(ModerationLogService.class));
    }

    @ParameterizedTest(name = "{0}: check yes verdict")
    @MethodSource("parameters")
    void checkProcessModerationResponse_OneBanner_YesVerdict(ModerationObjectType moderationType) {
        var bannerCreativeInfo = prepareBannerCreative(moderationType);
        processor.accept(List.of(getBannerModerationResponse(moderationType, Yes, bannerCreativeInfo)));

        checkBannerStatusModerate(bannerCreativeInfo, OldBannerStatusModerate.YES);
    }

    @ParameterizedTest(name = "{0}: check unknown verdict")
    @MethodSource("parameters")
    void checkProcessModerationResponse_OneBanner_UnknownVerdict(ModerationObjectType moderationType) {
        var bannerCreativeInfo = prepareBannerCreative(moderationType);
        processor.accept(List.of(getBannerModerationResponse(moderationType, Maybe, bannerCreativeInfo)));

        checkBannerStatusModerate(bannerCreativeInfo, OldBannerStatusModerate.SENT);
    }

    @ParameterizedTest(name = "{0}: check two yes verdicts")
    @MethodSource("parameters")
    void checkProcessModerationResponse_TwoBanners_YesVerdict(ModerationObjectType moderationType) {
        var bannerCreativeInfo = prepareBannerCreative(moderationType);
        var bannerCreativeInfo2 = prepareBannerCreative(moderationType);
        processor.accept(List.of(getBannerModerationResponse(moderationType, Yes, bannerCreativeInfo),
                getBannerModerationResponse(moderationType, Yes, bannerCreativeInfo2)));

        checkBannerStatusModerate(bannerCreativeInfo, OldBannerStatusModerate.YES);
        checkBannerStatusModerate(bannerCreativeInfo2, OldBannerStatusModerate.YES);
    }

    @ParameterizedTest(name = "{0}: check yes and unknown verdicts")
    @MethodSource("parameters")
    void checkProcessModerationResponse_OneBannerAndOneUnknownType_YesVerdict(ModerationObjectType moderationType) {
        var bannerCreativeInfo = prepareBannerCreative(moderationType);
        var bannerCreativeInfo2 = prepareBannerCreative(moderationType);
        processor.accept(List.of(getBannerModerationResponse(UNKNOWN, Yes, bannerCreativeInfo),
                getBannerModerationResponse(moderationType, Yes, bannerCreativeInfo2)));

        checkBannerStatusModerate(bannerCreativeInfo, OldBannerStatusModerate.SENT);
        checkBannerStatusModerate(bannerCreativeInfo2, OldBannerStatusModerate.YES);
    }

    @ParameterizedTest(name = "{0}: check yes and multiple unknown verdict")
    @MethodSource("parameters")
    void checkProcessModerationResponse_OneBannerAndMultipleUnknownType_YesVerdict(ModerationObjectType moderationType) {
        var bannerCreativeInfo = prepareBannerCreative(moderationType);
        processor.accept(List.of(getBannerModerationResponse(UNKNOWN, Yes, null),
                getBannerModerationResponse(UNKNOWN, Yes, null),
                getBannerModerationResponse(moderationType, Yes, bannerCreativeInfo),
                getBannerModerationResponse(UNKNOWN, Yes, null)));

        checkBannerStatusModerate(bannerCreativeInfo, OldBannerStatusModerate.YES);
    }

    private AbstractBannerInfo prepareBannerCreative(ModerationObjectType moderationType) {
        AbstractBannerInfo bannerInfo = BannerModerationResponseTestUtils.getAppropriateBanner(moderationType, steps);
        bannerRepository.updateStatusModerate(bannerInfo.getShard(),
                List.of(bannerInfo.getBannerId()),
                OldBannerStatusModerate.SENT);
        testBannerModerationVersionsRepository.addVersion(bannerInfo.getShard(),
                bannerInfo.getBannerId(), 1L);
        return bannerInfo;
    }

    private void checkBannerStatusModerate(AbstractBannerInfo bannerInfo, OldBannerStatusModerate statusModerate) {
        OldBanner actualBanner =
                bannerRepository.getBanners(bannerInfo.getShard(), List.of(bannerInfo.getBannerId())).get(0);
        assertThat(actualBanner.getStatusModerate(), equalTo(statusModerate));
    }

}
