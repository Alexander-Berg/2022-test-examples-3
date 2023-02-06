package ru.yandex.market.jmf.timings.test.deprecated;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.entity.EntitySnapshotService;
import ru.yandex.market.jmf.logic.def.Bo;
import ru.yandex.market.jmf.logic.wf.DefaultWfConfigurationProvider;
import ru.yandex.market.jmf.logic.wf.WfConfigurationProvider;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataProvider;
import ru.yandex.market.jmf.metadata.MetadataProviders;
import ru.yandex.market.jmf.timings.DefaultTimingMetadataProvider;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.TimingMetadataProvider;
import ru.yandex.market.jmf.timings.attributes.TimerStatus;
import ru.yandex.market.jmf.timings.attributes.backtimer.BackTimer;
import ru.yandex.market.jmf.timings.attributes.backtimer.BackTimerData;
import ru.yandex.market.jmf.timings.test.InternalTimingTestConfiguration;
import ru.yandex.market.jmf.timings.test.TimingTestConfiguration;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.jmf.utils.XmlUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withMarginOf;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Тестирование логики атрибута "Счетчик времени".
 */
@SpringJUnitConfig(InternalTimingTestConfiguration.class)
public class DeprecatedBackTimerTest {

    private static final Fqn SCRIPT_FQN = Fqn.parse("deprecatedEntityWithScriptBackTimer");
    private static final Fqn STATUS_FQN = Fqn.parse("deprecatedEntityWithStatusBackTimer");
    private static final Fqn RESETTABLE_FQN = Fqn.parse("deprecatedEntityWithResettableBackTimer");

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
        Entity entity = createScriptEntity("", 1);

        Thread.sleep(3_000);

        BackTimer data = getTimerValue(entity);
        assertEquals(TimerStatus.NOT_STARTED, data.getStatus());
        assertNull(data.getDeadline(), "Т.к. таймер не запущен, то deadline не должен быть определен");
        assertThat(data.getMargin())
                .as("Т.к. таймер не запушен, то запас времени не должен меняться")
                .isCloseTo(Duration.ofHours(1), withMarginOf(Duration.ofSeconds(1)));

