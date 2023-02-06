package step;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import client.TMClient;
import dto.responses.tm.admin.movement.TmAdminMovementResponse;
import dto.responses.tm.admin.movement.Values;
import dto.responses.tm.admin.register_unit.RegisterUnitResponse;
import dto.responses.tm.admin.search.ItemsItem;
import dto.responses.tm.admin.search.TmAdminSearchResponse;
import dto.responses.tm.admin.status_history.TmAdminStatusHistoryResponse;
import dto.responses.tm.admin.task.Item;
import dto.responses.tm.admin.task.TmAdminTaskResponse;
import dto.responses.tm.admin.transportation.TmTransportationResponse;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.springframework.data.domain.Pageable;
import toolkit.Retrier;

import ru.yandex.market.delivery.transport_manager.model.dto.MovementDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterUnitDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationSearchDto;
import ru.yandex.market.delivery.transport_manager.model.dto.UnitCountDto;
import ru.yandex.market.delivery.transport_manager.model.dto.trip.TripShortcutDto;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.model.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.model.filter.TransportationSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;

@Slf4j
public class TMSteps {
    private static final String INBOUND_ID_PREFIX = "TMU";

    private static final TMClient TM = new TMClient();

    @Step("Ищем перемещение между двумя партнёрами на выбранный день")
    public Long getTransportationIdForDay(
        long outboundPartnerId,
        long inboundPartnerId,
        LocalDate planned,
        TransportationStatus status
    ) {
        log.debug("Get transportationId for the selected day");
        return Retrier.retry(() -> {
            TmAdminSearchResponse transportationForDay = TM.getTransportationForDay(
                outboundPartnerId,
                inboundPartnerId,
                planned,
                status
            );
            Assertions.assertNotNull(transportationForDay.getItems(), "Пустой список итемов перемещений");
            Assertions.assertFalse(transportationForDay.getItems().isEmpty(), "Пустой список итемов перемещений");
            ItemsItem itemsItem = transportationForDay.getItems().get(0);
            Assertions.assertNotNull(itemsItem, "Пустой объект перемещений");
            return itemsItem.getId();
        });
    }

    @Step("Получаем статус перемещения")
    public String getTransportationStatus(long transportationId) {
        log.debug("Get transportation status");
        return Retrier.retry(() -> {
            TmAdminSearchResponse transportationById = TM.getTransportationById(transportationId);
            Assertions.assertNotNull(transportationById.getItems(), "Пустые итемы у перемещения " + transportationId);
            Assertions.assertFalse(transportationById.getItems().isEmpty(), "Пустой список итемов перемещений");
            ItemsItem itemsItem = transportationById.getItems().get(0);
            return itemsItem.getValues().getAdminTransportationStatus();
        });
    }

    public boolean getTransportationIsActive(long transportationId) {
        TmAdminSearchResponse transportationById = TM.getTransportationById(transportationId);
        Assertions.assertNotNull(transportationById.getItems(), "Пустые итемы у перемещения " + transportationId);
        Assertions.assertFalse(transportationById.getItems().isEmpty(), "Пустой список итемов перемещений");
        ItemsItem itemsItem = transportationById.getItems().get(0);
        Assertions.assertNotNull(itemsItem, "Пустой объект перемещений");
        return itemsItem.getValues().getActive();
    }

    @Step("Проверяем активность перемещения")
    public void verifyTransportationIsActive(long transportationId, boolean isActive) {
        log.debug("Check if transportation is active");
        Retrier.retry(() -> Assertions.assertEquals(isActive, getTransportationIsActive(transportationId)));
    }

    @Step("Обновляем перемещения по конфигу в YT")
    public void refreshTransportation() {
        log.debug("Refreshing transportation from YT config");
        Retrier.retry(() -> TM.refreshTransportation(), 3, 5, TimeUnit.SECONDS);
    }

