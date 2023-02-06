package ru.yandex.market.delivery.transport_manager.service.external.ffwf;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
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

import ru.yandex.market.delivery.transport_manager.converter.ffwf.FfwfConverter;
import ru.yandex.market.delivery.transport_manager.converter.ffwf.ReturnRegisterConverter;
import ru.yandex.market.delivery.transport_manager.converter.prefix.IdPrefixConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.PartialId;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.enums.IdType;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.service.external.lgw.LgwFFConverter;
import ru.yandex.market.ff.client.dto.PutSupplyRequestWithInboundRegisterDTO;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.common.RegistryBox;
import ru.yandex.market.tpl.common.lrm.client.model.ReturnBox;
import ru.yandex.market.tpl.common.lrm.client.model.SearchReturn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.byLessThan;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReturnRegisterConverterTest {

    private static final long INBOUND_PARTNER_ID = 42423L;
    private static final long OUTBOUND_PARTNER_ID = 44444L;
    private static final long DELIVERY_SERVICE_ID = 122342L;
    private static final String EXTERNAL_ID = "cse";
    private static final String DOC_ID = "doc-id";

    private static final ReturnRegisterConverter CONVERTER = new ReturnRegisterConverter(
        new LgwFFConverter(new IdPrefixConverter()),
        new FfwfConverter(null, null, null)
        );

    private static final Map<RegisterType, Integer> TYPES_MAP = Map.of(
        RegisterType.FACT_DELIVERED_ORDERS_RETURN, 7,
        RegisterType.FACT_UNDELIVERED_ORDERS_RETURN, 1008
    );
    public static final long LOGISTIC_POINT_ID = 123L;

    @RegisterExtension
    final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    @ParameterizedTest
    @MethodSource("registerTypeToOutType")
    void convertTypes(RegisterType inType, Integer outType) throws ReturnRegistryException {
        assertThat(convert(provideRegisterWithDetails().setType(inType))
                .getType()).isEqualTo(outType);
    }

    @ParameterizedTest
    @MethodSource("invalidRegisterTypes")
    void convertInvalidTypes(RegisterType registerType) {
        assertThrows(
            ReturnRegistryException.class,
            () -> convert(provideRegisterWithDetails().setType(registerType))
        );
    }

    @Test
    void nameFromDocumentId() throws ReturnRegistryException {
        assertThat(
            convert(provideRegisterWithDetails())
                .getInboundRegistry().getDocumentId()
        )
            .isEqualTo(DOC_ID);
    }

    @Test
    void checkDetails() throws ReturnRegistryException {
        PutSupplyRequestWithInboundRegisterDTO request = CONVERTER.toReturnRegistryRequest(
            INBOUND_PARTNER_ID, OUTBOUND_PARTNER_ID, provideRegisterWithDetails(), LOGISTIC_POINT_ID
        );

        softly.assertThat(request.getServiceId())
            .isEqualTo(INBOUND_PARTNER_ID);
        softly.assertThat(request.getShipper().getPartnerId())
            .isEqualTo(OUTBOUND_PARTNER_ID);
        softly.assertThat(request.getInboundRegistry().getDocumentId())
            .isEqualTo(DOC_ID);
        softly.assertThat(request.getDate())
            .isCloseTo(OffsetDateTime.now().plusDays(1), byLessThan(1, ChronoUnit.MINUTES));
        softly.assertThat(request.getLogisticsPointId())
            .isEqualTo(LOGISTIC_POINT_ID);
    }

    @ParameterizedTest
    @MethodSource("registerTypeToOutType")
    void checkUnits(RegisterType registerType) throws ReturnRegistryException {
        Register register = provideRegisterWithDetails()
            .setType(registerType)
            .setBoxes(List.of(
                registerUnit(
                    1L,
                    partialId(IdType.ORDER_ID, "100")
                ),
                registerUnit(
                    2L,
                    partialId(IdType.ORDER_ID, "200"),
                    partialId(IdType.BOX_ID, "100323")
                ),
                registerUnit(
                    3L,
                    partialId(IdType.BOX_ID, "100324")
                )
            ));

        PutSupplyRequestWithInboundRegisterDTO request = convert(register);

        List<RegistryBox> positions = request.getInboundRegistry().getBoxes();
        softly.assertThat(positions).hasSize(3);
        softly.assertThat(getPartialIdValue(positions.get(0), PartialIdType.BOX_ID)).isEqualTo(null);
        softly.assertThat(getPartialIdValue(positions.get(0), PartialIdType.ORDER_ID)).isEqualTo("100");
        softly.assertThat(getPartialIdValue(positions.get(1), PartialIdType.ORDER_ID)).isEqualTo("200");
        softly.assertThat(getPartialIdValue(positions.get(1), PartialIdType.BOX_ID)).isEqualTo("100323");
        softly.assertThat(getPartialIdValue(positions.get(2), PartialIdType.BOX_ID)).isEqualTo("100324");
        softly.assertThat(getPartialIdValue(positions.get(2), PartialIdType.ORDER_ID)).isEqualTo(null);
    }

    @ParameterizedTest
    @MethodSource("registerTypeToOutType")
    void multiBoxOrders(RegisterType registerType) throws ReturnRegistryException {
        Register register = provideRegisterWithDetails()
            .setType(registerType)
            .setBoxes(List.of(
                registerUnit(1L, partialId(IdType.BOX_ID, "300")),
                registerUnit(2L, partialId(IdType.BOX_ID, "300"))
            ));

        PutSupplyRequestWithInboundRegisterDTO request = convert(register);

        List<RegistryBox> positions = request.getInboundRegistry().getBoxes();
        softly.assertThat(positions).hasSize(2);
        softly.assertThat(getPartialIdValue(positions.get(0), PartialIdType.BOX_ID)).isEqualTo("300");
        softly.assertThat(getPartialIdValue(positions.get(1), PartialIdType.BOX_ID)).isEqualTo("300");
    }

    @ParameterizedTest
    @MethodSource("registerTypes")
    void getOrderIdsFromLrmForUndelivered(RegisterType registerType) throws ReturnRegistryException {
        Register register = provideRegisterWithDetails()
                .setType(registerType)
                .setBoxes(List.of(registerUnit(1L, partialId(IdType.BOX_ID, "300"))));

        ReturnBox returnBox = new ReturnBox();
        returnBox.externalId("300");
        SearchReturn searchReturn = new SearchReturn();
        searchReturn.setOrderExternalId("-1");
        searchReturn.addBoxesItem(returnBox);

        CONVERTER.addAndGetInfoFromLRM(register, List.of(searchReturn));

        softly.assertThat(register.getBoxes().get(0).getPartialId(IdType.ORDER_ID).orElse(null)).isEqualTo("-1");
    }

    private String getPartialIdValue(RegistryBox registryBox, PartialIdType idType) {
        return registryBox
            .getUnitInfo().getCompositeId().getPartialIds().stream()
            .filter(id -> id.getIdType().equals(idType)).findFirst()
            .map(ru.yandex.market.logistic.gateway.common.model.common.PartialId::getValue).orElse(null);
    }

    private static RegisterUnit registerUnit(long id, PartialId... partialIds) {
        return new RegisterUnit().setId(id).setPartialIds(List.of(partialIds));
    }

    private static PartialId partialId(IdType type, String value) {
        return new PartialId().setIdType(type).setValue(value);
    }

    private PutSupplyRequestWithInboundRegisterDTO convert(Register register) throws ReturnRegistryException {
        return CONVERTER.toReturnRegistryRequest(INBOUND_PARTNER_ID, OUTBOUND_PARTNER_ID, register, LOGISTIC_POINT_ID);
    }

    private static Stream<Arguments> registerTypeToOutType() {
        return TYPES_MAP.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
    }

    private static Stream<Arguments> registerTypes() {
        return TYPES_MAP.keySet().stream().sorted().map(Arguments::of);
    }

    private static Stream<Arguments> invalidRegisterTypes() {
        return EnumSet.complementOf(
            EnumSet.copyOf(TYPES_MAP.keySet())
        ).stream().sorted().map(Arguments::of);
    }

    private static Register provideRegisterWithDetails() {
        return provideRegisterWithDetails(DELIVERY_SERVICE_ID);
    }

    private static Register provideRegisterWithDetails(long deliveryServiceId) {
        return new Register()
            .setType(RegisterType.FACT_DELIVERED_ORDERS_RETURN)
            .setDate(Instant.now())
            .setDocumentId(DOC_ID)
            .setExternalId(EXTERNAL_ID)
            .setDate(Instant.parse("2021-04-07T10:00:00.000Z"))
            .setPartnerId(deliveryServiceId)
            .setBoxes(
                List.of(
                   new RegisterUnit().setType(UnitType.BOX).setPartialIds(List.of())));
    }
}
