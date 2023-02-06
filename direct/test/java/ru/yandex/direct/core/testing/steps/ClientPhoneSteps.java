package ru.yandex.direct.core.testing.steps;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhoneType;
import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestClientPhoneRepository;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.ClientId;

import static ru.yandex.direct.core.entity.clientphone.ClientPhoneTestUtils.getUniqPhone;

public class ClientPhoneSteps {

    @Autowired
    private ClientPhoneRepository clientPhoneRepository;
    @Autowired
    private TestClientPhoneRepository testClientPhoneRepository;

    @Autowired
    private OrganizationsClientStub organizationClient;

    public ClientPhone addDefaultClientManualPhone(ClientId clientId) {
        PhoneNumber phoneNumber = new PhoneNumber().withPhone(getUniqPhone()).withExtension(9L);
        return addClientManualPhone(clientId, phoneNumber);
    }

    public ClientPhone addClientManualPhone(ClientId clientId, String phone) {
        return addClientManualPhone(clientId, new PhoneNumber().withPhone(phone));
    }

    public ClientPhone addClientManualPhone(ClientId clientId, PhoneNumber phoneNumber) {
        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(phoneNumber)
                .withComment("Comment")
                .withPhoneType(ClientPhoneType.MANUAL)
                .withIsDeleted(false);
        return addClientManualPhone(clientId, clientPhone);
    }

    public ClientPhone addClientTelephonyPhone(ClientId clientId, PhoneNumber phoneNumber) {
        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(phoneNumber)
                .withComment("Comment")
                .withPhoneType(ClientPhoneType.TELEPHONY)
                .withIsDeleted(false);
        return addPhone(clientId, clientPhone);
    }

    public ClientPhone addCalltrackingOnSitePhone(ClientId clientId, String phone, long counterId) {
        return addCalltrackingOnSitePhone(clientId, phone, counterId, "+78000000000", "1");
    }

    public ClientPhone addCalltrackingOnSitePhone(
            ClientId clientId,
            String phone,
            long counterId,
            LocalDateTime lastShowTime
    ) {
        return addCalltrackingOnSitePhone(clientId,
                phone,
                counterId,
                getUniqPhone(),
                "1",
                lastShowTime);
    }

    public ClientPhone addCalltrackingOnSitePhone(ClientId clientId, String phone, long counterId,
                                                  String telephonyPhone, String telephoneServiceId) {
        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(new PhoneNumber().withPhone(phone))
                .withTelephonyPhone(new PhoneNumber().withPhone(telephonyPhone))
                .withComment("Comment")
                .withPhoneType(ClientPhoneType.TELEPHONY)
                .withTelephonyServiceId(telephoneServiceId)
                .withCounterId(counterId)
                .withPermalinkId(0L)
                .withIsDeleted(false);
        return addPhone(clientId, clientPhone);
    }

    public ClientPhone addCalltrackingOnSitePhone(ClientId clientId,
                                                  String phone,
                                                  long counterId,
                                                  String telephonyPhone,
                                                  String telephoneServiceId,
                                                  LocalDateTime lastShowTime) {
        ClientPhone clientPhone = addCalltrackingOnSitePhone(
                clientId,
                phone,
                counterId,
                telephonyPhone,
                telephoneServiceId)
                .withLastShowTime(lastShowTime);
        return addPhone(clientId, clientPhone);
    }

    public ClientPhone addPhone(ClientId clientId, ClientPhone clientPhone) {
        clientPhoneRepository.add(clientId, List.of(clientPhone));
        return clientPhone;
    }

    public ClientPhone addClientManualPhone(ClientId clientId, ClientPhone clientPhone) {
        clientPhoneRepository.add(clientId, List.of(clientPhone));
        return clientPhone;
    }

    public void linkPhoneIdToBanner(int shard, Long bannerId, Long phoneId) {
        clientPhoneRepository.linkBannerPhones(shard, Map.of(bannerId, phoneId));
    }

    public void linkPhoneIdToCampaign(int shard, Long campaignId, Long phoneId) {
        clientPhoneRepository.linkCampaignPhones(shard, Map.of(campaignId, phoneId));
    }

    public ClientPhone addDefaultClientOrganizationPhone(ClientId clientId) {
        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(new PhoneNumber().withPhone(getUniqPhone()).withExtension(1L))
                .withCounterId(1L)
                .withPermalinkId(1L)
                .withPhoneType(ClientPhoneType.SPRAV)
                .withIsDeleted(false);
        clientPhoneRepository.add(clientId, List.of(clientPhone));
        return clientPhone;
    }

    public ClientPhone addDefaultClientOrganizationPhone(ClientId clientId, Long permalinkId) {
        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(new PhoneNumber().withPhone(getUniqPhone()).withExtension(1L))
                .withCounterId(1L)
                .withPermalinkId(permalinkId)
                .withPhoneType(ClientPhoneType.SPRAV)
                .withIsDeleted(false);
        clientPhoneRepository.add(clientId, List.of(clientPhone));
        return clientPhone;
    }

    public ClientPhone addDefaultClientOrganizationPhone(ClientInfo clientInfo, Long permalinkId) {
        organizationClient.addUidsByPermalinkId(permalinkId, List.of(clientInfo.getUid()));
        return addDefaultClientOrganizationPhone(clientInfo.getClientId(), permalinkId);
    }

    public ClientPhone defaultClientManualPhone(ClientId clientId) {
        return new ClientPhone()
                .withClientId(clientId)
                .withPhoneNumber(new PhoneNumber().withPhone(getUniqPhone()).withExtension(9L))
                .withPhoneType(ClientPhoneType.MANUAL)
                .withIsDeleted(false);
    }

    public Map<ClientId, List<ClientPhone>> getSiteTelephonyByClientId(int shard) {
        return clientPhoneRepository.getSiteTelephonyByClientId(shard, true);
    }

    public void delete(int shard, ClientId clientId) {
        testClientPhoneRepository.delete(shard, clientId);
    }

    public void delete(int shard) {
        testClientPhoneRepository.delete(shard);
    }
}
