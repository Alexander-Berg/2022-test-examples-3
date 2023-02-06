package ru.yandex.market.logistics.werewolf.client;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.werewolf.model.entity.waybill.Car;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.Cargo;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.Courier;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.LegalInfo;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.Payer;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.ReceptionTransferAct;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.TransportationUnit;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.WaybillInformation;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Генерация транспортной накладной")
class GetWaybillTest extends AbstractClientTest {

    @Test
    @DisplayName("Успешная генерация в xlsx формате")
    void getWaybill() {
        prepareMockRequest(
            MockRequestUtils.MockRequest.builder()
                .requestMethod(HttpMethod.PUT)
                .header("accept", List.of("application/json; q=0.9", MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .path("document/waybill/generate")
                .requestContentPath("request/waybill.json")
                .responseContentPath("response/waybill.xlsx")
                .responseContentType(MediaType.APPLICATION_OCTET_STREAM)
                .build()
        );

        WaybillInformation data = createTrnInformationData();

        softly.assertThat(wwClient.generateWaybill(data))
            .isEqualTo(extractFileContent("response/waybill.xlsx").getBytes(StandardCharsets.UTF_8));
    }

    private static WaybillInformation createTrnInformationData() {
        return WaybillInformation.builder()
            .id("000012345")
            .date(LocalDate.parse("2022-02-22"))
            .receptionTransferAct(ReceptionTransferAct.builder()
                .id("000012345")
                .date(LocalDate.parse("2021-03-24"))
                .build()
            )
            .payer(Payer.builder()
                .organizationName("ООО «ЯНДЕКС.МАРКЕТ»")
                .address("121099, Россия, г. Москва, Новинский бульвар, дом 8, помещение 9.03, этаж 9")
                .account("40702810438000034726")
                .bankName("ПАО Сбербанк")
                .bic("044525225")
                .correspondentAccount("30101810400000000225")
                .build()
            )
            .sender(
                LegalInfo.builder()
                    .legalName("Имя отправителя")
                    .legalAddress("Адрес отправителя")
                    .inn("ИНН отправителя")
                    .phoneNumber("Телефон отправителя")
                    .build()
            )
            .receiver(
                LegalInfo.builder()
                    .legalName("Имя получателя")
                    .legalAddress("Адрес получателя")
                    .inn("ИНН получателя")
                    .phoneNumber("Телефон получателя")
                    .build()
            )
            .transporter(
                LegalInfo.builder()
                    .legalName("Имя перевозчика")
                    .legalAddress("Адрес перевозчика")
                    .inn("ИНН перевозчика")
                    .phoneNumber("Телефон перевозчика")
                    .build()
            )
            .car(
                Car.builder()
                    .model("Модель ТС")
                    .type("Тип ТС")
                    .number("Номер ТС")
                    .trailerNumber("Номер прицепа")
                    .ownershipType(1)
                    .build()
            )
            .inbound(
                TransportationUnit.builder()
                    .address("Адрес приёмщика")
                    .legalName("Имя приёмщика")
                    .inn("ИНН приёмщика")
                    .plannedIntervalStart(LocalDateTime.parse("2022-03-05T16:00"))
                    .build()
            )
            .outbound(
                TransportationUnit.builder()
                    .address("Адрес отправщика")
                    .legalName("Имя отправщика")
                    .inn("ИНН отправщика")
                    .plannedIntervalStart(LocalDateTime.parse("2022-03-05T15:00"))
                    .build()
            )
            .cargo(
                Cargo.builder()
                    .name("Товары бытового назначения")
                    .placesNumber(32)
                    .assessedValue(BigDecimal.TEN)
                    .weight(new BigDecimal(20))
                    .width(new BigDecimal(1))
                    .length(new BigDecimal(2))
                    .height(new BigDecimal(3))
                    .volume(new BigDecimal(6))
                    .build()
            )
            .courier(
                Courier.builder()
                    .name("Имя курьера")
                    .surName("Фамилия курьера")
                    .patronymic("Отчество курьера")
                    .inn("ИНН курьера")
                    .phoneNumber("Телефон курьера")
                    .build()
            )
            .build();
    }
}
