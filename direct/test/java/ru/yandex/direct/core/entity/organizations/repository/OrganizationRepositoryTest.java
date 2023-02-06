package ru.yandex.direct.core.entity.organizations.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository;
import ru.yandex.direct.core.entity.organization.model.BannerPermalink;
import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannerPermalinksPermalinkAssignType;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerPermalinksRecord;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish.PUBLISHED;
import static ru.yandex.direct.core.entity.organization.model.OrganizationStatusPublish.UNKNOWN;
import static ru.yandex.direct.core.entity.organization.model.PermalinkAssignType.AUTO;
import static ru.yandex.direct.core.entity.organization.model.PermalinkAssignType.MANUAL;
import static ru.yandex.direct.core.testing.data.TestOrganizations.copyOrganization;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultOrganization;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class OrganizationRepositoryTest {

    @Autowired
    private Steps steps;

    @Autowired
    private OrganizationRepository repository;

    @Autowired
    private ClientPhoneRepository clientPhoneRepository;

    private int shard;
    private Long bannerId;
    private ClientId clientId;
    private ClientInfo clientInfo;
    private Organization organization;
    private AdGroupInfo adGroupInfo;

    @Before
    public void setUp() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();

        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        bannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();
        organization = defaultOrganization(clientId);
    }

    @Test
    public void addOneOrganizationTest() {
        repository.addOrUpdateOrganizations(shard, singleton(organization));
        List<Organization> organizations = repository.getAllClientOrganizations(shard, clientId);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.get(0), is(organization));
    }

    @Test
    public void addOneOrganizationWithoutStatusPublishTest() {
        repository.addOrUpdateOrganizations(shard, singleton(organization.withStatusPublish(null)));
        List<Organization> organizations = repository.getAllClientOrganizations(shard, clientId);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.get(0), is(organization.withStatusPublish(UNKNOWN)));
    }

    @Test
    public void addAndUpdateOneOrganizationTest() {
        repository.addOrUpdateOrganizations(shard, singleton(organization));
        repository.addOrUpdateOrganizations(shard, singleton(organization.withChainId(null)));

        List<Organization> organizations = repository.getAllClientOrganizations(shard, clientId);
        assertThat(organizations.size(), is(1));
        assertThat(organizations.get(0), is(organization));
        assertThat(organizations.get(0).getChainId(), nullValue());
    }

    @Test
    public void addAndLinkOneOrganizationTest() {
        repository.addOrUpdateAndLinkOrganizations(shard, ImmutableMap.of(bannerId, organization));
        Map<Long, Organization> organizations = repository.getOrganizationsByBannerIds(shard, clientId, singleton(bannerId));
        assertThat(organizations.size(), is(1));
        assertThat(organizations.get(bannerId), is(organization));
    }

    @Test
    public void addAndLinkTwoBannersToOneOrganizationTest() {
        AdGroupInfo anotherAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        Long anotherBannerId = steps.bannerSteps().createActiveTextBanner(anotherAdGroupInfo).getBannerId();
        repository.addOrUpdateAndLinkOrganizations(shard, Map.of(bannerId, organization, anotherBannerId,
                organization));
        Map<Long, Organization> organizations = repository.getOrganizationsByBannerIds(shard, clientId,
                asList(bannerId, anotherBannerId));
        assertThat(organizations.size(), is(2));
        assertThat(organizations.get(bannerId), is(organization));
        assertThat(organizations.get(anotherBannerId), is(organization));
    }

    @Test
    public void addTwoOrganizationsTest() {
        Organization anotherOrganization = defaultOrganization(clientId);
        repository.addOrUpdateOrganizations(shard, asList(organization, anotherOrganization));
        List<Organization> organizations = repository.getAllClientOrganizations(shard, clientId);
        assertThat(organizations.size(), is(2));
        assertThat(organizations, containsInAnyOrder(organization, anotherOrganization));
    }

    @Test
    public void addAndLinkTwoOrganizationsTest() {
        Organization anotherOrganization = defaultOrganization(clientId);
        AdGroupInfo anotherAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        Long anotherBannerId = steps.bannerSteps().createActiveTextBanner(anotherAdGroupInfo).getBannerId();

        repository.addOrUpdateAndLinkOrganizations(shard,
                Map.of(bannerId, organization, anotherBannerId, anotherOrganization));

        Map<Long, Organization> organizations = repository.getOrganizationsByBannerIds(shard, clientId, singleton(bannerId));
        assertThat(organizations.size(), is(1));
        assertThat(organizations.get(bannerId), is(organization));

        organizations = repository.getOrganizationsByBannerIds(shard, clientId, asList(bannerId, anotherBannerId));
        assertThat(organizations.size(), is(2));
        assertThat(organizations.values(), containsInAnyOrder(organization, anotherOrganization));
    }

    @Test
    public void linkToBannersAndGetByAdGroupIdTest() {
        repository.addOrUpdateOrganizations(shard, singleton(organization));
        repository.linkOrganizationsToBanners(shard, ImmutableMap.of(bannerId, organization.getPermalinkId()));
        Map<Long, Organization> orgsByAdGroupId = repository.getOrganizationsByBannerIds(shard, clientId, singleton(bannerId));
        assertThat(orgsByAdGroupId.size(), is(1));
        assertThat(orgsByAdGroupId.get(bannerId), is(organization));
    }

    @Test
    public void getBannerIdsByPhoneId() {
        repository.addOrUpdateOrganizations(shard, singleton(organization));

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        Long anotherBannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();

        Long phoneId = 987L;
        repository.linkOrganizationsToBanners(shard, ImmutableMap.of(bannerId, organization.getPermalinkId(),
                anotherBannerId, organization.getPermalinkId()));
        steps.clientPhoneSteps().linkPhoneIdToBanner(shard, bannerId, phoneId);
        steps.clientPhoneSteps().linkPhoneIdToBanner(shard, anotherBannerId, phoneId);
        Map<Long, List<Long>> bannerIdsByPhoneId =
                clientPhoneRepository.getBannerIdsByPhoneId(shard, List.of(phoneId));
        Map<Long, Long> bannerPhonesByBids =
                clientPhoneRepository.getPhoneIdsByBannerIds(shard, List.of(bannerId, anotherBannerId));
        assertThat(bannerIdsByPhoneId.get(phoneId), hasSize(2));
        assertThat(bannerIdsByPhoneId.get(phoneId).get(0), equalTo(bannerId));
        assertThat(bannerIdsByPhoneId.get(phoneId).get(1), equalTo(anotherBannerId));
        assertThat(bannerPhonesByBids.get(bannerId), equalTo(phoneId));
        assertThat(bannerPhonesByBids.get(anotherBannerId), equalTo(phoneId));
    }

    @Test
    public void updateStatusPublishTest() {
        repository.addOrUpdateOrganizations(shard, singleton(organization));
        Organization organization = StreamEx.of(repository.getAllClientOrganizations(shard, clientId))
                .findFirst(org -> org.getPermalinkId().equals(this.organization.getPermalinkId())).get();
        assertThat(organization.getStatusPublish(), is(UNKNOWN));
        repository.updateOrganizationsStatusPublishByPermalinkIds(shard, singleton(organization.getPermalinkId()), PUBLISHED);
        organization = StreamEx.of(repository.getAllClientOrganizations(shard, clientId))
                .findFirst(org -> org.getPermalinkId().equals(this.organization.getPermalinkId())).get();
        assertThat(organization.getStatusPublish(), is(PUBLISHED));
    }

    @Test
    public void getLinkedBannerIdsByClientIdPermalinkIdsTest() {
        repository.addOrUpdateOrganizations(shard, singleton(organization));
        repository.linkOrganizationsToBanners(shard, ImmutableMap.of(bannerId, organization.getPermalinkId()));
        List<Long> linkedBannerIds =
                repository.getLinkedBannerIdsByClientIdPermalinkIds(shard, singletonList(Pair.of(clientId, organization.getPermalinkId())));
        assertThat(linkedBannerIds, is(singletonList(bannerId)));
    }

    @Test
    public void getLinkedBannerIdsByClientIdPermalinkIdsMultipleTest_ReturnAllBanners() {
        repository.addOrUpdateOrganizations(shard, singleton(organization));
        repository.linkOrganizationsToBanners(shard, ImmutableMap.of(bannerId, organization.getPermalinkId()));

        ClientInfo anotherClientInfo = steps.clientSteps().createDefaultClient();
        int anotherClientShard = anotherClientInfo.getShard();
        ClientId anotherClientId = anotherClientInfo.getClientId();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(anotherClientInfo);
        Long anotherBannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();
        Organization anotherOrganization = defaultOrganization(anotherClientId);

        repository.addOrUpdateOrganizations(anotherClientShard, singleton(anotherOrganization));
        repository.linkOrganizationsToBanners(anotherClientShard, ImmutableMap.of(anotherBannerId, anotherOrganization.getPermalinkId()));

        assumeThat(shard, is(anotherClientShard));

        List<Long> linkedBannerIds =
                repository.getLinkedBannerIdsByClientIdPermalinkIds(shard,
                        asList(Pair.of(clientId, organization.getPermalinkId()),
                                Pair.of(anotherClientId, anotherOrganization.getPermalinkId())));

        assertThat(linkedBannerIds, containsInAnyOrder(bannerId, anotherBannerId));
    }

    @Test
    public void getLinkedBannerIdsByClientIdPermalinkIdsMultipleTest_ReturnOnlyOneBanner() {
        repository.addOrUpdateOrganizations(shard, singleton(organization));
        repository.linkOrganizationsToBanners(shard, ImmutableMap.of(bannerId, organization.getPermalinkId()));

        ClientInfo anotherClientInfo = steps.clientSteps().createDefaultClient();
        int anotherClientShard = anotherClientInfo.getShard();
        ClientId anotherClientId = anotherClientInfo.getClientId();
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(anotherClientInfo);
        Long anotherBannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();
        Organization anotherOrganization = defaultOrganization(anotherClientId);

        repository.addOrUpdateOrganizations(anotherClientShard, singleton(anotherOrganization));
        repository.linkOrganizationsToBanners(anotherClientShard, ImmutableMap.of(anotherBannerId, anotherOrganization.getPermalinkId()));

        assumeThat(shard, is(anotherClientShard));

        List<Long> linkedBannerIds =
                repository.getLinkedBannerIdsByClientIdPermalinkIds(shard,
                        singletonList(Pair.of(clientId, organization.getPermalinkId())));

        assertThat(linkedBannerIds, is(singletonList(bannerId)));
    }

    @Test
    public void getLinkedBannerIdsByClientIdPermalinkIds_OrganizationNotLinked() {
        repository.addOrUpdateOrganizations(shard, singleton(organization));
        List<Long> linkedBannerIds =
                repository.getLinkedBannerIdsByClientIdPermalinkIds(shard, singletonList(Pair.of(clientId, organization.getPermalinkId())));
        assertThat(linkedBannerIds, is(emptyList()));
    }

    @Test
    public void getLinkedBannerIdsByClientIdPermalinkIds_OrganizationNotExists() {
        repository.linkOrganizationsToBanners(shard, ImmutableMap.of(bannerId, organization.getPermalinkId()));
        List<Long> linkedBannerIds =
                repository.getLinkedBannerIdsByClientIdPermalinkIds(shard, singletonList(Pair.of(clientId, organization.getPermalinkId())));
        assertThat(linkedBannerIds, is(emptyList()));
    }

    @Test
    public void getOrganizationsByPermalinkIdsMultipleTest() {
        Organization organization = defaultOrganization(ClientId.fromLong(1L));
        Organization anotherOrganization = copyOrganization(organization).withClientId(ClientId.fromLong(2L));
        repository.addOrUpdateOrganizations(shard, asList(organization, anotherOrganization));

        Map<Long, List<Organization>> organizationsByPermalinkId =
                repository.getOrganizationsByPermalinkIds(shard, singletonList(organization.getPermalinkId()));
        assertThat(organizationsByPermalinkId.get(organization.getPermalinkId()),
                containsInAnyOrder(organization, anotherOrganization));
    }

    @Test
    public void getOrganizationsByBannerIdsOneOrgTwoClientsTest() {
        TextBannerInfo banner = steps.bannerSteps().createActiveTextBanner();
        ClientId clientId = banner.getClientId();
        ClientId someOtherClientId = ClientId.fromLong(clientId.asLong() + 1);
        Long bannerId = banner.getBannerId();

        Organization organization = defaultOrganization(clientId);
        Organization anotherOrganization = copyOrganization(organization).withClientId(someOtherClientId);
        repository.addOrUpdateOrganizations(shard, asList(organization, anotherOrganization));
        repository.linkOrganizationsToBanners(shard, Map.of(bannerId, organization.getPermalinkId()));

        Map<Long, Organization> organizationsByBannerIds =
                repository.getOrganizationsByBannerIds(shard, singletonList(bannerId));
        assertThat(organizationsByBannerIds.get(bannerId), is(organization));
    }

    @Test
    public void getOrganizationsByBannerIdsSamePermalinkAutoAndManualTest() {
        TextBannerInfo banner = steps.bannerSteps().createActiveTextBanner();
        ClientId clientId = banner.getClientId();
        ClientId someOtherClientId = ClientId.fromLong(clientId.asLong() + 1);
        Long bannerId = banner.getBannerId();

        Organization organization = defaultOrganization(clientId);
        Organization anotherOrganization = copyOrganization(organization).withClientId(someOtherClientId);
        repository.addOrUpdateOrganizations(shard, asList(organization, anotherOrganization));
        repository.linkOrganizationsToBanners(shard, Map.of(bannerId, organization.getPermalinkId()));
        repository.linkOrganizationsToBanners(shard, Map.of(bannerId, organization.getPermalinkId()), AUTO);

        Map<Long, Organization> organizationsByBannerIds =
                repository.getOrganizationsByBannerIds(shard, singletonList(bannerId));
        assertThat(organizationsByBannerIds.get(bannerId), is(organization));
    }

    @Test
    public void getPermalinkAssignTypeByBannerIds_OneBanner_ManualPermalink() {
        repository.linkOrganizationsToBanners(shard, Map.of(bannerId, organization.getPermalinkId()), MANUAL);
        var permalinkAssignTypesByBannerId = repository.getBannerPermalinkByBannerIds(shard, List.of(bannerId));

        BannerPermalink organizationManual = new BannerPermalink()
                .withPermalinkId(organization.getPermalinkId())
                .withPermalinkAssignType(MANUAL)
                .withIsChangeToManualRejected(false)
                .withPreferVCardOverPermalink(false);
        var expectedResult = Map.of(bannerId, List.of(organizationManual));

        Assertions.assertThat(permalinkAssignTypesByBannerId).isEqualTo(expectedResult);
    }

    @Test
    public void getPermalinkAssignTypeByBannerIds_OneBanner_AutoPermalink() {
        repository.linkOrganizationsToBanners(shard, Map.of(bannerId, organization.getPermalinkId()), AUTO);
        var permalinkAssignTypesByBannerId = repository.getBannerPermalinkByBannerIds(shard, List.of(bannerId));

        BannerPermalink organizationAuto = new BannerPermalink()
                .withPermalinkId(organization.getPermalinkId())
                .withPermalinkAssignType(AUTO)
                .withIsChangeToManualRejected(false)
                .withPreferVCardOverPermalink(false);
        var expectedResult = Map.of(bannerId, List.of(organizationAuto));

        Assertions.assertThat(permalinkAssignTypesByBannerId).isEqualTo(expectedResult);
    }

    @Test
    public void getPermalinkAssignTypeByBannerIds_OneBanner_SeveralPermalinks() {
        Organization secondOrganization = defaultOrganization(clientId);
        repository.linkOrganizationsToBanners(shard, Map.of(bannerId, organization.getPermalinkId()), MANUAL);
        repository.linkOrganizationsToBanners(shard, Map.of(bannerId, secondOrganization.getPermalinkId()), AUTO);
        var permalinkAssignTypesByBannerId = repository.getBannerPermalinkByBannerIds(shard, List.of(bannerId));

        BannerPermalink organizationManual = new BannerPermalink()
                .withPermalinkId(organization.getPermalinkId())
                .withPermalinkAssignType(MANUAL)
                .withIsChangeToManualRejected(false)
                .withPreferVCardOverPermalink(false);
        BannerPermalink secondOrganizationAuto = new BannerPermalink()
                .withPermalinkId(secondOrganization.getPermalinkId())
                .withPermalinkAssignType(AUTO)
                .withIsChangeToManualRejected(false)
                .withPreferVCardOverPermalink(false);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(permalinkAssignTypesByBannerId.keySet()).isEqualTo(Set.of(bannerId));
            softly.assertThat(permalinkAssignTypesByBannerId.get(bannerId))
                    .containsExactlyInAnyOrder(organizationManual, secondOrganizationAuto);
        });
    }

    @Test
    public void getPermalinkAssignTypeByBannerIds_SeveralBanners_SeveralPermalinks() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        Long secondBannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();
        Long thirdBannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();
        Organization secondOrganization = defaultOrganization(clientId);
        repository.linkOrganizationsToBanners(
                shard,
                Map.of(
                        bannerId, organization.getPermalinkId(),
                        secondBannerId, organization.getPermalinkId()),
                MANUAL);
        repository.linkOrganizationsToBanners(
                shard,
                Map.of(
                        bannerId, secondOrganization.getPermalinkId(),
                        thirdBannerId, organization.getPermalinkId()),
                AUTO);
        var permalinkAssignTypesByBannerId = repository.getBannerPermalinkByBannerIds(
                shard, List.of(bannerId, secondBannerId, thirdBannerId));

        BannerPermalink organizationManual = new BannerPermalink()
                .withPermalinkId(organization.getPermalinkId())
                .withPermalinkAssignType(MANUAL)
                .withIsChangeToManualRejected(false)
                .withPreferVCardOverPermalink(false);
        BannerPermalink secondOrganizationAuto = new BannerPermalink()
                .withPermalinkId(secondOrganization.getPermalinkId())
                .withPermalinkAssignType(AUTO)
                .withIsChangeToManualRejected(false)
                .withPreferVCardOverPermalink(false);
        BannerPermalink organizationAuto = new BannerPermalink()
                .withPermalinkId(organization.getPermalinkId())
                .withPermalinkAssignType(AUTO)
                .withIsChangeToManualRejected(false)
                .withPreferVCardOverPermalink(false);
        var expectedKeySet = Set.of(bannerId, secondBannerId, thirdBannerId);

        SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(permalinkAssignTypesByBannerId.keySet()).isEqualTo(expectedKeySet);
                softly.assertThat(permalinkAssignTypesByBannerId.get(bannerId))
                        .containsExactlyInAnyOrder(organizationManual, secondOrganizationAuto);
                softly.assertThat(permalinkAssignTypesByBannerId.get(secondBannerId)).containsExactly(organizationManual);
                softly.assertThat(permalinkAssignTypesByBannerId.get(thirdBannerId)).containsExactly(organizationAuto);
        });
    }

    @Test
    public void addRecords_oneRecordAdded() {
        var record = new BannerPermalinksRecord();
        record.setBid(bannerId);
        record.setPermalinkAssignType(BannerPermalinksPermalinkAssignType.auto);
        record.setPermalink(organization.getPermalinkId());
        record.setChainId(0L);

        int inserted = repository.addRecords(shard, List.of(record));
        assertThat(inserted, is(1));
    }

    @Test
    public void addRecords_twoRecordsAdded() {
        var record = new BannerPermalinksRecord();
        record.setBid(bannerId);
        record.setPermalinkAssignType(BannerPermalinksPermalinkAssignType.auto);
        record.setPermalink(organization.getPermalinkId());
        record.setChainId(0L);
        var anotherRecord = new BannerPermalinksRecord();
        anotherRecord.setBid(bannerId);
        anotherRecord.setPermalinkAssignType(BannerPermalinksPermalinkAssignType.auto);
        anotherRecord.setPermalink(0L);
        anotherRecord.setChainId(organization.getPermalinkId());

        int inserted = repository.addRecords(shard, List.of(record, anotherRecord));
        assertThat(inserted, is(2));
    }

    @Test
    public void addRecords_twoDuplicateRecordsAdded() {
        var record = new BannerPermalinksRecord();
        record.setBid(bannerId);
        record.setPermalinkAssignType(BannerPermalinksPermalinkAssignType.auto);
        record.setPermalink(organization.getPermalinkId());
        record.setChainId(0L);
        var anotherRecord = new BannerPermalinksRecord();
        anotherRecord.setBid(bannerId);
        anotherRecord.setPermalinkAssignType(BannerPermalinksPermalinkAssignType.auto);
        anotherRecord.setPermalink(organization.getPermalinkId());
        anotherRecord.setChainId(0L);

        int inserted = repository.addRecords(shard, List.of(record, anotherRecord));
        assertThat(inserted, is(1));
    }

    @Test
    public void deleteRecords_deleteTwoRecords() {
        Long secondBannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();
        Long permalink = organization.getPermalinkId();
        Long secondPermalink = defaultOrganization(clientId).getPermalinkId();
        repository.linkOrganizationsToBanners(shard, Map.of(bannerId, permalink, secondBannerId, secondPermalink), AUTO);

        var record = new BannerPermalinksRecord();
        record.setBid(bannerId);
        record.setPermalinkAssignType(BannerPermalinksPermalinkAssignType.auto);
        record.setPermalink(permalink);
        record.setChainId(0L);
        var anotherRecord = new BannerPermalinksRecord();
        anotherRecord.setBid(secondBannerId);
        anotherRecord.setPermalinkAssignType(BannerPermalinksPermalinkAssignType.auto);
        anotherRecord.setPermalink(secondPermalink);
        anotherRecord.setChainId(0L);

        int deleted = repository.deleteRecords(shard, List.of(record, anotherRecord));
        assertThat(deleted, is(2));
    }

    @Test
    public void deleteRecords_deleteTwoRecords_oneNotFound() {
        Long secondBannerId = steps.bannerSteps().createActiveTextBanner(adGroupInfo).getBannerId();
        Long permalink = organization.getPermalinkId();
        repository.linkOrganizationsToBanners(shard, Map.of(bannerId, permalink, secondBannerId, permalink), AUTO);

        var record = new BannerPermalinksRecord();
        record.setBid(bannerId);
        record.setPermalinkAssignType(BannerPermalinksPermalinkAssignType.auto);
        record.setPermalink(permalink);
        record.setChainId(0L);
        var anotherRecord = new BannerPermalinksRecord();
        anotherRecord.setBid(secondBannerId);
        anotherRecord.setPermalinkAssignType(BannerPermalinksPermalinkAssignType.auto);
        anotherRecord.setPermalink(defaultOrganization(clientId).getPermalinkId());
        anotherRecord.setChainId(0L);

        int deleted = repository.deleteRecords(shard, List.of(record, anotherRecord));
        assertThat(deleted, is(1));
    }
}