    /**
     * @param registerIndex 1 - inboundRegister, 0 - outboundRegister
     */
    @Step("Получаем id реестра перемещения")
    public long getTransportationRegister(long transportationId, int registerIndex) {
        log.debug("Get plan register for transportation");
        return Retrier.retry(() -> {
            TmAdminSearchResponse transportationRegister = TM.getTransportationRegister(transportationId);
            Assertions.assertNotNull(
                transportationRegister.getItems(),
                "Пустые итемы у перемещения " + transportationId
            );
            Assertions.assertFalse(
                transportationRegister.getItems().isEmpty(),
                "Пустой список итемов у перемещения " + transportationId
            );
            return transportationRegister.getItems().get(registerIndex).getId();
        });
    }

    @Step("Стартуем перемещение у партнёров")
    public void startTransportation(Long transportationId) {
        log.debug("Starting transportation");
        Retrier.retry(() -> TM.startTransportation(transportationId), 3, 5, TimeUnit.SECONDS);
    }

    @Step("Проверка времени изменения статуса")
    public void verifyInboundArrivalTime(
        String status,
        String entityType,
        long transportationId,
        Consumer<LocalDateTime> assertion
    ) {
        Retrier.retry(() -> {
            LocalDateTime dateTime = TM.getStatusHistory(entityType, transportationId).getItems()
                .stream()
                .map(dto.responses.tm.admin.status_history.ItemsItem::getValues)
                .filter(statusHistoryInfoDto -> statusHistoryInfoDto.getNewStatus().equals(status))
                .map(dto.responses.tm.admin.status_history.Values::getChangedAt)
                .filter(Objects::nonNull)
                .map(LocalDateTime::parse)
                .sorted()
                .findFirst()
                .orElse(null);

            assertion.accept(dateTime);

        }, 20, 5, TimeUnit.SECONDS);
    }

    @Step("Получаем внешний id приёмки перемещения")
    public String getInboundExternalId(long transportationId) {
        log.debug("Get inbound id for transportation");
        return Retrier.retry(() -> {
            TmAdminMovementResponse transportationInbound = TM.getTransportationInbound(transportationId);
            Assertions.assertNotNull(
                transportationInbound.getItems(),
                "Пустые итемы по перемещению " + transportationId
            );
            Assertions.assertFalse(
                transportationInbound.getItems().isEmpty(),
                "Пустой список итемов по перемещению " + transportationId
            );
            Values values = transportationInbound.getItems().get(0).getValues();
            Assertions.assertNotNull(values, "Пустые значения по перемещению " + transportationId);
            Assertions.assertNotNull(
                values.getExternalId(),
                "Пустой ExternalId приёмки по перемещению " + transportationId
            );
            return values.getExternalId().toString();
        }, 80);
    }

    @Step("Получаем внешний id отгрузки перемещения")
    public String getOutboundExternalId(long transportationId) {
        log.debug("Get outbound id for transportation");
        return Retrier.retry(() -> {
            TmAdminMovementResponse transportationOutbound = TM.getTransportationOutbound(transportationId);
            Assertions.assertNotNull(
                transportationOutbound.getItems(),
                "Пустые итемы по перемещению  " + transportationId
            );
            Assertions.assertFalse(
                transportationOutbound.getItems().isEmpty(),
                "Пустой список итемов по перемещению " + transportationId
            );
            Values values = transportationOutbound.getItems().get(0).getValues();
            Assertions.assertNotNull(values, "Пустые значения по перемещению " + transportationId);
            Assertions.assertNotNull(
                values.getExternalId(),
                "Пустой ExternalId отгрузки по перемещению " + transportationId
            );
            return values.getExternalId().toString();
        }, 80);
    }

    private TmAdminMovementResponse getMovementById(Long transportationId) {
        TmAdminMovementResponse transportationMovement = TM.getTransportationMovement(transportationId);
        Assertions.assertNotNull(
            transportationMovement.getItems(),
            "Пустые итемы у мувментов по перемещению  " + transportationId
        );
        Assertions.assertFalse(
            transportationMovement.getItems().isEmpty(),
            "Пустой список итемов у мувментов по перемещению " + transportationId
        );
        return transportationMovement;
    }

    @Step("Получаем внешний id movement'a перемещения")
    public String getMovementExternalId(long transportationId) {
        log.debug("Get movement id for transportation");
        return Retrier.retry(() -> {
            TmAdminMovementResponse transportationMovement = getMovementById(transportationId);
            return transportationMovement.getItems().get(0).getValues().getExternalId().toString();
        });
    }

