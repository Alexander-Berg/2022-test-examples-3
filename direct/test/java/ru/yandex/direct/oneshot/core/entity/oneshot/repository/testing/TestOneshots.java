package ru.yandex.direct.oneshot.core.entity.oneshot.repository.testing;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;

import ru.yandex.direct.oneshot.core.model.LaunchStatus;
import ru.yandex.direct.oneshot.core.model.Oneshot;
import ru.yandex.direct.oneshot.core.model.OneshotLaunch;
import ru.yandex.direct.oneshot.core.model.OneshotLaunchData;
import ru.yandex.direct.oneshot.core.model.OneshotLaunchValidationStatus;

public class TestOneshots {
    private TestOneshots() {
    }

    public static Oneshot defaultOneshot() {
        return new Oneshot()
                .withClassName("ru.yandex.class_name" + RandomUtils.nextLong(1, Long.MAX_VALUE))
                .withApprovers(Set.of("jeeves", "wooster"))
                .withDeleted(false)
                .withMultiLaunch(true)
                .withSharded(true)
                .withSafeOneshot(false)
                .withPausedStatusOnFail(false)
                .withRetries(0)
                .withRetryTimeoutSeconds(0)
                .withCreateTime(LocalDateTime.now())
                .withTicket("DIRECTVANSHOT-123");
    }

    public static OneshotLaunch defaultLaunch(Long oneshotId) {
        return new OneshotLaunch()
                .withOneshotId(oneshotId)
                .withLaunchCreator("username")
                .withParams(null)
                .withTraceId(6548392L)
                .withValidationStatus(OneshotLaunchValidationStatus.READY)
                .withApprover("approvername")
                .withApprovedRevision("123456")
                .withLaunchRequestTime(null);
    }

    public static OneshotLaunchData defaultLaunchData(long launchId) {
        return new OneshotLaunchData()
                .withLaunchId(launchId)
                .withShard(0)
                .withSpanId(0L)
                .withState(null)
                .withLaunchStatus(LaunchStatus.READY)
                .withLaunchTime(null)
                .withLaunchedRevisions(Collections.emptySet())
                .withFinishTime(null);
    }
}
