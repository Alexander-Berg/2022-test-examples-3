package ru.yandex.market.logistics.werewolf.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.werewolf.dto.document.WriterOptions;
import ru.yandex.market.logistics.werewolf.model.entity.TransportationWaybillData;
import ru.yandex.market.logistics.werewolf.model.entity.TransportationWaybillData.CargoInfo.CargoInfoBuilder;
import ru.yandex.market.logistics.werewolf.model.entity.TransportationWaybillData.CarrierInfo.CarrierInfoBuilder;
import ru.yandex.market.logistics.werewolf.model.entity.TransportationWaybillData.DriverInfo.DriverInfoBuilder;
import ru.yandex.market.logistics.werewolf.model.entity.TransportationWaybillData.InboundInfo.InboundInfoBuilder;
import ru.yandex.market.logistics.werewolf.model.entity.TransportationWaybillData.OrganizationInfo.OrganizationInfoBuilder;
import ru.yandex.market.logistics.werewolf.model.entity.TransportationWaybillData.OutboundInfo.OutboundInfoBuilder;
import ru.yandex.market.logistics.werewolf.model.entity.TransportationWaybillData.PayerInfo.PayerInfoBuilder;
import ru.yandex.market.logistics.werewolf.model.entity.TransportationWaybillData.RtaInfo.RtaInfoBuilder;
import ru.yandex.market.logistics.werewolf.model.entity.TransportationWaybillData.TransportationWaybillDataBuilder;
import ru.yandex.market.logistics.werewolf.model.entity.TransportationWaybillData.VehicleInfo.VehicleInfoBuilder;
import ru.yandex.market.logistics.werewolf.model.enums.PageOrientation;
import ru.yandex.market.logistics.werewolf.model.enums.PageSize;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Генерация новой транспортной накладной (ТН)")
public class TransportationWaybillGeneratorTest extends AbstractDocumentGeneratorTest {

    @ParameterizedTest
    @MethodSource("transportationWaybillGenerationSource")
    @DisplayName("Успешная генерация ТН в формате HTML")
    void generateTransportationWaybillSuccess(
        String requestPath,
        String responsePath
    ) throws Exception {
        performAndDispatch(
            requestPath,
            request -> request.accept(MediaType.TEXT_HTML)
        )
            .andExpect(status().isOk())
            .andExpect(content()
                .string(extractFileContent(responsePath)));
    }

    @Nonnull
    private static Stream<Arguments> transportationWaybillGenerationSource() {
        return Stream.of(
            Arguments.of(
                "controller/documents/request/transportation_waybill.json",
                "controller/documents/response/transportation_waybill.html"
            ),
            Arguments.of(
                "controller/documents/request/transportation_waybill_with_null_fields.json",
                "controller/documents/response/transportation_waybill_with_null_fields.html"
            )
        );
    }

    @ParameterizedTest(name = "[{index}] {1} {2}")
    @MethodSource("validationSource")
    @DisplayName("Валидация входных параметров")
    void validation(
        Consumer<TransportationWaybillDataBuilder> builderConsumer,
        String field,
        String message
    ) throws Exception {
        TransportationWaybillDataBuilder dataBuilder = ordersDataBuilder();
        builderConsumer.accept(dataBuilder);
        performWithBody(
            objectMapper.writeValueAsString(dataBuilder.build()),
            request -> request.accept(MediaType.TEXT_HTML, APPLICATION_JSON_Q_09)
        )
            .andExpect(status().isBadRequest())
            .andExpect(fieldError(field, message));
    }

    @Nonnull
    private static Stream<Arguments> validationSource() {
        return Stream.of(
                validationCommon(),
                validationCargo(),
                validationRta(),
                validationInbound(),
                validationOutbound(),
                validationCarrier(),
                validationPayer()
            )
            .flatMap(Function.identity())
            .map(t -> Arguments.of(t.getLeft(), t.getMiddle(), t.getRight()));
    }

