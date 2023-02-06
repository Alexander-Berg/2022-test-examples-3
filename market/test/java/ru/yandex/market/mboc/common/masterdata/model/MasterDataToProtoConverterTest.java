package ru.yandex.market.mboc.common.masterdata.model;

import java.time.DayOfWeek;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.converter.ConversionAsserter;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.services.category.MboTimeUnitAliasesService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.ErrorInfo.Level;
import ru.yandex.market.mboc.http.MbocCommon;
import ru.yandex.market.mdm.http.MasterDataProto;
import ru.yandex.market.mdm.http.MdmCommon;
import ru.yandex.market.mdm.http.MdmDocument;

/**
 * @author masterj
 */
@SuppressWarnings("checkstyle:magicnumber")
public class MasterDataToProtoConverterTest {
    private static final long SEED = 9;

    private EnhancedRandom defaultRandom;

    private static void assertTimeInUnits(TimeInUnits value,
                                          int expectedTime,
                                          TimeInUnits.TimeUnit expectedUnit) {
        Assertions.assertThat(value).isEqualTo(new TimeInUnits(expectedTime, expectedUnit));
    }

    private static void assertProtoTimeWithUnits(MdmCommon.TimeInUnits value,
                                                 int expectedTime,
                                                 MdmCommon.TimeUnit expectedUnit) {
        Assertions.assertThat(value).isEqualTo(
            MdmCommon.TimeInUnits.newBuilder()
                .setValue(expectedTime)
                .setUnit(expectedUnit)
                .build());
    }

    private static MasterData getMasterData(EnhancedRandom defaultRandom, String s, int i) {
        MasterData masterData = TestDataUtils.generateMasterData(s, i, defaultRandom);

        // ConversionAsserter does not accept valid empty schedule
        if (masterData.getSupplySchedule().isEmpty()) {
            masterData.addSupplyEvent(new SupplyEvent(DayOfWeek.MONDAY));
        }
        return masterData;
    }

    @Before
    public void setUp() throws Exception {
        defaultRandom = TestDataUtils.defaultRandom(SEED);
    }

    @Test
    public void whenConvertsMasterDataToPpmdConverterShouldHandleAllFields() {
        MasterData masterData = getMasterData(defaultRandom, "a-string", 3);
        MasterDataProto.ProviderProductMasterData ppmd =
            MbocBaseProtoConverter.convertMasterDataToPpmd(masterData);
        ConversionAsserter.assertThatAllProtoFieldsAreSet(ppmd);
    }

    @Test
    public void whenConvertsMasterDataToYemdConverterShouldHandleAllFields() {
        MasterData masterData = getMasterData(defaultRandom, "a-string", 3);
        MasterDataProto.YtExportMasterData yemd =
            MasterDataToProtoConverter.convertMasterDataToYtExport(masterData);
        ConversionAsserter.assertThatAllProtoFieldsAreSet(yemd);
    }

    @Test
    public void whenConvertsEmptyMasterDataToPpmdShouldHaveNoFields() {
        ConversionAsserter.assertThatNoProtoFieldsAreSet(
            MbocBaseProtoConverter.convertMasterDataToPpmd(new MasterData())
        );
    }

    @Test
    public void whenConvertsEmptyMasterDataToYemdShouldHaveNoFields() {
        ConversionAsserter.assertThatNoProtoFieldsAreSet(
            MasterDataToProtoConverter.convertMasterDataToYtExport(new MasterData())
        );
    }

    @Test
    public void whenConvertsPpmdToMasterDataShouldHandleAllFields() {
        // start with a random proto
        MasterData randomMasterData = getMasterData(defaultRandom, "ssku1", 2);
        MasterDataProto.ProviderProductMasterData randomPpmd =
            MbocBaseProtoConverter.convertMasterDataToPpmd(randomMasterData);

        /* convert it to proto hoping no fields are forgotten in
         * {@link MbocBaseProtoConverter#convertPpmdToMasterData} */
        MasterData masterData = MbocBaseProtoConverter.convertPpmdToMasterData(randomPpmd);
        // now ensure the hope
        ConversionAsserter.assertThatAllProtoFieldsAreSet(
            MbocBaseProtoConverter.convertMasterDataToPpmd(masterData)
        );
    }

    @Test
    public void whenConvertsMasterDataToMdiShouldHandleAllFields() {
        MasterData masterData = getMasterData(defaultRandom, "ssku3", 4);
        MasterDataProto.MasterDataInfo ppmd =
            MasterDataToProtoConverter.convertMasterDataToMdi(masterData);
        ConversionAsserter.assertThatAllProtoFieldsAreSet(ppmd);
    }

