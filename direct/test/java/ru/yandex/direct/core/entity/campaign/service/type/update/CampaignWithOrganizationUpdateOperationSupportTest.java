package ru.yandex.direct.core.entity.campaign.service.type.update;


import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;

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

import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.BannerWithHrefAndTurboLandingAndVcardAndOrganization;
import ru.yandex.direct.core.entity.banner.model.BannerWithOrganizationAndPhone;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithOrganizationAndPhone;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.NewBannerInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithOrganizationUpdateOperationSupportTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final Long PERMALINK_ID = ThreadLocalRandom.current().nextLong();
    private static final Long ANOTHER_PERMALINK_ID = ThreadLocalRandom.current().nextLong();

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private BannerTypedRepository newBannerTypedRepository;

    @Autowired
    private BannersUpdateOperationFactory bannersUpdateOperationFactory;

    @Autowired
    private CampaignWithOrganizationAndPhoneUpdateOperationSupport support;

    @Autowired
    private OrganizationsClientStub organizationsClient;

    @Parameterized.Parameter
    public BiFunction<ClientId, Long, Campaign> campaignSupplier;

    private Integer shard;
    private UserInfo user;

    private CampaignInfo campaignInfo;
    private NewBannerInfo bannerInfo;

    private ClientPhone phone;
    private ClientPhone anotherPhone;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return StreamEx.<BiFunction<ClientId, Long, Campaign>>of(
                TestCampaigns::activeTextCampaign,
                TestCampaigns::activeDynamicCampaign
        ).map(List::of).map(List::toArray).toList();
    }

    @Before
    public void before() {
        user = steps.userSteps().createDefaultUser();
        shard = user.getShard();

        campaignInfo = steps.campaignSteps()
                .createCampaign(campaignSupplier.apply(user.getClientId(), user.getUid()), user.getClientInfo());
        if (campaignInfo.getCampaign().getType() == CampaignType.DYNAMIC) {
            AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(campaignInfo);
            bannerInfo = steps.dynamicBannerSteps().createDynamicBanner(adGroupInfo);
        } else {
            bannerInfo = steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                    .withCampaignInfo(campaignInfo));
        }

        steps.organizationSteps().createClientOrganization(user.getClientId(), PERMALINK_ID);
        steps.organizationSteps().createClientOrganization(user.getClientId(), ANOTHER_PERMALINK_ID);

        phone = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(user.getClientId(), PERMALINK_ID);
        anotherPhone = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(user.getClientId(), ANOTHER_PERMALINK_ID);

        organizationsClient.addUidsByPermalinkId(PERMALINK_ID, List.of(user.getUid()));
        organizationsClient.addUidsByPermalinkId(ANOTHER_PERMALINK_ID, List.of(user.getUid()));

        steps.featureSteps().addClientFeature(user.getClientId(),
                FeatureName.CHANGE_BANNER_ORGANIZATION_ON_DEFAULT_CAMPAIGN_ORGANIZATION_CHANGE, true);
    }

    @Test
    public void featureDisabled_noChanges() {
        steps.featureSteps().addClientFeature(user.getClientId(),
                FeatureName.CHANGE_BANNER_ORGANIZATION_ON_DEFAULT_CAMPAIGN_ORGANIZATION_CHANGE, false);

        steps.organizationSteps().linkOrganizationToBanner(user.getClientId(), PERMALINK_ID, bannerInfo.getBannerId());
        steps.clientPhoneSteps().linkPhoneIdToBanner(shard, bannerInfo.getBannerId(), phone.getId());

        var appliedChanges = getAppliedChanges(ANOTHER_PERMALINK_ID, phone.getId());
        support.updateRelatedEntitiesOutOfTransaction(getUpdateParameters(), appliedChanges);

        checkBannerOrganization(PERMALINK_ID, phone.getId(), false);
    }

    @Test
    public void assignDefaultOrganization_bannerOrganizationAssigned() {
        var appliedChanges = getAppliedChanges(PERMALINK_ID, phone.getId());
        support.updateRelatedEntitiesOutOfTransaction(getUpdateParameters(), appliedChanges);

        checkBannerOrganization(PERMALINK_ID, phone.getId(), false);
    }

    @Test
    public void changeDefaultOrganization_bannerOrganizationChanged() {
        steps.organizationSteps().linkDefaultOrganizationToCampaign(user.getClientId(), PERMALINK_ID, campaignInfo.getCampaignId());
        steps.clientPhoneSteps().linkPhoneIdToCampaign(shard, campaignInfo.getCampaignId(), phone.getId());

        var appliedChanges = getAppliedChanges(ANOTHER_PERMALINK_ID, anotherPhone.getId());
        support.updateRelatedEntitiesOutOfTransaction(getUpdateParameters(), appliedChanges);

        checkBannerOrganization(ANOTHER_PERMALINK_ID, anotherPhone.getId(), false);
    }

    @Test
    public void deleteDefaultOrganization_bannerOrganizationDeleted() {
        steps.organizationSteps().linkDefaultOrganizationToCampaign(user.getClientId(), PERMALINK_ID, campaignInfo.getCampaignId());
        steps.clientPhoneSteps().linkPhoneIdToCampaign(shard, campaignInfo.getCampaignId(), phone.getId());

        var appliedChanges = getAppliedChanges(null, null);
        support.updateRelatedEntitiesOutOfTransaction(getUpdateParameters(), appliedChanges);

        checkBannerOrganization(null, null, null);
    }

    @Test
    public void changeDefaultPhone_bannerPhoneChanged() {
        steps.organizationSteps().linkDefaultOrganizationToCampaign(user.getClientId(), PERMALINK_ID, campaignInfo.getCampaignId());
        steps.organizationSteps().linkOrganizationToBanner(user.getClientId(), PERMALINK_ID, bannerInfo.getBannerId());

        var appliedChanges = getAppliedChanges(PERMALINK_ID, phone.getId());
        support.updateRelatedEntitiesOutOfTransaction(getUpdateParameters(), appliedChanges);

        checkBannerOrganization(PERMALINK_ID, phone.getId(), false);
    }

    @Test
    public void bannerHasOnlyOrganization_deleteDefaultOrganization_noChangesInBannersWithHrefAndTurboLanding() {
        steps.organizationSteps().linkDefaultOrganizationToCampaign(user.getClientId(), PERMALINK_ID, campaignInfo.getCampaignId());
        steps.organizationSteps().linkOrganizationToBanner(user.getClientId(), PERMALINK_ID, bannerInfo.getBannerId());
        steps.clientPhoneSteps().linkPhoneIdToCampaign(shard, campaignInfo.getCampaignId(), phone.getId());
        steps.clientPhoneSteps().linkPhoneIdToBanner(shard, bannerInfo.getBannerId(), phone.getId());
        var banner = getBanner();
        if (banner instanceof BannerWithHrefAndTurboLandingAndVcardAndOrganization) {
            // Удаляем у баннера хреф
            var bannerModelChanges = new ModelChanges<>(banner.getId(), BannerWithHrefAndTurboLandingAndVcardAndOrganization.class)
                    .process(null, BannerWithHref.HREF);
            var bannerUpdateOperation = bannersUpdateOperationFactory
                    .createPartialUpdateOperation(Collections.singletonList(bannerModelChanges), user.getUid(),
                            user.getClientId(), BannerWithHrefAndTurboLandingAndVcardAndOrganization.class);
            bannerUpdateOperation.prepareAndApply();
        }

        // Пытаемся удалить организацию
        var appliedChanges = getAppliedChanges(null, null);
        support.updateRelatedEntitiesOutOfTransaction(getUpdateParameters(), appliedChanges);

        if (banner instanceof BannerWithHrefAndTurboLandingAndVcardAndOrganization) {
            checkBannerOrganization(PERMALINK_ID, phone.getId(), false);
        } else {
            checkBannerOrganization(null, null, null);
        }
    }

    private void checkBannerOrganization(Long permalinkId, Long phoneId, Boolean isPreferVCardOverPermalink) {
        var banner = getBanner();

        assertSoftly(softly -> {
            softly.assertThat(banner.getPermalinkId()).isEqualTo(permalinkId);
            softly.assertThat(banner.getPhoneId()).isEqualTo(phoneId);
            softly.assertThat(banner.getPreferVCardOverPermalink()).isEqualTo(isPreferVCardOverPermalink);
        });
    }

    private BannerWithOrganizationAndPhone getBanner() {
        return newBannerTypedRepository
                .getStrictly(shard, Collections.singleton(bannerInfo.getBannerId()), BannerWithOrganizationAndPhone.class)
                .get(0);
    }

    private RestrictedCampaignsUpdateOperationContainer getUpdateParameters() {
        return RestrictedCampaignsUpdateOperationContainer.create(shard,
                user.getUid(),
                user.getClientId(),
                user.getUid(),
                user.getUid());
    }

    private List<AppliedChanges<CampaignWithOrganizationAndPhone>> getAppliedChanges(Long defaultPermalinkId,
                                                                                     Long defaultPhoneId) {
        var campaign = campaignTypedRepository
                .getStrictly(shard, Collections.singleton(campaignInfo.getCampaignId()), CampaignWithOrganizationAndPhone.class)
                .get(0);

        AppliedChanges<CampaignWithOrganizationAndPhone> appliedChanges =
                new ModelChanges<>(campaign.getId(), CampaignWithOrganizationAndPhone.class)
                        .process(defaultPermalinkId, CampaignWithOrganizationAndPhone.DEFAULT_PERMALINK_ID)
                        .process(defaultPhoneId, CampaignWithOrganizationAndPhone.DEFAULT_TRACKING_PHONE_ID)
                        .applyTo(campaign);
        return Collections.singletonList(appliedChanges);
    }
}
