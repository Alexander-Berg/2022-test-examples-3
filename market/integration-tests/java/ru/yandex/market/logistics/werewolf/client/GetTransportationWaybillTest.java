package ru.yandex.market.logistics.werewolf.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.werewolf.model.entity.TransportationWaybillData;
import ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat;

@DisplayName("Генерация ТН (транспортной накладной)")
class GetTransportationWaybillTest extends AbstractClientTest {

    @Test
    @DisplayName("Успешная генерация в html формате")
    void getTransportationWaybill() {
        prepareMockRequest(
            MockRequestUtils.MockRequest.builder()
                .requestMethod(HttpMethod.PUT)
                .header("accept", List.of("application/json; q=0.9", MediaType.TEXT_HTML_VALUE))
                .path("document/transportationWaybill/generate")
                .requestContentPath("request/transportation_waybill.json")
                .responseContentPath("response/transportation_waybill.html")
                .responseContentType(MediaType.TEXT_HTML)
                .build()
        );

        TransportationWaybillData data = createTransportationWaybillData();

        softly.assertThat(wwClient.generateTransportationWaybill(data, DocumentFormat.HTML))
            .isEqualTo(readFileIntoByteArray("response/transportation_waybill.html"));
    }

    @Test
    @DisplayName("Ошибка генерации: DocumentFormat не поддерживается")
    void getTransportationWaybillUnsupportedContent() {
        TransportationWaybillData transportationWaybillData = createTransportationWaybillData();

        softly.assertThatThrownBy(
            () -> wwClient.generateTransportationWaybill(transportationWaybillData, DocumentFormat.UNSUPPORTED)
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unsupported format: %s", DocumentFormat.UNSUPPORTED);
    }

    @Nonnull
    private TransportationWaybillData createTransportationWaybillData() {
        return TransportationWaybillData.builder()
            .id("000012345")
            .cargo(TransportationWaybillData.CargoInfo.builder()
                .name("Товарно-материальные ценности")
                .assessedValue(BigDecimal.valueOf(365066.26))
                .weight(BigDecimal.valueOf(900))
                .build()
            )
            .receptionTransferAct(TransportationWaybillData.RtaInfo.builder()
                .id("000012345")
                .date(LocalDate.parse("2021-03-24"))
                .build()
            )
            .inbound(TransportationWaybillData.InboundInfo.builder()
                .address(
                    "Россия, Московская область, городской округ Люберцы, "
                        + "рабочий поселок Томилино, мкрн. Птицефабрика, корпус 8"
                )
                .organization(TransportationWaybillData.OrganizationInfo.builder()
                    .organizationName("«Яндекс.Маркет»")
                    .address("121099, Россия, г. Москва, Новинский бульвар, дом 8, помещение 9.03, этаж 9")
                    .type("ООО")
                    .phoneNumber("89660001144")
                    .build()
                )
                .build()
            )
            .outbound(TransportationWaybillData.OutboundInfo.builder()
                .address("603157, г. Нижний Новогорд, ул. Коминтерна, д. 39 литер С")
                .organization(TransportationWaybillData.OrganizationInfo.builder()
                    .organizationName("«Компания Интерлогистика»")
                    .address("Россия, 105203, город Москва, улица Первомайская Ср., дом 50 корпус 1, комната 1")
                    .type("ЗАО")
                    .phoneNumber("89660001133")
                    .build()
                )
                .build()
            )
            .carrier(TransportationWaybillData.CarrierInfo.builder()
                .organization(TransportationWaybillData.OrganizationInfo.builder()
                    .organizationName("«ИстВард»")
                    .address("Россия, 125124, г. Москва, 5я улица Ямского поля, д.5, стр.1, помещение I, комната 15")
                    .type("ОАО")
                    .phoneNumber("89660001122")
                    .build()
                )
                .driver(TransportationWaybillData.DriverInfo.builder()
                    .name("Пурпурович Артур Ромуальдович")
                    .phoneNumber("89779272994")
                    .build()
                )
                .vehicle(TransportationWaybillData.VehicleInfo.builder()
                    .description("Газель")
                    .registrationNumber("Е299НМ799")
                    .build()
                )
                .build()
            )
            .payerInfo(
                TransportationWaybillData.PayerInfo.builder()
                    .organizationName("ООО «ЯНДЕКС.МАРКЕТ»")
                    .address("121099, Россия, г. Москва, Новинский бульвар, дом 8, помещение 9.03, этаж 9")
                    .account("40702810438000034726")
                    .bankName("ПАО Сбербанк")
                    .bic("044525225")
                    .correspondentAccount("30101810400000000225")
                    .build()
            )
            .build();
    }
}