    @Test
    public void whenConvertsEmptyMasterDataToMdiShouldHaveNoFields() {
        ConversionAsserter.assertThatNoProtoFieldsAreSet(
            MasterDataToProtoConverter.convertMasterDataToMdi(new MasterData())
        );
    }

    @Test
    public void whenConvertsMdiToMasterDataShouldHandleAllFields() {
        MasterData randomMasterData = getMasterData(defaultRandom, "sku0", 1);
        MasterDataProto.MasterDataInfo randomMdi = MasterDataToProtoConverter.convertMasterDataToMdi(randomMasterData);
        MboTimeUnitAliasesService timeUnitAliasesService = Mockito.mock(MboTimeUnitAliasesService.class);
        MasterDataFromMdiConverter masterDataFromMdiConverter = new MasterDataFromMdiConverter(timeUnitAliasesService);
        MasterData masterData = masterDataFromMdiConverter.convertMdiToMasterData(
            Collections.singleton(Maps.immutableEntry(randomMasterData.getShopSkuKey(), randomMdi))).get(0);
        MasterDataProto.MasterDataInfo mdi = MasterDataToProtoConverter.convertMasterDataToMdi(masterData);
        ConversionAsserter.assertThatAllProtoFieldsAreSet(mdi);
    }

    @Test
    public void testMasterDataTimePropertiesToProtoConversion() {
        MasterData masterData = new MasterData()
            .setShelfLife(7, TimeInUnits.TimeUnit.DAY)
            .setLifeTime(24, TimeInUnits.TimeUnit.MONTH)
            .setGuaranteePeriod(25, TimeInUnits.TimeUnit.YEAR)
            .setShelfLifeComment("Comment1")
            .setLifeTimeComment("Comment2")
            .setGuaranteePeriodComment("Comment3");

        MasterDataProto.MasterDataInfo converted = MasterDataToProtoConverter.convertMasterDataToMdi(masterData);
        assertProtoTimeWithUnits(converted.getShelfLifeWithUnits(), 7, MdmCommon.TimeUnit.DAY);
        assertProtoTimeWithUnits(converted.getLifeTimeWithUnits(), 24, MdmCommon.TimeUnit.MONTH);
        assertProtoTimeWithUnits(converted.getGuaranteePeriodWithUnits(), 25, MdmCommon.TimeUnit.YEAR);
        Assertions.assertThat(converted.getShelfLifeComment()).isEqualTo("Comment1");
        Assertions.assertThat(converted.getLifeTimeComment()).isEqualTo("Comment2");
        Assertions.assertThat(converted.getGuaranteePeriodComment()).isEqualTo("Comment3");
    }

    @Test
    public void testMasterDataTimePropertiesToProtoConversionTooBigValues() {
        MasterData masterData = new MasterData()
            .setShelfLife(29112021, TimeInUnits.TimeUnit.YEAR)
            .setLifeTime(29112021, TimeInUnits.TimeUnit.YEAR)
            .setGuaranteePeriod(29112021, TimeInUnits.TimeUnit.YEAR)
            .setShelfLifeComment("Comment1")
            .setLifeTimeComment("Comment2")
            .setGuaranteePeriodComment("Comment3");

        MasterDataProto.MasterDataInfo converted = MasterDataToProtoConverter.convertMasterDataToMdi(masterData);
        assertProtoTimeWithUnits(converted.getShelfLifeWithUnits(), 29112021, MdmCommon.TimeUnit.YEAR);
        assertProtoTimeWithUnits(converted.getLifeTimeWithUnits(), 29112021, MdmCommon.TimeUnit.YEAR);
        assertProtoTimeWithUnits(converted.getGuaranteePeriodWithUnits(), 29112021, MdmCommon.TimeUnit.YEAR);
        Assertions.assertThat(converted.getShelfLifeComment()).isEqualTo("Comment1");
        Assertions.assertThat(converted.getLifeTimeComment()).isEqualTo("Comment2");
        Assertions.assertThat(converted.getGuaranteePeriodComment()).isEqualTo("Comment3");
    }

    @Test
    public void testConvertAllRequiredFieldsInSskuMasterData() {
        List<MasterData> original = TestDataUtils.generateSskuMsterData(100, defaultRandom);

        List<MasterData> converted = original.stream()
            .map(MbocBaseProtoConverter::pojoToProto)
            .map(MbocBaseProtoConverter::protoToPojo)
            .collect(Collectors.toList());

        Assertions.assertThat(converted)
            .usingElementComparatorIgnoringFields("qualityDocuments", "modifiedTimestamp", "heavyGood", "categoryId",
                "itemShippingUnit", "nonItemShippingUnits", "preciousGood", "goldenItemShippingUnit", "goldenRsl",
                "surplusHandleMode", "cisHandleMode", "heavyGood20", "regNumbers", "version",
                "datacampMasterDataVersion", "traceable")
            .containsExactlyElementsOf(original);
    }

