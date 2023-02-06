package ru.yandex.direct.ess.router.testutils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;

import static ru.yandex.direct.dbschema.ppc.Tables.MOBILE_APP_GOALS_EXTERNAL_TRACKER;
import static ru.yandex.direct.ess.router.testutils.TestUtils.fillChangedInRow;

public class MobileGoalsExternalTrackerChange extends BaseTableChange {
    public Long goalId;

    public static BinlogEvent createMobileGoalsExternalTrackerEvent(List<MobileGoalsExternalTrackerChange> goalsChanges,
                                                                    Operation operation) {
        BinlogEvent binlogEvent = new BinlogEvent().withTable(MOBILE_APP_GOALS_EXTERNAL_TRACKER.getName()).withOperation(operation);
        List<BinlogEvent.Row> rows = goalsChanges.stream()
                .map(goalsChange -> createGoalsTableRow(goalsChange, operation)).collect(Collectors.toList());
        binlogEvent.withRows(rows);
        return binlogEvent;
    }

    private static BinlogEvent.Row createGoalsTableRow(MobileGoalsExternalTrackerChange goalsChange,
                                                       Operation operation) {
        Map<String, Object> primaryKeys = Map.of(MOBILE_APP_GOALS_EXTERNAL_TRACKER.GOAL_ID.getName(), goalsChange.goalId);
        Map<String, Object> before = new HashMap<>();
        Map<String, Object> after = new HashMap<>();

        fillChangedInRow(before, after, goalsChange.getChangedColumns(), operation);
        return new BinlogEvent.Row().withPrimaryKey(primaryKeys).withBefore(before).withAfter(after);
    }

    public MobileGoalsExternalTrackerChange withGoalId(Long goalId) {
        this.goalId = goalId;
        return this;
    }
}
