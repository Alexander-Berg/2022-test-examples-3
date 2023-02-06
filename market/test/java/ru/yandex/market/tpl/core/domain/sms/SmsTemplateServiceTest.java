package ru.yandex.market.tpl.core.domain.sms;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.receipt.FiscalDataDto;
import ru.yandex.market.tpl.api.model.receipt.ReceiptDataType;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.ds.DsZoneOffsetCachingService;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderDelivery;
import ru.yandex.market.tpl.core.domain.usershift.tracking.Tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class SmsTemplateServiceTest {

    private final ZoneOffset offset = ZoneOffset.of("+03:00");
    private final String externalOrderId = "12345678";
    private final String trackingId = "cc2ce24f8aa64e9cb24025285f5219b3";
    private final BigDecimal orderPrice = BigDecimal.valueOf(12345.54);
    private final BigDecimal orderPrice2 = BigDecimal.valueOf(15345.00);
    private final int deliveryHourFrom = 10;
    private final int deliveryHourTo = 14;

    private ConfigurationProviderAdapter configurationProviderAdapter;
    private SmsTemplateService smsTemplateService;

    @BeforeEach
    void setup() {
        var offsetService = mock(DsZoneOffsetCachingService.class);
        var makerV1 = new SmsPayloadMakerV1(offsetService);
        var makerV2 = new SmsPayloadMakerV2(offsetService);
        configurationProviderAdapter = mock(ConfigurationProviderAdapter.class);
        smsTemplateService = new SmsTemplateService(List.of(makerV1, makerV2), configurationProviderAdapter);
        lenient().doReturn(offset).when(offsetService).getOffsetForDs(any());
        lenient().doReturn(Optional.of(1)).when(configurationProviderAdapter)
                .getValueAsInteger(ConfigurationProperties.SMS_PAYLOAD_VERSION);
    }

    @Test
    void smsTemplatesV1Test() {
        var tracking1 = createTestTracking();
        var tracking2 = createTestTracking(true);
        var linkV1 = "https://m.pokupki.market.yandex.ru/tracking/" + trackingId;

        assertThat(smsTemplateService.unpaidOrder(tracking1)).isEqualTo(String.format("Курьер привезёт заказ %s " +
                "сегодня с %d:00 до %d:00. К оплате — %s ₽. Точный интервал и детали доставки смотрите на %s",
                externalOrderId, deliveryHourFrom, deliveryHourTo, orderPrice, linkV1));

        assertThat(smsTemplateService.prepaidOrder(tracking1)).isEqualTo(String.format("Курьер привезёт заказ %s " +
                "сегодня с %d:00 до %d:00. Точный интервал и детали доставки смотрите на %s",
                externalOrderId, deliveryHourFrom, deliveryHourTo, linkV1));

        assertThat(smsTemplateService.orderDeliveredToTheDoor(tracking1)).isEqualTo(String.format("Заказ %s ждёт у " +
                "двери. Пожалуйста, заберите его и подтвердите, что получили: %s",
                externalOrderId, linkV1));

        assertThat(smsTemplateService.roverDelivery(tracking1)).isEqualTo(String.format("Заказ %s может доставить " +
                "Яндекс.Ровер. Хотите? Тогда в течение 10 минут подтвердите встречу %s. Если нет, оставим заказ в " +
                "курьерском подъезде", externalOrderId, linkV1));

        assertThat(smsTemplateService.orderLeavingConfirmation(tracking1)).isEqualTo(String.format("Заказ %s " +
                "ждёт вас в курьерском подъезде", externalOrderId));

        assertThat(smsTemplateService.multiOrderPreviouslyConfirmationWithTracking(tracking1)).isEqualTo(
                String.format("Вам удобно встретить курьера в течение 1,5 часов? Пожалуйста, подтвердите " +
                        "или перенесите доставку%s", ": " + linkV1 + "?utm_source=tpl"));

        assertThat(smsTemplateService.multiOrderPreviouslyConfirmation()).isEqualTo("Вам удобно встретить курьера " +
                "в течение 1,5 часов? Пожалуйста, подтвердите или перенесите доставку.");

        assertThat(smsTemplateService.multiOrder(List.of(tracking1, tracking2))).isEqualTo(String.format(
                "Курьер привезёт заказы %s сегодня с %d:00 до %d:00. К оплате — " + orderPrice2 +
                        " ₽. Точный интервал и детали доставки смотрите на %s",
                externalOrderId + ", " + externalOrderId, deliveryHourFrom, deliveryHourTo, linkV1
        ));
    }

    @Test
    void smsTemplatesV2Test() {
        doReturn(Optional.of(2)).when(configurationProviderAdapter)
                .getValueAsInteger(ConfigurationProperties.SMS_PAYLOAD_VERSION);

        var tracking1 = createTestTracking();
        var tracking2 = createTestTracking(true);
        var linkV2 = "https://m.pokupki.market.yandex.ru/tracking/" + trackingId;

        // Сумма с копейками должна быть выведена полностью
        assertThat(smsTemplateService.unpaidOrder(tracking1)).isEqualTo(String.format("Привезём заказ %s " +
                        "с %d:00 до %d:00 %s К оплате %s ₽",
                externalOrderId, deliveryHourFrom, deliveryHourTo, linkV2, orderPrice.toPlainString()));

        // Сумма без копеек должна быть выведена без .00 и укладываться в 2 СМС
        check2SMSText(smsTemplateService.unpaidOrder(tracking2), String.format("Привезём заказ %s " +
                        "с %d:00 до %d:00 %s К оплате %s ₽",
                externalOrderId, deliveryHourFrom + 1, deliveryHourTo + 1, linkV2, orderPrice2.intValue()));

        check2SMSText(smsTemplateService.prepaidOrder(tracking1), String.format("Привезём заказ %s сегодня " +
                        "с %d:00 до %d:00: %s",
                externalOrderId, deliveryHourFrom, deliveryHourTo, linkV2));

        check2SMSText(smsTemplateService.orderDeliveredToTheDoor(tracking1), String.format("Заказ %s ждёт у двери. " +
                        "Подтвердите получение: %s",
                externalOrderId, linkV2));

        check2SMSText(smsTemplateService.multiOrderPreviouslyConfirmationWithTracking(tracking1),
                String.format("Доставка будет в течение 1,5 ч. Подтвердить или перенести %s", linkV2));

        assertThat(smsTemplateService.multiOrder(List.of(tracking1, tracking2))).isEqualTo(String.format(
                "Привезём заказы %s с %d:00 до %d:00: %s К оплате %s ₽",
                externalOrderId + ", " + externalOrderId, deliveryHourFrom, deliveryHourTo, linkV2, orderPrice2.intValue()
        ));
    }

    @Test
    void getReceiptTest() {
        assertThat(smsTemplateService.getReceiptType(ReceiptDataType.INCOME)).isEqualTo("продажа");
        assertThat(smsTemplateService.getReceiptType(ReceiptDataType.CHARGE)).isEqualTo("покупка");
        assertThat(smsTemplateService.getReceiptType(ReceiptDataType.RETURN_INCOME)).isEqualTo("возврат");
        assertThat(smsTemplateService.getReceiptType(ReceiptDataType.RETURN_CHARGE)).isEqualTo("возврат");

        var dateTime = LocalDateTime.of(1990, 10, 11, 15, 30);
        var fiscalData = mock(FiscalDataDto.class);
        when(fiscalData.getDt()).thenReturn(dateTime);
        when(fiscalData.getTotal()).thenReturn(BigDecimal.valueOf(23456));
        when(fiscalData.getOfdUrl()).thenReturn("testOfdUrl");

        assertThat(smsTemplateService.getReceiptText(ReceiptDataType.INCOME, fiscalData)).isEqualTo(String.format(
                "чек - %s%nдата: %s%nитого: %s%n%s", "продажа", "1990-10-11 15:30", 23456, "testOfdUrl"
        ));
    }

    /**
     * Проверка, что полученный текст соответсвует ожидаемому и влезает в 2 СМС (134 символа)
     */
    private void check2SMSText(String actual, String expected) {
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.length()).isLessThanOrEqualTo(134);
    }

    private Tracking createTestTracking() {
        return createTestTracking(false);
    }

    private Tracking createTestTracking(boolean altVersion) {
        var tracking = new Tracking();
        var order = mock(Order.class);
        when(order.getExternalOrderId()).thenReturn(externalOrderId);
        lenient().when(order.getDeliveryServiceId()).thenReturn(1L);
        var delivery = mock(OrderDelivery.class);

        if (altVersion) {
            when(order.getPaymentStatus()).thenReturn(OrderPaymentStatus.UNPAID);
            when(order.getTotalPrice()).thenReturn(orderPrice2);
            lenient().when(delivery.getDeliveryIntervalFrom())
                    .thenReturn(LocalDate.now().atTime(deliveryHourFrom + 1, 0).toInstant(offset));
            lenient().when(delivery.getDeliveryIntervalTo())
                    .thenReturn(LocalDate.now().atTime(deliveryHourTo + 1, 0).toInstant(offset));
        } else {
            when(order.getPaymentStatus()).thenReturn(OrderPaymentStatus.PAID);
            when(order.getTotalPrice()).thenReturn(orderPrice);
            when(delivery.getDeliveryIntervalFrom())
                    .thenReturn(LocalDate.now().atTime(deliveryHourFrom, 0).toInstant(offset));
            when(delivery.getDeliveryIntervalTo())
                    .thenReturn(LocalDate.now().atTime(deliveryHourTo, 0).toInstant(offset));
        }

        lenient().when(order.getDelivery()).thenReturn(delivery);
        tracking.setId(trackingId);
        tracking.setOrder(order);
        return tracking;
    }
}
