package ru.yandex.direct.core.entity.banner.type.phone;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainerService;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
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
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
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
public class BannerWithOrganizationAndPhoneValidatorProviderAddTest {

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Mock
    private BannerWithCreativeHelper bannerWithCreativeHelper;
    @Mock
    private CreativeService creativeService;
    @Mock
    private AdGroupRepository adGroupRepository;
    @Mock
    private BannerImageRepository bannerImageRepository;
    @Mock
    private BannerImageFormatRepository bannerImageFormatRepository;
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
    private MobileAppRepository mobileAppRepository;

    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private ClientPhoneRepository clientPhoneRepository;
    @Mock
    private BannersAddOperationContainerImpl container;
    @Mock
    private NetAcl netAcl;
    @Autowired
    public Steps steps;

    private ClientId clientId;
    private TextBanner banner;
    private BannersAddOperationContainerService service;
    private BannerWithOrganizationAndPhoneValidatorProvider provider;

    @Before
    public void before() {
        organizationService = spy(organizationService);
        var clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        var adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        Long permalinkId = defaultActiveOrganization(clientInfo.getClientId()).getPermalinkId();
        banner = fullTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withPermalinkId(permalinkId)
                .withPreferVCardOverPermalink(false);

        when(container.getClientOrganizations()).thenReturn(emptyMap());
        when(container.getClientId()).thenReturn(clientInfo.getClientId());
        when(container.isUniversalCampaignBanner(eq(banner))).thenReturn(false);
        when(container.isUcPreValidation()).thenReturn(false);
        when(container.getAllowedPhoneIds()).thenCallRealMethod();
        doCallRealMethod().when(container).setAllowedPhoneIds(any(ClientPhoneIdsByTypeContainer.class));
        service = new BannersAddOperationContainerService(campaignTypedRepository,
                ppcPropertiesSupport,
                bannerWithCreativeHelper,
                creativeService,
                adGroupRepository,
                organizationService,
                bannerImageRepository,
                bannerImageFormatRepository,
                sitelinkSetRepository,
                turboLandingRepository,
                pricePackageRepository,
                vcardRepository,
                clientPhoneRepository,
                mobileAppRepository,
                netAcl);
        provider = new BannerWithOrganizationAndPhoneValidatorProvider();
    }

    @Test
    public void validator_addBannerWithoutPhone_success() {
        when(container.isFeatureEnabledForClient(FeatureName.TELEPHONY_ALLOWED)).thenReturn(false);
        service.fillContainers(container, List.of(banner.withPhoneId(null)));
        provider.addValidator(container, List.of(banner.withPhoneId(null)));
        verify(organizationService, times(0)).hasAccess(any(), anyCollection());
    }

    @Test
    public void validator_addBannerWithPhone_success() {
        when(container.isFeatureEnabledForClient(FeatureName.TELEPHONY_ALLOWED)).thenReturn(false);
        Long phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        service.fillContainers(container, List.of(banner.withPhoneId(phoneId)));
        provider.addValidator(container, List.of(banner.withPhoneId(phoneId)));
        verify(organizationService, times(1)).hasAccess(any(), anyCollection());
    }

    @Test
    public void validator_addBannerWithPhone_featureAllowed_success() {
        when(container.isFeatureEnabledForClient(FeatureName.TELEPHONY_ALLOWED)).thenReturn(true);
        Long phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        service.fillContainers(container, List.of(banner.withPhoneId(phoneId)));
        provider.addValidator(container, List.of(banner.withPhoneId(phoneId)));
        verify(organizationService, times(1)).hasAccess(any(), anyCollection());
    }

    @Test
    public void validator_addBannerWithOrgPhone_featureAllowed_success() {
        when(container.isFeatureEnabledForClient(FeatureName.TELEPHONY_ALLOWED)).thenReturn(true);
        Long phoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId).getId();
        service.fillContainers(container, List.of(banner.withPhoneId(phoneId)));
        provider.addValidator(container, List.of(banner.withPhoneId(phoneId)));
        verify(organizationService, times(0)).hasAccess(any(), anyCollection());
    }

    @Test
    public void validator_addUcBannerWithPhoneOnPreValidation_featureAllowed_success() {
        when(container.isFeatureEnabledForClient(FeatureName.TELEPHONY_ALLOWED)).thenReturn(true);
        when(container.isUniversalCampaignBanner(eq(banner))).thenReturn(true);
        when(container.isUcPreValidation()).thenReturn(true);

        Long phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        service.fillContainers(container, List.of(banner.withPhoneId(phoneId)));
        provider.addValidator(container, List.of(banner.withPhoneId(phoneId)));
        verify(organizationService, times(1)).hasAccess(any(), anyCollection());
    }

    @Test
    public void validator_addUcBannerWithPhone_featureAllowed_success() {
        when(container.isFeatureEnabledForClient(FeatureName.TELEPHONY_ALLOWED)).thenReturn(true);
        when(container.isUniversalCampaignBanner(eq(banner))).thenReturn(true);
        when(container.isUcPreValidation()).thenReturn(false);

        Long phoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        service.fillContainers(container, List.of(banner.withPhoneId(phoneId)));
        provider.addValidator(container, List.of(banner.withPhoneId(phoneId)));
        verify(organizationService, never()).hasAccess(any(), anyCollection());
    }

}
