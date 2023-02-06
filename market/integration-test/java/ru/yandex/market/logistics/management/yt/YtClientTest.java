package ru.yandex.market.logistics.management.yt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.yt.TransportationSchedule;
import ru.yandex.market.logistics.management.domain.entity.yt.TransportationType;
import ru.yandex.market.logistics.management.domain.entity.yt.YtHoliday;
import ru.yandex.market.logistics.management.domain.entity.yt.YtRoutingConfig;
import ru.yandex.market.logistics.management.domain.entity.yt.YtSchedule;
import ru.yandex.market.logistics.management.service.yt.YtScheduleTableWriter;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.yt.utils.YtSchemaBuilder;
import ru.yandex.yt.ytclient.proxy.request.ObjectType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class YtClientTest extends AbstractContextualTest {

    @Autowired
    YtScheduleTableWriter writer;

    @Autowired
    Yt yt;

    @Autowired
    @Qualifier("backup")
    Yt backupYt;

    YtTables tables;
    Cypress cypress;
    YtTables backupTables;
    Cypress backupCypress;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        cypress = Mockito.mock(Cypress.class);
        tables = Mockito.mock(YtTables.class);

        backupCypress = Mockito.mock(Cypress.class);
        backupTables = Mockito.mock(YtTables.class);

        LocalDate today = LocalDate.of(2020, 6, 17);
        clock.setFixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    }

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(tables, cypress, backupTables, backupCypress);
    }

    @Test
    void mockYtTest() {
        mockYt(yt, cypress, tables);
        mockYt(backupYt, backupCypress, backupTables);

        long timestamp = 1592352000L;
        long hash = -1852943667;
        String expectedName = String.format("%s/%s_%s", "//some/yt/path", timestamp, hash);
        writer.initYtUpload(List.of(
            TransportationSchedule.builder()
                .outboundPartnerId(1L)
                .outboundLogisticsPointId(10L)
                .movingPartnerId(1L)
                .movementSegmentId(100L)
                .inboundPartnerId(2L)
                .inboundLogisticsPointId(20L)
                .transportationSchedule(getWeekDaySchedule())
                .transportationHolidays(List.of(
                    new YtHoliday(LocalDate.of(2021, 5, 3), 1L),
                    new YtHoliday(LocalDate.of(2021, 5, 5), 2L)
                ))
                .volume(2000)
                .weight(200)
                .duration(24)
                .transportationType(TransportationType.ORDERS_OPERATION)
                .build()
        ));
        verifyYt(expectedName, tables, cypress);
        verifyYt(expectedName, backupTables, backupCypress);
    }

    @Test
    void withNullMover() {
        mockYt(yt, cypress, tables);
        mockYt(backupYt, backupCypress, backupTables);

        long timestamp = 1592352000L;
        long hash = -1948289057;
        String expectedName = String.format("%s/%s_%s", "//some/yt/path", timestamp, hash);
        writer.initYtUpload(List.of(
            TransportationSchedule.builder()
                .outboundPartnerId(1L)
                .outboundLogisticsPointId(10L)
                .inboundPartnerId(2L)
                .inboundLogisticsPointId(20L)
                .transportationSchedule(getWeekDaySchedule())
                .transportationHolidays(List.of(
                    new YtHoliday(LocalDate.of(2021, 5, 3), 1L),
                    new YtHoliday(LocalDate.of(2021, 5, 5), 2L)
                ))
                .volume(2000)
                .weight(200)
                .duration(24)
                .transportationType(TransportationType.ORDERS_OPERATION)
                .routingConfig(new YtRoutingConfig(
                    true,
                    null,
                    BigDecimal.ZERO,
                    0d,
                    false,
                    ""
                ))
                .build()
        ));
        verifyYt(expectedName, tables, cypress);
        verifyYt(expectedName, backupTables, backupCypress);
    }

    private void verifyYt(String expectedName, YtTables tables, Cypress cypress) {
        verify(tables).write(eq(YPath.simple(expectedName)), any(), anyIterable());
        verify(cypress).create(Mockito.argThat(argument ->
            argument.getPath().equals(YPath.simple(expectedName)) &&
                argument.getType().equals(ObjectType.Table) &&
                argument.getAttributes().equals(getExpectedAttributes())));
    }

    private void mockYt(Yt yt, Cypress cypress, YtTables tables) {
        when(yt.tables()).thenReturn(tables);
        when(yt.cypress()).thenReturn(cypress);
        doNothing().when(tables).write(any(), any(), anyIterable());
        doNothing().when(cypress).create(any(), any(), anyBoolean(), anyBoolean(), any());
    }

    private List<YtSchedule> getWeekDaySchedule() {
        return List.of(
            new YtSchedule(1L, 1, LocalTime.of(10, 0), LocalTime.of(18, 0), false),
            new YtSchedule(2L, 2, LocalTime.of(10, 0), LocalTime.of(18, 0), false),
            new YtSchedule(3L, 3, LocalTime.of(10, 0), LocalTime.of(18, 0), false),
            new YtSchedule(4L, 4, LocalTime.of(10, 0), LocalTime.of(18, 0), false),
            new YtSchedule(5L, 5, LocalTime.of(10, 0), LocalTime.of(18, 0), false)
        );
    }

    private MapF<String, YTreeNode> getExpectedAttributes() {
        return Cf.map(
            "schema", new YtSchemaBuilder()
                .field("outboundPartnerId", "int64", true)
                .field("outboundLogisticsPointId", "int64", true)
                .field("movingPartnerId", "int64", false)
                .field("movementSegmentId", "int64", false)
                .field("inboundPartnerId", "int64", true)
                .field("inboundLogisticsPointId", "int64", true)
                .field("duration", "int32", true)
                .field("volume", "int32", true)
                .field("weight", "int32", true)
                .field("hash", "string", true)
                .field("transportationSchedule", "any", false)
                .field("transportationHolidays", "any", false)
                .field("transportationType", "string", true)
                .field("partnerCutoffData", "any", false)
                .field("routingConfig", "any", false)
                .build(),
            "expiration_time", YTree.stringNode("2020-06-17T00:10")
        );
    }
}
