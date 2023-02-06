package ru.yandex.market.logistics.nesu.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.MovementCourierDto;
import ru.yandex.market.delivery.transport_manager.model.dto.MovementDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterDto.RegisterDtoBuilder;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterUnitDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationPartnerExtendedInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationSearchDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationUnitDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationUnitDto.TransportationUnitDtoBuilder;
import ru.yandex.market.delivery.transport_manager.model.enums.MovementStatus;
import ru.yandex.market.delivery.transport_manager.model.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.model.enums.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.model.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.model.filter.RegisterUnitSearchFilter;
import ru.yandex.market.delivery.transport_manager.model.page.Page;
import ru.yandex.market.delivery.transport_manager.model.page.PageRequest;

import static org.mockito.Mockito.doReturn;

@ParametersAreNonnullByDefault
public final class TMFactory {

    public static final long SHIPMENT_ID = 500;
    public static final long PARTNER_ID = 100;
    public static final long SECOND_PARTNER_ID = 101;
    public static final long THIRD_PARTNER_ID = 102;
    public static final long WAREHOUSE_FROM = 900;
    public static final long WAREHOUSE_TO = 910;
    public static final long DROPOFF_LOGISTIC_POINT = 913;
    public static final long OUTBOUND_REGISTER_ID = 1000;
    public static final long INBOUND_REGISTER_ID = 1100;
    public static final String PARTNER_NAME = "Какой-то партнёр";
    public static final String SECOND_PARTNER_NAME = "Второй партнёр";
    public static final String THIRD_PARTNER_NAME = "Третий партнёр";
    public static final String PARTNER_TYPE = "LEGAL_ENTITY";
    public static final String PARTNER_ADDRESS = "Какой-то адрес";
    public static final String COURIER_SURNAME = "Курьеров";
    public static final String COURIER_NAME = "Курьер";
    public static final String COURIER_PATRONYMIC = "Курьерович";
    public static final String COURIER_PHONE = "+7654321";
    public static final String CAR_MODEL = "Газелька";
    public static final String CAR_NUMBER = "е123бой45";
    public static final LocalDate SHIPMENT_DATE = LocalDate.of(2021, 3, 4);
    public static final LocalDateTime SHIPMENT_DATE_TIME = SHIPMENT_DATE.atTime(10, 21);

    private static final String OUTBOUND_PREFIX = "TMU";
    private static final long OUTBOUND_ID = 300;

    private TMFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static Optional<TransportationDto> transportation(TransportationUnitDto outbound, MovementDto movement) {
        return transportation(outbound, movement, defaultInbound().build());
    }

    @Nonnull
    public static Optional<TransportationDto> transportation(
        TransportationUnitDto outbound,
        MovementDto movement,
        TransportationUnitDto inbound
    ) {
        return Optional.of(fill(new TransportationDto(), outbound, movement, inbound));
    }

    @Nonnull
    public static TransportationSearchDto transportationSearch(TransportationUnitDto outbound, MovementDto movement) {
        return transportationSearch(outbound, movement, defaultInbound().build());
    }

    @Nonnull
    public static TransportationSearchDto transportationSearch(TransportationUnitDto outbound) {
        return transportationSearch(outbound, defaultMovement().build(), defaultInbound().build());
    }

    @Nonnull
    public static TransportationSearchDto transportationSearch(int index) {
        TransportationSearchDto result = transportationSearch(
            defaultOutbound().yandexId(outboundId(index)).build(),
            defaultMovement().build()
        );
        result.setId(SHIPMENT_ID + index);
        return result;
    }

    @Nonnull
    public static TransportationSearchDto transportationSearch(
        TransportationUnitDto outbound,
        MovementDto movement,
        TransportationUnitDto inbound
    ) {
        return fill(new TransportationSearchDto(), outbound, movement, inbound);
    }

