package ru.yandex.direct.jobs.receiveorganizationstatuschanges;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.altay.direct.Direct;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerModerationRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.tvm.TvmIntegrationStub;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultOrganization;

@JobsTest
@ExtendWith(SpringExtension.class)
class ReceiveOrganizationStatusChangesJobTest {

    @Autowired
    private Steps steps;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private BannerCommonRepository bannerCommonRepository;

    @Autowired
    private BannerModerationRepository bannerModerationRepository;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    private int shard;
    private Long bannerId;
    private Organization organization;

    private ReceiveOrganizationStatusChangesJob job;

    @BeforeEach
    void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        bannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();

        organization = defaultOrganization(clientId);

        job = new ReceiveOrganizationStatusChangesJob(mock(DirectConfig.class), new TvmIntegrationStub(),
                shardHelper, organizationRepository, bannerCommonRepository, bannerModerationRepository);
    }

    @Test
    void getPermalinkIdsByStatusPublish_TwoPermalinkChangesSeveralTimes_OnlyLastChangeTaken() {
        Map<OrganizationStatusPublish, List<Long>> changedStatusPublishByPermalinkId =
                job.getPermalinkIdsByStatusPublish(
                asList(
                        Direct.ChangedPermalinkStatus.newBuilder().setPermalink(1L).setPublish(true).build(),
                        Direct.ChangedPermalinkStatus.newBuilder().setPermalink(2L).setPublish(false).build(),
                        Direct.ChangedPermalinkStatus.newBuilder().setPermalink(1L).setPublish(false).build(),
                        Direct.ChangedPermalinkStatus.newBuilder().setPermalink(2L).setPublish(true).build(),
                        Direct.ChangedPermalinkStatus.newBuilder().setPermalink(2L).setPublish(true).build()));

        assertThat(changedStatusPublishByPermalinkId).isEqualTo(Map.of(
                OrganizationStatusPublish.UNPUBLISHED, List.of(1L),
                OrganizationStatusPublish.PUBLISHED, List.of(2L)));
    }

    @Test
    void processStatusChangeForShard_StatusPublishedNotChanged_NothingChanges() {
        organization.setStatusPublish(OrganizationStatusPublish.UNKNOWN);
        organizationRepository.addOrUpdateAndLinkOrganizations(shard, ImmutableMap.of(bannerId, organization));

        Long permalinkId = organization.getPermalinkId();

        job.processStatusChangeForShard(shard, singletonList(permalinkId), OrganizationStatusPublish.UNKNOWN);

        List<Organization> actualOrganizations = organizationRepository.getOrganizationsByPermalinkIds(shard,
                singletonList(permalinkId)).get(permalinkId);
        assertThat(actualOrganizations)
                .hasSize(1)
                .first()
                .isEqualToIgnoringNullFields(new Organization()
                        .withStatusPublish(OrganizationStatusPublish.UNKNOWN));

        var banners = bannerTypedRepository.getTyped(shard, singletonList(bannerId));
        assertThat(banners)
                .hasSize(1)
                .first()
                .isEqualToIgnoringNullFields(new TextBanner()
                        .withStatusBsSynced(StatusBsSynced.YES)
                        .withStatusModerate(BannerStatusModerate.YES));
    }

    @Test
    void processEvents_StatusPublishedChanged_StatusesUpdated() {
        organization.setStatusPublish(OrganizationStatusPublish.UNKNOWN);
        organizationRepository.addOrUpdateAndLinkOrganizations(shard, ImmutableMap.of(bannerId, organization));

        Long permalinkId = organization.getPermalinkId();

        job.processEvents(singletonList(
                Direct.ChangedPermalinkStatus.newBuilder()
                        .setPermalink(permalinkId)
                        .setPublish(true)
                        .build()));

        List<Organization> actualOrganizations = organizationRepository.getOrganizationsByPermalinkIds(shard,
                singletonList(permalinkId)).get(permalinkId);

        assertThat(actualOrganizations)
                .hasSize(1)
                .first()
                .isEqualToIgnoringNullFields(new Organization()
                        .withStatusPublish(OrganizationStatusPublish.PUBLISHED));

        var banners = bannerTypedRepository.getTyped(shard, singletonList(bannerId));
        assertThat(banners)
                .hasSize(1)
                .first()
                .isEqualToIgnoringNullFields(new TextBanner()
                        .withStatusBsSynced(StatusBsSynced.NO)
                        .withStatusModerate(BannerStatusModerate.READY));
    }

    @Test
    void processEvents_StatusPublishedChanged_StatusOfDraftNotUpdated() {
        bannerModerationRepository.updateStatusModerate(shard, singletonList(bannerId), BannerStatusModerate.NEW);

        organization.setStatusPublish(OrganizationStatusPublish.UNKNOWN);
        organizationRepository.addOrUpdateAndLinkOrganizations(shard, ImmutableMap.of(bannerId, organization));

        Long permalinkId = organization.getPermalinkId();

        job.processEvents(singletonList(
                Direct.ChangedPermalinkStatus.newBuilder()
                        .setPermalink(permalinkId)
                        .setPublish(true)
                        .build()));

        List<Organization> actualOrganizations = organizationRepository.getOrganizationsByPermalinkIds(shard,
                singletonList(permalinkId)).get(permalinkId);

        assertThat(actualOrganizations)
                .hasSize(1)
                .first()
                .isEqualToIgnoringNullFields(new Organization()
                        .withStatusPublish(OrganizationStatusPublish.PUBLISHED));

        var banners = bannerTypedRepository.getTyped(shard, singletonList(bannerId));
        assertThat(banners)
                .hasSize(1)
                .first()
                .isEqualToIgnoringNullFields(new TextBanner()
                        .withStatusBsSynced(StatusBsSynced.NO)
                        .withStatusModerate(BannerStatusModerate.NEW));
    }
}
