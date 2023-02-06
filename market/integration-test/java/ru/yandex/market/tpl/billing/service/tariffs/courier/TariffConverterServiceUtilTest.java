package ru.yandex.market.tpl.billing.service.tariffs.courier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.CourierShiftCommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.CourierShiftJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.CourierShiftTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.CourierTariffTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.ModelType;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffZoneEnum;
import ru.yandex.market.tpl.billing.model.courier.CourierTariffDto;
import ru.yandex.market.tpl.billing.model.courier.CourierTariffKey;
import ru.yandex.market.tpl.billing.model.courier.CourierTariffType;
import ru.yandex.market.tpl.billing.model.courier.CourierTariffZone;
import ru.yandex.market.tpl.billing.model.entity.enums.CourierShiftType;
import ru.yandex.market.tpl.billing.model.entity.enums.UserType;
import ru.yandex.market.tpl.billing.service.courier.TariffConverterServiceUtil;

/**
 * Тесты для {@link TariffConverterServiceUtil}
 */
public class TariffConverterServiceUtilTest {

    private static final ModelType COURIER = ModelType.THIRD_PARTY_LOGISTICS_COURIER;
    private static final ModelType SELF_EMPLOYED = ModelType.THIRD_PARTY_LOGISTICS_SELF_EMPLOYED_COURIER;

    @Test
    void testConverter() {
        List<CourierTariffDto> expected = getCourierTariffDtoList();
        var actual = TariffConverterServiceUtil.convertExternalTariffsToDto(
                List.of(
                        createTariff(1L, ServiceTypeEnum.COURIER_SHIFT, COURIER),
                        createTariff(2L, ServiceTypeEnum.COURIER_DROPOFF_RETURN, COURIER),
                        //тарифы самозанятых
                        createTariff(3L, ServiceTypeEnum.COURIER_BULKY_CARGO, SELF_EMPLOYED),
                        createTariff(4L, ServiceTypeEnum.COURIER_SMALL_GOODS, SELF_EMPLOYED),
                        createTariff(5L, ServiceTypeEnum.COURIER_WITHDRAW, SELF_EMPLOYED),
                        createTariff(6L, ServiceTypeEnum.COURIER_YANDEX_DRIVE, SELF_EMPLOYED),
                        createTariff(7L, ServiceTypeEnum.COURIER_DROPOFF_RETURN, SELF_EMPLOYED)
                )
        );

        checkCourierShiftServiceType(actual, expected);
        checkCourierDropoffReturnServiceType(actual, expected);
        checkSelfEmployedTariffs(actual, expected);
    }

    private void checkCourierShiftServiceType(
            Map<CourierTariffKey, List<CourierTariffDto>> actual,
            List<CourierTariffDto> expected
    ) {
        Assertions.assertEquals(
                expected.get(0),
                actual.get(new CourierTariffKey(UserType.PARTNER, CourierTariffType.MIN, CourierShiftType.SMALL_GOODS)).get(0)
        );

        Assertions.assertEquals(
                expected.get(1),
                actual.get(new CourierTariffKey(UserType.PARTNER, CourierTariffType.BUSINESS, CourierShiftType.SMALL_GOODS)).get(0)
        );

        Assertions.assertEquals(
                expected.get(2),
                actual.get(new CourierTariffKey(UserType.PARTNER, CourierTariffType.STANDARD, CourierShiftType.SMALL_GOODS)).get(0)
        );

        Assertions.assertEquals(
                expected.get(3),
                actual.get(new CourierTariffKey(UserType.PARTNER, CourierTariffType.LOCKER, CourierShiftType.SMALL_GOODS)).get(0)
        );

        Assertions.assertEquals(
                expected.get(4),
                actual.get(new CourierTariffKey(UserType.PARTNER, CourierTariffType.PVZ, CourierShiftType.SMALL_GOODS)).get(0)
        );

        Assertions.assertEquals(
                expected.get(5),
                actual.get(new CourierTariffKey(UserType.PARTNER, CourierTariffType.LOCKER_ORDER, CourierShiftType.SMALL_GOODS)).get(0)
        );

        Assertions.assertEquals(
                expected.get(6),
                actual.get(new CourierTariffKey(UserType.PARTNER, CourierTariffType.PVZ_ORDER, CourierShiftType.SMALL_GOODS)).get(0)
        );
    }


