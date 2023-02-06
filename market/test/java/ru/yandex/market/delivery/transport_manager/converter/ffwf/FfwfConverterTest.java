package ru.yandex.market.delivery.transport_manager.converter.ffwf;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.transport_manager.converter.ffwf.dto.TransportationTypeWithSubtypeDto;
import ru.yandex.market.delivery.transport_manager.converter.prefix.IdPrefixConverter;
import ru.yandex.market.delivery.transport_manager.domain.dto.NextReceiver;
import ru.yandex.market.delivery.transport_manager.domain.entity.Korobyte;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TimeSlot;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationLegalInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationScheme;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourier;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourierStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Barcode;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Contractor;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.PartialId;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitCount;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitMeta;
import ru.yandex.market.delivery.transport_manager.domain.enums.BarcodeSource;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.IdType;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSubtype;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.util.TimeUtil;
import ru.yandex.market.ff.client.dto.KorobyteDto;
import ru.yandex.market.ff.client.dto.PutSupplyRequestDTO;
import ru.yandex.market.ff.client.dto.PutWithdrawRequestDTO;
import ru.yandex.market.ff.client.dto.RealSupplierInfoDTO;
import ru.yandex.market.ff.client.dto.RegistryDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitIdDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitInfoDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitMetaDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitPartialIdDTO;
import ru.yandex.market.ff.client.dto.RequestItemDTO;
import ru.yandex.market.ff.client.enums.RegistryFlowType;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.client.enums.StockType;

import static org.assertj.core.api.Assertions.assertThat;

class FfwfConverterTest {
    private static final FfwfConverter FFWF_CONVERTER =
        new FfwfConverter(
            new IdPrefixConverter(),
            new FfwfLegalFormConverter(),
            new KorobyteConverter()
        );
    private static final int XDOC_TRANSPORT_BREAK_BULK_XDOCK_OUTBOUND_SUBTYPE_ID = 1123;
    private static final int XDOC_TRANSPORT_BREAK_BULK_XDOCK_INBOUND_SUBTYPE_ID = 1122;

    @Test
    void convertToSupplyRequestDtoTest() {
        Transportation transportation = getTransportation(TransportationType.INTERWAREHOUSE);
        RealSupplierInfoDTO realSupplierInfoDTO = new RealSupplierInfoDTO();
        realSupplierInfoDTO.setId("1");
        realSupplierInfoDTO.setName("test-supplier");
        PutSupplyRequestDTO putSupplyRequestDTO =
            FFWF_CONVERTER.convertToSupplyRequestDto(
                transportation,
                Set.of(CountType.DEFECT),
                getLegalInfo("mover"),
                getLegalInfo("outbound"),
                new NextReceiver(getLegalInfo("next"), 1001L),
                getCourier(),
                "TMU2",
                Optional.empty(),
                true,
                realSupplierInfoDTO,
                "Зпер123"
            );
        assertThat(putSupplyRequestDTO.getStockType()).isEqualTo(StockType.DEFECT);
        assertThat(putSupplyRequestDTO.getLogisticsPointId())
            .isEqualTo(transportation.getInboundUnit().getLogisticPointId());
        assertThat(putSupplyRequestDTO.getExternalRequestId()).isEqualTo("TMU2");
        assertThat(putSupplyRequestDTO.getWithdrawIds()).isEqualTo(Set.of(11L));
        assertThat(Objects.requireNonNull(putSupplyRequestDTO.getShipper()).getLogisticsPointId()).isEqualTo(10L);
        assertThat(putSupplyRequestDTO.getShipper().getLegalEntity().getLegalName()).isEqualTo("ООО outbound");
        assertThat(putSupplyRequestDTO.getNextReceiver().getLegalEntity().getLegalName()).isEqualTo("ООО next");
        assertThat(putSupplyRequestDTO.getNextReceiver().getLogisticsPointId()).isEqualTo(1001L);
        assertThat(putSupplyRequestDTO.getBookingId()).isEqualTo(1L);
        assertThat(putSupplyRequestDTO.getRealSupplier()).isEqualTo(realSupplierInfoDTO);
        assertThat(putSupplyRequestDTO.getAxaptaMovementRequestId()).isEqualTo("Зпер123");
        assertThat(putSupplyRequestDTO.getTransportationId()).isEqualTo("TM1");
    }

    private Optional<MovementCourier> getCourier() {
        return Optional.of(
            new MovementCourier(
                1L,
                4L,
                "ext-1",
                "Олег",
                "Егоров",
                "Васильевич",
                "Белый форд транзит",
                null,
                "О123НО790",
                null,
                null,
                "+7(903) 012-11-10",
                null,
                null,
                null,
                MovementCourierStatus.SENT,
                MovementCourier.Unit.ALL,
                LocalDateTime.now(),
                LocalDateTime.now()
            )
        );
    }

