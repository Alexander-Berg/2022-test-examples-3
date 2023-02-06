package ru.yandex.direct.jobs.calltracking;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.calltracking.model.CampCalltrackingPhones;
import ru.yandex.direct.core.entity.calltrackingsettings.repository.CalltrackingSettingsRepository;
import ru.yandex.direct.core.entity.campcalltrackingphones.service.CampCalltrackingPhonesService;
import ru.yandex.direct.core.entity.clientphone.ClientPhoneService;
import ru.yandex.direct.core.entity.clientphone.TelephonyPhoneService;
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.telephony.client.TelephonyClient;
import ru.yandex.direct.telephony.client.model.TelephonyPhoneRequest;
import ru.yandex.telephony.backend.lib.proto.telephony_platform.ServiceNumber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@JobsTest
@ExtendWith(SpringExtension.class)
public class SyncCalltrackingTelephonyPhonesJobTest {

    private static final int SHARD = 1;
    private static final long COUNTER_ID = 59049;
    private static final long COUNTER_ID_2 = 117649;
    private static final String DOMAIN_POSTFIX = "yandex-team.ru";
    private static final String PHONE_1 = "+74950350365";
    private static final String PHONE_2 = "+74990350365";
    private static final String TELEPHONY_PHONE = "+77007007070";
    private static final String TELEPHONY_SERVICE_ID = "0A";

    @Autowired
    private Steps steps;
    @Autowired
    private CalltrackingSettingsRepository calltrackingSettingsRepository;
    @Autowired
    private CampCalltrackingPhonesService campCalltrackingPhonesService;
    @Autowired
    private ClientPhoneService clientPhoneService;
    @Autowired
    private TelephonyPhoneService telephonyPhoneService;
    @Autowired
    private TelephonyClient telephonyClient;

    private SyncCalltrackingTelephonyPhonesJob syncCalltrackingTelephonyPhonesJob;

    private ClientId clientId;
    private long campaignId;
    private long domainId;
    private long calltrackingSettingsId;
    private ClientInfo clientInfo;

    @BeforeEach
    void init() {
        syncCalltrackingTelephonyPhonesJob = spy(new SyncCalltrackingTelephonyPhonesJob(
                SHARD, calltrackingSettingsRepository, campCalltrackingPhonesService,
                clientPhoneService, telephonyPhoneService
        ));

        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();
        domainId = steps.domainSteps().createDomain(SHARD, DOMAIN_POSTFIX).getDomainId();
        calltrackingSettingsId = steps.calltrackingSettingsSteps()
                .add(clientId, domainId, COUNTER_ID, List.of(PHONE_1));
        steps.campCalltrackingSettingsSteps().link(SHARD, campaignId, calltrackingSettingsId);
    }


    @AfterEach
    private void cleanUp() {
        clearInvocations(telephonyClient);
        // подчистим инфу о выданных на кампании подменниках и установленных на кампании настройках колтрекинга на сайте
        steps.campCalltrackingPhonesSteps().deleteAll(SHARD);
        steps.campCalltrackingSettingsSteps().deleteAll(SHARD);
        steps.clientPhoneSteps().delete(SHARD, clientId);
        steps.calltrackingSettingsSteps().updateIsCounterAvailable(SHARD, calltrackingSettingsId, true);
    }

    @Test
    void doNothingOnGoodSettings() {
        ClientPhone clientPhone = steps.clientPhoneSteps()
                .addCalltrackingOnSitePhone(clientId, PHONE_1, COUNTER_ID);
        steps.campCalltrackingPhonesSteps().add(SHARD, clientPhone.getId(), campaignId);

        syncCalltrackingTelephonyPhonesJob.execute();

        verifyZeroInteractions(telephonyClient);
        testCampCalltrackingPhones(List.of(new CampCalltrackingPhones()
                .withClientPhoneId(clientPhone.getId())
                .withCid(campaignId)
        ));
    }

