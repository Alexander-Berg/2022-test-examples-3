package ru.yandex.market.tpl.core.domain.receipt;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.api.model.receipt.CreateReceiptRequestDto;
import ru.yandex.market.tpl.api.model.receipt.FiscalDataDto;
import ru.yandex.market.tpl.api.model.receipt.FiscalTaxSumDto;
import ru.yandex.market.tpl.api.model.receipt.GetReceiptResponseDto;
import ru.yandex.market.tpl.api.model.receipt.ReceiptAgentType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptDataDto;
import ru.yandex.market.tpl.api.model.receipt.ReceiptDataType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptFfdVersion;
import ru.yandex.market.tpl.api.model.receipt.ReceiptItemDiscountType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptItemMarkingCodeStatus;
import ru.yandex.market.tpl.api.model.receipt.ReceiptItemMeasurementUnit;
import ru.yandex.market.tpl.api.model.receipt.ReceiptItemPaymentType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptItemTaxType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptItemType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptNotificationRequestDto;
import ru.yandex.market.tpl.api.model.receipt.ReceiptServiceClientDto;
import ru.yandex.market.tpl.api.model.receipt.ReceiptTaxSystem;
import ru.yandex.market.tpl.api.model.receipt.lifepay.LifePayProcessingResultDto;
import ru.yandex.market.tpl.api.model.receipt.lifepay.LifePayReceiptType;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_EMAIL_PERSONAL_ID;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_FIO_PERSONAL_ID;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_PHONE_PERSONAL_ID;

/**
 * @author valter
 */
@Component
@RequiredArgsConstructor
@Transactional
public class ReceiptHelper {

    private final ReceiptDataRepository receiptDataRepository;
    private final ReceiptService receiptService;
    private final ReceiptDtoMapper receiptDtoMapper;
    private final static Long DEFAULT_ORDER_ID = 123L;

    public LifePayProcessingResultDto createLifePayProcessingResultDto() {
        return createLifePayProcessingResultDto(null, null, "99990789463");
    }

    public LifePayProcessingResultDto createLifePayProcessingResultDto(LifePayReceiptType type,
                                                                       String uuid) {
        return createLifePayProcessingResultDto(type, uuid, "99990789463");
    }

    @SneakyThrows
    public LifePayProcessingResultDto createLifePayProcessingResultDto(LifePayReceiptType type,
                                                                       String uuid,
                                                                       String fn) {
        var result = JacksonUtil.fromString(
                String.format(IOUtils.toString(
                        this.getClass().getResourceAsStream("/lifepay/fiscal_response.json"),
                        StandardCharsets.UTF_8
                ), fn),
                LifePayProcessingResultDto.class
        );
        if (type != null) {
            result.setType(type);
        }
        if (uuid != null) {
            result.setUuid(uuid);
        }
        return result;
    }

    public GetReceiptResponseDto createReceiptAndFiscalize(ReceiptDataType receiptDataType,
                                                           String baseReceiptId,
                                                           ReceiptServiceClient client,
                                                           String fn) {
        return createReceiptAndFiscalize(null, baseReceiptId, receiptDataType, client, fn, DEFAULT_ORDER_ID);

    }

    public GetReceiptResponseDto createReceiptAndFiscalize(ReceiptDataType receiptDataType,
                                                           String baseReceiptId,
                                                           ReceiptServiceClient client) {
        return createReceiptAndFiscalize(receiptDataType, baseReceiptId, client, "1");
    }

    public GetReceiptResponseDto createReceiptAndFiscalize(ReceiptNotificationRequestDto notificationRequestDto,
                                                           ReceiptServiceClient client,
                                                           Long orderId) {
        return createReceiptAndFiscalize(notificationRequestDto, client, "1", orderId);
    }

    public GetReceiptResponseDto createReceiptAndFiscalize(ReceiptNotificationRequestDto notificationRequestDto,
                                                           ReceiptServiceClient client,
                                                           String fn,
                                                           Long orderId) {
        return createReceiptAndFiscalize(notificationRequestDto, null, ReceiptDataType.INCOME, client, fn,
                orderId);
    }

    public ReceiptData createReturnReceiptData(ReceiptData receiptData, ReceiptServiceClient client) {
        var request = createReceiptRequestDto(
                createReceiptDataDto(client, ReceiptDataType.getReturnType(receiptData.getType()).orElseThrow()),
                receiptData.getReceiptId(), null
        );
        return createReceiptData(request, client);
    }

    public ReceiptData createReceiptData(ReceiptDataType receiptDataType, ReceiptServiceClient client) {
        var request = createReceiptRequestDto(
                createReceiptDataDto(client, receiptDataType), null, null
        );
        return createReceiptData(request, client);
    }

    private ReceiptData createReceiptData(CreateReceiptRequestDto request, ReceiptServiceClient client) {
        GetReceiptResponseDto receiptData = receiptService.createReceiptData(
                request,
                new ReceiptServiceClientDto(client.getId())
        );
        return receiptDataRepository.findByReceiptIdAndServiceClientOrThrow(receiptData.getReceiptId(), client);
    }

    public GetReceiptResponseDto createReceiptAndFiscalize(ReceiptNotificationRequestDto notificationRequestDto,
                                                           String baseReceiptId,
                                                           ReceiptDataType receiptDataType,
                                                           ReceiptServiceClient client,
                                                           String fn,
                                                           Long orderId) {
        var request = createReceiptRequestDto(
                createReceiptDataDto(client, receiptDataType), baseReceiptId, notificationRequestDto, orderId
        );
        GetReceiptResponseDto receiptData = receiptService.createReceiptData(
                request,
                new ReceiptServiceClientDto(client.getId())
        );

        var fiscalDataRequest = new CreateFiscalDataRequest(
                createFiscalDataDto(client, fn), Instant.ofEpochMilli(0L),
                receiptDataRepository.findByReceiptIdAndServiceClientOrThrow(
                        receiptData.getReceiptId(), client).getId()
        );
        receiptService.createFiscalData(fiscalDataRequest).orElseThrow();
        return receiptService.getReceipt(receiptData.getReceiptId(), new ReceiptServiceClientDto(client.getId()));
    }