    private Transportation getTransportation(TransportationType transportationType) {
        return new Transportation()
            .setId(1L)
            .setTransportationType(transportationType)
            .setStatus(TransportationStatus.SCHEDULED)
            .setOutboundUnit(new TransportationUnit()
                .setId(1L)
                .setPartnerId(1L)
                .setStatus(TransportationUnitStatus.ACCEPTED)
                .setType(TransportationUnitType.OUTBOUND)
                .setLogisticPointId(10L)
                .setPlannedIntervalStart(LocalDateTime.of(2020, 7, 10, 12, 0, 0))
                .setPlannedIntervalEnd(LocalDateTime.of(2020, 7, 10, 20, 0, 0))
                .setRequestId(11L)
            )
            .setInboundUnit(new TransportationUnit()
                .setId(2L)
                .setExternalId("ID_AT_PARTNER_01")
                .setStatus(TransportationUnitStatus.ACCEPTED)
                .setType(TransportationUnitType.INBOUND)
                .setPartnerId(2L)
                .setLogisticPointId(20L)
                .setPlannedIntervalStart(LocalDateTime.of(2020, 7, 12, 12, 0, 0))
                .setPlannedIntervalEnd(LocalDateTime.of(2020, 7, 12, 20, 0, 0))
                .setBookedTimeSlot(new TimeSlot()
                    .setId(2L)
                    .setCalendaringServiceId(1L)
                    .setZoneId(TimeUtil.DEFAULT_ZONE_OFFSET.getId())
                )
            )
            .setMovement(new Movement()
                .setId(4L)
                .setPartnerId(2L)
                .setStatus(MovementStatus.NEW)
                .setWeight(94)
                .setVolume(15)
            )
            .setScheme(TransportationScheme.NEW);
    }

    private TransportationLegalInfo getLegalInfo(String name) {
        return new TransportationLegalInfo()
            .setLegalName(name)
            .setLegalType("OOO")
            .setLegalAddress("address")
            .setInn("123")
            .setOgrn("1234")
            .setMarketId(6L)
            .setPartnerId(15L);
    }

    @DisplayName("Mapping всех RegistryUnitIdType в IdType")
    @Test
    void ffwfRegistryUnitIdType() {
        RegistryUnitIdDTO unitId = new RegistryUnitIdDTO();
        unitId.setParts(
            Arrays.stream(RegistryUnitIdType.values())
                .map(x -> {
                    RegistryUnitPartialIdDTO dto = new RegistryUnitPartialIdDTO();
                    dto.setType(x);
                    dto.setValue(x.name());
                    return dto;
                })
                .collect(Collectors.toSet())
        );

        KorobyteDto korobyteDto = new KorobyteDto();
        korobyteDto.setHeight(new BigDecimal("100"));
        korobyteDto.setWidth(new BigDecimal("200"));
        korobyteDto.setLength(new BigDecimal("300"));
        korobyteDto.setWeightGross(new BigDecimal("11.1"));
        korobyteDto.setWeightNet(new BigDecimal("1.2"));
        korobyteDto.setWeightTare(new BigDecimal("1.3"));

        RegistryUnitMetaDTO registryUnitMetaDTO = new RegistryUnitMetaDTO();
        registryUnitMetaDTO.setKorobyte(korobyteDto);

        RegistryUnitInfoDTO unitInfo = new RegistryUnitInfoDTO();
        unitInfo.setUnitId(unitId);
        RegistryUnitDTO registryUnit = new RegistryUnitDTO();
        registryUnit.setUnitInfo(unitInfo);
        registryUnit.setType(RegistryUnitType.BOX);
        registryUnit.setMeta(registryUnitMetaDTO);

        RegisterUnit registerUnit = FFWF_CONVERTER.convertFromRegisterUnitDTO(registryUnit, 0L);

        Korobyte korobyte = registerUnit.getKorobyte();
        Assertions.assertEquals(korobyte.getHeight(), 100);
        Assertions.assertEquals(korobyte.getWidth(), 200);
        Assertions.assertEquals(korobyte.getLength(), 300);
        Assertions.assertEquals(korobyte.getWeightGross(), new BigDecimal("11.1"));
        Assertions.assertEquals(korobyte.getWeightNet(), new BigDecimal("1.2"));
        Assertions.assertEquals(korobyte.getWeightTare(), new BigDecimal("1.3"));

        List<PartialId> partialIds = registerUnit.getPartialIds();

        Assertions.assertEquals(
            Set.of(
                new PartialId().setIdType(IdType.ORDER_ID).setValue("ORDER_ID"),
                new PartialId().setIdType(IdType.STAMP_ID).setValue("STAMP_ID"),
                new PartialId().setIdType(IdType.PALLET_ID).setValue("PALLET_ID"),
                new PartialId().setIdType(IdType.BOX_ID).setValue("BOX_ID"),
                new PartialId().setIdType(IdType.BAG_ID).setValue("BAG_ID"),
                new PartialId().setIdType(IdType.ARTICLE).setValue("SHOP_SKU"),
                new PartialId().setIdType(IdType.VENDOR_ID).setValue("VENDOR_ID"),
                new PartialId().setIdType(IdType.CIS).setValue("CIS"),
                new PartialId().setIdType(IdType.CIS_FULL).setValue("CIS_FULL"),
                new PartialId().setIdType(IdType.CONSIGNMENT_ID).setValue("CONSIGNMENT_ID"),
                new PartialId().setIdType(IdType.UIT).setValue("UIT"),
                new PartialId().setIdType(IdType.IMEI).setValue("IMEI"),
                new PartialId().setIdType(IdType.SERIAL_NUMBER).setValue("SERIAL_NUMBER"),
                new PartialId().setIdType(IdType.ORDER_RETURN_ID).setValue("ORDER_RETURN_ID"),
                new PartialId().setIdType(IdType.ORDER_RETURN_REASON_ID).setValue("ORDER_RETURN_REASON_ID"),
                new PartialId().setIdType(IdType.ASSORTMENT_ARTICLE).setValue("ASSORTMENT_ARTICLE"),
                new PartialId().setIdType(IdType.VIRTUAL_ID).setValue("VIRTUAL_ID")
            ),
            new HashSet<>(partialIds)
        );

        Assertions.assertEquals(
            partialIds.stream().map(PartialId::getIdType).collect(Collectors.toSet()),
            // REAL_VENDOR_ID is not supported by yet
            Arrays.stream(IdType.values()).filter(x -> x != IdType.REAL_VENDOR_ID).collect(Collectors.toSet())
        );
    }

