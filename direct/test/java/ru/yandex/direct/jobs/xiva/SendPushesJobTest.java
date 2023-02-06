package ru.yandex.direct.jobs.xiva;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.xiva.XivaPushTypeInfo;
import ru.yandex.direct.core.entity.xiva.XivaPushesQueueService;
import ru.yandex.direct.core.entity.xiva.model.XivaPushesQueueItem;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.XivaPushesQueuePushType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.xiva.client.XivaClient;
import ru.yandex.direct.xiva.client.model.Push;
import ru.yandex.direct.xiva.client.model.Recipient;
import ru.yandex.direct.xiva.client.model.SendStatus;
import ru.yandex.direct.xiva.client.model.SendStatusList;
import ru.yandex.direct.xiva.client.model.SendStatusSingleOrList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

@JobsTest
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendPushesJobTest {

    public ClientId xivaUserOk;
    public ClientId xivaUserUnavailable;
    public ClientId xivaUserSeveralSubscriptions;
    public ClientId xivaUserOldUnavailable;
    public static final SendStatus XIVA_SEND_STATUS_OK = new SendStatus(200, "", "");
    public static final SendStatus XIVA_SEND_STATUS_UNAVAILABLE = new SendStatus(502, "", "");
    public static final SendStatus XIVA_SEND_STATUS_NO_SUBSCRIPTIONS = new SendStatus(204, "", "");
    public static final XivaPushesQueuePushType FAKE_PUSH = XivaPushesQueuePushType.FAKE_PUSH;
    public static final int MAX_PUSHES_COUNT = 4;

    private static final int SHARD = 1;

    private SendPushesJob job;

    @Autowired
    private XivaClient xivaClient;

    @Autowired
    private XivaPushesQueueService xivaPushesQueueService;

    @Autowired
    public Steps steps;

    @BeforeAll
    public void createClients() {
        xivaUserOk = steps.clientSteps().createDefaultClient().getClientId();
        xivaUserUnavailable = steps.clientSteps().createDefaultClient().getClientId();
        xivaUserSeveralSubscriptions = steps.clientSteps().createDefaultClient().getClientId();
        xivaUserOldUnavailable = steps.clientSteps().createDefaultClient().getClientId();
    }

    @BeforeEach
    void setup() {
        xivaClient = spy(xivaClient);
        Answer<List<SendStatusSingleOrList>> voidAnswer = invocation -> {
            List<Recipient> recipients = invocation.getArgument(0);
            List<SendStatusSingleOrList> result = new ArrayList<>();
            for (Recipient recipient : recipients) {
                String user = recipient.getRecipient().toString();
                if (user.equals(xivaUserOk.toString())) {
                    result.add(XIVA_SEND_STATUS_OK);
                } else if (user.equals(xivaUserUnavailable.toString())
                        || user.equals(xivaUserOldUnavailable.toString())) {
                    result.add(XIVA_SEND_STATUS_UNAVAILABLE);
                } else if (user.equals(xivaUserSeveralSubscriptions.toString())) {
                    result.add(
                            new SendStatusList(
                                    List.of(XIVA_SEND_STATUS_OK, XIVA_SEND_STATUS_UNAVAILABLE)
                            )
                    );
                } else {
                    result.add(XIVA_SEND_STATUS_NO_SUBSCRIPTIONS);
                }
            }
            return result;
        };
        doAnswer(voidAnswer).when(xivaClient).sendBatch(anyList(), any(Push.class), any());

        job = new SendPushesJob(SHARD, xivaPushesQueueService, xivaClient);
    }

    private void executeJob() {
        assertThatCode(() -> job.execute())
                .doesNotThrowAnyException();
    }

    @Test
    void testDeletingSuccessfulFromQueue() {
        xivaPushesQueueService.addPushToQueue(xivaUserOk, FAKE_PUSH);
        executeJob();

        List<XivaPushesQueueItem> pushes = xivaPushesQueueService.getPushes(SHARD, MAX_PUSHES_COUNT);

        boolean noOkPushes = true;
        for (XivaPushesQueueItem item : pushes) {
            if (item.getClientId() == xivaUserOk.asLong()) {
                noOkPushes = false;
                break;
            }
        }
        assertThat(noOkPushes).isTrue();

        xivaPushesQueueService.deletePushFromQueue(SHARD, xivaUserOk, FAKE_PUSH);
    }

    @Test
    void testResendingOneSubscription() {
        xivaPushesQueueService.addPushToQueue(xivaUserUnavailable, FAKE_PUSH);
        executeJob();

        List<XivaPushesQueueItem> pushes = xivaPushesQueueService.getPushes(SHARD, MAX_PUSHES_COUNT);

        boolean hasUnavailable = false;
        for (XivaPushesQueueItem item : pushes) {
            if (item.getClientId() == xivaUserUnavailable.asLong()) {
                hasUnavailable = true;
                break;
            }
        }
        assertThat(hasUnavailable).isTrue();

        xivaPushesQueueService.deletePushFromQueue(SHARD, xivaUserUnavailable, FAKE_PUSH);
    }

    @Test
    void testResendingSeveralSubscriptions() {
        xivaPushesQueueService.addPushToQueue(xivaUserSeveralSubscriptions, FAKE_PUSH);
        executeJob();

        List<XivaPushesQueueItem> pushes = xivaPushesQueueService.getPushes(SHARD, MAX_PUSHES_COUNT);

        boolean hasSeveralSubscription = false;
        for (XivaPushesQueueItem item : pushes) {
            if (item.getClientId() == xivaUserSeveralSubscriptions.asLong()) {
                hasSeveralSubscription = true;
                break;
            }
        }
        assertThat(hasSeveralSubscription).isTrue();

        xivaPushesQueueService.deletePushFromQueue(SHARD, xivaUserSeveralSubscriptions, FAKE_PUSH);
    }

    @Test
    void testDeletingOld() {
        var xivaPushTypeInfoMock = mockStatic(XivaPushTypeInfo.class);
        //noinspection ResultOfMethodCallIgnored
        xivaPushTypeInfoMock.when(() -> XivaPushTypeInfo.getTTL(any())).thenReturn(0);
        xivaPushesQueueService.addPushToQueue(xivaUserOldUnavailable, FAKE_PUSH);
        executeJob();

        List<XivaPushesQueueItem> pushes = xivaPushesQueueService.getPushes(SHARD, MAX_PUSHES_COUNT);

        boolean noOldPushes = true;
        for (XivaPushesQueueItem item : pushes) {
            if (item.getAddTime().isBefore(LocalDateTime.now().minusSeconds(10))) {
                noOldPushes = false;
                break;
            }
        }
        assertThat(noOldPushes).isTrue();

        xivaPushesQueueService.deletePushFromQueue(SHARD, xivaUserUnavailable, FAKE_PUSH);
        xivaPushTypeInfoMock.close();
    }

    @Test
    void testXivaRequestException() {
        // В случае ошибок запросов xivaClient возвращает null
        doReturn(null).when(xivaClient).sendBatch(anyList(), any(Push.class), any());

        xivaPushesQueueService.addPushToQueue(xivaUserOk, FAKE_PUSH);

        // Достаточно успешного выполнения, без исключений
        executeJob();
    }

}