    @Nonnull
    private static <T extends TransportationDto> T fill(
        T result,
        TransportationUnitDto outbound,
        MovementDto movement,
        TransportationUnitDto inbound
    ) {
        result
            .setId(SHIPMENT_ID)
            .setOutbound(outbound)
            .setMovement(movement)
            .setInbound(inbound);
        return result;
    }

    @Nonnull
    public static TransportationUnitDtoBuilder defaultOutbound() {
        return TransportationUnitDto.builder()
            .status(TransportationUnitStatus.NEW)
            .yandexId(outboundId())
            .plannedIntervalStart(SHIPMENT_DATE_TIME)
            .plannedIntervalEnd(SHIPMENT_DATE_TIME.plusMonths(1).plusDays(1).plusHours(1).plusMinutes(1))
            .logisticPointId(WAREHOUSE_FROM)
            .partner(transportationPartner(PARTNER_ID, PARTNER_NAME));
    }

    @Nonnull
    public static RegisterDtoBuilder outboundRegister() {
        return outboundRegister(0);
    }

    @Nonnull
    public static RegisterDtoBuilder outboundRegister(int index) {
        return RegisterDto.builder().id(OUTBOUND_REGISTER_ID + index).type(RegisterType.PLAN);
    }

    @Nonnull
    public static TransportationUnitDtoBuilder defaultInbound() {
        return TransportationUnitDto.builder()
            .status(TransportationUnitStatus.NEW)
            .logisticPointId(WAREHOUSE_TO)
            .partner(transportationPartner(SECOND_PARTNER_ID, SECOND_PARTNER_NAME));
    }

    @Nonnull
    public static RegisterDtoBuilder inboundRegister() {
        return inboundRegister(0);
    }

    @Nonnull
    public static RegisterDtoBuilder inboundRegister(int index) {
        return RegisterDto.builder().id(INBOUND_REGISTER_ID + index).type(RegisterType.FACT);
    }

    @Nonnull
    public static MovementDto.MovementDtoBuilder defaultMovement() {
        return movement(PARTNER_ID, PARTNER_NAME);
    }

    @Nonnull
    public static MovementDto.MovementDtoBuilder movement(long partnerId, String name) {
        return MovementDto.builder()
            .status(MovementStatus.NEW)
            .partner(transportationPartner(partnerId, name))
            .courier(MovementCourierDto.builder()
                .surname(COURIER_SURNAME)
                .name(COURIER_NAME)
                .patronymic(COURIER_PATRONYMIC)
                .phone(COURIER_PHONE)
                .carModel(CAR_MODEL)
                .carNumber(CAR_NUMBER)
                .build()
            );
    }

    @Nonnull
    public static TransportationPartnerExtendedInfoDto transportationPartner(@Nullable Long partnerId, String name) {
        return TransportationPartnerExtendedInfoDto.builder()
            .id(partnerId)
            .name(name)
            .legalName("legal " + name)
            .legalAddress(PARTNER_ADDRESS)
            .legalType(PARTNER_TYPE)
            .build();
    }

    public static void mockOutboundUnits(TransportManagerClient transportManagerClient, List<RegisterUnitDto> units) {
        mockOutboundUnits(transportManagerClient, units, 0, 1);
    }

    public static void mockOutboundUnits(
        TransportManagerClient transportManagerClient,
        List<RegisterUnitDto> units,
        int page,
        int totalPages
    ) {
        doReturn(new Page<RegisterUnitDto>().setData(units).setTotalPages(totalPages))
            .when(transportManagerClient)
            .searchRegisterUnits(
                RegisterUnitSearchFilter.builder()
                    .registerId(OUTBOUND_REGISTER_ID)
                    .unitType(UnitType.ITEM)
                    .build(),
                new PageRequest(page, Integer.MAX_VALUE)
            );
    }

    @Nonnull
    public static String outboundId() {
        return outboundId(0);
    }

    @Nonnull
    public static String outboundId(int index) {
        return OUTBOUND_PREFIX + (OUTBOUND_ID + index);
    }
}