    @Test
    void addPhone() {
        when(telephonyClient.getServiceNumber()).thenReturn(ServiceNumber.newBuilder()
                .setNum(TELEPHONY_PHONE)
                .setServiceNumberID(TELEPHONY_SERVICE_ID)
                .build()
        );
        when(telephonyClient.getClientServiceNumbers(clientId.asLong())).thenReturn(List.of(
                ServiceNumber.newBuilder()
                        .setNum(TELEPHONY_PHONE)
                        .setServiceNumberID(TELEPHONY_SERVICE_ID)
                        .setVersion(0)
                        .putMeta("counterID", Long.toString(COUNTER_ID))
                        .putMeta("orgID", "0")
                        .build()
        ));

        syncCalltrackingTelephonyPhonesJob.execute();

        var clientPhones = steps.clientPhoneSteps().getSiteTelephonyByClientId(SHARD);
        assertThat(clientPhones.get(clientId).size()).isEqualTo(1);
        ClientPhone clientPhone = clientPhones.get(clientId).get(0);
        assertThat(clientPhone.getTelephonyPhone().getPhone()).isEqualTo(TELEPHONY_PHONE);
        assertThat(clientPhone.getPhoneNumber().getPhone()).isEqualTo(PHONE_1);
        assertThat(clientPhone.getTelephonyServiceId()).isEqualTo(TELEPHONY_SERVICE_ID);

        verify(telephonyClient).linkServiceNumber(
                clientId.asLong(),
                new TelephonyPhoneRequest()
                        .withRedirectPhone(PHONE_1)
                        .withTelephonyServiceId(TELEPHONY_SERVICE_ID)
                        .withCounterId(COUNTER_ID)
                        .withPermalinkId(0L),
                0
        );
        testCampCalltrackingPhones(List.of(
                new CampCalltrackingPhones()
                        .withClientPhoneId(clientPhone.getId())
                        .withCid(campaignId)
        ));
    }

    @Test
    void addTwoPhones_onlyOneIsInThePool() {
        when(telephonyClient.getServiceNumber())
                .thenReturn(ServiceNumber.newBuilder()
                        .setNum(TELEPHONY_PHONE)
                        .setServiceNumberID(TELEPHONY_SERVICE_ID)
                        .build()
                )
                .thenReturn(null);
        when(telephonyClient.getClientServiceNumbers(clientId.asLong())).thenReturn(List.of(ServiceNumber.newBuilder()
                .setNum(TELEPHONY_PHONE)
                .setServiceNumberID(TELEPHONY_SERVICE_ID)
                .setVersion(0)
                .putMeta("counterID", Long.toString(COUNTER_ID))
                .putMeta("orgID", "0")
                .build()
        ));

        // Добавим вторую кампанию, чтобы джобе понадобилось аллоцировать два подменника
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();
        steps.campCalltrackingSettingsSteps().link(SHARD, campaignId, calltrackingSettingsId);

        syncCalltrackingTelephonyPhonesJob.execute();

        // проверим, что нужный подменник в базе сохранился
        var clientPhones = steps.clientPhoneSteps().getSiteTelephonyByClientId(SHARD);
        assertThat(clientPhones.get(clientId).size()).isEqualTo(1);
        ClientPhone clientPhone = clientPhones.get(clientId).get(0);
        assertThat(clientPhone.getTelephonyPhone().getPhone()).isEqualTo(TELEPHONY_PHONE);
        assertThat(clientPhone.getPhoneNumber().getPhone()).isEqualTo(PHONE_1);
        assertThat(clientPhone.getTelephonyServiceId()).isEqualTo(TELEPHONY_SERVICE_ID);
    }

