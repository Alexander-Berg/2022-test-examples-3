package ru.yandex.market.deliveryintegrationtests.delivery.tests.transportation;

import java.util.concurrent.TimeUnit;

import dto.responses.lgw.LgwTaskFlow;
import dto.responses.tm.TmCheckpointStatus;
import io.qameta.allure.Epic;
import io.qameta.allure.TmsLink;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import toolkit.Delayer;

import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationStatus;

@Slf4j
@DisplayName("TM Test")
@Epic("TM")
public class TransportationTaskTest extends AbstractTransportationTest {
    /**
     * Константы с набором данных ниже нужны только в этом тесте, уносить их наружу нет смысла
     **/
    private static final long TOMILINO_LOGISTIC_POINT = 10000004401L;
    private static final long SOFIINO_LOGISTIC_POINT = 10000004403L;
    private static final long MARSCHROUTE_LOGISTIC_POINT = 10000927726L;
    private static final long CANCELLATION_TEST_LOGISTIC_POINT = 10000994208L;
    private static final long SC_SDT_LOGISTIC_POINT = 10000917466L;
    private static final long NO_TRANSPORT_LOGISTIC_POINT = 10000896302L;
    private static final long NO_METHODS_LOGISTIC_POINT_1 = 10000994180L;
    private static final long NO_METHODS_LOGISTIC_POINT_2 = 10000994207L;
    private static final String SSKU_BIG = "hid.100439119696";
    private static final String SSKU_DROPSHIP = "100126190439";
    private static final String SSKU_NOT_ON_STOCK = "100126190438";
    private static final String SUPPLIER_ID_DROPSHIP = "10358489";
    private static final String SUPPLIER_ID_FOR_BIG_SKU = "10671634";
    private static final String REAL_SUPPLIER_ID_AXA = "000719";
    private static final String invalidStatus = "INVALID";
    private final int COUNT = 1;
    private static final int COUNT_AXA = 4;
    private static final int COUNT_FOR_3_PALLETES = 2980;
    private static final int COUNT_FOR_3_TRANSPORTATIONS = 2980;
    private static final int COUNT_NOT_AVAILABLE = 5;
    long transportationTaskId;
    long movementTrackerId;
    Long transportationId;

    @Test
    @TmsLink("logistic-82")
    @DisplayName("ТМ: Создание межскладского перемещения из задачи на перемещение")
    void transportationTaskTest() {
        log.info("Starting Transportation Task test...");
        transportationTaskId = TM_STEPS.createTransportationTask(
            MARSCHROUTE_LOGISTIC_POINT,
            TOMILINO_LOGISTIC_POINT,
            SSKU_DROPSHIP,
            SUPPLIER_ID_DROPSHIP,
            "",
            COUNT
        );
        transportationId = TM_STEPS.getTransportationForTask(transportationTaskId);
        TM_STEPS.verifyTransportationStatus(transportationId, TransportationStatus.WAITING_DEPARTURE);
        TM_STEPS.verifyMovementStatus(transportationId, "NEVER_SEND");
        TM_STEPS.getOutboundExternalId(transportationId);
        TM_STEPS.getInboundExternalId(transportationId);
    }

    /**
     * Валидация выключена, тест disabled
     **/
    @Test
    @TmsLink("logistic-95")
    @Disabled
    @DisplayName("TM: Валидация задачи на перемещение без транспорта между логточками")
    void transportValidationTransportationTaskTest() {
        log.info("Starting Transport Validation Transportation Task test...");
        transportationTaskId = TM_STEPS.createTransportationTask(
            NO_TRANSPORT_LOGISTIC_POINT,
            TOMILINO_LOGISTIC_POINT,
            SSKU_DROPSHIP,
            SUPPLIER_ID_DROPSHIP,
            "",
            COUNT
        );
        TM_STEPS.verifyTransportationTaskStatus(transportationTaskId, invalidStatus);
        TM_STEPS.verifyTransportationTaskValidationError(
            transportationTaskId,
            "Отсутствует подходящий транспорт в LMS"
        );
    }

