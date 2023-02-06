package ru.yandex.direct.core.entity.clientphone;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneMapping;
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository;
import ru.yandex.direct.core.entity.metrika.container.CounterIdWithDomain;
import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.core.entity.organization.model.PermalinkAssignType;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhoneType;
import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestOrganizations;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.organizations.swagger.model.CompanyPhone;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.telephony.client.TelephonyClient;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.telephony.backend.lib.proto.telephony_platform.ServiceNumber;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.clientphone.ClientPhoneTestUtils.getUniqPhone;
import static ru.yandex.direct.telephony.client.ProtobufMapper.CLIENT_ID_META_KEY;
import static ru.yandex.direct.telephony.client.ProtobufMapper.COUNTER_ID_META_KEY;
import static ru.yandex.direct.telephony.client.ProtobufMapper.ORG_ID_META_KEY;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.filterAndMapList;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedObject;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientPhoneServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private ClientPhoneRepository clientPhoneRepository;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ClientPhoneService clientPhoneService;

    @Autowired
    private ClientPhoneReplaceService clientPhoneReplaceService;

    @Autowired
    OrganizationsClientStub organizationClient;

    @Autowired
    private TelephonyClient telephonyClient;

    private ClientInfo clientInfo;
    private Long operatorUid;
    private ClientId clientId;
    private int shard;
    private Organization organization;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        operatorUid = clientInfo.getUid();
        clientId = clientInfo.getClientId();
        steps.featureSteps().setCurrentClient(clientId);
        shard = clientInfo.getShard();
        organization = TestOrganizations.defaultOrganization(clientId);
        organizationClient.addUidsByPermalinkId(organization.getPermalinkId(), List.of(clientInfo.getUid()));
    }

    @Test
    public void update() {
        ClientPhone clientPhone = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId);
        Long clientPhoneId = clientPhone.getId();
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        steps.bannerSteps().createDefaultBanner(adGroup);
        TextBannerInfo banner = steps.bannerSteps().createDefaultBanner(adGroup);
        Long bannerId = banner.getBannerId();

        addOrganizationAndPhoneToBanner(bannerId, clientPhoneId);

        ClientPhone updatedPhone = steps.clientPhoneSteps().defaultClientManualPhone(clientId)
                .withId(clientPhoneId)
                .withComment("new comment");

        clientPhoneService.updateClientPhone(updatedPhone);

        Map<Long, ClientPhone> clientPhones = listToMap(clientPhoneRepository.getAllClientPhones(clientId,
                emptyList()),
                ClientPhone::getId);
        Map<Long, OldBanner> banners = listToMap(bannerRepository.getBanners(shard, List.of(bannerId)),
                OldBanner::getId);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(clientPhones.get(clientPhoneId).getComment()).isEqualTo(updatedPhone.getComment());
            sa.assertThat(banners.get(bannerId).getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
        });
    }

    @Test
    public void addManualClientPhone() {
        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(new PhoneNumber()
                        .withPhone("+71111111111")
                        .withExtension(9L))
                .withComment("comment")
                .withIsDeleted(false);

        Result<Long> result = clientPhoneService.addManualClientPhone(clientPhone);
        Long clientPhoneId = result.getResult();

        Map<Long, ClientPhone> clientPhones = listToMap(clientPhoneRepository.getAllClientPhones(clientId,
                emptyList()),
                ClientPhone::getId);
        SoftAssertions.assertSoftly(sa -> {
            ClientPhone actual = clientPhones.get(clientPhoneId);
            sa.assertThat(actual.getPhoneNumber().getPhone()).isEqualTo(clientPhone.getPhoneNumber().getPhone());
            sa.assertThat(actual.getPhoneNumber().getExtension()).isEqualTo(clientPhone.getPhoneNumber().getExtension());
            sa.assertThat(actual.getPhoneType()).isEqualTo(ClientPhoneType.MANUAL);
            sa.assertThat(actual.getComment()).isEqualTo(clientPhone.getComment());
        });
    }

    @Test
    public void addManualClientPhone_one_duplicate_failure() {
        PhoneNumber phoneNumber = new PhoneNumber().withPhone(getUniqPhone()).withExtension(9L);
        var firstClientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(phoneNumber)
                .withIsDeleted(false);

        var secondClientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(phoneNumber)
                .withIsDeleted(false);

        clientPhoneService.addManualClientPhone(firstClientPhone);
        Result<Long> result = clientPhoneService.addManualClientPhone(secondClientPhone);
        var errPath = path(field(ClientPhone.PHONE_NUMBER));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, duplicatedObject())));
    }

    @Test
    public void addManualClientPhones_two_duplicate_failure() {
        var clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneType(ClientPhoneType.MANUAL)
                .withPhoneNumber(new PhoneNumber().withPhone(getUniqPhone()).withExtension(9L));

        var operation = new ClientPhoneAddOperation(
                clientPhone.getClientId(),
                List.of(clientPhone, clientPhone),
                clientPhoneRepository);
        MassResult<Long> massResult = operation.prepareAndApply();
        var errPath = path(field(ClientPhone.PHONE_NUMBER));
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(massResult.get(0).getValidationResult())
                    .is(matchedBy(hasDefectWithDefinition(validationError(errPath, duplicatedObject()))));
            soft.assertThat(massResult.get(1).getValidationResult())
                    .is(matchedBy(hasDefectWithDefinition(validationError(errPath, duplicatedObject()))));
        });
    }

    @Test
    public void updateManualClientPhone_duplicate_failure() {
        var firstUniqPhone = getUniqPhone();
        var firstPhoneNumber = new PhoneNumber().withPhone(firstUniqPhone);
        steps.clientPhoneSteps().addClientManualPhone(clientId, firstPhoneNumber);

        var secondPhoneNumber = new PhoneNumber().withPhone(getUniqPhone());
        var secondPhone = steps.clientPhoneSteps().addClientManualPhone(clientId, secondPhoneNumber);

        var updatedSecondPhone = secondPhone.withPhoneNumber(firstPhoneNumber);
        Result<Long> result = clientPhoneService.updateClientPhone(updatedSecondPhone);
        var errPath = path(field(ClientPhone.PHONE_NUMBER));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, duplicatedObject())));
    }

    @Test
    public void updateManualClientPhone_comment_success() {
        var phoneNumber = new PhoneNumber().withPhone(getUniqPhone());
        Long phoneId = steps.clientPhoneSteps().addClientManualPhone(clientId, phoneNumber).getId();

        // Обновляем только комментарий
        var updatedPhone = new ClientPhone()
                .withId(phoneId)
                .withClientId(clientId)
                .withPhoneNumber(phoneNumber)
                .withComment("new");
        Result<Long> result = clientPhoneService.updateClientPhone(updatedPhone);
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    public void delete() {
        ClientPhone clientPhone = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId);
        Long clientPhoneId = clientPhone.getId();
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        steps.bannerSteps().createDefaultBanner(adGroup);
        TextBannerInfo banner = steps.bannerSteps().createDefaultBanner(adGroup);
        Long bannerId = banner.getBannerId();

        addOrganizationAndPhoneToBanner(bannerId, clientPhoneId);
        clientPhoneService.delete(clientId, List.of(clientPhoneId));

        Map<Long, ClientPhone> clientPhones = listToMap(clientPhoneRepository.getAllClientPhones(clientId,
                emptyList()),
                ClientPhone::getId);
        Map<Long, OldBanner> banners = listToMap(bannerRepository.getBanners(shard, List.of(bannerId)),
                OldBanner::getId);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(clientPhones.get(clientPhoneId)).isNull();
            OldTextBanner textBanner = (OldTextBanner) banners.get(bannerId);
            sa.assertThat(textBanner.getPhoneId()).isNull();
            sa.assertThat(textBanner.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
        });
    }

    @Test
    public void delete_fromCampaign() {
        ClientPhone clientPhone = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId);
        Long clientPhoneId = clientPhone.getId();
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        steps.bannerSteps().createDefaultBanner(adGroup);
        TextBannerInfo banner = steps.bannerSteps().createDefaultBanner(adGroup);
        Long bannerId = banner.getBannerId();
        Long campaignId = adGroup.getCampaignId();
        steps.organizationSteps()
                .linkDefaultOrganizationToCampaign(clientId, organization.getPermalinkId(), campaignId);
        steps.clientPhoneSteps().linkPhoneIdToCampaign(shard, campaignId, clientPhoneId);

        addOrganizationAndPhoneToBanner(bannerId, clientPhoneId);
        clientPhoneService.delete(clientId, List.of(clientPhoneId));

        Map<Long, ClientPhone> clientPhones = listToMap(clientPhoneRepository.getAllClientPhones(clientId,
                emptyList()),
                ClientPhone::getId);
        Map<Long, OldBanner> banners = listToMap(bannerRepository.getBanners(shard, List.of(bannerId)),
                OldBanner::getId);
        TextCampaign campaign = (TextCampaign)
                campaignTypedRepository.getTypedCampaigns(shard, List.of(campaignId)).get(0);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(clientPhones.get(clientPhoneId)).isNull();
            OldTextBanner textBanner = (OldTextBanner) banners.get(bannerId);
            sa.assertThat(textBanner.getPhoneId()).isNull();
            sa.assertThat(textBanner.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
            sa.assertThat(campaign.getDefaultTrackingPhoneId()).isNull();
        });
    }

    @Test
    public void handleTelephonyPhones_success() {
        Long permalinkId = RandomUtils.nextLong();
        Long counterId = RandomUtils.nextLong();
        String spravPhone = getUniqPhone();

        Long spravPhoneId = clientPhoneRepository.add(clientId, List.of(
                new ClientPhone()
                        .withClientId(clientId)
                        .withPermalinkId(permalinkId)
                        .withCounterId(counterId)
                        .withPhoneType(ClientPhoneType.SPRAV)
                        .withPhoneNumber(new PhoneNumber().withPhone(spravPhone))
                        .withIsDeleted(false)
        )).get(0);

        String telephonyNum = getUniqPhone().substring(1);
        String telephonyServiceId = String.valueOf(RandomUtils.nextInt());

        when(telephonyClient.getServiceNumber()).thenReturn(
                ServiceNumber.newBuilder()
                        .setNum(telephonyNum)
                        .setServiceNumberID(telephonyServiceId)
                        .setVersion(1)
                        .build()
        );

        Map<String, String> meta = Map.of(
                ORG_ID_META_KEY, permalinkId.toString(),
                CLIENT_ID_META_KEY, clientId.toString(),
                COUNTER_ID_META_KEY, counterId.toString());
        when(telephonyClient.getClientServiceNumbers(clientId.asLong())).thenReturn(
                List.of(
                        ServiceNumber.newBuilder()
                                .setNum(telephonyNum)
                                .setServiceNumberID(telephonyServiceId)
                                .setVersion(1)
                                .putAllMeta(meta)
                                .build()
                )
        );

        organizationClient.addUidsAndCounterIdsByPermalinkId(permalinkId, List.of(clientInfo.getUid()), counterId);

        List<ClientPhone> phones = clientPhoneRepository.getByPhoneIds(clientId, List.of(spravPhoneId));

        List<ClientPhone> telephonyPhones = clientPhoneService.handleTelephonyPhones(clientId, phones);

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(telephonyPhones).hasSize(1);
            ClientPhone phone = telephonyPhones.get(0);
            sa.assertThat(phone.getCounterId()).isEqualTo(counterId);
            sa.assertThat(phone.getPermalinkId()).isEqualTo(permalinkId);
            sa.assertThat(phone.getTelephonyServiceId()).isEqualTo(telephonyServiceId);
            sa.assertThat(phone.getTelephonyPhone()).isEqualTo(new PhoneNumber().withPhone("+" + telephonyNum));
            sa.assertThat(phone.getPhoneType()).isEqualTo(ClientPhoneType.TELEPHONY);
        });
    }

    @Test
    public void handleTelephonyPhones_onlyOneToAdd_success() {

        Long permalinkId1 = RandomUtils.nextLong();
        Long counterId1 = RandomUtils.nextLong();
        String phone1 = getUniqPhone();
        Long permalinkId2 = RandomUtils.nextLong();
        Long counterId2 = RandomUtils.nextLong();
        String phone2 = getUniqPhone();

        String serviceId1 = String.valueOf(RandomUtils.nextInt());
        clientPhoneRepository.add(clientId, List.of(
                new ClientPhone()
                        .withClientId(clientId)
                        .withPermalinkId(permalinkId1)
                        .withCounterId(counterId1)
                        .withPhoneType(ClientPhoneType.SPRAV)
                        .withPhoneNumber(new PhoneNumber().withPhone(phone1))
                        .withIsDeleted(false),
                new ClientPhone()
                        .withClientId(clientId)
                        .withPermalinkId(permalinkId2)
                        .withCounterId(counterId2)
                        .withPhoneType(ClientPhoneType.SPRAV)
                        .withPhoneNumber(new PhoneNumber().withPhone(phone2))
                        .withIsDeleted(false),
                new ClientPhone()
                        .withClientId(clientId)
                        .withPermalinkId(permalinkId1)
                        .withCounterId(counterId1)
                        .withPhoneType(ClientPhoneType.TELEPHONY)
                        .withPhoneNumber(new PhoneNumber().withPhone(getUniqPhone()))
                        .withTelephonyServiceId(serviceId1)
                        .withIsDeleted(false)
        ));
        String serviceId2 = String.valueOf(RandomUtils.nextInt());
        when(telephonyClient.getServiceNumber()).thenReturn(
                ServiceNumber.newBuilder()
                        .setNum(getUniqPhone().substring(1))
                        .setServiceNumberID(serviceId2)
                        .setVersion(1)
                        .build()
        );

        Map<String, String> meta1 = Map.of(
                ORG_ID_META_KEY, permalinkId1.toString(),
                CLIENT_ID_META_KEY, clientId.toString(),
                COUNTER_ID_META_KEY, counterId1.toString());
        Map<String, String> meta2 = Map.of(
                ORG_ID_META_KEY, permalinkId2.toString(),
                CLIENT_ID_META_KEY, clientId.toString(),
                COUNTER_ID_META_KEY, counterId2.toString());
        when(telephonyClient.getClientServiceNumbers(clientId.asLong())).thenReturn(
                List.of(
                        ServiceNumber.newBuilder()
                                .setServiceNumberID(serviceId1)
                                .setVersion(1)
                                .putAllMeta(meta1)
                                .build(),
                        ServiceNumber.newBuilder()
                                .setServiceNumberID(serviceId2)
                                .setVersion(1)
                                .putAllMeta(meta2)
                                .build()
                )
        );

        organizationClient.addUidsAndCounterIdsByPermalinkId(permalinkId1, List.of(clientInfo.getUid()), counterId1);
        organizationClient.addUidsAndCounterIdsByPermalinkId(permalinkId2, List.of(clientInfo.getUid()), counterId2);

        List<ClientPhone> phones = clientPhoneRepository.getAllClientPhones(clientId,
                List.of(permalinkId1, permalinkId2));

        List<ClientPhone> telephonyPhones = clientPhoneService.handleTelephonyPhones(clientId, phones);

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(telephonyPhones).hasSize(1);
            ClientPhone phone = telephonyPhones.get(0);
            sa.assertThat(phone.getPhoneType()).isEqualTo(ClientPhoneType.TELEPHONY);
            sa.assertThat(phone.getCounterId()).isEqualTo(counterId2);
            sa.assertThat(phone.getPermalinkId()).isEqualTo(permalinkId2);
        });
    }

    @Test
    public void updateBannersPhone_updateToOrganizationPhone_success() {
        ClientPhone clientPhone = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId);
        Long clientPhoneId = clientPhone.getId();
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        TextBannerInfo banner = steps.bannerSteps().createDefaultBanner(adGroup);
        Long bannerId = banner.getBannerId();
        addOrganizationAndPhoneToBanner(bannerId, clientPhoneId);

        ClientPhone clientPhoneNew = steps.clientPhoneSteps()
                .addDefaultClientOrganizationPhone(clientId, organization.getPermalinkId());

        MassResult<Long> result = clientPhoneService.updateBannersPhone(clientId, operatorUid,
                clientPhoneNew.getId(), List.of(bannerId),
                List.of(organization.getPermalinkId()));

        var banners = listToMap(bannerRepository.getBanners(shard, List.of(bannerId)), OldBanner::getId);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(result.getValidationResult().hasAnyErrors()).isFalse();
            result.getValidationResult().flattenErrors().forEach(fe -> sa.assertThat(fe).isNull());
            sa.assertThat(((OldTextBanner) banners.get(bannerId)).getPhoneId()).isEqualTo(clientPhoneNew.getId());
            sa.assertThat(banners.get(bannerId).getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
        });
    }

    @Test
    public void updateBannersPhone_updateToManualPhone_success() {
        ClientPhone clientPhone = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId);
        Long clientPhoneId = clientPhone.getId();
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        TextBannerInfo banner = steps.bannerSteps().createDefaultBanner(adGroup);
        Long bannerId = banner.getBannerId();
        addOrganizationAndPhoneToBanner(bannerId, clientPhoneId);

        ClientPhone clientPhoneNew = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId);

        MassResult<Long> result = clientPhoneService.updateBannersPhone(clientId, operatorUid,
                clientPhoneNew.getId(), List.of(bannerId),
                List.of(organization.getPermalinkId()));

        var banners = listToMap(bannerRepository.getBanners(shard, List.of(bannerId)), OldBanner::getId);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(result.getValidationResult().hasAnyErrors()).isFalse();
            result.getValidationResult().flattenErrors().forEach(fe -> sa.assertThat(fe).isNull());
            sa.assertThat(((OldTextBanner) banners.get(bannerId)).getPhoneId()).isEqualTo(clientPhoneNew.getId());
            sa.assertThat(banners.get(bannerId).getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
        });
    }

    @Test
    public void updateBannersPhone_updateToNull_success() {
        ClientPhone clientPhoneOld = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId);
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        TextBannerInfo banner = steps.bannerSteps().createDefaultBanner(adGroup);
        Long bannerId = banner.getBannerId();
        addOrganizationAndPhoneToBanner(bannerId, clientPhoneOld.getId());

        MassResult<Long> result = clientPhoneService.updateBannersPhone(clientId, operatorUid,
                null,
                List.of(bannerId), List.of(organization.getPermalinkId()));

        Map<Long, OldBanner> banners = listToMap(bannerRepository.getBanners(shard, List.of(bannerId)),
                OldBanner::getId);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(result.getValidationResult().hasAnyErrors()).isFalse();
            result.getValidationResult().flattenErrors().forEach(fe -> sa.assertThat(fe).isNull());
            sa.assertThat(((OldTextBanner) banners.get(bannerId)).getPhoneId()).isNull();
            sa.assertThat(banners.get(bannerId).getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
        });
    }

    @Test
    public void updateBannersPhone_updatePhoneFromOtherOrganization_exception() {
        ClientPhone clientPhone = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId);
        Long clientPhoneId = clientPhone.getId();
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        TextBannerInfo banner = steps.bannerSteps().createDefaultBanner(adGroup);
        Long bannerId = banner.getBannerId();
        addOrganizationAndPhoneToBanner(bannerId, clientPhoneId);

        Organization anotherOrganization = TestOrganizations.defaultOrganization(clientId);
        ClientPhone anotherOrgClientPhone = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId,
                anotherOrganization.getPermalinkId());

        MassResult<Long> result = clientPhoneService.updateBannersPhone(clientId, operatorUid,
                anotherOrgClientPhone.getId(), List.of(bannerId),
                List.of(organization.getPermalinkId(), anotherOrganization.getPermalinkId()));

        Path errPath = path(index(0), field(OldTextBanner.PHONE_ID));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, CommonDefects.objectNotFound())));
    }

    @Test
    public void handleTelephonyPhones_alreadyHaveTelephony() {

        Long permalinkId = RandomUtils.nextLong();
        Long counterId = RandomUtils.nextLong();

        Long spravPhoneId = clientPhoneRepository.add(clientId, List.of(
                new ClientPhone()
                        .withClientId(clientId)
                        .withPermalinkId(permalinkId)
                        .withCounterId(counterId)
                        .withPhoneType(ClientPhoneType.SPRAV)
                        .withPhoneNumber(new PhoneNumber().withPhone(getUniqPhone()))
                        .withIsDeleted(false)
        )).get(0);

        Long telephonyPhoneId = clientPhoneRepository.add(clientId, List.of(
                new ClientPhone()
                        .withClientId(clientId)
                        .withPermalinkId(permalinkId)
                        .withCounterId(counterId)
                        .withPhoneType(ClientPhoneType.TELEPHONY)
                        .withPhoneNumber(new PhoneNumber().withPhone(getUniqPhone()))
                        .withIsDeleted(false)
        )).get(0);

        List<ClientPhone> phones = clientPhoneRepository.getByPhoneIds(clientId, List.of(spravPhoneId,
                telephonyPhoneId));

        List<ClientPhone> telephonyPhones = clientPhoneService.handleTelephonyPhones(clientId, phones);

        SoftAssertions.assertSoftly(sa -> sa.assertThat(telephonyPhones).isEmpty());
    }

    @Test
    public void handleTelephonyPhones_withoutTelephonyAllowed() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.TELEPHONY_ALLOWED, false);

        Long permalinkId = RandomUtils.nextLong();
        steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId, permalinkId);
        var telephonyPhones = clientPhoneService.getAndSaveTelephonyPhones(clientId, List.of(permalinkId));
        SoftAssertions.assertSoftly(sa -> sa.assertThat(telephonyPhones).isEmpty());
    }

    @Test
    public void handleTelephonyPhones_withTelephonyAllowed() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.TELEPHONY_ALLOWED, true);

        Long permalinkId = RandomUtils.nextLong();
        Long counterId = RandomUtils.nextLong();

        // Добавляем организацию, чтобы у нее появился счетчик метрики, т.к. без него не выдается телефон Телефонии
        organizationClient.addUidsAndCounterIdsByPermalinkId(permalinkId, List.of(clientInfo.getUid()), counterId);
        // Имитируем отсутствие прав на организацию
        when(organizationClient.getOrganizationsUidsWithModifyPermission(anyCollection(), anyCollection(),
                anyString(), anyString())).thenReturn(Map.of(permalinkId, Collections.emptyList()));

        String telephonyServiceId = String.valueOf(RandomUtils.nextInt());

        when(telephonyClient.getServiceNumber()).thenReturn(
                ServiceNumber.newBuilder()
                        .setNum(getUniqPhone())
                        .setServiceNumberID(telephonyServiceId)
                        .setVersion(1)
                        .build()
        );

        Map<String, String> meta = Map.of(
                ORG_ID_META_KEY, permalinkId.toString(),
                CLIENT_ID_META_KEY, clientId.toString(),
                COUNTER_ID_META_KEY, counterId.toString());
        when(telephonyClient.getClientServiceNumbers(clientId.asLong())).thenReturn(
                List.of(
                        ServiceNumber.newBuilder()
                                .setServiceNumberID(telephonyServiceId)
                                .setVersion(1)
                                .putAllMeta(meta)
                                .build()
                )
        );

        steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId, permalinkId);
        var telephonyPhones = clientPhoneService.getAndSaveTelephonyPhones(clientId, List.of(permalinkId));
        SoftAssertions.assertSoftly(sa -> sa.assertThat(telephonyPhones).isNotEmpty());
    }

    @Test
    public void change_success() {
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        steps.bannerSteps().createDefaultBanner(adGroup);

        Long bannerId1 = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();
        Long bannerId2 = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();

        Long clientPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        addOrganizationAndPhoneToBanner(bannerId1, clientPhoneId);
        addOrganizationAndPhoneToBanner(bannerId2, clientPhoneId);
        organizationClient.addUidsByPermalinkId(organization.getPermalinkId(), List.of(operatorUid));

        Long replacePhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();

        UidAndClientId uidAndClientId = UidAndClientId.of(clientInfo.getUid(), clientId);
        MassResult<Long> massResult =
                clientPhoneReplaceService.replaceTrackingPhone(uidAndClientId, operatorUid, List.of(clientPhoneId),
                        replacePhoneId);

        Map<Long, OldBanner> banners =
                listToMap(bannerRepository.getBanners(shard, List.of(bannerId1, bannerId2)), OldBanner::getId);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(massResult.getValidationResult().hasAnyErrors()).isFalse();
            OldTextBanner textBanner1 = (OldTextBanner) banners.get(bannerId1);
            sa.assertThat(textBanner1.getPhoneId()).isEqualTo(replacePhoneId);
            OldTextBanner textBanner2 = (OldTextBanner) banners.get(bannerId2);
            sa.assertThat(textBanner2.getPhoneId()).isEqualTo(replacePhoneId);
        });
    }

    @Test
    public void change_onCampaigns_success() {
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        Long bannerId = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();

        // Создаем телефон и привязываем его к баннеру
        Long clientPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        addOrganizationAndPhoneToBanner(bannerId, clientPhoneId);
        // Привязываем телефон к кампании
        Long campaignId = adGroup.getCampaignId();
        steps.organizationSteps().linkDefaultOrganizationToCampaign(clientId, organization.getPermalinkId(),
                campaignId);
        steps.clientPhoneSteps().linkPhoneIdToCampaign(shard, campaignId, clientPhoneId);
        // Создаем номер, на который хотим заменить
        Long replacePhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();

        UidAndClientId uidAndClientId = UidAndClientId.of(clientInfo.getUid(), clientId);
        MassResult<Long> massResult =
                clientPhoneReplaceService.replaceTrackingPhone(uidAndClientId, operatorUid, List.of(clientPhoneId),
                        replacePhoneId);

        Map<Long, OldBanner> banners =
                listToMap(bannerRepository.getBanners(shard, List.of(bannerId)), OldBanner::getId);
        TextCampaign campaign = (TextCampaign)
                campaignTypedRepository.getTypedCampaigns(shard, List.of(campaignId)).get(0);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(massResult.getValidationResult().hasAnyErrors()).isFalse();
            OldTextBanner textBanner = (OldTextBanner) banners.get(bannerId);
            sa.assertThat(textBanner.getPhoneId()).isEqualTo(replacePhoneId);
            sa.assertThat(campaign.getDefaultTrackingPhoneId()).isEqualTo(replacePhoneId);
        });
    }

    @Test
    public void change_onCampaignsWithoutBanners_success() {
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        Long bannerId = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();

        // Создаем телефон
        Long clientPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        // Привязываем телефон к кампании
        Long campaignId = adGroup.getCampaignId();
        steps.organizationSteps().linkDefaultOrganizationToCampaign(clientId, organization.getPermalinkId(),
                campaignId);
        steps.clientPhoneSteps().linkPhoneIdToCampaign(shard, campaignId, clientPhoneId);
        // Создаем номер, на который хотим заменить
        Long replacePhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();

        UidAndClientId uidAndClientId = UidAndClientId.of(clientInfo.getUid(), clientId);
        MassResult<Long> massResult =
                clientPhoneReplaceService.replaceTrackingPhone(uidAndClientId, operatorUid, List.of(clientPhoneId),
                        replacePhoneId);

        TextCampaign campaign = (TextCampaign)
                campaignTypedRepository.getTypedCampaigns(shard, List.of(campaignId)).get(0);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(massResult.getValidationResult().hasAnyErrors()).isFalse();
            // Телефон должен замениться даже если он не привязан ни к одному баннеру
            sa.assertThat(campaign.getDefaultTrackingPhoneId()).isEqualTo(replacePhoneId);
        });
    }

    @Test
    public void change_hasNoAccessToOrganization_success() {
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        steps.bannerSteps().createDefaultBanner(adGroup);

        Long bannerId1 = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();
        Long bannerId2 = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();

        Long clientPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        addOrganizationAndPhoneToBanner(bannerId1, clientPhoneId);
        ClientInfo otherClientInfo = steps.clientSteps().createDefaultClient();
        Organization otherOrganization = TestOrganizations.defaultOrganization(otherClientInfo.getClientId());
        addOrganizationAndPhoneToBanner(otherOrganization, bannerId2, clientPhoneId);
        organizationClient.addUidsByPermalinkId(organization.getPermalinkId(), List.of(operatorUid));
        organizationClient.addUidsByPermalinkId(otherOrganization.getPermalinkId(), List.of(otherClientInfo.getUid()));

        Long replacePhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();

        UidAndClientId uidAndClientId = UidAndClientId.of(clientInfo.getUid(), clientId);
        MassResult<Long> massResult =
                clientPhoneReplaceService.replaceTrackingPhone(uidAndClientId, operatorUid, List.of(clientPhoneId),
                        replacePhoneId);

        Map<Long, OldBanner> banners =
                listToMap(bannerRepository.getBanners(shard, List.of(bannerId1, bannerId2)), OldBanner::getId);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(massResult.getValidationResult().hasAnyErrors()).isFalse();
            OldTextBanner textBanner1 = (OldTextBanner) banners.get(bannerId1);
            sa.assertThat(textBanner1.getPhoneId()).isEqualTo(replacePhoneId);
            OldTextBanner textBanner2 = (OldTextBanner) banners.get(bannerId2);
            sa.assertThat(textBanner2.getPhoneId()).isNull();
        });
    }

    @Test
    public void change_otherClientPhone_failed() {
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        steps.bannerSteps().createDefaultBanner(adGroup);

        Long bannerId1 = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();
        Long bannerId2 = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();

        Long clientPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        addOrganizationAndPhoneToBanner(bannerId1, clientPhoneId);
        addOrganizationAndPhoneToBanner(bannerId2, clientPhoneId);
        organizationClient.addUidsByPermalinkId(organization.getPermalinkId(), List.of(operatorUid));

        ClientId otherClientId = steps.clientSteps().createDefaultClient().getClientId();
        Long replacePhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(otherClientId).getId();
        UidAndClientId uidAndClientId = UidAndClientId.of(clientInfo.getUid(), clientId);

        MassResult<Long> massResult =
                clientPhoneReplaceService.replaceTrackingPhone(uidAndClientId, operatorUid, List.of(clientPhoneId),
                        replacePhoneId);
        List<ClientPhone> replacePhoneIdFromDb =
                clientPhoneService.getByPhoneIds(otherClientId, List.of(replacePhoneId));
        Map<Long, OldBanner> banners =
                listToMap(bannerRepository.getBanners(shard, List.of(bannerId1, bannerId2)), OldBanner::getId);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(massResult.getValidationResult().hasAnyErrors()).isTrue();
            OldTextBanner textBanner1 = (OldTextBanner) banners.get(bannerId1);
            sa.assertThat(textBanner1.getPhoneId()).isEqualTo(clientPhoneId);
            OldTextBanner textBanner2 = (OldTextBanner) banners.get(bannerId2);
            sa.assertThat(textBanner2.getPhoneId()).isEqualTo(clientPhoneId);
            sa.assertThat(replacePhoneIdFromDb).isNotEmpty();
        });
    }

    @Test
    public void getAndSaveOrganizationPhones_notChangesInOrgApi_success() {
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        steps.bannerSteps().createDefaultBanner(adGroup);
        Long bannerId = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();
        Long permalink = organization.getPermalinkId();

        Long clientPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        addOrganizationAndPhoneToBanner(bannerId, clientPhoneId);

        organizationClient.addUidsByPermalinkId(permalink, List.of(operatorUid));
        Long spavPhoneId = addSpravPhonesToDb(permalink).get(0);
        Map<Long, List<ClientPhone>> orgPhones = clientPhoneService.getAndSaveOrganizationPhones(clientId,
                List.of(permalink));

        List<ClientPhone> clientPhones = clientPhoneService.getAllClientPhones(clientId, List.of(permalink));
        List<ClientPhone> spravClientPhones = filterList(clientPhones, p -> p.getPhoneType() == ClientPhoneType.SPRAV);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(spravClientPhones).hasSize(1);
            sa.assertThat(spravClientPhones.get(0).getId()).isEqualTo(spavPhoneId);
            sa.assertThat(orgPhones.get(permalink).get(0).getIsHidden()).isEqualTo(false);
        });
    }

    @Test
    public void getAndSaveOrganizationPhones_noException_whenManyOrgsWithSamePhone() {
        String theSamePhone = "+70001112233";

        Long permalink = organization.getPermalinkId();
        organizationClient.addUidsByPermalinkId(permalink, List.of(operatorUid));
        organizationClient.changeCompanyPhones(permalink, List.of(theSamePhone));

        Organization organization2 = TestOrganizations.defaultOrganization(clientId);
        Long permalink2 = organization2.getPermalinkId();
        organizationClient.addUidsByPermalinkId(permalink2, List.of(operatorUid));
        organizationClient.changeCompanyPhones(permalink2, List.of(theSamePhone));

        Assertions.assertThatCode(
                () -> clientPhoneService.getAndSaveOrganizationPhones(clientId, List.of(permalink, permalink2)))
                .doesNotThrowAnyException();
    }

    @Test
    public void getAndSaveOrganizationPhones_notChangesInOrgApi_success_isHideTrue() {
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        steps.bannerSteps().createDefaultBanner(adGroup);
        Long bannerId = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();
        Long permalink = organization.getPermalinkId();

        Long clientPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        addOrganizationAndPhoneToBanner(bannerId, clientPhoneId);

        organizationClient.addUidsByPermalinkId(permalink, List.of(operatorUid));
        addSpravPhonesToDb(permalink).get(0);
        Map<Long, List<ClientPhone>> orgPhones1 = clientPhoneService.getAndSaveOrganizationPhones(clientId, List.of(permalink));

        Organization organization2 = TestOrganizations.defaultOrganization(clientId);
        Long permalink2 = organization2.getPermalinkId();
        organizationClient.addUidsWithHiddenPhoneByPermalinkId(permalink2, List.of(operatorUid));
        Long spavPhoneId2 = addSpravPhonesToDb(permalink2).get(0);
        Map<Long, List<ClientPhone>> orgPhones2 = clientPhoneService.getAndSaveOrganizationPhones(clientId, List.of(permalink2));

        List<ClientPhone> clientPhones = clientPhoneService.getAllClientPhones(clientId, List.of(permalink2));
        List<ClientPhone> spravClientPhones = filterList(clientPhones, p -> p.getPhoneType() == ClientPhoneType.SPRAV);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(spravClientPhones).hasSize(1);
            sa.assertThat(spravClientPhones.get(0).getId()).isEqualTo(spavPhoneId2);
            /**
             * ru.yandex.direct.core.entity.clientphone.ClientPhoneService#getAllClientPhones
             * берет данные из базы, поэтому isHide равен null
             */
            sa.assertThat(spravClientPhones.get(0).getIsHidden()).isNull();
            /**
             * Поле isHide не равно null, только тогда, когда оно берется из API справочника, т.е. при вызове метода
             * ru.yandex.direct.core.entity.clientphone.ClientPhoneService#getAndSaveOrganizationPhones
             */
            sa.assertThat(orgPhones1.get(permalink).get(0).getIsHidden()).isEqualTo(false);
            sa.assertThat(orgPhones2.get(permalink2).get(0).getIsHidden()).isEqualTo(true);
        });
    }

    @Test
    public void getAndSaveOrganizationPhones_changeSpravPhoneInOrgApi_success() {
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        steps.bannerSteps().createDefaultBanner(adGroup);
        Long bannerId = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();
        Long permalink = organization.getPermalinkId();

        Long clientPhoneId = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId).getId();
        addOrganizationAndPhoneToBanner(bannerId, clientPhoneId);

        organizationClient.addUidsByPermalinkId(permalink, List.of(operatorUid));
        Long spavPhoneId = addSpravPhonesToDb(permalink).get(0);
        organizationClient.changeCompanyPhones(permalink, List.of(getUniqPhone()));
        clientPhoneService.getAndSaveOrganizationPhones(clientId, List.of(permalink));

        List<ClientPhone> clientPhones = clientPhoneService.getAllClientPhones(clientId, List.of(permalink));
        List<ClientPhone> spravClientPhones = filterList(clientPhones, p -> p.getPhoneType() == ClientPhoneType.SPRAV);
        List<CompanyPhone> orgPhones = organizationClient.getCompanyPhones(permalink);
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(spravClientPhones).hasSize(1);
            ClientPhone orgPhone = spravClientPhones.get(0);
            sa.assertThat(orgPhone.getId()).isNotEqualTo(spavPhoneId);
            sa.assertThat(ClientPhoneMapping.phoneNumberToDb(orgPhone.getPhoneNumber()))
                    .isEqualTo(ClientPhoneMapping.phoneNumberToDb(ClientPhoneUtils.toPhoneNumber(orgPhones.get(0))));
        });
    }

    @Test
    public void getAndSaveOrganizationPhones_changeSpravPhonesInOrgApi_success() {
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        steps.bannerSteps().createDefaultBanner(adGroup);
        Long bannerId = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();
        Long permalink = organization.getPermalinkId();

        // Изначально у организации было 3 телефона
        var firstPhone = getUniqPhone();
        organizationClient.addUidsWithPhonesByPermalinkId(
                permalink,
                List.of(operatorUid),
                List.of(firstPhone, getUniqPhone(), getUniqPhone())
        );
        List<Long> spavPhoneIds = addSpravPhonesToDb(permalink);

        // К баннеру привязываем второй номер организации
        Long oldFirstPhoneId = spavPhoneIds.get(0);
        Long oldSecondPhoneId = spavPhoneIds.get(1);
        Long oldThirdPhoneId = spavPhoneIds.get(2);
        addOrganizationAndPhoneToBanner(bannerId, oldSecondPhoneId);

        // Изменяем телефоны организации: оставляем первый, второй изменяем, третий удаляем
        organizationClient.changeCompanyPhones(permalink, List.of(firstPhone, getUniqPhone()));
        Map<Long, List<ClientPhone>> phoneIdsByPermalink =
                clientPhoneService.getAndSaveOrganizationPhones(clientId, List.of(permalink));

        List<ClientPhone> clientPhones = clientPhoneService.getAllClientPhones(clientId, List.of(permalink));
        List<ClientPhone> manualPhones = filterList(clientPhones, p -> p.getPhoneType() == ClientPhoneType.MANUAL);
        Map<Long, ClientPhone> manualPhonesById = listToMap(
                manualPhones,
                ClientPhone::getId,
                Function.identity()
        );
        List<ClientPhone> spravClientPhones = filterList(clientPhones, p -> p.getPhoneType() == ClientPhoneType.SPRAV);
        Map<Long, String> spravPhoneNumbersById = listToMap(
                spravClientPhones,
                ClientPhone::getId,
                p -> ClientPhoneMapping.phoneNumberToDb(p.getPhoneNumber())
        );
        List<CompanyPhone> orgPhones = organizationClient.getCompanyPhones(permalink);
        var bannerIdsByPhoneId = clientPhoneRepository.getBannerIdsByPhoneId(shard, List.of(oldSecondPhoneId));
        SoftAssertions.assertSoftly(sa -> {
            // Должны остаться: первый номер телефона, второй новый
            // Второй старый (т.к. привязан к баннеру) должен быть переведен в ручной
            // Третий номер должен быть удален, т.к. в Справочнике его нет, и он не привязан к баннеру
            sa.assertThat(spravPhoneNumbersById).hasSize(2);
            sa.assertThat(manualPhonesById).hasSize(1);

            // первый номер телефона
            String oldFirstPhone = spravPhoneNumbersById.get(oldFirstPhoneId);
            sa.assertThat(oldFirstPhone)
                    .isEqualTo(ClientPhoneMapping.phoneNumberToDb(ClientPhoneUtils.toPhoneNumber(orgPhones.get(0))));

            //второй старый (т.к. привязан к баннеру)
            // будет переведен из телефонов организации в ручной
            sa.assertThat(spravPhoneNumbersById).doesNotContainKey(oldSecondPhoneId);
            sa.assertThat(manualPhonesById).containsKey(oldSecondPhoneId);
            ClientPhone oldSecondPhone = manualPhonesById.get(oldSecondPhoneId);
            sa.assertThat(oldSecondPhone.getCounterId()).isNull();
            sa.assertThat(oldSecondPhone.getPermalinkId()).isNull();
            sa.assertThat(oldSecondPhone.getComment()).isNotEmpty();
            sa.assertThat(bannerIdsByPhoneId.get(oldSecondPhoneId)).contains(bannerId);

            //второй новый
            sa.assertThat(spravPhoneNumbersById)
                    .containsValue(ClientPhoneMapping.phoneNumberToDb(ClientPhoneUtils.toPhoneNumber(orgPhones.get(1))));

            // Третий номер
            sa.assertThat(spravPhoneNumbersById).doesNotContainKey(oldThirdPhoneId);

            Long mainPhoneId = phoneIdsByPermalink.get(permalink).get(0).getId();
            sa.assertThat(mainPhoneId).isEqualTo(oldFirstPhoneId);
        });
    }

    @Test
    public void getAndSaveOrganizationPhones_notValidExtensions_success() {
        Long permalink = organization.getPermalinkId();

        String firstPhone = getUniqPhone();
        String secondPhone = getUniqPhone();
        String thirdPhone = getUniqPhone();
        String fourthPhone = getUniqPhone();
        organizationClient.addUidsWithPhonesByPermalinkId(
                permalink,
                List.of(operatorUid),
                List.of(
                        firstPhone + ",123456",
                        secondPhone + ",Менеджер",
                        thirdPhone + ",1234567",
                        fourthPhone + ",-12345"
                )
        );
        List<ClientPhone> clientPhones = clientPhoneService
                .getAndSaveOrganizationPhones(clientId, List.of(permalink))
                .get(permalink);

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(clientPhones).hasSize(4);

            PhoneNumber firstPhoneNumber = clientPhones.get(0).getPhoneNumber();
            sa.assertThat(firstPhoneNumber.getPhone()).isEqualTo(firstPhone);
            sa.assertThat(firstPhoneNumber.getExtension()).isEqualTo(123456L);

            PhoneNumber secondPhoneNumber = clientPhones.get(1).getPhoneNumber();
            sa.assertThat(secondPhoneNumber.getPhone()).isEqualTo(secondPhone);
            sa.assertThat(secondPhoneNumber.getExtension()).isNull();

            PhoneNumber thirdPhoneNumber = clientPhones.get(2).getPhoneNumber();
            sa.assertThat(thirdPhoneNumber.getPhone()).isEqualTo(thirdPhone);
            sa.assertThat(thirdPhoneNumber.getExtension()).isNull();

            PhoneNumber fourthPhoneNumber = clientPhones.get(3).getPhoneNumber();
            sa.assertThat(fourthPhoneNumber.getPhone()).isEqualTo(fourthPhone);
            sa.assertThat(fourthPhoneNumber.getExtension()).isNull();
        });
    }

    @Test
    public void getAndSaveOrganizationPhones_notValidOrgPhone_success() {
        Long permalink = organization.getPermalinkId();

        String firstPhone = getUniqPhone();
        String secondPhone = "+799912"; // должно быть минимум 8
        String thirdPhone = "+799912345678910"; // должно быть максимум 14
        organizationClient.addUidsWithPhonesByPermalinkId(
                permalink,
                List.of(operatorUid),
                List.of(firstPhone, secondPhone, thirdPhone)
        );
        List<ClientPhone> clientPhones = clientPhoneService
                .getAndSaveOrganizationPhones(clientId, List.of(permalink))
                .get(permalink);

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(clientPhones).hasSize(1);

            PhoneNumber firstPhoneNumber = clientPhones.get(0).getPhoneNumber();
            sa.assertThat(firstPhoneNumber.getPhone()).isEqualTo(firstPhone);
        });
    }

    /**
     * Мы должны удалять телефоны организаций, если они не привязаны ни к какому из баннеров/кампаний.
     * Этим тестом проверяется, что удаление будет выполнено, в случае, когда при загрузке телефонов
     * организации из Справочника, все номера были невалидными.
     */
    @Test
    public void getAndSaveOrganizationPhones_deletedUnusedOrgPhoneWhenNoOrgValidatedPhone_success() {
        Long permalinkId = organization.getPermalinkId();
        List<Long> permalinkIds = List.of(permalinkId);
        // Телефон организации, не привязанный ни к какому из баннеров/кампаний
        steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId, permalinkId);
        assumeThat(clientPhoneService.getAllClientPhones(clientId, permalinkIds), hasSize(1));

        String notValidOrgPhone = "+799912"; // должно быть минимум 8
        organizationClient.addUidsWithPhonesByPermalinkId(
                permalinkId,
                List.of(operatorUid),
                List.of(notValidOrgPhone)
        );
        List<ClientPhone> clientPhones = clientPhoneService
                .getAndSaveOrganizationPhones(clientId, permalinkIds)
                .get(permalinkId);

        var clientPhonesForPermalink = clientPhoneService.getAllClientPhones(clientId, permalinkIds);

        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(clientPhones).hasSize(0);
            sa.assertThat(clientPhonesForPermalink).hasSize(0);
        });
    }

    @Test
    public void getAndSaveOrganizationPhones_convertToManualWithoutDupl_success() {
        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        steps.bannerSteps().createDefaultBanner(adGroup);
        Long firstBannerId = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();
        Long secondBannerId = steps.bannerSteps().createDefaultBanner(adGroup).getBannerId();
        Long permalink = organization.getPermalinkId();

        // Изначально у организации было 2 телефона
        var firstPhone = getUniqPhone();
        var secondPhone = getUniqPhone();
        organizationClient.addUidsWithPhonesByPermalinkId(
                permalink,
                List.of(operatorUid),
                List.of(firstPhone, secondPhone)
        );
        List<Long> spavPhoneIds = addSpravPhonesToDb(permalink);

        // К баннерам привязываем номера организации
        Long oldFirstPhoneId = spavPhoneIds.get(0);
        Long oldSecondPhoneId = spavPhoneIds.get(1);
        addOrganizationAndPhoneToBanner(firstBannerId, oldFirstPhoneId);
        addOrganizationAndPhoneToBanner(secondBannerId, oldSecondPhoneId);

        // Добавляем ручной номер с не пустым комментарием, совпадающий с первым номером организации
        steps.clientPhoneSteps().addClientManualPhone(clientId, new PhoneNumber().withPhone(firstPhone));
        // Добавляем ручной номер с пустым комментарием, совпадающий со вторым номером организации
        steps.clientPhoneSteps().addClientManualPhone(clientId,
                new ClientPhone()
                        .withClientId(clientId)
                        .withPhoneNumber(new PhoneNumber().withPhone(secondPhone))
                        .withComment("")
                        .withPhoneType(ClientPhoneType.MANUAL)
                        .withIsDeleted(false)
        );

        // Изменяем телефоны организации: удаляем старые номера, добавляем новый третий
        organizationClient.changeCompanyPhones(permalink, List.of(getUniqPhone()));

        clientPhoneService.getAndSaveOrganizationPhones(clientId, List.of(permalink));

        List<ClientPhone> clientPhones = clientPhoneService.getAllClientPhones(clientId, List.of(permalink));
        List<ClientPhone> manualPhones = filterList(clientPhones, p -> p.getPhoneType() == ClientPhoneType.MANUAL);
        List<Long> spravPhoneIds = filterAndMapList(
                clientPhones,
                p -> p.getPhoneType() == ClientPhoneType.SPRAV,
                ClientPhone::getId
        );

        Map<Long, List<Long>> bannerIdsByPhoneId = clientPhoneRepository.getBannerIdsByPhoneId(
                shard,
                List.of(oldFirstPhoneId, oldSecondPhoneId, manualPhones.get(0).getId(), manualPhones.get(1).getId())
        );
        SoftAssertions.assertSoftly(sa -> {
            // Должны остаться: новый третий номер телефона
            sa.assertThat(spravPhoneIds).hasSize(1);
            sa.assertThat(spravPhoneIds).doesNotContain(oldFirstPhoneId);
            sa.assertThat(spravPhoneIds).doesNotContain(oldSecondPhoneId);

            // На баннере не должны быть привязаны старые телефоны организации
            sa.assertThat(bannerIdsByPhoneId.get(oldFirstPhoneId)).isNull();
            sa.assertThat(bannerIdsByPhoneId.get(oldSecondPhoneId)).isNull();

            sa.assertThat(manualPhones).hasSize(2);

            var firstUpdatedManualPhone = manualPhones.get(0);
            sa.assertThat(firstUpdatedManualPhone.getPhoneNumber().getPhone()).isEqualTo(firstPhone);
            sa.assertThat(firstUpdatedManualPhone.getComment()).isEqualTo("Comment");
            sa.assertThat(bannerIdsByPhoneId.get(firstUpdatedManualPhone.getId())).contains(firstBannerId);

            var secondUpdatedManualPhone = manualPhones.get(1);
            sa.assertThat(secondUpdatedManualPhone.getPhoneNumber().getPhone()).isEqualTo(secondPhone);
            sa.assertThat(secondUpdatedManualPhone.getComment()).isEqualTo("привязанный номер из организации");
            sa.assertThat(bannerIdsByPhoneId.get(secondUpdatedManualPhone.getId())).contains(secondBannerId);
        });
    }

    @Test
    public void attributeClicksOnPhones_success() {
        var counterIdWithDomain = new CounterIdWithDomain(RandomNumberUtils.nextPositiveLong(), "domain.ru");
        // Один телефон с двумя разными номерами Телефонии
        var phoneNumber = getUniqPhone();
        var firstTelephonyPhone = addClientTelephonyPhone(phoneNumber);
        var secondTelephonyPhone = addClientTelephonyPhone(phoneNumber);
        // Телефон, для которого есть клики и по нему и по его номеру Телефонии
        var thirdTelephonyPhone = addClientTelephonyPhone();
        // Телефон, для которого есть клики только по номеру Телефонии
        var fourthTelephonyPhone = addClientTelephonyPhone();
        // Телефон, для которого есть клики только по этому номеру
        var fifthTelephonyPhone = addClientTelephonyPhone();

        var originClicksOnPhones = Map.of(
                counterIdWithDomain, Map.of(
                        firstTelephonyPhone.getTelephonyPhone().getPhone(), 2,
                        phoneNumber, 3,
                        secondTelephonyPhone.getTelephonyPhone().getPhone(), 7,
                        thirdTelephonyPhone.getPhoneNumber().getPhone(), 4,
                        thirdTelephonyPhone.getTelephonyPhone().getPhone(), 5,
                        fourthTelephonyPhone.getTelephonyPhone().getPhone(), 10,
                        fifthTelephonyPhone.getTelephonyPhone().getPhone(), 11
                )
        );
        var attributedClicksOnPhones = Map.of(
                counterIdWithDomain, Map.of(
                        phoneNumber, 12, // 2 + 3 + 7
                        thirdTelephonyPhone.getPhoneNumber().getPhone(), 9, // 4 + 5
                        fourthTelephonyPhone.getPhoneNumber().getPhone(), 10, // telephonyPhone -> phoneNumber
                        fifthTelephonyPhone.getPhoneNumber().getPhone(), 11
                )
        );

        var expectedClicksOnPhones = clientPhoneService.attributeClicksOnPhones(
                shard,
                clientId,
                originClicksOnPhones
        );

        SoftAssertions.assertSoftly(sa -> sa.assertThat(attributedClicksOnPhones).isEqualTo(expectedClicksOnPhones));
    }

    private ClientPhone addClientTelephonyPhone(String phoneNumber) {
        var clientPhone = new ClientPhone()
                .withClientId(clientInfo.getClientId())
                .withPhoneType(ClientPhoneType.TELEPHONY)
                .withTelephonyServiceId(String.valueOf(RandomNumberUtils.nextPositiveInteger()))
                .withPhoneNumber(new PhoneNumber().withPhone(phoneNumber))
                .withTelephonyPhone(new PhoneNumber().withPhone(getUniqPhone()))
                .withIsDeleted(false);

        steps.clientPhoneSteps().addPhone(clientId, clientPhone);
        return clientPhone;
    }

    private ClientPhone addClientTelephonyPhone() {
        return addClientTelephonyPhone(getUniqPhone());
    }

    private List<Long> addSpravPhonesToDb(Long permalink) {
        List<CompanyPhone> companyPhones = organizationClient.getCompanyPhones(permalink);
        return clientPhoneRepository.add(clientId, mapList(companyPhones, p ->
                new ClientPhone()
                        .withClientId(clientId)
                        .withPermalinkId(permalink)
                        .withPhoneType(ClientPhoneType.SPRAV)
                        .withPhoneNumber(ClientPhoneUtils.toPhoneNumber(p))
                        .withIsDeleted(false)
        ));
    }

    private void addOrganizationAndPhoneToBanner(Long bannerId, Long clientPhoneId) {
        addOrganizationAndPhoneToBanner(organization, bannerId, clientPhoneId);
    }

    private void addOrganizationAndPhoneToBanner(Organization organization, Long bannerId, Long clientPhoneId) {
        organizationRepository.addOrUpdateOrganizations(shard, singleton(organization));
        organizationRepository.linkOrganizationsToBanners(shard,
                ImmutableMap.of(bannerId, organization.getPermalinkId()), PermalinkAssignType.MANUAL);
        steps.clientPhoneSteps().linkPhoneIdToBanner(shard, bannerId, clientPhoneId);
    }
}
