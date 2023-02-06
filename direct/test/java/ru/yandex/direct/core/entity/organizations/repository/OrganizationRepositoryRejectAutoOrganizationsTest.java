package ru.yandex.direct.core.entity.organizations.repository;

import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.organization.model.BannerPermalink;
import ru.yandex.direct.core.entity.organization.model.PermalinkAssignType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.organization.model.PermalinkAssignType.AUTO;
import static ru.yandex.direct.core.entity.organization.model.PermalinkAssignType.MANUAL;
import static ru.yandex.direct.core.testing.data.TestOrganizations.createBannerPermalink;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultOrganization;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class OrganizationRepositoryRejectAutoOrganizationsTest {

    @Autowired
    private Steps steps;

    @Autowired
    private OrganizationRepository repository;

    private int shard;
    private Long bannerId;
    private Long permalinkId;
    private Long secondPermalinkId;

    @Before
    public void setUp() throws Exception {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        bannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();

        permalinkId = defaultOrganization(clientId).getPermalinkId();
        secondPermalinkId = defaultOrganization(clientId).getPermalinkId();
    }

    @Test
    public void rejectAutoOrganizations_AutoOrganization_Rejected() {
        PermalinkAssignType assignType = AUTO;

        repository.linkOrganizationsToBanners(shard, Map.of(bannerId, permalinkId), assignType);
        repository.rejectAutoOrganizations(shard, Map.of(bannerId, permalinkId));

        Map<Long, List<BannerPermalink>> actual =
                repository.getBannerPermalinkByBannerIds(shard, List.of(bannerId));
        BannerPermalink bannerPermalink = createBannerPermalink(permalinkId, assignType, true);
        var expected = Map.of(bannerId, List.of(bannerPermalink));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void rejectAutoOrganizations_ManualOrganization_NotRejected() {
        PermalinkAssignType assignType = MANUAL;

        repository.linkOrganizationsToBanners(shard, Map.of(bannerId, permalinkId), assignType);
        repository.rejectAutoOrganizations(shard, Map.of(bannerId, permalinkId));

        Map<Long, List<BannerPermalink>> actual =
                repository.getBannerPermalinkByBannerIds(shard, List.of(bannerId));
        BannerPermalink bannerPermalink = createBannerPermalink(permalinkId, assignType, false);
        var expected = Map.of(bannerId, List.of(bannerPermalink));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void rejectAutoOrganizations_AutoOrganization_InvalidPermalink_NotRejected() {
        PermalinkAssignType assignType = MANUAL;
        Long invalidPermalink = permalinkId + 100;

        repository.linkOrganizationsToBanners(shard, Map.of(bannerId, permalinkId), assignType);
        repository.rejectAutoOrganizations(shard, Map.of(bannerId, invalidPermalink));

        Map<Long, List<BannerPermalink>> actual =
                repository.getBannerPermalinkByBannerIds(shard, List.of(bannerId));
        BannerPermalink bannerPermalink = createBannerPermalink(permalinkId, assignType, false);
        var expected = Map.of(bannerId, List.of(bannerPermalink));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void rejectAutoOrganizations_AutoOrganization_InvalidBannerId_NotRejected() {
        PermalinkAssignType assignType = MANUAL;
        Long invalidBannerId = bannerId + 100;

        repository.linkOrganizationsToBanners(shard, Map.of(bannerId, permalinkId), assignType);
        repository.rejectAutoOrganizations(shard, Map.of(invalidBannerId, permalinkId));

        Map<Long, List<BannerPermalink>> actual =
                repository.getBannerPermalinkByBannerIds(shard, List.of(bannerId));
        BannerPermalink bannerPermalink = createBannerPermalink(permalinkId, assignType, false);
        var expected = Map.of(bannerId, List.of(bannerPermalink));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void rejectAutoOrganizations_TwoAutoOrganizations_OneRejected() {
        PermalinkAssignType assignType = AUTO;

        repository.linkOrganizationsToBanners(shard, Map.of(bannerId, permalinkId), assignType);
        repository.linkOrganizationsToBanners(shard, Map.of(bannerId, secondPermalinkId), assignType);
        repository.rejectAutoOrganizations(shard, Map.of(bannerId, permalinkId));

        Map<Long, List<BannerPermalink>> actual =
                repository.getBannerPermalinkByBannerIds(shard, List.of(bannerId));
        var expected = createBannerPermalink(permalinkId, assignType, true);
        var secondExpected = createBannerPermalink(secondPermalinkId, assignType, false);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual).containsOnlyKeys(bannerId);
            softly.assertThat(actual.get(bannerId)).containsExactlyInAnyOrder(expected, secondExpected);
        });
    }
}