    @Nonnull
    private static Stream<Triple<Consumer<TransportationWaybillDataBuilder>, String, String>> validationCommon() {
        return Stream.of(
            Triple.of(
                b -> b.id(null),
                "id",
                "must not be blank"
            ),
            Triple.of(
                b -> b.cargo(null),
                "cargo",
                "must not be null"
            ),
            Triple.of(
                b -> b.receptionTransferAct(null),
                "receptionTransferAct",
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
                b -> b.carrier(null),
                "carrier",
                "must not be null"
            ),
            Triple.of(
                b -> b.payerInfo(null),
                "payerInfo",
                "must not be null"
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<TransportationWaybillDataBuilder>, String, String>> validationCargo() {
        return Stream.<Triple<UnaryOperator<CargoInfoBuilder>, String, String>>of(
            Triple.of(
                b -> b.name(null),
                "cargo.name",
                "must not be blank"
            ),
            Triple.of(
                b -> b.assessedValue(null),
                "cargo.assessedValue",
                "must not be null"
            ),
            Triple.of(
                b -> b.weight(null),
                "cargo.weight",
                "must not be null"
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
    private static Stream<Triple<Consumer<TransportationWaybillDataBuilder>, String, String>> validationRta() {
        return Stream.<Triple<UnaryOperator<RtaInfoBuilder>, String, String>>of(
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
    private static Stream<Triple<Consumer<TransportationWaybillDataBuilder>, String, String>> validationInbound() {
        return Stream.<Triple<UnaryOperator<InboundInfoBuilder>, String,
            String>>of(
            Triple.of(
                b -> b.address(null),
                "inbound.address",
                "must not be blank"
            ),
            Triple.of(
                b -> b.organization(null),
                "inbound.organization",
                "must not be null"
            ),
            Triple.of(
                b -> b.organization(organizationBuilder().type(null).build()),
                "inbound.organization.type",
                "must not be blank"
            ),
            Triple.of(
                b -> b.organization(organizationBuilder().organizationName(null).build()),
                "inbound.organization.organizationName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.organization(organizationBuilder().address(null).build()),
                "inbound.organization.address",
                "must not be blank"
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
    private static Stream<Triple<Consumer<TransportationWaybillDataBuilder>, String, String>> validationOutbound() {
        return Stream.<Triple<UnaryOperator<OutboundInfoBuilder>, String,
            String>>of(
            Triple.of(
                b -> b.address(null),
                "outbound.address",
                "must not be blank"
            ),
            Triple.of(
                b -> b.organization(null),
                "outbound.organization",
                "must not be null"
            ),
            Triple.of(
                b -> b.organization(organizationBuilder().type(null).build()),
                "outbound.organization.type",
                "must not be blank"
            ),
            Triple.of(
                b -> b.organization(organizationBuilder().organizationName(null).build()),
                "outbound.organization.organizationName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.organization(organizationBuilder().address(null).build()),
                "outbound.organization.address",
                "must not be blank"
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
    private static Stream<Triple<Consumer<TransportationWaybillDataBuilder>, String, String>> validationCarrier() {
        return Stream.<Triple<UnaryOperator<CarrierInfoBuilder>, String,
            String>>of(
            Triple.of(
                b -> b.organization(organizationBuilder().type(null).build()),
                "carrier.organization.type",
                "must not be blank"
            ),
            Triple.of(
                b -> b.organization(organizationBuilder().organizationName(null).build()),
                "carrier.organization.organizationName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.organization(organizationBuilder().address(null).build()),
                "carrier.organization.address",
                "must not be blank"
            ),
            Triple.of(
                b -> b.driver(driverBuilder().name(null).build()),
                "carrier.driver.name",
                "must not be blank"
            ),
            Triple.of(
                b -> b.driver(driverBuilder().phoneNumber(null).build()),
                "carrier.driver.phoneNumber",
                "must not be blank"
            ),
            Triple.of(
                b -> b.vehicle(vehicleBuilder().description(null).build()),
                "carrier.vehicle.description",
                "must not be blank"
            ),
            Triple.of(
                b -> b.vehicle(vehicleBuilder().registrationNumber(null).build()),
                "carrier.vehicle.registrationNumber",
                "must not be blank"
            )
        ).map(
            t -> Triple.of(
                b -> b.carrier(t.getLeft().apply(carrierBuilder()).build()),
                t.getMiddle(),
                t.getRight()
            )
        );
    }

    @Nonnull
    private static Stream<Triple<Consumer<TransportationWaybillDataBuilder>, String, String>> validationPayer() {
        return Stream.<Triple<UnaryOperator<PayerInfoBuilder>, String, String>>of(
            Triple.of(
                b -> b.address(null),
                "payerInfo.address",
                "must not be blank"
            ),
            Triple.of(
                b -> b.account(null),
                "payerInfo.account",
                "must not be blank"
            ),
            Triple.of(
                b -> b.correspondentAccount(null),
                "payerInfo.correspondentAccount",
                "must not be blank"
            ),
            Triple.of(
                b -> b.organizationName(null),
                "payerInfo.organizationName",
                "must not be blank"
            ),
            Triple.of(
                b -> b.bic(null),
                "payerInfo.bic",
                "must not be blank"
            ),
            Triple.of(
                b -> b.bankName(null),
                "payerInfo.bankName",
                "must not be blank"
            )
        ).map(
            t -> Triple.of(
                b -> b.payerInfo(t.getLeft().apply(payerBuilder()).build()),
                t.getMiddle(),
                t.getRight()
            )
        );
    }

    @Nonnull
    @Override
    protected String defaultRequestBodyPath() {
        return "controller/documents/request/transportation_waybill.json";
    }

    @Nonnull
    @Override
    protected String defaultHtmlResponseBodyPath() {
        return "controller/documents/response/transportation_waybill.html";
    }

    @Nonnull
    @Override
    protected String defaultFilename() {
        return "TW-000012345";
    }

    @Nonnull
    @Override
    protected String requestPath() {
        return "/document/transportationWaybill/generate";
    }

    @Nonnull
    @Override
    protected WriterOptions defaultWriterOptions() {
        return new WriterOptions(PageSize.A4, PageOrientation.PORTRAIT);
    }

    @Nonnull
    private static TransportationWaybillDataBuilder ordersDataBuilder() {
        return TransportationWaybillData.builder()
            .id("transportationWaybillId")
            .cargo(cargoBuilder().build())
            .receptionTransferAct(rtaBuilder().build())
            .inbound(inboundBuilder().build())
            .outbound(outboundBuilder().build())
            .carrier(carrierBuilder().build())
            .payerInfo(payerBuilder().build());
    }

    @Nonnull
    private static CargoInfoBuilder cargoBuilder() {
        return TransportationWaybillData.CargoInfo.builder()
            .name("cargoName")
            .assessedValue(BigDecimal.TEN)
            .weight(BigDecimal.ONE);
    }

    @Nonnull
    private static RtaInfoBuilder rtaBuilder() {
        return TransportationWaybillData.RtaInfo.builder()
            .date(LocalDate.parse("2020-10-01"))
            .id("rtaId");
    }

    @Nonnull
    private static InboundInfoBuilder inboundBuilder() {
        return TransportationWaybillData.InboundInfo.builder()
            .address("inboundAddress")
            .organization(organizationBuilder().build());
    }

    @Nonnull
    private static OutboundInfoBuilder outboundBuilder() {
        return TransportationWaybillData.OutboundInfo.builder()
            .address("outboundAddress")
            .organization(organizationBuilder().build());
    }

    @Nonnull
    private static CarrierInfoBuilder carrierBuilder() {
        return TransportationWaybillData.CarrierInfo.builder()
            .organization(organizationBuilder().build())
            .driver(driverBuilder().build())
            .vehicle(vehicleBuilder().build());
    }

    @Nonnull
    private static DriverInfoBuilder driverBuilder() {
        return TransportationWaybillData.DriverInfo.builder()
            .name("driverName")
            .phoneNumber("driverPhone");
    }

    @Nonnull
    private static VehicleInfoBuilder vehicleBuilder() {
        return TransportationWaybillData.VehicleInfo.builder()
            .description("vehicleDescription")
            .registrationNumber("registrationNumber");
    }

    @Nonnull
    private static PayerInfoBuilder payerBuilder() {
        return TransportationWaybillData.PayerInfo.builder()
            .account("account")
            .bic("bic")
            .address("address")
            .correspondentAccount("corrAccount")
            .bankName("bankName")
            .organizationName("organizationName");
    }

    @Nonnull
    private static OrganizationInfoBuilder organizationBuilder() {
        return TransportationWaybillData.OrganizationInfo.builder()
            .address("organizationAddress")
            .organizationName("organizationName")
            .phoneNumber("organizationPhone")
            .type("organizationType");
    }
}
