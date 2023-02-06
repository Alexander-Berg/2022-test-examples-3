package ru.yandex.market.delivery.transport_manager.yt;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.ScheduleMetaData;
import ru.yandex.market.delivery.transport_manager.domain.enums.ConfigTransportationType;
import ru.yandex.market.delivery.transport_manager.domain.enums.DimensionsClass;
import ru.yandex.market.delivery.transport_manager.domain.yt.PartnerCutoffData;
import ru.yandex.market.delivery.transport_manager.domain.yt.RoutingConfigDto;
import ru.yandex.market.delivery.transport_manager.domain.yt.TransportationConfigDto;
import ru.yandex.market.delivery.transport_manager.domain.yt.YtHoliday;
import ru.yandex.market.delivery.transport_manager.domain.yt.YtSchedule;
import ru.yandex.market.delivery.transport_manager.repository.mappers.YtScheduleMetadataMapper;
import ru.yandex.market.delivery.transport_manager.service.yt.YtReader;
import ru.yandex.market.delivery.transport_manager.service.yt.YtScheduleReader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleYtReaderTest extends AbstractContextualTest {

    private static final String HASH = "hash1";
    YtScheduleReader scheduleReader;

    @Autowired
    YtScheduleMetadataMapper scheduleMapper;

    @Test
    void testIncomingData() {
        mockYt();
        ArrayList<TransportationConfigDto> schedules = new ArrayList<>();
        scheduleReader.populateIfUpdated(schedules, new ScheduleMetaData(1592928073L, "2"));
        softly.assertThat(schedules).isEqualTo(List.of(
            TransportationConfigDto.builder()
                .outboundPartnerId(1L)
                .outboundLogisticsPointId(2L)
                .movingPartnerId(10L)
                .movementSegmentId(101L)
                .inboundPartnerId(100L)
                .inboundLogisticsPointId(1000L)
                .transportationSchedule(List.of(
                    new YtSchedule(1L, 1, LocalTime.of(9, 0, 0), LocalTime.of(21, 0, 0), 5, 1L)
                ))
                .transportationHolidays(List.of(
                    new YtHoliday(LocalDate.of(2021, 5, 10), 1L),
                    new YtHoliday(LocalDate.of(2021, 5, 11), 100L)
                ))
                .volume(500)
                .weight(50)
                .duration(24)
                .transportationType(ConfigTransportationType.ORDERS_RETURN)
                .partnerCutoffData(
                    PartnerCutoffData.builder()
                        .cutoffTime(LocalTime.of(16, 0))
                        .warehouseOffsetSeconds(3 * 60 * 60)
                        .handlingTimeDays(1)
                        .build()
                )
                .routingConfig(new RoutingConfigDto(
                    true,
                    DimensionsClass.MEDIUM_SIZE_CARGO,
                    1.1D,
                    false,
                    "DEFAULT"
                ))
                .hash(HASH)
                .build()
        ));
    }

    private void mockYt() {
        Yt ytMock = mock(Yt.class);
        YtTables tablesMock = mock(YtTables.class);
        Cypress cypressMock = mock(Cypress.class);

        scheduleReader = new YtScheduleReader(
            new YtReader(ytMock, ytMock)
        );
        scheduleReader.setTablePrefix("//home/market/testing/delivery/logistics_management_system" +
            "/transportation_schedules");

        when(ytMock.tables()).thenReturn(tablesMock);
        when(ytMock.cypress()).thenReturn(cypressMock);
        when(cypressMock.list(any(YPath.class))).thenReturn(Cf.arrayList(YTree.stringNode("1592928074_3")));
        doAnswer((invocation) -> {
            Consumer<YTreeMapNode> argument = invocation.getArgument(2);
            emitFakeNodes().forEach(argument::accept);
            return null;
        }).when(tablesMock).read(any(), any(), any(Consumer.class));
    }

    private static List<YTreeMapNode> emitFakeNodes() {
        return List.of(
            YTree.mapBuilder()
                .key("outboundPartnerId").value(1)
                .key("outboundLogisticsPointId").value(2)
                .key("movingPartnerId").value(10)
                .key("inboundPartnerId").value(100)
                .key("inboundLogisticsPointId").value(1000)
                .key("movementSegmentId").value(101)
                .key("duration").value(24)
                .key("volume").value(500)
                .key("weight").value(50)
                .key("hash").value(HASH)
                .key("transportationSchedule").value(
                new YTreeBuilder()
                    .beginList()
                    .value(YTree.mapBuilder()
                        .key("id").value(1)
                        .key("day").value(1)
                        .key("timeFrom").value("09:00")
                        .key("timeTo").value("21:00")
                        .key("pallets").value(5)
                        .key("transportId").value(1)
                        .buildMap()
                    ).endList()
                    .build()
            )
                .key("routingConfig").value(
                    YTree.mapBuilder()
                        .key("enabled").value(true)
                        .key("dimensionsClass").value("MEDIUM_SIZE_CARGO")
                        .key("expectedVolume").value(new byte[]{-128, 0, 0, 11})
                        .key("expectedVolumeDouble").value(1.1)
                        .key("excludeFromLocationGroup").value(false)
                        .key("locationGroupTag").value("DEFAULT")
                        .buildMap()
                )
                .key("transportationHolidays").value(
                new YTreeBuilder()
                    .beginList()
                    .value(
                        YTree.mapBuilder()
                            .key("day").value("2021-05-10")
                            .key("partnerId").value(1L)
                            .buildMap()
                    )
                    .value(
                        YTree.mapBuilder()
                            .key("day").value("2021-05-11")
                            .key("partnerId").value(100L)
                            .buildMap()
                    )
                    .endList()
                    .build()
            )
                .key("transportationType").value("ORDERS_RETURN")
                .key("partnerCutoffData").value(
                    YTree.mapBuilder()
                        .key("cutoffTime").value("16:00")
                        .key("warehouseOffsetSeconds").value(3 * 60 * 60)
                        .key("handlingTimeDays").value(1)
                        .buildMap()
                )
                .buildMap()
        );
    }
}
