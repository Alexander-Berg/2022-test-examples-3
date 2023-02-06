package ru.yandex.market.mbo.mdm.common.masterdata.repository.queue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuQueueInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;

public class MdmQueueInfoReasonDeduplicationTest {
    @Test
    public void whenSameReasonsChooseLatestTime() {
        // given
        MdmMskuQueueInfo info = new MdmMskuQueueInfo();
        Instant baseTs = Instant.now();

        // when
        info.addRefreshTimeAndReason(
            new MdmQueueInfoBase.TimeAndReason(MdmEnqueueReason.CHANGED_BY_MDM_ADMIN, baseTs)
        );
        info.addRefreshTimeAndReason(
            new MdmQueueInfoBase.TimeAndReason(
                MdmEnqueueReason.CHANGED_BY_MDM_ADMIN,
                baseTs.plus(1, ChronoUnit.MINUTES)
            )
        );
        info.addRefreshTimeAndReason(
            new MdmQueueInfoBase.TimeAndReason(
                MdmEnqueueReason.CHANGED_BY_MDM_ADMIN,
                baseTs.minus(1, ChronoUnit.MINUTES)
            )
        );

        //then
        Assertions.assertThat(info.getRefreshReasons())
            .containsExactly(new MdmQueueInfoBase.TimeAndReason(
                MdmEnqueueReason.CHANGED_BY_MDM_ADMIN,
                baseTs.plus(1, ChronoUnit.MINUTES)
            ));
    }
}
