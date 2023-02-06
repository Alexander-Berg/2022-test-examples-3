package ru.yandex.direct.jobs.telephony;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.clientphone.ClientPhoneService;
import ru.yandex.direct.core.entity.clientphone.TelephonyPhoneService;
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository;
import ru.yandex.direct.core.entity.metrika.repository.CalltrackingNumberCallsRepository;
import ru.yandex.direct.core.entity.metrika.repository.CalltrackingNumberClicksRepository;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhoneType;
import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

@JobsTest
@ExtendWith(SpringExtension.class)
public class TelephonyCalcLastShowTimeJobTest {

    private static final String PHONE_1 = "+74950350365";
    private static final String PHONE_2 = "+74990350365";
    private static final String PHONE_3 = "+75000350365";
    private static final String TELEPHONY_PHONE_1 = "+77007007070";
    private static final String TELEPHONY_PHONE_2 = "+77007007071";
    private static final String TELEPHONY_SERVICE_ID_1 = "0A";
    private static final String TELEPHONY_SERVICE_ID_2 = "1A";
    private static final Long COUNTER_ID_1 = 111L;
    private static final Long COUNTER_ID_2 = 222L;
    private static final Long COUNTER_ID_3 = 333L;
    private static final LocalDateTime NOW = LocalDateTime.now();

    @Autowired
    private Steps steps;
    @Autowired
    private ClientPhoneRepository clientPhoneRepository;
    @Autowired
    private ClientPhoneService clientPhoneService;
    @Autowired
    private TelephonyPhoneService telephonyPhoneService;
    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private ShardHelper shardHelper;
    @Mock
    private CalltrackingNumberClicksRepository calltrackingNumberClicksRepository;
    @Mock
    private CalltrackingNumberCallsRepository calltrackingNumberCallsRepository;

    private TelephonyCalcLastShowTimeJob telephonyCalcLastShowTimeJob;

    private ClientId clientId;
    private ClientInfo clientInfo;

    @BeforeEach
    void init() {
        PpcProperty<Boolean> enabledProperty = mock(PpcProperty.class);
        telephonyPhoneService = spy(telephonyPhoneService);
        doReturn(true).when(enabledProperty).getOrDefault(any());
        doReturn(enabledProperty).when(ppcPropertiesSupport).get(PpcPropertyNames.TELEPHONY_CALC_LAST_SHOW_TIME_ENABLED);

        PpcProperty<Integer> daysProperty = mock(PpcProperty.class);
        doReturn(30).when(daysProperty).getOrDefault(any());
        doReturn(daysProperty).when(ppcPropertiesSupport)
                .get(PpcPropertyNames.MAX_DAYS_WITHOUT_ACTIONS_FOR_SITE_TELEPHONY_PHONE);

        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        clientPhoneRepository = spy(clientPhoneRepository);
        telephonyCalcLastShowTimeJob = new TelephonyCalcLastShowTimeJob(
                clientPhoneRepository, telephonyPhoneService, ppcPropertiesSupport,
                shardHelper, calltrackingNumberClicksRepository, calltrackingNumberCallsRepository
        );
        steps.clientPhoneSteps().delete(clientInfo.getShard());
        steps.campCalltrackingPhonesSteps().deleteAll(clientInfo.getShard());
    }

    @AfterEach
    private void cleanUp() {
        steps.clientPhoneSteps().delete(clientInfo.getShard());
        steps.campCalltrackingPhonesSteps().deleteAll(clientInfo.getShard());
    }

    @Test
    void doNothingWhenNoClicksAndCalls() {
        List<ClientPhone> phones = prepareTest(25, 0, 29, 29);
        telephonyCalcLastShowTimeJob.execute();
        doPhonesInDbAsserts(phones, false, false, false);
    }

    @Test
    void updateOnlyFirstWhenLastCallTimeAfterCurrentLastShowTime() {
        List<ClientPhone> phones = prepareTest(25, 29, 29, 29);
        doReturn(Map.of(phones.get(0).getTelephonyPhone().getPhone(), NOW.minusDays(25)))
                .when(calltrackingNumberCallsRepository)
                .getLastCallTimesByPhones(any(), any(), any());
        telephonyCalcLastShowTimeJob.execute();
        doPhonesInDbAsserts(phones, true, false, false);
    }

    @Test
    void updateOnlyFirstWhenLastClickTimeAfterCurrentLastShowTime() {
        List<ClientPhone> phones = prepareTest(25, 29, 29, 29);
        doReturn(Map.of(phones.get(0).getTelephonyPhone().getPhone(), NOW.minusDays(25)))
                .when(calltrackingNumberClicksRepository)
                .getLastClickTimesByPhones(any(), any());
        telephonyCalcLastShowTimeJob.execute();
        doPhonesInDbAsserts(phones, true, false, false);
    }

    @Test
    void updateAllWhenLastCallTimeAfterCurrentLastShowTime() {
        List<ClientPhone> phones = prepareTest(25, 29, 29, 29);
        doReturn(Map.of(phones.get(0).getTelephonyPhone().getPhone(), NOW.minusDays(25),
                phones.get(1).getTelephonyPhone().getPhone(), NOW.minusDays(24)))
                .when(calltrackingNumberCallsRepository)
                .getLastCallTimesByPhones(any(), any(), any());
        telephonyCalcLastShowTimeJob.execute();
        doPhonesInDbAsserts(phones, true, true, false);
    }

