package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.banner.type.href.BannerDomainRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.core.entity.moderation.model.BannerModerationResponse;
import ru.yandex.direct.core.entity.moderation.repository.receiving.BannersModerationReceivingRepository;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.receiving.BannerChangesValidator;
import ru.yandex.direct.core.entity.moderation.service.receiving.CpmYndxFrontpageModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.dyn_disclaimers.DisclaimersUpdatingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.processing_configurations.CpmYndxFrontpageOperations;
import ru.yandex.direct.core.entity.moderation.service.receiving.processor.response_parser.BannerResponseParser;
import ru.yandex.direct.core.entity.moderationdiag.service.ModerationDiagService;
import ru.yandex.direct.core.entity.moderationreason.repository.ModerationReasonRepository;
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService;
import ru.yandex.direct.core.entity.turbolanding.repository.TurboLandingRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CpmBannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.intapi.client.IntApiClient;

import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate.SENT;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.Yes;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5;

@ContextHierarchy(value = {
        @ContextConfiguration(classes = CoreTestingConfiguration.class),
        @ContextConfiguration(classes = ReceiveYndxFrontpageModerationResponseTest.MockedConf.class),
})

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveYndxFrontpageModerationResponseTest extends OldAbstractBannerModerationResponseTest {


    @Configuration
    public static class MockedConf {
        @Bean
        public IntApiClient intApiClient() {
            return mock(IntApiClient.class);
        }

    }

    @Autowired
    private IntApiClient intApiClient;

    private CpmYndxFrontpageModerationReceivingService cpmYndxFrontpageModerationReceivingService;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private Steps steps;


    @Autowired
    private ModerationReasonRepository moderationReasonRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ModerationDiagService moderationDiagService;

    @Autowired
    private TurboLandingRepository turboLandingRepository;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private PricePackageService pricePackageService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private DisclaimersUpdatingService disclaimersUpdatingService;

    @Autowired
    private BannersModerationReceivingRepository bannersModerationReceivingRepository;

    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private BannerDomainRepository bannerDomainRepository;

    private ClientInfo clientInfo;
    private CpmBannerInfo bannerInfo;
    private AdGroupInfo adGroupInfo;

    private int shard;
    private long adGroupId;
    private long creativeId;

    @Autowired
    private CpmYndxFrontpageOperations bannerOperations;

    @Autowired
    private BannerResponseParser bannerResponseParser;

    @Autowired
    private BannerChangesValidator bannerChangesValidator;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Before
    public void setUp() throws Exception {
        if (cpmYndxFrontpageModerationReceivingService == null) {
            cpmYndxFrontpageModerationReceivingService =
                    new CpmYndxFrontpageModerationReceivingService(bannerRelationsRepository,
                            adGroupRepository,
                            dslContextProvider,
                            bannerOperations,
                            bannersModerationReceivingRepository, bannerResponseParser,
                            bannerChangesValidator,
                            ppcPropertiesSupport
                    );
        }

        clientInfo = steps.clientSteps().createDefaultClient();
        adGroupInfo = steps.adGroupSteps().createDefaultCpmYndxFrontpageAdGroup(clientInfo);
        Creative creative = defaultHtml5(null, null).withHeight(67L).withWidth(320L);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, clientInfo);

        adGroupId = adGroupInfo.getAdGroupId();
        creativeId = creativeInfo.getCreativeId();

        OldCpmBanner banner =
                activeCpmBanner(adGroupInfo.getCampaignId(), adGroupId, creativeId)
                        .withStatusModerate(SENT).withLanguage(defaultLanguage());
        bannerInfo = steps.bannerSteps().createActiveCpmBanner(banner, adGroupInfo);
        shard = bannerInfo.getShard();
        testModerationRepository.createBannerVersion(shard, bannerInfo.getBannerId(), getDefaultVersion());
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected ModerationReceivingService<BannerModerationResponse> getReceivingService() {
        return cpmYndxFrontpageModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        OldCpmBanner banner =
                activeCpmBanner(adGroupInfo.getCampaignId(), adGroupId, creativeId)
                        .withStatusModerate(SENT).withLanguage(defaultLanguage());
        var bannerInfo = steps.bannerSteps().createActiveCpmBanner(banner, adGroupInfo);
        testModerationRepository.createBannerVersion(shard, bannerInfo.getBannerId(), version);

        return banner.getId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return ModerationObjectType.YNDX_FRONTPAGE_CREATIVE;
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

    /**
     * Язык
     */

    @Test
    public void moderationResponseWithLanguage_savedInDb() {
        BannerModerationResponse response = createResponseForDefaultObject(Yes, "uk");

        var unknownVerdictCountAndSuccess = cpmYndxFrontpageModerationReceivingService
                .processModerationResponses(shard, singletonList(response));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response);
        OldBanner bannerInDb = getBanner(getDefaultObjectId());
        assertThat("языка из ответа модерации должен попадать в таблицу", bannerInDb.getLanguage(), is(Language.UK));
    }

    @Test
    public void moderationResponseWithRuLanguage_savedInDb() {
        BannerModerationResponse response = createResponseForDefaultObject(Yes, "ru");

        var unknownVerdictCountAndSuccess = cpmYndxFrontpageModerationReceivingService
                .processModerationResponses(shard, singletonList(response));

        assertEquals(0, (int) unknownVerdictCountAndSuccess.getLeft());
        assertEquals(1, unknownVerdictCountAndSuccess.getRight().size());
        checkInDb(response);
        OldBanner bannerInDb = getBanner(getDefaultObjectId());
        assertThat("языка из ответа модерации должен попадать в таблицу", bannerInDb.getLanguage(), is(Language.RU_));
    }

    @Test
    public void moderationResponseWithLanguageUpdated_savedInDb() {
        BannerModerationResponse response1 = createResponseForDefaultObject(Yes, "de");
        BannerModerationResponse response2 = createResponseForDefaultObject(Yes, "en");

        cpmYndxFrontpageModerationReceivingService.processModerationResponses(shard, singletonList(response1));
        cpmYndxFrontpageModerationReceivingService.processModerationResponses(shard, singletonList(response2));

        checkInDb(response2);
        OldBanner bannerInDb = getBanner(getDefaultObjectId());
        assertThat("язык из ответа модерации должен затирать существующий язык", bannerInDb.getLanguage(),
                is(Language.EN));
    }

    @Test
    public void moderationResponseWithLanguageNotUpdated_oldValueIsInDb() {
        BannerModerationResponse response1 = createResponseForDefaultObject(Yes, "kk");
        BannerModerationResponse response2 = createResponseForDefaultObject(Yes, null);

        cpmYndxFrontpageModerationReceivingService.processModerationResponses(shard, singletonList(response1));
        cpmYndxFrontpageModerationReceivingService.processModerationResponses(shard, singletonList(response2));

        checkInDb(response2);
        OldBanner bannerInDb = getBanner(getDefaultObjectId());

        assertThat("отсутствие языка не должно влиять на значение в базе", bannerInDb.getLanguage(),
                is(Language.KK));
    }

    @Test
    public void moderationResponseWithInvalidLanguage_noException() {
        BannerModerationResponse response = createResponseForDefaultObject(Yes, "invalid");

        cpmYndxFrontpageModerationReceivingService.processModerationResponses(shard, singletonList(response));
        checkInDb(response);
        OldBanner bannerInDb = getBanner(getDefaultObjectId());
        assertThat("язык из ответа модерации не должен затирать существующий язык", bannerInDb.getLanguage(),
                is(Language.RU_));
    }

    @Test
    public void moderationResponseWithYesLanguage_notSavedInDb() {
        BannerModerationResponse response1 = createResponseForDefaultObject(Yes, "tr");
        BannerModerationResponse response2 = createResponseForDefaultObject(Yes, "Yes");

        cpmYndxFrontpageModerationReceivingService.processModerationResponses(shard, singletonList(response1));
        cpmYndxFrontpageModerationReceivingService.processModerationResponses(shard, singletonList(response2));

        checkInDb(response2);
        OldBanner bannerInDb = getBanner(getDefaultObjectId());
        assertThat("язык из ответа модерации должен затирать существующий язык", bannerInDb.getLanguage(),
                is(Language.YES));
    }

    @Test
    public void moderationResponseWithNoLanguage_notSavedInDb() {
        BannerModerationResponse response1 = createResponseForDefaultObject(Yes, "uz");
        BannerModerationResponse response2 = createResponseForDefaultObject(Yes, "No");

        cpmYndxFrontpageModerationReceivingService.processModerationResponses(shard, singletonList(response1));
        cpmYndxFrontpageModerationReceivingService.processModerationResponses(shard, singletonList(response2));

        checkInDb(response2);
        OldBanner bannerInDb = getBanner(getDefaultObjectId());
        assertThat("язык из ответа модерации должен затирать существующий язык", bannerInDb.getLanguage(),
                is(Language.NO));
    }


}