    @Test
    @DisplayName("ТМ: Проверка наличия товара в axapta")
    void axaptaValidationTest() {
        log.info("Starting Axapta Validation happy path test...");
        transportationTaskId = TM_STEPS.createTransportationTask(
            SOFIINO_LOGISTIC_POINT,
            TOMILINO_LOGISTIC_POINT,
            SSKU_DROPSHIP,
            SUPPLIER_ID_DROPSHIP,
            REAL_SUPPLIER_ID_AXA,
            COUNT_AXA
        );
        TM_STEPS.verifyStatusInHistory("transportation-task", transportationTaskId, "STOCK_AVAILABILITY_CHECKING");
        TM_STEPS.verifyStatusInHistory("transportation-task", transportationTaskId, "STOCK_AVAILABILITY_CHECKED");
        long registerId = TM_STEPS.getPlanRegisterIdForTask(transportationTaskId);
        TM_STEPS.verifyStatusInHistory("transportation-task", transportationTaskId, "PALLETS_CREATED");
        TM_STEPS.verifyCountFromRegister(registerId, COUNT_AXA, 1);
    }

    @Test
    @DisplayName("ТМ: Фейл проверки при отсутствии 100% товаров")
    void stockCheckFailedTest() {
        log.info("Starting stock avaliability check failed test...");
        transportationTaskId = TM_STEPS.createTransportationTask(
            SOFIINO_LOGISTIC_POINT,
            TOMILINO_LOGISTIC_POINT,
            SSKU_NOT_ON_STOCK,
            SUPPLIER_ID_DROPSHIP,
            "",
            COUNT_NOT_AVAILABLE
        );
        TM_STEPS.verifyStatusInHistory("transportation-task", transportationTaskId, "STOCK_AVAILABILITY_CHECKING");
        TM_STEPS.verifyStatusInHistory("transportation-task", transportationTaskId, "STOCK_AVAILABILITY_CHECK_FAILED");
        long deniedRegisterId = TM_STEPS.getDeniedRegisterIdForTask(transportationTaskId);
        TM_STEPS.verifyCountFromRegister(deniedRegisterId, COUNT_NOT_AVAILABLE, 0);
    }

    @Test
    @DisplayName("ТМ: Разбивка задачи на перемещение на паллеты")
    void palletesTest() {
        log.info("Starting palletes test...");
        transportationTaskId = TM_STEPS.createTransportationTask(
            MARSCHROUTE_LOGISTIC_POINT,
            TOMILINO_LOGISTIC_POINT,
            SSKU_BIG,
            SUPPLIER_ID_FOR_BIG_SKU,
            "",
            COUNT_FOR_3_PALLETES
        );
        TM_STEPS.verifyStatusInHistory("transportation-task", transportationTaskId, "PREPARING");
        TM_STEPS.verifyStatusInHistory("transportation-task", transportationTaskId, "TRANSPORTATIONS_CREATED");
        long registerId = TM_STEPS.getPlanRegisterIdForTask(transportationTaskId);
        TM_STEPS.verifyCountFromRegister(registerId, COUNT_FOR_3_PALLETES, 3);
    }

    @Test
    @DisplayName("ТМ: Выбор транспорта для перемещения для кейса с 1 авто")
    @Disabled
    void choosingCarTest() {
        log.info("Starting choosing car test...");
        /**
         * Создание нескольких перемещений одновременно могут привести к выбору другого транспорта, чем ожидает тест
         * Локально тест можно запускать без 30-секундной задержки
         * Отключен, поскольку убрали создание мувментов у перемещений межсклада
         * **/
        Delayer.delay(1, TimeUnit.MINUTES);
        transportationTaskId = TM_STEPS.createTransportationTask(
            MARSCHROUTE_LOGISTIC_POINT,
            TOMILINO_LOGISTIC_POINT,
            SSKU_BIG,
            SUPPLIER_ID_FOR_BIG_SKU,
            "",
            COUNT_FOR_3_PALLETES
        );
        transportationId = TM_STEPS.getTransportationForTask(transportationTaskId);
        TM_STEPS.verifyTransport(transportationId, "13");
    }

