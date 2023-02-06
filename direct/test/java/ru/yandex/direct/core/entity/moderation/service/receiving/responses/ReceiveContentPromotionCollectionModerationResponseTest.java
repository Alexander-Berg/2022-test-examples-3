package ru.yandex.direct.core.entity.moderation.service.receiving.responses;


import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.repository.receiving.BannersModerationReceivingRepository;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.receiving.BannerChangesValidator;
import ru.yandex.direct.core.entity.moderation.service.receiving.ContentPromotionCollectionModerationResponseReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.processing_configurations.DefaultBannerOperations;
import ru.yandex.direct.core.entity.moderation.service.receiving.processor.response_parser.BannerResponseParser;
import ru.yandex.direct.core.entity.moderationdiag.service.ModerationDiagService;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ContentPromotionBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.intapi.client.IntApiClient;

import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate.SENT;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerCollectionType;

@ContextHierarchy(value = {
        @ContextConfiguration(classes = CoreTestingConfiguration.class),
        @ContextConfiguration(classes = ReceiveContentPromotionCollectionModerationResponseTest.MockedConf.class),
})

@ParametersAreNonnullByDefault
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveContentPromotionCollectionModerationResponseTest extends OldAbstractBannerModerationResponseTest {
    @Configuration
    public static class MockedConf {
        @Bean
        @Primary
        public IntApiClient intApiClient() {
            return mock(IntApiClient.class);
        }
    }

    private ContentPromotionCollectionModerationResponseReceivingService
            contentPromotionCollectionModerationResponseReceivingService;

    @Autowired
    private BannerRelationsRepository bannerRelationsRepository;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ModerationDiagService moderationDiagService;

    @Autowired
    private BannersModerationReceivingRepository bannersModerationReceivingRepository;

    private ClientInfo clientInfo;
    private ContentPromotionBannerInfo bannerInfo;
    private AdGroupInfo adGroupInfo;

    private int shard;

    @Autowired
    private DefaultBannerOperations bannerOperations;

    @Autowired
    private BannerResponseParser bannerResponseParser;

    @Autowired
    private BannerChangesValidator bannerChangesValidator;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Before
    public void setUp() throws Exception {

        if (contentPromotionCollectionModerationResponseReceivingService == null) {

            contentPromotionCollectionModerationResponseReceivingService =
                    new ContentPromotionCollectionModerationResponseReceivingService(bannerRelationsRepository,
                            adGroupRepository,
                            dslContextProvider,
                            bannersModerationReceivingRepository, bannerOperations, bannerResponseParser,
                            bannerChangesValidator, ppcPropertiesSupport);
        }

        bannerInfo = steps.bannerSteps().
                createActiveContentPromotionBannerCollectionType(
                        activeContentPromotionBannerCollectionType().
                                withStatusModerate(SENT));

        clientInfo = bannerInfo.getClientInfo();
        adGroupInfo = bannerInfo.getAdGroupInfo();
        shard = bannerInfo.getShard();

        testModerationRepository.createBannerVersion(shard, bannerInfo.getBannerId(), getDefaultVersion());
    }


    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected ModerationReceivingService<BannerModerationResponse> getReceivingService() {
        return contentPromotionCollectionModerationResponseReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        var bannerInfo = steps.bannerSteps().
                createActiveContentPromotionBannerCollectionType(
                        activeContentPromotionBannerCollectionType().
                                withStatusModerate(SENT), adGroupInfo);

        testModerationRepository.createBannerVersion(shard, bannerInfo.getBannerId(), version);

        return bannerInfo.getBannerId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return ModerationObjectType.CONTENT_PROMOTION_COLLECTION;
    }

    @Override
    protected long getDefaultVersion() {
        return 1L;
    }

    @Override
    protected long getDefaultObjectId() {
        return bannerInfo.getBannerId();
    }

    @Override
    protected ClientInfo getDefaultObjectClientInfo() {
        return clientInfo;
    }
}
