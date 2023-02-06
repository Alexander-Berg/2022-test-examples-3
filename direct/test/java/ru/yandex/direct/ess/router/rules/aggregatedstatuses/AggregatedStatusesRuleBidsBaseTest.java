package ru.yandex.direct.ess.router.rules.aggregatedstatuses;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.aggregatedstatuses.AggregatedStatusObjectType;
import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.ess.logicobjects.aggregatedstatuses.AggregatedStatusEventObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.BidsBaseTableChange;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlog.model.Operation.DELETE;
import static ru.yandex.direct.binlog.model.Operation.INSERT;
import static ru.yandex.direct.binlog.model.Operation.UPDATE;
import static ru.yandex.direct.dbschema.ppc.Tables.BIDS_BASE;
import static ru.yandex.direct.ess.router.testutils.BidsBaseTableChange.createBidsBaseEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class AggregatedStatusesRuleBidsBaseTest {
    private static final Long BID_ID = 123L;
    private static final Long PID_ID = 321L;

    @Autowired
    private AggregatedStatusesRule rule;

    @Test
    void mapBinlogEventInsert() {
        BidsBaseTableChange bidsBaseTableChange = new BidsBaseTableChange().withBidId(BID_ID).withPid(PID_ID);
        bidsBaseTableChange.addInsertedColumn(BIDS_BASE.STATUS_BS_SYNCED, "No");
        BinlogEvent binlogEvent = createBidsBaseEvent(singletonList(bidsBaseTableChange), INSERT);

        List<AggregatedStatusEventObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).containsExactly(new AggregatedStatusEventObject(
                AggregatedStatusObjectType.BID_BASE, BID_ID, PID_ID, null, false));
    }

    @Test
    void mapBinlogEventDelete() {
        BidsBaseTableChange bidsBaseTableChange = new BidsBaseTableChange().withBidId(BID_ID).withPid(PID_ID);
        BinlogEvent binlogEvent = createBidsBaseEvent(singletonList(bidsBaseTableChange), DELETE);

        List<AggregatedStatusEventObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).containsExactly(new AggregatedStatusEventObject(
                AggregatedStatusObjectType.BID_BASE, BID_ID, PID_ID, null, false));
    }

    @Test
    void mapBinlogEventUpdateStatusBsSynced() {
        BidsBaseTableChange bidsBaseTableChange = new BidsBaseTableChange().withBidId(BID_ID).withPid(PID_ID);
        bidsBaseTableChange.addChangedColumn(BIDS_BASE.STATUS_BS_SYNCED, "No", "Yes");
        BinlogEvent binlogEvent = createBidsBaseEvent(singletonList(bidsBaseTableChange), UPDATE);

        List<AggregatedStatusEventObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).isEmpty();
    }

    @Test
    void mapBinlogEventUpdateOpts() {
        BidsBaseTableChange bidsBaseTableChange = new BidsBaseTableChange().withBidId(BID_ID).withPid(PID_ID);
        bidsBaseTableChange.addChangedColumn(BIDS_BASE.OPTS, "", "suspended");
        BinlogEvent binlogEvent = createBidsBaseEvent(singletonList(bidsBaseTableChange), UPDATE);

        List<AggregatedStatusEventObject> resultObjects = rule.mapBinlogEvent(binlogEvent);

        assertThat(resultObjects).containsExactly(new AggregatedStatusEventObject(
                AggregatedStatusObjectType.BID_BASE, BID_ID, PID_ID, null, false));
    }
}
