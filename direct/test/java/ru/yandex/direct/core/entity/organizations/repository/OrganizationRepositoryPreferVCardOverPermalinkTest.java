package ru.yandex.direct.core.entity.organizations.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.organization.model.BannerPermalink;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.organization.model.PermalinkAssignType.AUTO;
import static ru.yandex.direct.core.entity.organization.model.PermalinkAssignType.MANUAL;
import static ru.yandex.direct.core.testing.data.TestOrganizations.createBannerPermalink;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultOrganization;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault

public class OrganizationRepositoryPreferVCardOverPermalinkTest {

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private Steps steps;

    @Autowired
    private OrganizationRepository repository;

    private int shard;
    private DSLContext dslContext;
    private Long bannerId;
    private Long secondBannerId;
    private Long permalinkId;

    @Before
    public void setUp() throws Exception {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        dslContext = dslContextProvider.ppc(shard);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        bannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();
        secondBannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();
        permalinkId = defaultOrganization(clientId).getPermalinkId();
    }

    @Test
    public void linkOrganization_AutoOrganization_PreferPermalink() {
        repository.linkOrganizationsToBanners(dslContext, Map.of(bannerId, permalinkId), AUTO);

        Map<Long, List<BannerPermalink>> actual =
                repository.getBannerPermalinkByBannerIds(shard, List.of(bannerId));
        BannerPermalink bannerPermalink = createBannerPermalink(permalinkId, AUTO, false, false);
        var expected = Map.of(bannerId, List.of(bannerPermalink));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void linkOrganization_ManualOrganization_PreferPermalink() {
        repository.linkOrganizationsToBanners(dslContext, Map.of(bannerId, permalinkId), MANUAL);

        Map<Long, List<BannerPermalink>> actual =
                repository.getBannerPermalinkByBannerIds(shard, List.of(bannerId));
        BannerPermalink bannerPermalink = createBannerPermalink(permalinkId, MANUAL, false, false);
        var expected = Map.of(bannerId, List.of(bannerPermalink));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void linkOrganization_ManualOrganization_PreferVCard() {
        repository.linkOrganizationsToBanners(dslContext, Map.of(bannerId, permalinkId), Set.of(bannerId), MANUAL);

        Map<Long, List<BannerPermalink>> actual =
                repository.getBannerPermalinkByBannerIds(shard, List.of(bannerId));
        BannerPermalink bannerPermalink = createBannerPermalink(permalinkId, MANUAL, false, true);
        var expected = Map.of(bannerId, List.of(bannerPermalink));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void linkOrganization_TwoManualOrganization_PreferDifferent() {
        repository.linkOrganizationsToBanners(dslContext, Map.of(bannerId, permalinkId, secondBannerId, permalinkId), Set.of(bannerId), MANUAL);

        Map<Long, List<BannerPermalink>> actual =
                repository.getBannerPermalinkByBannerIds(shard, List.of(bannerId, secondBannerId));
        BannerPermalink bannerPermalink = createBannerPermalink(permalinkId, MANUAL, false, true);
        var expected = Map.of(
                bannerId, List.of(createBannerPermalink(permalinkId, MANUAL, false, true)),
                secondBannerId, List.of(createBannerPermalink(permalinkId, MANUAL, false, false))
        );
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void updateOrganizations_ToPreferVCard() {
        repository.linkOrganizationsToBanners(dslContext, Map.of(bannerId, permalinkId), MANUAL);
        repository.updateOrganizationsToBannerBinds(dslContext, Map.of(bannerId, true));
        Map<Long, List<BannerPermalink>> actual =
                repository.getBannerPermalinkByBannerIds(shard, List.of(bannerId));
        BannerPermalink bannerPermalink = createBannerPermalink(permalinkId, MANUAL, false, true);
        var expected = Map.of(bannerId, List.of(bannerPermalink));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void updateOrganizations_ToPreferPermalink() {
        repository.linkOrganizationsToBanners(dslContext, Map.of(bannerId, permalinkId), Set.of(bannerId), MANUAL);
        repository.updateOrganizationsToBannerBinds(dslContext, Map.of(bannerId, false));
        Map<Long, List<BannerPermalink>> actual =
                repository.getBannerPermalinkByBannerIds(shard, List.of(bannerId));
        BannerPermalink bannerPermalink = createBannerPermalink(permalinkId, MANUAL, false, false);
        var expected = Map.of(bannerId, List.of(bannerPermalink));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void updateTwoOrganizations_PreferDifferent() {
        repository.linkOrganizationsToBanners(dslContext, Map.of(bannerId, permalinkId, secondBannerId, permalinkId), Set.of(bannerId), MANUAL);
        repository.updateOrganizationsToBannerBinds(dslContext, Map.of(bannerId, false, secondBannerId, true));

        Map<Long, List<BannerPermalink>> actual =
                repository.getBannerPermalinkByBannerIds(shard, List.of(bannerId, secondBannerId));
        var expected = Map.of(
                bannerId, List.of(createBannerPermalink(permalinkId, MANUAL, false, false)),
                secondBannerId, List.of(createBannerPermalink(permalinkId, MANUAL, false, true))
        );
        assertThat(actual).isEqualTo(expected);
    }
}