    @Step("Получаем внутренний id movement'a перемещения")
    public String getMovementIdWithPrefix(long transportationId) {
        log.debug("Get movement id for transportation");
        return Retrier.retry(() -> {
            TmAdminMovementResponse transportationMovement = getMovementById(transportationId);
            return transportationMovement.getItems().get(0).getValues().getIdWithPrefix();
        });
    }

    @Step("Получаем внутренний id outbound'a перемещения")
    public String getOutboundIdWithPrefix(long transportationId) {
        log.debug("Get outbound id with prefix for transportation");
        return Retrier.retry(() -> {
            TmAdminMovementResponse transportationOutbound = TM.getTransportationOutbound(transportationId);
            Assertions.assertNotNull(
                transportationOutbound.getItems(),
                "Пустые итемы по перемещению  " + transportationId
            );
            Assertions.assertFalse(
                transportationOutbound.getItems().isEmpty(),
                "Пустой список итемов по перемещению " + transportationId
            );
            Assertions.assertNotNull(
                transportationOutbound.getItems().get(0).getValues().getIdWithPrefix(),
                "Пустой id with prefix отгрузки по перемещению " + transportationId
            );
            return transportationOutbound.getItems().get(0).getValues().getIdWithPrefix();
        });
    }

    @Step("Получаем id movement'a по id перемещения")
    public Long getMovementId(long transportationId) {
        log.debug("Get movement id for transportation");
        return Retrier.retry(() -> {
            TmAdminMovementResponse transportationMovement = getMovementById(transportationId);
            return transportationMovement.getItems().get(0).getId().longValue();
        });
    }

    @Step("Проверяем, что выбрался правильный транспорт для перемещения")
    public void verifyTransport(long transportationId, String expectedTransport) {
        log.debug("Expect transport with id {} for transportation {}", expectedTransport, transportationId);
        Retrier.retry(() -> Assertions.assertEquals(
            expectedTransport,
            getMovementById(transportationId).getItems().get(0).getValues().getTransport().getId(),
            "Был выбран неправильный транспорт для перемещения " + transportationId
        ));

    }

    @Step("Создаём задачу на перемещение")
    public long createTransportationTask(
        long logisticPointFrom,
        long logisticPointTo,
        String ssku,
        String supplierId,
        String realSupplierId,
        int count
    ) {
        log.debug("Creating transportation task");
        return Retrier.retry(() ->
            TM.createTransportationTask(
                logisticPointFrom,
                logisticPointTo,
                ssku,
                supplierId,
                realSupplierId,
                count
            ), 3, 5, TimeUnit.SECONDS
        );
    }

    @Step("Получаем первое перемещение для задачи на перемещение")
    public Long getTransportationForTask(long transportationTaskId) {
        log.debug("Get first transportation for transportation task");
        return Retrier.retry(() -> {
            TmAdminSearchResponse transportationForTask = TM.getTransportationForTask(transportationTaskId);
            Assertions.assertNotNull(
                transportationForTask.getItems(),
                "Не создалось перемещений для задачи на перемещение " + transportationTaskId
            );
            Assertions.assertFalse(
                transportationForTask.getItems().isEmpty(),
                "Не создалось перемещений для задачи на перемещение " + transportationTaskId
            );
            return transportationForTask.getItems().get(0).getId();
        });
    }

    @Step("Сравниваем количество перемещений для задачи на перемещение с ожидаемым")
    public void verifyQuantityOfTransportationForTask(long transportationTaskId, int expectedTransportations) {
        log.debug(
            "Expect {} transportations for transportation task {}",
            expectedTransportations,
            transportationTaskId
        );
        List<ItemsItem> items = TM.getTransportationForTask(transportationTaskId).getItems();
        long transportationsCount = items.size();
        Assertions.assertEquals(
            expectedTransportations,
            transportationsCount,
            "Количество перемещений не соответствует ожидаемому в задаче на перемещение " +
                transportationTaskId +
                " ожидалось " +
                expectedTransportations
        );
    }

