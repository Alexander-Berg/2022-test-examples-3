package ru.yandex.market.core.offer.mapping;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.OptionalInt;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mdm.http.MdmCommon;

import static ru.yandex.market.mdm.http.MasterDataProto.MasterDataInfo;
import static ru.yandex.market.mdm.http.MasterDataProto.ProviderProductMasterData;
import static ru.yandex.market.mdm.http.MasterDataProto.ProviderProductSupplyEvent;
import static ru.yandex.market.mdm.http.MasterDataProto.ProviderProductSupplySchedule;

@ParametersAreNonnullByDefault
class ToMasterDataMappedOfferConversionsTest extends FunctionalTest {

    private static final TimePeriodWithUnits DAYS_10 = TimePeriodWithUnits.ofDays(10);
    private static final TimePeriodWithUnits DAYS_180 = TimePeriodWithUnits.ofDays(180);
    private static final TimePeriodWithUnits DAYS_365 = TimePeriodWithUnits.ofDays(365);
    private static final MdmCommon.TimeInUnits UNLIMITED_FROM_MBOC = MdmCommon.TimeInUnits.newBuilder()
            .setValue(1)
            .setUnit(MdmCommon.TimeUnit.UNLIMITED)
            .build();

    @Autowired
    private OfferConversionService offerConversionService;

    @Test
    void testEmptyMasterDataFromMboc() {
        MasterDataInfo source = MasterDataInfo.newBuilder().build();
        Optional<MasterData> converted = offerConversionService.toMasterData(source);
        MatcherAssert.assertThat(converted, Matchers.is(Optional.empty()));
    }

    @Test
    void testSomeMasterDataFromMboc() {
        MasterDataInfo source = MasterDataInfo.newBuilder()
                .setShelfLifeWithUnits(DAYS_10.getTime())
                .setShelfLifeComment("ShelfLifeComment")
                .setProviderProductMasterData(ProviderProductMasterData.newBuilder()
                        .addManufacturerCountry("Россия")
                        .setMinShipment(100)
                        .addVetisGuid("123456789-987654321-123456789-987654321")
                        .build())
                .build();
        Optional<MasterData> converted = offerConversionService.toMasterData(source);
        MatcherAssert.assertThat(converted, MbiMatchers.isPresent(
                MbiMatchers.<MasterData>newAllOfBuilder()
                        .add(MasterData::minShipment, MbiMatchers.isIntPresent(100))
                        .add(MasterData::shelfLife, MbiMatchers.isPresent(DAYS_10))
                        .add(MasterData::shelfLifeComment, MbiMatchers.isPresent("ShelfLifeComment"))
                        .add(MasterData::manufacturer, Optional.empty())
                        .add(MasterData::manufacturerCountries, Collections.singletonList("Россия"))
                        .add(MasterData::customsCommodityCode, Optional.empty())
                        .add(MasterData::deliveryDuration, Optional.empty())
                        .add(MasterData::guaranteePeriod, Optional.empty())
                        .add(MasterData::lifeTime, Optional.empty())
                        .add(MasterData::quantumOfSupply, OptionalInt.empty())
                        .add(MasterData::boxCount, OptionalInt.empty())
                        .add(MasterData::supplySchedule, Collections.emptySet())
                        .add(MasterData::transportUnitSize, OptionalInt.empty())
                        .build())
        );
    }