    @DisplayName("Проверяем, что не добавился новый IdType в FFWF, который мы забыли прокинуть в TM")
    @Test
    void unsupportedFfwfRegistryUnitIdTypes() {
        Set<RegistryUnitIdType> mappingToNothing = Set.of(
            RegistryUnitIdType.VIRTUAL_ID,
            RegistryUnitIdType.UNKNOWN
        );

        Assertions.assertTrue(
            Arrays.stream(RegistryUnitIdType.values())
                .filter(v -> !mappingToNothing.contains(v))
                .allMatch(FfwfConverter.FFWF_TO_TM_ID_TYPES::containsKey)
        );
    }

    @DisplayName("Преобразование из item (старый flow FFWF) в RegisterUnit с использованием полей из unit_identifier")
    @Test
    void convertFromItemDTOWithUnitIdentifier() {
        long registerId = 1L;
        String vendorId = "123";
        String barcode = "12345";
        String article = "article";
        String name = "Some product";
        BigDecimal price = BigDecimal.valueOf(1000);
        String realSupplierId = "rs1";

        RequestItemDTO itemDTO = new RequestItemDTO();
        itemDTO.setBarcodes(List.of(barcode));
        itemDTO.setFactCount(10);
        itemDTO.setCount(1);
        itemDTO.setArticle(article);
        itemDTO.setName(name);
        itemDTO.setSupplyPrice(price);
        itemDTO.setLength(BigDecimal.valueOf(10));
        itemDTO.setWidth(BigDecimal.valueOf(4));
        itemDTO.setHeight(BigDecimal.valueOf(3));
        itemDTO.setRealSupplierId(realSupplierId);
        itemDTO.setRealSupplierName("supplier1");
        RegistryUnitIdDTO registryUnitIdDTO = new RegistryUnitIdDTO();
        registryUnitIdDTO.setParts(Set.of(
            new RegistryUnitPartialIdDTO(RegistryUnitIdType.SHOP_SKU, article),
            new RegistryUnitPartialIdDTO(RegistryUnitIdType.ORDER_ID, barcode),
            new RegistryUnitPartialIdDTO(RegistryUnitIdType.VENDOR_ID, vendorId)
        ));
        itemDTO.setUnitId(registryUnitIdDTO);
        itemDTO.setBoxCount(1);

        RegisterUnit expected = new RegisterUnit();
        expected.setRegisterId(registerId);
        expected.setType(UnitType.ITEM);
        expected.setBarcode(barcode);
        expected.setCounts(List.of(new UnitCount().setCountType(CountType.FIT).setQuantity(10)));
        expected.setPartialIds(List.of(
            new PartialId().setIdType(IdType.ARTICLE).setValue(article),
            new PartialId().setIdType(IdType.VENDOR_ID).setValue(vendorId),
            new PartialId().setIdType(IdType.ORDER_ID).setValue(barcode),
            new PartialId().setIdType(IdType.REAL_VENDOR_ID).setValue(realSupplierId)
        ));
        expected.setUnitMeta(
            new UnitMeta()
                .setBarcodes(List.of(new Barcode().setCode(barcode).setSource(BarcodeSource.UNKNOWN)))
                .setName(name)
                .setPrice(price)
                .setBoxCount(1)
                .setContractor(new Contractor().setName("supplier1").setId(realSupplierId))
                .setCargoTypes(List.of())
        );
        expected.setKorobyte(
            new Korobyte()
                .setLength(10)
                .setWidth(4)
                .setHeight(3)
        );

        RegisterUnit actual = FFWF_CONVERTER.convertFromItemDTO(itemDTO, registerId, true, null);
        Assertions.assertEquals(expected, actual);
    }