    private TmAdminTaskResponse getTransportationTask(Long transportationTaskId) {
        TmAdminTaskResponse transportationTask = TM.getTransportationTask(transportationTaskId);
        Assertions.assertNotNull(
            transportationTask.getItem(),
            "Пустой итем у задачи на перемещение " + transportationTaskId
        );
        Item item = transportationTask.getItem();
        Assertions.assertNotNull(item.getValues(), "Пустые значения у задачи на перемещение " + transportationTaskId);
        return transportationTask;
    }

    private TmTransportationResponse getTransportation(Long transportationId) {
        TmTransportationResponse transportation = TM.getTransportation(transportationId);
        Assertions.assertNotNull(transportation.getItem(), "Пустой итем у перемещения " + transportationId);
        dto.responses.tm.admin.transportation.Item item = transportation.getItem();
        Assertions.assertNotNull(item.getValues(), "Пустые значения у перемещения " + transportationId);
        return transportation;
    }

    @Step("Получаем id реквеста в ffwf для отгрузки")
    public Long getShopRequestIdForOutbound(Long transportationId) {
        log.debug("Getting shop_request id for outbound of transportationId = {}");
        Retrier.retry(() -> Assertions.assertNotNull(
            TM.getTransportationOutbound(transportationId).getItems().get(0),
            "Нет отгрузки у перемещения  " + transportationId
        ));
        Assertions.assertFalse(
            TM.getTransportationOutbound(transportationId).getItems().isEmpty(),
            "Пустой список отгрузок по перемещению " + transportationId
        );
        long outboundId = TM.getTransportationOutbound(transportationId).getItems().get(0).getId().longValue();
        Retrier.retry(() -> Assertions.assertNotNull(
            TM.getTransportationUnit(outboundId).getRequestId(),
            "Нет shop_request'a у отгрузки " + outboundId
        ));
        Assertions.assertFalse(
            TM.getTransportationUnit(outboundId).getRequestId().equals(""),
            "Пустое значение shop_request'a у отгрузки " + outboundId
        );
        return TM.getTransportationUnit(outboundId).getRequestId();
    }

    @Step("Получаем id реквеста в ffwf для приёмки")
    public Long getShopRequestIdForInbound(Long transportationId) {
        log.debug("Getting shop_request id for inbound of transportationId = {}");
        Retrier.retry(() -> Assertions.assertNotNull(
            TM.getTransportationInbound(transportationId).getItems().get(0),
            "Нет приёмки у перемещения  " + transportationId
        ));
        Assertions.assertFalse(
            TM.getTransportationInbound(transportationId).getItems().isEmpty(),
            "Пустой список приёмок по перемещению " + transportationId
        );
        long inboundId = TM.getTransportationInbound(transportationId).getItems().get(0).getId().longValue();
        Retrier.retry(() -> Assertions.assertNotNull(
            TM.getTransportationUnit(inboundId).getRequestId(),
            "Нет shop_request'a у приёмки " + inboundId
        ));
        Assertions.assertFalse(
            TM.getTransportationUnit(inboundId).getRequestId().equals(""),
            "Пустое значение shop_request'a у приёмки " + inboundId
        );
        return TM.getTransportationUnit(inboundId).getRequestId();
    }

    @Step("Получаем id приёмки по перемещению")
    public String getInboundIdWithPrefix(long transportationId) {
        log.debug("Getting inbound id for transportationId = {}", transportationId);
        return INBOUND_ID_PREFIX + Retrier.retry(
            () -> TM.getTransportationInbound(transportationId).getItems().get(0).getId().longValue()
        );
    }

    @Step("Ожидаем корректный статус перемещения")
    public void verifyTransportationStatus(Long transportationId, TransportationStatus status) {
        log.debug("Verifying transportation status");
        Retrier.retry(() -> Assertions.assertEquals(
            status,
            getTransportation(transportationId).getItem().getValues().getAdminTransportationStatus(),
            "Некорректный статус у перемещения " + transportationId
        ));
    }

