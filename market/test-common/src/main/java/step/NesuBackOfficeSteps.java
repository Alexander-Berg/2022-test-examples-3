package step;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import client.NesuBackOfficeClient;
import dto.responses.nesu.DeliveryOptionsItem;
import dto.responses.nesu.ShipmentLogisticPoint;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import toolkit.Retrier;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@Slf4j
public class NesuBackOfficeSteps {

    private static final NesuBackOfficeClient NESU = new NesuBackOfficeClient();

    @Step("Создание заказа на забор")
    public long createNewWithdrawOrder(
        long senderId,
        long shopId,
        long userId,
        LocalDate todayDay,
        LocalDate deliveryDate
    ) {
        log.info("Creating new order");
        long orderId = NESU.createWithdrawOrders(senderId, shopId, userId, todayDay, deliveryDate)
            .extract()
            .body()
            .as(Long.TYPE);

        log.info("Committing order creation");
        NESU.commitOrder(shopId, userId, orderId);

        return orderId;
    }

    @Step("Создание заказа на самопривоз")
    public long createSelfExportNewOrder(
        long senderId,
        long shopId,
        long userId,
        LocalDate todayDay,
        LocalDate deliveryDate,
        long warehouseId
    ) {
        log.info("Creating new order");
        long orderId = NESU.createSelfExportOrders(senderId, shopId, userId, todayDay, deliveryDate, warehouseId)
            .extract()
            .body()
            .as(Long.TYPE);

        log.info("Committing order creation");
        NESU.commitOrder(shopId, userId, orderId);

        return orderId;
    }