    @Test
    public void testMasterDataTimePropertiesFromProtoConversion() {
        // Прото-мастер-данные в самом старом формате, когда мы указывали просто число дней
        MasterDataProto.MasterDataInfo mdiWithDays = MasterDataProto.MasterDataInfo.newBuilder()
            .setShelfLife("120")
            .setLifeTime("3")
            .setGuaranteePeriod("30")
            .build();

        // Мастер данные в старом формате, когда в строчке указывался срок с единицами времени
        MasterDataProto.MasterDataInfo mdiWithNotOnlyDays = MasterDataProto.MasterDataInfo.newBuilder()
            .setShelfLife("2 месяца")
            .setLifeTime("5 лет")
            .setGuaranteePeriod("10000 часов")
            .build();

        // Мастер-данные в новом формате, когда срок указывается в виде структуры TimeInUnits
        MasterDataProto.MasterDataInfo mdiInNewFormat = MasterDataProto.MasterDataInfo.newBuilder()
            .setShelfLifeWithUnits(MdmCommon.TimeInUnits.newBuilder().setValue(4).setUnit(MdmCommon.TimeUnit.WEEK))
            .setLifeTimeWithUnits(MdmCommon.TimeInUnits.newBuilder().setValue(120).setUnit(MdmCommon.TimeUnit.DAY))
            .setGuaranteePeriodWithUnits(
                MdmCommon.TimeInUnits.newBuilder().setValue(10000).setUnit(MdmCommon.TimeUnit.HOUR))
            .setShelfLifeComment("Comment1")
            .setLifeTimeComment("COMMENT2")
            .setGuaranteePeriodComment("CommenT3")
            .build();

        Map<ShopSkuKey, MasterDataProto.MasterDataInfo> mdiMap = ImmutableMap.of(
            new ShopSkuKey(100, "shopSku1"), mdiWithDays,
            new ShopSkuKey(100, "shopSku2"), mdiWithNotOnlyDays,
            new ShopSkuKey(100, "shopSku3"), mdiInNewFormat
        );

        MboTimeUnitAliasesService timeUnitAliasesService = Mockito.mock(MboTimeUnitAliasesService.class);
        MasterDataFromMdiConverter masterDataFromMdiConverter = new MasterDataFromMdiConverter(timeUnitAliasesService);

        List<MasterData> mdiList = masterDataFromMdiConverter.convertMdiToMasterData(mdiMap.entrySet());

        assertTimeInUnits(mdiList.get(0).getShelfLife(), 120, TimeInUnits.TimeUnit.DAY);
        assertTimeInUnits(mdiList.get(0).getLifeTime(), 3, TimeInUnits.TimeUnit.DAY);
        assertTimeInUnits(mdiList.get(0).getGuaranteePeriod(), 30, TimeInUnits.TimeUnit.DAY);
        Assertions.assertThat(mdiList.get(0).hasShelfLifeComment()).isFalse();
        Assertions.assertThat(mdiList.get(0).hasLifeTimeComment()).isFalse();
        Assertions.assertThat(mdiList.get(0).hasGuaranteePeriodComment()).isFalse();

        assertTimeInUnits(mdiList.get(1).getShelfLife(), 2, TimeInUnits.TimeUnit.MONTH);
        assertTimeInUnits(mdiList.get(1).getLifeTime(), 5, TimeInUnits.TimeUnit.YEAR);
        assertTimeInUnits(mdiList.get(1).getGuaranteePeriod(), 10000, TimeInUnits.TimeUnit.HOUR);

        assertTimeInUnits(mdiList.get(2).getShelfLife(), 4, TimeInUnits.TimeUnit.WEEK);
        assertTimeInUnits(mdiList.get(2).getLifeTime(), 120, TimeInUnits.TimeUnit.DAY);
        assertTimeInUnits(mdiList.get(2).getGuaranteePeriod(), 10000, TimeInUnits.TimeUnit.HOUR);
        Assertions.assertThat(mdiList.get(2).getShelfLifeComment()).isEqualTo("comment1");
        Assertions.assertThat(mdiList.get(2).getLifeTimeComment()).isEqualTo("cOMMENT2");
        Assertions.assertThat(mdiList.get(2).getGuaranteePeriodComment()).isEqualTo("commenT3");
    }

    @Test
    public void testProviderProductMasterData() {
        new ConversionAsserter<>(
            () -> MasterDataProto.ProviderProductMasterData.newBuilder().build(),
            () -> new MasterData(),
            () -> getMasterData(defaultRandom, "sku0", 1),
            MbocBaseProtoConverter::convertMasterDataToPpmd,
            MbocBaseProtoConverter::convertPpmdToMasterData
        ).doAssertions();
    }