    @DisplayName("Преобразование из item (старый flow FFWF) в RegisterUnit")
    @Test
    void convertFromItemDTO() {
        testConvertFromItemDto(123L, null, 123L);
        testConvertFromItemDto(123L, 456L, 123L);
        testConvertFromItemDto(null, 123L, 123L);
    }

    private void testConvertFromItemDto(
        Long itemSupplierId,
        Long requestSupplierId,
        Long expectedSupplierId
    ) {
        long registerId = 1L;
        String barcode = "12345";
        Long article = 100L;
        String name = "Some product";
        BigDecimal price = BigDecimal.valueOf(1000);
        String realSupplierId = "rs1";

        RequestItemDTO itemDTO = new RequestItemDTO();
        itemDTO.setBarcodes(List.of(barcode));
        itemDTO.setOrderId(barcode);
        itemDTO.setFactCount(10);
        itemDTO.setCount(1);
        itemDTO.setArticle(Objects.toString(article));
        itemDTO.setSku(article);
        itemDTO.setName(name);
        itemDTO.setSupplyPrice(price);
        itemDTO.setLength(BigDecimal.valueOf(10));
        itemDTO.setWidth(BigDecimal.valueOf(4));
        itemDTO.setHeight(BigDecimal.valueOf(3));
        itemDTO.setSupplierId(itemSupplierId);
        itemDTO.setRealSupplierId(realSupplierId);
        itemDTO.setRealSupplierName("supplier1");
        RegistryUnitIdDTO registryUnitIdDTO = new RegistryUnitIdDTO();
        registryUnitIdDTO.setParts(null);
        itemDTO.setUnitId(registryUnitIdDTO);
        itemDTO.setBoxCount(1);

        RegisterUnit expected = new RegisterUnit();
        expected.setRegisterId(registerId);
        expected.setType(UnitType.ITEM);
        expected.setBarcode(barcode);
        expected.setCounts(List.of(new UnitCount().setCountType(CountType.FIT).setQuantity(10)));
        expected.setPartialIds(List.of(
            new PartialId().setIdType(IdType.ARTICLE).setValue(Objects.toString(article)),
            new PartialId().setIdType(IdType.VENDOR_ID).setValue(Objects.toString(expectedSupplierId)),
            new PartialId().setIdType(IdType.ORDER_ID).setValue(barcode),
            new PartialId().setIdType(IdType.REAL_VENDOR_ID).setValue(realSupplierId)
        ));
        expected.setUnitMeta(
            new UnitMeta()
                .setBarcodes(List.of(new Barcode().setCode(barcode).setSource(BarcodeSource.UNKNOWN)))
                .setName(name)
                .setPrice(price)
                .setBoxCount(1)
                .setContractor(new Contractor().setName("supplier1").setId(realSupplierId))
                .setCargoTypes(List.of())
        );
        expected.setKorobyte(
            new Korobyte()
                .setLength(10)
                .setWidth(4)
                .setHeight(3)
        );

        RegisterUnit actual = FFWF_CONVERTER.convertFromItemDTO(itemDTO, registerId, true, requestSupplierId);
        Assertions.assertEquals(expected, actual);
    }

    @DisplayName("Преобразование из item (старый flow FFWF) в RegisterUnit: нет factCount")
    @Test
    void convertFromItemDTONoFactCount() {
        long registerId = 1L;
        String barcode = "12345";
        String article = "article";
        String name = "Some product";
        BigDecimal price = BigDecimal.valueOf(1000);

        RequestItemDTO itemDTO = new RequestItemDTO();
        itemDTO.setBarcodes(List.of(barcode));
        itemDTO.setCount(10);
        itemDTO.setArticle(article);
        itemDTO.setName(name);
        itemDTO.setSupplyPrice(price);
        itemDTO.setLength(BigDecimal.valueOf(10));
        itemDTO.setWidth(BigDecimal.valueOf(4));
        itemDTO.setHeight(BigDecimal.valueOf(3));
        RegistryUnitIdDTO registryUnitIdDTO = new RegistryUnitIdDTO();
        registryUnitIdDTO.setParts(Set.of(
            new RegistryUnitPartialIdDTO(RegistryUnitIdType.SHOP_SKU, article),
            new RegistryUnitPartialIdDTO(RegistryUnitIdType.ORDER_ID, barcode)
        ));
        itemDTO.setUnitId(registryUnitIdDTO);
        itemDTO.setBoxCount(1);

        RegisterUnit actual = FFWF_CONVERTER.convertFromItemDTO(itemDTO, registerId, false, null);
        Assertions.assertEquals(
            List.of(new UnitCount().setCountType(CountType.FIT).setQuantity(10)),
            actual.getCounts()
        );
    }

