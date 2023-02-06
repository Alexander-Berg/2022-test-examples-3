package ru.yandex.direct.jobs.segment.log;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;

import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;

import static java.util.Arrays.asList;
import static ru.yandex.direct.jobs.segment.SegmentTestUtils.TEST_LOG_PATH_PROVIDER;
import static ru.yandex.direct.jobs.segment.SegmentTestUtils.getDefaultYtOperatorMock;
import static ru.yandex.direct.jobs.segment.SegmentTestUtils.getDefaultYtProviderMock;

public class LogTableNavigatorTestBase {

    static final LocalDate TODAY = LocalDate.now();

    YtCluster ytCluster = YtCluster.HAHN;
    YtProvider ytProvider;
    YtOperator ytOperator;

    LocalDate startLogDate;
    LocalDate finishLogDate;
    Set<LocalDate> missedDays = new HashSet<>();

    LogTableNavigator logTableNavigator;

    private final Supplier<Boolean> ignoreMissedDays;

    public LogTableNavigatorTestBase(Supplier<Boolean> ignoreMissedDays) {
        this.ignoreMissedDays = ignoreMissedDays;
    }

    @BeforeEach
    public void before() {
        ytOperator = getDefaultYtOperatorMock(() -> startLogDate, () -> finishLogDate, () -> missedDays);

        ytProvider = getDefaultYtProviderMock(ytCluster, ytOperator);

        logTableNavigator = new LogTableNavigator(ytProvider, ytCluster,
                TEST_LOG_PATH_PROVIDER, ignoreMissedDays);
    }

    void mockLogPeriod(LocalDate startLogDate, LocalDate finishLogDate) {
        this.startLogDate = startLogDate;
        this.finishLogDate = finishLogDate;
    }

    void mockMissedDays(LocalDate... missedDays) {
        this.missedDays = new HashSet<>(asList(missedDays));
    }
}
