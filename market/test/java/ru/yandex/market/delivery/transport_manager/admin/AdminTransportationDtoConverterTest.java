package ru.yandex.market.delivery.transport_manager.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.delivery.transport_manager.admin.converter.AdminExternalLinkConverter;
import ru.yandex.market.delivery.transport_manager.admin.converter.AdminStatusHistoryDtoConverter;
import ru.yandex.market.delivery.transport_manager.admin.converter.AdminTransportationDtoConverter;
import ru.yandex.market.delivery.transport_manager.admin.converter.TransportationWithMetaFields;
import ru.yandex.market.delivery.transport_manager.admin.dto.DetailBaseTransportationDto;
import ru.yandex.market.delivery.transport_manager.admin.dto.DetailMovementDto;
import ru.yandex.market.delivery.transport_manager.admin.dto.DetailRegisterDto;
import ru.yandex.market.delivery.transport_manager.admin.dto.DetailTimeSlotDto;
import ru.yandex.market.delivery.transport_manager.admin.dto.DetailTransportationDto;
import ru.yandex.market.delivery.transport_manager.admin.dto.DetailXdocTransportationDto;
import ru.yandex.market.delivery.transport_manager.admin.dto.GridMovementDto;
import ru.yandex.market.delivery.transport_manager.admin.dto.GridRegisterDto;
import ru.yandex.market.delivery.transport_manager.admin.dto.GridRegisterItemDownloadableDto;
import ru.yandex.market.delivery.transport_manager.admin.dto.GridRegisterUnitDto;
import ru.yandex.market.delivery.transport_manager.admin.dto.GridTransportationDownloadableDto;
import ru.yandex.market.delivery.transport_manager.admin.dto.GridTransportationDto;
import ru.yandex.market.delivery.transport_manager.admin.dto.GridTransportationUnitDto;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminMovementStatus;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminRegisterRelation;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminRegisterUnitType;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminShipmentType;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTransportationLifecycleState;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTransportationScheme;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTransportationStatus;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTransportationType;
import ru.yandex.market.delivery.transport_manager.config.properties.LmsExtraProperties;
import ru.yandex.market.delivery.transport_manager.config.startrek.StartrekProperties;
import ru.yandex.market.delivery.transport_manager.config.tpl.TplProperties;
import ru.yandex.market.delivery.transport_manager.config.tsum.TsumProperties;
import ru.yandex.market.delivery.transport_manager.config.tsup.TsupProperties;
import ru.yandex.market.delivery.transport_manager.config.yard.YardProperties;
import ru.yandex.market.delivery.transport_manager.converter.prefix.IdPrefixConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.Address;
import ru.yandex.market.delivery.transport_manager.domain.entity.Korobyte;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationLegalInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationScheme;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourier;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourierStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Barcode;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.PartialId;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterMeta;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitCount;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitMeta;
import ru.yandex.market.delivery.transport_manager.domain.enums.BarcodeSource;
import ru.yandex.market.delivery.transport_manager.domain.enums.CargoType;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.IdType;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterRelation;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSubtype;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitOperationType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitSendingStrategy;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterAdminService.EnrichedRegisterUnit;
import ru.yandex.market.logistics.front.library.dto.ExternalReferenceObject;
import ru.yandex.market.logistics.front.library.dto.FormattedTextObject;
import ru.yandex.market.logistics.front.library.dto.ReferenceObject;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.assertj.core.api.Assertions.assertThat;

public class AdminTransportationDtoConverterTest {

    private static final LocalDateTime FIXED_CREATED_UPDATED = LocalDateTime.of(2020, 9, 19, 15, 30, 0);
    private static final LocalDateTime INTERVAL_START = LocalDateTime.of(2020, 9, 19, 16, 30, 0);
    private static final LocalDateTime INTERVAL_END = LocalDateTime.of(2020, 9, 19, 19, 30, 0);
    private static final String LMS_PARTNER_SLUG = "lms/partner";
    private static final String LMS_POINT_SLUG = "lms/logistics-point";
    private static final String LMS_TRANSPORT_SLUG = "lms/partner-transport";
    private static final String LMS_SEGMENT_SLUG = "lms/logistic-segments";
    private static final String TM_TRANSPORTATION_SLUG = "transport-manager/transportations";
    private static final String STARTREK_WEB_URL = "startrek-url";
    private static final String TIME_SLOT_URL_TEMPLATE =
        "https://yard.ru/ui";
    private static final String TSUP_URL = "tsup-url";
    private static final String TSUM_URL = "https://tsum.yandex-team.ru";
    private static final String HOST = "localhost:3000";
    private static final Long TPL_PARTNER_ID = 50000L;
    public static final ZonedDateTime PRICE_DATE =
        ZonedDateTime.of(2021, 9, 1, 12, 0, 0, 0, ZoneId.of("Europe/Moscow"));

    private final AdminTransportationDtoConverter adminTransportationDtoConverter =
        new AdminTransportationDtoConverter(
            new IdPrefixConverter(),
            new AdminExternalLinkConverter(
                new LmsExtraProperties().setAdminUrl(HOST),
                new TplProperties().setVirtualLinehaul(TPL_PARTNER_ID),
                new StartrekProperties().setWebUrl(STARTREK_WEB_URL),
                new YardProperties().setFrontUrl(TIME_SLOT_URL_TEMPLATE),
                new TsupProperties().setHost(TSUP_URL),
                new TsumProperties().setHost(TSUM_URL)
            ),
            new AdminStatusHistoryDtoConverter(
                new AdminExternalLinkConverter(
                    new LmsExtraProperties().setAdminUrl(HOST),
                    new TplProperties().setVirtualLinehaul(TPL_PARTNER_ID),
                    new StartrekProperties().setWebUrl(STARTREK_WEB_URL),
                    new YardProperties().setFrontUrl(TIME_SLOT_URL_TEMPLATE),
                    new TsupProperties().setHost(TSUP_URL),
                    new TsumProperties().setHost(TSUM_URL)
                )
            )
        );