    @Step("Ожидаем корректный статус задачи на перемещение")
    public void verifyTransportationTaskStatus(Long transportationTaskId, String status) {
        log.debug("Get transportation status");
        Retrier.retry(() -> Assertions.assertEquals(
            status,
            getTransportationTask(transportationTaskId).getItem().getValues().getStatus(),
            "Некорректный статус у задачи на перемещение " + transportationTaskId
        ));
    }

    @Step("Ожидаем корректный статус мувмента")
    public void verifyMovementStatus(Long transportationId, String status) {
        log.debug("Verifying movement status");
        Retrier.retry(() -> Assertions.assertEquals(
            status,
            getMovementById(transportationId).getItems().get(0).getValues().getStatus(),
            "Некорректный статус мувмента у перемещения " + transportationId
        ));
    }

    @Step("Ожидаем корректный статус отгрузки")
    public void verifyOutboundStatus(Long transportationId, String status) {
        log.debug("Verifying outbound status");
        Retrier.retry(() -> TM.getTransportationOutbound(transportationId), 3, 5, TimeUnit.SECONDS);
        Assertions.assertNotNull(
            TM.getTransportationOutbound(transportationId).getItems().get(0),
            "Нет отгрузки у перемещения  " + transportationId
        );
        Assertions.assertFalse(
            TM.getTransportationOutbound(transportationId).getItems().isEmpty(),
            "Пустой список отгрузок по перемещению " + transportationId
        );
        // Если проверяем, что таска упала в ошибку, делаем длинный кастомный ретрай
        int retries = status.equals("ERROR") ? 6 : 60;
        int timeout = status.equals("ERROR") ? 263 : 15;
        Retrier.retry(() -> Assertions.assertEquals(
            status,
            TM.getTransportationOutbound(transportationId).getItems().get(0).getValues().getStatus(),
            "Некорректный статус отгрузки у перемещения " + transportationId
        ), retries, timeout, TimeUnit.SECONDS);
    }

    @Step("Ожидаем корректный статус приёмки")
    public void verifyInboundStatus(Long transportationId, String status) {
        log.debug("Verifying inbound status");
        Retrier.retry(() -> TM.getTransportationInbound(transportationId), 3, 5, TimeUnit.SECONDS);
        Assertions.assertNotNull(
            TM.getTransportationInbound(transportationId).getItems().get(0),
            "Нет приёмки у перемещения  " + transportationId
        );
        Assertions.assertFalse(
            TM.getTransportationInbound(transportationId).getItems().isEmpty(),
            "Пустой список приёмок по перемещению " + transportationId
        );
        // Если проверяем, что таска упала в ошибку, делаем длинный кастомный ретрай
        int retries = status.equals("ERROR") ? 6 : 60;
        int timeout = status.equals("ERROR") ? 263 : 15;
        Retrier.retry(() -> Assertions.assertEquals(
            status,
            TM.getTransportationInbound(transportationId).getItems().get(0).getValues().getStatus(),
            "Некорректный статус приёмки у перемещения " + transportationId
        ), retries, timeout, TimeUnit.SECONDS);
    }

    @Step("Ожидаем корректный статус задачи на перемещение")
    public void verifyTransportationTaskValidationError(Long transportationTaskId, String validationError) {
        log.debug("Get transportation status");
        Retrier.retry(() -> Assertions.assertTrue(
            StringUtils.contains(
                getTransportationTask(transportationTaskId).getItem().getValues().getValidationErrors(),
                validationError
            ),
            "Некорректный validationError у задачи на перемещение " + transportationTaskId
        ));
    }

    @Step("Получаем id планового реестра задачи на перемещение")
    public long getPlanRegisterIdForTask(long transportationTaskId) {
        log.debug("Get plan register for transportation task");
        return getTransportationTask(transportationTaskId).getItem().getValues().getRegisterId().getId();
    }

    @Step("Получаем id реестра отказа для задачи на перемещение")
    public long getDeniedRegisterIdForTask(long transportationTaskId) {
        log.debug("Get denied register for transportation task");
        return getTransportationTask(transportationTaskId).getItem().getValues().getDeniedRegisterId().getId();
    }

