package ru.yandex.market.delivery.logbroker;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logistics.management.entity.logbroker.BusinessWarehouseSnapshotDto;
import ru.yandex.market.logistics.management.entity.logbroker.EntityType;
import ru.yandex.market.logistics.management.entity.logbroker.EventDto;
import ru.yandex.market.shop.FunctionalTest;
import ru.yandex.market.utils.logbroker.MessageBatchBuilder;
import ru.yandex.market.utils.logbroker.MessageBatchItem;

/**
 * Тесты для {@link LmsEventsLogbrokerConsumer}.
 */
@DbUnitDataSet(before = "LmsEventsLogbrokerConsumerTest.csv")
public class LmsEventsLogbrokerConsumerTest extends FunctionalTest {
    @Autowired
    private LmsEventsLogbrokerConsumer lmsEventsLogbrokerConsumer;

    @Test
    @DbUnitDataSet(after = "LmsEventsLogbrokerConsumerTest.csv")
    void testWarehouseNotExists() {
        lmsEventsLogbrokerConsumer.process(buildMessageBatch(100L, "newName"));
    }

    @Test
    @DbUnitDataSet(after = "LmsEventsLogbrokerConsumerTest.csv")
    void testFby() {
        lmsEventsLogbrokerConsumer.process(buildMessageBatch(200L, "newName"));
    }

    @Test
    @DbUnitDataSet(after = "LmsEventsLogbrokerConsumerTest.csv")
    void testOldMultiWarehouses() {
        lmsEventsLogbrokerConsumer.process(buildMessageBatch(300L, "newName"));
    }

    @Test
    @DbUnitDataSet(after = "LmsEventsLogbrokerConsumerTest.csv")
    void testNameNotChanged() {
        lmsEventsLogbrokerConsumer.process(buildMessageBatch(400L, "oldName"));
    }

    @Test
    @DbUnitDataSet(after = "LmsEventsLogbrokerConsumerTest.testSuccessFbs.after.csv")
    void testSuccessFbs() {
        lmsEventsLogbrokerConsumer.process(buildMessageBatch(400L, "newName"));
    }

    @Test
    @DbUnitDataSet(after = "LmsEventsLogbrokerConsumerTest.testSuccessDbs.after.csv")
    void testSuccessDbs() {
        lmsEventsLogbrokerConsumer.process(buildMessageBatch(500L, "newName"));
    }

    @Test
    void testDeserialize() throws IOException {
        String json = "{\n" +
                "  \"partnerId\": 57812,\n" +
                "  \"partnerType\": \"DROPSHIP\",\n" +
                "  \"shipmentType\": \"EXPRESS\",\n" +
                "  \"name\": \"Первый склад для единой страницы\",\n" +
                "  \"readableName\": \"Первый склад для единой страницы\",\n" +
                "  \"businessId\": 11391746,\n" +
                "  \"marketId\": 2000673,\n" +
                "  \"externalId\": \"d5aa738f-1c8f-40\\ncc-ad98-f4127ce2544e\",\n" +
                "  \"phones\": [\n" +
                "    {\n" +
                "      \"number\": \"3434\",\n" +
                "      \"internalNumber\": \"343434\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        BusinessWarehouseSnapshotDto businessWarehouseSnapshotDto =
                LmsEventsLogbrokerConsumer.OBJECT_MAPPER.readValue(json, BusinessWarehouseSnapshotDto.class);
        Assertions.assertEquals(businessWarehouseSnapshotDto.getPhones().stream().findAny().get().getNumber(), "3434");
    }

    private static MessageBatch buildMessageBatch(long serviceId, String name) {
        return new MessageBatchBuilder<WarehouseItem>()
                .addItem(
                        new WarehouseItem(new BusinessWarehouseSnapshotDto()
                                .setPartnerId(serviceId)
                                .setReadableName(name))
                ).build();
    }

    private static class WarehouseItem implements MessageBatchItem {

        private final EventDto eventDto;

        public WarehouseItem(BusinessWarehouseSnapshotDto businessWarehouseSnapshotDto) {
            EventDto eventDto = new EventDto();
            eventDto.setEntityType(EntityType.BUSINESS_WAREHOUSE);
            eventDto.setEntitySnapshot(LmsEventsLogbrokerConsumer.OBJECT_MAPPER.convertValue(businessWarehouseSnapshotDto,
                    JsonNode.class));
            this.eventDto = eventDto;
        }

        @Override
        public byte[] toByteArray() {
            try {
                return LmsEventsLogbrokerConsumer.OBJECT_MAPPER.writeValueAsBytes(eventDto);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
