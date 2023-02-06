package ru.yandex.direct.jobs.util.juggler.checkinfo;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.ansiblejuggler.PlaybookBuilder;
import ru.yandex.direct.ansiblejuggler.model.notifications.NotificationMethod;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.juggler.JugglerStatus;
import ru.yandex.direct.juggler.check.annotation.JugglerCheck;
import ru.yandex.direct.juggler.check.annotation.OnChangeNotification;
import ru.yandex.direct.juggler.check.model.CheckTag;
import ru.yandex.direct.scheduler.support.DirectJob;
import ru.yandex.direct.scheduler.support.DirectShardedJob;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.juggler.check.model.NotificationRecipient.CHAT_INTERNAL_SYSTEMS_MONITORING;

class JobWorkingCheckInfoTest {
    private static final EnvironmentType ENV = EnvironmentType.TESTING;
    private static final List<Integer> SHARDS = Arrays.asList(1, 154, 90);

    @JugglerCheck(ttl = @JugglerCheck.Duration(hours = 4),
            tags = {CheckTag.GROUP_INTERNAL_SYSTEMS},
            notifications = {
                    @OnChangeNotification(recipient = CHAT_INTERNAL_SYSTEMS_MONITORING,
                            status = {JugglerStatus.OK, JugglerStatus.CRIT},
                            method = NotificationMethod.TELEGRAM),
            })
    private static class SomeClass {
    }

    @Mock
    private PlaybookBuilder playbookBuilder;

    @Mock
    private ru.yandex.direct.ansiblejuggler.model.checks.JugglerCheck jugglerCheck;

    @Mock
    private ru.yandex.direct.ansiblejuggler.model.meta.JugglerMeta jugglerMeta;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(jugglerMeta)
                .when(jugglerMeta).withUrl(any());

        doReturn(jugglerMeta)
                .when(jugglerCheck).getMeta();

        doReturn(jugglerCheck)
                .when(playbookBuilder).addSingleServiceCheck(any(), any());
        doReturn(jugglerCheck)
                .when(playbookBuilder).addMultiServicesCheck(any(), any(), any());
    }

    @Test
    void testNonShardedJobCheck() {
        JugglerCheck annotation = SomeClass.class.getAnnotation(JugglerCheck.class);
        JobWorkingCheckInfo checkInfo = new JobWorkingCheckInfo(annotation, DirectJob.class, SHARDS, ENV, null);

        SoftAssertions soft = new SoftAssertions();

        String name = "jobs.DirectJob.working." + ENV.getLegacyName();
        soft.assertThat(checkInfo.getServiceName())
                .isEqualTo(name);
        soft.assertThatThrownBy(() -> checkInfo.shardedServiceName(1))
                .isInstanceOf(IllegalStateException.class);

        soft.assertAll();

        checkInfo.addCheckToPlaybook(playbookBuilder);

        verify(playbookBuilder)
                .addSingleServiceCheck(eq(name), eq(Duration.ofHours(4)));
        verify(jugglerCheck)
                .withTag(eq(CheckTag.GROUP_INTERNAL_SYSTEMS.getName()));
        verify(jugglerCheck)
                .withNotification(any());
        verify(jugglerMeta, atLeastOnce())
                .withUrl(any());
    }

    @Test
    void testShardedJobCheck() {
        JugglerCheck annotation = SomeClass.class.getAnnotation(JugglerCheck.class);
        JobWorkingCheckInfo checkInfo =
                new JobWorkingCheckInfo(annotation, DirectShardedJob.class, SHARDS, ENV, null);

        SoftAssertions soft = new SoftAssertions();

        String name = "jobs.DirectShardedJob.working." + ENV.getLegacyName();
        soft.assertThat(checkInfo.getServiceName())
                .isEqualTo(name);
        soft.assertThat(checkInfo.shardedServiceName(1))
                .isEqualTo(name + ".shard_1");

        soft.assertAll();

        checkInfo.addCheckToPlaybook(playbookBuilder);

        verify(playbookBuilder)
                .addMultiServicesCheck(
                        eq(Arrays.asList(name + ".shard_1", name + ".shard_154", name + ".shard_90")),
                        eq(name),
                        eq(Duration.ofHours(4)));
        verify(jugglerCheck)
                .withTag(eq(CheckTag.GROUP_INTERNAL_SYSTEMS.getName()));
        verify(jugglerCheck)
                .withNotification(any());
        verify(jugglerMeta, atLeastOnce())
                .withUrl(any());
    }
}