    @Step("Проверяем наличие статуса в истории")
    public void verifyStatusInHistory(String entityType, long id, String expectedStatus) {
        log.debug("Verifying status for {} with id = {} equals {}", entityType, id, expectedStatus);
        Retrier.retry(() -> {
            TmAdminStatusHistoryResponse statusHistory = TM.getStatusHistory(entityType, id);
            Assertions.assertNotNull(statusHistory.getItems(), "Пустые итемы у истории статусов" + id);
            Assertions.assertTrue(
                statusHistory.getItems()
                    .stream()
                    .anyMatch(item -> item.getValues().getNewStatus().equals(expectedStatus)),
                "Не появился статус " + expectedStatus + " у " + entityType + " с id или id перемещения = " + id
            );
        });
    }

    /**
     * expectedCount - сколько ожидаем товаров в реестре. expectedPalletes - кол-во паллет, для красного реестра 0
     **/
    @Step("Проверяем соответствие количества товаров в реестре ожидаемому")
    public void verifyCountFromRegister(long registerId, int expectedCount, int expectedPalletes) {
        log.debug("Expect {} items and {} palletes from register{}", expectedCount, expectedPalletes, registerId);
        Retrier.retry(() -> {
        List<RegisterUnitDto> data = TM.getRegister(registerId).getData();
        long itemsCount = data.stream()
            .filter(unit -> unit.getType().equals(UnitType.ITEM))
            .flatMap(dat -> dat.getCounts().stream())
            .mapToInt(UnitCountDto::getQuantity)
            .sum();
        long palletesCount = data.stream()
            .filter(unit -> unit.getType().equals(UnitType.PALLET))
            .flatMap(dat -> dat.getCounts().stream())
            .mapToInt(UnitCountDto::getQuantity)
            .sum();
        Assertions.assertEquals(
            expectedPalletes,
            palletesCount,
            "Количество паллет не соответствует ожидаемому в реестре " +
                registerId +
                " ожидалось паллет " +
                expectedPalletes
        );
        Assertions.assertEquals(
            expectedCount,
            itemsCount,
            "Фактическое число товаров в реестре не соответствует ожидаемому реестр id = " + registerId
        );
        });
    }

    @Step("Проверяем наличие заказа в реестре")
    public void checkOrderInRegister(long registerId, Long orderId) {
        log.debug("Try to find order in plan register");
        Retrier.retry(() -> {
            RegisterUnitResponse registerUnits = TM.getRegisterUnits(registerId);
            Assertions.assertNotNull(registerUnits.getItems(), "Пустые итемы у заказа в реестре " + registerId);
            Assertions.assertFalse(registerUnits.getItems().isEmpty(), "Пустые итемы у заказа в реестре" + registerId);
            Assertions.assertTrue(
                registerUnits
                    .getItems()
                    .stream()
                    .anyMatch(regUnit -> regUnit.getValues().getBarcode().contains(orderId.toString())),
                "Заказ " + orderId + " не присутствует в реестре " + registerId
            );
        });
    }

    @Step("Получаем  идентификаторы заказов в реестре")
    public List<String> getOrderIdsInRegister(long registerId) {
        log.debug("Try to get orders in register");
        return Retrier.retry(() -> {
            RegisterUnitResponse registerUnits = TM.getRegisterUnits(registerId);
            Assertions.assertNotNull(registerUnits.getItems(), "Пустые итемы у заказа в реестре " + registerId);
            Assertions.assertFalse(registerUnits.getItems().isEmpty(), "Пустые итемы у заказа в реестре" + registerId);
            return registerUnits.getItems()
                .stream()
                .map(dto.responses.tm.admin.register_unit.ItemsItem::getValues)
                .map(dto.responses.tm.admin.register_unit.Values::getBarcode)
                .collect(Collectors.toList());
        });
    }

