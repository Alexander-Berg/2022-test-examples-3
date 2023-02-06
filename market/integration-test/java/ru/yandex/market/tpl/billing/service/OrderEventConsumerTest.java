package ru.yandex.market.tpl.billing.service;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.enums.EntityType;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.checker.QueueTaskChecker;
import ru.yandex.market.tpl.billing.repository.EventRepository;
import ru.yandex.market.tpl.billing.service.logbroker.OrderEventConsumer;

import static ru.yandex.market.tpl.billing.util.TestUtils.extractFileContent;

class OrderEventConsumerTest extends AbstractFunctionalTest {
    private static final Long LOM_EVENT_ID = 133L;
    private static final String JSON_PATH_PREFIX = "request/service/ordereventconsumer/event";
    @Autowired
    OrderEventConsumer orderEventConsumer;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private QueueTaskChecker queueTaskChecker;
    @Autowired
    private EventRepository eventRepository;

    @Test
    @DbUnitDataSet(
            before = "/database/service/ordereventconsumer/before/100136_partner_is_market_courier_sc.csv",
            after = "/database/service/ordereventconsumer/after/event_creation_success.csv")
    void orderLeftMarketCourierSc() throws Exception {
        JsonNode diff = jsonNodeOf(JSON_PATH_PREFIX + "/diff/segment_status_updated_to_out.json");
        JsonNode snapshot = jsonNodeOf(JSON_PATH_PREFIX + "/snapshot/order_left_market_courier_sc.json");

        EventDto eventDto = createEventDto(diff, snapshot);
        orderEventConsumer.accept(List.of(eventDto));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/ordereventconsumer/before/100136_partner_is_dropoff_pickup_point.csv",
            after = "/database/service/ordereventconsumer/after/pickup_point_event_creation_success.csv")
    void orderLeftMarketDropoffPickupPoint() throws Exception {
        JsonNode diff = jsonNodeOf(JSON_PATH_PREFIX + "/diff/pickup_point_segment_status_updated_to_out.json");
        JsonNode snapshot = jsonNodeOf(JSON_PATH_PREFIX + "/snapshot/order_left_market_dropoff_pickup_point.json");

        EventDto eventDto = createEventDto(diff, snapshot);
        orderEventConsumer.accept(List.of(eventDto));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/ordereventconsumer/before/returned_order_left_sc.csv",
            after = "/database/service/ordereventconsumer/after/returned_order_left_sc_success.csv")
    void returnedOrderLeftMarketCourierSc() throws Exception {
        JsonNode diff = jsonNodeOf(JSON_PATH_PREFIX + "/diff/segment_status_updated_to_return_preparing_sender.json");
        JsonNode snapshot = jsonNodeOf(JSON_PATH_PREFIX + "/snapshot/returned_order_left_market_courier_sc.json");

        EventDto eventDto = createEventDto(diff, snapshot);
        orderEventConsumer.accept(List.of(eventDto));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/ordereventconsumer/before/order_is_stored_at_sc.csv",
            after = "/database/service/ordereventconsumer/after/order_is_stored_at_sc_success.csv")
    void orderIsStoredAtMarketCourierSc() throws Exception {
        JsonNode diff = jsonNodeOf(
                JSON_PATH_PREFIX + "/diff/segment_status_updated_to_transit_awaiting_clarification.json"
        );
        JsonNode snapshot = jsonNodeOf(JSON_PATH_PREFIX + "/snapshot/order_is_stored_at_market_courier_sc.json");

        EventDto eventDto = createEventDto(diff, snapshot);
        orderEventConsumer.accept(List.of(eventDto));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/ordereventconsumer/before/100136_partner_is_not_market_courier_sc.csv",
            after = "/database/service/ordereventconsumer/before/clear.csv")
    void orderLeftNotMarketCourierSc() throws Exception {
        JsonNode diff = jsonNodeOf(JSON_PATH_PREFIX + "/diff/segment_status_updated_to_out.json");
        JsonNode snapshot = jsonNodeOf(JSON_PATH_PREFIX + "/snapshot/order_left_market_courier_sc.json");

        EventDto eventDto = createEventDto(diff, snapshot);
        orderEventConsumer.accept(List.of(eventDto));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/ordereventconsumer/before/100136_partner_is_market_courier_sc.csv",
            after = "/database/service/ordereventconsumer/before/clear.csv")
    void orderReceivedStatusIsNotOut() throws Exception {
        JsonNode diff = jsonNodeOf(JSON_PATH_PREFIX + "/diff/segment_status_updated_to_info_received.json");
        JsonNode snapshot = jsonNodeOf(JSON_PATH_PREFIX + "/snapshot/order_left_market_courier_sc.json");

        EventDto eventDto = createEventDto(diff, snapshot);
        orderEventConsumer.accept(List.of(eventDto));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/ordereventconsumer/before/100136_partner_is_not_market_courier_sc.csv",
            after = "/database/service/ordereventconsumer/before/clear.csv")
    void orderLeftNotMarketCourierScPlaceHasNoDimensions() throws Exception {
        JsonNode diff = jsonNodeOf(JSON_PATH_PREFIX + "/diff/segment_status_updated_to_out.json");
        JsonNode snapshot = jsonNodeOf(JSON_PATH_PREFIX + "/snapshot/place_has_no_dimensions.json");

        EventDto eventDto = createEventDto(diff, snapshot);
        orderEventConsumer.accept(List.of(eventDto));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/ordereventconsumer/before/100136_partner_is_market_courier_sc.csv",
            after = "/database/service/ordereventconsumer/before/clear.csv")
    void orderStatusNotChanged() throws Exception {
        JsonNode diff = jsonNodeOf(JSON_PATH_PREFIX + "/diff/order_external_id_changed.json");
        JsonNode snapshot = jsonNodeOf(JSON_PATH_PREFIX + "/snapshot/order_left_market_courier_sc.json");

        EventDto eventDto = createEventDto(diff, snapshot);
        orderEventConsumer.accept(List.of(eventDto));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/ordereventconsumer/before/100136_partner_is_market_courier_sc.csv",
            after = "/database/service/ordereventconsumer/before/clear.csv")
    void rootStorageUnitHasNoDimensions() throws Exception {
        JsonNode diff = jsonNodeOf(JSON_PATH_PREFIX + "/diff/order_external_id_changed.json");
        JsonNode snapshot = jsonNodeOf(JSON_PATH_PREFIX + "/snapshot/root_storage_unit_has_no_dimensions.json");

        EventDto eventDto = createEventDto(diff, snapshot);
        orderEventConsumer.accept(List.of(eventDto));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/ordereventconsumer/before/100136_partner_is_market_courier_sc.csv",
            after = "/database/service/ordereventconsumer/before/clear.csv")
    void rootStorageUnitHasNoType() throws Exception {
        JsonNode diff = jsonNodeOf(JSON_PATH_PREFIX + "/diff/order_external_id_changed.json");
        JsonNode snapshot = jsonNodeOf(JSON_PATH_PREFIX + "/snapshot/storage_unit_has_no_type.json");

        EventDto eventDto = createEventDto(diff, snapshot);
        orderEventConsumer.accept(List.of(eventDto));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/ordereventconsumer/before/dbs_order.csv",
            after = "/database/service/ordereventconsumer/after/dbs_order_income.csv")
    void testDbsOrderIncome() throws Exception {
        JsonNode diff = jsonNodeOf(JSON_PATH_PREFIX + "/diff/segment_status_updated_to_transit_pickup.json");
        JsonNode snapshot = jsonNodeOf(JSON_PATH_PREFIX + "/snapshot/dbs_order_income.json");

        EventDto eventDto = createEventDto(diff, snapshot);
        orderEventConsumer.accept(List.of(eventDto));
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "/database/service/ordereventconsumer/before/dbs_order.csv")
    @MethodSource("dbsOrderDontSavedData")
    void testDbsOrderDoesNotSaved(
            String diffPath,
            String snapshotPath
    ) throws Exception {
        JsonNode diff = jsonNodeOf(JSON_PATH_PREFIX + diffPath);
        JsonNode snapshot = jsonNodeOf(JSON_PATH_PREFIX + snapshotPath);

        EventDto eventDto = createEventDto(diff, snapshot);
        orderEventConsumer.accept(List.of(eventDto));

        Assertions.assertThat(eventRepository.findAll()).isEmpty();
    }

