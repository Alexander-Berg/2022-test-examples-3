package ru.yandex.market.jmf.timings.test;

import java.time.Duration;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.entity.EntitySnapshotService;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.attributes.TimerStatus;
import ru.yandex.market.jmf.timings.attributes.timer.Timer;
import ru.yandex.market.jmf.timings.attributes.timer.TimerData;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.tx.TxService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withMarginOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Тестирование логики атрибута "Счетчик времени".
 */
@SpringJUnitConfig(InternalTimingTestConfiguration.class)
public class TimerTest {

    private static final Fqn SCRIPT_FQN = Fqn.parse("entityWithScriptTimer");
    private static final Fqn STATUS_FQN = Fqn.parse("entityWithStatusTimer");

    @Inject
    ServiceTimeTestUtils utils;
    @Inject
    TxService txService;
    @Inject
    BcpService bcpService;
    @Inject
    EntityService entityService;
    @Inject
    DbService dbService;
    @Inject
    EntitySnapshotService snapshotService;

    @Test
    public void notStarted() throws Exception {
        Entity entity = createScriptEntity("");

        Thread.sleep(3_000);

        Timer data = getTimerValue(entity);
        assertEquals(TimerStatus.NOT_STARTED, data.getStatus());
        assertThat(data.getDuration())
                .as("Т.к. таймер не запушен, то ничего не должны отсчитать")
                .isCloseTo(Duration.ZERO, withMarginOf(Duration.ofSeconds(1)));
    }

    private Timer getTimerValue(Entity entity) {
        return txService.doInNewTx(() -> dbService.get(entity.getGid()).getAttribute("timer"));
    }

    @Test
    public void paused() throws Exception {
        Entity entity = createScriptEntity("PAUSE_ME");

        Thread.sleep(3_000);

        Timer data = getTimerValue(entity);
        assertEquals(TimerStatus.PAUSED, data.getStatus());
        assertThat(data.getDuration())
                .as("Т.к. таймер не запушен, то ничего не должны отсчитать")
                .isCloseTo(Duration.ZERO, withMarginOf(Duration.ofSeconds(1)));
    }

    @Test
    @Transactional
    public void snapshot() throws InterruptedException {
        Entity entity = createScriptEntity("PAUSE_ME");

        Thread.sleep(3_000);

        Timer value = getTimerValue(entity);
        JsonNode snapshot = snapshotService.toSnapshot(entity);
        Entity afterSnapshot = snapshotService.fromSnapshot(entity.getFqn(), snapshot);
        Object result = afterSnapshot.getAttribute("timer");

        assertEquals(value, result, "Должны получить значение объекта до создания snapshot-а");
    }

    @Test
    public void stopped() throws Exception {
        Entity entity = createScriptEntity("STOP_ME");

        Thread.sleep(3_000);

        Timer data = getTimerValue(entity);
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertThat(data.getDuration())
                .as("Т.к. таймер не запушен, то ничего не должны отсчитать")
                .isCloseTo(Duration.ZERO, withMarginOf(Duration.ofSeconds(1)));
    }

    @Test
    public void active() throws Exception {
        Entity entity = createScriptEntity("START_ME");

        Thread.sleep(3_000);

        Timer data = getTimerValue(entity);
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
        assertThat(data.getDuration())
                .as("Т.к. таймер запушен, то должны отсчитать 3 сек. (размер паузы)")
                .isCloseTo(Duration.ofSeconds(3), withMarginOf(Duration.ofSeconds(1)));
    }


    @Test
    public void restart() throws Exception {
        Entity entity = createScriptEntity("RESTART_ME");

        Thread.sleep(3_000);

        Timer data = getTimerValue(entity);
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
        assertThat(data.getDuration())
                .as("Т.к. таймер запущен, то должны отсчитать 3 сек. (размер паузы)")
                .isCloseTo(Duration.ofSeconds(3), withMarginOf(Duration.ofSeconds(1)));
    }

    @Test
    public void edit_script_restartActive() throws Exception {
        // настройка системы
        Entity entity = createScriptEntity("START_ME");

        Thread.sleep(2_000);

        // вызов системы
        Entity result = editScriptEntity(entity, "RESTART_ME");

        Timer data = getTimerValue(result);
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
        assertThat(data.getDuration())
                .as("Т.к. таймер перезапущен, то должны отсчитать 0 сек.")
                .isCloseTo(Duration.ZERO, withMarginOf(Duration.ofSeconds(1)));
    }

    @Test
    public void edit_status_restart() {
        Entity entity = createStatusEntity();
        entity = editStatusEntity(entity, "st2");

        Entity result = editStatusEntity(entity, "st5");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
    }

    @Test
    public void create_status_st1() {
        Entity entity = createStatusEntity();

        TimerData data = entityService.internalGetAttribute(entity, "timer");
        assertEquals(TimerStatus.PAUSED, data.getStatus());
    }

    @Test
    public void edit_status_st2() {
        Entity entity = createStatusEntity();

        Entity result = editStatusEntity(entity, "st2");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
    }

