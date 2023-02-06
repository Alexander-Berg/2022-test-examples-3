package ru.yandex.direct.grid.core.entity.banner.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.core.configuration.GridCoreTest;
import ru.yandex.direct.grid.core.entity.AdGroupIdInFilterBaseTest;
import ru.yandex.direct.grid.core.entity.banner.model.GdiBanner;
import ru.yandex.direct.rbac.RbacService;

import static ru.yandex.direct.core.entity.organization.model.PermalinkAssignType.AUTO;
import static ru.yandex.direct.core.entity.organization.model.PermalinkAssignType.MANUAL;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultOrganization;
import static ru.yandex.direct.feature.FeatureName.SHOW_AUTO_ASSIGNED_ORGANIZATIONS;

/**
 * Проверка обогащения баннеров данными организации.
 */
@GridCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class GridBannerServiceAddOrganizationDataTest extends AdGroupIdInFilterBaseTest {
    @Autowired
    private Steps steps;
    @Autowired
    private GridBannerService gridBannerService;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private RbacService rbacService;
    @Autowired
    private OrganizationsClientStub organizationsClient;
    @Autowired
    private FeatureService featureService;

    private ClientId clientId;
    private Integer shard;
    private AdGroupInfo adGroupInfo;
    private Long bannerId;
    private Long ownedPermalink;
    private Long secondOwnedPermalink;
    private Long notOwnedPermalink;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();

        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        bannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();

        ownedPermalink = defaultOrganization(clientId).getPermalinkId();
        secondOwnedPermalink = defaultOrganization(clientId).getPermalinkId();
        notOwnedPermalink = defaultOrganization(clientId).getPermalinkId();

        Long chief = rbacService.getChiefByClientId(clientId);
        organizationsClient.addUidsByPermalinkId(ownedPermalink, List.of(chief));
        organizationsClient.addUidsByPermalinkId(secondOwnedPermalink, List.of(chief));
    }

    @Test
    public void addExternalDataToGdiBanner_featureEnabled_BannerWithoutPermalink_NoPermalinkAdded() {
        steps.featureSteps().addClientFeature(clientId, SHOW_AUTO_ASSIGNED_ORGANIZATIONS, true);
        List<GdiBanner> gdiBanners = getBanners(List.of(bannerId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdiBanners.get(0).getPermalinkId()).isEqualTo(null);
            softly.assertThat(gdiBanners.get(0).getPermalinkAssignType()).isEqualTo(null);
        });
    }

    @Test
    public void addExternalDataToGdiBanner_featureEnabled_BannerWithManualOwnedPermalink_ManualPermalinkAdded() {
        steps.featureSteps().addClientFeature(clientId, SHOW_AUTO_ASSIGNED_ORGANIZATIONS, true);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, ownedPermalink), MANUAL);

        List<GdiBanner> gdiBanners = getBanners(List.of(bannerId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdiBanners.get(0).getPermalinkId()).isEqualTo(ownedPermalink);
            softly.assertThat(gdiBanners.get(0).getPermalinkAssignType()).isEqualTo(MANUAL);
        });
    }

    @Test
    public void addExternalDataToGdiBanner_featureDisabled_BannerWithManualOwnedPermalink_ManualPermalinkAdded() {
        steps.featureSteps().addClientFeature(clientId, SHOW_AUTO_ASSIGNED_ORGANIZATIONS, false);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, ownedPermalink), MANUAL);

        List<GdiBanner> gdiBanners = getBanners(List.of(bannerId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdiBanners.get(0).getPermalinkId()).isEqualTo(ownedPermalink);
            softly.assertThat(gdiBanners.get(0).getPermalinkAssignType()).isEqualTo(MANUAL);
        });
    }

    @Test
    public void addExternalDataToGdiBanner_featureEnabled_BannerWithAutoOwnedPermalink_AutoPermalinkAdded() {
        steps.featureSteps().addClientFeature(clientId, SHOW_AUTO_ASSIGNED_ORGANIZATIONS, true);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, ownedPermalink), AUTO);

        List<GdiBanner> gdiBanners = getBanners(List.of(bannerId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdiBanners.get(0).getPermalinkId()).isEqualTo(ownedPermalink);
            softly.assertThat(gdiBanners.get(0).getPermalinkAssignType()).isEqualTo(AUTO);
        });
    }

    @Test
    public void addExternalDataToGdiBanner_featureDisabled_BannerWithAutoOwnedPermalink_NoPermalinkAdded() {
        steps.featureSteps().addClientFeature(clientId, SHOW_AUTO_ASSIGNED_ORGANIZATIONS, false);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, ownedPermalink), AUTO);

        List<GdiBanner> gdiBanners = getBanners(List.of(bannerId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdiBanners.get(0).getPermalinkId()).isEqualTo(null);
            softly.assertThat(gdiBanners.get(0).getPermalinkAssignType()).isEqualTo(null);
        });
    }

    /**
     * Вручную привязанный пермалинк возвращается, даже если у клиента больше нет прав на организацию.
     */
    @Test
    public void addExternalDataToGdiBanner_featureEnabled_BannerWithManualNotOwnedPermalink_ManualPermalinkAdded() {
        steps.featureSteps().addClientFeature(clientId, SHOW_AUTO_ASSIGNED_ORGANIZATIONS, true);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, notOwnedPermalink), MANUAL);

        List<GdiBanner> gdiBanners = getBanners(List.of(bannerId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdiBanners.get(0).getPermalinkId()).isEqualTo(notOwnedPermalink);
            softly.assertThat(gdiBanners.get(0).getPermalinkAssignType()).isEqualTo(MANUAL);
        });
    }

    /**
     * Автоматически привязанный пермалинк не возвращается, если на него нет прав.
     */
    @Test
    public void addExternalDataToGdiBanner_featureEnabled_BannerWithAutoNotOwnedPermalink_NoPermalinkAdded() {
        steps.featureSteps().addClientFeature(clientId, SHOW_AUTO_ASSIGNED_ORGANIZATIONS, true);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, notOwnedPermalink), AUTO);

        List<GdiBanner> gdiBanners = getBanners(List.of(bannerId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdiBanners.get(0).getPermalinkId()).isEqualTo(null);
            softly.assertThat(gdiBanners.get(0).getPermalinkAssignType()).isEqualTo(null);
        });
    }

    @Test
    public void addExternalDataToGdiBanner_featureDisabled_BannerWithAutoNotOwnedPermalink_NoPermalinkAdded() {
        steps.featureSteps().addClientFeature(clientId, SHOW_AUTO_ASSIGNED_ORGANIZATIONS, false);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, notOwnedPermalink), AUTO);

        List<GdiBanner> gdiBanners = getBanners(List.of(bannerId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdiBanners.get(0).getPermalinkId()).isEqualTo(null);
            softly.assertThat(gdiBanners.get(0).getPermalinkAssignType()).isEqualTo(null);
        });
    }

    /**
     * Вручную привязанный пермалинк всегда имеет приоритет над автоматически привязанным.
     */
    @Test
    public void addExternalDataToGdiBanner_featureEnabled_BannerWithManualOwnedAndAutoOwnedPermalink_ManualPermalinkAdded() {
        steps.featureSteps().addClientFeature(clientId, SHOW_AUTO_ASSIGNED_ORGANIZATIONS, true);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, ownedPermalink), MANUAL);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, secondOwnedPermalink), AUTO);

        List<GdiBanner> gdiBanners = getBanners(List.of(bannerId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdiBanners.get(0).getPermalinkId()).isEqualTo(ownedPermalink);
            softly.assertThat(gdiBanners.get(0).getPermalinkAssignType()).isEqualTo(MANUAL);
        });
    }

    @Test
    public void addExternalDataToGdiBanner_featureDisabled_BannerWithManualOwnedAndAutoOwnedPermalink_ManualPermalinkAdded() {
        steps.featureSteps().addClientFeature(clientId, SHOW_AUTO_ASSIGNED_ORGANIZATIONS, false);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, ownedPermalink), MANUAL);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, secondOwnedPermalink), AUTO);

        List<GdiBanner> gdiBanners = getBanners(List.of(bannerId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdiBanners.get(0).getPermalinkId()).isEqualTo(ownedPermalink);
            softly.assertThat(gdiBanners.get(0).getPermalinkAssignType()).isEqualTo(MANUAL);
        });
    }

    /**
     * Вручную привязанный пермалинк всегда имеет приоритет над автоматически привязанным.
     */
    @Test
    public void addExternalDataToGdiBanner_featureEnabled_BannerWithManualNotOwnedAndAutoOwnedPermalink_ManualPermalinkAdded() {
        steps.featureSteps().addClientFeature(clientId, SHOW_AUTO_ASSIGNED_ORGANIZATIONS, true);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, notOwnedPermalink), MANUAL);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, secondOwnedPermalink), AUTO);

        List<GdiBanner> gdiBanners = getBanners(List.of(bannerId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdiBanners.get(0).getPermalinkId()).isEqualTo(notOwnedPermalink);
            softly.assertThat(gdiBanners.get(0).getPermalinkAssignType()).isEqualTo(MANUAL);
        });
    }

    @Test
    public void addExternalDataToGdiBanner_featureDisabled_BannerWithManualNotOwnedAndAutoOwnedPermalink_ManualPermalinkAdded() {
        steps.featureSteps().addClientFeature(clientId, SHOW_AUTO_ASSIGNED_ORGANIZATIONS, false);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, notOwnedPermalink), MANUAL);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, secondOwnedPermalink), AUTO);

        List<GdiBanner> gdiBanners = getBanners(List.of(bannerId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdiBanners.get(0).getPermalinkId()).isEqualTo(notOwnedPermalink);
            softly.assertThat(gdiBanners.get(0).getPermalinkAssignType()).isEqualTo(MANUAL);
        });
    }

    /**
     * Автоматически привязанный пермалинк не возвращается, если на него нет прав.
     */
    @Test
    public void addExternalDataToGdiBanner_featureEnabled_BannerWithAutoOwnedAndAutoNotOwnedPermalink_AutoOwnedPermalinkAdded() {
        steps.featureSteps().addClientFeature(clientId, SHOW_AUTO_ASSIGNED_ORGANIZATIONS, true);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, ownedPermalink), AUTO);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, notOwnedPermalink), AUTO);

        List<GdiBanner> gdiBanners = getBanners(List.of(bannerId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdiBanners.get(0).getPermalinkId()).isEqualTo(ownedPermalink);
            softly.assertThat(gdiBanners.get(0).getPermalinkAssignType()).isEqualTo(AUTO);
        });
    }

    /**
     * Если есть несколько автоматически привязанных пермалинков, на которые есть права, то возвращается случайный.
     */
    @Test
    public void addExternalDataToGdiBanner_featureEnabled_BannerWithTwoAutoOwnedPermalinks_SmallestPermalinkAdded() {
        steps.featureSteps().addClientFeature(clientId, SHOW_AUTO_ASSIGNED_ORGANIZATIONS, true);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, ownedPermalink), AUTO);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, secondOwnedPermalink), AUTO);

        List<GdiBanner> gdiBanners = getBanners(List.of(bannerId));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdiBanners.get(0).getPermalinkId())
                    .isEqualTo(Math.min(ownedPermalink, secondOwnedPermalink));
            softly.assertThat(gdiBanners.get(0).getPermalinkAssignType()).isEqualTo(AUTO);
        });
    }

    @Test
    public void addExternalDataToGdiBanner_featureEnabled_SeveralBannersWithPermalinks() {
        steps.featureSteps().addClientFeature(clientId, SHOW_AUTO_ASSIGNED_ORGANIZATIONS, true);
        Long secondBannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();
        Long thirdBannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();
        Long fourthBannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();
        List<Long> bannerIds = List.of(bannerId, secondBannerId, thirdBannerId, fourthBannerId);

        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, ownedPermalink), MANUAL);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(secondBannerId, notOwnedPermalink), MANUAL);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(thirdBannerId, ownedPermalink), AUTO);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(fourthBannerId, notOwnedPermalink), AUTO);

        List<GdiBanner> gdiBanners = getBanners(bannerIds);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gdiBanners.get(0).getPermalinkId()).isEqualTo(ownedPermalink);
            softly.assertThat(gdiBanners.get(0).getPermalinkAssignType()).isEqualTo(MANUAL);

            softly.assertThat(gdiBanners.get(1).getPermalinkId()).isEqualTo(notOwnedPermalink);
            softly.assertThat(gdiBanners.get(1).getPermalinkAssignType()).isEqualTo(MANUAL);

            softly.assertThat(gdiBanners.get(2).getPermalinkId()).isEqualTo(ownedPermalink);
            softly.assertThat(gdiBanners.get(2).getPermalinkAssignType()).isEqualTo(AUTO);

            softly.assertThat(gdiBanners.get(3).getPermalinkId()).isEqualTo(null);
            softly.assertThat(gdiBanners.get(3).getPermalinkAssignType()).isEqualTo(null);
        });
    }

    private List<GdiBanner> getBanners(List<Long> bannerIds) {
        Set<String> features = featureService.getEnabledForClientId(clientId);
        return gridBannerService.getBannersWithoutStats(shard, bannerIds, clientId, features, true);
    }
}