    @Test
    @DisplayName("ТМ: Несколько перемещений из одной задачи на перемещение")
    void splitTransportationTest() {
        log.info("Starting split transportation test...");
        transportationTaskId = TM_STEPS.createTransportationTask(
            MARSCHROUTE_LOGISTIC_POINT,
            TOMILINO_LOGISTIC_POINT,
            SSKU_DROPSHIP,
            SUPPLIER_ID_DROPSHIP,
            "",
            COUNT_FOR_3_TRANSPORTATIONS
        );
        TM_STEPS.verifyStatusInHistory("transportation-task", transportationTaskId, "TRANSPORTATIONS_CREATED");
        TM_STEPS.verifyQuantityOfTransportationForTask(transportationTaskId, 3);
    }

    @Test
    @DisplayName("ТМ: статус перемещения, когда партнёры не поддержали методы")
    void methodsNotSupportedTest() {
        log.info("Starting methods not supported INVALID task status test...");
        transportationTaskId = TM_STEPS.createTransportationTask(
            NO_METHODS_LOGISTIC_POINT_1,
            NO_METHODS_LOGISTIC_POINT_2,
            SSKU_DROPSHIP,
            SUPPLIER_ID_DROPSHIP,
            "",
            COUNT
        );
        TM_STEPS.verifyTransportationTaskStatus(transportationTaskId, invalidStatus);
        TM_STEPS.verifyTransportationTaskValidationError(
            transportationTaskId,
            "не поддерживает следующие необходимые методы"
        );
    }

    /**
     * Тест выключен, поскольку проходит 20 минут. Можно спокойно прогонять локально
     **/
    @Test
    @DisplayName("ТМ: отмена перемещения, когда партнёр не поддержал метод отмены")
    @Disabled
    @Tag("SlowTest")
    void transportationCancelledTest() {
        log.info("Starting CANCELLED transportation status test...");
        transportationTaskId = TM_STEPS.createTransportationTask(
            CANCELLATION_TEST_LOGISTIC_POINT,
            SC_SDT_LOGISTIC_POINT,
            SSKU_DROPSHIP,
            SUPPLIER_ID_DROPSHIP,
            "",
            COUNT
        );
        transportationId = TM_STEPS.getTransportationForTask(transportationTaskId);
        TM_STEPS.verifyTransportationStatus(transportationId, TransportationStatus.MOVEMENT_SENT);
        TM_STEPS.getMovementExternalId(transportationId);
        movementTrackerId =
            DELIVERY_TRACKER_STEPS.getTrackerMetaByEntityId(TM_STEPS.getMovementIdWithPrefix(transportationId)).getId();
        DELIVERY_TRACKER_STEPS.addTmCheckpointToTracker(
            movementTrackerId,
            TmCheckpointStatus.MOVEMENT_COURIER_FOUND,
            EntityType.MOVEMENT
        );
        LGW_STEPS.getTasksFromListWithEntityIdAndRequestFlow(
            String.valueOf(TM_STEPS.getShopRequestIdForOutbound(transportationId)),
            LgwTaskFlow.FF_PUT_OUTBOUND,
            1,
            "NEW"
        );
        TM_STEPS.verifyOutboundStatus(transportationId, "ERROR");
        TM_STEPS.verifyTransportationStatus(transportationId, TransportationStatus.CANCELLED);
        TM_STEPS.verifyMovementStatus(transportationId, "WAITING_MANUAL_CANCELLATION");
    }
}
