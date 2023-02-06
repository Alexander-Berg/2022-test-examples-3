package ru.yandex.market.logistics.werewolf.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.test.integration.utils.ExcelFileComparisonUtils;
import ru.yandex.market.logistics.werewolf.AbstractTest;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.Car;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.Cargo;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.Courier;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.LegalInfo;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.Payer;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.ReceptionTransferAct;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.ReceptionTransferAct.ReceptionTransferActBuilder;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.TransportationUnit;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.WaybillInformation;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.WaybillInformation.WaybillInformationBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContentInBytes;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Генерация транспортной накладной")
public class WaybillGeneratorTest extends AbstractTest {
    private static final MediaType APPLICATION_JSON_Q_09 = new MediaType("application", "json", 0.9);
    @Autowired
    protected ObjectMapper objectMapper;

    @Test
    @DisplayName("Успех")
    void success() throws Exception {
        WaybillInformationBuilder dataBuilder = waybillBuilder();

        MvcResult syncResult = mockMvc.perform(buildRequest(dataBuilder))
            .andExpect(status().isOk())
            .andReturn();

        MvcResult asyncResult = mockMvc.perform(asyncDispatch(syncResult))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andReturn();

        ExcelFileComparisonUtils.assertEquals(
            asyncResult.getResponse().getContentAsByteArray(),
            extractFileContentInBytes("controller/documents/response/waybill.xlsx")
        );
    }

    @Test
    @DisplayName("Успех, минимальное заполнение")
    void successMinimal() throws Exception {
        WaybillInformationBuilder dataBuilder = minimalWaybillBuilder();

        MvcResult syncResult = mockMvc.perform(buildRequest(dataBuilder))
            .andExpect(status().isOk())
            .andReturn();

        MvcResult asyncResult = mockMvc.perform(asyncDispatch(syncResult))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
            .andReturn();

        ExcelFileComparisonUtils.assertEquals(
            asyncResult.getResponse().getContentAsByteArray(),
            extractFileContentInBytes("controller/documents/response/minimalWaybill.xlsx")
        );
    }

    @ParameterizedTest(name = "[{index}] {1} {2}")
    @MethodSource("validationSource")
    @DisplayName("Валидация входных параметров")
    void validation(
        Consumer<WaybillInformationBuilder> builderConsumer,
        String field,
        String message
    ) throws Exception {
        WaybillInformationBuilder dataBuilder = waybillBuilder();
        builderConsumer.accept(dataBuilder);

        mockMvc.perform(buildRequest(dataBuilder))
            .andExpect(status().isBadRequest())
            .andExpect(fieldError(field, message));
    }