    @Test
    void testFullMasterDataFromMboc() {
        MasterDataInfo source = MasterDataInfo.newBuilder()
                .setShelfLifeWithUnits(DAYS_10.getTime())
                .setShelfLifeComment("ShelfLifeComment")
                .setGuaranteePeriodWithUnits(DAYS_180.getTime())
                .setGuaranteePeriodComment("GuaranteePeriodComment")
                .setLifeTimeWithUnits(DAYS_365.getTime())
                .setLifeTimeComment("LifeTimeComment")
                .setProviderProductMasterData(ProviderProductMasterData.newBuilder()
                        .setCustomsCommodityCode("HG405235")
                        .setMinShipment(100)
                        .setDeliveryTime(3)
                        .setManufacturer("ОАО Ромашка")
                        .addManufacturerCountry("Россия")
                        .addManufacturerCountry("Казахстан")
                        .setQuantumOfSupply(20)
                        .setBoxCount(2)
                        .setUseInMercury(true)
                        .addVetisGuid("746352810-345712312-3467745-23478711")
                        .setWeightDimensionsInfo(
                                MdmCommon.WeightDimensionsInfo.newBuilder()
                                        .setBoxLengthUm(120_000)
                                        .setBoxHeightUm(55_000)
                                        .setBoxWidthUm(7_800_000)
                                        .setWeightGrossMg(95_000_000)
                                        .setWeightNetMg(120_000_000)
                                        .build()
                        )
                        .setTransportUnitSize(10)
                        .setSupplySchedule(ProviderProductSupplySchedule.newBuilder()
                                .addSupplyEvent(ProviderProductSupplyEvent.newBuilder()
                                        .setDayOfWeek(2)
                                        .build())
                                .addSupplyEvent(ProviderProductSupplyEvent.newBuilder()
                                        .setDayOfWeek(5)
                                        .build())
                                .build())
                        .build())
                .build();
        Optional<MasterData> converted = offerConversionService.toMasterData(source);
        MatcherAssert.assertThat(converted, MbiMatchers.isPresent(
                MbiMatchers.<MasterData>newAllOfBuilder()
                        .add(MasterData::minShipment, MbiMatchers.isIntPresent(100))
                        .add(MasterData::shelfLife, MbiMatchers.isPresent(DAYS_10))
                        .add(MasterData::shelfLifeComment, MbiMatchers.isPresent("ShelfLifeComment"))
                        .add(MasterData::manufacturer, MbiMatchers.isPresent("ОАО Ромашка"))
                        .add(MasterData::manufacturerCountries, Arrays.asList("Россия", "Казахстан"))
                        .add(MasterData::customsCommodityCode, MbiMatchers.isPresent("HG405235"))
                        .add(MasterData::deliveryDuration, MbiMatchers.isPresent(Duration.ofDays(3)))
                        .add(MasterData::guaranteePeriod, MbiMatchers.isPresent(DAYS_180))
                        .add(MasterData::useInMercury, MbiMatchers.isPresent(true))
                        .add(MasterData::guaranteePeriodComment, MbiMatchers.isPresent("GuaranteePeriodComment"))
                        .add(MasterData::lifeTime, MbiMatchers.isPresent(DAYS_365))
                        .add(MasterData::lifeTimeComment, MbiMatchers.isPresent("LifeTimeComment"))
                        .add(MasterData::quantumOfSupply, MbiMatchers.isIntPresent(20))
                        .add(MasterData::boxCount, MbiMatchers.isIntPresent(2))
                        .add(MasterData::weightDimensions, MbiMatchers.isPresent(
                                new WeightDimensions.Builder()
                                        .setLength(120_000L)
                                        .setHeight(55_000L)
                                        .setWidth(7_800_000L)
                                        .setWeight(95_000_000L)
                                        .setWeightNet(120_000_000L)
                                        .build()))
                        .add(MasterData::supplySchedule, EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY))
                        .add(MasterData::transportUnitSize, MbiMatchers.isIntPresent(10))
                        .build()
        ));
    }

    @Test
    void testEmptyMasterDataToMboc() {
        MasterData source = new MasterData.Builder().build();
        MasterDataInfo converted = MappedOfferConversions.toMasterDataInfo(source);
        MatcherAssert.assertThat(converted, MbiMatchers.<MasterDataInfo>newAllOfBuilder()
                .add(MasterDataInfo::hasShelfLifeWithUnits, false)
                .add(MasterDataInfo::hasGuaranteePeriodWithUnits, false)
                .add(MasterDataInfo::hasLifeTimeWithUnits, false)
                .add(
                        MasterDataInfo::getProviderProductMasterData,
                        MbiMatchers.<ProviderProductMasterData>newAllOfBuilder()
                                .add(ProviderProductMasterData::hasManufacturer, false)
                                .add(ProviderProductMasterData::getManufacturerCountryCount, 0)
                                .add(ProviderProductMasterData::hasCustomsCommodityCode, false)
                                .add(ProviderProductMasterData::hasDeliveryTime, false)
                                .add(ProviderProductMasterData::hasQuantumOfSupply, false)
                                .add(ProviderProductMasterData::hasBoxCount, false)
                                .add(ProviderProductMasterData::hasSupplySchedule, false)
                                .add(ProviderProductMasterData::hasTransportUnitSize, false)
                                .add(ProviderProductMasterData::hasMinShipment, false)
                                .add(ProviderProductMasterData::hasWeightDimensionsInfo, false)
                                .build()
                )
                .build());
    }

    @Test
    void testSomeMasterDataToMboc() {
        MasterData source = new MasterData.Builder()
                .setShelfLife(DAYS_10)
                .setShelfLifeComment("ShelfLifeComment")
                .addManufacturerCountries(Collections.singleton("Россия"))
                .setMinShipment(100)
                .build();
        MasterDataInfo converted = MappedOfferConversions.toMasterDataInfo(source);
        MatcherAssert.assertThat(converted, MbiMatchers.<MasterDataInfo>newAllOfBuilder()
                .add(MasterDataInfo::hasShelfLifeWithUnits, true)
                .add(MasterDataInfo::getShelfLifeWithUnits, DAYS_10.getTime())
                .add(MasterDataInfo::getShelfLifeComment, "ShelfLifeComment")
                .add(MasterDataInfo::hasGuaranteePeriodWithUnits, false)
                .add(MasterDataInfo::hasLifeTimeWithUnits, false)
                .add(
                        MasterDataInfo::getProviderProductMasterData,
                        MbiMatchers.<ProviderProductMasterData>newAllOfBuilder()
                                .add(ProviderProductMasterData::hasManufacturer, false)
                                .add(ProviderProductMasterData::getManufacturerCountryCount, 1)
                                .add(
                                        ProviderProductMasterData::getManufacturerCountryList,
                                        Collections.singletonList("Россия")
                                )
                                .add(ProviderProductMasterData::hasCustomsCommodityCode, false)
                                .add(ProviderProductMasterData::hasDeliveryTime, false)
                                .add(ProviderProductMasterData::hasQuantumOfSupply, false)
                                .add(ProviderProductMasterData::hasBoxCount, false)
                                .add(ProviderProductMasterData::hasSupplySchedule, false)
                                .add(ProviderProductMasterData::hasTransportUnitSize, false)
                                .build()
                )
                .build());
    }

    @Test
    void testFullMasterDataToMboc() {
        MasterData source = new MasterData.Builder()
                .setShelfLife(DAYS_10)
                .setShelfLifeComment("ShelfLifeComment")
                .setGuaranteePeriod(DAYS_180)
                .setGuaranteePeriodComment("GuaranteePeriodComment")
                .setLifeTime(DAYS_365)
                .setLifeTimeComment("LifeTimeComment")
                .setCustomsCommodityCode("HG405235")
                .setMinShipment(100)
                .setDeliveryDuration(Duration.ofDays(3))
                .setManufacturer("ОАО Ромашка")
                .addManufacturerCountries(Arrays.asList("Россия", "Казахстан"))
                .setQuantumOfSupply(20)
                .setTransportUnitSize(10)
                .setBoxCount(2)
                .setWeightDimensions(
                        new WeightDimensions.Builder()
                                .setHeight(55_000L)
                                .setWidth(7_800_000L)
                                .setLength(120_000L)
                                .setWeight(95_000_000L)
                                .build()
                )
                .addSupplyScheduleDays(EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY))
                .build();
        MasterDataInfo converted = MappedOfferConversions.toMasterDataInfo(source);
        MatcherAssert.assertThat(converted, MbiMatchers.<MasterDataInfo>newAllOfBuilder()
                .add(MasterDataInfo::hasShelfLifeWithUnits, true)
                .add(MasterDataInfo::getShelfLifeWithUnits, DAYS_10.getTime())
                .add(MasterDataInfo::getShelfLifeComment, "ShelfLifeComment")
                .add(MasterDataInfo::hasGuaranteePeriodWithUnits, true)
                .add(MasterDataInfo::getGuaranteePeriodWithUnits, DAYS_180.getTime())
                .add(MasterDataInfo::getGuaranteePeriodComment, "GuaranteePeriodComment")
                .add(MasterDataInfo::hasLifeTimeWithUnits, true)
                .add(MasterDataInfo::getLifeTimeWithUnits, DAYS_365.getTime())
                .add(MasterDataInfo::getLifeTimeComment, "LifeTimeComment")
                .add(
                        MasterDataInfo::getProviderProductMasterData,
                        MbiMatchers.<ProviderProductMasterData>newAllOfBuilder()
                                .add(ProviderProductMasterData::hasManufacturer, true)
                                .add(ProviderProductMasterData::getManufacturer, "ОАО Ромашка")
                                .add(ProviderProductMasterData::getManufacturerCountryCount, 2)
                                .add(
                                        ProviderProductMasterData::getManufacturerCountryList,
                                        Arrays.asList("Россия", "Казахстан")
                                )
                                .add(ProviderProductMasterData::hasCustomsCommodityCode, true)
                                .add(ProviderProductMasterData::getCustomsCommodityCode, "HG405235")
                                .add(ProviderProductMasterData::hasDeliveryTime, true)
                                .add(ProviderProductMasterData::getDeliveryTime, 3)
                                .add(ProviderProductMasterData::hasQuantumOfSupply, true)
                                .add(ProviderProductMasterData::getQuantumOfSupply, 20)
                                .add(ProviderProductMasterData::hasBoxCount, true)
                                .add(ProviderProductMasterData::getBoxCount, 2)
                                .add(ProviderProductMasterData::hasTransportUnitSize, true)
                                .add(ProviderProductMasterData::getTransportUnitSize, 10)
                                .add(ProviderProductMasterData::hasSupplySchedule, true)
                                .add(
                                        ProviderProductMasterData::getSupplySchedule,
                                        MbiMatchers.transformedBy(
                                                ProviderProductSupplySchedule::getSupplyEventList,
                                                Matchers.contains(
                                                        MbiMatchers.transformedBy(ProviderProductSupplyEvent::getDayOfWeek, 1),
                                                        MbiMatchers.transformedBy(ProviderProductSupplyEvent::getDayOfWeek, 3),
                                                        MbiMatchers.transformedBy(ProviderProductSupplyEvent::getDayOfWeek, 7)
                                                )
                                        )
                                )
                                .add(
                                        ProviderProductMasterData::getWeightDimensionsInfo,
                                        MdmCommon.WeightDimensionsInfo.newBuilder()
                                                .setBoxLengthUm(120_000)
                                                .setBoxHeightUm(55_000)
                                                .setBoxWidthUm(7_800_000)
                                                .setWeightGrossMg(95_000_000)
                                                .build()
                                )
                                .build()
                )
                .build());
    }

    /**
     * Before the start of UNLIMITED time unit support in MARKETBTOB-1452 such unlimited times should be replced
     * with empty.
     * TODO: Remove this test after the start of UNLIMITED support.
     */
    @Test
    void testMasterDataWithUnlimitedTimeFromMboc() {
        MasterDataInfo source = MasterDataInfo.newBuilder()
                .setShelfLifeWithUnits(UNLIMITED_FROM_MBOC)
                .setShelfLifeComment("ShelfLifeComment")
                .setLifeTimeWithUnits(UNLIMITED_FROM_MBOC)
                .setGuaranteePeriodWithUnits(UNLIMITED_FROM_MBOC)
                .setProviderProductMasterData(ProviderProductMasterData.newBuilder()
                        .addManufacturerCountry("Россия")
                        .setMinShipment(100)
                        .addVetisGuid("123456789-987654321-123456789-987654321")
                        .build())
                .build();
        Optional<MasterData> converted = offerConversionService.toMasterData(source);
        MatcherAssert.assertThat(converted, MbiMatchers.isPresent(
                MbiMatchers.<MasterData>newAllOfBuilder()
                        .add(MasterData::shelfLife, Optional.empty())
                        .add(MasterData::guaranteePeriod, Optional.empty())
                        .add(MasterData::lifeTime, Optional.empty())
                        .add(MasterData::shelfLifeComment, MbiMatchers.isPresent("ShelfLifeComment"))
                        .add(MasterData::manufacturer, Optional.empty())
                        .add(MasterData::minShipment, MbiMatchers.isIntPresent(100))
                        .add(MasterData::manufacturerCountries, Collections.singletonList("Россия"))
                        .add(MasterData::customsCommodityCode, Optional.empty())
                        .add(MasterData::deliveryDuration, Optional.empty())
                        .add(MasterData::quantumOfSupply, OptionalInt.empty())
                        .add(MasterData::boxCount, OptionalInt.empty())
                        .add(MasterData::supplySchedule, Collections.emptySet())
                        .add(MasterData::transportUnitSize, OptionalInt.empty())
                        .build())
        );
    }
}
