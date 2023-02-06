package ru.yandex.direct.jobs.telephony;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.clientphone.ClientPhoneService;
import ru.yandex.direct.core.entity.clientphone.ClientPhoneTestUtils;
import ru.yandex.direct.core.entity.clientphone.TelephonyPhoneService;
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhoneType;
import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.telephony.client.TelephonyClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

@JobsTest
@ExtendWith(SpringExtension.class)
public class TelephonyDeletedPhonesDetacherJobTest {

    private static final int SHARD = 1;
    private static final String PHONE_1 = ClientPhoneTestUtils.getUniqPhone();
    private static final String PHONE_2 = ClientPhoneTestUtils.getUniqPhone();
    private static final String PHONE_3 = ClientPhoneTestUtils.getUniqPhone();
    private static final String TELEPHONY_PHONE_1 = ClientPhoneTestUtils.getUniqPhone();
    private static final String TELEPHONY_PHONE_2 = ClientPhoneTestUtils.getUniqPhone();
    private static final String TELEPHONY_SERVICE_ID_1 = "0A";
    private static final String TELEPHONY_SERVICE_ID_2 = "1A";
    private static final LocalDateTime NOW = LocalDateTime.now();

    @Autowired
    private Steps steps;
    @Autowired
    private ClientPhoneRepository clientPhoneRepository;
    @Autowired
    private ClientPhoneService clientPhoneService;
    @Autowired
    private TelephonyPhoneService telephonyPhoneService;
    @Autowired
    private TelephonyClient telephonyClient;

    private TelephonyDeletedPhonesDetacherJob telephonyDeletedPhonesDetacherJob;

    private ClientId clientId;
    private ClientInfo clientInfo;

    @BeforeEach
    void init() {
        telephonyPhoneService = spy(telephonyPhoneService);
        telephonyDeletedPhonesDetacherJob = spy(new TelephonyDeletedPhonesDetacherJob(
                SHARD, clientPhoneRepository, telephonyPhoneService
        ));
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
    }

    @AfterEach
    private void cleanUp() {
        clearInvocations(telephonyClient);
        steps.clientPhoneSteps().delete(SHARD, clientId);
    }

