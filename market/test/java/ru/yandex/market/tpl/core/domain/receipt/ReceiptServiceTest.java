package ru.yandex.market.tpl.core.domain.receipt;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.receipt.CreateReceiptRequestDto;
import ru.yandex.market.tpl.api.model.receipt.FiscalReceiptStatus;
import ru.yandex.market.tpl.api.model.receipt.GetReceiptResponseDto;
import ru.yandex.market.tpl.api.model.receipt.ReceiptDataType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptNotificationDto;
import ru.yandex.market.tpl.api.model.receipt.ReceiptNotificationRequestDto;
import ru.yandex.market.tpl.api.model.receipt.ReceiptNotificationStatus;
import ru.yandex.market.tpl.api.model.receipt.ReceiptNotificationsDto;
import ru.yandex.market.tpl.api.model.receipt.ReceiptServiceClientDto;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.receipt.lifepay.LifePayFiscalRequestRepository;
import ru.yandex.market.tpl.core.domain.receipt.lifepay.LifePayService;
import ru.yandex.market.tpl.core.external.lifepay.LifePayClient;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_EMAIL_PERSONAL_ID;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_PHONE_PERSONAL_ID;

/**
 * @author valter
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class ReceiptServiceTest {

    private static final String SERVICE_ID = "test-service-id";

    private final ReceiptService receiptService;
    private final ReceiptDataRepository receiptDataRepository;
    private final ReceiptFiscalDataRepository receiptFiscalDataRepository;
    private final ReceiptServiceClientRepository receiptServiceClientRepository;
    private final ReceiptProcessorResolver receiptProcessorResolver;
    private final LifePayFiscalRequestRepository lifePayFiscalRequestRepository;
    private final LifePayService lifePayService;
    private final ReceiptHelper receiptHelper;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final Clock clock;
    private final TestDataFactory testDataFactory;

    private ReceiptServiceClient receiptServiceClient;
    private Order order;

    @MockBean
    private LifePayClient lifePayClient;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock);
        receiptServiceClient = receiptServiceClientRepository.save(
                new ReceiptServiceClient(SERVICE_ID, "8436583647", ReceiptProcessorType.LIFE_PAY));
        order = testDataFactory.generateOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId("LO-1234667")
                        .deliveryServiceId(-3L)
                        .build()
        );
        dbQueueTestUtil.clear(QueueType.RECEIPT_SMS_NOTIFICATION_SEND);
    }


    @Test
    void createReturnReceiptWithoutBase() {
        assertThatThrownBy(
                () -> receiptHelper.createReceiptAndFiscalize(
                        ReceiptDataType.RETURN_INCOME, "not_existing_base_id", receiptServiceClient
                )
        ).isInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void createReturnReceiptWithBaseWrongType() {
        GetReceiptResponseDto baseDto = receiptHelper.createReceiptAndFiscalize(
                ReceiptDataType.INCOME, null, receiptServiceClient
        );
        assertThatThrownBy(
                () -> receiptHelper.createReceiptAndFiscalize(
                        ReceiptDataType.RETURN_CHARGE, baseDto.getReceiptId(), receiptServiceClient
                )
        ).isInstanceOf(TplInvalidParameterException.class);
    }

    @Test
    void createReturnReceiptWithBase() {
        GetReceiptResponseDto baseDto = receiptHelper.createReceiptAndFiscalize(
                ReceiptDataType.INCOME, null, receiptServiceClient, "1"
        );
        GetReceiptResponseDto returnDto = receiptHelper.createReceiptAndFiscalize(
                ReceiptDataType.RETURN_INCOME, baseDto.getReceiptId(), receiptServiceClient, "2"
        );
        assertThat(returnDto.getStatus()).isEqualTo(FiscalReceiptStatus.OK);
        assertThat(returnDto.getBaseReceiptId()).isEqualTo(baseDto.getReceiptId());
    }

    @Test
    void notificationsInProgressAfterCreation() {
        var request = receiptHelper.createReceiptRequestDto(receiptHelper.createReceiptDataDto(receiptServiceClient),
                new ReceiptNotificationRequestDto(
                        "valter@yandex-team.ru",
                        "+1234567890",
                        DEFAULT_EMAIL_PERSONAL_ID,
                        DEFAULT_PHONE_PERSONAL_ID
                ));
        ReceiptNotificationsDto notifications = receiptService.createReceiptData(
                request,
                clientDto()
        ).getNotifications();
        assertThat(notifications.getEmail().get(0).getStatus()).isEqualTo(ReceiptNotificationStatus.IN_PROGRESS);
        assertThat(notifications.getSms()).isEmpty();
    }

    @Test
    void loOrderSendSmsWhenAlsoWriteEmail() {
        ReceiptNotificationsDto notifications = receiptHelper.createReceiptAndFiscalize(
                new ReceiptNotificationRequestDto(
                        "valter@yandex-team.ru",
                        "89999999999",
                        DEFAULT_EMAIL_PERSONAL_ID,
                        DEFAULT_PHONE_PERSONAL_ID), receiptServiceClient,
                order.getId()
        ).getNotifications();
        assertThat(notifications.getSms().size()).isEqualTo(1);
        assertThat(notifications.getSms().get(0).getStatus()).isEqualTo(ReceiptNotificationStatus.IN_QUEUE);
    }

    @Test
    void smsSentAfterFiscalization() {
        GetReceiptResponseDto receiptData = receiptHelper.createReceiptAndFiscalize(
                new ReceiptNotificationRequestDto(null,
                        "+712345678",
                        null,
                        DEFAULT_PHONE_PERSONAL_ID), receiptServiceClient, order.getId()
        );
        ReceiptNotificationsDto notifications = receiptData.getNotifications();
        assertThat(notifications.getEmail()).isEmpty();
        assertThat(notifications.getSms().size()).isEqualTo(1);
        assertThat(notifications.getSms().get(0).getStatus()).isEqualTo(ReceiptNotificationStatus.IN_QUEUE);

        sendSms(receiptData.getReceiptId());
    }

    private void sendSms(String receiptId) {
        dbQueueTestUtil.executeSingleQueueItem(QueueType.RECEIPT_SMS_NOTIFICATION_SEND);
        ReceiptNotificationsDto notifications = receiptService.getReceipt(receiptId, clientDto()).getNotifications();
        assertTrue(
                notifications.getSms().stream()
                        .map(ReceiptNotificationDto::getStatus)
                        .anyMatch(s -> s == ReceiptNotificationStatus.SENT)
        );
    }

    @Test
    void notificationsAfterFiscalization() {
        GetReceiptResponseDto receiptData = receiptHelper.createReceiptAndFiscalize(null, receiptServiceClient,
                order.getId());
        ReceiptNotificationsDto notifications = receiptService.createNotifications(new ReceiptNotificationRequestDto(
                "valter@yandex-team.ru",
                "+1234567890",
                DEFAULT_EMAIL_PERSONAL_ID,
                DEFAULT_PHONE_PERSONAL_ID
        ), receiptData.getReceiptId(), clientDto());
        assertThat(notifications.getSms().get(0).getStatus()).isEqualTo(ReceiptNotificationStatus.IN_QUEUE);

        sendSms(receiptData.getReceiptId());
    }

    @Test
    void createNotificationSaveEmailAndPhoneTest() {
        var email = "test@yandex.ru";
        var phone = "+1234567890";
        var receiptData = receiptHelper.createReceiptAndFiscalize(null, receiptServiceClient, order.getId());
        var clientDto = clientDto();
        receiptService.createNotifications(new ReceiptNotificationRequestDto(
                email,
                phone,
                DEFAULT_EMAIL_PERSONAL_ID,
                DEFAULT_PHONE_PERSONAL_ID),
                receiptData.getReceiptId(), clientDto);
        var serviceClient = receiptServiceClientRepository.findByIdOrThrow(clientDto.getServiceId());
        var savedReceiptData = receiptDataRepository
                .findByReceiptIdAndServiceClient(receiptData.getReceiptId(), serviceClient);
        assertThat(savedReceiptData).isPresent();
        var savedNotifications = savedReceiptData.get().getNotifications();
        assertThat(savedNotifications).hasSize(1);
        for (var notification : savedNotifications) {
            assertThat(notification.getEmail()).isEqualTo(email);
            assertThat(notification.getPhone()).isEqualTo(phone);
        }
    }

    @Test
    void newNotificationsAfterFiscalization() {
        var notificationsRequestDto = new ReceiptNotificationRequestDto(
                "valter@yandex-team.ru",
                "+1234567890",
                DEFAULT_EMAIL_PERSONAL_ID,
                DEFAULT_PHONE_PERSONAL_ID
        );
        GetReceiptResponseDto receiptData = receiptHelper.createReceiptAndFiscalize(notificationsRequestDto,
                receiptServiceClient, order.getId());
        ReceiptNotificationsDto notifications = receiptService.createNotifications(
                notificationsRequestDto, receiptData.getReceiptId(), clientDto());
        assertThat(notifications.getSms().get(0).getStatus()).isEqualTo(ReceiptNotificationStatus.IN_QUEUE);
        assertThat(notifications.getSms().get(1).getStatus()).isEqualTo(ReceiptNotificationStatus.IN_QUEUE);

        sendSms(receiptData.getReceiptId());
    }

    @Test
    void createReceiptData() {
        var request = receiptHelper.createReceiptRequestDto(receiptHelper.createReceiptDataDto(receiptServiceClient));
        assertThat(receiptService.createReceiptData(
                request,
                clientDto()
        ))
                .isEqualToIgnoringGivenFields(expectedResponse(request, null), "receiptCreationTime");
    }

    private GetReceiptResponseDto expectedResponse(CreateReceiptRequestDto receiptRequestDto,
                                                   @Nullable CreateFiscalDataRequest fiscalDataRequest) {
        return new GetReceiptResponseDto(
                FiscalReceiptStatus.PROCESSING,
                null,
                receiptRequestDto.getReceiptId(),
                null,
                receiptRequestDto.getPayload(),
                new ReceiptNotificationsDto(
                        List.of(),
                        List.of()
                ),
                fiscalDataRequest == null ? null : ReceiptDtoMapper.extractQrCode(
                        receiptFiscalDataRepository.findByReceiptData(
                                receiptDataRepository.findByReceiptIdAndServiceClientOrThrow(
                                        receiptRequestDto.getReceiptId(), receiptServiceClient)
                        ).orElseThrow()
                ),
                receiptRequestDto.getReceipt(),
                Instant.ofEpochMilli(0L),
                Optional.ofNullable(fiscalDataRequest).map(CreateFiscalDataRequest::getDto).orElse(null),
                Optional.ofNullable(fiscalDataRequest).map(CreateFiscalDataRequest::getFiscalRequestTime).orElse(null),
                Optional.ofNullable(fiscalDataRequest).map(r -> Instant.ofEpochMilli(0L)).orElse(null)
        );
    }

    @Test
    void createReceiptDataTwice() {
        var request = receiptHelper.createReceiptRequestDto(receiptHelper.createReceiptDataDto(receiptServiceClient));
        var expected = receiptService.createReceiptData(request, clientDto());
        assertThat(receiptService.createReceiptData(
                request,
                clientDto()
        ))
                .isEqualTo(expected);
    }

    @Test
    void getReceipt() {
        var request = receiptHelper.createReceiptRequestDto(receiptHelper.createReceiptDataDto(receiptServiceClient));
        receiptService.createReceiptData(request, clientDto());
        assertThat(receiptService.getReceipt(
                request.getReceiptId(),
                clientDto()
        ))
                .isEqualToIgnoringGivenFields(expectedResponse(request, null), "receiptCreationTime");
    }

    @Test
    void getReceiptQr() {
        var request = receiptHelper.createReceiptRequestDto(receiptHelper.createReceiptDataDto(receiptServiceClient));
        var receiptDataRequest = receiptService.createReceiptData(request, clientDto());
        var fiscalDataRequest = new CreateFiscalDataRequest(
                receiptHelper.createFiscalDataDto(receiptServiceClient), Instant.ofEpochMilli(0L),
                receiptDataRepository.findByReceiptIdAndServiceClientOrThrow(
                        receiptDataRequest.getReceiptId(), receiptServiceClient).getId()
        );
        receiptService.createFiscalData(fiscalDataRequest);
        assertThat(receiptService.getReceiptQr(
                request.getReceiptId(),
                clientDto()
        ))
                .isEqualTo(expectedResponse(request, fiscalDataRequest).getQr());
    }

    @Test
    void createFiscalData() {
        var receiptDataRequest =
                receiptHelper.createReceiptRequestDto(receiptHelper.createReceiptDataDto(receiptServiceClient));
        receiptService.createReceiptData(receiptDataRequest, clientDto());
        var fiscalDataRequest = new CreateFiscalDataRequest(
                receiptHelper.createFiscalDataDto(receiptServiceClient), Instant.ofEpochMilli(0L),
                receiptDataRepository.findByReceiptIdAndServiceClientOrThrow(
                        receiptDataRequest.getReceiptId(), receiptServiceClient).getId()
        );
        receiptService.createFiscalData(fiscalDataRequest);

        var actual = receiptService.getReceipt(
                receiptDataRequest.getReceiptId(),
                clientDto()
        );
        assertThat(actual).isEqualToIgnoringGivenFields(expectedResponse(receiptDataRequest, fiscalDataRequest),
                "receiptCreationTime", "fiscalDataCreationTime", "status");
        assertThat(actual.getStatus()).isEqualTo(FiscalReceiptStatus.OK);
    }

    @Test
    void createFiscalDataLifePay() {
        var receiptDataRequest =
                receiptHelper.createReceiptRequestDto(receiptHelper.createReceiptDataDto(receiptServiceClient));
        var uuid = "0bf63680-1c0d-5af8-b10c-eb456c603cc7";
        doReturn(uuid).when(lifePayClient).createReceipt(any());
        receiptService.createReceiptData(receiptDataRequest, clientDto());

        var receiptData = receiptDataRepository
                .findByReceiptIdAndServiceClient(receiptDataRequest.getReceiptId(), receiptServiceClient)
                .orElseThrow();
        receiptProcessorResolver.resolve(receiptServiceClient).registerCheque(receiptData);

        var dto = receiptHelper.createLifePayProcessingResultDto();
        lifePayService.createFiscalData(dto);

        var createFiscalDataRequest = lifePayService.mapToCreateFiscalDataRequest(dto,
                lifePayFiscalRequestRepository.findByUuidOrThrow(uuid));

        var actual = receiptService.getReceipt(
                receiptDataRequest.getReceiptId(),
                clientDto()
        );
        assertThat(actual).isEqualToIgnoringGivenFields(expectedResponse(receiptDataRequest, createFiscalDataRequest),
                "receiptCreationTime", "fiscalDataCreationTime", "status");
        assertThat(actual.getStatus()).isEqualTo(FiscalReceiptStatus.OK);
    }

    private ReceiptServiceClientDto clientDto() {
        return new ReceiptServiceClientDto(SERVICE_ID);
    }

}