    @Step("Ждем, что заказ получит номер заказа от службы")
    public void waitForExternalOrderlIdIsPresent(long shopId, long userId, long orderId) {
        log.info("Wait for Order externalId");
        Retrier.retry(
            () -> NESU.getOrder(shopId, userId, orderId)
                .assertThat()
                .body("deliveryServiceExternalId", not(empty())),
            5,
            10,
            TimeUnit.SECONDS
        );
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Step("Получение вариантов доставки")
    public List<DeliveryOptionsItem> getDeliveryOptions(
        long senderId,
        long shopId,
        long userId,
        String locationFrom,
        String locationTo,
        long length,
        long height,
        long width,
        long weight
    ) {
        return NESU.getOptions(senderId, shopId, userId, locationFrom, locationTo, length, height, width, weight);
    }

    @Step("Создание склада")
    public long createNewWarehouse(long shopId, long userId) {
        log.info("Creating new werehouse");

        return NESU.createWarehouse(shopId, userId)
            .extract()
            .path("id");
    }

    @Step("Создание заявки на забор")
    public void createWithDrawShipment(long shopId, long userId, LocalDate date, long warehouseId) {
        log.info("Creating new withdraw shipment");
        NESU.createNewWithdrawShipment(shopId, userId, date, warehouseId);
    }

    @Step("Получение номера заявки на забор")
    public long returnWithdrawShipmentId(long shopId, long userId, LocalDate date, long warehouseId) {
        log.info("Take shipment id");
        return NESU.createNewWithdrawShipment(shopId, userId, date, warehouseId)
            .extract()
            .jsonPath()
            .getLong("application.id");
    }

    @Step("Получение номера заявки на самопривоз")
    public long returnSelfExportShipmentId(long shopId, long userId, LocalDate date, long warehouseId) {
        log.info("Take import shipment id");
        return NESU.createNewImportCourierShipment(shopId, userId, date, warehouseId)
            .extract()
            .jsonPath()
            .getLong("application.id");
    }

    @Step("Создание заявки на самопривоз пешим курьером")
    public void createImportCourierShipment(long shopId, long userId, LocalDate date, long warehouseId) {
        log.info("Creating new courier withdraw shipment");
        NESU.createNewImportCourierShipment(shopId, userId, date, warehouseId);
    }

    @Step("Создание заявки на самопривоз курьером  на машине")
    public void createImportCourierWithCarShipment(long shopId, long userId, LocalDate date, long warehouseId) {
        log.info("Creating new courier by car withdraw shipment");
        NESU.createNewImportCourierWithCarShipment(shopId, userId, date, warehouseId);
    }

    @Step("Создание магазина")
    public void createNewShop(long shopId) {
        log.info("Creating new shop");
        NESU.createShop(shopId);
    }

    @Step("Деактивирование склада")
    public void deleteWarehouse(long shopId, long userId, long warehouseId) {
        log.info("Cancelled warehouse");
        NESU.deleteWarehouseById(shopId, userId, warehouseId);
    }

    @Step("Отмена заявки")
    public void cancelShipment(long shopId, long userId, long shipmentId) {
        log.info("Cancelled shipment");
        NESU.cancelShipmentById(shopId, userId, shipmentId);
    }

    @Step("Проверяем что заявка на забор создаеться")
    public void waitForCreateNewWithdrawShipment(long shopId, long userId, LocalDate futureDay, long warehouseId) {
        log.info("Wait for Shipment externalId");
        Retrier.retry(
            () -> NESU.findWithdrawShipment(shopId, userId, futureDay, warehouseId)
                .assertThat()
                .body("data[0].application.status", equalTo("NEW")),
            5,
            10,
            TimeUnit.SECONDS
        );
    }

    @Step("Проверяем что отгрузка подтверждена")
    public void shipmentApplicationConfirm(long createdSelfExportShipmentId, long shopId, long userId) {
        log.info("Confirm shipment");
        NESU.confirmShipmentById(createdSelfExportShipmentId, shopId, userId);
    }

    @Step("Проверяем что заявка отменена")
    public void waitForCancelledWithdrawShipment(long shopId, long userId, LocalDate futureDay, long warehouseId) {
        log.info("Wait for Shipment is cancelled");
        Retrier.retry(
            () -> NESU.findWithdrawShipment(shopId, userId, futureDay, warehouseId)
                .assertThat()
                .body("data[0].application.status", empty()),
            5,
            10,
            TimeUnit.SECONDS
        );
    }

    @Step("Проверяем что отгрузка подтверждена")
    public void waitForConfirImportShipment(long shopId, long userId, LocalDate todayDay, long warehouseId) {
        log.info("Wait for Confirm Shipment");
        Retrier.retry(
            () -> NESU.findWithdrawShipment(shopId, userId, todayDay, warehouseId)
                .assertThat()
                .body("data[0].application.status", equalTo("REGISTRY_SENT")),
            5,
            10,
            TimeUnit.SECONDS
        );
    }

    @Step("Ждем, что заявка получит номер в службе")
    public void waitForCreateNewImportShipment(long shopId, long userId, LocalDate futureDay, long warehouseId) {
        log.info("Wait for Shipment externalId");
        Retrier.retry(
            () -> NESU.findCourierShipment(shopId, userId, futureDay, warehouseId)
                .assertThat()
                .body("data[0].application.status", equalTo("CREATED"))
                .body("data[0].application.externalId", not(empty())),
            5,
            10,
            TimeUnit.SECONDS
        );
    }

    @Step("Запускаем джобу обновления дейоффов из YT")
    public void runYtUpdateDropoffDayoffJob() {
        log.info("Run YT update dropoff dayoff job");
        NESU.runYtUpdateDropoffDayoffJob();
    }

    @Step("Запускаем джобу обновления информации о дропоффе из YT")
    public void runYtUpdateDropoffSegmentsInfoJob() {
        log.info("Run YT update dropoff segment info job");
        NESU.runYtUpdateDropoffSegmentsInfoJob();
    }

    @Step("Ожидаем вариант подключения для магазина")
    public ShipmentLogisticPoint waitAvailableShipmentOption(
        long partnerId,
        long shopId,
        long userId,
        long expectedDropoffId,
        Consumer<ShipmentLogisticPoint> assertion
    ) {
        return Retrier.retry(
            () -> {
                List<ShipmentLogisticPoint> availablePoints = NESU.getAvailableShipmentOptions(
                    partnerId,
                    shopId,
                    userId,
                    null
                );

                ShipmentLogisticPoint dropoff = availablePoints.stream()
                    .filter(point -> point.getId() == expectedDropoffId)
                    .findAny()
                    .orElseThrow(() -> new AssertionError("Не найдена точка " + expectedDropoffId));

                assertion.accept(dropoff);

                return dropoff;
            },
            20,
            30,
            TimeUnit.SECONDS
        );
    }

    @Step("Поиск доступных опций доставки")
    public List<ShipmentLogisticPoint> searchAvailableShipmentOptions(
        long partnerId,
        long shopId,
        long userId,
        String pointType,
        @Nullable Boolean showReturnDisabled
    ) {
        log.info("Start search available shipment options");
        return NESU.getAvailableShipmentOptions(partnerId, shopId, userId, showReturnDisabled)
            .stream()
            .filter(
                shipmentLogisticPoint -> shipmentLogisticPoint.getPointType() != null &&
                    Objects.equals(shipmentLogisticPoint.getPointType(), pointType)
            )
            .toList();
    }
}