    private static final Address OUTBOUND_ADDRESS = new Address()
        .setLocality("Москва")
        .setStreet("Льва Толстого")
        .setHouse("18").setHousing("Б");
    private static final TransportationPartnerInfo OUTBOUND_PARTNER = new TransportationPartnerInfo()
        .setPartnerId(1L)
        .setPartnerType(PartnerType.DELIVERY)
        .setPartnerName("Partner 1");
    private static final TransportationLegalInfo OUTBOUND_LEGAL = new TransportationLegalInfo()
        .setInn("0123456789")
        .setLegalName("Partner-1")
        .setLegalType("OOO")
        .setUrl("https://partner-1.ru")
        .setOgrn("000000000000")
        .setLegalAddress("Льва Толстого, 16");
    private static final TransportationLegalInfo MOVING_LEGAL = new TransportationLegalInfo()
        .setInn("0000000000")
        .setLegalName("Partner-2")
        .setLegalType("ЗАО")
        .setUrl("https://partner-2.ru")
        .setOgrn("000000000000")
        .setLegalAddress("Ленина, 1");
    private static final TransportationPartnerInfo INBOUND_PARTNER = new TransportationPartnerInfo()
        .setPartnerId(2L)
        .setPartnerType(PartnerType.DELIVERY)
        .setPartnerName("Partner 2");
    private static final TransportationLegalInfo INBOUND_LEGAL = null;
    private static final Address INBOUND_ADDRESS = new Address()
        .setLocality("Москва")
        .setStreet("ул. Логистическая")
        .setHouse("2");
    private static final TransportationPartnerInfo TARGET_PARTNER = new TransportationPartnerInfo()
        .setPartnerId(3L)
        .setPartnerType(PartnerType.FULFILLMENT)
        .setPartnerName("Partner 3");
    private static final String SOME_ERROR = "some error message";
    private static final String ERROR_TICKET = "ERROR-666";