    private void checkSelfEmployedTariffs(
            Map<CourierTariffKey, List<CourierTariffDto>> actual,
            List<CourierTariffDto> expected
    ) {
        checkTariffsGroup(UserType.SELF_EMPLOYED, CourierShiftType.BULKY_CARGO, actual, expected, 14);
        checkTariffsGroup(UserType.SELF_EMPLOYED, CourierShiftType.SMALL_GOODS, actual, expected, 21);
        checkTariffsGroup(UserType.SELF_EMPLOYED, CourierShiftType.WITHDRAW, actual, expected, 28);
        checkTariffsGroup(UserType.SELF_EMPLOYED, CourierShiftType.YANDEX_DRIVE, actual, expected, 35);
        checkTariffsGroup(UserType.SELF_EMPLOYED, CourierShiftType.DROPOFF_RETURN, actual, expected, 42);
    }

    private void checkTariffsGroup(
            UserType userType,
            CourierShiftType courierShiftType,
            Map<CourierTariffKey, List<CourierTariffDto>> actual,
            List<CourierTariffDto> expected,
            int firstIndex
    ) {
        Assertions.assertEquals(
                expected.get(firstIndex),
                actual.get(new CourierTariffKey(userType, CourierTariffType.MIN, courierShiftType)).get(0)
        );
        Assertions.assertEquals(
                expected.get(firstIndex + 1),
                actual.get(new CourierTariffKey(userType, CourierTariffType.STANDARD, courierShiftType)).get(0)
        );
        Assertions.assertEquals(
                expected.get(firstIndex + 2),
                actual.get(new CourierTariffKey(userType, CourierTariffType.BUSINESS, courierShiftType)).get(0)
        );
        Assertions.assertEquals(
                expected.get(firstIndex + 3),
                actual.get(new CourierTariffKey(userType, CourierTariffType.LOCKER, courierShiftType)).get(0)
        );
        Assertions.assertEquals(
                expected.get(firstIndex + 4),
                actual.get(new CourierTariffKey(userType, CourierTariffType.PVZ, courierShiftType)).get(0)
        );
        Assertions.assertEquals(
                expected.get(firstIndex + 5),
                actual.get(new CourierTariffKey(userType, CourierTariffType.PVZ_ORDER, courierShiftType)).get(0)
        );
        Assertions.assertEquals(
                expected.get(firstIndex + 6),
                actual.get(new CourierTariffKey(userType, CourierTariffType.LOCKER_ORDER, courierShiftType)).get(0)
        );
    }

    private static TariffDTO createTariff(Long id, ServiceTypeEnum serviceType, ModelType modelType) {
        TariffDTO tariff = new TariffDTO();
        tariff.setId(id);
        tariff.setServiceType(serviceType);
        tariff.setModelType(modelType);
        tariff.setDateFrom(LocalDate.of(2021, 1, 1));
        tariff.setPartner(null);
        tariff.setDateTo(null);

        List<Object> metaList;
        switch (serviceType) {
            case COURIER_SHIFT:
                metaList = createCourierShiftMetaList();
                break;
            case COURIER_DROPOFF_RETURN:
            case COURIER_BULKY_CARGO:
            case COURIER_SMALL_GOODS:
            case COURIER_WITHDRAW:
            case COURIER_YANDEX_DRIVE:
                metaList = createCourierCommonMetaList();
                break;
            default:
                throw new IllegalArgumentException("Illegal service type for creation courier tariff");
        }

        tariff.setMeta(metaList);
        return tariff;
    }