        List<Entity> versions = getVersions(entity);
        assertEquals(1, versions.size());
        assertVersionDeadline(data.getDeadline(), versions, 0);
    }

    @Test
    public void paused() throws Exception {
        Entity entity = createScriptEntity("PAUSE_ME", 1);

        Thread.sleep(3_000);

        BackTimer data = getTimerValue(entity);
        assertEquals(TimerStatus.PAUSED, data.getStatus());
        assertNull(data.getDeadline(), "Так как таймер не запущен, то deadline не определен");
        assertThat(data.getMargin())
                .as("Т.к. таймер не запушен, то запас времени не изменился")
                .isCloseTo(Duration.ofHours(1), withMarginOf(Duration.ofSeconds(1)));

        List<Entity> versions = getVersions(entity);
        assertEquals(1, versions.size());
        assertVersionDeadline(data.getDeadline(), versions, 0);
    }

    @Test
    @Transactional
    public void snapshot() throws InterruptedException {
        Entity entity = createScriptEntity("PAUSE_ME", 1);

        Thread.sleep(3_000);

        BackTimer value = getTimerValue(entity);
        JsonNode snapshot = snapshotService.toSnapshot(entity);
        Entity afterSnapshot = snapshotService.fromSnapshot(entity.getFqn(), snapshot);
        Object result = afterSnapshot.getAttribute("timer");

        assertEquals(value, result, "Должны получить значение объекта до создания snapshot-а");
    }

    @Test
    public void stopped() throws Exception {
        Entity entity = createScriptEntity("STOP_ME", 1);

        Thread.sleep(3_000);

        BackTimer data = getTimerValue(entity);
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertNull(data.getDeadline(), "Так как таймер не запущен, то deadline не определен");
        assertThat(data.getMargin())
                .as("Т.к. таймер не запушен, то запас времени не изменился")
                .isCloseTo(Duration.ofHours(1), withMarginOf(Duration.ofSeconds(1)));

        List<Entity> versions = getVersions(entity);
        assertEquals(1, versions.size());
        assertVersionDeadline(data.getDeadline(), versions, 0);
    }

    @Test
    public void active() throws Exception {
        Entity entity = createScriptEntity("START_ME", 1);
        OffsetDateTime expectedDeadline = OffsetDateTime.now().plusHours(1);

        Thread.sleep(4_000);

        BackTimer data = getTimerValue(entity);
        assertEquals(TimerStatus.ACTIVE, data.getStatus(), "Счетчик должен быть просрочен т.к. прошло больше 1 сек");
        assertThat(data.getDeadline())
                .as("Таймер должен закончить считать в %s", expectedDeadline)
                .isCloseTo(expectedDeadline, within(2, ChronoUnit.SECONDS));
        assertThat(data.getMargin())
                .as("Остаток времени должен уменьшится на 4 сек. т.к. мы запросили атрибут после паузы в " +
                        "4 сек.")
                .isCloseTo(Duration.ofHours(1).minusSeconds(4), withMarginOf(Duration.ofSeconds(2)));

        List<Entity> versions = getVersions(entity);
        assertEquals(1, versions.size());
        assertVersionDeadline(data.getDeadline(), versions, 0);
    }

    @Test
    public void activeWithDayChange() throws Exception {
        int hours = 25;
        Entity entity = createScriptEntity("START_ME", hours);
        OffsetDateTime expectedDeadline = OffsetDateTime.now().plusHours(hours);

        Thread.sleep(4_000);

        BackTimer data = getTimerValue(entity);
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
        assertThat(data.getDeadline())
                .as("Таймер должен закончить считать в %s", expectedDeadline)
                .isCloseTo(expectedDeadline, within(2, ChronoUnit.SECONDS));
        assertThat(data.getMargin())
                .as("Остаток времени должен уменьшится на 4 сек. т.к. мы запросили атрибут после паузы в " +
                        "4 сек.")
                .isCloseTo(Duration.ofHours(hours).minusSeconds(4), withMarginOf(Duration.ofSeconds(2)));

        List<Entity> versions = getVersions(entity);
        assertEquals(1, versions.size());
        assertVersionDeadline(data.getDeadline(), versions, 0);
    }

    @Test
    public void restart() throws Exception {
        Entity entity = createScriptEntity("RESTART_ME", 1);
        OffsetDateTime expectedDeadline = OffsetDateTime.now().plusHours(1);

        Thread.sleep(4_000);

        BackTimer data = getTimerValue(entity);
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
        assertThat(data.getDeadline())
                .as("Таймер должен закончить считать в %s", expectedDeadline)
                .isCloseTo(expectedDeadline, within(2, ChronoUnit.SECONDS));
        assertThat(data.getMargin())
                .as("Остаток времени должен уменьшится на 4 сек. т.к. мы запросили атрибут после паузы в " +
                        "4 сек.")
                .isCloseTo(Duration.ofHours(1).minusSeconds(4), withMarginOf(Duration.ofSeconds(2)));

        List<Entity> versions = getVersions(entity);
        assertEquals(1, versions.size());
        assertVersionDeadline(data.getDeadline(), versions, 0);
    }

    @Test
    public void edit_script_restartActive() throws Exception {
        // настройка системы
        Entity entity = createScriptEntity("START_ME", 1);

        Thread.sleep(4_000);

        // вызов системы
        Entity result = editScriptEntity(entity, "RESTART_ME");
        OffsetDateTime expectedDeadline = OffsetDateTime.now().plusHours(1);

        BackTimer data = getTimerValue(result);
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
        assertThat(data.getDeadline())
                .as("Таймер должен закончить считать в %s", expectedDeadline)
                .isCloseTo(expectedDeadline, within(2, ChronoUnit.SECONDS));
        assertThat(data.getMargin())
                .as("Остаток времени не должен уменьшиться на 4 сек. т.к. мы перезапустили счетчик")
                .isCloseTo(Duration.ofHours(1), withMarginOf(Duration.ofSeconds(2)));
    }

    @Test
    public void edit_script_restartExceed() throws Exception {
        // настройка системы
        Duration duration = Duration.ofSeconds(3);
        Entity entity = createScriptEntity("START_ME", duration);

        Thread.sleep(4_000);

        BackTimer data = getTimerValue(entity);
        assertEquals(TimerStatus.EXCEED, data.getStatus());

        // вызов системы
        Entity result = editScriptEntity(entity, "RESTART_ME");
        OffsetDateTime expectedDeadline = OffsetDateTime.now().plus(duration);

        data = getTimerValue(result);
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
        assertThat(data.getDeadline())
                .as("Таймер должен закончить считать в %s", expectedDeadline)
                .isCloseTo(expectedDeadline, within(2, ChronoUnit.SECONDS));
        assertThat(data.getMargin())
                .as("Остаток времени должен быть как при старте, т.к. мы перезапустили счетчик")
                .isCloseTo(duration, withMarginOf(Duration.ofSeconds(2)));
    }

    @Test
    public void edit_status_restart() {
        Entity entity = createStatusEntity();
        entity = editStatusEntity(entity, "st2");

        Entity result = editStatusEntity(entity, "st5");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
    }

    @Test
    public void exceeded() throws Exception {
        Entity entity = createScriptEntity("START_ME", Duration.ofSeconds(2));
        OffsetDateTime expectedDeadline = OffsetDateTime.now().plusSeconds(2);

        Thread.sleep(3_000);

        BackTimer data = getTimerValue(entity);
        assertEquals(TimerStatus.EXCEED, data.getStatus());
        assertThat(data.getDeadline())
                .as("Таймер должен закончить считать в %s", expectedDeadline)
                .isCloseTo(expectedDeadline, within(2, ChronoUnit.SECONDS));
        assertThat(data.getMargin())
                .as("Остаток времени должен быьт 0 т.к. таймер просрочен")
                .isCloseTo(Duration.ZERO, withMarginOf(Duration.ofSeconds(1)));

        List<Entity> versions = getVersions(entity);
        assertEquals(1, versions.size());
        assertVersionDeadline(data.getDeadline(), versions, 0);
    }

    @Test
    public void exceeded1() throws Exception {
        Entity entity = createScriptEntity("START_ME", Duration.ofSeconds(5));
        Thread.sleep(3_000);
        txService.runInNewTx(() -> bcpService.edit(entity, Maps.of("resolutionTime", 1)));

    }

    @Test
    public void create_status_st1() {
        Entity entity = createStatusEntity();

        BackTimerData data = entityService.internalGetAttribute(entity, "timer");
        assertEquals(TimerStatus.PAUSED, data.getStatus());
    }

    @Test
    public void edit_status_st2() {
        Entity entity = createStatusEntity();

        Entity result = editStatusEntity(entity, "st2");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.ACTIVE, data.getStatus());

        List<Entity> versions = getVersions(entity);
        assertEquals(2, versions.size());
        assertVersionDeadline(data.getDeadline(), versions, 1);
    }

    @Test
    public void edit_status_st2_st3() {
        Entity entity = createStatusEntity();
        entity = editStatusEntity(entity, "st2");

        Entity result = editStatusEntity(entity, "st3");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.PAUSED, data.getStatus());

        List<Entity> versions = getVersions(entity);
        assertEquals(3, versions.size());
        assertVersionDeadline(data.getDeadline(), versions, 2);
    }

    @Test
    public void edit_resettable_st2_st3() {
        Entity entity = createResettableEntity();
        entity = editStatusEntity(entity, "st2");

        Entity result = editStatusEntity(entity, "st3");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.NOT_STARTED, data.getStatus());

        List<Entity> versions = getVersions(entity);
        assertEquals(3, versions.size());
        assertVersionDeadline(data.getDeadline(), versions, 2);
    }

    /**
     * кейс:
     * счетчик просрочен, срабатывает триггер, который меняет объект (не определяющие счетчик атрибуты), после чего
     * счетчик начинает отсчитываеть время сначала.
     * <p>
     * а должно быть: счетчик перезапускается толькро после перехода в статус, в котором он должен сбросится
     *
     * @see
     * <a href="https://st.yandex-team.ru/OCRM-3391">OCRM-3391: Некорректное поведение таймера allowanceAlertTakingTimer у тикетов</a>
     */
    @Test
    public void edit_resettable_exceed() throws Exception {
        Entity entity = createResettableEntity();
        setResolutionTime(entity, Duration.ofSeconds(1));
        entity = editStatusEntity(entity, "st2");

        Thread.sleep(2000);

        BackTimer timer = getTimerValue(entity);
        assertEquals(TimerStatus.EXCEED,
                timer.getStatus());

        changeTitle(entity);

        timer = getTimerValue(entity);
        assertEquals(TimerStatus.EXCEED, timer.getStatus(), "Счетчик должен остаться просроченным т.к. " +
                "не переходили в статус ressetable");
    }

    public void changeTitle(Entity entity) {
        txService.runInNewTx(() -> bcpService.edit(entity, Maps.of(Bo.TITLE, Randoms.string())));
    }

    public void setResolutionTime(Entity entity, Duration time) {
        txService.runInNewTx(() -> bcpService.edit(entity, Maps.of("resolutionTime", time)));
    }

    @Test
    public void edit_status_st2_st4() {
        Entity entity = createStatusEntity();
        entity = editStatusEntity(entity, "st2");

        Entity result = editStatusEntity(entity, "st4");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.PAUSED, data.getStatus());

        List<Entity> versions = getVersions(entity);
        assertEquals(3, versions.size());
        assertVersionDeadline(data.getDeadline(), versions, 2);
    }

    @Test
    public void create_script_notStarted() {
        Entity entity = createScriptEntity("", 1);

        BackTimerData data = entityService.internalGetAttribute(entity, "timer");
        assertEquals(TimerStatus.NOT_STARTED, data.getStatus());
        assertNull(data.getStartTime());
        assertEquals(Duration.ZERO, data.getDuration());

        List<Entity> versions = getVersions(entity);
        assertEquals(1, versions.size());
        assertVersionDeadline(data.getDeadline(), versions, 0);
    }

    @Test
    public void create_script_active() {
        Entity entity = createScriptEntity("START_ME", 1);

        BackTimerData data = entityService.internalGetAttribute(entity, "timer");
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
        assertNotNull(data.getStartTime());
        assertEquals(Duration.ZERO, data.getDuration());
        assertNotNull(data.getDeadline());

        List<Entity> versions = getVersions(entity);
        assertEquals(1, versions.size());
        assertVersionDeadline(data.getDeadline(), versions, 0);
    }

    @Test
    public void edit_script_activateActive() {
        // настройка системы
        Entity entity = createScriptEntity("START_ME", 1);

        // вызов системы
        Entity result = editScriptEntity(entity, "START_ME");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
        assertNotNull(data.getStartTime());
        assertNotNull(data.getDeadline());

        List<Entity> versions = getVersions(entity);
        assertEquals(1, versions.size(), "Редактировали объект, но значение счетчика не изменилось");
    }

    @Test
    public void edit_script_resumeActive() {
        // настройка системы
        Entity entity = createScriptEntity("START_ME", 1);

        // вызов системы
        Entity result = editScriptEntity(entity, "RESUME_ME");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
        assertNotNull(data.getStartTime());
        assertNotNull(data.getDeadline());

        List<Entity> versions = getVersions(entity);
        assertEquals(1, versions.size(), "Редактировали объект, но значение счетчика не изменилось");
    }

    @Test
    public void edit_script_passivateActive() {
        // настройка системы
        Entity entity = createScriptEntity("START_ME", 1);

        // вызов системы
        Entity result = editScriptEntity(entity, "PAUSE_ME");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.PAUSED, data.getStatus());
        assertNull(data.getStartTime());
        assertNull(data.getDeadline());

        List<Entity> versions = getVersions(entity);
        assertEquals(2, versions.size());
        assertVersionDeadline(data.getDeadline(), versions, 1);
    }

    @Test
    public void edit_script_stopActive() {
        // настройка системы
        Entity entity = createScriptEntity("START_ME", 1);

        // вызов системы
        Entity result = editScriptEntity(entity, "STOP_ME");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertNull(data.getStartTime());
        assertNull(data.getDeadline());
    }

    @Test
    public void edit_script_stopStopped() {
        // настройка системы
        Entity entity = createScriptEntity("STOP_ME", 1);

        // вызов системы
        Entity result = editScriptEntity(entity, "STOP_ME");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertNull(data.getStartTime());
        assertNull(data.getDeadline());
    }

    @Test
    public void edit_script_pauseStopped() {
        // настройка системы
        Entity entity = createScriptEntity("STOP_ME", 1);

        // вызов системы
        Entity result = editScriptEntity(entity, "PAUSE_ME");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertNull(data.getStartTime());
        assertNull(data.getDeadline());
    }

    @Test
    public void edit_script_activateStopped() {
        // настройка системы
        Entity entity = createScriptEntity("STOP_ME", 1);

        // вызов системы
        Entity result = editScriptEntity(entity, "ACTIVATE_ME");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertNull(data.getStartTime());
        assertNull(data.getDeadline());
    }

    @Test
    public void edit_script_resumeStopped() {
        // настройка системы
        Entity entity = createScriptEntity("STOP_ME", 1);

        // вызов системы
        Entity result = editScriptEntity(entity, "RESUME_ME");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertNull(data.getStartTime());
        assertNull(data.getDeadline());
    }

    @Test
    public void edit_script_stopPaused() {
        // настройка системы
        Entity entity = createScriptEntity("PAUSE_ME", 1);

        // вызов системы
        Entity result = editScriptEntity(entity, "STOP_ME");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertNull(data.getStartTime());
        assertNull(data.getDeadline());
    }

    @Test
    public void edit_script_pausePaused() {
        // настройка системы
        Entity entity = createScriptEntity("PAUSE_ME", 1);

        // вызов системы
        Entity result = editScriptEntity(entity, "PAUSE_ME");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.PAUSED, data.getStatus());
        assertNull(data.getStartTime());
        assertNull(data.getDeadline());
    }

    @Test
    public void edit_script_activatePaused() {
        // настройка системы
        Entity entity = createScriptEntity("PAUSE_ME", 1);

        // вызов системы
        Entity result = editScriptEntity(entity, "ACTIVATE_ME");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.PAUSED, data.getStatus());
        assertNull(data.getStartTime());
        assertNull(data.getDeadline());
    }

    @Test
    public void edit_script_resumePaused() {
        // настройка системы
        Entity entity = createScriptEntity("PAUSE_ME", 1);

        // вызов системы
        Entity result = editScriptEntity(entity, "RESUME_ME");

        BackTimerData data = entityService.internalGetAttribute(result, "timer");
        assertEquals(TimerStatus.ACTIVE, data.getStatus());
        assertNotNull(data.getStartTime());
        assertNotNull(data.getDeadline());
    }

    @Test
    public void create_script_paused() {
        Entity entity = createScriptEntity("PAUSE_ME", 1);

        BackTimerData data = entityService.internalGetAttribute(entity, "timer");
        assertEquals(TimerStatus.PAUSED, data.getStatus());
        assertNull(data.getStartTime());
        assertEquals(Duration.ZERO, data.getDuration());
        assertNull(data.getDeadline());
    }

    @Test
    public void create_script_stop() {
        Entity entity = createScriptEntity("STOP_ME", 1);

        BackTimerData data = entityService.internalGetAttribute(entity, "timer");
        assertEquals(TimerStatus.STOPPED, data.getStatus());
        assertNull(data.getStartTime());
        assertEquals(Duration.ZERO, data.getDuration());
        assertNull(data.getDeadline());
    }

    private Entity createScriptEntity(String sync, int hours) {
        return createScriptEntity(sync, Duration.ofHours(hours));
    }

    private Entity createScriptEntity(String sync, Duration resolutionTime) {
        ServiceTime st = createServiceTime();
        ImmutableMap<String, Object> properties = ImmutableMap.of(
                "timerSync", sync,
                "serviceTime", st,
                "resolutionTime", resolutionTime,
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

    private Entity createResettableEntity() {
        ServiceTime st = createServiceTime();
        ImmutableMap<String, Object> properties = ImmutableMap.of(
                "serviceTime", st,
                "title", Randoms.string()
        );
        return txService.doInNewTx(() -> bcpService.create(RESETTABLE_FQN, properties));
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

    private void assertVersionDeadline(OffsetDateTime deadline, List<Entity> versions, int index) {
        Entity version = versions.get(index);
        OffsetDateTime versionDeadline = txService.doInNewTx(() -> version.getAttribute("timer"));
        if (deadline == versionDeadline) {
            return;
        }
        assertThat(versionDeadline)
                .as("Таймер должен закончить считать в %s", deadline)
                .isCloseTo(deadline, within(2, ChronoUnit.SECONDS));
    }

    private ArrayList<Entity> getVersions(Entity entity) {
        return txService.doInNewTx(() -> new ArrayList<>(dbService.listOfVersions(entity)));
    }

    private BackTimer getTimerValue(Entity entity) {
        return txService.doInNewTx(() -> dbService.get(entity.getGid()).getAttribute("timer"));
    }

    @Configuration
    @Import(TimingTestConfiguration.class)
    public static class MetadataConfiguration {
        @Bean
        public MetadataProvider providerTimerTestMetadata(MetadataProviders providers) {
            return providers.of("classpath:backtimer_metadata.xml");
        }

        @Bean
        public TimingMetadataProvider providerTimerTestTimingMetadata(XmlUtils xmlUtils) {
            return new DefaultTimingMetadataProvider("classpath:backtimer_configuration.xml", xmlUtils);
        }

        @Bean
        public WfConfigurationProvider providerTimerTestWfMetadata(XmlUtils xmlUtils) {
            return new DefaultWfConfigurationProvider("classpath:timer_wf.xml", xmlUtils);
        }
    }
}