    public CreateReceiptRequestDto createReceiptRequestDto(ReceiptDataDto data) {
        return createReceiptRequestDto(data, null, null);
    }

    public CreateReceiptRequestDto createReceiptRequestDto(ReceiptDataDto data,
                                                           @Nullable ReceiptNotificationRequestDto notifications) {
        return createReceiptRequestDto(data, null, notifications);
    }

    public CreateReceiptRequestDto createReceiptRequestDto(ReceiptDataDto data,
                                                           String baseReceiptId,
                                                           @Nullable ReceiptNotificationRequestDto notifications) {
        return new CreateReceiptRequestDto(
                "test-receipt-id-" + data.getType(),
                baseReceiptId,
                data,
                Map.of("a", "b", "orderId", 123),
                notifications
        );
    }

    public CreateReceiptRequestDto createReceiptRequestDto(ReceiptDataDto data,
                                                           String baseReceiptId,
                                                           @Nullable ReceiptNotificationRequestDto notifications,
                                                           Long orderId) {
        return new CreateReceiptRequestDto(
                "test-receipt-id-" + data.getType(),
                baseReceiptId,
                data,
                Map.of("a", "b", "orderId", orderId),
                notifications
        );
    }

    public ReceiptDataDto createReceiptDataDto(ReceiptServiceClient client) {
        return createReceiptDataDto(client, ReceiptDataType.INCOME);
    }

    public ReceiptDataDto createReceiptDataDto(ReceiptServiceClient client, ReceiptDataType type) {
        return createReceiptDataDto(client, type, ReceiptFfdVersion.FFD_1_05);
    }

    public ReceiptDataDto createReceiptDataDto(ReceiptServiceClient client, ReceiptDataType type,
                                               ReceiptFfdVersion ffdVersion) {
        return new ReceiptDataDto(
                ffdVersion,
                type,
                new ReceiptDataDto.PaymentDto(
                        BigDecimal.valueOf(100L),
                        null, null, null, null, null
                ),
                List.of(ReceiptDataDto.ItemDto.builder()
                        .name("молоко")
                        .price(BigDecimal.valueOf(100L))
                        .quantity(BigDecimal.valueOf(1.1d))
                        .tax(ReceiptItemTaxType.VAT18)
                        .type(ReceiptItemType.PRODUCT)
                        .paymentType(ReceiptItemPaymentType.FULL_PAYMENT_W_DELIVERY)
                        .unit("л")
                        .discount(new ReceiptDataDto.DiscountDto(
                                ReceiptItemDiscountType.AMOUNT,
                                BigDecimal.TEN
                        ))
                        .agentItemType(ReceiptAgentType.NONE_AGENT)
                        .supplierInn("3948593487")
                        .supplierAddress("Льва Толстого, 17")
                        .nomenclatureCode("9237492374")
                        .countryCode("23423")
                        .declarationNumber("2349728349823")
                        .exciseAmount("15")
                        .additionalDetail("доп детали")
                        .unitValue("1.1")
                        .cisValues(List.of())
                        .cisFullValues(List.of())
                        .markingCodeStatus(ReceiptItemMarkingCodeStatus.MEASURED_SOLD)
                        .measurementUnit(ReceiptItemMeasurementUnit.LITER)
                        .markingFractionalQuantity("1/3")
                        .supplierData(new ReceiptDataDto.SupplierDataDto(
                                List.of("+70001234567"),
                                "ООО Поставщик"
                        ))
                        .build()
                ),
                ReceiptTaxSystem.OSN,
                receiptDtoMapper.mapToDto(client),
                ReceiptAgentType.NONE_AGENT,
                new ReceiptDataDto.CustomerInfoDto(
                        "Иван Иваныч",
                        DEFAULT_FIO_PERSONAL_ID,
                        "239472394",
                        "+7-555-ivaniva",
                        DEFAULT_PHONE_PERSONAL_ID,
                        "ivan@ivan.iva",
                        DEFAULT_EMAIL_PERSONAL_ID
                ),
                new ReceiptDataDto.CashierInfoDto(
                        "Петр Петрович",
                        "9485734985"
                ),
                "Льва Толстого, 16",
                "доп детали разные",
                null
        );
    }

    public FiscalDataDto createFiscalDataDto(ReceiptServiceClient client) {
        return createFiscalDataDto(client, "1");
    }

    public FiscalDataDto createFiscalDataDto(ReceiptServiceClient client, String fn) {
        return new FiscalDataDto(
                receiptDtoMapper.mapToDto(client),
                "2398423",
                "34923",
                fn,
                "3242348",
                "3204823",
                LocalDate.of(1990, 1, 12).atStartOfDay(),
                "3242343",
                "49785345",
                "Яндекс.ОФД",
                "http://ofd.ru/moy_check/" + fn,
                true,
                new FiscalTaxSumDto(
                        null,
                        null,
                        null,
                        BigDecimal.valueOf(18L),
                        null
                ),
                BigDecimal.valueOf(100L),
                "Льва Толстого, 16",
                "Льва Толстого, 17"
        );
    }

    public void setFfdVersion(ReceiptData receiptData, ReceiptFfdVersion ffdVersion) {
        receiptData.setFfdVersion(ffdVersion);
    }

}