    private static List<Object> createCourierShiftMetaList() {
        return List.of(
                getCourierShiftMetaCommonPart()
                        .tariffType(CourierTariffTypeEnum.MIN)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(3000)),
                getCourierShiftMetaCommonPart()
                        .tariffType(CourierTariffTypeEnum.BUSINESS)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(10)),
                getCourierShiftMetaCommonPart()
                        .tariffType(CourierTariffTypeEnum.STANDARD)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(20)),
                getCourierShiftMetaCommonPart()
                        .tariffType(CourierTariffTypeEnum.LOCKER)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(30)),
                getCourierShiftMetaCommonPart()
                        .tariffType(CourierTariffTypeEnum.PVZ)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(40)),
                getCourierShiftMetaCommonPart()
                        .tariffType(CourierTariffTypeEnum.LOCKER_ORDER)
                        .billingUnit(BillingUnitEnum.ORDER)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(50)),
                getCourierShiftMetaCommonPart()
                        .tariffType(CourierTariffTypeEnum.PVZ_ORDER)
                        .billingUnit(BillingUnitEnum.ORDER)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(60))
        );
    }

    private static CourierShiftJsonSchema getCourierShiftMetaCommonPart() {
        return new CourierShiftJsonSchema()
                .fromDistance(0L)
                .toDistance(100L)
                .shiftType(CourierShiftTypeEnum.SMALL_GOODS)
                .tariffZone(TariffZoneEnum.MOSCOW);
    }


    private void checkCourierDropoffReturnServiceType(
            Map<CourierTariffKey, List<CourierTariffDto>> actual,
            List<CourierTariffDto> expected
    ) {
        Assertions.assertEquals(
                expected.get(7),
                actual.get(new CourierTariffKey(UserType.PARTNER, CourierTariffType.MIN, CourierShiftType.DROPOFF_RETURN)).get(0)
        );

        Assertions.assertEquals(
                expected.get(8),
                actual.get(new CourierTariffKey(UserType.PARTNER, CourierTariffType.STANDARD, CourierShiftType.DROPOFF_RETURN)).get(0)
        );

        Assertions.assertEquals(
                expected.get(9),
                actual.get(new CourierTariffKey(UserType.PARTNER, CourierTariffType.BUSINESS, CourierShiftType.DROPOFF_RETURN)).get(0)
        );

        Assertions.assertEquals(
                expected.get(10),
                actual.get(new CourierTariffKey(UserType.PARTNER, CourierTariffType.LOCKER, CourierShiftType.DROPOFF_RETURN)).get(0)
        );

        Assertions.assertEquals(
                expected.get(11),
                actual.get(new CourierTariffKey(UserType.PARTNER, CourierTariffType.PVZ, CourierShiftType.DROPOFF_RETURN)).get(0)
        );

        Assertions.assertEquals(
                expected.get(12),
                actual.get(new CourierTariffKey(UserType.PARTNER, CourierTariffType.PVZ_ORDER, CourierShiftType.DROPOFF_RETURN)).get(0)
        );

        Assertions.assertEquals(
                expected.get(13),
                actual.get(new CourierTariffKey(UserType.PARTNER, CourierTariffType.LOCKER_ORDER, CourierShiftType.DROPOFF_RETURN)).get(0)
        );
    }

    private static List<Object> createCourierCommonMetaList() {
        return List.of(
                getCourierShiftCommonMetaCommonPart()
                        .tariffType(CourierTariffTypeEnum.MIN)
                        .billingUnit(BillingUnitEnum.SHIFT)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(11)),
                getCourierShiftCommonMetaCommonPart()
                        .tariffType(CourierTariffTypeEnum.STANDARD)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(22)),
                getCourierShiftCommonMetaCommonPart()
                        .tariffType(CourierTariffTypeEnum.BUSINESS)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(33)),
                getCourierShiftCommonMetaCommonPart()
                        .tariffType(CourierTariffTypeEnum.LOCKER)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(44)),
                getCourierShiftCommonMetaCommonPart()
                        .tariffType(CourierTariffTypeEnum.PVZ)
                        .billingUnit(BillingUnitEnum.POINT_VISITED)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(55)),
                getCourierShiftCommonMetaCommonPart()
                        .tariffType(CourierTariffTypeEnum.PVZ_ORDER)
                        .billingUnit(BillingUnitEnum.ORDER)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(66)),
                getCourierShiftCommonMetaCommonPart()
                        .tariffType(CourierTariffTypeEnum.LOCKER_ORDER)
                        .billingUnit(BillingUnitEnum.ORDER)
                        .type(CommonJsonSchema.TypeEnum.ABSOLUTE)
                        .currency("RUB")
                        .amount(BigDecimal.valueOf(77))

        );
    }

    private static CourierShiftCommonJsonSchema getCourierShiftCommonMetaCommonPart() {
        return new CourierShiftCommonJsonSchema()
                .fromDistance(0L)
                .toDistance(100L)
                .tariffZone(TariffZoneEnum.MOSCOW);
    }

    private static CourierTariffDto getCourierTariffDto(
            long id,
            UserType userType,
            CourierShiftType courierShiftType,
            CourierTariffType courierTariffType,
            long amount
    ) {
        return CourierTariffDto.builder()
                .id(id)
                .fromDistance(0)
                .toDistance(100)
                .fromDate(LocalDate.of(2021, 1, 1))
                .toDate(LocalDate.MAX)
                .companyId(null)
                .tariffZoneId(CourierTariffZone.MOSCOW)
                .shiftType(courierShiftType)
                .tariffType(courierTariffType)
                .amount(BigDecimal.valueOf(amount))
                .courierUserType(userType)
                .build();
    }

    private static List<CourierTariffDto> getCourierTariffDtoList() {
        return List.of(
                // тарифы для service-type: COURIER_SHIFT
                CourierTariffDto.builder()
                        .id(1L)
                        .fromDistance(0)
                        .toDistance(100)
                        .fromDate(LocalDate.of(2021, 1, 1))
                        .toDate(LocalDate.MAX)
                        .companyId(null)
                        .tariffZoneId(CourierTariffZone.MOSCOW)
                        .shiftType(CourierShiftType.SMALL_GOODS)
                        .tariffType(CourierTariffType.MIN)
                        .amount(BigDecimal.valueOf(3000))
                        .courierUserType(UserType.PARTNER)
                        .build(),
                CourierTariffDto.builder()
                        .id(1L)
                        .fromDistance(0)
                        .toDistance(100)
                        .fromDate(LocalDate.of(2021, 1, 1))
                        .toDate(LocalDate.MAX)
                        .companyId(null)
                        .tariffZoneId(CourierTariffZone.MOSCOW)
                        .shiftType(CourierShiftType.SMALL_GOODS)
                        .tariffType(CourierTariffType.BUSINESS)
                        .amount(BigDecimal.valueOf(10))
                        .courierUserType(UserType.PARTNER)
                        .build(),
                CourierTariffDto.builder()
                        .id(1L)
                        .fromDistance(0)
                        .toDistance(100)
                        .fromDate(LocalDate.of(2021, 1, 1))
                        .toDate(LocalDate.MAX)
                        .companyId(null)
                        .tariffZoneId(CourierTariffZone.MOSCOW)
                        .shiftType(CourierShiftType.SMALL_GOODS)
                        .tariffType(CourierTariffType.STANDARD)
                        .amount(BigDecimal.valueOf(20))
                        .courierUserType(UserType.PARTNER)
                        .build(),
                CourierTariffDto.builder()
                        .id(1L)
                        .fromDistance(0)
                        .toDistance(100)
                        .fromDate(LocalDate.of(2021, 1, 1))
                        .toDate(LocalDate.MAX)
                        .companyId(null)
                        .tariffZoneId(CourierTariffZone.MOSCOW)
                        .shiftType(CourierShiftType.SMALL_GOODS)
                        .tariffType(CourierTariffType.LOCKER)
                        .amount(BigDecimal.valueOf(30))
                        .courierUserType(UserType.PARTNER)
                        .build(),
                CourierTariffDto.builder()
                        .id(1L)
                        .fromDistance(0)
                        .toDistance(100)
                        .fromDate(LocalDate.of(2021, 1, 1))
                        .toDate(LocalDate.MAX)
                        .companyId(null)
                        .tariffZoneId(CourierTariffZone.MOSCOW)
                        .shiftType(CourierShiftType.SMALL_GOODS)
                        .tariffType(CourierTariffType.PVZ)
                        .amount(BigDecimal.valueOf(40))
                        .courierUserType(UserType.PARTNER)
                        .build(),
                CourierTariffDto.builder()
                        .id(1L)
                        .fromDistance(0)
                        .toDistance(100)
                        .fromDate(LocalDate.of(2021, 1, 1))
                        .toDate(LocalDate.MAX)
                        .companyId(null)
                        .tariffZoneId(CourierTariffZone.MOSCOW)
                        .shiftType(CourierShiftType.SMALL_GOODS)
                        .tariffType(CourierTariffType.LOCKER_ORDER)
                        .amount(BigDecimal.valueOf(50))
                        .courierUserType(UserType.PARTNER)
                        .build(),
                CourierTariffDto.builder()
                        .id(1L)
                        .fromDistance(0)
                        .toDistance(100)
                        .fromDate(LocalDate.of(2021, 1, 1))
                        .toDate(LocalDate.MAX)
                        .companyId(null)
                        .tariffZoneId(CourierTariffZone.MOSCOW)
                        .shiftType(CourierShiftType.SMALL_GOODS)
                        .tariffType(CourierTariffType.PVZ_ORDER)
                        .amount(BigDecimal.valueOf(60))
                        .courierUserType(UserType.PARTNER)
                        .build(),
                // тарифы для service-type: COURIER_DROPOFF_RETURN
                CourierTariffDto.builder()
                        .id(2L)
                        .fromDistance(0)
                        .toDistance(100)
                        .fromDate(LocalDate.of(2021, 1, 1))
                        .toDate(LocalDate.MAX)
                        .companyId(null)
                        .tariffZoneId(CourierTariffZone.MOSCOW)
                        .shiftType(CourierShiftType.DROPOFF_RETURN)
                        .tariffType(CourierTariffType.MIN)
                        .amount(BigDecimal.valueOf(11))
                        .courierUserType(UserType.PARTNER)
                        .build(),
                CourierTariffDto.builder()
                        .id(2L)
                        .fromDistance(0)
                        .toDistance(100)
                        .fromDate(LocalDate.of(2021, 1, 1))
                        .toDate(LocalDate.MAX)
                        .companyId(null)
                        .tariffZoneId(CourierTariffZone.MOSCOW)
                        .shiftType(CourierShiftType.DROPOFF_RETURN)
                        .tariffType(CourierTariffType.STANDARD)
                        .amount(BigDecimal.valueOf(22))
                        .courierUserType(UserType.PARTNER)
                        .build(),
                CourierTariffDto.builder()
                        .id(2L)
                        .fromDistance(0)
                        .toDistance(100)
                        .fromDate(LocalDate.of(2021, 1, 1))
                        .toDate(LocalDate.MAX)
                        .companyId(null)
                        .tariffZoneId(CourierTariffZone.MOSCOW)
                        .shiftType(CourierShiftType.DROPOFF_RETURN)
                        .tariffType(CourierTariffType.BUSINESS)
                        .amount(BigDecimal.valueOf(33))
                        .courierUserType(UserType.PARTNER)
                        .build(),
                CourierTariffDto.builder()
                        .id(2L)
                        .fromDistance(0)
                        .toDistance(100)
                        .fromDate(LocalDate.of(2021, 1, 1))
                        .toDate(LocalDate.MAX)
                        .companyId(null)
                        .tariffZoneId(CourierTariffZone.MOSCOW)
                        .shiftType(CourierShiftType.DROPOFF_RETURN)
                        .tariffType(CourierTariffType.LOCKER)
                        .amount(BigDecimal.valueOf(44))
                        .courierUserType(UserType.PARTNER)
                        .build(),
                CourierTariffDto.builder()
                        .id(2L)
                        .fromDistance(0)
                        .toDistance(100)
                        .fromDate(LocalDate.of(2021, 1, 1))
                        .toDate(LocalDate.MAX)
                        .companyId(null)
                        .tariffZoneId(CourierTariffZone.MOSCOW)
                        .shiftType(CourierShiftType.DROPOFF_RETURN)
                        .tariffType(CourierTariffType.PVZ)
                        .amount(BigDecimal.valueOf(55))
                        .courierUserType(UserType.PARTNER)
                        .build(),
                CourierTariffDto.builder()
                        .id(2L)
                        .fromDistance(0)
                        .toDistance(100)
                        .fromDate(LocalDate.of(2021, 1, 1))
                        .toDate(LocalDate.MAX)
                        .companyId(null)
                        .tariffZoneId(CourierTariffZone.MOSCOW)
                        .shiftType(CourierShiftType.DROPOFF_RETURN)
                        .tariffType(CourierTariffType.PVZ_ORDER)
                        .amount(BigDecimal.valueOf(66))
                        .courierUserType(UserType.PARTNER)
                        .build(),
                CourierTariffDto.builder()
                        .id(2L)
                        .fromDistance(0)
                        .toDistance(100)
                        .fromDate(LocalDate.of(2021, 1, 1))
                        .toDate(LocalDate.MAX)
                        .companyId(null)
                        .tariffZoneId(CourierTariffZone.MOSCOW)
                        .shiftType(CourierShiftType.DROPOFF_RETURN)
                        .tariffType(CourierTariffType.LOCKER_ORDER)
                        .amount(BigDecimal.valueOf(77))
                        .courierUserType(UserType.PARTNER)
                        .build(),
                //SELF_EMPLOYED BULKY_CARGO
                getCourierTariffDto(3L, UserType.SELF_EMPLOYED, CourierShiftType.BULKY_CARGO, CourierTariffType.MIN, 11L),
                getCourierTariffDto(3L, UserType.SELF_EMPLOYED, CourierShiftType.BULKY_CARGO, CourierTariffType.STANDARD, 22L),
                getCourierTariffDto(3L, UserType.SELF_EMPLOYED, CourierShiftType.BULKY_CARGO, CourierTariffType.BUSINESS, 33L),
                getCourierTariffDto(3L, UserType.SELF_EMPLOYED, CourierShiftType.BULKY_CARGO, CourierTariffType.LOCKER, 44L),
                getCourierTariffDto(3L, UserType.SELF_EMPLOYED, CourierShiftType.BULKY_CARGO, CourierTariffType.PVZ, 55L),
                getCourierTariffDto(3L, UserType.SELF_EMPLOYED, CourierShiftType.BULKY_CARGO, CourierTariffType.PVZ_ORDER, 66L),
                getCourierTariffDto(3L, UserType.SELF_EMPLOYED, CourierShiftType.BULKY_CARGO, CourierTariffType.LOCKER_ORDER, 77L),
                // SELF_EMPLOYED SMALL_GOODS
                getCourierTariffDto(4L, UserType.SELF_EMPLOYED, CourierShiftType.SMALL_GOODS, CourierTariffType.MIN, 11L),
                getCourierTariffDto(4L, UserType.SELF_EMPLOYED, CourierShiftType.SMALL_GOODS, CourierTariffType.STANDARD, 22L),
                getCourierTariffDto(4L, UserType.SELF_EMPLOYED, CourierShiftType.SMALL_GOODS, CourierTariffType.BUSINESS, 33L),
                getCourierTariffDto(4L, UserType.SELF_EMPLOYED, CourierShiftType.SMALL_GOODS, CourierTariffType.LOCKER, 44L),
                getCourierTariffDto(4L, UserType.SELF_EMPLOYED, CourierShiftType.SMALL_GOODS, CourierTariffType.PVZ, 55L),
                getCourierTariffDto(4L, UserType.SELF_EMPLOYED, CourierShiftType.SMALL_GOODS, CourierTariffType.PVZ_ORDER, 66L),
                getCourierTariffDto(4L, UserType.SELF_EMPLOYED, CourierShiftType.SMALL_GOODS, CourierTariffType.LOCKER_ORDER, 77L),
                //SELF_EMPLOYED WITHDRAW
                getCourierTariffDto(5L, UserType.SELF_EMPLOYED, CourierShiftType.WITHDRAW, CourierTariffType.MIN, 11L),
                getCourierTariffDto(5L, UserType.SELF_EMPLOYED, CourierShiftType.WITHDRAW, CourierTariffType.STANDARD, 22L),
                getCourierTariffDto(5L, UserType.SELF_EMPLOYED, CourierShiftType.WITHDRAW, CourierTariffType.BUSINESS, 33L),
                getCourierTariffDto(5L, UserType.SELF_EMPLOYED, CourierShiftType.WITHDRAW, CourierTariffType.LOCKER, 44L),
                getCourierTariffDto(5L, UserType.SELF_EMPLOYED, CourierShiftType.WITHDRAW, CourierTariffType.PVZ, 55L),
                getCourierTariffDto(5L, UserType.SELF_EMPLOYED, CourierShiftType.WITHDRAW, CourierTariffType.PVZ_ORDER, 66L),
                getCourierTariffDto(5L, UserType.SELF_EMPLOYED, CourierShiftType.WITHDRAW, CourierTariffType.LOCKER_ORDER, 77L),
                //SELF_EMPLOYED YANDEX_DRIVE
                getCourierTariffDto(6L, UserType.SELF_EMPLOYED, CourierShiftType.YANDEX_DRIVE, CourierTariffType.MIN, 11L),
                getCourierTariffDto(6L, UserType.SELF_EMPLOYED, CourierShiftType.YANDEX_DRIVE, CourierTariffType.STANDARD, 22L),
                getCourierTariffDto(6L, UserType.SELF_EMPLOYED, CourierShiftType.YANDEX_DRIVE, CourierTariffType.BUSINESS, 33L),
                getCourierTariffDto(6L, UserType.SELF_EMPLOYED, CourierShiftType.YANDEX_DRIVE, CourierTariffType.LOCKER, 44L),
                getCourierTariffDto(6L, UserType.SELF_EMPLOYED, CourierShiftType.YANDEX_DRIVE, CourierTariffType.PVZ, 55L),
                getCourierTariffDto(6L, UserType.SELF_EMPLOYED, CourierShiftType.YANDEX_DRIVE, CourierTariffType.PVZ_ORDER, 66L),
                getCourierTariffDto(6L, UserType.SELF_EMPLOYED, CourierShiftType.YANDEX_DRIVE, CourierTariffType.LOCKER_ORDER, 77L),
                //SELF_EMPLOYED
                getCourierTariffDto(7L, UserType.SELF_EMPLOYED, CourierShiftType.DROPOFF_RETURN, CourierTariffType.MIN, 11L),
                getCourierTariffDto(7L, UserType.SELF_EMPLOYED, CourierShiftType.DROPOFF_RETURN, CourierTariffType.STANDARD, 22L),
                getCourierTariffDto(7L, UserType.SELF_EMPLOYED, CourierShiftType.DROPOFF_RETURN, CourierTariffType.BUSINESS, 33L),
                getCourierTariffDto(7L, UserType.SELF_EMPLOYED, CourierShiftType.DROPOFF_RETURN, CourierTariffType.LOCKER, 44L),
                getCourierTariffDto(7L, UserType.SELF_EMPLOYED, CourierShiftType.DROPOFF_RETURN, CourierTariffType.PVZ, 55L),
                getCourierTariffDto(7L, UserType.SELF_EMPLOYED, CourierShiftType.DROPOFF_RETURN, CourierTariffType.PVZ_ORDER, 66L),
                getCourierTariffDto(7L, UserType.SELF_EMPLOYED, CourierShiftType.DROPOFF_RETURN, CourierTariffType.LOCKER_ORDER, 77L)
        );
    }
}
