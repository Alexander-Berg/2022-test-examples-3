package ru.yandex.direct.core.testing.steps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish;
import ru.yandex.direct.core.entity.organization.model.PermalinkAssignType;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestOrganizationRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.organizations.swagger.OrganizationsClient;
import ru.yandex.direct.organizations.swagger.model.PubApiCompaniesData;
import ru.yandex.direct.organizations.swagger.model.PubApiCompany;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.organizations.swagger.model.PublishingStatus.PUBLISH;

@ParametersAreNonnullByDefault
public class OrganizationsSteps {
    private final OrganizationRepository organizationRepository;
    private final TestOrganizationRepository testOrganizationRepository;
    private final ShardHelper shardHelper;

    @Autowired
    public OrganizationsSteps(OrganizationRepository organizationRepository,
                              TestOrganizationRepository testOrganizationRepository,
                              ShardHelper shardHelper) {
        this.organizationRepository = organizationRepository;
        this.testOrganizationRepository = testOrganizationRepository;
        this.shardHelper = shardHelper;
    }

    public Organization createClientOrganization(ClientInfo clientInfo) {
        return createClientOrganization(clientInfo.getClientId(), RandomNumberUtils.nextPositiveLong());
    }

    public Organization createClientOrganization(ClientId clientId, long permalinkId) {
        return createClientOrganization(clientId, permalinkId, null);
    }

    public Organization createClientOrganization(ClientId clientId, long permalinkId,
                                                 @Nullable OrganizationStatusPublish statusPublish) {
        int shard = shardHelper.getShardByClientId(clientId);
        Organization organization = new Organization()
                .withClientId(clientId)
                .withPermalinkId(permalinkId)
                .withStatusPublish(statusPublish);
        organizationRepository.addOrUpdateOrganizations(shard, List.of(organization));
        return organization;
    }

    public void linkOrganizationToBanner(ClientId clientId, long permalinkId, long bannerId) {
        int shard = shardHelper.getShardByClientId(clientId);
        organizationRepository.linkOrganizationsToBanners(shard, Map.of(bannerId, permalinkId), PermalinkAssignType.MANUAL);
    }

    public void linkDefaultOrganizationToCampaign(ClientId clientId, long permalinkId, long campaignId) {
        int shard = shardHelper.getShardByClientId(clientId);
        testOrganizationRepository.linkDefaultOrganizationToCampaign(shard, permalinkId, campaignId);
    }

    public static void mockOrganizationsClient(OrganizationsClient organizationsClient, List<Long> permalinkIds) {
        if (!MockUtil.isMock(organizationsClient)) {
            throw new IllegalArgumentException("OrganizationsClient should be a mock");
        }

        reset(organizationsClient);
        var companies = StreamEx.of(permalinkIds)
                .map(permalink -> new PubApiCompany()
                        .id(permalink)
                        .publishingStatus(PUBLISH))
                .toList();
        PubApiCompaniesData mockResult = new PubApiCompaniesData();
        mockResult.setCompanies(companies);
        when(organizationsClient.getMultipleOrganizationsInfo(anyCollection(), anyCollection(),
                anyString(), nullable(String.class))
        ).thenReturn(mockResult);
        when(organizationsClient.getMultiplePublishedOrganizationsInfo(anyCollection(), anyCollection(),
                anyString(), nullable(String.class))
        ).thenReturn(mockResult);
    }

    public static void mockOrganizationsClient(OrganizationsClient organizationsClient, Collection<Long> permalinkIds,
                                               Collection<Long> uids, List<PubApiCompany> result) {
        if (!MockUtil.isMock(organizationsClient)) {
            throw new IllegalArgumentException("OrganizationsClient should be a mock");
        }
        PubApiCompaniesData mockResult = new PubApiCompaniesData();
        mockResult.setCompanies(new ArrayList<>(result));
        if (!permalinkIds.isEmpty()) {
            when(organizationsClient.getMultipleOrganizationsInfo(eq(permalinkIds),
                    anyCollection(), anyString(), anyString()))
                    .thenReturn(mockResult);
            when(organizationsClient.getMultiplePublishedOrganizationsInfo(eq(permalinkIds),
                    anyCollection(), anyString(), anyString()))
                    .thenReturn(mockResult);
        }
        if (!uids.isEmpty()) {
            when(organizationsClient.getMultipleOwnOrganizationsInfo(eq(uids),
                    anyCollection(), anyString(), anyString()))
                    .thenReturn(mockResult);
        }
    }
}
