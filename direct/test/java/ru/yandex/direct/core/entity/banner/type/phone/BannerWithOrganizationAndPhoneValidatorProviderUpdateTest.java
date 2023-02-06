package ru.yandex.direct.core.entity.banner.type.phone;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.container.BannersUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.container.BannersUpdateOperationContainerService;
import ru.yandex.direct.core.entity.banner.model.BannerWithOrganizationAndPhone;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.type.creative.BannerWithCreativeHelper;
import ru.yandex.direct.core.entity.banner.type.image.BannerImageRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.clientphone.ClientPhoneIdsByTypeContainer;
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository;
import ru.yandex.direct.core.entity.creative.service.CreativeService;
import ru.yandex.direct.core.entity.image.repository.BannerImageFormatRepository;
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppRepository;
import ru.yandex.direct.core.entity.organizations.service.OrganizationService;
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository;
import ru.yandex.direct.core.entity.sitelink.repository.SitelinkSetRepository;
import ru.yandex.direct.core.entity.turbolanding.repository.TurboLandingRepository;
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultActiveOrganization;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithOrganizationAndPhoneValidatorProviderUpdateTest {


    @Mock
    private BannerTypedRepository typedRepository;
    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Mock
    private BannerWithCreativeHelper bannerWithCreativeHelper;
    @Mock
    private CreativeService creativeService;
    @Mock
    private AdGroupRepository adGroupRepository;
    @Mock
    private SitelinkSetRepository sitelinkSetRepository;
    @Mock
    private TurboLandingRepository turboLandingRepository;
    @Mock
    PricePackageRepository pricePackageRepository;
    @Mock
    private VcardRepository vcardRepository;
    @Mock
    private CampaignTypedRepository campaignTypedRepository;
    @Mock
    private BannerImageRepository bannerImageRepository;
    @Mock
    private BannerImageFormatRepository bannerImageFormatRepository;
    @Mock
    private MobileAppRepository mobileAppRepository;

    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private ClientPhoneRepository clientPhoneRepository;
    @Mock
    private BannersUpdateOperationContainerImpl container;
    @Mock
    private NetAcl netAcl;
    @Autowired
    public Steps steps;

    private ClientInfo clientInfo;
    private BannersUpdateOperationContainerService service;
    private BannerWithOrganizationAndPhoneValidatorProvider provider;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        when(container.getClientOrganizations()).thenReturn(emptyMap());
        when(container.getClientId()).thenReturn(clientInfo.getClientId());
        when(container.isUniversalCampaignBanner(any())).thenReturn(false);
        when(container.isUcPreValidation()).thenReturn(false);
        when(container.isFeatureEnabledForClient(FeatureName.TELEPHONY_ALLOWED))
                .thenReturn(false);
        when(container.getAllowedPhoneIds()).thenCallRealMethod();
        doCallRealMethod().when(container).setAllowedPhoneIds(any(ClientPhoneIdsByTypeContainer.class));
        organizationService = spy(organizationService);
        service = new BannersUpdateOperationContainerService(campaignTypedRepository,
                ppcPropertiesSupport,
                bannerWithCreativeHelper,
                creativeService,
                typedRepository,
                adGroupRepository,
                organizationService,
                vcardRepository,
                sitelinkSetRepository,
                turboLandingRepository,
                pricePackageRepository,
                bannerImageRepository,
                bannerImageFormatRepository,
                clientPhoneRepository,
                mobileAppRepository,
                netAcl);
        provider = new BannerWithOrganizationAndPhoneValidatorProvider();
    }

    @Test
    public void validator_phoneNoChanged_success() {
        var phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientInfo.getClientId()).getId();
        NewTextBannerInfo bannerInfoWithPhone = createBanner(phoneId);
        var modelChanges = new ModelChanges<>(bannerInfoWithPhone.getBannerId(), TextBanner.class)
                .process("new title", TextBanner.TITLE);
        var ac = modelChanges.applyTo(bannerInfoWithPhone.getBanner()).castModelUp(BannerWithOrganizationAndPhone.class);
        service.fillContainerOnChangesApplied(container, List.of(ac));
        provider.updateValidator(container, Map.of(1, ac));
        verify(organizationService, times(0)).hasAccess(any(), anyCollection());
    }

    @Test
    public void validator_phoneNoChanged_permalinkChanged_bannerWithoutPhone_success() {
        NewTextBannerInfo bannerInfoWithoutPhone = createBanner(null);
        var modelChanges = new ModelChanges<>(bannerInfoWithoutPhone.getBannerId(), TextBanner.class)
                .process(123L, TextBanner.PERMALINK_ID)
                .process("new title", TextBanner.TITLE);
        var ac = modelChanges.applyTo(bannerInfoWithoutPhone.getBanner()).castModelUp(BannerWithOrganizationAndPhone.class);
        service.fillContainerOnChangesApplied(container, List.of(ac));
        provider.updateValidator(container, Map.of(1, ac));
        verify(organizationService, times(0)).hasAccess(any(), anyCollection());
    }

    @Test
    public void validator_phoneNoChanged_permalinkChanged_bannerWithPhone_success() {
        var phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientInfo.getClientId()).getId();
        NewTextBannerInfo bannerInfoWithPhone = createBanner(phoneId);
        var modelChanges = new ModelChanges<>(bannerInfoWithPhone.getBannerId(), TextBanner.class)
                .process(123L, TextBanner.PERMALINK_ID)
                .process("new title", TextBanner.TITLE);
        var ac = modelChanges.applyTo(bannerInfoWithPhone.getBanner()).castModelUp(BannerWithOrganizationAndPhone.class);
        service.fillContainerOnChangesApplied(container, List.of(ac));
        provider.updateValidator(container, Map.of(1, ac));
        verify(organizationService, times(1)).hasAccess(any(), anyCollection());
    }

    @Test
    public void validator_phoneNoChanged_permalinkChanged_bannerWithOrgPhone_featureAllowed_success() {
        when(container.isFeatureEnabledForClient(FeatureName.TELEPHONY_ALLOWED)).thenReturn(true);
        var phoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientInfo.getClientId()).getId();
        NewTextBannerInfo bannerInfoWithPhone = createBanner(phoneId);
        var modelChanges = new ModelChanges<>(bannerInfoWithPhone.getBannerId(), TextBanner.class)
                .process(123L, TextBanner.PERMALINK_ID)
                .process("new title", TextBanner.TITLE);
        var ac = modelChanges.applyTo(bannerInfoWithPhone.getBanner()).castModelUp(BannerWithOrganizationAndPhone.class);
        service.fillContainerOnChangesApplied(container, List.of(ac));
        provider.updateValidator(container, Map.of(1, ac));
        verify(organizationService, times(0)).hasAccess(any(), anyCollection());
    }

    @Test
    public void validator_phoneDeleted_permalinkChanged_success() {
        var phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientInfo.getClientId()).getId();
        NewTextBannerInfo bannerInfoWithPhone = createBanner(phoneId);
        var modelChanges = new ModelChanges<>(bannerInfoWithPhone.getBannerId(), TextBanner.class)
                .process(123L, TextBanner.PERMALINK_ID)
                .process(null, TextBanner.PHONE_ID)
                .process("new title", TextBanner.TITLE);
        var ac = modelChanges.applyTo(bannerInfoWithPhone.getBanner()).castModelUp(BannerWithOrganizationAndPhone.class);
        service.fillContainerOnChangesApplied(container, List.of(ac));
        provider.updateValidator(container, Map.of(1, ac));
        verify(organizationService, times(0)).hasAccess(any(), anyCollection());
    }

    @Test
    public void validator_phoneChanges_success() {
        var phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientInfo.getClientId()).getId();
        NewTextBannerInfo bannerInfoWithPhone = createBanner(phoneId);
        var newPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientInfo.getClientId()).getId();
        var modelChanges = new ModelChanges<>(bannerInfoWithPhone.getBannerId(), TextBanner.class)
                .process(newPhoneId, TextBanner.PHONE_ID);
        var ac = modelChanges.applyTo(bannerInfoWithPhone.getBanner()).castModelUp(BannerWithOrganizationAndPhone.class);
        service.fillContainerOnChangesApplied(container, List.of(ac));
        provider.updateValidator(container, Map.of(1, ac));
        verify(organizationService, times(1)).hasAccess(any(), anyCollection());
    }

    @Test
    public void validator_phoneChanges_featureAllowed_success() {
        when(container.isFeatureEnabledForClient(FeatureName.TELEPHONY_ALLOWED)).thenReturn(true);
        var phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientInfo.getClientId()).getId();
        NewTextBannerInfo bannerInfoWithPhone = createBanner(phoneId);
        var newPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientInfo.getClientId()).getId();
        var modelChanges = new ModelChanges<>(bannerInfoWithPhone.getBannerId(), TextBanner.class)
                .process(newPhoneId, TextBanner.PHONE_ID);
        var ac = modelChanges.applyTo(bannerInfoWithPhone.getBanner()).castModelUp(BannerWithOrganizationAndPhone.class);
        service.fillContainerOnChangesApplied(container, List.of(ac));
        provider.updateValidator(container, Map.of(1, ac));
        verify(organizationService, times(1)).hasAccess(any(), anyCollection());
    }

    @Test
    public void validator_orgPhoneChanges_featureAllowed_success() {
        when(container.isFeatureEnabledForClient(FeatureName.TELEPHONY_ALLOWED)).thenReturn(true);
        var phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientInfo.getClientId()).getId();
        NewTextBannerInfo bannerInfoWithPhone = createBanner(phoneId);
        var newPhoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientInfo.getClientId()).getId();
        var modelChanges = new ModelChanges<>(bannerInfoWithPhone.getBannerId(), TextBanner.class)
                .process(newPhoneId, TextBanner.PHONE_ID);
        var ac = modelChanges.applyTo(bannerInfoWithPhone.getBanner()).castModelUp(BannerWithOrganizationAndPhone.class);
        service.fillContainerOnChangesApplied(container, List.of(ac));
        provider.updateValidator(container, Map.of(1, ac));
        verify(organizationService, times(0)).hasAccess(any(), anyCollection());
    }

    @Test
    public void validator_phoneChanges_ucPreValidation_success() {
        when(container.isUniversalCampaignBanner(any())).thenReturn(true);
        when(container.isUcPreValidation()).thenReturn(true);

        var phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientInfo.getClientId()).getId();
        NewTextBannerInfo bannerInfoWithPhone = createBanner(phoneId);
        var newPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientInfo.getClientId()).getId();
        var modelChanges = new ModelChanges<>(bannerInfoWithPhone.getBannerId(), TextBanner.class)
                .process(newPhoneId, TextBanner.PHONE_ID);
        var ac = modelChanges.applyTo(bannerInfoWithPhone.getBanner()).castModelUp(BannerWithOrganizationAndPhone.class);
        service.fillContainerOnChangesApplied(container, List.of(ac));
        provider.updateValidator(container, Map.of(1, ac));
        verify(organizationService, times(1)).hasAccess(any(), anyCollection());
    }

    @Test
    public void validator_phoneChanges_ucBanner_success() {
        when(container.isUniversalCampaignBanner(any())).thenReturn(true);
        when(container.isUcPreValidation()).thenReturn(false);

        var phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientInfo.getClientId()).getId();
        NewTextBannerInfo bannerInfoWithPhone = createBanner(phoneId);
        var newPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientInfo.getClientId()).getId();
        var modelChanges = new ModelChanges<>(bannerInfoWithPhone.getBannerId(), TextBanner.class)
                .process(newPhoneId, TextBanner.PHONE_ID);
        var ac = modelChanges.applyTo(bannerInfoWithPhone.getBanner()).castModelUp(BannerWithOrganizationAndPhone.class);
        service.fillContainerOnChangesApplied(container, List.of(ac));
        provider.updateValidator(container, Map.of(1, ac));
        verify(organizationService, never()).hasAccess(any(), anyCollection());
    }

    private NewTextBannerInfo createBanner(Long phoneId) {
        var adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        Long permalinkId = defaultActiveOrganization(clientInfo.getClientId()).getPermalinkId();
        var bannerWithPhone = fullTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withPermalinkId(permalinkId)
                .withPreferVCardOverPermalink(false)
                .withPhoneId(phoneId);
        var textBannerInfoWithPhone = new NewTextBannerInfo().withBanner(bannerWithPhone).withAdGroupInfo(adGroupInfo);
        return steps.textBannerSteps().createBanner(textBannerInfoWithPhone);
    }
}