    private static Stream<Arguments> dbsOrderDontSavedData() {
        return Stream.of(
                Arguments.of(
                        "/diff/segment_status_updated_to_transit_pickup.json",
                        "/snapshot/dbs_order_without_transit_pickup_status.json"
                ),
                Arguments.of(
                        "/diff/segment_status_updated_to_transit_pickup.json",
                        "/snapshot/dbs_order_without_transit_pickup_status.json"
                ),
                Arguments.of(
                        "/diff/segment_status_updated_to_transit_pickup.json",
                        "/snapshot/dbs_order_without_dbs_flag.json"
                )
        );
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/ordereventconsumer/before/dbs_order.csv",
            after = "/database/service/ordereventconsumer/after/dbs_order_outcome.csv")
    void testDbsOrderOutcome() throws Exception {
        JsonNode diff = jsonNodeOf(JSON_PATH_PREFIX + "/diff/segment_status_updated_to_return_arrived.json");
        JsonNode snapshot = jsonNodeOf(JSON_PATH_PREFIX + "/snapshot/dbs_order_outcome.json");

        EventDto eventDto = createEventDto(diff, snapshot);
        orderEventConsumer.accept(List.of(eventDto));
    }

    private JsonNode jsonNodeOf(String path) throws java.io.IOException {
        return objectMapper.readValue(
                extractFileContent(path),
                JsonNode.class
        );
    }

    @Nonnull
    private EventDto createEventDto(JsonNode diff, JsonNode snapshot) {
        return new EventDto()
            .setId(LOM_EVENT_ID)
            .setEntityType(EntityType.ORDER)
            .setDiff(diff)
            .setSnapshot(snapshot);
    }

}
