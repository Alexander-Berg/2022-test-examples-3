package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.ServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperHelper.ORDER_IN_REFERENCE_ITEM;

@SuppressWarnings("checkstyle:MagicNumber")
public class ServiceSskuConverterTest extends MdmBaseDbTestClass {
    private static final Logger log = LoggerFactory.getLogger(ServiceSskuConverterTest.class);

    private ServiceSskuConverter converter;
    @Autowired
    private MdmParamCache mdmParamCache;
    private EnhancedRandom random;
    private static final int TEST_COUNT = 100;
    private static final String[] EXCLUDED = {
        "qualityDocuments", "itemShippingUnit", "modifiedTimestamp",
        "goldenItemShippingUnit", "goldenRsl", "surplusHandleMode",
        "cisHandleMode", "measurementState"
    };

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandom(18046L);
        converter = new ServiceSskuConverterImpl(mdmParamCache);
    }

    @Test
    public void whenConstructedFromMasterDataShouldRetainMostFields() {
        for (int i = 0; i < TEST_COUNT; ++i) {
            MasterData source = generateMasterData();
            MasterData target = converter.toMasterData(converter.toServiceSsku(source, (ReferenceItemWrapper) null));
            Assertions.assertThat(source.getRegNumbers()).isEqualTo(target.getRegNumbers());
            Assertions.assertThat(source).isEqualTo(target);
        }
    }

    @Test
    public void whenConstructedFromProtoItemShouldRetainAllFields() {
        for (int i = 0; i < TEST_COUNT; ++i) {
            log.info("Iter number #" + i);
            ReferenceItemWrapper source = generateIrisItem(i, Instant.now());
            ReferenceItemWrapper target = converter.toReferenceItem(converter.toServiceSsku(null, source));
            Assertions.assertThat(target).isEqualToIgnoringGivenFields(source, "receivedTs", "updated_ts", "updatedTs");
        }
    }

    @Test
    public void whenConstructedFromBothShouldConvertBackTheSame() {
        for (int i = 0; i < TEST_COUNT; ++i) {
            MasterData sourceMD = generateMasterData();
            ReferenceItemWrapper sourceItem = generateIrisItem(i);
            sourceMD.setShopSkuKey(sourceItem.getShopSkuKey());
            ServiceSsku ssku = converter.toServiceSsku(sourceMD, sourceItem);
            Assertions.assertThat(sourceMD).isEqualTo(converter.toMasterData(ssku));
            Assertions.assertThat(sourceMD.getRegNumbers()).isEqualTo(converter.toMasterData(ssku).getRegNumbers());
            Assertions.assertThat(sourceItem)
                .isEqualToIgnoringGivenFields(converter.toReferenceItem(ssku), "receivedTs");
        }
    }

    @Test
    public void whenDeconstructedToBothShouldConvertBackTheSame() {
        for (int i = 0; i < TEST_COUNT; ++i) {
            ServiceSsku ssku = generateSsku();
            ReferenceItemWrapper irisItem = converter.toReferenceItem(ssku);
            MasterData masterData = converter.toMasterData(ssku);
            ServiceSsku target = converter.toServiceSsku(masterData, irisItem);
            Assertions.assertThat(ssku.getValues()).usingElementComparator(
                (one, two) -> one.valueEquals(two) ? 0 : -1
            ).containsExactlyInAnyOrderElementsOf(target.getValues());
        }
    }

    @Test
    public void whenConstructedFromReferenceShouldBeRevertedAndSaveDqScoreInParams() {
        for (int i = 0; i < TEST_COUNT; ++i) {
            Integer dqScore = random.nextInt(1000);
            ReferenceItemWrapper source1 = generateIrisItem(i, dqScore, Instant.now());
            ServiceSsku ssku1 = converter.toServiceSsku(null, source1);

            ReferenceItemWrapper target1 = converter.toReferenceItem(ssku1);
            Assertions.assertThat(source1).isEqualToIgnoringGivenFields(target1, "receivedTs");

            MasterData sourceMD = generateMasterData();
            ReferenceItemWrapper source2 = generateIrisItem(i);
            sourceMD.setShopSkuKey(source2.getShopSkuKey());
            ServiceSsku ssku2 = converter.toServiceSsku(sourceMD, source2);
            ReferenceItemWrapper converted = converter.toReferenceItem(ssku2);
            Assertions.assertThat(sourceMD).isEqualTo(converter.toMasterData(ssku2));
            Assertions.assertThat(source2).isEqualToIgnoringGivenFields(converted, "receivedTs");
        }
    }

    @Test
    public void whenProtoItemHasMeasurementStateShouldCovertItToPostInheritanceParamsAndBack() {
        var validItem1 = nextMeasuredItem(true, 12345L);
        var validItem2 = nextMeasuredItem(true, 0L);
        var validItem3 = nextMeasuredItem(true, null);
        var invalidItem1 = nextMeasuredItem(null, 12345L);
        var invalidItem2 = nextMeasuredItem(null, null);
        var invalidItem3 = nextMeasuredItem(false, 12345L);
        var invalidItem4 = nextMeasuredItem(false, 0L);

        // valid items
        for (var item : List.of(validItem1, validItem2, validItem3)) {
            var ssku = converter.toServiceSsku(null, item);
            Assertions.assertThat(ssku.getValuesByParamId()).hasSize(2);
            Assertions.assertThat(ssku.getValuesByParamId()).containsKeys(
                KnownMdmParams.HAS_MEASUREMENT_AFTER_INHERIT, KnownMdmParams.LAST_MEASUREMENT_TIMESTAMP_AFTER_INHERIT
            );
            var result = converter.toReferenceItem(ssku);
            Assertions.assertThat(result).isEqualTo(item);
        }

        // invalid items
        for (var item : List.of(invalidItem1, invalidItem2, invalidItem3, invalidItem4)) {
            var ssku = converter.toServiceSsku(null, item);
            Assertions.assertThat(ssku.getValuesByParamId()).isEmpty();
            var result = converter.toReferenceItem(ssku);
            Assertions.assertThat(result).isEqualTo(emptyMeasuredItem(item));
        }
    }

    private ReferenceItemWrapper emptyMeasuredItem(ReferenceItemWrapper source) {
        ShopSkuKey key = source.getKey();
        var item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku())
                .build());
        return new ReferenceItemWrapper(item.build());
    }

    private ReferenceItemWrapper nextMeasuredItem(@Nullable Boolean flag, @Nullable Long millis) {
        ShopSkuKey key = random.nextObject(ShopSkuKey.class);
        var item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku())
                .build());
        var measurementState = MdmIrisPayload.MeasurementState.newBuilder();
        if (flag != null) {
            measurementState.setIsMeasured(flag);
        }
        if (millis != null) {
            measurementState.setLastMeasurementTs(millis);
        }
        if (flag != null || millis != null) {
            item.setMeasurementState(measurementState);
        }
        return new ReferenceItemWrapper(item.build());
    }

    private MasterData generateMasterData() {
        MasterData md = random.nextObject(MasterData.class, EXCLUDED);
        if (md.getVat() == VatRate.VAT_10_110) {
            md.setVat(VatRate.VAT_10);
        } else if (md.getVat() == VatRate.VAT_18_118) {
            md.setVat(VatRate.VAT_18);
        } else {
            md.setVat(VatRate.VAT_0);
        }
        return md;
    }

    private ReferenceItemWrapper generateIrisItem(Integer type) {
        return generateIrisItem(type, null, Instant.now());
    }

    private ReferenceItemWrapper generateIrisItem(Integer type, Instant updateTs) {
        return generateIrisItem(type, null, updateTs);
    }

    private ReferenceItemWrapper generateIrisItem(Integer type, Integer dqScore, Instant updateTs) {
        ArrayList<BiFunction<Integer, Instant, ReferenceItemWrapper>> generators = new ArrayList<>();
        generators.add(this::goldVghNetTare);
        generators.add(this::goldVghNet_Tare);
        generators.add(this::goldVgh_NetTare);
        generators.add(this::goldVgh_Net_Tare);
        generators.add(this::goldVghTare_Net);
        generators.add(this::goldVghNet);
        generators.add(this::goldVghTare);
        generators.add(this::goldVgh);
        generators.add(this::goldEmpty);
        generators.add(this::goldVgh_Net);
        generators.add(this::goldVgh_Tare);
        return generators.get(type % generators.size()).apply(dqScore, updateTs);
    }

    private List<MdmIrisPayload.ReferenceInformation.Builder> infos(int count) {
        List<MdmIrisPayload.ReferenceInformation.Builder> infos = new ArrayList<>();
        getSourceCombination(count).forEach(source -> {
            var info = MdmIrisPayload.ReferenceInformation.newBuilder();
            info.setSource(MasterDataSourceType.pojo2proto(source, random.nextObject(String.class)));
            infos.add(info);
        });
        var info = MdmIrisPayload.ReferenceInformation.newBuilder();
        info.setSource(MdmIrisPayload.Associate.newBuilder()
            .setId(MasterDataSourceType.RSL_SOURCE_ID)
            .setType(MdmIrisPayload.MasterDataSource.MDM)
            .setSubtype(MasterDataSourceType.MDM_OPERATOR.name())
            .build());
        info.addMinInboundLifetimeDay(generateRsl())
            .addMinOutboundLifetimeDay(generateRsl())
            .addMinInboundLifetimePercentage(generateRsl())
            .addMinOutboundLifetimePercentage(generateRsl());
        infos.add(info);
        return infos;
    }

    private MdmIrisPayload.Item.Builder item() {
        return MdmIrisPayload.Item.newBuilder().setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
            .setSupplierId(random.nextInt()).setShopSku(random.nextObject(String.class)).build());
    }

    private ReferenceItemWrapper goldVghNetTare(Integer dqSCore, Instant updateTs) {
        var item = item();
        var infos = infos(1);

        infos.get(0).setItemShippingUnit(generateShippingUnit(dqSCore, updateTs));
        infos.forEach(item::addInformation);

        return new ReferenceItemWrapper().setReferenceItem(item.build());
    }

    private ReferenceItemWrapper goldVghNet_Tare(Integer dqSCore, Instant updateTs) {
        var item = item();
        var infos = infos(2);

        infos.get(0).setItemShippingUnit(generateShippingUnit(dqSCore, updateTs).clearWeightTareMg());
        infos.get(1).setItemShippingUnit(generateShippingUnit(dqSCore, updateTs)
            .clearLengthMicrometer()
            .clearHeightMicrometer()
            .clearWidthMicrometer()
            .clearWeightGrossMg()
            .clearWeightNetMg()
        );
        infos.forEach(item::addInformation);

        return new ReferenceItemWrapper().setReferenceItem(item.build());
    }

    private ReferenceItemWrapper goldVgh_NetTare(Integer dqSCore, Instant updateTs) {
        var item = item();
        var infos = infos(2);

        infos.get(0).setItemShippingUnit(
            generateShippingUnit(dqSCore, updateTs).clearWeightTareMg().clearWeightNetMg()
        );
        infos.get(1).setItemShippingUnit(generateShippingUnit(dqSCore, updateTs)
            .clearLengthMicrometer()
            .clearHeightMicrometer()
            .clearWidthMicrometer()
            .clearWeightGrossMg()
        );
        infos.forEach(item::addInformation);

        return new ReferenceItemWrapper().setReferenceItem(item.build());
    }

    private ReferenceItemWrapper goldVgh_Net_Tare(Integer dqSCore, Instant updateTs) {
        var item = item();
        var infos = infos(3);

        infos.get(0).setItemShippingUnit(
            generateShippingUnit(dqSCore, updateTs).clearWeightTareMg().clearWeightNetMg()
        );
        infos.get(1).setItemShippingUnit(generateShippingUnit(dqSCore, updateTs)
            .clearLengthMicrometer()
            .clearHeightMicrometer()
            .clearWidthMicrometer()
            .clearWeightGrossMg()
            .clearWeightTareMg()
        );
        infos.get(2).setItemShippingUnit(generateShippingUnit(dqSCore, updateTs)
            .clearLengthMicrometer()
            .clearHeightMicrometer()
            .clearWidthMicrometer()
            .clearWeightGrossMg()
            .clearWeightNetMg()
        );
        infos.forEach(item::addInformation);

        return new ReferenceItemWrapper().setReferenceItem(item.build());
    }

    private ReferenceItemWrapper goldVghTare_Net(Integer dqSCore, Instant updateTs) {
        var item = item();
        var infos = infos(2);

        infos.get(0).setItemShippingUnit(generateShippingUnit(dqSCore, updateTs).clearWeightNetMg());
        infos.get(1).setItemShippingUnit(generateShippingUnit(dqSCore, updateTs)
            .clearLengthMicrometer()
            .clearHeightMicrometer()
            .clearWidthMicrometer()
            .clearWeightGrossMg()
            .clearWeightTareMg()
        );
        infos.forEach(item::addInformation);

        return new ReferenceItemWrapper().setReferenceItem(item.build());
    }

    private ReferenceItemWrapper goldVghNet(Integer dqScore, Instant updateTs) {
        var item = item();
        var infos = infos(1);

        infos.get(0).setItemShippingUnit(generateShippingUnit(dqScore, updateTs).clearWeightTareMg());
        infos.forEach(item::addInformation);

        return new ReferenceItemWrapper().setReferenceItem(item.build());
    }

    private ReferenceItemWrapper goldVghTare(Integer dqScore, Instant updateTs) {
        var item = item();
        var infos = infos(1);

        infos.get(0).setItemShippingUnit(generateShippingUnit(dqScore, updateTs).clearWeightNetMg());
        infos.forEach(item::addInformation);

        return new ReferenceItemWrapper().setReferenceItem(item.build());
    }

    private ReferenceItemWrapper goldVgh(Integer dqScore, Instant updateTs) {
        var item = item();
        var infos = infos(1);

        infos.get(0).setItemShippingUnit(
            generateShippingUnit(dqScore, updateTs).clearWeightTareMg().clearWeightNetMg()
        );
        infos.forEach(item::addInformation);

        return new ReferenceItemWrapper().setReferenceItem(item.build());
    }

    private ReferenceItemWrapper goldEmpty(Integer dqScore, Instant updateTs) {
        var item = item();
        return new ReferenceItemWrapper().setReferenceItem(item.build());
    }

    private ReferenceItemWrapper goldVgh_Net(Integer dqScore, Instant updateTs) {
        var item = item();
        var infos = infos(2);

        infos.get(0).setItemShippingUnit(
            generateShippingUnit(dqScore, updateTs).clearWeightTareMg().clearWeightNetMg()
        );
        infos.get(1).setItemShippingUnit(generateShippingUnit(dqScore, updateTs)
            .clearLengthMicrometer()
            .clearHeightMicrometer()
            .clearWidthMicrometer()
            .clearWeightGrossMg()
            .clearWeightTareMg()
        );
        infos.forEach(item::addInformation);

        return new ReferenceItemWrapper().setReferenceItem(item.build());
    }

    private ReferenceItemWrapper goldVgh_Tare(Integer dqScore, Instant updateTs) {
        var item = item();
        var infos = infos(2);

        infos.get(0).setItemShippingUnit(
            generateShippingUnit(dqScore, updateTs).clearWeightTareMg().clearWeightNetMg()
        );
        infos.get(1).setItemShippingUnit(generateShippingUnit(dqScore, updateTs)
            .clearLengthMicrometer()
            .clearHeightMicrometer()
            .clearWidthMicrometer()
            .clearWeightGrossMg()
            .clearWeightNetMg()
        );
        infos.forEach(item::addInformation);

        return new ReferenceItemWrapper().setReferenceItem(item.build());
    }

    private List<MasterDataSourceType> getSourceCombination(int informationCount) {
        List<MasterDataSourceType> sources = new ArrayList<>(List.of(
            MasterDataSourceType.MEASUREMENT,
            MasterDataSourceType.SUPPLIER,
            MasterDataSourceType.WAREHOUSE,
            MasterDataSourceType.MDM_OPERATOR,
            MasterDataSourceType.MDM_DEFAULT
        ));
        Collections.shuffle(sources, random);
        return sources.stream()
            .limit(informationCount)
            .sorted(Comparator.comparingInt(s ->
                ORDER_IN_REFERENCE_ITEM.get(new MasterDataSource(s, "").toIrisProto().getType())))
            .collect(Collectors.toList());
    }

    private MdmIrisPayload.ShippingUnit.Builder generateShippingUnit(Integer dqScore, Instant updateTs) {
        return MdmIrisPayload.ShippingUnit.newBuilder()
            .setWeightNetMg(generateIntValue(dqScore, updateTs))
            .setWeightTareMg(generateIntValue(dqScore, updateTs))
            .setWeightGrossMg(generateIntValue(dqScore, updateTs))
            .setWidthMicrometer(generateIntValue(dqScore, updateTs))
            .setHeightMicrometer(generateIntValue(dqScore, updateTs))
            .setLengthMicrometer(generateIntValue(dqScore, updateTs));
    }

    private MdmIrisPayload.Int64Value generateIntValue(Integer dqScore, Instant updateTs) {
        MdmIrisPayload.Int64Value.Builder valueBuilder = MdmIrisPayload.Int64Value.newBuilder()
            .setValue(random.nextInt(10000)).setUpdatedTs(updateTs.toEpochMilli());
        if (dqScore != null) {
            valueBuilder.setDqScore(dqScore);
        }
        return valueBuilder.build();
    }

    private MdmIrisPayload.RemainingLifetime generateRsl() {
        return MdmIrisPayload.RemainingLifetime.newBuilder()
            .setSource(MdmIrisPayload.Associate.newBuilder()
                .setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MDM_OPERATOR.name())
                .build())
            .setStartDate(1000)
            .setValue(random.nextInt(100))
            .setUpdatedTs(1)
            .build();
    }

    private ServiceSsku generateSsku() {
        var key = random.nextObject(ShopSkuKey.class);
        ServiceSsku ssku = new ServiceSsku(key);
        List<SskuParamValue> values = List.of(
            generateNumeric(key, KnownMdmParams.WEIGHT_TARE, null),
            generateNumeric(key, KnownMdmParams.QUANTITY_IN_PACK, "quantityInPack"),
            generateString(key, KnownMdmParams.MANUFACTURER, "manufacturer"),
            generateStrings(key, KnownMdmParams.GTIN, "gtin"),
            generateBool(key, KnownMdmParams.EXPIR_DATE, "expir_date")
        );
        ssku.addParamValues(values);
        return ssku;
    }

    private SskuParamValue generateNumeric(ShopSkuKey key, long paramId, String xslName) {
        SskuParamValue value = new SskuParamValue().setShopSkuKey(key);
        value.setMdmParamId(paramId);
        value.setNumeric(BigDecimal.valueOf(random.nextLong() % 1000));
        value.setXslName(xslName);
        return value;
    }

    private SskuParamValue generateString(ShopSkuKey key, long paramId, String xslName) {
        SskuParamValue value = new SskuParamValue().setShopSkuKey(key);
        value.setMdmParamId(paramId);
        value.setString(random.nextObject(String.class));
        value.setXslName(xslName);
        return value;
    }

    private SskuParamValue generateStrings(ShopSkuKey key, long paramId, String xslName) {
        SskuParamValue value = new SskuParamValue().setShopSkuKey(key);
        value.setMdmParamId(paramId);
        value.setStrings(List.of(
            random.nextObject(String.class),
            random.nextObject(String.class),
            random.nextObject(String.class)));
        value.setXslName(xslName);
        return value;
    }

    private SskuParamValue generateBool(ShopSkuKey key, long paramId, String xslName) {
        SskuParamValue value = new SskuParamValue().setShopSkuKey(key);
        value.setMdmParamId(paramId);
        value.setBool(random.nextBoolean());
        value.setXslName(xslName);
        return value;
    }
}
