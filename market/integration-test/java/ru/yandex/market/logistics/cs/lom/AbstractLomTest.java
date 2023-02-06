package ru.yandex.market.logistics.cs.lom;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.logbroker.lom.LomEventConsumer;
import ru.yandex.market.logistics.cs.repository.EventRepository;
import ru.yandex.market.logistics.cs.repository.QueueTaskTestRepository;
import ru.yandex.market.logistics.cs.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.enums.EntityType;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.PlatformClient;
import ru.yandex.market.logistics.lom.model.enums.PointType;

import static ru.yandex.market.logistics.cs.util.RouteBuilder.item;
import static ru.yandex.market.logistics.cs.util.RouteBuilder.route;
import static ru.yandex.market.logistics.cs.util.RouteBuilder.segment;
import static ru.yandex.market.logistics.cs.util.RouteBuilder.service;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.CUTOFF;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.DELIVERY;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.HANDING;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.INBOUND;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.LAST_MILE;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.MOVEMENT;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.PROCESSING;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.SHIPMENT;
import static ru.yandex.market.logistics.lom.model.enums.ServiceCodeName.SORT;

public class AbstractLomTest extends AbstractIntegrationTest {
    protected static final Long LOM_EVENT_ID = 1234567890L;
    protected static final String BARCODE = "LOtesting-12345";
    protected static final String OLD_LOM_ROUTE_UUID = "1137cab5-30b9-4696-a132-cbf431c3dee7";
    protected static final String LOM_ROUTE_UUID = "e54b0ebc-529d-44ce-9e00-02bdc2605479";

    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected EventRepository eventRepository;
    @Autowired
    protected LomEventConsumer lomEventConsumer;
    @Autowired
    protected QueueTaskTestRepository queueTaskRepository;

    @Nonnull
    protected CombinatorRoute.DeliveryRoute getRealRoute() {
        return route(
            segment(10)
                .type(PointType.WAREHOUSE)
                .partnerName("Основной склад")
                .partnerType(PartnerType.DROPSHIP)
                .partnerId(100)
                .services(
                    service(1).code(CUTOFF).start(today("00:00").plusDays(0)).items(items()),     // NOW
                    service(2).code(PROCESSING).start(today("10:00").plusDays(0)).items(items()), // NOW
                    service(3).code(SHIPMENT).start(today("10:00").plusDays(0)).items(items())    // NOW
                ),
            segment(20)
                .type(PointType.MOVEMENT)
                .partnerName("Основной склад")
                .partnerType(PartnerType.DROPSHIP)
                .partnerId(100)
                .services(
                    service(4).code(MOVEMENT).start(today("10:00").plusDays(0)).items(items()),   // NOW
                    service(5).code(SHIPMENT).start(today("20:00").plusDays(0)).items(items())    // NOW
                ),
            segment(30)
                .type(PointType.WAREHOUSE)
                .partnerName("Яндекс.Маркет DS")
                .partnerType(PartnerType.SORTING_CENTER)
                .partnerId(200)
                .services(
                    service(6).code(INBOUND).start(today("20:00").plusDays(0)).items(items()),    // NOW+1d from below
                    service(7).code(SORT).start(today("20:00").plusDays(0)).items(items()),       // NOW+1d from below
                    service(8).code(SHIPMENT).start(today("22:00").plusDays(0)).items(items())    // NOW+1d from below
                ),
            segment(40)
                .type(PointType.MOVEMENT)
                .partnerName("МК Тарный")
                .partnerType(PartnerType.DELIVERY)
                .partnerId(300)
                .services(
                    service(9).code(INBOUND).start(today("22:00").plusDays(0)).items(items()),    // NOW
                    service(10).code(MOVEMENT).start(today("00:00").plusDays(1)).items(items()),  // NOW+1d
                    service(11).code(SHIPMENT).start(today("04:00").plusDays(1)).items(items())   // NOW+1d
                ),
            segment(50)
                .type(PointType.LINEHAUL)
                .partnerName("МК Тарный")
                .partnerType(PartnerType.DELIVERY)
                .partnerId(300)
                .services(
                    service(12).code(DELIVERY).start(today("04:00").plusDays(1)).items(items()),  // NOW+1d
                    service(13).code(LAST_MILE).start(today("04:00").plusDays(1)).items(items())  // NOW+1d
                ),
            segment(60)
                .type(PointType.HANDING)
                .partnerName("МК Тарный")
                .partnerType(PartnerType.DELIVERY)
                .partnerId(300)
                .services(
                    service(14).code(HANDING).start(today("09:00").plusDays(1)).items(items())    // NOW+1d
                )
        );
    }

    @Nonnull
    protected LocalDateTime today(String time) {
        return LocalDateTime.of(DateTimeUtils.nowDayUtc(), LocalTime.parse(time));
    }

    @Nonnull
    protected List<EventDto> createLomEvent(PlatformClient platformClient, JsonNode diff) {
        return List.of(
            new EventDto()
                .setId(LOM_EVENT_ID)
                .setEntityType(EntityType.ORDER)
                .setDiff(diff)
                .setSnapshot(createSnapshot(platformClient))
                .setCreated(DateTimeUtils.nowUtc().toInstant(ZoneOffset.UTC))
        );
    }

    @Nonnull
    @SneakyThrows
    private JsonNode createSnapshot(PlatformClient platformClient) {
        return objectMapper.readTree(String.format(
            "{\"barcode\": \"%s\", \"routeUuid\": \"%s\", \"platformClientId\": %d}",
            BARCODE,
            LOM_ROUTE_UUID,
            platformClient.getId()
        ));
    }

    @Nonnull
    protected CombinatorRoute.ProcessedItem[] items() {
        return new CombinatorRoute.ProcessedItem[]{
            item(0).quantity(30),
            item(1).quantity(5),
            item(2).quantity(7),
            item(3).quantity(8)
        };
    }
}
