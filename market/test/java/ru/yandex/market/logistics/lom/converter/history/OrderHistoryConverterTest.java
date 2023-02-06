package ru.yandex.market.logistics.lom.converter.history;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.POJONode;
import org.assertj.core.api.Assertions;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.FieldPredicates;
import org.jeasy.random.api.Randomizer;
import org.jeasy.random.randomizers.AbstractRandomizer;
import org.jeasy.random.randomizers.misc.UUIDRandomizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.configuration.properties.TrackerCheckpointProcessingProperties;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegmentStatusHistory;
import ru.yandex.market.logistics.lom.entity.embedded.cancellation.reasondetails.MissingItemsCancellationOrderRequestReasonDetails;
import ru.yandex.market.logistics.lom.entity.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.entity.enums.WaybillStatus;
import ru.yandex.market.logistics.lom.entity.items.OrderItem;
import ru.yandex.market.logistics.lom.entity.items.StorageUnit;
import ru.yandex.market.logistics.lom.service.order.history.JsonDiffComputer;
import ru.yandex.market.logistics.lom.service.order.history.converter.HistoryOrderConverter;
import ru.yandex.market.logistics.lom.service.order.history.dto.HistoryOrder;

@DisplayName("Конвертация изменений в заказе в историю изменений заказа")
class OrderHistoryConverterTest extends AbstractTest {
    /**
     * Поля заказа, которые игнорируются при сохранении изменений.
     */
    private static final Set<String> NOT_COMPARING = Stream.of(
        // рекурсия
        "balancePayment.order",
        "balancePayment.updated",
        "billingEntity.charges.billingEntity",
        "billingEntity.order",
        "places.items.place",
        "places.order",
        "waybill.order",
        "orderContacts.order",
        "cancellationRequests.cancellationSegmentRequests",
        "cancellationRequests.order",
        "items.order",
        "items.updated",
        "items.boxes.item",
        "items.boxes.units.updated",
        "items.boxes.updated",
        "waybill.storageUnit.parent",
        "waybill.storageUnit.children",
        "waybill.storageUnit.order",
        "waybill.storageUnit.updated",
        "items.boxes.units.storageUnit.parent",
        "items.boxes.units.storageUnit.children",
        "items.boxes.units.storageUnit.order",
        "items.boxes.units.storageUnit.boxes",
        "items.boxes.units.storageUnit.updated",
        "items.boxes.units.orderItemBox",
        "units.parent",
        "units.children",
        "units.order",
        "units.updated",
        "orderItemNotFoundRequests",
        "orderChangedByPartnerRequest",
        "orderItemIsNotSuppliedRequests.order",
        "waybill.waybillSegmentStatusHistory.waybillSegment",
        "labels.order",
        "changeOrderRequests.changeOrderSegmentRequests",
        "changeOrderRequests.order",
        "changeOrderRequests.changeOrderRequestPayloads.changeOrderRequest",
        "changeOrderRequests.waybillSegment",
        "waybill.waybillSegmentTags.waybillSegment",
        "waybill.returnWaybillSegment",
        "orderStatusHistory.order",

        // не сохраняются в историю
        "waybill.partnerSettings",
        "billingEntity.transactions",
        "waybill.storageUnit.boxes",
        "units.boxes",
        "orderTags",
        "cancellationRequests.reasonDetails",
        "units.storageUnitIndex",
        "waybill.storageUnit.storageUnitIndex",
        "items.boxes.units.storageUnit.storageUnitIndex",
        "waybill.transferCodesString",
        "orderReturns",
        "waybill.waybillStatus",
        "waybill.waybillSegmentStatusHistory.unprocessed",

        // генерируемые поля, сохранять в историю нет особого смысла
        "balancePayment.id",
        "id",
        "orderContacts.contact.id",
        "orderContacts.contact.searchString",
        "orderContacts.id",
        "places.id",
        "places.items.id",
        "returnSortingCenterWarehouse.contact.id",
        "returnSortingCenterWarehouse.contact.searchString",
        "waybill.id",
        "waybill.waybillSegmentIndex",
        "waybill.waybillShipment.locationFrom.contact.id",
        "waybill.waybillShipment.locationFrom.contact.searchString",
        "waybill.waybillShipment.locationTo.contact.id",
        "waybill.waybillShipment.locationTo.contact.searchString",
        "waybill.warehouseLocation.contact.id",
        "waybill.warehouseLocation.contact.searchString",
        "waybill.returnWarehouseLocation.contact.id",
        "waybill.returnWarehouseLocation.contact.searchString",
        "waybill.storageUnit.id",
        "items.id",
        "items.boxes.id",
        "items.boxes.units.id",
        "items.boxes.units.storageUnit.id",
        "units.id",
        "waybill.waybillSegmentStatusHistory.date",
        "waybill.waybillSegmentStatusHistory.trackerCheckpointId",
        "waybill.waybillSegmentStatusHistory.trackerStatus",
        "waybill.waybillSegmentStatusHistory.additionalData",
        "changeOrderRequests.id",

        // От waybill.shipment в историю сохраняется только waybill.shipment.id
        "waybill.shipment.created",
        "waybill.shipment.marketIdFrom",
        "waybill.shipment.marketIdTo",
        "waybill.shipment.partnerIdTo",
        "waybill.shipment.partnerType",
        "waybill.shipment.shipmentType",
        "waybill.shipment.shipmentDate",
        "waybill.shipment.warehouseFrom",
        "waybill.shipment.warehouseTo",
        "waybill.shipment.waybill",
        "waybill.shipment.shipmentApplications",
        "waybill.shipment.registry",
        "waybill.shipment.billingEntity",
        "waybill.shipment.fake",

        // Неизменяемые поля
        "cancellationRequests.reason.reasonDetailsClass",
        "cancellationRequests.reason.name",
        "sender.warehouseId"
    )
        .collect(Collectors.toSet());