    @Test
    void updateCounterId() {
        ClientPhone clientPhone = steps.clientPhoneSteps()
                .addCalltrackingOnSitePhone(clientId, PHONE_1, COUNTER_ID_2, TELEPHONY_PHONE, TELEPHONY_SERVICE_ID);
        steps.campCalltrackingPhonesSteps().add(SHARD, clientPhone.getId(), campaignId);

        when(telephonyClient.getServiceNumber()).thenReturn(ServiceNumber.newBuilder()
                .setNum(TELEPHONY_PHONE)
                .setServiceNumberID(TELEPHONY_SERVICE_ID)
                .build()
        );

        syncCalltrackingTelephonyPhonesJob.execute();

        var clientPhones = steps.clientPhoneSteps().getSiteTelephonyByClientId(SHARD);
        assertThat(clientPhones.get(clientId).size()).isEqualTo(1);
        clientPhone = clientPhones.get(clientId).get(0);
        assertThat(clientPhone.getPhoneNumber().getPhone()).isEqualTo(PHONE_1);
        assertThat(clientPhone.getCounterId()).isEqualTo(COUNTER_ID);

        verify(telephonyClient).linkServiceNumber(clientId.asLong(), new TelephonyPhoneRequest()
                .withRedirectPhone(PHONE_1)
                .withTelephonyServiceId(TELEPHONY_SERVICE_ID)
                .withCounterId(COUNTER_ID)
                .withPermalinkId(0L)
        );
        testCampCalltrackingPhones(List.of(
                new CampCalltrackingPhones()
                        .withClientPhoneId(clientPhone.getId())
                        .withCid(campaignId)
        ));
    }

    @Test
    void deleteClientPhone() {
        ClientPhone clientPhoneToKeep = steps.clientPhoneSteps()
                .addCalltrackingOnSitePhone(clientId, PHONE_1, COUNTER_ID);
        ClientPhone clientPhoneToDelete = steps.clientPhoneSteps()
                .addCalltrackingOnSitePhone(clientId, PHONE_2, COUNTER_ID);
        steps.campCalltrackingPhonesSteps().add(SHARD, clientPhoneToKeep.getId(), campaignId);
        steps.campCalltrackingPhonesSteps().add(SHARD, clientPhoneToDelete.getId(), campaignId);

        syncCalltrackingTelephonyPhonesJob.execute();

        var clientPhones = steps.clientPhoneSteps().getSiteTelephonyByClientId(SHARD);
        assertThat(clientPhones.get(clientId).size()).isEqualTo(2);
        ClientPhone clientPhone = clientPhones.get(clientId).get(0);
        assertThat(clientPhone.getPhoneNumber().getPhone()).isEqualTo(PHONE_1);
        assertThat(clientPhone.getIsDeleted()).isFalse();
        clientPhone = clientPhones.get(clientId).get(1);
        assertThat(clientPhone.getPhoneNumber().getPhone()).isEqualTo(PHONE_2);
        assertThat(clientPhone.getIsDeleted()).isTrue();

        verifyZeroInteractions(telephonyClient);
        testCampCalltrackingPhones(List.of(new CampCalltrackingPhones()
                .withClientPhoneId(clientPhoneToKeep.getId())
                .withCid(campaignId)
        ));
    }

    @Test
    void deleteClientPhoneForNotAvailableCounter() {
        steps.calltrackingSettingsSteps().updateIsCounterAvailable(SHARD, calltrackingSettingsId, false);

        ClientPhone clientPhone = steps.clientPhoneSteps()
                .addCalltrackingOnSitePhone(clientId, PHONE_1, COUNTER_ID);
        steps.campCalltrackingPhonesSteps().add(SHARD, clientPhone.getId(), campaignId);

        syncCalltrackingTelephonyPhonesJob.execute();

        var clientPhones = steps.clientPhoneSteps().getSiteTelephonyByClientId(SHARD);
        assertThat(clientPhones.get(clientId).size()).isEqualTo(1);
        ClientPhone gotClientPhone = clientPhones.get(clientId).get(0);
        assertThat(gotClientPhone.getIsDeleted()).isTrue();

        // отвязывается отдельной джобой
        verifyZeroInteractions(telephonyClient);
        testCampCalltrackingPhones(null);
    }

    private void testCampCalltrackingPhones(List<CampCalltrackingPhones> expected) {
        List<CampCalltrackingPhones> actual =
                steps.campCalltrackingPhonesSteps().getByCampaignId(SHARD).get(campaignId);
        assertThat(expected).isEqualTo(actual);
    }
}
