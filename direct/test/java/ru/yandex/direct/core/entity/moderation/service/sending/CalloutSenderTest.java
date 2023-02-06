package ru.yandex.direct.core.entity.moderation.service.sending;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.banner.model.CalloutWithModerationInfo;
import ru.yandex.direct.core.entity.moderation.model.callout.CalloutModerationRequest;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCalloutRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.AdditionsItemCalloutsStatusmoderate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.dbschema.ppc.enums.AdditionsItemCalloutsStatusmoderate.Ready;
import static ru.yandex.direct.dbschema.ppc.enums.AdditionsItemCalloutsStatusmoderate.Sending;
import static ru.yandex.direct.dbschema.ppc.enums.AdditionsItemCalloutsStatusmoderate.Sent;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CalloutSenderTest {
    @Autowired
    private Steps steps;

    @Autowired
    private CalloutSender calloutSender;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private TestCalloutRepository testCalloutRepository;

    private int shard;
    Callout callout;
    Callout callout2;

    @Before
    public void before() throws IOException {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        callout = steps.calloutSteps().createCalloutWithText(clientInfo, "CalloutText");
        callout2 = steps.calloutSteps().createCalloutWithText(clientInfo, "CalloutText2");
    }

    @Test
    public void createNewVersionTest() {
        calloutSender.send(shard, List.of(callout.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        TestModerationRepository.ModerationVersion version =
                testModerationRepository.getCalloutVersionObj(shard, callout.getId());
        assertThat(version.getVersion()).isEqualTo(CalloutSender.INITIAL_VERSION);
        assertThat(version.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void incrementExistingVersionTest() {
        testModerationRepository.createCalloutVersion(shard, callout.getId(), 100, LocalDateTime.now().minusDays(1));

        calloutSender.send(shard, List.of(callout.getId()), e -> System.currentTimeMillis(), e -> "", lst -> {});

        TestModerationRepository.ModerationVersion version =
                testModerationRepository.getCalloutVersionObj(shard, callout.getId());
        assertThat(version.getVersion()).isEqualTo(101);
        assertThat(version.getTime())
                .isCloseTo(LocalDateTime.now(), within(15, ChronoUnit.SECONDS));
    }

    @Test
    public void sendTwoObjectsTest() {
        testModerationRepository.createCalloutVersion(shard, callout.getId(), 100L,
                LocalDateTime.now().minusDays(1));
        testModerationRepository.createCalloutVersion(shard, callout2.getId(), 200L,
                LocalDateTime.now().minusDays(1));

        calloutSender.send(shard, List.of(callout.getId(), callout2.getId()), e -> System.currentTimeMillis(),
                e -> "", lst -> {});

        assertThat(testModerationRepository.getCalloutVersionObj(shard, callout.getId()).getVersion()).isEqualTo(101L);
        assertThat(testModerationRepository.getCalloutVersionObj(shard, callout2.getId()).getVersion()).isEqualTo(201L);

        checkStatusModerate(Sent, callout.getId());
        checkStatusModerate(Sent, callout2.getId());
    }

    @Test
    public void upperTransaction() {
        AtomicReference<List<CalloutModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<CalloutWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(Ready, callout.getId());

        calloutSender.beforeSendTransaction(shard, List.of(callout.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        checkStatusModerate(Sending, callout.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(CalloutSender.INITIAL_VERSION);

        calloutSender.afterSendTransaction(shard, objects);

        checkStatusModerate(Sent, callout.getId());

        assertThat(testModerationRepository.getCalloutVersionObj(shard, callout.getId()).getVersion())
                .isEqualTo(CalloutSender.INITIAL_VERSION);
    }

    @Test
    public void versionChangeAfterRetry() {
        AtomicReference<List<CalloutModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<CalloutWithModerationInfo>> objects = new AtomicReference<>();

        checkStatusModerate(Ready, callout.getId());

        calloutSender.beforeSendTransaction(shard, List.of(callout.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);
        calloutSender.beforeSendTransaction(shard, List.of(callout.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);


        checkStatusModerate(Sending, callout.getId());

        assertThat(requests.get()).hasSize(1);
        assertThat(requests.get().get(0).getMeta().getVersionId()).isEqualTo(CalloutSender.INITIAL_VERSION + 1);

        calloutSender.afterSendTransaction(shard, objects);

        checkStatusModerate(Sent, callout.getId());
        assertThat(testModerationRepository.getCalloutVersionObj(shard, callout.getId()).getVersion())
                .isEqualTo(CalloutSender.INITIAL_VERSION + 1);
    }

    @Test
    public void checkRequestDataIsCorrect() {
        AtomicReference<List<CalloutModerationRequest>> requests = new AtomicReference<>();
        AtomicReference<List<CalloutWithModerationInfo>> objects = new AtomicReference<>();

        calloutSender.beforeSendTransaction(shard, List.of(callout.getId()), objects, requests,
                e -> System.currentTimeMillis(), el -> null);

        assertThat(requests.get()).hasSize(1);
        var data = requests.get().get(0).getData();
        assertThat(data.getText()).isEqualTo("CalloutText");
    }

    private void checkStatusModerate(AdditionsItemCalloutsStatusmoderate expectedStatusModerate, Long id) {
        AdditionsItemCalloutsStatusmoderate statusModerate =
                testCalloutRepository.getCalloutsStatusModerate(shard, id);
        assertEquals(expectedStatusModerate, statusModerate);
    }
}
