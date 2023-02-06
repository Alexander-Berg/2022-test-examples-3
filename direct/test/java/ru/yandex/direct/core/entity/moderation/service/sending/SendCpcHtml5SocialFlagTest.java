package ru.yandex.direct.core.entity.moderation.service.sending;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithModerationInfo;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.moderation.ModerationOperationModeProvider;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.html5.Html5BannerModerationRequest;
import ru.yandex.direct.core.entity.moderation.model.html5.Html5BannerRequestData;
import ru.yandex.direct.core.entity.moderation.repository.sending.BusinessUnitModerationRepository;
import ru.yandex.direct.core.entity.moderation.repository.sending.ModerationSendingRepository;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationFlagsRepository;
import ru.yandex.direct.core.entity.moderation.service.sending.banner.ModerationFlagsConverter;
import ru.yandex.direct.core.entity.moderation.service.sending.hrefs.parameterizer.HrefParameterizingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.feature.FeatureName.SOCIAL_ADVERTISING;

@CoreTest
@RunWith(SpringRunner.class)
public class SendCpcHtml5SocialFlagTest {
    @Autowired
    DslContextProvider dslContextProvider;

    @Mock
    ModerationSendingRepository<Long, BannerWithModerationInfo> moderationSendingRepository;

    @Mock
    RemoderationFlagsRepository remoderationFlagsRepository;

    @Mock
    CampaignRepository campaignRepository;

    @Mock
    HrefParameterizingService hrefParameterizingService;

    @Mock
    FeatureService featureService;

    @Autowired
    ModerationFlagsConverter moderationFlagsConverter;

    @Autowired
    ModerationOperationModeProvider moderationOperationModeProvider;

    BannerModerationSender bannerModerationSender;

    @Autowired
    private BusinessUnitModerationRepository businessUnitModerationRepository;

    @Before
    public void before() {
        initMocks(this);
        bannerModerationSender = new BannerModerationSender(dslContextProvider, moderationSendingRepository,
                remoderationFlagsRepository, campaignRepository, hrefParameterizingService, featureService,
                businessUnitModerationRepository, moderationFlagsConverter, moderationOperationModeProvider);
    }

    @Test
    public void testSocialFlagIsTrue() {
        BannerWithModerationInfo moderationInfo = new BannerWithModerationInfo()
                .withBsBannerId(12L)
                .withClientId(145L)
                .withClientFlags(Collections.emptySet());

        Mockito.when(featureService.isEnabledForClientId(Mockito.any(ClientId.class), Mockito.eq(SOCIAL_ADVERTISING)))
                .thenReturn(true);

        bannerModerationSender.setNewContext(bannerModerationSender.makeNewContext(1, List.of(moderationInfo)));
        var request = bannerModerationSender.convert(moderationInfo, 5L
        );

        assertThat(request.getData().getIsSocialAdvertisement()).isTrue();
    }

    @Test
    public void testSocialFlagIsFalse() {
        BannerWithModerationInfo moderationInfo = new BannerWithModerationInfo()
                .withClientId(145L)
                .withBsBannerId(12L)
                .withClientFlags(Collections.emptySet());

        Mockito.when(featureService.isEnabledForClientId(Mockito.any(ClientId.class), Mockito.eq(SOCIAL_ADVERTISING)))
                .thenReturn(false);

        bannerModerationSender.setNewContext(bannerModerationSender.makeNewContext(1, List.of(moderationInfo)));
        var request = bannerModerationSender.convert(moderationInfo, 5L
        );

        assertThat(request.getData().getIsSocialAdvertisement()).isNull();
    }

    public static class BannerModerationSender extends BaseBannerSender<Html5BannerModerationRequest,
            BannerWithModerationInfo, BannerModerationMeta> {

        public BannerModerationSender(DslContextProvider dslContextProvider, ModerationSendingRepository<Long,
                BannerWithModerationInfo> moderationSendingRepository,
                                      RemoderationFlagsRepository remoderationFlagsRepository,
                                      CampaignRepository campaignRepository,
                                      HrefParameterizingService hrefParameterizingService,
                                      FeatureService featureService,
                                      BusinessUnitModerationRepository businessUnitModerationRepository,
                                      ModerationFlagsConverter moderationFlagsConverter,
                                      ModerationOperationModeProvider moderationOperationModeProvider) {
            super(dslContextProvider, moderationSendingRepository, remoderationFlagsRepository, campaignRepository,
                    hrefParameterizingService, featureService, businessUnitModerationRepository,
                    moderationFlagsConverter, moderationOperationModeProvider);
        }

        @Override
        protected Html5BannerModerationRequest convertBanner(BannerWithModerationInfo moderationInfo, long version) {
            var x = new Html5BannerModerationRequest();
            x.setData(new Html5BannerRequestData());
            x.setMeta(new BannerModerationMeta());

            return x;
        }

        @Override
        protected BannerModerationMeta makeMetaObject() {
            return null;
        }

        @Override
        public String typeName() {
            return "test";
        }

        @Override
        protected Logger getLogger() {
            return null;
        }

        @Override
        protected long getVersion(BannerWithModerationInfo object) {
            return 0;
        }
    }

}
