package ru.yandex.direct.ess.router.rules.bsexport.mobilegoals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.binlog.model.BinlogEvent;
import ru.yandex.direct.binlog.model.Operation;
import ru.yandex.direct.ess.logicobjects.bsexport.mobilegoals.BsExportMobileGoalsExternalTrackerObject;
import ru.yandex.direct.ess.router.configuration.TestConfiguration;
import ru.yandex.direct.ess.router.testutils.MobileGoalsExternalTrackerChange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.ess.router.testutils.MobileGoalsExternalTrackerChange.createMobileGoalsExternalTrackerEvent;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
class BsExportMobileGoalsExternalTrackerRuleTest {
    private static final String METHOD = "method";
    private static final String SERVICE = "service";
    private static final Long REQID = 1342352352532L;

    @Autowired
    private BsExportMobileGoalsExternalTrackerRule rule;

    @Test
    void insertGoalTest() {
        Long goalId = 10L;
        MobileGoalsExternalTrackerChange goalsChange = new MobileGoalsExternalTrackerChange().withGoalId(goalId);
        var binlogEvent = createMobileGoalsExternalTrackerEvent(List.of(goalsChange), Operation.INSERT);
        addSystemFieldsToEvent(binlogEvent);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportMobileGoalsExternalTrackerObject(goalId);
        addSystemFieldsToObject(expectedObject);
        assertThat(objects).hasSize(1);
        assertThat(objects.get(0)).isEqualToComparingFieldByField(expectedObject);
    }

    @Test
    void updateGoalTest() {
        Long goalId = 10L;
        MobileGoalsExternalTrackerChange goalsChange = new MobileGoalsExternalTrackerChange().withGoalId(goalId);
        var binlogEvent = createMobileGoalsExternalTrackerEvent(List.of(goalsChange), Operation.UPDATE);
        addSystemFieldsToEvent(binlogEvent);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportMobileGoalsExternalTrackerObject(goalId);
        addSystemFieldsToObject(expectedObject);
        assertThat(objects).hasSize(1);
        assertThat(objects.get(0)).isEqualToComparingFieldByField(expectedObject);
    }

    @Test
    void deleteGoalTest() {
        Long goalId = 10L;
        MobileGoalsExternalTrackerChange goalsChange = new MobileGoalsExternalTrackerChange().withGoalId(goalId);
        var binlogEvent = createMobileGoalsExternalTrackerEvent(List.of(goalsChange), Operation.DELETE);
        addSystemFieldsToEvent(binlogEvent);
        var objects = rule.mapBinlogEvent(binlogEvent);
        var expectedObject = new BsExportMobileGoalsExternalTrackerObject(goalId, true);
        addSystemFieldsToObject(expectedObject);
        assertThat(objects).hasSize(1);
        assertThat(objects.get(0)).isEqualToComparingFieldByField(expectedObject);
    }

    private void addSystemFieldsToEvent(BinlogEvent binlogEvent) {
        binlogEvent.setTraceInfoMethod(METHOD);
        binlogEvent.setTraceInfoService(SERVICE);
        binlogEvent.setTraceInfoReqId(REQID);
    }

    private void addSystemFieldsToObject(BsExportMobileGoalsExternalTrackerObject object) {
        object.setMethod(METHOD);
        object.setService(SERVICE);
        object.setReqid(REQID);
    }
}
