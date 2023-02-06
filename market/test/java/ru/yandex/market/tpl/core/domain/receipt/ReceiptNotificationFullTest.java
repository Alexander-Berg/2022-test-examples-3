package ru.yandex.market.tpl.core.domain.receipt;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderChequeRemoteDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.receipt.CreateReceiptRequestDto;
import ru.yandex.market.tpl.api.model.receipt.ReceiptNotificationRequestDto;
import ru.yandex.market.tpl.api.model.receipt.ReceiptServiceClientDto;
import ru.yandex.market.tpl.api.model.receipt.lifepay.LifePayReceiptType;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.receipt.lifepay.LifePayService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.external.lifepay.LifePayClient;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_EMAIL_PERSONAL_ID;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_PHONE_PERSONAL_ID;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class ReceiptNotificationFullTest extends TplAbstractTest {

    private final ReceiptService receiptService;
    private final ReceiptHelper receiptHelper;
    private final ReceiptServiceClientRepository receiptServiceClientRepository;
    private final ReceiptDataRepository receiptDataRepository;
    private final ConfigurationService configurationService;
    private final LifePayService lifePayService;
    private final LifePayClient lifePayClient;
    private final OrderCommandService orderCommandService;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final OrderRepository orderRepository;
    private final TransactionTemplate transactionTemplate;
    private final DbQueueTestUtil dbQueueTestUtil;

    @AfterEach
    void afterEach() {
    }

    /**
     * Проверка полного жиненного цикла оповещения от создания до отправки в триггерную платформу
     */
    @Test
    void notificationFullLifeCycleSuccessTest() {
        configurationService.mergeValue(ConfigurationProperties.RECEIPT_EMAIL_NOTIFICATION_ENABLED.name(), "true");

        // тестовые данные
        var user = testUserHelper.findOrCreateUser(83746L);
        var userShift = testUserHelper.createEmptyShift(user, LocalDate.now());
        var routePoint = testDataFactory.createEmptyRoutePoint(user, userShift.getId());
        var task = testDataFactory.addDeliveryTaskManual(user, userShift.getId(), routePoint.getId(), OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("54357762")
                .deliveryServiceId(239L)
                .paymentType(OrderPaymentType.CARD)
                .build());

        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        testUserHelper.arriveAtRoutePoint(userShift, routePoint.getId());

        transactionTemplate.execute( status -> {
                    var order = orderRepository.findByIdOrThrow(task.getOrderId());
                    order.setBuyerYandexUid(983375397364L);
                    orderRepository.save(order);
                    return status;
                });
        var order = orderRepository.findByIdOrThrow(task.getOrderId());
        var chequeRemoteDto = new OrderChequeRemoteDto(OrderPaymentType.CARD, OrderChequeType.SELL);
        orderCommandService.registerCheque(new OrderCommand.RegisterCheque(
                task, order.getId(), "aaa", null, chequeRemoteDto, true, false, null, Optional.empty()
        ));
        var orderCheque = transactionTemplate.execute(status -> orderRepository
                .findByIdOrThrow(order.getId())
                .streamCheques()
                .findFirst()
                .orElseThrow()
        );
        var serviceClient = receiptServiceClientRepository.findByIdOrThrow(ReceiptService.TPL_CLIENT_ID);
        var serviceClientDto = new ReceiptServiceClientDto(serviceClient.getId());

        // Создание чека и оповещения
        var receiptDataDto = receiptHelper.createReceiptDataDto(serviceClient);
        var createReceiptRequestDto = new CreateReceiptRequestDto(
                orderCheque.getReceiptId(), null, receiptDataDto, null,
                new ReceiptNotificationRequestDto(
                        "test@yandex.ru",
                        "+1234567890",
                        DEFAULT_EMAIL_PERSONAL_ID,
                        DEFAULT_PHONE_PERSONAL_ID
                )
        );
        createReceiptRequestDto.setPayload(Map.of("orderId", order.getId()));
        var receiptDataResponseDto = receiptService.createReceiptData(createReceiptRequestDto, serviceClientDto);

        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 2);

        // Фискализация
        var lifePayUuid = UUID.randomUUID().toString();
        doReturn(lifePayUuid).when(lifePayClient).createReceipt(any());
        var receiptData = receiptDataRepository.findByReceiptIdAndServiceClientOrThrow(
                receiptDataResponseDto.getReceiptId(), serviceClient);
        lifePayService.registerCheque(receiptData);
        var lifePayResultDto = receiptHelper.createLifePayProcessingResultDto(
                LifePayReceiptType.payment, lifePayUuid);
        lifePayService.createFiscalData(lifePayResultDto);

        // Проверка записи в очередь
        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 3);
    }

}