    @DisplayName("Проставление externalRequestId в ffwf dto")
    @Test
    void testExternalRequestId() {
        final String axaptaBarcode = "ЗП-1234";
        Transportation transportation = getTransportation(TransportationType.ORDERS_OPERATION);

        PutSupplyRequestDTO putSupplyRequestDTO = FFWF_CONVERTER.convertToSupplyRequestDto(
            transportation,
            Set.of(),
            getLegalInfo("mover"),
            getLegalInfo("outbound"),
            new NextReceiver(getLegalInfo("next"), 1001L),
            getCourier(),
            axaptaBarcode,
            Optional.empty(),
            true,
            null,
            null
        );
        Assertions.assertEquals(putSupplyRequestDTO.getExternalRequestId(), axaptaBarcode);
    }

    @DisplayName("Проставление subtype в ffwf dto")
    @Test
    void testSupplySubtype() {
        Transportation transportation = getTransportation(TransportationType.XDOC_TRANSPORT)
            .setSubtype(TransportationSubtype.BREAK_BULK_XDOCK);

        PutSupplyRequestDTO putSupplyRequestDTO = FFWF_CONVERTER.convertToSupplyRequestDto(
            transportation,
            Set.of(),
            getLegalInfo("mover"),
            getLegalInfo("outbound"),
            new NextReceiver(getLegalInfo("next"), 1001L),
            getCourier(),
            "",
            Optional.empty(),
            true,
            null,
            null
        );
        Assertions.assertEquals(
            XDOC_TRANSPORT_BREAK_BULK_XDOCK_INBOUND_SUBTYPE_ID,
            putSupplyRequestDTO.getType()
        );
    }

    @DisplayName("Выбор поля, которое попадёт в barcode")
    @ParameterizedTest
    @MethodSource("getBarcodeFieldTestCases")
    void ffwfRegistryUnitBarcodeField(IdType idType, String expectedBarcode) {
        RegistryUnitIdDTO unitId = new RegistryUnitIdDTO();
        unitId.setParts(
            Arrays.stream(RegistryUnitIdType.values())
                .filter(x -> x != RegistryUnitIdType.BOX_ID)
                .map(x -> {
                    RegistryUnitPartialIdDTO dto = new RegistryUnitPartialIdDTO();
                    dto.setType(x);
                    dto.setValue(x.name());
                    return dto;
                })
                .collect(Collectors.toSet())
        );

        RegistryUnitInfoDTO unitInfo = new RegistryUnitInfoDTO();
        unitInfo.setUnitId(unitId);
        RegistryUnitDTO registryUnit = new RegistryUnitDTO();
        registryUnit.setUnitInfo(unitInfo);
        registryUnit.setType(RegistryUnitType.BOX);

        RegisterUnit registerUnit = FFWF_CONVERTER.convertFromRegisterUnitDTO(registryUnit, 0L, idType);

        if (expectedBarcode == null) {
            Assertions.assertNull(registerUnit.getBarcode());
        } else {
            Assertions.assertEquals(expectedBarcode, registerUnit.getBarcode());
        }
    }