    private MockHttpServletRequestBuilder buildRequest(WaybillInformationBuilder dataBuilder)
        throws JsonProcessingException {
        return MockMvcRequestBuilders
            .put("/document/waybill/generate")
            .accept(MediaType.APPLICATION_OCTET_STREAM, APPLICATION_JSON_Q_09)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dataBuilder.build()));
    }

    @Nonnull
    private static Stream<Arguments> validationSource() {
        return Stream.of(
                validationCommon(),
                validationRta(),
                validationPayer(),
                validationInbound(),
                validationOutbound(),
                validationCargo()
            )
            .flatMap(Function.identity())
            .map(t -> Arguments.of(t.getLeft(), t.getMiddle(), t.getRight()));
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillInformationBuilder>, String, String>> validationCommon() {
        return Stream.of(
            Triple.of(
                b -> b.id(null),
                "id",
                "must not be blank"
            ),
            Triple.of(
                b -> b.date(null),
                "date",
                "must not be null"
            ),
            Triple.of(
                b -> b.receptionTransferAct(null),
                "receptionTransferAct",
                "must not be null"
            ),
            Triple.of(
                b -> b.payer(null),
                "payer",
                "must not be null"
            ),
            Triple.of(
                b -> b.sender(null),
                "sender",
                "must not be null"
            ),
            Triple.of(
                b -> b.receiver(null),
                "receiver",
                "must not be null"
            ),
            Triple.of(
                b -> b.transporter(null),
                "transporter",
                "must not be null"
            ),
            Triple.of(
                b -> b.car(null),
                "car",
                "must not be null"
            ),
            Triple.of(
                b -> b.inbound(null),
                "inbound",
                "must not be null"
            ),
            Triple.of(
                b -> b.outbound(null),
                "outbound",
                "must not be null"
            ),
            Triple.of(
                b -> b.cargo(null),
                "cargo",
                "must not be null"
            ),
            Triple.of(
                b -> b.courier(null),
                "courier",
                "must not be null"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillInformationBuilder>, String, String>> validationRta() {
        return Stream.<Triple<UnaryOperator<ReceptionTransferActBuilder>, String, String>>of(
            Triple.of(
                b -> b.date(null),
                "receptionTransferAct.date",
                "must not be null"
            ),
            Triple.of(
                b -> b.id(null),
                "receptionTransferAct.id",
                "must not be blank"
            )
        ).map(
            t -> Triple.of(
                b -> b.receptionTransferAct(t.getLeft().apply(rtaBuilder()).build()),
                t.getMiddle(),
                t.getRight()
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillInformationBuilder>, String, String>> validationPayer() {
        return Stream.<Triple<UnaryOperator<Payer.PayerBuilder>, String, String>>of(
            Triple.of(
                b -> b.address(null),
                "payer.address",
                "must not be blank"
            ),
            Triple.of(
                b -> b.account(null),
                "payer.account",
                "must not be blank"
            ),
            Triple.of(
                b -> b.correspondentAccount(null),
                "payer.correspondentAccount",
                "must not be blank"
            ),
            Triple.of(
                b -> b.organizationName(null),
                "payer.organizationName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.bic(null),
                "payer.bic",
                "must not be blank"
            ),
            Triple.of(
                b -> b.bankName(null),
                "payer.bankName",
                "must not be blank"
            )
        ).map(
            t -> Triple.of(
                b -> b.payer(t.getLeft().apply(payerBuilder()).build()),
                t.getMiddle(),
                t.getRight()
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillInformationBuilder>, String, String>> validationInbound() {
        return Stream.<Triple<UnaryOperator<TransportationUnit.TransportationUnitBuilder>, String, String>>of(
            Triple.of(
                b -> b.address(null),
                "inbound.address",
                "must not be blank"
            ),
            Triple.of(
                b -> b.legalName(null),
                "inbound.legalName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.inn(null),
                "inbound.inn",
                "must not be blank"
            ),
            Triple.of(
                b -> b.plannedIntervalStart(null),
                "inbound.plannedIntervalStart",
                "must not be null"
            )
        ).map(
            t -> Triple.of(
                b -> b.inbound(t.getLeft().apply(inboundBuilder()).build()),
                t.getMiddle(),
                t.getRight()
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillInformationBuilder>, String, String>> validationOutbound() {
        return Stream.<Triple<UnaryOperator<TransportationUnit.TransportationUnitBuilder>, String, String>>of(
            Triple.of(
                b -> b.address(null),
                "outbound.address",
                "must not be blank"
            ),
            Triple.of(
                b -> b.legalName(null),
                "outbound.legalName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.inn(null),
                "outbound.inn",
                "must not be blank"
            ),
            Triple.of(
                b -> b.plannedIntervalStart(null),
                "outbound.plannedIntervalStart",
                "must not be null"
            )
        ).map(
            t -> Triple.of(
                b -> b.outbound(t.getLeft().apply(outboundBuilder()).build()),
                t.getMiddle(),
                t.getRight()
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<WaybillInformationBuilder>, String, String>> validationCargo() {
        return Stream.<Triple<UnaryOperator<Cargo.CargoBuilder>, String, String>>of(
            Triple.of(
                b -> b.name(null),
                "cargo.name",
                "must not be blank"
            )
        ).map(
            t -> Triple.of(
                b -> b.cargo(t.getLeft().apply(cargoBuilder()).build()),
                t.getMiddle(),
                t.getRight()
            )
        );
    }

    @Nonnull
    private static WaybillInformationBuilder waybillBuilder() {
        return WaybillInformation.builder()
            .id("149132")
            .date(LocalDate.parse("2022-03-05"))
            .receptionTransferAct(rtaBuilder().build())
            .payer(payerBuilder().build())
            .sender(senderBuilder().build())
            .receiver(receiverBuilder().build())
            .transporter(transporterBuilder().build())
            .car(carBuilder().build())
            .inbound(inboundBuilder().build())
            .outbound(outboundBuilder().build())
            .cargo(cargoBuilder().build())
            .courier(courierBuilder().build());
    }

    @Nonnull
    private static WaybillInformationBuilder minimalWaybillBuilder() {
        return WaybillInformation.builder()
            .id("149132")
            .date(LocalDate.parse("2022-03-05"))
            .receptionTransferAct(rtaBuilder().build())
            .payer(payerBuilder().build())
            .sender(senderBuilder().build())
            .receiver(receiverBuilder().build())
            .inbound(inboundBuilder().build())
            .outbound(outboundBuilder().build())
            .transporter(LegalInfo.builder().build())
            .car(Car.builder().build())
            .cargo(Cargo.builder().name("Товары бытового назначения").build())
            .courier(Courier.builder().build());
    }

    @Nonnull
    private static ReceptionTransferAct.ReceptionTransferActBuilder rtaBuilder() {
        return ReceptionTransferAct.builder()
            .id("149132")
            .date(LocalDate.parse("2022-03-04"));
    }

    @Nonnull
    private static Payer.PayerBuilder payerBuilder() {
        return Payer.builder()
            .organizationName("ООО «ЯНДЕКС.МАРКЕТ»")
            .address("121099, Россия, г. Москва, Новинский бульвар, дом 8, помещение 9.03, этаж 9")
            .account("40702810438000034726")
            .bankName("ПАО Сбербанк")
            .bic("044525225")
            .correspondentAccount("30101810400000000225");
    }

    @Nonnull
    private static LegalInfo.LegalInfoBuilder senderBuilder() {
        return LegalInfo.builder()
            .legalName("ИП Иванов Иван Иванович")
            .legalAddress("454052, Челябинская область, г. Челябинск, ул. Черкасская, д. 8, кв. 45")
            .inn("1111111111")
            .phoneNumber("892311111111");
    }

    @Nonnull
    private static LegalInfo.LegalInfoBuilder receiverBuilder() {
        return LegalInfo.builder()
            .legalName("ООО \"Маркет.Операции\"")
            .legalAddress("121099, Россия, г.Москва, ул.Тимура Фрунзе, д.11, корпус 2 БЦ Мамонтов 3 этаж Комната 83122")
            .inn("9704083264")
            .phoneNumber("89322222222");
    }

    @Nonnull
    private static LegalInfo.LegalInfoBuilder transporterBuilder() {
        return LegalInfo.builder()
            .legalName("ООО \"Перевозчик\"")
            .legalAddress("109542, Москва г, Солнечная ул., дом № 1, оф. 15")
            .inn("3333333333")
            .phoneNumber("89233333333");
    }

    @Nonnull
    private static Car.CarBuilder carBuilder() {
        return Car.builder()
            .model("КАМАЗ")
            .type("Грузовик")
            .number("Р958ЕА797")
            .trailerNumber("WIELTON ЕО623777")
            .ownershipType(1);
    }

    @Nonnull
    private static TransportationUnit.TransportationUnitBuilder inboundBuilder() {
        return TransportationUnit.builder()
            .address(
                "Россия, Московская область, городской округ Люберцы, рабочий поселок Томилино, мкрн. Птицефабрика, "
                    + "корпус 8"
            )
            .legalName("ООО \"Маркет.Операции\"")
            .inn("9704083264")
            .plannedIntervalStart(LocalDateTime.parse("2022-03-05T16:00:00"));
    }

    @Nonnull
    private static TransportationUnit.TransportationUnitBuilder outboundBuilder() {
        return TransportationUnit.builder()
            .address("г. Москва, ул. Травяная, 8, корп. 4")
            .legalName("ИП Иванов Иван Иванович")
            .inn("1111111111")
            .plannedIntervalStart(LocalDateTime.parse("2022-03-05T15:00:00"));
    }

    @Nonnull
    private static Cargo.CargoBuilder cargoBuilder() {
        return Cargo.builder()
            .name("Товары бытового назначения")
            .placesNumber(32)
            .assessedValue(BigDecimal.valueOf(10))
            .weight(BigDecimal.valueOf(20));
    }

    @Nonnull
    private static Courier.CourierBuilder courierBuilder() {
        return Courier.builder()
            .name("Петр")
            .surName("Петров")
            .patronymic("Петрович")
            .inn("4444444444")
            .phoneNumber("89234444444");
    }

    @Nonnull
    private ResultMatcher fieldError(String field, String message) {
        return jsonPath(
            "message",
            Matchers.equalTo(String.format(
                "Following validation errors occurred:\nField: '%s', message: '%s'",
                field,
                message
            ))
        );
    }
}
