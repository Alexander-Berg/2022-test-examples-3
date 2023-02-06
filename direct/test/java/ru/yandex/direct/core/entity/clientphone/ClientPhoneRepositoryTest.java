package ru.yandex.direct.core.entity.clientphone;

import java.util.List;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhoneType;
import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.emptyList;
import static org.assertj.core.util.Preconditions.checkState;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientPhoneRepositoryTest {

    @Autowired
    private ClientPhoneRepository clientPhoneRepository;

    @Autowired
    private Steps steps;

    @Test
    public void add_success() {
        ClientId clientId = steps.clientSteps().createDefaultClient().getClientId();
        ClientPhone clientPhone = new ClientPhone()
                .withPhoneNumber(new PhoneNumber().withPhone("+71111111111").withExtension(9L))
                .withClientId(clientId)
                .withPhoneType(ClientPhoneType.MANUAL)
                .withCounterId(2L)
                .withPermalinkId(3L)
                .withTelephonyServiceId(UUID.randomUUID().toString())
                .withIsDeleted(false);
        clientPhoneRepository.add(clientId, List.of(clientPhone));

        List<ClientPhone> clientPhones = clientPhoneRepository.getAllClientPhones(clientId, emptyList());
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(clientPhones).hasSize(1);
            ClientPhone actual = clientPhones.get(0);
            sa.assertThat(actual.getId()).isNotNull();
            sa.assertThat(actual.getPhoneType()).isEqualTo(ClientPhoneType.MANUAL);
            sa.assertThat(actual.getPhoneNumber()).isEqualTo(clientPhone.getPhoneNumber());
            sa.assertThat(actual.getCounterId()).isEqualTo(clientPhone.getCounterId());
            sa.assertThat(actual.getPermalinkId()).isEqualTo(clientPhone.getPermalinkId());
            sa.assertThat(actual.getTelephonyServiceId()).isEqualTo(clientPhone.getTelephonyServiceId());
            sa.assertThat(actual.getIsDeleted()).isEqualTo(clientPhone.getIsDeleted());
        });
    }

    @Test
    public void update_success() {
        ClientInfo defaultClient = steps.clientSteps().createDefaultClient();
        ClientId clientId = defaultClient.getClientId();
        ClientPhone clientPhone = new ClientPhone()
                .withPhoneNumber(new PhoneNumber().withPhone("+71111111111").withExtension(8L))
                .withClientId(clientId)
                .withPhoneType(ClientPhoneType.MANUAL)
                .withCounterId(2L)
                .withPermalinkId(3L)
                .withTelephonyServiceId(UUID.randomUUID().toString());
        Long phoneId = clientPhoneRepository.add(clientId, List.of(clientPhone)).get(0);

        ClientPhone newPhone = new ClientPhone()
                .withId(phoneId)
                .withPhoneNumber(new PhoneNumber().withPhone("+71111111222").withExtension(9L))
                .withClientId(clientId)
                .withCounterId(22L)
                .withPermalinkId(33L)
                .withTelephonyServiceId(UUID.randomUUID().toString())
                .withIsDeleted(true);
        AppliedChanges<ClientPhone> appliedChanges = getAppliedChanges(clientPhone, newPhone);
        clientPhoneRepository.update(defaultClient.getShard(), List.of(appliedChanges));

        List<ClientPhone> clientPhones = clientPhoneRepository.getAllClientPhones(clientId, emptyList());
        SoftAssertions.assertSoftly(sa -> {
            sa.assertThat(clientPhones).hasSize(1);
            ClientPhone actual = clientPhones.get(0);
            sa.assertThat(actual.getId()).isNotNull();
            sa.assertThat(actual.getPhoneType()).isEqualTo(ClientPhoneType.MANUAL);
            sa.assertThat(actual.getPhoneNumber()).isEqualTo(newPhone.getPhoneNumber());
            sa.assertThat(actual.getCounterId()).isEqualTo(newPhone.getCounterId());
            sa.assertThat(actual.getPermalinkId()).isEqualTo(newPhone.getPermalinkId());
            sa.assertThat(actual.getTelephonyServiceId()).isEqualTo(newPhone.getTelephonyServiceId());
            sa.assertThat(actual.getIsDeleted()).isEqualTo(newPhone.getIsDeleted());
        });
    }

    @Test
    public void delete_success() {
        ClientId clientId = steps.clientSteps().createDefaultClient().getClientId();
        ClientPhone clientPhone = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId);
        List<ClientPhone> clientPhonesCheck = clientPhoneRepository.getAllClientPhones(clientId, emptyList());
        checkState(clientPhonesCheck.get(0).getId().equals(clientPhone.getId()), "Номер успешно добавлен в базу");

        clientPhoneRepository.delete(clientId, List.of(clientPhone.getId()));

        List<ClientPhone> clientPhones = clientPhoneRepository.getAllClientPhones(clientId, emptyList());
        SoftAssertions.assertSoftly(sa -> sa.assertThat(clientPhones).isEmpty());
    }

    private AppliedChanges<ClientPhone> getAppliedChanges(ClientPhone before, ClientPhone after) {
        return new ModelChanges<>(after.getId(), ClientPhone.class)
                .processNotNull(after.getPhoneNumber(), ClientPhone.PHONE_NUMBER)
                .processNotNull(after.getPermalinkId(), ClientPhone.PERMALINK_ID)
                .processNotNull(after.getCounterId(), ClientPhone.COUNTER_ID)
                .processNotNull(after.getTelephonyServiceId(), ClientPhone.TELEPHONY_SERVICE_ID)
                .processNotNull(after.getComment(), ClientPhone.COMMENT)
                .processNotNull(after.getIsDeleted(), ClientPhone.IS_DELETED)
                .applyTo(before);
    }

}