    static Stream<Arguments> getBarcodeFieldTestCases() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of(IdType.ORDER_ID, "ORDER_ID"),
            Arguments.of(IdType.PALLET_ID, "PALLET_ID"),
            Arguments.of(IdType.BOX_ID, null)
        );
    }

    @Test
    @DisplayName("В качестве даты поставки берется начало забронированного слота")
    void bookedSlotStartAsSupplyDate() {
        Transportation transportation = getTransportation(TransportationType.ORDERS_OPERATION);
        transportation.getInboundUnit().setBookedTimeSlot(
            new TimeSlot()
                .setFromDate(LocalDateTime.parse("2021-04-01T18:05:00"))
                .setToDate(LocalDateTime.parse("2021-04-01T19:15:00"))
                .setZoneId(TimeUtil.DEFAULT_ZONE_OFFSET.getId())
        );
        PutSupplyRequestDTO request = FFWF_CONVERTER.convertToSupplyRequestDto(
            transportation,
            Set.of(),
            getLegalInfo("mover"),
            getLegalInfo("outbound"),
            new NextReceiver(getLegalInfo("next"), 1001L),
            getCourier(),
            "ABC",
            Optional.empty(),
            true,
            null,
            null
        );
        Assertions.assertEquals(
            OffsetDateTime.parse("2021-04-01T18:05:00+03"),
            request.getDate()
        );
    }

    @Test
    @DisplayName("Дата только после заданной для поставки")
    void convertToSupplyRequestDtoWithMinDateTest() {
        Transportation transportation = getTransportation(TransportationType.XDOC_TRANSPORT);
        transportation.getInboundUnit().setBookedTimeSlot(
            new TimeSlot()
                .setFromDate(LocalDateTime.parse("2021-04-01T18:05:00"))
                .setToDate(LocalDateTime.parse("2021-04-01T19:15:00"))
                .setZoneId(TimeUtil.DEFAULT_ZONE_OFFSET.getId())
        );
        PutSupplyRequestDTO request = FFWF_CONVERTER.convertToSupplyRequestDto(
            transportation,
            Set.of(),
            getLegalInfo("mover"),
            getLegalInfo("outbound"),
            new NextReceiver(getLegalInfo("next"), 1001L),
            getCourier(),
            "ABC",
            Optional.of(OffsetDateTime.parse("2021-04-01T20:00:00+03")),
            true,
            null,
            null
        );
        Assertions.assertEquals(
            OffsetDateTime.parse("2021-04-01T20:00:00+03"),
            request.getDate()
        );
    }

    @Test
    @DisplayName("В качестве даты отгрузки берется начало забронированного слота")
    void bookedSlotStartAsWithdrawDate() {
        Transportation transportation = getTransportation(TransportationType.INTERWAREHOUSE);
        transportation.getOutboundUnit().setBookedTimeSlot(
            new TimeSlot()
                .setFromDate(LocalDateTime.parse("2021-04-01T18:05:00"))
                .setToDate(LocalDateTime.parse("2021-04-01T19:15:00"))
                .setZoneId(TimeUtil.DEFAULT_ZONE_OFFSET.getId())
        );
        PutWithdrawRequestDTO request = FFWF_CONVERTER.convertToWithdrawRequestDto(
            transportation,
            null,
            Set.of(CountType.DEFECT),
            getLegalInfo("mover"),
            getLegalInfo("outbound"),
            getCourier(),
            Optional.empty(),
            "TMU1"
        );
        assertThat(request.getStockType()).isEqualTo(StockType.DEFECT);
        Assertions.assertEquals(
            OffsetDateTime.parse("2021-04-01T18:05:00+03"),
            request.getDate()
        );
        assertThat(request.getTransportationId()).isEqualTo("TM1");
    }

    @Test
    @DisplayName("Дата только после заданной для отгрузки")
    void convertToWithdrawRequestDtoWithMinDateTest() {
        Transportation transportation = getTransportation(TransportationType.XDOC_TRANSPORT);
        transportation.getInboundUnit().setBookedTimeSlot(
            new TimeSlot()
                .setFromDate(LocalDateTime.parse("2021-04-01T18:05:00"))
                .setToDate(LocalDateTime.parse("2021-04-01T19:15:00"))
        );
        PutWithdrawRequestDTO request = FFWF_CONVERTER.convertToWithdrawRequestDto(
            transportation,
            null,
            Set.of(),
            getLegalInfo("mover"),
            getLegalInfo("outbound"),
            getCourier(),
            Optional.of(OffsetDateTime.parse("2021-04-01T20:00:00+03")),
            "TMU1"
        );
        Assertions.assertEquals(
            OffsetDateTime.parse("2021-04-01T20:00:00+03"),
            request.getDate()
        );
    }

    @DisplayName("Проставление id родительской поставки в DTO изъятия")
    @Test
    void testWithdrawParentRequestId() {
        Transportation transportation = getTransportation(TransportationType.XDOC_TRANSPORT);
        final long parentFfwfRequestId = 1000L;

        PutWithdrawRequestDTO putWithdrawRequestDTO = FFWF_CONVERTER.convertToWithdrawRequestDto(
            transportation,
            parentFfwfRequestId,
            Set.of(),
            getLegalInfo("mover"),
            getLegalInfo("inbound"),
            getCourier(),
            Optional.empty(),
            "TMU1"
        );
        Assertions.assertEquals(
            parentFfwfRequestId,
            putWithdrawRequestDTO.getSupplyRequestId()
        );
    }

    @DisplayName("Проставление subtype в ffwf dto")
    @Test
    void testWithdrawSubtype() {
        Transportation transportation = getTransportation(TransportationType.XDOC_TRANSPORT)
            .setSubtype(TransportationSubtype.BREAK_BULK_XDOCK);

        PutWithdrawRequestDTO putWithdrawRequestDTO = FFWF_CONVERTER.convertToWithdrawRequestDto(
            transportation,
            null,
            Set.of(),
            getLegalInfo("mover"),
            getLegalInfo("inbound"),
            getCourier(),
            Optional.empty(),
            "TMU1"
        );
        Assertions.assertEquals(
            XDOC_TRANSPORT_BREAK_BULK_XDOCK_OUTBOUND_SUBTYPE_ID,
            putWithdrawRequestDTO.getType()
        );
    }

    @Test
    void testConvertToStockType() {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(
            FFWF_CONVERTER.toStockType(TransportationType.INTERWAREHOUSE, Set.of(CountType.DEFECT))
        )
            .isEqualTo(StockType.DEFECT);
        softly.assertThat(
            FFWF_CONVERTER.toStockType(TransportationType.FULFILLMENT_ASSEMBLAGE, Set.of(CountType.DEFECT))
        )
            .isEqualTo(StockType.DEFECT);
        Sets.complementOf(Set.of(TransportationType.INTERWAREHOUSE, TransportationType.FULFILLMENT_ASSEMBLAGE))
            .forEach(
                transportationType -> softly.assertThat(
                    FFWF_CONVERTER.toStockType(transportationType, Set.of(CountType.DEFECT))
                )
                    .isNull()
            );
        Arrays.stream(TransportationType.values()).forEach(
            transportationType -> softly.assertThat(
                FFWF_CONVERTER.toStockType(transportationType, Set.of())
            )
                .isNull()
        );
        softly.assertThatThrownBy(() ->
            FFWF_CONVERTER.toStockType(
                TransportationType.INTERWAREHOUSE,
                new LinkedHashSet<>(List.of(CountType.FIT, CountType.DEFECT, CountType.EXPIRED))
            )
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Expected one distinct count type for movement, got [FIT, DEFECT, EXPIRED]");
        Sets.complementOf(Set.of(CountType.FIT, CountType.DEFECT, CountType.EXPIRED))
            .forEach(countType -> {
                softly.assertThatThrownBy(() ->
                    FFWF_CONVERTER.toStockType(
                        TransportationType.INTERWAREHOUSE,
                        Set.of(countType)
                    )
                )
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Unexpected count type for movement " + countType);
            });
        softly.assertAll();
    }

    @ParameterizedTest
    @MethodSource("transportationAndUnitTypes")
    void mapType(
        TransportationType transportationType,
        TransportationSubtype transportationSubtype,
        TransportationUnitType transportationUnitType,
        Integer expected
    ) {
        assertThat(expected)
            .isEqualTo(FfwfConverter.mapType(transportationType, transportationSubtype, transportationUnitType));
    }

    @ParameterizedTest
    @MethodSource("transportationsWithoutUnitType")
    void mapTypeInvalidType(
        TransportationType transportationType,
        TransportationSubtype transportationSubtype,
        TransportationUnitType transportationUnitType
    ) {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> FfwfConverter.mapType(transportationType, transportationSubtype, transportationUnitType)
        );
    }

    @Test
    void mapTypeFullCoverage() {
        testSupportedAndTestedTypes(TransportationUnitType.INBOUND, FfwfConverter.supportedInboundTypes());
        testSupportedAndTestedTypes(TransportationUnitType.OUTBOUND, FfwfConverter.supportedOutboundTypes());
    }

    @DisplayName("Негативные тесты конвертера реестра")
    @ParameterizedTest(name = "{argumentsWithNames}")
    @MethodSource("registerToException")
    void convertFromRegistryDTOTest(
        RegistryFlowType registryFlowType,
        OffsetDateTime partnerDate,
        Class<IllegalArgumentException> expectedType
    ) {
        RegistryDTO registryDTO = new RegistryDTO();
        registryDTO.setType(registryFlowType);
        registryDTO.setPartnerDate(partnerDate);
        Assertions.assertThrows(
            expectedType,
            () -> FFWF_CONVERTER.convertFromRegistryDTO(registryDTO, 1L, RegisterStatus.PREPARING)
        );
    }

    private static Stream<Arguments> registerToException() {
        return Stream.of(
            Arguments.of(
                null,
                OffsetDateTime.of(
                    LocalDate.of(2021, 8, 5),
                    LocalTime.of(10, 0, 0),
                    ZoneOffset.ofHours(0)
                ),
                NullPointerException.class
            ),
            Arguments.of(
                RegistryFlowType.FACT_ACCEPTANCE_SECONDARY,
                OffsetDateTime.of(LocalDate.of(2021, 8, 5),
                    LocalTime.of(10, 0, 0),
                    ZoneOffset.ofHours(0)
                ),
                IllegalArgumentException.class
            ),
            Arguments.of(
                RegistryFlowType.FACT_DELIVERED_ORDERS_RETURN,
                null,
                IllegalArgumentException.class
            )
        );
    }

    private void testSupportedAndTestedTypes(
        TransportationUnitType unitType,
        Set<TransportationTypeWithSubtypeDto> supportedTypes
    ) {
        Set<TransportationTypeWithSubtypeDto> testedTypesAndSubtypes = transportationAndUnitTypes()
            .map(Arguments::get)
            .filter(a -> a[2] == unitType)
            .filter(a -> a[3] != null)
            .map(a -> new TransportationTypeWithSubtypeDto((TransportationType) a[0], (TransportationSubtype) a[1]))
            .collect(Collectors.toSet());
        Assertions.assertTrue(testedTypesAndSubtypes.containsAll(supportedTypes));
    }

    static Stream<Arguments> transportationsWithoutUnitType() {
        return Stream.of(
            Arguments.of(
                TransportationType.ANOMALY_LINEHAUL,
                null,
                TransportationUnitType.OUTBOUND
            ),
            Arguments.of(
                TransportationType.SCRAP_LINEHAUL,
                null,
                TransportationUnitType.OUTBOUND
            ),
            Arguments.of(
                TransportationType.XDOC_PARTNER_SUPPLY_TO_DISTRIBUTION_CENTER,
                null,
                TransportationUnitType.OUTBOUND
            ),
            Arguments.of(
                TransportationType.XDOC_PARTNER_SUPPLY_TO_FF,
                null,
                TransportationUnitType.OUTBOUND
            ),
            Arguments.of(
                TransportationType.XDOC_PARTNER_SUPPLY_TO_FF,
                null,
                TransportationUnitType.INBOUND
            ),
            Arguments.of(
                TransportationType.ORDERS_RETURN,
                null,
                TransportationUnitType.INBOUND
            )
        );
    }

    @SuppressWarnings("all")
    static Stream<Arguments> transportationAndUnitTypes() {
        return Stream.of(
            Arguments.of(
                TransportationType.ANOMALY_LINEHAUL,
                null,
                TransportationUnitType.INBOUND,
                24
            ),
            Arguments.of(
                TransportationType.SCRAP_LINEHAUL,
                null,
                TransportationUnitType.INBOUND,
                24
            ),
            Arguments.of(
                TransportationType.XDOC_PARTNER_SUPPLY_TO_DISTRIBUTION_CENTER,
                null,
                TransportationUnitType.INBOUND,
                24
            ),
            Arguments.of(
                TransportationType.XDOC_PARTNER_SUPPLY_TO_FF,
                TransportationSubtype.BREAK_BULK_XDOCK,
                TransportationUnitType.OUTBOUND,
                1117
            ),
            Arguments.of(
                TransportationType.XDOC_TRANSPORT,
                null,
                TransportationUnitType.OUTBOUND,
                23
            ),
            Arguments.of(
                TransportationType.XDOC_TRANSPORT,
                TransportationSubtype.BREAK_BULK_XDOCK,
                TransportationUnitType.OUTBOUND,
                1123
            ),
            Arguments.of(
                TransportationType.XDOC_TRANSPORT,
                null,
                TransportationUnitType.INBOUND,
                22
            ),
            Arguments.of(
                TransportationType.XDOC_TRANSPORT,
                TransportationSubtype.BREAK_BULK_XDOCK,
                TransportationUnitType.INBOUND,
                1122
            ),
            Arguments.of(
                TransportationType.LINEHAUL,
                null,
                TransportationUnitType.OUTBOUND,
                11
            ),
            Arguments.of(
                TransportationType.LINEHAUL,
                TransportationSubtype.MAIN,
                TransportationUnitType.OUTBOUND,
                11
            ),
            Arguments.of(
                TransportationType.LINEHAUL,
                null,
                TransportationUnitType.INBOUND,
                10
            ),
            Arguments.of(
                TransportationType.LINEHAUL,
                TransportationSubtype.SUPPLEMENTARY_1,
                TransportationUnitType.INBOUND,
                10
            ),
            Arguments.of(
                TransportationType.INTERWAREHOUSE,
                null,
                TransportationUnitType.OUTBOUND,
                17
            ),
            Arguments.of(
                TransportationType.INTERWAREHOUSE,
                TransportationSubtype.INTERWAREHOUSE_FIT,
                TransportationUnitType.OUTBOUND,
                1123
            ),
            Arguments.of(
                TransportationType.INTERWAREHOUSE,
                TransportationSubtype.INTERWAREHOUSE_DEFECT,
                TransportationUnitType.OUTBOUND,
                1123
            ),
            Arguments.of(
                TransportationType.INTERWAREHOUSE,
                null,
                TransportationUnitType.INBOUND,
                16
            ),
            Arguments.of(
                TransportationType.ORDERS_OPERATION,
                null,
                TransportationUnitType.OUTBOUND,
                11
            ),
            Arguments.of(
                TransportationType.ORDERS_OPERATION,
                null,
                TransportationUnitType.INBOUND,
                10
            ),
            Arguments.of(
                TransportationType.RETURN_FROM_SC_TO_DROPOFF,
                null,
                TransportationUnitType.OUTBOUND,
                26
            ),
            Arguments.of(
                TransportationType.RETURN_FROM_SC_TO_DROPOFF,
                null,
                TransportationUnitType.INBOUND,
                10
            ),
            Arguments.of(
                TransportationType.ORDERS_RETURN,
                null,
                TransportationUnitType.OUTBOUND,
                20
            ),
            Arguments.of(
                TransportationType.FULFILLMENT_ASSEMBLAGE,
                TransportationSubtype.INTERWAREHOUSE_FIT,
                TransportationUnitType.OUTBOUND,
                17
            ),
            Arguments.of(
                TransportationType.FULFILLMENT_ASSEMBLAGE,
                TransportationSubtype.INTERWAREHOUSE_DEFECT,
                TransportationUnitType.OUTBOUND,
                17
            )
        );
    }
}