    private static final EasyRandomParameters RANDOM_PARAMETERS = new EasyRandomParameters()
        .overrideDefaultInitialization(true)
        .randomize(
            JsonNode.class,
            jsonNodeRandomizer()
        )
        .randomize(
            FieldPredicates.named("instances")
                .and(FieldPredicates.ofType(List.class))
                .and(FieldPredicates.inClass(OrderItem.class)),
            instancesRandomizer()
        )
        .randomize(
            FieldPredicates.named("id")
                .and(FieldPredicates.ofType(Long.class))
                .and(FieldPredicates.inClass(Order.class)),
            constantLongRandomizer(1L)
        )
        .randomize(
            FieldPredicates.named("id")
                .and(FieldPredicates.ofType(Long.class))
                .and(FieldPredicates.inClass(WaybillSegmentStatusHistory.class)),
            constantLongRandomizer(1L)
        )
        .randomize(
            CancellationOrderReason.class,
            new AbstractRandomizer<>() {
                @Override
                public CancellationOrderReason<MissingItemsCancellationOrderRequestReasonDetails> getRandomValue() {
                    return CancellationOrderReason.MISSING_ITEM;
                }
            }
        )
        .randomize(
            FieldPredicates.named("routeUuid")
                .and(FieldPredicates.ofType(UUID.class))
                .and(FieldPredicates.inClass(Order.class)),
            new UUIDRandomizer()
        )
        .excludeField(FieldPredicates.named("parent")
            .and(FieldPredicates.ofType(StorageUnit.class))
            .and(FieldPredicates.inClass(StorageUnit.class))
        )
        .excludeField(FieldPredicates.named("children")
            .and(FieldPredicates.ofType(Set.class))
            .and(FieldPredicates.inClass(StorageUnit.class))
        )
        .randomize(
            FieldPredicates.named("unprocessed")
                .and(FieldPredicates.ofType(Boolean.class))
                .and(FieldPredicates.inClass(WaybillSegmentStatusHistory.class)),
            constantBooleanRandomizer(false)
        )
        .collectionSizeRange(1, 3);

    private static final EasyRandom RANDOMIZER = new EasyRandom(RANDOM_PARAMETERS);
    private final HistoryOrderConverter converter = new HistoryOrderConverter(createProperties(true));
    private final JsonDiffComputer jsonDiffComputer = new JsonDiffComputer(objectMapper);

    @Test
    @DisplayName("Отсутствуют циклы при обходе заказа")
    void noLoopInOrderPaths() {
        Assertions.assertThatCode(() -> PathsExtractor.extractAllPathsExcept(Order.class, NOT_COMPARING))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("orderPathArguments")
    @DisplayName("Изменение всех полей, кроме перечисленных, сохраняется в историю")
    void checkAllRequiredFieldsAreConverted(String path) {
        Order order = RANDOMIZER.nextObject(Order.class);
        order.getWaybill().forEach(ws -> ws.setWaybillStatus(WaybillStatus.ACTIVE));
        HistoryOrder before = converter.fromEntity(order, false);
        HistoryOrder after = converter.fromEntity(changeFieldValue(order, path), false);
        JsonNode diff = jsonDiffComputer.computeDiff(before, after);
        Assertions.assertThat(diff.size()).as("Ожидаются изменения заказа, путь: " + path).isGreaterThan(0);
    }

    @Nonnull
    private static Stream<Arguments> orderPathArguments() {
        return PathsExtractor.extractAllPathsExcept(Order.class, NOT_COMPARING).stream().sorted().map(Arguments::of);
    }

    @Nonnull
    private Order changeFieldValue(Order order, String path) {
        String[] pathParts = path.split("\\.");
        String[] pathPartsWithoutLast = Arrays.copyOfRange(pathParts, 0, pathParts.length - 1);
        String lastPathPart = pathParts[pathParts.length - 1];

        Object currentObject = order;
        for (String pathPart : pathPartsWithoutLast) {
            currentObject = FieldHelper.getFieldValue(currentObject, pathPart);
        }
        FieldHelper.setFieldValue(currentObject, lastPathPart, null);
        return order;
    }

    @Nonnull
    public static AbstractRandomizer<List<Map<String, String>>> instancesRandomizer() {
        return new AbstractRandomizer<>() {
            @Override
            public List<Map<String, String>> getRandomValue() {
                return List.of(Map.of("key", "value"));
            }
        };
    }

    @Nonnull
    private static AbstractRandomizer<JsonNode> jsonNodeRandomizer() {
        return new AbstractRandomizer<>() {
            @Override
            public JsonNode getRandomValue() {
                return new POJONode(Map.of("key", "value"));
            }
        };
    }

    @Nonnull
    private static Randomizer<Long> constantLongRandomizer(Long constant) {
        return new AbstractRandomizer<>() {
            @Override
            public Long getRandomValue() {
                return constant;
            }
        };
    }

    @Nonnull
    private static Randomizer<Boolean> constantBooleanRandomizer(boolean constant) {
        return new AbstractRandomizer<>() {
            @Override
            public Boolean getRandomValue() {
                return constant;
            }
        };
    }

    private TrackerCheckpointProcessingProperties createProperties(boolean readFromTracker) {
        TrackerCheckpointProcessingProperties properties = new TrackerCheckpointProcessingProperties();
        properties.setReadFromTracker(readFromTracker);
        return properties;
    }
}
