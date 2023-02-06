package ru.yandex.direct.core.entity.campaign.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.BannerWithVcard;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.update.CampaignUpdateOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.utils.CampaignModifyTestUtils.getExpectedCampaignByCampaignType;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithContactInfoModifyUpdateTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    public CampaignModifyRepository campaignModifyRepository;
    @Autowired
    public CampaignUpdateOperationSupportFacade campaignUpdateOperationSupportFacade;
    @Autowired
    CampaignTypedRepository campaignTypedRepository;
    @Autowired
    CampaignRepository campaignRepository;
    @Autowired
    SspPlatformsRepository sspPlatformsRepository;
    @Autowired
    public Steps steps;
    @Autowired
    public BannerTypedRepository bannerTypedRepository;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    private CampaignInfo campaignInfo;

    private ClientInfo defaultClientAndUser;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        defaultClientAndUser = steps.clientSteps().createDefaultClient();
        campaignInfo = steps.campaignSteps().createActiveCampaignByCampaignType(campaignType, defaultClientAndUser);
        steps.bannerSteps().createActiveBannerByCampaignType(campaignType, campaignInfo);
        steps.bannerSteps().createActiveBannerByCampaignType(campaignType, campaignInfo);
    }

    @Test
    public void updateCampaign_NewVcard_AllBannersGetNewVcardId() {
        Long vcardId = setVcardIdToBanners();

        var banners = bannerTypedRepository.getBannersByCampaignIds(campaignInfo.getShard(),
                List.of(campaignInfo.getCampaignId()));

        List<BannerWithVcard> bannersWithVcard = StreamEx.of(banners)
                .select(BannerWithVcard.class)
                .toList();


        assertThat(bannersWithVcard.get(0).getVcardId()).isNotNull();
        assertThat(bannersWithVcard.get(1).getVcardId()).isNotNull();
        assertThat(bannersWithVcard.get(1).getVcardId()).isEqualTo(vcardId);
    }

    @Test
    public void updateCampaign_NullVcard_AllBannersGetNewVcardId() {
        setVcardIdToBanners();

        CommonCampaign updatingCampaign = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withId(campaignInfo.getCampaignId())
                .withCurrency(campaignInfo.getClientInfo().getClient().getWorkCurrency())
                .withContactInfo(fullVcard());

        CommonCampaign newCampaign = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withId(campaignInfo.getCampaignId())
                .withContactInfo(null);

        updateCampaign(updatingCampaign, newCampaign, true);

        var banners = bannerTypedRepository.getBannersByCampaignIds(campaignInfo.getShard(),
                List.of(campaignInfo.getCampaignId()));

        List<BannerWithVcard> bannersWithVcard = StreamEx.of(banners)
                .select(BannerWithVcard.class)
                .toList();


        assertThat(bannersWithVcard.get(0).getVcardId()).isNull();
        assertThat(bannersWithVcard.get(1).getVcardId()).isNull();
    }

    @Test
    public void updateCampaign_SameNullVcardProcessed_AllBannersVcardsCleaned() {
        CommonCampaign updatingCampaign = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withId(campaignInfo.getCampaignId())
                .withCurrency(campaignInfo.getClientInfo().getClient().getWorkCurrency())
                .withContactInfo(null);

        CommonCampaign newCampaign = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withId(campaignInfo.getCampaignId())
                .withContactInfo(null);

        updateCampaign(updatingCampaign, newCampaign, true);

        var banners = bannerTypedRepository.getBannersByCampaignIds(campaignInfo.getShard(),
                List.of(campaignInfo.getCampaignId()));

        List<BannerWithVcard> bannersWithVcard = StreamEx.of(banners)
                .select(BannerWithVcard.class)
                .toList();


        assertThat(bannersWithVcard.get(0).getVcardId()).isNull();
        assertThat(bannersWithVcard.get(1).getVcardId()).isNull();
    }

    @Test
    public void updateCampaign_VcardNotProcessed_AllBannersVcardsUnchnged() {
        Long vcardId = setVcardIdToBanners();

        CommonCampaign updatingCampaign = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withId(campaignInfo.getCampaignId())
                .withCurrency(campaignInfo.getClientInfo().getClient().getWorkCurrency())
                .withContactInfo(null);

        CommonCampaign newCampaign = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withId(campaignInfo.getCampaignId())
                .withContactInfo(null);

        updateCampaign(updatingCampaign, newCampaign, false);

        var banners = bannerTypedRepository.getBannersByCampaignIds(campaignInfo.getShard(),
                List.of(campaignInfo.getCampaignId()));

        List<BannerWithVcard> bannersWithVcard = StreamEx.of(banners)
                .select(BannerWithVcard.class)
                .toList();


        assertThat(bannersWithVcard.get(0).getVcardId()).isEqualTo(vcardId);
        assertThat(bannersWithVcard.get(1).getVcardId()).isEqualTo(vcardId);
    }

    private Long setVcardIdToBanners() {
        CommonCampaign updatingCampaign = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withId(campaignInfo.getCampaignId())
                .withCurrency(campaignInfo.getClientInfo().getClient().getWorkCurrency());
        var vcard = fullVcard();
        CommonCampaign newCampaign = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withId(campaignInfo.getCampaignId())
                .withContactInfo(vcard);

        updateCampaign(updatingCampaign, newCampaign, true);
        return vcard.getId();
    }

    private void updateCampaign(CommonCampaign updatingCampaign,
                                CommonCampaign newCampaign, boolean processContactInfo) {
        ModelChanges<CommonCampaign> campaignModelChanges =
                new ModelChanges(updatingCampaign.getId(), TestCampaigns.getCampaignClassByCampaignType(campaignType));
        campaignModelChanges.process(LocalDate.now().plusDays(1), CommonCampaign.START_DATE);

        if (processContactInfo) {
            campaignModelChanges.process(newCampaign.getContactInfo(), CommonCampaign.CONTACT_INFO);
        }

        AppliedChanges<CommonCampaign> campaignAppliedChanges =
                campaignModelChanges.applyTo(updatingCampaign);
        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClientStub,
                List.of(defaultClientAndUser.getUid()), Set.of());
        var container = new RestrictedCampaignsUpdateOperationContainerImpl(campaignInfo.getShard(),
                campaignInfo.getUid(), campaignInfo.getClientId(), campaignInfo.getUid(), campaignInfo.getUid(),
                metrikaClientAdapter, new CampaignOptions(), null, emptyMap());
        container.setCampaignType(updatingCampaign.getId(), campaignType);

        campaignModifyRepository.updateCampaigns(container,
                Collections.singletonList(campaignAppliedChanges));

        campaignUpdateOperationSupportFacade.updateRelatedEntitiesOutOfTransaction(container,
                List.of(campaignAppliedChanges));
        campaignUpdateOperationSupportFacade.updateRelatedEntitiesOutOfTransactionWithModelChanges(container,
                List.of(campaignModelChanges), List.of(campaignAppliedChanges));

        getExpectedCampaignByCampaignType(campaignType, campaignInfo, campaignModelChanges);
    }

}
