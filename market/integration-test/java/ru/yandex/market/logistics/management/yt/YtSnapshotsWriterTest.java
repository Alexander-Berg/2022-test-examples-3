package ru.yandex.market.logistics.management.yt;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.front.deliveryInterval.YtDeliveryIntervalSnapshotDto;
import ru.yandex.market.logistics.management.domain.entity.PartnerDeliveryIntervalSnapshot;
import ru.yandex.market.logistics.management.repository.PartnerDeliveryIntervalSnapshotRepository;
import ru.yandex.market.logistics.management.service.yt.YtDeliveryIntervalSnapshotsTableWriter;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.yt.ytclient.proxy.request.ObjectType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.service.yt.YtDeliveryIntervalSnapshotsTableWriter.CALENDAR_SCHEMA;
import static ru.yandex.market.logistics.management.service.yt.YtDeliveryIntervalSnapshotsTableWriter.SCHEDULE_SCHEMA;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@DatabaseSetup("/data/controller/admin/deliveryIntervalSnapshots/expired_snapshots.xml")
class YtSnapshotsWriterTest extends AbstractContextualTest {

    @Autowired
    YtDeliveryIntervalSnapshotsTableWriter writer;
    @Autowired
    Yt yt;
    @Autowired
    @Qualifier("backup")
    Yt backupYt;
    @Mock
    YtTables tables;
    @Mock
    Cypress cypress;
    @Autowired
    private TestableClock clock;

    @Autowired
    private PartnerDeliveryIntervalSnapshotRepository snapshotRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Map<Integer, String> regionsMap = Map.of(
        12, "Новосибирск",
        101, "Москва",
        102, "Смоленск"
    );

    @BeforeEach
    void setup() {
        LocalDate today = LocalDate.of(2020, 12, 4);
        clock.setFixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        when(yt.tables()).thenReturn(tables);
        when(yt.cypress()).thenReturn(cypress);
        doNothing().when(tables).write(any(), any(), anyIterable());
        doNothing().when(cypress).create(any(), any(), anyBoolean(), anyBoolean(), any());
    }

    @AfterEach
    void teardown() {
        Mockito.verifyNoMoreInteractions(backupYt);
    }

    @Test
    void tablesCreationTest() {

        transactionTemplate.execute(ts -> {
            PartnerDeliveryIntervalSnapshot snapshot = snapshotRepository.findById(2L).get();
            writer.initYtUpload(List.of(YtDeliveryIntervalSnapshotDto.toDto(snapshot, regionsMap)));
            String expectedScheduleName = getExpectedScheduleName(snapshot);
            String expectedCalendarName = getExpectedCalendarName(snapshot);

            verify(tables).write(eq(YPath.simple(expectedScheduleName)), any(), anyIterable());
            verify(tables).write(eq(YPath.simple(expectedCalendarName)), any(), anyIterable());

            verify(cypress).create(Mockito.argThat(argument ->
                argument.getPath().equals(YPath.simple(expectedScheduleName)) &&
                    argument.getType().equals(ObjectType.Table) &&
                    argument.getAttributes().equals(formScheduleAttributes()))
            );

            verify(cypress).create(Mockito.argThat(argument ->
                argument.getPath().equals(YPath.simple(expectedCalendarName)) &&
                    argument.getType().equals(ObjectType.Table) &&
                    argument.getAttributes().equals(formCalendarAttributes()))
            );

            return null;
        });
    }

    @Test
    void noCalendarTableCreatedWhenNullCalendarTest() {
        transactionTemplate.execute(ts -> {
            PartnerDeliveryIntervalSnapshot snapshot = snapshotRepository.findById(5L).get();
            writer.initYtUpload(List.of(YtDeliveryIntervalSnapshotDto.toDto(snapshot, regionsMap)));
            String expectedScheduleName = getExpectedScheduleName(snapshot);

            verify(tables).write(eq(YPath.simple(expectedScheduleName)), any(), anyIterable());

            verify(cypress).create(Mockito.argThat(argument ->
                argument.getPath().equals(YPath.simple(expectedScheduleName)) &&
                    argument.getType().equals(ObjectType.Table) &&
                    argument.getAttributes().equals(formScheduleAttributes()))
            );

            return null;
        });
    }

    private MapF<String, YTreeNode> formScheduleAttributes() {
        return formAttributes(SCHEDULE_SCHEMA);
    }

    private MapF<String, YTreeNode> formCalendarAttributes() {
        return formAttributes(CALENDAR_SCHEMA);
    }

    private MapF<String, YTreeNode> formAttributes(YTreeNode schema) {
        return Cf.map(
            "schema", schema,
            "expiration_time", YTree.stringNode("2021-01-03T00:00")
        );
    }

    private String getExpectedScheduleName(PartnerDeliveryIntervalSnapshot snapshot) {
        return getExpectedName(snapshot, "schedules");
    }

    private String getExpectedCalendarName(PartnerDeliveryIntervalSnapshot snapshot) {
        return getExpectedName(snapshot, "calendars");
    }

    private String getExpectedName(PartnerDeliveryIntervalSnapshot snapshot, String entity) {
        String loadedTime = snapshot.getLoaded().toString();

        return String.format(
            "%s/%s/%d_%s_%d",
            "//home/yt",
            entity,
            snapshot.getPartner().getId(),
            loadedTime,
            snapshot.getId()
        );
    }

}
