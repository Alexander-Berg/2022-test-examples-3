package ru.yandex.market.deepmind.tms.executors;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.market.deepmind.common.DeepmindBaseEnhancedTrackerApproverExecutorTestClass;
import ru.yandex.market.deepmind.tms.executors.utils.YtUtils;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;

public class PrepareEnrichToPendingExecutorTest extends DeepmindBaseEnhancedTrackerApproverExecutorTestClass {
    private final Yt yt = new TestYt();

    @Test
    public void testCheckNeedRun() {
        var unstableUnitYt = UnstableInit.simple(yt);
        var now = LocalDate.now();
        var table = YtUtils.getOutputTablePath(deepmindYtProperties.getPrepareEnrichToPendingTablePath(), now);
        Assertions.assertThat(YtUtils.isPathExists(unstableUnitYt, table)).isFalse();
        yt.cypress().create(YtUtils.getOutputTablePath(
            deepmindYtProperties.getPrepareEnrichToPendingTablePath(), now), CypressNodeType.TABLE);
        Assertions.assertThat(YtUtils.isPathExists(unstableUnitYt, table)).isTrue();

    }
}