    @Test
    public void edit_status_st2_st3() {
        Entity entity = createStatusEntity();
        entity = editStatusEntity(entity, "st2");

        Entity result = editStatusEntity(entity, "st3");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.PAUSED, data.getStatus());
    }

    @Test
    public void edit_status_st2_st4() {
        Entity entity = createStatusEntity();
        entity = editStatusEntity(entity, "st2");

        Entity result = editStatusEntity(entity, "st4");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.PAUSED, data.getStatus());
    }

    @Test
    public void create_script_notStarted() {
        Entity entity = createScriptEntity("");

        TimerData data = entityService.internalGetAttribute(entity, "timer");
        assertEquals(TimerStatus.NOT_STARTED, data.getStatus());
        assertNull(data.getStartTime());
        assertEquals(Duration.ZERO, data.getDuration());
    }

    @Test
    public void create_script_active() {
        Entity entity = createScriptEntity("START_ME");

        TimerData data = entityService.internalGetAttribute(entity, "timer");
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
        assertNotNull(data.getStartTime());
        assertEquals(Duration.ZERO, data.getDuration());
    }

    @Test
    public void edit_script_activateActive() {
        // настройка системы
        Entity entity = createScriptEntity("START_ME");

        // вызов системы
        Entity result = editScriptEntity(entity, "START_ME");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
        assertNotNull(data.getStartTime());
    }

    @Test
    public void edit_script_resumeActive() {
        // настройка системы
        Entity entity = createScriptEntity("START_ME");

        // вызов системы
        Entity result = editScriptEntity(entity, "RESUME_ME");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
        assertNotNull(data.getStartTime());
    }

    @Test
    public void edit_script_passivateActive() {
        // настройка системы
        Entity entity = createScriptEntity("START_ME");

        // вызов системы
        Entity result = editScriptEntity(entity, "PAUSE_ME");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.PAUSED, data.getStatus());
        assertNull(data.getStartTime());
    }

    @Test
    public void edit_script_stopActive() {
        // настройка системы
        Entity entity = createScriptEntity("START_ME");

        // вызов системы
        Entity result = editScriptEntity(entity, "STOP_ME");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertNull(data.getStartTime());
    }

    @Test
    public void edit_script_stopStopped() {
        // настройка системы
        Entity entity = createScriptEntity("STOP_ME");

        // вызов системы
        Entity result = editScriptEntity(entity, "STOP_ME");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertNull(data.getStartTime());
    }

    @Test
    public void edit_script_pauseStopped() {
        // настройка системы
        Entity entity = createScriptEntity("STOP_ME");

        // вызов системы
        Entity result = editScriptEntity(entity, "PAUSE_ME");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertNull(data.getStartTime());
    }

    @Test
    public void edit_script_activateStopped() {
        // настройка системы
        Entity entity = createScriptEntity("STOP_ME");

        // вызов системы
        Entity result = editScriptEntity(entity, "ACTIVATE_ME");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertNull(data.getStartTime());
    }

    @Test
    public void edit_script_resumeStopped() {
        // настройка системы
        Entity entity = createScriptEntity("STOP_ME");

        // вызов системы
        Entity result = editScriptEntity(entity, "RESUME_ME");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertNull(data.getStartTime());
    }

    @Test
    public void edit_script_stopPaused() {
        // настройка системы
        Entity entity = createScriptEntity("PAUSE_ME");

        // вызов системы
        Entity result = editScriptEntity(entity, "STOP_ME");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertNull(data.getStartTime());
    }

    @Test
    public void edit_script_pausePaused() {
        // настройка системы
        Entity entity = createScriptEntity("PAUSE_ME");

        // вызов системы
        Entity result = editScriptEntity(entity, "PAUSE_ME");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.PAUSED, data.getStatus());
        assertNull(data.getStartTime());
    }

    @Test
    public void edit_script_activatePaused() {
        // настройка системы
        Entity entity = createScriptEntity("PAUSE_ME");

        // вызов системы
        Entity result = editScriptEntity(entity, "ACTIVATE_ME");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.PAUSED, data.getStatus());
        assertNull(data.getStartTime());
    }

    @Test
    public void edit_script_resumePaused() {
        // настройка системы
        Entity entity = createScriptEntity("PAUSE_ME");

        // вызов системы
        Entity result = editScriptEntity(entity, "RESUME_ME");

        TimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
        assertNotNull(data.getStartTime());
    }

    @Test
    public void create_script_paused() {
        Entity entity = createScriptEntity("PAUSE_ME");

        TimerData data = entityService.internalGetAttribute(entity, "timer");
        assertEquals(TimerStatus.PAUSED, data.getStatus());
        assertNull(data.getStartTime());
        assertEquals(Duration.ZERO, data.getDuration());
    }

    @Test
    public void create_script_stop() {
        Entity entity = createScriptEntity("STOP_ME");

        TimerData data = entityService.internalGetAttribute(entity, "timer");
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertNull(data.getStartTime());
        assertEquals(Duration.ZERO, data.getDuration());
    }

    private Entity createScriptEntity(String sync) {
        ServiceTime st = createServiceTime();
        ImmutableMap<String, Object> properties = ImmutableMap.of(
                "timerSync", sync,
                "serviceTime", st,
                "title", Randoms.string()
        );
        return txService.doInNewTx(() -> bcpService.create(SCRIPT_FQN, properties));
    }

    private Entity createStatusEntity() {
        ServiceTime st = createServiceTime();
        ImmutableMap<String, Object> properties = ImmutableMap.of(
                "serviceTime", st,
                "title", Randoms.string()
        );
        return txService.doInNewTx(() -> bcpService.create(STATUS_FQN, properties));
    }

    private Entity editScriptEntity(Entity entity, String sync) {
        ImmutableMap<String, Object> properties = ImmutableMap.of(
                "timerSync", sync
        );
        return txService.doInNewTx(() -> bcpService.edit(entity, properties));
    }

    private Entity editStatusEntity(Entity entity, String status) {
        ImmutableMap<String, Object> properties = ImmutableMap.of(
                "status", status
        );
        return txService.doInNewTx(() -> bcpService.edit(entity, properties));
    }

    private ServiceTime createServiceTime() {
        return txService.doInNewTx(() -> utils.createServiceTime24x7());
    }

}
