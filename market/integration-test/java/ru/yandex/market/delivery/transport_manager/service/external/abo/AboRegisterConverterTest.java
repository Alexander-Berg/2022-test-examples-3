package ru.yandex.market.delivery.transport_manager.service.external.abo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.abo.api.entity.resupply.registry.RegistryPosition;
import ru.yandex.market.abo.api.entity.resupply.registry.RegistryType;
import ru.yandex.market.abo.api.entity.resupply.registry.UploadRegistryRequest;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.PartialId;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.enums.IdType;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.tpl.common.lrm.client.model.ReturnBox;
import ru.yandex.market.tpl.common.lrm.client.model.SearchReturn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AboRegisterConverterTest {

    private static final long INBOUND_PARTNER_ID = 42423L;
    private static final long DELIVERY_SERVICE_ID = 122342L;
    private static final String EXTERNAL_ID = "cse";
    private static final String DOC_ID = "doc-id";

    private static final AboRegisterConverter CONVERTER = new AboRegisterConverter(
        Clock.system(ZoneId.of("Europe/Moscow"))
    );
    private static final AboRegisterValidator VALIDATOR = new AboRegisterValidator();

    private static final Map<RegisterType, RegistryType> TYPES_MAP = Map.of(
        RegisterType.FACT_DELIVERED_ORDERS_RETURN, RegistryType.REFUND,
        RegisterType.FACT_UNDELIVERED_ORDERS_RETURN, RegistryType.UNPAID
    );

    @RegisterExtension
    final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @ParameterizedTest
    @MethodSource("getRegisterTypes")
    void convertTypes(RegisterType inType, RegistryType outType) throws AboRegistryConversionException {
        assertThat(
            convert(provideRegisterWithDetails().setType(inType))
                .getRegistryDetails()
                .getType()
        )
            .isEqualTo(outType);
    }

    @ParameterizedTest
    @MethodSource("getInvalidRegisterTypes")
    void convertInvalidTypes(RegisterType inType) {
        assertThrows(
            AboRegistryConversionException.class,
            () -> convert(provideRegisterWithDetails().setType(inType))
        );
    }

    @Test
    void nameFromDocumentId() throws AboRegistryConversionException {
        assertThat(
            convert(provideRegisterWithDetails().setDocumentId(DOC_ID))
                .getRegistryDetails()
                .getName()
        )
            .isEqualTo(DELIVERY_SERVICE_ID + "-" + DOC_ID);
    }

    @Test
    void checkDetails() throws AboRegistryConversionException {
        UploadRegistryRequest request = CONVERTER.toUploadRegistryRequest(
            INBOUND_PARTNER_ID, provideRegisterWithDetails(), null
        );

        softly.assertThat(request.getRegistryDetails().getWarehouseId())
            .isEqualTo(INBOUND_PARTNER_ID);
        softly.assertThat(request.getRegistryDetails().getName())
            .isEqualTo(DELIVERY_SERVICE_ID + "-EXT_ID-" + EXTERNAL_ID);
        softly.assertThat(request.getRegistryDetails().getDate())
            .isEqualTo(LocalDate.of(2021, 4, 7));
        softly.assertThat(request.getRegistryDetails().getDeliveryServiceId())
            .isEqualTo(DELIVERY_SERVICE_ID);
    }

    @Test
    void checkDetailsAndSetTransportationUnitId() throws AboRegistryConversionException {
        UploadRegistryRequest request = CONVERTER.toUploadRegistryRequest(
            INBOUND_PARTNER_ID, provideRegisterWithDetails(), 123L
        );

        softly.assertThat(request.getRegistryDetails().getWarehouseId())
                .isEqualTo(INBOUND_PARTNER_ID);
        softly.assertThat(request.getRegistryDetails().getName())
                .isEqualTo(DELIVERY_SERVICE_ID + "-EXT_ID-" + EXTERNAL_ID);
        softly.assertThat(request.getRegistryDetails().getDate())
                .isEqualTo(LocalDate.of(2021, 4, 7));
        softly.assertThat(request.getRegistryDetails().getDeliveryServiceId())
                .isEqualTo(DELIVERY_SERVICE_ID);
        softly.assertThat(request.getRegistryDetails().getLogisticPointId())
                .isEqualTo(123L);
    }

    @Test
    void checkDetailsWithLongRegisterName() throws AboRegistryConversionException {
        long deliveryServiceId = 1212121231212121211L;
        UploadRegistryRequest request = CONVERTER.toUploadRegistryRequest(
            INBOUND_PARTNER_ID, provideRegisterWithDetails(deliveryServiceId), null
        );

        softly.assertThat(request.getRegistryDetails().getWarehouseId())
            .isEqualTo(INBOUND_PARTNER_ID);
        softly.assertThat(request.getRegistryDetails().getName())
            .isEqualTo(EXTERNAL_ID);
        softly.assertThat(request.getRegistryDetails().getName().length())
            .isEqualTo(EXTERNAL_ID.length());
        softly.assertThat(request.getRegistryDetails().getDate())
            .isEqualTo(LocalDate.of(2021, 4, 7));
        softly.assertThat(request.getRegistryDetails().getDeliveryServiceId())
            .isEqualTo(deliveryServiceId);
    }

    @Test
    void checkFilterNullOrderIdForUndeliveredType() throws AboRegistryConversionException {
        Register register = provideRegisterWithDetails()
            .setType(RegisterType.FACT_UNDELIVERED_ORDERS_RETURN)
            .setBoxes(List.of(
                registerUnit(partialId(IdType.BOX_ID, "B1")),
                registerUnit(partialId(IdType.BOX_ID, "B2"), partialId(IdType.ORDER_ID, null)),
                registerUnit(partialId(IdType.BOX_ID, "B3"), partialId(IdType.ORDER_ID, "O3"))
            ));

        UploadRegistryRequest request = convert(register);

        softly.assertThat(request.getRegistryPositions()).hasSize(1);
        softly.assertThat(request.getRegistryPositions()).containsExactly(
            new RegistryPosition("O3", "B3")
        );

    }

    @ParameterizedTest
    @MethodSource("getRegisterWithInvalidDetails")
    void checkInvalidDetails(Long inboundPartnerId, Register register, Class<? extends Exception> exceptionClass) {
        assertThrows(
                exceptionClass,
                () -> VALIDATOR.validate(
                        CONVERTER.toUploadRegistryRequest(inboundPartnerId, register, null))
        );
    }

    @Test
    void checkUnits() throws AboRegistryConversionException {
        Register register = provideRegisterWithDetails()
            .setType(RegisterType.FACT_UNDELIVERED_ORDERS_RETURN)
            .setBoxes(List.of(
                registerUnit(
                    partialId(IdType.ORDER_ID, "100")
                ),
                registerUnit(
                    partialId(IdType.ORDER_ID, "200"),
                    partialId(IdType.BOX_ID, "100323")
                )
            ));

        UploadRegistryRequest request = convert(register);

        List<RegistryPosition> positions = request.getRegistryPositions();
        softly.assertThat(positions).hasSize(2);
        softly.assertThat(positions.get(0).getOrderId()).isEqualTo("100");
        softly.assertThat(positions.get(0).getTrackCode()).isNull();
        softly.assertThat(positions.get(1).getOrderId()).isEqualTo("200");
        softly.assertThat(positions.get(1).getTrackCode()).isEqualTo("100323");
    }

    @Test
    void ordersOnly() throws AboRegistryConversionException {
        Register register = provideRegisterWithDetails()
            .setType(RegisterType.FACT_UNDELIVERED_ORDERS_RETURN)
            .setItems(List.of(
                registerUnit(partialId(IdType.BOX_ID, "100"))
            ))
            .setPallets(List.of(
                registerUnit(partialId(IdType.BOX_ID, "200"))
            ))
            .setBoxes(List.of(
                registerUnit(partialId(IdType.ORDER_ID, "300"))
            ));

        UploadRegistryRequest request = convert(register);

        List<RegistryPosition> positions = request.getRegistryPositions();
        softly.assertThat(positions).hasSize(1);
        softly.assertThat(positions.get(0).getOrderId()).isEqualTo("300");
    }

    @Test
    void multiBoxOrders() throws AboRegistryConversionException {
        Register register = provideRegisterWithDetails()
            .setType(RegisterType.FACT_UNDELIVERED_ORDERS_RETURN)
            .setBoxes(List.of(
                registerUnit(partialId(IdType.ORDER_ID, "300")),
                registerUnit(partialId(IdType.ORDER_ID, "300")),
                registerUnit(partialId(IdType.ORDER_ID, "400"))
            ));

        UploadRegistryRequest request = convert(register);

        List<RegistryPosition> positions = request.getRegistryPositions();
        softly.assertThat(positions).hasSize(3);
        softly.assertThat(positions.get(0).getOrderId()).isEqualTo("300");
        softly.assertThat(positions.get(1).getOrderId()).isEqualTo("300");
        softly.assertThat(positions.get(2).getOrderId()).isEqualTo("400");
    }

    @Test
    void realOrderIdsForUndelivered() throws AboRegistryConversionException {
        Register register = provideRegisterWithDetails()
                .setType(RegisterType.FACT_UNDELIVERED_ORDERS_RETURN)
                .setBoxes(List.of(
                        registerUnit(partialId(IdType.BOX_ID, "300")),
                        registerUnit(partialId(IdType.ORDER_ID, "12021"), partialId(IdType.BOX_ID, "400")),
                        registerUnit(partialId(IdType.BOX_ID, "600")),
                        registerUnit(partialId(IdType.BOX_ID, "500"))
                ));

        ReturnBox returnBox = new ReturnBox();
        returnBox.externalId("300");
        ReturnBox returnBox2 = new ReturnBox();
        returnBox2.externalId("500");
        SearchReturn searchReturn = new SearchReturn();
        searchReturn.setOrderExternalId("-1");
        SearchReturn searchReturn2 = new SearchReturn();
        searchReturn2.setOrderExternalId("-2");
        searchReturn.addBoxesItem(returnBox);
        searchReturn2.addBoxesItem(returnBox2);

        CONVERTER.addAndGetInfoFromLRM(register, List.of(searchReturn, searchReturn2));

        UploadRegistryRequest request = convert(register);

        List<RegistryPosition> positions = request.getRegistryPositions();
        softly.assertThat(positions).hasSize(3);
        softly.assertThat(positions.get(0).getOrderId()).isEqualTo("-1");
        softly.assertThat(positions.get(0).getTrackCode()).isEqualTo("300");
        softly.assertThat(positions.get(1).getOrderId()).isEqualTo("12021");
        softly.assertThat(positions.get(1).getTrackCode()).isEqualTo("400");
        softly.assertThat(positions.get(2).getOrderId()).isEqualTo("-2");
        softly.assertThat(positions.get(2).getTrackCode()).isEqualTo("500");
    }

    @Test
    void orderIdIsRequiredForUndelivered() {
        Register register = provideRegisterWithDetails()
                .setType(RegisterType.FACT_UNDELIVERED_ORDERS_RETURN)
                .setBoxes(List.of(
                        registerUnit(partialId(IdType.BOX_ID, "300"))
                ));

        assertThrows(
                InvalidAboRegisterException.class,
                () -> VALIDATOR.validate(convert(register))
        );
    }

    @Test
    void fakeOrderIdsForDelivered() throws AboRegistryConversionException {
        Register register = provideRegisterWithDetails()
                .setType(RegisterType.FACT_DELIVERED_ORDERS_RETURN)
                .setBoxes(List.of(
                        registerUnit(partialId(IdType.BOX_ID, "300")),
                        registerUnit(partialId(IdType.ORDER_ID, "12021"), partialId(IdType.BOX_ID, "400")),
                        registerUnit(partialId(IdType.BOX_ID, "500"))
                ));

        UploadRegistryRequest request = convert(register);

        List<RegistryPosition> positions = request.getRegistryPositions();
        softly.assertThat(positions).hasSize(3);
        softly.assertThat(positions.get(0).getOrderId()).isEqualTo("1");
        softly.assertThat(positions.get(0).getTrackCode()).isEqualTo("300");
        softly.assertThat(positions.get(1).getOrderId()).isEqualTo("12021");
        softly.assertThat(positions.get(1).getTrackCode()).isEqualTo("400");
        softly.assertThat(positions.get(2).getOrderId()).isEqualTo("3");
        softly.assertThat(positions.get(2).getTrackCode()).isEqualTo("500");
    }

    private static RegisterUnit registerUnit(PartialId... partialIds) {
        return new RegisterUnit().setPartialIds(List.of(partialIds));
    }

    private static PartialId partialId(IdType type, String value) {
        return new PartialId().setIdType(type).setValue(value);
    }

    private static Stream<Arguments> getRegisterWithInvalidDetails() {
        return Stream.of(
            Arguments.of(
                null,
                provideRegisterWithDetails(),
                InvalidAboRegisterException.class
            ),
            Arguments.of(
                INBOUND_PARTNER_ID,
                provideRegisterWithDetails().setType(null),
                AboRegistryConversionException.class
            ),
            Arguments.of(
                INBOUND_PARTNER_ID,
                provideRegisterWithDetails().setDate(null),
                AboRegistryConversionException.class
            ),
            Arguments.of(
                INBOUND_PARTNER_ID,
                provideRegisterWithDetails().setExternalId(null),
                AboRegistryConversionException.class
            ),
            Arguments.of(
                INBOUND_PARTNER_ID,
                provideRegisterWithDetails().setPartnerId(null),
                AboRegistryConversionException.class
            )
        );
    }

    private UploadRegistryRequest convert(Register register) throws AboRegistryConversionException {
        return CONVERTER.toUploadRegistryRequest(INBOUND_PARTNER_ID, register, null);
    }

    private static Stream<Arguments> getRegisterTypes() {
        return TYPES_MAP.entrySet().stream()
            .sorted(Map.Entry.<RegisterType, RegistryType>comparingByKey())
            .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
    }

    private static Stream<Arguments> getInvalidRegisterTypes() {
        return EnumSet.complementOf(
            EnumSet.copyOf(TYPES_MAP.keySet())
        ).stream().map(Arguments::of);
    }

    private static Register provideRegisterWithDetails() {
        return provideRegisterWithDetails(DELIVERY_SERVICE_ID);
    }

    private static Register provideRegisterWithDetails(long deliveryServiceId) {
        return new Register()
            .setType(RegisterType.FACT_DELIVERED_ORDERS_RETURN)
            .setDate(Instant.now())
            .setExternalId(EXTERNAL_ID)
            .setDate(Instant.parse("2021-04-07T10:00:00.000Z"))
            .setPartnerId(deliveryServiceId);
    }
}