    @Test
    public void testYtExportMasterData() {
        new ConversionAsserter<>(
            () -> MasterDataProto.YtExportMasterData.newBuilder().build(),
            () -> new MasterData(),
            () -> getMasterData(defaultRandom, "a-string", 2),
            MasterDataToProtoConverter::convertMasterDataToYtExport,
            MbocBaseProtoConverter::convertYtExportToMasterData
        ).doAssertions();
    }

    @Test
    public void testDocumentMetadata() {
        new ConversionAsserter<>(
            () -> MdmDocument.Document.Metadata.newBuilder().build(),
            () -> new QualityDocument.Metadata(),
            () -> TestDataUtils.generate(QualityDocument.Metadata.class, defaultRandom),
            DocumentProtoConverter::createProtoMetadata,
            DocumentProtoConverter::createQualityMetadata
        ).doAssertions();
    }

    @Test
    public void testDocumentErrorsConversion() {
        var pojo = MbocErrors.get().qdIncorrectRegistrationNumberFormat();
        MbocCommon.Message proto = MbocCommon.Message.newBuilder()
            .setMessageCode(pojo.getErrorCode())
            .setMustacheTemplate(pojo.getMessageTemplate())
            .setJsonDataForMustacheTemplate("{\"num\": 2, \"str\": \"val\", \"float\": 1.5}")
            .build();
        var result = MbocBaseProtoConverter.protoToPojo(proto);
        Assertions.assertThat(result.getErrorCode()).isEqualTo(pojo.getErrorCode());
        Assertions.assertThat(result.getLevel()).isEqualTo(Level.ERROR);
        Assertions.assertThat(result.getMessageTemplate()).isEqualTo(pojo.getMessageTemplate());
        Assertions.assertThat(result.getParams()).isEqualTo(ImmutableMap.of(
            "num", 2,
            "str", "val",
            "float", 1.5
        ));
    }

    @Test
    public void testCountriesWithSynonymsConversion() {
        MasterData masterData = new MasterData();
        masterData.setManufacturerCountries(List.of("Республика Беларусь"));
        var proto = MasterDataToProtoConverter.convertMasterDataToYtExport(masterData);
        Assertions.assertThat(proto.getManufacturerCountryList()).containsExactly(
            MasterDataProto.ManufacturerCountry.newBuilder().setGeoId(149).setRuName("Беларусь").build()
        );

        masterData.setManufacturerCountries(List.of("Россия"));
        proto = MasterDataToProtoConverter.convertMasterDataToYtExport(masterData);
        Assertions.assertThat(proto.getManufacturerCountryList()).containsExactly(
            MasterDataProto.ManufacturerCountry.newBuilder().setGeoId(225).setRuName("Россия").build()
        );

        masterData.setManufacturerCountries(List.of("Vanuatu"));
        proto = MasterDataToProtoConverter.convertMasterDataToYtExport(masterData);
        Assertions.assertThat(proto.getManufacturerCountryList()).containsExactly(
            MasterDataProto.ManufacturerCountry.newBuilder().setGeoId(21556).setRuName("Вануату").build()
        );

        masterData.setManufacturerCountries(List.of("NZ"));
        proto = MasterDataToProtoConverter.convertMasterDataToYtExport(masterData);
        Assertions.assertThat(proto.getManufacturerCountryList()).containsExactly(
            MasterDataProto.ManufacturerCountry.newBuilder().setGeoId(139).setRuName("Новая Зеландия").build()
        );

        masterData.setManufacturerCountries(List.of("Российская Федерация"));
        proto = MasterDataToProtoConverter.convertMasterDataToYtExport(masterData);
        Assertions.assertThat(proto.getManufacturerCountryList()).containsExactly(
            MasterDataProto.ManufacturerCountry.newBuilder().setGeoId(225).setRuName("Россия").build()
        );

        masterData.setManufacturerCountries(List.of("Czech Republic", "Latvijas Republika"));
        proto = MasterDataToProtoConverter.convertMasterDataToYtExport(masterData);
        Assertions.assertThat(proto.getManufacturerCountryList()).containsExactly(
            MasterDataProto.ManufacturerCountry.newBuilder().setGeoId(125).setRuName("Чехия").build(),
            MasterDataProto.ManufacturerCountry.newBuilder().setGeoId(206).setRuName("Латвия").build()
        );

        masterData.setManufacturerCountries(List.of("Косово"));
        proto = MasterDataToProtoConverter.convertMasterDataToYtExport(masterData);
        Assertions.assertThat(proto.getManufacturerCountryList()).isEmpty();
    }
}
