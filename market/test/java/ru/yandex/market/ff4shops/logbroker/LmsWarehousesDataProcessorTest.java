package ru.yandex.market.ff4shops.logbroker;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.stocks.service.WarehousesService;
import ru.yandex.market.logistics.management.entity.logbroker.BusinessWarehouseSnapshotDto;
import ru.yandex.market.logistics.management.entity.logbroker.EntityType;
import ru.yandex.market.logistics.management.entity.logbroker.EventDto;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.type.ExtendedShipmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

class LmsWarehousesDataProcessorTest extends FunctionalTest {

    private static final String TOPIC_NAME = "lmstopicname";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    private WarehousesService warehousesService;
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Test
    @DbUnitDataSet(before = "lmsWarehousesDataProcessorTest.before.csv",
    after = "lmsWarehousesDataProcessorTest.after.csv")
    public void insertAndUpdateWarehouseTest() throws ParseException {
        var dataProcessor = new LmsWarehousesDataProcessor(warehousesService);
        var messageBatch = buildMessageBatch(List.of(
                Map.entry(dateFormat.parse("1999-01-01 00:00:00"),
                new BusinessWarehouseSnapshotDto()
                    .setName("name1")
                    .setPartnerType(PartnerType.DROPSHIP)
                    .setShipmentType(ExtendedShipmentType.IMPORT)
                    .setAddress(Address.newBuilder()
                            .settlement("city")
                            .locationId(123)
                            .addressString("address")
                            .build())
                    .setPartnerId(1L)),
                Map.entry(dateFormat.parse("1980-01-01 00:00:00"),
                new BusinessWarehouseSnapshotDto()
                        .setName("name2")
                        .setPartnerType(PartnerType.DROPSHIP)
                        .setShipmentType(ExtendedShipmentType.IMPORT)
                        .setAddress(Address.newBuilder()
                                .settlement("city")
                                .locationId(124)
                                .addressString("address")
                                .build())
                        .setPartnerId(2L)),
                Map.entry(dateFormat.parse("2009-01-01 00:00:00"),
                new BusinessWarehouseSnapshotDto()
                        .setName("name3")
                        .setPartnerType(PartnerType.DROPSHIP)
                        .setShipmentType(ExtendedShipmentType.IMPORT)
                        .setAddress(Address.newBuilder()
                                .settlement("city")
                                .locationId(125)
                                .addressString("address")
                                .build())
                        .setPartnerId(3L)),
                Map.entry(dateFormat.parse("2009-01-01 00:00:00"),
                new BusinessWarehouseSnapshotDto()
                       .setName("name349")
                       .setPartnerType(PartnerType.DROPSHIP)
                       .setShipmentType(ExtendedShipmentType.IMPORT)
                       .setAddress(Address.newBuilder()
                                .settlement("city")
                                .locationId(125)
                                .addressString("address")
                                .build())
                        .setPartnerId(4L)
                        )));
        dataProcessor.process(messageBatch);
    }


    private static MessageBatch buildMessageBatch(List<Map.Entry<Date, BusinessWarehouseSnapshotDto>> data) {
        var meta = new MessageMeta("test".getBytes(), 0, 0, 0, "::1", CompressionCodec.RAW,
                Collections.emptyMap());
        AtomicInteger offeset = new AtomicInteger();
        var messageData = data.stream()
                .map(m -> new MessageData(toByteArray(createEventDto(m.getValue(),m.getKey())), offeset.getAndIncrement(), meta))
                .collect(Collectors.toList());
        return new MessageBatch(TOPIC_NAME, 0, messageData);
    }

    private static EventDto createEventDto(BusinessWarehouseSnapshotDto businessWarehouseSnapshotDto, Date date) {
        var eventDto = new EventDto();
        eventDto.setEntityType(EntityType.BUSINESS_WAREHOUSE);
        eventDto.setEventTimestamp(date.toInstant());
        eventDto.setEntitySnapshot(OBJECT_MAPPER.convertValue(businessWarehouseSnapshotDto, JsonNode.class));
        return  eventDto;
    }

    private static byte[] toByteArray(Object data) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