    @Test
    void doNothingWhenLastShowTimeTooEarly() {
        ClientPhone phone1 = addAttachedPhone(
                PHONE_1,
                TELEPHONY_PHONE_1,
                TELEPHONY_SERVICE_ID_1,
                NOW.minusHours(2),
                true);
        ClientPhone phone2 = addAttachedPhone(
                PHONE_2,
                TELEPHONY_PHONE_2,
                TELEPHONY_SERVICE_ID_2,
                NOW.minusHours(3),
                true);
        List<Long> phoneIds = List.of(phone1.getId(), phone2.getId());

        telephonyDeletedPhonesDetacherJob.execute();

        verifyZeroInteractions(telephonyClient);
        List<ClientPhone> clientPhonesInDb = clientPhoneService.getByPhoneIds(clientId, phoneIds);
        Map<Long, ClientPhone> clientPhoneMap = listToMap(clientPhonesInDb, ClientPhone::getId);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(clientPhonesInDb).hasSize(2);
            soft.assertThat(clientPhoneMap.get(phone1.getId()).getPhoneNumber())
                    .isEqualTo(phone1.getPhoneNumber());
            soft.assertThat(clientPhoneMap.get(phone1.getId()).getTelephonyPhone())
                    .isEqualTo(phone1.getTelephonyPhone());
            soft.assertThat(clientPhoneMap.get(phone2.getId()).getPhoneNumber())
                    .isEqualTo(phone2.getPhoneNumber());
            soft.assertThat(clientPhoneMap.get(phone2.getId()).getTelephonyPhone())
                    .isEqualTo(phone2.getTelephonyPhone());
        });
    }

    @Test
    void doNothingWhenIsDeletedIsFalse() {
        ClientPhone phone1 = addAttachedPhone(
                PHONE_1,
                TELEPHONY_PHONE_1,
                TELEPHONY_SERVICE_ID_1,
                NOW.minusHours(5),
                false);
        ClientPhone phone2 = addAttachedPhone(
                PHONE_2,
                TELEPHONY_PHONE_2,
                TELEPHONY_SERVICE_ID_2,
                NOW.minusHours(6),
                false);
        List<Long> phoneIds = List.of(phone1.getId(), phone2.getId());

        telephonyDeletedPhonesDetacherJob.execute();

        verifyZeroInteractions(telephonyClient);
        List<ClientPhone> clientPhonesInDb = clientPhoneService.getByPhoneIds(clientId, phoneIds);
        Map<Long, ClientPhone> clientPhoneMap = listToMap(clientPhonesInDb, ClientPhone::getId);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(clientPhonesInDb.size()).isEqualTo(2);
            soft.assertThat(clientPhoneMap.get(phone1.getId()).getPhoneNumber())
                    .isEqualTo(phone1.getPhoneNumber());
            soft.assertThat(clientPhoneMap.get(phone1.getId()).getTelephonyPhone())
                    .isEqualTo(phone1.getTelephonyPhone());
            soft.assertThat(clientPhoneMap.get(phone2.getId()).getPhoneNumber())
                    .isEqualTo(phone2.getPhoneNumber());
            soft.assertThat(clientPhoneMap.get(phone2.getId()).getTelephonyPhone())
                    .isEqualTo(phone2.getTelephonyPhone());
        });
    }

    @Test
    void detachOnlyOne() {
        ClientPhone phone1 = addAttachedPhone(
                PHONE_1,
                TELEPHONY_PHONE_1,
                TELEPHONY_SERVICE_ID_1,
                NOW.minusHours(5),
                true);
        ClientPhone phone2 = addAttachedPhone(
                PHONE_2,
                TELEPHONY_PHONE_2,
                TELEPHONY_SERVICE_ID_2,
                NOW.minusHours(1),
                true);
        List<Long> phoneIds = List.of(phone1.getId(), phone2.getId());

        telephonyDeletedPhonesDetacherJob.execute();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telephonyClient, times(1))
                .unlinkServiceNumber(captor.capture(), eq(true));
        List<String> tryDetachServiceNumberIds = captor.getAllValues();
        List<ClientPhone> clientPhonesInDb = clientPhoneService.getByPhoneIds(clientId, phoneIds);
        Map<Long, ClientPhone> clientPhoneMap = listToMap(clientPhonesInDb, ClientPhone::getId);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(tryDetachServiceNumberIds).hasSize(1);
            soft.assertThat(tryDetachServiceNumberIds).contains(phone1.getTelephonyServiceId());
            soft.assertThat(clientPhonesInDb).hasSize(1);
            soft.assertThat(clientPhoneMap.get(phone2.getId()).getPhoneNumber())
                    .isEqualTo(phone2.getPhoneNumber());
            soft.assertThat(clientPhoneMap.get(phone2.getId()).getTelephonyPhone())
                    .isEqualTo(phone2.getTelephonyPhone());
        });
    }

    @Test
    void executeSuccess() {
        ClientPhone phone1 = addAttachedPhone(
                PHONE_1,
                TELEPHONY_PHONE_1,
                TELEPHONY_SERVICE_ID_1,
                NOW.minusHours(5),
                true);
        ClientPhone phone2 = addAttachedPhone(
                PHONE_2,
                TELEPHONY_PHONE_2,
                TELEPHONY_SERVICE_ID_2,
                NOW.minusHours(6),
                true);
        addDetachedPhone(PHONE_3, NOW.minusHours(7));

        telephonyDeletedPhonesDetacherJob.execute();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(telephonyClient, times(2))
                .unlinkServiceNumber(captor.capture(), eq(true));
        List<String> tryDetachServiceNumberIds = captor.getAllValues();
        List<Long> phoneIds = List.of(phone1.getId(), phone2.getId());
        List<ClientPhone> dbPhones = clientPhoneService.getByPhoneIds(clientId, phoneIds);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(tryDetachServiceNumberIds).hasSize(2);
            soft.assertThat(tryDetachServiceNumberIds).contains(phone1.getTelephonyServiceId());
            soft.assertThat(tryDetachServiceNumberIds).contains(phone2.getTelephonyServiceId());
            soft.assertThat(dbPhones).isEmpty();
        });
    }

    private ClientPhone addDetachedPhone(String phone, LocalDateTime lastShowTime) {
        ClientPhone clientPhone = createPhone(phone, lastShowTime, true);
        return steps.clientPhoneSteps().addPhone(clientId, clientPhone);
    }

    private ClientPhone addAttachedPhone(String phone,
                                         String telephonyPhone,
                                         String telephonyServiceId,
                                         LocalDateTime lastShowTime,
                                         boolean isDeleted) {
        ClientPhone clientPhone = createPhone(phone, lastShowTime, isDeleted);
        clientPhone.withTelephonyPhone(new PhoneNumber().withPhone(telephonyPhone))
                .withTelephonyServiceId(telephonyServiceId);
        return steps.clientPhoneSteps().addPhone(clientId, clientPhone);
    }

    private ClientPhone createPhone(String phone, LocalDateTime lastShowTime, boolean isDeleted) {
        return new ClientPhone()
                .withClientId(clientId)
                .withPhoneType(ClientPhoneType.TELEPHONY)
                .withPhoneNumber(new PhoneNumber().withPhone(phone))
                .withLastShowTime(lastShowTime)
                .withIsDeleted(isDeleted);
    }

}