    @Step("Проверяем наличие принятого фактического реестра отгрузки")
    public void verifyOutboundFactRegister(Long transportationId, int expectedCount, int expectedPalletes) {
        log.debug("Expect ACCEPTED fact outbound register for transportation {}");
        Retrier.retry(() -> {
            TmAdminSearchResponse transportationRegister = TM.getTransportationRegister(transportationId);
            Assertions.assertEquals(
                3,
                transportationRegister.getItems().size(),
                "Не пришёл фактический реестр"
            );
            ItemsItem factRegister = transportationRegister.getItems().get(2);
            Assertions.assertNotNull(factRegister, "Отсутствует фактический реестр");
            Assertions.assertEquals("FACT", factRegister.getValues().getType(), "Третий реестр имеет тип не FACT");
            Assertions.assertEquals(
                "ACCEPTED",
                factRegister.getValues().getStatus(),
                "Cтатус реестра не соответствует ожидаемому ACCEPTED"
            );
            verifyCountFromRegister(factRegister.getId().longValue(), expectedCount, expectedPalletes);
        });
    }

    @Step("Получаем перемещения по shop_request'y")
    public Long getTransportationsByTag(int num, String tagValue, String tagCode) {
        log.debug("Get transportation {} for tag {}", num, tagValue);
        return Retrier.retry(() -> {
            List<ItemsItem> response = TM.getTransportationsByTag(tagCode, tagValue);
            Assertions.assertNotNull(response, "Пустой список итемов перемещений");
            Assertions.assertFalse(response.isEmpty(), "Пустой список итемов перемещений");
            Assertions.assertNotNull(response.get(num), "Не создалось перемещение №" + num);
            Assertions.assertNotNull(response.get(num).getId(), "Не создалось перемещение №" + num);
            ItemsItem itemsItem = response.get(num);
            return itemsItem.getId();
        });
    }

    @Step("Проверяем данные курьера в мувменте")
    public void verifyCourierInMovement(
        Long movementId,
        String expectedCourierName,
        String expectedCourierSurname,
        String expectedCarNumber,
        String expectedPhone
    ) {
        log.debug("Expecting correct courier data in movement");
        Retrier.retry(() -> {
            MovementDto movement = TM.getMovement(movementId);
            Assertions.assertNotNull(movement.getId(), "Пустой id у movemet'a " + movementId);
            Assertions.assertEquals(
                expectedCourierName,
                movement.getCourier().getName(),
                "Имя курьера не совпадает с ожидаемым" + expectedCourierName
            );
            Assertions.assertEquals(
                expectedCourierSurname,
                movement.getCourier().getSurname(),
                "Фамилия курьера не совпадает с ожидаемой" + expectedCourierSurname
            );
            Assertions.assertEquals(
                expectedCarNumber,
                movement.getCourier().getCarNumber(),
                "Номер автомобиля не совпадает с ожидаемым" + expectedCarNumber
            );
            Assertions.assertEquals(
                expectedPhone,
                movement.getCourier().getPhone(),
                "Телефон курьера не совпадает с ожидаемым" + expectedCourierSurname
            );
        });
    }

    @Step("Получаем рейс по id перемещения")
    public Long getTripByTransportationId(Long transportationId) {
        log.debug("Get trip for transportation {}", transportationId);
        return Retrier.retry(() -> {
            List<TripShortcutDto> response = TM.getTripSearch(transportationId, null);
            Assertions.assertNotNull(response.get(0).getTripId(), "Пустой id рейса");
            return response.get(0).getTripId();
        });
    }

    @Step("Получаем перемещение по id заказа")
    public TransportationSearchDto searchTransportation(Long orderId) {
        return Retrier.retry(() -> searchTransportationByOrderId(orderId));
    }

    @Step("Проверка, что заказ перепривязался к другой отгрузке")
    public void verifyTransportationChanged(Long orderId, Long oldTransportationId) {
        Retrier.retry(() -> Assertions.assertNotEquals(
            oldTransportationId,
            searchTransportationByOrderId(orderId).getId(),
            "Перемещение не изменилось"
        ));
    }

    @Nonnull
    private TransportationSearchDto searchTransportationByOrderId(Long orderId) {
        PageResult<TransportationSearchDto> result = TM.searchTransportations(
            TransportationSearchFilter.builder()
                .outboundOrderIds(Set.of(orderId))
                .build(),
            Pageable.unpaged()
        );
        List<TransportationSearchDto> data = result.getData();
        Assertions.assertEquals(
            1,
            data.size(),
            String.format("Более одного перемещения соответствует заказу %d", orderId)
        );
        return data.get(0);
    }
}
