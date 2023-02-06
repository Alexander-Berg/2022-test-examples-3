package ru.yandex.market.logistics.lom.jobs.producer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.entity.Location;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.OrderReturn;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.WaybillSegmentStatusHistory;
import ru.yandex.market.logistics.lom.entity.embedded.Recipient;
import ru.yandex.market.logistics.lom.entity.embedded.Sender;
import ru.yandex.market.logistics.lom.entity.enums.OrderReturnStatus;
import ru.yandex.market.logistics.lom.entity.enums.OrderStatus;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.entity.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;
import ru.yandex.market.logistics.lom.entity.items.OrderItem;
import ru.yandex.market.logistics.lom.entity.items.StorageUnit;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.repository.OrderRepository;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.sdk.userinfo.service.UidConstants;

@DisplayName("Отправка отмены заказа в LRM")
class SendCancellationToLrmProducerTest extends AbstractContextualTest {

    private static final long WAREHOUSE_ID = 456;
    private static final long SC_PARTNER_ID = 234;

    @Autowired
    private FeatureProperties featureProperties;

    @Autowired
    private SendCancellationToLrmProducer sendCancellationToLrmProducer;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-08-30T11:12:13.00Z"), clock.getZone());
        featureProperties.setUseNewFlowForExpressCancellation(true);
    }

    @AfterEach
    void close() {
        featureProperties.setCancellationWithLrmAllEnabled(false);
        featureProperties.setCancellationWithLrmFirstPartnerTypes(Set.of());
        featureProperties.setCancellationWithLrmLogisticPointIds(Set.of());
        featureProperties.setCancellationWithLrmFbsLogisticPointIds(Set.of());
        featureProperties.setCancellationWithLrmRecipientUids(Set.of());
        featureProperties.setCancellationWithLrmFaasEnabled(false);
        featureProperties.setCancellationMultiplaceEnabled(null);
        featureProperties.setCancellationWithLrmDbsEnabled(false);
    }

    @Test
    @DisplayName("Включено для всех")
    void successAll() {
        featureProperties.setCancellationWithLrmAllEnabled(true);
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder());
        checkTaskCreation(true);
    }

    @Test
    @DisplayName("Включено по пользователям")
    void successRecipientUid() {
        featureProperties.setCancellationWithLrmRecipientUids(Set.of(123L));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder());
        checkTaskCreation(true);
    }

    @Test
    @DisplayName("Включено по логистическим точкам fby. Fby заказ")
    void successFbyLogisticPointIdFbyOrder() {
        featureProperties.setCancellationWithLrmLogisticPointIds(Set.of(WAREHOUSE_ID));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder(PartnerType.FULFILLMENT));
        checkTaskCreation(true);
    }

    @Test
    @DisplayName("Включено по логистическим точкам fbs. Fbs заказ")
    void successFbsLogisticPointIdFbsOrder() {
        featureProperties.setCancellationWithLrmFbsLogisticPointIds(Set.of(WAREHOUSE_ID));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder(PartnerType.DROPSHIP));
        checkTaskCreation(true);
    }

    @Test
    @DisplayName("Включено по логистическим точкам fby. Fbs заказ")
    void fbyLogisticPointIdFbsOrder() {
        featureProperties.setCancellationWithLrmLogisticPointIds(Set.of(WAREHOUSE_ID));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder(PartnerType.DROPSHIP));
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("Включено по логистическим точкам fbs. Fby заказ")
    void fbsLogisticPointIdFbyOrder() {
        featureProperties.setCancellationWithLrmFbsLogisticPointIds(Set.of(WAREHOUSE_ID));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder(PartnerType.FULFILLMENT));
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("Включено по типу партнёра FBY. Список лог. точек не заполнен. FBY заказ")
    void successFbyFirstPartnerTypeEmptyLogisticPointsFbyOrder() {
        featureProperties.setCancellationWithLrmFirstPartnerTypes(Set.of(PartnerType.FULFILLMENT));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder(PartnerType.FULFILLMENT));
        checkTaskCreation(true);
    }

    @Test
    @DisplayName("Включено по типу партнёра FBS. Список лог. точек не заполнен. FBS заказ")
    void successFbsFirstPartnerTypeEmptyLogisticPointsFbsOrder() {
        featureProperties.setCancellationWithLrmFirstPartnerTypes(Set.of(PartnerType.DROPSHIP));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder(PartnerType.DROPSHIP));
        checkTaskCreation(true);
    }

    @Test
    @DisplayName("Включено по типу партнёра FBY. Список лог. точек не заполнен. FBS заказ")
    void fbyFirstPartnerTypeEmptyLogisticPointsFbyOrder() {
        featureProperties.setCancellationWithLrmFirstPartnerTypes(Set.of(PartnerType.FULFILLMENT));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder(PartnerType.DROPSHIP));
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("Включено по типу партнёра FBS. Список лог. точек не заполнен. FBY заказ")
    void fbsFirstPartnerTypeEmptyLogisticPointsFbyOrder() {
        featureProperties.setCancellationWithLrmFirstPartnerTypes(Set.of(PartnerType.DROPSHIP));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder(PartnerType.FULFILLMENT));
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("Выключено для всех")
    void disabled() {
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder());
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("Включено по типу партнёра FBY и по лог. точкам FBY. FBY заказ.")
    void successFbyFirstPartnerTypeFbyLogisticPointIdFbyOrder() {
        featureProperties.setCancellationWithLrmFirstPartnerTypes(Set.of(PartnerType.FULFILLMENT));
        featureProperties.setCancellationWithLrmLogisticPointIds(Set.of(WAREHOUSE_ID));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder());
        checkTaskCreation(true);
    }

    @Test
    @DisplayName("Включено по типу партнёра FBS и по лог. точкам FBS. FBS заказ.")
    void successFbsFirstPartnerTypeFbsLogisticPointIdFbsOrder() {
        featureProperties.setCancellationWithLrmFirstPartnerTypes(Set.of(PartnerType.DROPSHIP));
        featureProperties.setCancellationWithLrmFbsLogisticPointIds(Set.of(WAREHOUSE_ID));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder(PartnerType.DROPSHIP));
        checkTaskCreation(true);
    }

    @Test
    @DisplayName("Включено по типу партнёра FBY и по лог. точкам FBY. FBY заказ. Условие по лог. точке не проходит")
    void fbyFirstPartnerTypeFbyLogisticPointIdFbyOrder() {
        featureProperties.setCancellationWithLrmFirstPartnerTypes(Set.of(PartnerType.FULFILLMENT));
        featureProperties.setCancellationWithLrmLogisticPointIds(Set.of(4567L));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder());
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("Включено по типу партнёра FBS и по лог. точкам FBS. FBS заказ. Условие по лог. точке не проходит")
    void fbsFirstPartnerTypeFbsLogisticPointIdFbsOrder() {
        featureProperties.setCancellationWithLrmFirstPartnerTypes(Set.of(PartnerType.DROPSHIP));
        featureProperties.setCancellationWithLrmFbsLogisticPointIds(Set.of(4567L));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder(PartnerType.DROPSHIP));
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("Включено по типу партнёра FBY и по лог. точкам FBS. FBY заказ.")
    void successFbyFirstPartnerTypeFbsLogisticPointIdFbyOrder() {
        featureProperties.setCancellationWithLrmFirstPartnerTypes(Set.of(PartnerType.FULFILLMENT));
        featureProperties.setCancellationWithLrmFbsLogisticPointIds(Set.of(WAREHOUSE_ID));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder());
        checkTaskCreation(true);
    }

    @Test
    @DisplayName("Включено по типу партнёра FBS и по лог. точкам FBY. FBS заказ.")
    void successFbsFirstPartnerTypeFbyLogisticPointIdFbsOrder() {
        featureProperties.setCancellationWithLrmFirstPartnerTypes(Set.of(PartnerType.DROPSHIP));
        featureProperties.setCancellationWithLrmLogisticPointIds(Set.of(WAREHOUSE_ID));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder());
        checkTaskCreation(true);
    }

    @Test
    @DisplayName(
        "Включено по типу партнёра-поставщика и по лог. точкам. "
            + "В комбинации условия не проходят, не проходит условие на лог. точку"
    )
    void failLogisticPointIdFirstPartnerType() {
        featureProperties.setCancellationWithLrmLogisticPointIds(Set.of(4567L));
        featureProperties.setCancellationWithLrmFirstPartnerTypes(Set.of(PartnerType.FULFILLMENT));
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareOrder());
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("Многоместный заказ. Включено для FBS по партнёру СЦ")
    void multiplacesEnabledScPartnerId() {
        featureProperties.setCancellationWithLrmFirstPartnerTypes(Set.of(PartnerType.DROPSHIP));
        featureProperties.setCancellationMultiplaceEnabled(Map.of(PartnerType.DROPSHIP, Set.of(SC_PARTNER_ID)));
        sendCancellationToLrmProducer.produceTaskIfNeeded(multiplaceOrder(PartnerType.DROPSHIP));
        checkTaskCreation(true);
    }

    @Test
    @DisplayName("Многоместный заказ. Включено для FBY по типу партнёра")
    void multiplacesEnabledPartnerType() {
        featureProperties.setCancellationWithLrmFirstPartnerTypes(Set.of(PartnerType.FULFILLMENT));
        featureProperties.setCancellationMultiplaceEnabled(Map.of(PartnerType.FULFILLMENT, Set.of()));
        sendCancellationToLrmProducer.produceTaskIfNeeded(multiplaceOrder(PartnerType.FULFILLMENT));
        checkTaskCreation(true);
    }

    @Test
    @DisplayName("Неподходящая платформа")
    void notBeru() {
        featureProperties.setCancellationWithLrmAllEnabled(true);
        sendCancellationToLrmProducer.produceTaskIfNeeded(
            prepareOrder().setPlatformClient(PlatformClient.YANDEX_DELIVERY)
        );
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("Многоместный заказ")
    void multiplePlaces() {
        Order order = multiplaceOrder(PartnerType.FULFILLMENT);
        featureProperties.setCancellationWithLrmAllEnabled(true);
        sendCancellationToLrmProducer.produceTaskIfNeeded(order);
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("В заказе только рутовый юнит")
    void noPlaces() {
        Order order = prepareOrder();
        order.clearUnits();
        order.addStorageUnit(
            storageUnit(1L, StorageUnitType.ROOT)
        );
        featureProperties.setCancellationWithLrmAllEnabled(true);
        sendCancellationToLrmProducer.produceTaskIfNeeded(order);
        checkTaskCreation(true);
    }

    @Test
    @DisplayName("Нет СЦ")
    void withoutSc() {
        Order order = prepareOrder();
        order.getWaybill().get(1).setSegmentType(SegmentType.COURIER);
        featureProperties.setCancellationWithLrmAllEnabled(true);
        sendCancellationToLrmProducer.produceTaskIfNeeded(order);
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("Заказ не создан в FF")
    void notCreatedInFF() {
        Order order = prepareOrder();
        order.getWaybill().get(0).setExternalId(null);
        featureProperties.setCancellationWithLrmAllEnabled(true);
        sendCancellationToLrmProducer.produceTaskIfNeeded(order);
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("60й чп от курьерки был получен более часа назад")
    void courierCheckpoint60MoreThanHourAgo() {
        Order order = prepareOrder();
        order.getWaybill().get(2)
            .setSegmentType(SegmentType.COURIER)
            .addWaybillSegmentStatusHistory(
                new WaybillSegmentStatusHistory()
                    .setStatus(SegmentStatus.RETURN_PREPARING)
                    .setCreated(clock.instant().minus(2, ChronoUnit.HOURS)),
                SegmentStatus.RETURN_PREPARING
            );
        featureProperties.setCancellationWithLrmAllEnabled(true);
        sendCancellationToLrmProducer.produceTaskIfNeeded(order);
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("60й чп от курьерки был получен менее часа назад")
    void successCourierCheckpoint60() {
        Order order = prepareOrder();
        order.getWaybill().get(2)
            .setSegmentType(SegmentType.COURIER)
            .addWaybillSegmentStatusHistory(
                new WaybillSegmentStatusHistory()
                    .setStatus(SegmentStatus.RETURN_PREPARING)
                    .setCreated(clock.instant()),
                SegmentStatus.RETURN_PREPARING
            );
        featureProperties.setCancellationWithLrmAllEnabled(true);
        sendCancellationToLrmProducer.produceTaskIfNeeded(order);
        checkTaskCreation(true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Невыкуп едет через LRM для FaaS заказа")
    void successFaasOrder(boolean propertyEnabled) {
        Order order = prepareOrder();
        order.setPlatformClient(PlatformClient.FAAS);
        order.getWaybill().get(2)
            .setSegmentType(SegmentType.COURIER)
            .addWaybillSegmentStatusHistory(
                new WaybillSegmentStatusHistory()
                    .setStatus(SegmentStatus.RETURN_PREPARING)
                    .setCreated(clock.instant()),
                SegmentStatus.RETURN_PREPARING
            );
        featureProperties.setCancellationWithLrmAllEnabled(true);
        featureProperties.setCancellationWithLrmFaasEnabled(propertyEnabled);
        sendCancellationToLrmProducer.produceTaskIfNeeded(order);
        checkTaskCreation(propertyEnabled);
    }

    @Test
    @DisplayName("Стрельбовый заказ, не создаем возврат в LRM")
    void shootingOrder() {
        Order order = prepareOrder();
        order.getRecipient().setUid(UidConstants.NO_SIDE_EFFECT_UID);
        order.clearUnits();
        orderRepository.saveAndFlush(order);

        sendCancellationToLrmProducer.produceTaskIfNeeded(order);
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("Флаг выключен")
    void disabledExpress() {
        featureProperties.setUseNewFlowForExpressCancellation(false);
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareExpressOrder());
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("Нет склада для невыкупов")
    void withoutReturnScExpress() {
        sendCancellationToLrmProducer.produceTaskIfNeeded(
            prepareExpressOrder().setWaybill(List.of(
                new WaybillSegment()
                    .setPartnerType(PartnerType.DROPSHIP)
                    .setExternalId("fulfillment-external-id"),
                new WaybillSegment()
                    .setSegmentType(SegmentType.COURIER)
                    .setPartnerType(PartnerType.DELIVERY)
                    .addTag(WaybillSegmentTag.CALL_COURIER)
            ))
        );
        checkTaskCreation(false);
    }

    @ParameterizedTest
    @EnumSource(value = OrderReturnStatus.class, names = {"COMMITTED", "CREATED"})
    @DisplayName("Есть активные возвраты экспресс-заказа")
    void withActiveReturnsExpress(OrderReturnStatus status) {
        Order order = prepareExpressOrder();
        order.addReturn(new OrderReturn().setReturnStatus(status).setOrder(order));

        sendCancellationToLrmProducer.produceTaskIfNeeded(order);
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("Многоместный экспресс-заказ")
    void multiplacesExpress() {
        Order order = prepareExpressOrder()
            .addStorageUnit(place(1))
            .addStorageUnit(place(2));

        sendCancellationToLrmProducer.produceTaskIfNeeded(order);
        checkTaskCreation(false);
    }

    @Test
    @DisplayName("Успех экспресс")
    void successExpress() {
        sendCancellationToLrmProducer.produceTaskIfNeeded(prepareExpressOrder());
        checkTaskCreation(true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Невыкуп едет через LRM для DBS заказа")
    void successDbsOrder(boolean propertyEnabled) {
        Order order = prepareOrder();
        order.setPlatformClient(PlatformClient.DBS);

        featureProperties.setCancellationWithLrmDbsEnabled(propertyEnabled);
        sendCancellationToLrmProducer.produceTaskIfNeeded(order);
        checkTaskCreation(propertyEnabled);
    }

    @Test
    @DisplayName("Невыкуп не едет через LRM для DBS заказа, у которого есть позиция с незаполненным артикулом")
    void dbsOrderNoItemArticle() {
        featureProperties.setCancellationWithLrmDbsEnabled(true);
        Order order = prepareOrder();
        order.setPlatformClient(PlatformClient.DBS);
        order.setItems(List.of(
            new OrderItem().setArticle("1"),
            new OrderItem()
        ));

        sendCancellationToLrmProducer.produceTaskIfNeeded(order);
        checkTaskCreation(false);
    }

    private void checkTaskCreation(boolean created) {
        if (created) {
            queueTaskChecker.assertQueueTaskCreated(
                QueueType.SEND_CANCELLATION_TO_LRM,
                PayloadFactory.createOrderIdPayload(1, "1", 1)
            );
        } else {
            queueTaskChecker.assertQueueTaskNotCreated(QueueType.SEND_CANCELLATION_TO_LRM);
        }
    }

    @Nonnull
    private Order multiplaceOrder(PartnerType firstPartnerType) {
        return prepareOrder(firstPartnerType).addStorageUnit(place(2));
    }

    @Nonnull
    private Order prepareOrder() {
        return prepareOrder(PartnerType.FULFILLMENT);
    }

    @Nonnull
    private Order prepareOrder(PartnerType firstPartnerType) {
        return baseOrder()
            .setWaybill(List.of(
                new WaybillSegment()
                    .setPartnerType(firstPartnerType)
                    .setPartnerId(-1L)
                    .setExternalId("fulfillment-external-id")
                    .setSegmentType(SegmentType.NO_OPERATION)
                    .setWarehouseLocation(new Location().setWarehouseId(WAREHOUSE_ID)),
                new WaybillSegment()
                    .setSegmentType(SegmentType.SORTING_CENTER)
                    .setPartnerId(SC_PARTNER_ID),
                new WaybillSegment()
                    .setSegmentType(SegmentType.PICKUP)
                    .setPartnerId(-2L)
            ));
    }

    @Nonnull
    private Order prepareExpressOrder() {
        return baseOrder()
            .setWaybill(List.of(
                new WaybillSegment()
                    .setPartnerType(PartnerType.DROPSHIP)
                    .setPartnerId(-1L)
                    .setSegmentType(SegmentType.FULFILLMENT)
                    .setExternalId("fulfillment-external-id")
                    .setReturnWarehouseLocation(new Location().setWarehouseId(9000L)),
                new WaybillSegment()
                    .setSegmentType(SegmentType.COURIER)
                    .setPartnerId(-2L)
                    .setPartnerType(PartnerType.DELIVERY)
                    .addTag(WaybillSegmentTag.CALL_COURIER)
            ));
    }

    @Nonnull
    private Order baseOrder() {
        return new Order()
            .setId(1L)
            .setStatus(OrderStatus.PROCESSING, clock)
            .setSender(new Sender().setId(888L))
            .setBarcode("barcode")
            .setPlatformClient(PlatformClient.BERU)
            .setRecipient(new Recipient().setUid(123L))
            .addStorageUnit(place(1));
    }

    @Nonnull
    private StorageUnit place(long id) {
        return storageUnit(id, StorageUnitType.PLACE);
    }

    @Nonnull
    private StorageUnit storageUnit(long id, StorageUnitType type) {
        return new StorageUnit().setId(id).setUnitType(type);
    }
}
