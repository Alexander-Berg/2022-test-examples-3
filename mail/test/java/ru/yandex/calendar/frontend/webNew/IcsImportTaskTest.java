package ru.yandex.calendar.frontend.webNew;

import lombok.SneakyThrows;
import lombok.val;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.frontend.webNew.actions.LayerActions;
import ru.yandex.calendar.frontend.webNew.dto.in.ImportIcsData;
import ru.yandex.calendar.frontend.webNew.dto.in.LayerData;
import ru.yandex.calendar.frontend.worker.task.IcsImportTask;
import ru.yandex.calendar.logic.XivaNotificationManager;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsXWrCalname;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.commune.bazinga.BazingaBender;
import ru.yandex.commune.bazinga.BazingaTaskManager;
import ru.yandex.commune.bazinga.impl.OnetimeJob;
import ru.yandex.commune.bazinga.impl.OnetimeUtils;
import ru.yandex.commune.bazinga.scheduler.ExecutionContext;
import ru.yandex.misc.db.q.SqlLimits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@ContextConfiguration(classes = IcsImportTaskTest.IcsImportConf.class)
public class IcsImportTaskTest extends WebNewTestBase {
    @Autowired
    private TestManager testManager;
    @Autowired
    private LayerActions layerActions;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private BazingaTaskManager taskManager;
    @Autowired
    private IcsImportTask icsImportTask;
    @Autowired
    private XivaNotificationManager xivaNotificationManager;

    private IcsVEvent getVEvent() {
        return new IcsVEvent()
                .withUid(CalendarUtils.generateExternalId())
                .withSummary("Ics event")
                .withDtStart(new LocalDate(2017, 12, 18))
                .withDtEnd(new LocalDate(2017, 12, 18));
    }

    @Test
    @SneakyThrows
    public void icsToJsonTest() {
        val parameters = new IcsImportTask.Parameters(user.getUid(), 1L, 2L);
        val json = BazingaBender.mapper.serializeJson(parameters);
        val parametersFromJson = BazingaBender.mapper.parseJson(parameters.getClass(), json);

        assertThat(parametersFromJson).isEqualTo(parameters);
    }

    @SneakyThrows
    private void importIcsTest(int eventCount, String externalId, ImportIcsData data) {
        val imported = layerActions.importIcs(uid, data);

        OnetimeJob job = taskManager.getActiveJobs(icsImportTask.id(), SqlLimits.first(1)).single();

        icsImportTask.setParameters(OnetimeUtils.parseParameters(icsImportTask, job.getParameters()));
        icsImportTask.execute(new ExecutionContext(null, true, job.getId(), Option.empty()));

        assertThat(eventRoutines.findLayerIdsByEventExternalId(externalId)).contains(imported.getId());

        val importedEvents = eventDao.findEventsByLayerId(imported.getId());
        assertThat(importedEvents.size()).isEqualTo(eventCount);

        verify(xivaNotificationManager).notifyLayersUsersAboutEventsChange(any(), any());
        reset(xivaNotificationManager);
    }

    private void importIcsFromUrlExecution(int eventCount, int port, String name, String layerId, String externalId) {
        val layerData = createLayerData();
        val url = "http://localhost:" + port + "/unittest_data/db/ics/import/" + name;
        importIcsTest(eventCount, externalId, new ImportIcsData(Option.empty(),
                Option.of(url), new ImportIcsData.Layer(layerId, layerData)));
    }

    @Test
    public void importIcsFromUrlExecutionTest() {
        testManager.runWithFileResourceJetty(port -> {
            val layerId = user.getDefaultLayerId();

            importIcsFromUrlExecution(1, port, "exdate.ics", Long.toString(layerId),
                    "D221593D-F576-4A02-AE50-8B878A12E031");
            importIcsFromUrlExecution(2, port, "recurrence_id_tz.ics", "new",
                    "4A4DCFCD-4ACF-49FB-B12E-3B6D6B0C8859");
        });
    }

    private void importIcsFromDataExecution(boolean isNewCalendar, String layerId) {
        val layerData = createLayerData();
        layerData.setName(Option.empty());
        val vevent = getVEvent();
        val calendar = isNewCalendar
                ? vevent.makeCalendar()
                : vevent.makeCalendar().addProperty(new IcsXWrCalname("Name from ics"));
        importIcsTest(1, vevent.getUid().get(), new ImportIcsData(Option.of(calendar.serializeToBytes()),
                    Option.empty(), new ImportIcsData.Layer(layerId, layerData)));
    }

    @Test
    public void importIcsFromDataExecutionTest() {
        val layerId = user.getDefaultLayerId();

        importIcsFromDataExecution(false, Long.toString(layerId));
        importIcsFromDataExecution(true, "new");
    }

    private LayerData createLayerData() {
        val notifications =
                Cf.list(Notification.email(Duration.standardMinutes(-15)), Notification.sms(Duration.ZERO));

        val participant = new LayerData.Participant(user.getEmail(), LayerActionClass.EDIT);

        return LayerData.empty().toBuilder()
                .color(Option.of("#89abcd"))
                .notifications(notifications)
                .name(Option.of("Название"))
                .participants(Option.of(Cf.list(participant)))
                .build();
    }

    @Configuration
    public static class IcsImportConf {
        @Bean
        public IcsImportTask icsImportTask() {
            return new IcsImportTask();
        }

        @Bean
        public XivaNotificationManager xivaNotificationManager() {
            return mock(XivaNotificationManager.class);
        }
    }
}