    @Test
    void updateAllWhenLastClickTimeAfterCurrentLastShowTime() {
        List<ClientPhone> phones = prepareTest(25, 29, 29, 29);
        doReturn(
                Map.of(
                        phones.get(0).getTelephonyPhone().getPhone(),
                        NOW.minusDays(25), phones.get(1).getTelephonyPhone().getPhone(),
                        NOW.minusDays(24), phones.get(2).getPhoneNumber().getPhone(), NOW.minusDays(23)))
                .when(calltrackingNumberClicksRepository)
                .getLastClickTimesByPhones(any(), any());
        telephonyCalcLastShowTimeJob.execute();
        doPhonesInDbAsserts(phones, true, true, true);
    }

    private ClientPhone addTelephonyPhone(String phone,
                                          String telephonyPhone,
                                          String telephonyServiceId,
                                          Long counterId,
                                          LocalDateTime lastShowTime) {
        ClientPhone clientPhone = new ClientPhone()
                .withClientId(clientId)
                .withPhoneType(ClientPhoneType.TELEPHONY)
                .withPhoneNumber(new PhoneNumber().withPhone(phone))
                .withTelephonyPhone(telephonyPhone == null ? null : new PhoneNumber().withPhone(telephonyPhone))
                .withCounterId(counterId)
                .withPermalinkId(0L)
                .withTelephonyServiceId(telephonyServiceId)
                .withLastShowTime(lastShowTime)
                .withIsDeleted(false);
        return steps.clientPhoneSteps().addPhone(clientId, clientPhone);
    }

    private List<ClientPhone> prepareTest(
            long jobLastTime,
            long firstDaysBeforeNow,
            long secondDaysBeforeNow,
            long thirdDaysBeforeNow
    ) {
        PpcProperty<Integer> property = mock(PpcProperty.class);
        doReturn(NOW.minusHours(jobLastTime)).when(property).get();
        doReturn(property).when(ppcPropertiesSupport)
                .get(PpcPropertyNames.TELEPHONY_CALC_LAST_SHOW_TIME_JOB_LAST_TIME);
        ClientPhone phone1 = addTelephonyPhone(PHONE_1, TELEPHONY_PHONE_1, TELEPHONY_SERVICE_ID_1, COUNTER_ID_1,
                NOW.minusDays(firstDaysBeforeNow).withNano(0));
        ClientPhone phone2 = addTelephonyPhone(PHONE_2, TELEPHONY_PHONE_2, TELEPHONY_SERVICE_ID_2, COUNTER_ID_2,
                NOW.minusDays(secondDaysBeforeNow).withNano(0));
        ClientPhone phone3 = addTelephonyPhone(PHONE_3, null, "", COUNTER_ID_3,
                NOW.minusDays(thirdDaysBeforeNow).withNano(0));
        return List.of(phone1, phone2, phone3);
    }

    private void doPhonesInDbAsserts(
            List<ClientPhone> phones,
            boolean firstUpdated,
            boolean secondUpdated,
            boolean thirdUpdated
    ) {
        ClientPhone phone1 = phones.get(0);
        ClientPhone phone2 = phones.get(1);
        ClientPhone phone3 = phones.get(2);
        List<Long> phoneIds = List.of(phone1.getId(), phone2.getId(), phone3.getId());

        List<ClientPhone> clientPhonesInDb = clientPhoneService.getByPhoneIds(clientId, phoneIds);
        Map<Long, ClientPhone> clientPhoneMap = listToMap(clientPhonesInDb, ClientPhone::getId);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(clientPhonesInDb).hasSize(3);
            soft.assertThat(clientPhoneMap.get(phone1.getId()).getPhoneNumber()).isEqualTo(phone1.getPhoneNumber());
            if (firstUpdated) {
                soft.assertThat(clientPhoneMap.get(phone1.getId()).getLastShowTime()).isAfter(phone1.getLastShowTime());
            } else {
                soft.assertThat(clientPhoneMap.get(phone1.getId()).getLastShowTime()).isEqualTo(phone1.getLastShowTime());
            }
            soft.assertThat(clientPhoneMap.get(phone2.getId()).getPhoneNumber()).isEqualTo(phone2.getPhoneNumber());
            if (secondUpdated) {
                soft.assertThat(clientPhoneMap.get(phone2.getId()).getLastShowTime()).isAfter(phone2.getLastShowTime());
            } else {
                soft.assertThat(clientPhoneMap.get(phone2.getId()).getLastShowTime()).isEqualTo(phone2.getLastShowTime());
            }
            if (thirdUpdated) {
                soft.assertThat(clientPhoneMap.get(phone3.getId()).getLastShowTime()).isAfter(phone3.getLastShowTime());
            } else {
                soft.assertThat(clientPhoneMap.get(phone3.getId()).getLastShowTime()).isEqualTo(phone3.getLastShowTime());
            }
        });
    }

}