    @Test
    void transportationGrid() {
        GridTransportationDto expected = new GridTransportationDto()
            .setActive(true)
            .setId(1L)
            .setTransportationType(AdminTransportationType.ORDERS_OPERATION)
            .setOutboundPartnerId(new ReferenceObject("1", "1", LMS_PARTNER_SLUG))
            .setOutboundLogisticPointId(new ReferenceObject("10", "10", LMS_POINT_SLUG))
            .setMovingPartnerId(new ReferenceObject("2", "2", LMS_PARTNER_SLUG))
            .setInboundPartnerId(new ReferenceObject("2", "2", LMS_PARTNER_SLUG))
            .setInboundLogisticPointId(new ReferenceObject("20", "20", LMS_POINT_SLUG))
            .setAdminShipmentType(AdminShipmentType.INTAKE)
            .setAdminTransportationStatus(AdminTransportationStatus.SCHEDULED)
            .setCreated(FIXED_CREATED_UPDATED.toLocalDate())
            .setPlanned(LocalDate.of(2020, 7, 10))
            .setAdminTransportationScheme(AdminTransportationScheme.NEW)
            .setTargetPartnerId(new ReferenceObject("3", "3", LMS_PARTNER_SLUG))
            .setTargetLogisticsPointId(new ReferenceObject("103", "103", LMS_POINT_SLUG))
            .setState(AdminTransportationLifecycleState.PREPARED)
            .setMovementSegmentId(new ReferenceObject("1", "1", LMS_SEGMENT_SLUG));

        GridTransportationDto actual =
            adminTransportationDtoConverter.toGridDto(withoutMetaFields(createTransportation()));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void deletedTransportation() {
        GridTransportationDto expected = new GridTransportationDto()
            .setActive(false)
            .setId(1L)
            .setTransportationType(AdminTransportationType.ORDERS_OPERATION)
            .setOutboundPartnerId(new ReferenceObject("1", "1", LMS_PARTNER_SLUG))
            .setOutboundLogisticPointId(new ReferenceObject("10", "10", LMS_POINT_SLUG))
            .setMovingPartnerId(new ReferenceObject("2", "2", LMS_PARTNER_SLUG))
            .setInboundPartnerId(new ReferenceObject("2", "2", LMS_PARTNER_SLUG))
            .setInboundLogisticPointId(new ReferenceObject("20", "20", LMS_POINT_SLUG))
            .setAdminShipmentType(AdminShipmentType.INTAKE)
            .setAdminTransportationStatus(AdminTransportationStatus.SCHEDULED)
            .setCreated(FIXED_CREATED_UPDATED.toLocalDate())
            .setPlanned(LocalDate.of(2020, 7, 10))
            .setAdminTransportationScheme(AdminTransportationScheme.NEW)
            .setTargetPartnerId(new ReferenceObject("3", "3", LMS_PARTNER_SLUG))
            .setTargetLogisticsPointId(new ReferenceObject("103", "103", LMS_POINT_SLUG))
            .setState(AdminTransportationLifecycleState.PREPARED)
            .setMovementSegmentId(new ReferenceObject("1", "1", LMS_SEGMENT_SLUG));

        GridTransportationDto actual =
            adminTransportationDtoConverter.toGridDto(withoutMetaFields(createTransportation().setDeleted(true)));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void transportationEnrichedGrid() {
        GridTransportationDto expected = new GridTransportationDto()
            .setId(1L)
            .setTransportationType(AdminTransportationType.ORDERS_OPERATION)
            .setActive(true)
            .setOutboundPartnerId(new ReferenceObject("1", "Partner 1", LMS_PARTNER_SLUG))
            .setOutboundLogisticPointId(new ReferenceObject("10", "Москва, Льва Толстого, д.18, Б", LMS_POINT_SLUG))
            .setMovingPartnerId(new ReferenceObject("2", "Partner 2", LMS_PARTNER_SLUG))
            .setInboundPartnerId(new ReferenceObject("2", "Partner 2", LMS_PARTNER_SLUG))
            .setInboundLogisticPointId(new ReferenceObject("20", "Москва, ул. Логистическая, д.2", LMS_POINT_SLUG))
            .setAdminShipmentType(AdminShipmentType.INTAKE)
            .setAdminTransportationStatus(AdminTransportationStatus.SCHEDULED)
            .setCreated(FIXED_CREATED_UPDATED.toLocalDate())
            .setPlanned(LocalDate.of(2020, 7, 10))
            .setAdminTransportationScheme(AdminTransportationScheme.NEW)
            .setTargetPartnerId(new ReferenceObject("3", "Partner 3", LMS_PARTNER_SLUG))
            .setTargetLogisticsPointId(new ReferenceObject("103", "103", LMS_POINT_SLUG))
            .setState(AdminTransportationLifecycleState.PREPARED)
            .setMovementSegmentId(new ReferenceObject("1", "1", LMS_SEGMENT_SLUG));

        GridTransportationDto actual =
            adminTransportationDtoConverter.toGridDto(withMetaFields(createTransportation()));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void transportationDownloadableGrid() {
        GridTransportationDto expectedBase = new GridTransportationDto()
            .setId(1L)
            .setTransportationType(AdminTransportationType.ORDERS_OPERATION)
            .setActive(true)
            .setOutboundPartnerId(new ReferenceObject("1", "Partner 1", LMS_PARTNER_SLUG))
            .setOutboundLogisticPointId(new ReferenceObject("10", "Москва, Льва Толстого, д.18, Б", LMS_POINT_SLUG))
            .setMovingPartnerId(new ReferenceObject("2", "Partner 2", LMS_PARTNER_SLUG))
            .setInboundPartnerId(new ReferenceObject("2", "Partner 2", LMS_PARTNER_SLUG))
            .setInboundLogisticPointId(new ReferenceObject("20", "Москва, ул. Логистическая, д.2", LMS_POINT_SLUG))
            .setAdminShipmentType(AdminShipmentType.INTAKE)
            .setAdminTransportationStatus(AdminTransportationStatus.SCHEDULED)
            .setCreated(FIXED_CREATED_UPDATED.toLocalDate())
            .setPlanned(LocalDate.of(2020, 7, 10))
            .setState(AdminTransportationLifecycleState.PREPARED)
            .setAdminTransportationScheme(AdminTransportationScheme.NEW);

        GridTransportationDownloadableDto expected = new GridTransportationDownloadableDto(expectedBase);
        expected.setRawOutboundPartnerId(1L);
        expected.setRawMovingPartnerId(2L);
        expected.setRawInboundPartnerId(2L);
        expected.setOutboundExternalId("PARTNER_OUTBOUND_01");
        expected.setInboundExternalId("PARTNER_INBOUND_02");
        expected.setRawOutboundLogisticsPointId(10L);
        expected.setRawInboundLogisticsPointId(20L);

        GridTransportationDownloadableDto actual = adminTransportationDtoConverter.toExtendedGridDto(withMetaFields(
            createTransportation()));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void transportationOrderDetails() {
        DetailBaseTransportationDto expected = new DetailTransportationDto()
            .setId(1L)
            .setTransportationType(AdminTransportationType.ORDERS_OPERATION.getTitle())
            .setActive(true)
            .setTitle("Перемещение №1")
            .setOutboundPartnerId(new ReferenceObject("1", "Partner 1", LMS_PARTNER_SLUG))
            .setOutboundLogisticPointId(new ReferenceObject("10", "Москва, Льва Толстого, д.18, Б", LMS_POINT_SLUG))
            .setOutboundLegal(
                new FormattedTextObject(
                    "Название: OOO Partner-1\n" +
                        "ИНН: 0123456789\n" +
                        "ОГРН: 000000000000\n" +
                        "Адрес: Льва Толстого, 16\n" +
                        "Сайт: https://partner-1.ru"
                )
            )
            .setMovingPartnerId(new ReferenceObject("2", "Partner 2", LMS_PARTNER_SLUG))
            .setMovingLegal(
                new FormattedTextObject(
                    "Название: ЗАО Partner-2\n" +
                        "ИНН: 0000000000\n" +
                        "ОГРН: 000000000000\n" +
                        "Адрес: Ленина, 1\n" +
                        "Сайт: https://partner-2.ru"
                )
            )
            .setInboundPartnerId(new ReferenceObject("2", "Partner 2", LMS_PARTNER_SLUG))
            .setInboundLogisticPointId(new ReferenceObject("20", "Москва, ул. Логистическая, д.2", LMS_POINT_SLUG))
            .setInboundLegal(new FormattedTextObject("Информация отсутствует"))
            .setAdminShipmentType(AdminShipmentType.INTAKE)
            .setAdminTransportationStatus(AdminTransportationStatus.SCHEDULED)
            .setCreated(FIXED_CREATED_UPDATED)
            .setUpdated(FIXED_CREATED_UPDATED)
            .setAdminTransportationScheme(AdminTransportationScheme.NEW)
            .setValidationErrors("some error message")
            .setTargetPartnerId(new ReferenceObject("3", "Partner 3", LMS_PARTNER_SLUG))
            .setTargetLogisticsPointId(new ReferenceObject("103", "103", LMS_POINT_SLUG))
            .setStartrekTicket(
                new ExternalReferenceObject()
                    .setUrl(STARTREK_WEB_URL + "/" + ERROR_TICKET)
                    .setDisplayName(ERROR_TICKET)
                    .setOpenNewTab(true)
            )
            .setParticipantsFilterReference(
                new ExternalReferenceObject()
                    .setUrl(
                        HOST + "/" + TM_TRANSPORTATION_SLUG +
                            "?page=0&size=20&outboundPartnerId=1&movingPartnerId=2&inboundPartnerId=2"
                    )
                    .setDisplayName("Partner 1 -> Partner 2 -> Partner 2")
                    .setOpenNewTab(true)
            )
            .setMovementSegmentId(new ReferenceObject("1", "1", LMS_SEGMENT_SLUG));

        DetailBaseTransportationDto actual =
            adminTransportationDtoConverter.toDetails(withMetaFields(createTransportation()));
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void transportationDetailsWithNullBookingSlot() {
        DetailBaseTransportationDto expected = new DetailXdocTransportationDto()
            .setId(1L)
            .setTransportationType(AdminTransportationType.XDOC_TRANSPORT.getTitle())
            .setActive(true)
            .setTitle("Перемещение №1")
            .setOutboundPartnerId(new ReferenceObject("1", "Partner 1", LMS_PARTNER_SLUG))
            .setOutboundLogisticPointId(new ReferenceObject("10", "Москва, Льва Толстого, д.18, Б", LMS_POINT_SLUG))
            .setOutboundLegal(
                new FormattedTextObject(
                    "Название: OOO Partner-1\n" +
                        "ИНН: 0123456789\n" +
                        "ОГРН: 000000000000\n" +
                        "Адрес: Льва Толстого, 16\n" +
                        "Сайт: https://partner-1.ru"
                )
            )
            .setMovingPartnerId(new ReferenceObject("2", "Partner 2", LMS_PARTNER_SLUG))
            .setMovingLegal(
                new FormattedTextObject(
                    "Название: ЗАО Partner-2\n" +
                        "ИНН: 0000000000\n" +
                        "ОГРН: 000000000000\n" +
                        "Адрес: Ленина, 1\n" +
                        "Сайт: https://partner-2.ru"
                )
            )
            .setInboundPartnerId(new ReferenceObject("2", "Partner 2", LMS_PARTNER_SLUG))
            .setInboundLogisticPointId(new ReferenceObject("20", "Москва, ул. Логистическая, д.2", LMS_POINT_SLUG))
            .setInboundLegal(new FormattedTextObject("Информация отсутствует"))
            .setAdminShipmentType(AdminShipmentType.INTAKE)
            .setAdminTransportationStatus(AdminTransportationStatus.SCHEDULED)
            .setCreated(FIXED_CREATED_UPDATED)
            .setUpdated(FIXED_CREATED_UPDATED)
            .setAdminTransportationScheme(AdminTransportationScheme.NEW)
            .setValidationErrors("some error message")
            .setTargetPartnerId(new ReferenceObject("3", "Partner 3", LMS_PARTNER_SLUG))
            .setTargetLogisticsPointId(new ReferenceObject("103", "103", LMS_POINT_SLUG))
            .setStartrekTicket(
                new ExternalReferenceObject()
                    .setUrl(STARTREK_WEB_URL + "/" + ERROR_TICKET)
                    .setDisplayName(ERROR_TICKET)
                    .setOpenNewTab(true)
            )
            .setParticipantsFilterReference(
                new ExternalReferenceObject()
                    .setUrl(
                        HOST + "/" + TM_TRANSPORTATION_SLUG +
                            "?page=0&size=20&outboundPartnerId=1&movingPartnerId=2&inboundPartnerId=2"
                    )
                    .setDisplayName("Partner 1 -> Partner 2 -> Partner 2")
                    .setOpenNewTab(true)
            )
            .setMovementSegmentId(new ReferenceObject("1", "1", LMS_SEGMENT_SLUG))
            .setOutboundTimeSlot(new DetailTimeSlotDto()
                .setCalendaringServiceLink(
                    new ExternalReferenceObject(
                        "Здесь могла быть Ваша бронь",
                        "https://yard.ru/ui/delivery-calendar/outbound?date=2020-07-10&warehouseId=1&selectId=null",
                        true
                    )
                )
            );

        DetailBaseTransportationDto actual =
            adminTransportationDtoConverter.toDetails(withMetaFields(createTransportationWithNullBookingSlot()));
        assertThat(actual.getOutboundTimeSlot()).isEqualTo(expected.getOutboundTimeSlot());
    }

    @Test
    void testSubtypeAddedToType() {
        DetailBaseTransportationDto actual =
            adminTransportationDtoConverter.toDetails(withMetaFields(createTransportationWithNullBookingSlot()));
        assertThat(actual.getTransportationType())
            .isEqualTo("Транспорт XDOC с транзитного на целевой склад (BBXD)");
    }

    @Test
    void outboundTransportationUnit() {
        GridTransportationUnitDto expected = new GridTransportationUnitDto()
            .setId(1L)
            .setRequestId("1000")
            .setExternalId("PARTNER_OUTBOUND_01")
            .setIdWithPrefix("TMU1")
            .setLgwFilter(new ExternalReferenceObject(
                "Искать в LGW",
                HOST + "/lgw/client-tasks?page=0&size=10&entityId=1000",
                true
            ))
            .setTrackerFilter(
                new ExternalReferenceObject(
                    "Искать в Трекере",
                    HOST + "/tracker/tracks?page=0&size=10&entityId=1000",
                    true
                )
            )
            .setPartnerId(new ReferenceObject("1", "Partner 1", LMS_PARTNER_SLUG))
            .setPartnerType("DELIVERY")
            .setLogisticPointId(new ReferenceObject("10", "Москва, Льва Толстого, д.18, Б", LMS_POINT_SLUG))
            .setTitle("1 / PARTNER_OUTBOUND_01")
            .setStatus(TransportationUnitStatus.ACCEPTED)
            .setPlannedIntervalStart(LocalDateTime.of(2020, 7, 10, 12, 0, 0))
            .setPlannedIntervalEnd(LocalDateTime.of(2020, 7, 10, 20, 0, 0))
            .setCreated(FIXED_CREATED_UPDATED)
            .setUpdated(FIXED_CREATED_UPDATED)
            .setSendingUnitStrategy(UnitSendingStrategy.VIA_FFWF_TO_LGW);

        GridTransportationUnitDto actual =
            adminTransportationDtoConverter.convert(
                withMetaFields(createTransportation()).getTransportation().getOutboundUnit(),
                OUTBOUND_PARTNER,
                OUTBOUND_ADDRESS
            );

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void inboundTransportationUnit() {
        GridTransportationUnitDto expected = new GridTransportationUnitDto()
            .setId(2L)
            .setRequestId("null")
            .setExternalId("PARTNER_INBOUND_02")
            .setIdWithPrefix("TMU2")
            // с пустым requestId ищем по старой схеме, через idWithPrefix:
            .setLgwFilter(new ExternalReferenceObject(
                "Искать в LGW",
                HOST + "/lgw/client-tasks?page=0&size=10&entityId=TMU2",
                true
            ))
            .setTrackerFilter(
                new ExternalReferenceObject(
                    "Искать в Трекере",
                    HOST + "/tracker/tracks?page=0&size=10&entityId=TMU2",
                    true
                )
            )
            .setPartnerId(new ReferenceObject("2", "Partner 2", LMS_PARTNER_SLUG))
            .setPartnerType("DELIVERY")
            .setLogisticPointId(new ReferenceObject("20", "Москва, ул. Логистическая, д.2", LMS_POINT_SLUG))
            .setTitle("2 / PARTNER_INBOUND_02")
            .setStatus(TransportationUnitStatus.ACCEPTED)
            .setPlannedIntervalStart(LocalDateTime.of(2020, 7, 12, 12, 0, 0))
            .setPlannedIntervalEnd(LocalDateTime.of(2020, 7, 12, 20, 0, 0))
            .setCreated(FIXED_CREATED_UPDATED)
            .setUpdated(FIXED_CREATED_UPDATED)
            .setSendingUnitStrategy(UnitSendingStrategy.DIRECTLY_TO_LGW);

        GridTransportationUnitDto actual =
            adminTransportationDtoConverter.convert(
                withMetaFields(createTransportation()).getTransportation().getInboundUnit(),
                INBOUND_PARTNER,
                INBOUND_ADDRESS
            );
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void movement() {
        GridMovementDto expected = createMovement();

        GridMovementDto actual = adminTransportationDtoConverter.convert(
            withMetaFields(createTransportation()).getTransportation().getMovement(),
            INBOUND_PARTNER
        );
        assertThat(actual).isEqualTo(expected);

    }

    @Test
    void detailedMovement() {
        GridMovementDto basic = createMovement();
        DetailMovementDto expected = new DetailMovementDto(basic);

        expected.setCourierName("Доставкин Иван Вячеславович");
        expected.setCar("-1L, С227НА69");
        expected.setPhone("+79218887766 / +79218887765");
        expected.setTitle("Перевозка №4");
        expected.setPlannedTransport(new ReferenceObject("1", "1", "lms/partner-transport", true));
        expected.setTransport(new ReferenceObject("2", "2", "lms/partner-transport", true));
        expected.setPrice(new BigDecimal("20.00"));
        expected.setPriceDate(PRICE_DATE);
        expected.setTransportation(new ReferenceObject("12", "12", TM_TRANSPORTATION_SLUG, false));

        GridMovementDto actual = adminTransportationDtoConverter.convert(
            withMetaFields(createTransportation()).getTransportation().getMovement(),
            12L,
            INBOUND_PARTNER,
            new MovementCourier(
                1L,
                4L,
                "ext-1",
                "Иван",
                "Доставкин",
                "Вячеславович",
                "фура камаз",
                null,
                "С227НА69",
                null,
                null,
                "+79218887766",
                "+79218887765",
                -1L,
                "-1L",
                MovementCourierStatus.SENT,
                MovementCourier.Unit.ALL,
                FIXED_CREATED_UPDATED,
                FIXED_CREATED_UPDATED
            )
        );
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void tplMovement() {
        GridMovementDto expected = new GridMovementDto()
            .setId(4L)
            .setIdWithPrefix("TMM4")
            .setLgwFilter(
                new ExternalReferenceObject(
                    "Искать в LGW",
                    HOST + "/lgw/client-tasks?page=0&size=10&entityId=TMM4",
                    true
                ))
            .setTrackerFilter(
                new ExternalReferenceObject(
                    "Искать в Трекере",
                    HOST + "/tracker/tracks?page=0&size=10&entityId=TMM4",
                    true
                ))
            .setExternalId(
                new ExternalReferenceObject(
                    "PARTNER_MOVEMENT_04",
                    HOST + "/market-tpl/movements/PARTNER_MOVEMENT_04",
                    true
                ))
            .setPartnerId(new ReferenceObject("50000", "Partner 2", LMS_PARTNER_SLUG))
            .setTransport(new ReferenceObject("1", "1", LMS_TRANSPORT_SLUG))
            .setPlannedTransport(new ReferenceObject("1", "1", LMS_TRANSPORT_SLUG))
            .setPartnerType("DELIVERY")
            .setTitle("4 / PARTNER_MOVEMENT_04")
            .setStatus(AdminMovementStatus.NEW)
            .setPlannedIntervalStart(INTERVAL_START)
            .setPlannedIntervalEnd(INTERVAL_END)
            .setCreated(FIXED_CREATED_UPDATED)
            .setUpdated(FIXED_CREATED_UPDATED)
            .setPlannedTransport(new ReferenceObject("1", "1", "lms/partner-transport", true))
            .setTransport(new ReferenceObject("2", "2", "lms/partner-transport", true))
            .setPrice(new BigDecimal("20.00"));

        Movement movement = withMetaFields(createTransportation()).getTransportation().getMovement();
        movement.setPartnerId(TPL_PARTNER_ID);
        movement.setPlannedTransportId(1L);
        movement.setTransportId(2L);
        GridMovementDto actual = adminTransportationDtoConverter.convert(
            movement,
            INBOUND_PARTNER
        );
        assertThat(actual).isEqualTo(expected);

    }

    @Test
    void registers() {
        GridRegisterDto expected = new GridRegisterDto()
            .setId(1L)
            .setIdWithPrefix("TMR1")
            .setTitle("1 / ext2")
            .setType(RegisterType.PLAN)
            .setStatus(RegisterStatus.PREPARING)
            .setExternalId("ext2")
            .setDocumentId(new ReferenceObject(null, "discarded", null))
            .setPartnerId(new ReferenceObject("1", "Partner 1", LMS_PARTNER_SLUG))
            .setDate(FIXED_CREATED_UPDATED)
            .setCreated(FIXED_CREATED_UPDATED)
            .setComment("comment")
            .setRegisterRelation(AdminRegisterRelation.INBOUND);
        GridRegisterDto actual = adminTransportationDtoConverter.convert(
            createRegister(),
            OUTBOUND_PARTNER,
            new RegisterMeta().setRelation(RegisterRelation.INBOUND)
        );
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void detailedRegister() {
        DetailRegisterDto expected = new DetailRegisterDto()
            .setId(1L)
            .setTitle("Реестр №1 / ext2")
            .setType(RegisterType.PLAN)
            .setStatus(RegisterStatus.PREPARING)
            .setExternalId("ext2")
            .setTransportation(new ReferenceObject("10", "10", TM_TRANSPORTATION_SLUG))
            .setDocumentId(new ReferenceObject(null, "discarded", null))
            .setPartnerId(new ReferenceObject("1", "Partner 1", LMS_PARTNER_SLUG))
            .setDate(FIXED_CREATED_UPDATED)
            .setCreated(FIXED_CREATED_UPDATED)
            .setComment("comment")
            .setRegisterRelation(AdminRegisterRelation.INBOUND);
        DetailRegisterDto actual = adminTransportationDtoConverter.toDetailRegister(
            createRegister(),
            OUTBOUND_PARTNER,
            new RegisterMeta().setRelation(RegisterRelation.INBOUND).setTransportationId(10L)
        );
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void downloadableRegister() {

        GridRegisterItemDownloadableDto expectedPallet = createRegisterUnitStub();
        expectedPallet
            .setRegisterId(1L)
            .setArticle("PALLET 1")
            .setPalletId("PALLET #5")
            .setBarcode("-")
            .setParentUnits("")
            .setItem("Паллета с товарами");

        GridRegisterItemDownloadableDto expectedBox1 = createRegisterUnitStub();
        expectedBox1
            .setRegisterId(1L)
            .setArticle("-")
            .setBarcode("-")
            .setPalletId("BOX #1")
            .setParentUnits("")
            .setItem("Коробка с товарами");

        GridRegisterItemDownloadableDto expectedBox2 = createRegisterUnitStub();
        expectedBox2
            .setRegisterId(1L)
            .setArticle("BOX 1")
            .setPalletId("BOX #3")
            .setBarcode("-")
            .setParentUnits("ITEM #2")
            .setItem("Коробка с товарами");

        GridRegisterItemDownloadableDto expectedUnnamedItem = createRegisterUnitStub();
        expectedUnnamedItem
            .setRegisterId(1L)
            .setArticle("-")
            .setBarcode("-")
            .setPalletId("ITEM #2")
            .setParentUnits("")
            .setItem("Название товара не указано");

        GridRegisterItemDownloadableDto expectedIphone = createRegisterUnitStub();

        expectedIphone
            .setRegisterId(1L)
            .setArticle("art0002")
            .setBarcode("LO1234")
            .setPalletId("ITEM #4")
            .setParentUnits("ITEM #2")
            .setItem("apple iPhone SE3");

        List<GridRegisterItemDownloadableDto> actualRegisterItems = adminTransportationDtoConverter.convertExtended(
            createRegister(),
            OUTBOUND_PARTNER,
            new RegisterMeta().setRelation(RegisterRelation.INBOUND)
        );
        assertThat(actualRegisterItems).isEqualTo(List.of(
            expectedPallet,
            expectedBox1,
            expectedBox2,
            expectedUnnamedItem,
            expectedIphone
        ));
    }

    @Test
    void registerUnit() {
        var expected = new GridRegisterUnitDto()
            .setWeight("30 брутто, 30 нетто, – тара")
            .setDimensions("20 x 15 x 10")
            .setId(1L)
            .setName("Йогурт клубничный обезжиренный")
            .setUnitType(AdminRegisterUnitType.BOX)
            .setBarcode("-")
            .setParentUnits("")
            .setId(5L)
            .setCargotypes("FOOD, COOL_FOOD")
            .setSsku("eto_yogurt")
            .setVendorId("pokupki.market.yandex.ru")
            .setRealVendorId("mr. supplier ltd.")
            .setParentUnits("PALLET: SC_123")
            .setCount("FIT : 1, SURPLUS : 1");
        GridRegisterUnitDto actual = adminTransportationDtoConverter.convert(new EnrichedRegisterUnit(
            createRegisterUnit(5L),
            List.of(new RegisterUnit().setType(UnitType.PALLET).setBarcode("SC_123")))
        );
        assertThat(actual).isEqualTo(expected);
    }

    private GridRegisterItemDownloadableDto createRegisterUnitStub() {
        return (GridRegisterItemDownloadableDto) new GridRegisterItemDownloadableDto()
            .setId(1L)
            .setIdWithPrefix("TMR1")
            .setTitle("1 / ext2")
            .setType(RegisterType.PLAN)
            .setStatus(RegisterStatus.PREPARING)
            .setExternalId("ext2")
            .setDocumentId(new ReferenceObject(null, "discarded", null))
            .setPartnerId(new ReferenceObject("1", "Partner 1", LMS_PARTNER_SLUG))
            .setDate(FIXED_CREATED_UPDATED)
            .setComment("comment")
            .setRegisterRelation(AdminRegisterRelation.INBOUND);
    }

    private RegisterUnit createRegisterUnit(Long id) {
        return new RegisterUnit()
            .setId(id)
            .setParentIds(Set.of(2L))
            .setType(UnitType.BOX)
            .setRegisterId(2L)
            .setPartialIds(
                List.of(
                    new PartialId().setIdType(IdType.BOX_ID).setValue("BOX 1"),
                    new PartialId().setIdType(IdType.VENDOR_ID).setValue("pokupki.market.yandex.ru"),
                    new PartialId().setIdType(IdType.REAL_VENDOR_ID).setValue("mr. supplier ltd."),
                    new PartialId().setIdType(IdType.ARTICLE).setValue("eto_yogurt")
                ))
            .setUnitMeta(new UnitMeta().setName("Йогурт клубничный обезжиренный")
                .setCargoTypes(List.of(CargoType.FOOD, CargoType.COOL_FOOD)))
            .setCounts(List.of(
                new UnitCount().setQuantity(1).setCountType(CountType.FIT),
                new UnitCount().setQuantity(1).setCountType(CountType.SURPLUS)
            ))
            .setKorobyte(
                new Korobyte()
                    .setHeight(10)
                    .setWidth(15)
                    .setLength(20)
                    .setWeightGross(BigDecimal.valueOf(30))
                    .setWeightNet(BigDecimal.valueOf(30))
                    .setWeightTare(null))
            .setDescription("some info");
    }

    private RegisterUnit createFullRegisterUnit(Long id) {
        return createRegisterUnit(id)
            .setType(UnitType.ITEM)
            .setUnitMeta(new UnitMeta().setBoxCount(2)
                .setBarcodes(
                    List.of(new Barcode().setCode("999999").setSource(BarcodeSource.SUPPLIER))
                )
                .setPrice(BigDecimal.valueOf(1000))
                .setRemovableIfAbsent(true)
                .setUnitOperationType(UnitOperationType.FULFILLMENT)
                .setContractor(null)
                .setName("apple iPhone SE3")
            )
            .setCounts(List.of(
                new UnitCount().setCountType(CountType.FIT).setQuantity(1),
                new UnitCount().setCountType(CountType.DEFECT).setQuantity(1)
            ))
            .setPartialIds(
                List.of(
                    new PartialId().setValue("444").setIdType(IdType.VENDOR_ID),
                    new PartialId().setValue("LO1234").setIdType(IdType.ORDER_ID),
                    new PartialId().setValue("art0002").setIdType(IdType.ARTICLE)
                )
            );
    }

    private GridMovementDto createMovement() {
        return new GridMovementDto()
            .setId(4L)
            .setIdWithPrefix("TMM4")
            .setLgwFilter(
                new ExternalReferenceObject(
                    "Искать в LGW",
                    HOST + "/lgw/client-tasks?page=0&size=10&entityId=TMM4",
                    true
                ))
            .setTrackerFilter(
                new ExternalReferenceObject(
                    "Искать в Трекере",
                    HOST + "/tracker/tracks?page=0&size=10&entityId=TMM4",
                    true
                )
            )
            .setExternalId(
                new ExternalReferenceObject(
                    "PARTNER_MOVEMENT_04",
                    "#",
                    false
                ))
            .setPartnerId(new ReferenceObject("2", "Partner 2", LMS_PARTNER_SLUG))
            .setTransport(new ReferenceObject(
                "2",
                "2",
                LMS_TRANSPORT_SLUG,
                true
            ))
            .setPlannedTransport(new ReferenceObject(
                "1",
                "1",
                LMS_TRANSPORT_SLUG,
                true
            ))
            .setPrice(new BigDecimal("20.00"))
            .setPartnerType("DELIVERY")
            .setTitle("4 / PARTNER_MOVEMENT_04")
            .setStatus(AdminMovementStatus.NEW)
            .setPlannedIntervalStart(INTERVAL_START)
            .setPlannedIntervalEnd(INTERVAL_END)
            .setCreated(FIXED_CREATED_UPDATED)
            .setUpdated(FIXED_CREATED_UPDATED);
    }

    private TransportationWithMetaFields withMetaFields(Transportation transportation) {
        return new TransportationWithMetaFields(
            transportation,
            OUTBOUND_PARTNER,
            OUTBOUND_ADDRESS,
            OUTBOUND_LEGAL,
            INBOUND_PARTNER,
            MOVING_LEGAL,
            INBOUND_PARTNER,
            INBOUND_ADDRESS,
            INBOUND_LEGAL,
            SOME_ERROR,
            TARGET_PARTNER,
            List.of(),
            List.of(),
            List.of(),
            ERROR_TICKET
        );
    }

    private TransportationWithMetaFields withoutMetaFields(Transportation transportation) {
        return new TransportationWithMetaFields(
            transportation,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    private Transportation createTransportation() {
        return new Transportation()
            .setId(1L)
            .setTransportationType(TransportationType.ORDERS_OPERATION)
            .setStatus(TransportationStatus.SCHEDULED)
            .setCreated(FIXED_CREATED_UPDATED)
            .setUpdated(FIXED_CREATED_UPDATED)
            .setOutboundUnit(new TransportationUnit()
                .setId(1L)
                .setExternalId("PARTNER_OUTBOUND_01")
                .setPartnerId(1L)
                .setStatus(TransportationUnitStatus.ACCEPTED)
                .setType(TransportationUnitType.OUTBOUND)
                .setLogisticPointId(10L)
                .setPlannedIntervalStart(LocalDateTime.of(2020, 7, 10, 12, 0, 0))
                .setPlannedIntervalEnd(LocalDateTime.of(2020, 7, 10, 20, 0, 0))
                .setRequestId(1000L)
                .setCreated(FIXED_CREATED_UPDATED)
                .setUpdated(FIXED_CREATED_UPDATED)
                .setBookedTimeSlot(null)
                .setSendingStrategy(UnitSendingStrategy.VIA_FFWF_TO_LGW)
            )
            .setInboundUnit(new TransportationUnit()
                .setId(2L)
                .setExternalId("PARTNER_INBOUND_02")
                .setStatus(TransportationUnitStatus.ACCEPTED)
                .setType(TransportationUnitType.INBOUND)
                .setPartnerId(2L)
                .setLogisticPointId(20L)
                .setPlannedIntervalStart(LocalDateTime.of(2020, 7, 12, 12, 0, 0))
                .setPlannedIntervalEnd(LocalDateTime.of(2020, 7, 12, 20, 0, 0))
                .setRequestId(null)
                .setCreated(FIXED_CREATED_UPDATED)
                .setUpdated(FIXED_CREATED_UPDATED)
                .setSendingStrategy(UnitSendingStrategy.DIRECTLY_TO_LGW)
            )
            .setMovement(new Movement()
                .setId(4L)
                .setExternalId("PARTNER_MOVEMENT_04")
                .setPartnerId(2L)
                .setStatus(MovementStatus.NEW)
                .setWeight(94)
                .setVolume(15)
                .setPlannedIntervalStart(INTERVAL_START)
                .setPlannedIntervalEnd(INTERVAL_END)
                .setCreated(FIXED_CREATED_UPDATED)
                .setUpdated(FIXED_CREATED_UPDATED)
                .setPlannedTransportId(1L)
                .setTransportId(2L)
                .setPrice(2_000L)
                .setPriceDate(PRICE_DATE)
            )
            .setScheme(TransportationScheme.NEW)
            .setTargetPartnerId(3L)
            .setTargetLogisticsPointId(103L)
            .setMovementSegmentId(1L);
    }

    private Transportation createTransportationWithNullBookingSlot() {
        return new Transportation()
            .setId(1L)
            .setTransportationType(TransportationType.XDOC_TRANSPORT)
            .setSubtype(TransportationSubtype.BREAK_BULK_XDOCK)
            .setStatus(TransportationStatus.SCHEDULED)
            .setCreated(FIXED_CREATED_UPDATED)
            .setUpdated(FIXED_CREATED_UPDATED)
            .setOutboundUnit(new TransportationUnit()
                .setId(1L)
                .setExternalId("PARTNER_OUTBOUND_01")
                .setPartnerId(1L)
                .setStatus(TransportationUnitStatus.ACCEPTED)
                .setType(TransportationUnitType.OUTBOUND)
                .setLogisticPointId(10L)
                .setPlannedIntervalStart(LocalDateTime.of(2020, 7, 10, 12, 0, 0))
                .setPlannedIntervalEnd(LocalDateTime.of(2020, 7, 10, 20, 0, 0))
                .setRequestId(1000L)
                .setCreated(FIXED_CREATED_UPDATED)
                .setUpdated(FIXED_CREATED_UPDATED)
                .setBookedTimeSlot(null)
            )
            .setInboundUnit(new TransportationUnit()
                .setId(2L)
                .setExternalId("PARTNER_INBOUND_02")
                .setStatus(TransportationUnitStatus.ACCEPTED)
                .setType(TransportationUnitType.INBOUND)
                .setPartnerId(2L)
                .setLogisticPointId(20L)
                .setPlannedIntervalStart(LocalDateTime.of(2020, 7, 12, 12, 0, 0))
                .setPlannedIntervalEnd(LocalDateTime.of(2020, 7, 12, 20, 0, 0))
                .setRequestId(null)
                .setCreated(FIXED_CREATED_UPDATED)
                .setUpdated(FIXED_CREATED_UPDATED)
            )
            .setMovement(new Movement()
                .setId(4L)
                .setExternalId("PARTNER_MOVEMENT_04")
                .setPartnerId(2L)
                .setStatus(MovementStatus.NEW)
                .setWeight(94)
                .setVolume(15)
                .setPlannedIntervalStart(INTERVAL_START)
                .setPlannedIntervalEnd(INTERVAL_END)
                .setCreated(FIXED_CREATED_UPDATED)
                .setUpdated(FIXED_CREATED_UPDATED)
                .setPlannedTransportId(1L)
                .setTransportId(2L)
                .setPrice(2_000L)
                .setPriceDate(PRICE_DATE)
            )
            .setScheme(TransportationScheme.NEW)
            .setTargetPartnerId(3L)
            .setTargetLogisticsPointId(103L)
            .setMovementSegmentId(1L);
    }

    private Register createRegister() {
        return new Register()
            .setId(1L)
            .setType(RegisterType.PLAN)
            .setStatus(RegisterStatus.PREPARING)
            .setExternalId("ext2")
            .setDocumentId("discarded")
            .setPartnerId(1L)
            .setDate(FIXED_CREATED_UPDATED.toInstant(DateTimeUtils.MOSCOW_ZONE.getRules()
                .getOffset(FIXED_CREATED_UPDATED)))
            .setComment("comment")
            .setPallets(List.of(
                    new RegisterUnit().setType(UnitType.PALLET).setId(5L).setPartialIds(
                        List.of(new PartialId().setIdType(IdType.PALLET_ID).setValue("PALLET 1")))
                )
            )
            .setBoxes(List.of(new RegisterUnit().setType(UnitType.BOX).setId(1L), createRegisterUnit(3L)))
            .setItems(List.of(new RegisterUnit().setType(UnitType.ITEM).setId(2L), createFullRegisterUnit(4L)))
            .setCreated(FIXED_CREATED_UPDATED)
            .setUpdated(FIXED_CREATED_UPDATED);
    }
}
