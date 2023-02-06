package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang.RandomStringUtils;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmModificationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamExternals;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValueType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mboc.common.masterdata.model.CargoType;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits.TimeUnit;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class TestMdmParamUtils {

    private static final Set<Long> NOT_WRITTEN_PARAM_IDS = Set.of(KnownMdmParams.DQ_SCORE);

    public static final Comparator<MdmModificationInfo> MODIFICATION_INFO_COMPARATOR =
        Comparator.comparing(MdmModificationInfo::getMasterDataSourceId)
            .thenComparing(MdmModificationInfo::getMasterDataSourceType)
            .thenComparing(MdmModificationInfo::getSourceUpdatedTs)
            .thenComparing(MdmModificationInfo::getUpdatedByUid)
            .thenComparing(MdmModificationInfo::getUpdatedByLogin);

    private TestMdmParamUtils() {
    }

    @SuppressWarnings("checkstyle:MethodLength")
    public static List<MdmParam> createDefaultKnownMdmParams() {
        MdmParam shelfLife = new MdmParam()
            .setId(KnownMdmParams.SHELF_LIFE)
            .setXslName("-")
            .setTitle("Полка жизнь")
            .setValueType(MdmParamValueType.NUMERIC)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam shelfLifeUnit = new MdmParam()
            .setId(KnownMdmParams.SHELF_LIFE_UNIT)
            .setXslName("-")
            .setTitle("Полка единичная жизнь")
            .setValueType(MdmParamValueType.MBO_ENUM)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        addTimeUnitOptionRenders(shelfLifeUnit);
        addTimeUnitsOptions(shelfLifeUnit);
        MdmParam shelfLifeComment = new MdmParam()
            .setId(KnownMdmParams.SHELF_LIFE_COMMENT)
            .setXslName("-")
            .setTitle("Полка жизнь прокомментировал")
            .setValueType(MdmParamValueType.STRING)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));

        MdmParam lifeTime = new MdmParam()
            .setId(KnownMdmParams.LIFE_TIME)
            .setXslName("-")
            .setTitle("Жизненное время")
            .setValueType(MdmParamValueType.NUMERIC)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam lifeTimeUnit = new MdmParam()
            .setId(KnownMdmParams.LIFE_TIME_UNIT)
            .setXslName("-")
            .setTitle("Жизнь - единица времени")
            .setValueType(MdmParamValueType.MBO_ENUM)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        addTimeUnitOptionRenders(lifeTimeUnit);
        addTimeUnitsOptions(lifeTimeUnit);
        MdmParam lifeTimeComment = new MdmParam()
            .setId(KnownMdmParams.LIFE_TIME_COMMENT)
            .setXslName("-")
            .setTitle("Прожить комментарий времени")
            .setValueType(MdmParamValueType.STRING)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));

        MdmParam guaranteePeriod = new MdmParam()
            .setId(KnownMdmParams.GUARANTEE_PERIOD)
            .setXslName("-")
            .setTitle("Гарантирую эти дни")
            .setValueType(MdmParamValueType.NUMERIC)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam guaranteePeriodUnit = new MdmParam()
            .setId(KnownMdmParams.GUARANTEE_PERIOD_UNIT)
            .setXslName("-")
            .setTitle("Гарантия периода боевой единицы")
            .setValueType(MdmParamValueType.MBO_ENUM)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        addTimeUnitOptionRenders(guaranteePeriodUnit);
        addTimeUnitsOptions(guaranteePeriodUnit);
        MdmParam guaranteePeriodComment = new MdmParam()
            .setId(KnownMdmParams.GUARANTEE_PERIOD_COMMENT)
            .setXslName("-")
            .setTitle("Гарантировать точку комментарий")
            .setValueType(MdmParamValueType.STRING)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));

        MdmParam vatRate = new MdmParam()
            .setId(KnownMdmParams.VAT)
            .setXslName("-")
            .setTitle("ЧАН")
            .setOptions(List.of(
                KnownMdmParams.VAT_0_OPTION, KnownMdmParams.VAT_10_OPTION, KnownMdmParams.VAT_18_OPTION
            ))
            .setValueType(MdmParamValueType.MBO_NUMERIC_ENUM)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam expirDate = new MdmParam()
            .setId(KnownMdmParams.EXPIR_DATE)
            .setXslName("-")
            .setTitle("Истёкшие свидания примени")
            .setValueType(MdmParamValueType.MBO_BOOL)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam expirationDatesApply = new MdmParam()
            .setId(KnownMdmParams.EXPIRATION_DATES_APPLY)
            .setXslName("-")
            .setTitle("Применим срок годности")
            .setValueType(MdmParamValueType.MBO_ENUM)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        addExpirationDatesApplyOptionRenders(expirationDatesApply);
        MdmParam customsCommCode = new MdmParam()
            .setId(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID)
            .setXslName("-")
            .setTitle("Обычаи КОММ код МДМ Айдахо")
            .setValueType(MdmParamValueType.STRING)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam customsCommCodePrefix = new MdmParam()
            .setId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX)
            .setXslName("-")
            .setTitle("Обычаи КОММ код предпочинка")
            .setValueType(MdmParamValueType.STRING)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam heavyGood = new MdmParam()
            .setId(KnownMdmParams.HEAVY_GOOD)
            .setXslName("-")
            .setTitle("Крупногабаритный")
            .setValueType(MdmParamValueType.MBO_BOOL)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam heavyGood20 = new MdmParam()
            .setId(KnownMdmParams.HEAVY_GOOD_20)
            .setXslName("-")
            .setTitle("Крупногабаритный 20 кг")
            .setValueType(MdmParamValueType.MBO_BOOL)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam vetisGuids = new MdmParam()
            .setId(KnownMdmParams.VETIS_GUID)
            .setXslName("-")
            .setTitle("Vetis GUID")
            .setValueType(MdmParamValueType.STRING)
            .setMultivalue(true)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam gtin = new MdmParam()
            .setId(KnownMdmParams.GTIN)
            .setXslName("-")
            .setTitle("GTIN")
            .setValueType(MdmParamValueType.STRING)
            .setMultivalue(true)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));

        MdmParam length = new MdmParam()
            .setId(KnownMdmParams.LENGTH)
            .setXslName("-")
            .setTitle("Длительность")
            .setValueType(MdmParamValueType.NUMERIC)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam width = new MdmParam()
            .setId(KnownMdmParams.WIDTH)
            .setXslName("-")
            .setTitle("Ширина")
            .setValueType(MdmParamValueType.NUMERIC)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam height = new MdmParam()
            .setId(KnownMdmParams.HEIGHT)
            .setXslName("-")
            .setTitle("Высота")
            .setValueType(MdmParamValueType.NUMERIC)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam weightGross = new MdmParam()
            .setId(KnownMdmParams.WEIGHT_GROSS)
            .setXslName("-")
            .setTitle("Тяжесть вульгарно")
            .setValueType(MdmParamValueType.NUMERIC)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam weightNet = new MdmParam()
            .setId(KnownMdmParams.WEIGHT_NET)
            .setXslName("-")
            .setTitle("Вес сеть")
            .setValueType(MdmParamValueType.NUMERIC)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam weightTare = new MdmParam()
            .setId(KnownMdmParams.WEIGHT_TARE)
            .setTitle("Вес тары")
            .setXslName("-")
            .setValueType(MdmParamValueType.NUMERIC)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam imeiControl = new MdmParam()
            .setId(KnownMdmParams.IMEI_CONTROL)
            .setTitle("Требуется контроль imei")
            .setXslName("-")
            .setValueType(MdmParamValueType.MBO_BOOL)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam imeiMask = new MdmParam()
            .setId(KnownMdmParams.IMEI_MASK)
            .setTitle("Маска imei")
            .setXslName("-")
            .setValueType(MdmParamValueType.STRING)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam serialNumberControl = new MdmParam()
            .setId(KnownMdmParams.SERIAL_NUMBER_CONTROL)
            .setTitle("Требуется контроль серийных номеров")
            .setXslName("-")
            .setValueType(MdmParamValueType.MBO_BOOL)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam serialNumberMask = new MdmParam()
            .setId(KnownMdmParams.SERIAL_NUMBER_MASK)
            .setTitle("Маска серийных номеров")
            .setXslName("-")
            .setValueType(MdmParamValueType.STRING)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam hideShelfLife = new MdmParam()
            .setId(KnownMdmParams.HIDE_SHELF_LIFE)
            .setXslName("-")
            .setTitle("Спрятать полку жизни")
            .setValueType(MdmParamValueType.MBO_BOOL)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam hideLifeTime = new MdmParam()
            .setId(KnownMdmParams.HIDE_LIFE_TIME)
            .setXslName("-")
            .setTitle("Утаить жизненное время")
            .setValueType(MdmParamValueType.MBO_BOOL)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam hideGuaranteePeriod = new MdmParam()
            .setId(KnownMdmParams.HIDE_GUARANTEE_PERIOD)
            .setXslName("-")
            .setTitle("Укрывать гарантированное время")
            .setValueType(MdmParamValueType.MBO_BOOL)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));
        MdmParam boxCount = new MdmParam()
            .setId(KnownMdmParams.BOX_COUNT)
            .setXslName("-")
            .setTitle("Коробка считать")
            .setValueType(MdmParamValueType.NUMERIC);
        MdmParam deliveryTime = new MdmParam()
            .setId(KnownMdmParams.DELIVERY_TIME)
            .setXslName("-")
            .setTitle("Время доставки")
            .setValueType(MdmParamValueType.NUMERIC);
        MdmParam minShipment = new MdmParam()
            .setId(KnownMdmParams.MIN_SHIPMENT)
            .setXslName("-")
            .setTitle("Минимальный груз")
            .setValueType(MdmParamValueType.NUMERIC);
        MdmParam quantumOfSupply = new MdmParam()
            .setId(KnownMdmParams.QUANTUM_OF_SUPPLY)
            .setXslName("-")
            .setTitle("Кванты снабжения")
            .setValueType(MdmParamValueType.NUMERIC);
        MdmParam transportUnitSize = new MdmParam()
            .setId(KnownMdmParams.TRANSPORT_UNIT_SIZE)
            .setXslName("-")
            .setTitle("Переместить размер единицы")
            .setValueType(MdmParamValueType.NUMERIC);
        MdmParam quantityInPack = new MdmParam()
            .setId(KnownMdmParams.QUANTITY_IN_PACK)
            .setXslName("-")
            .setTitle("Численность в стае")
            .setValueType(MdmParamValueType.NUMERIC);
        MdmParam manufacturerCountries = new MdmParam()
            .setId(KnownMdmParams.MANUFACTURER_COUNTRY)
            .setXslName("-")
            .setTitle("Производитель стран")
            .setValueType(MdmParamValueType.STRING);
        MdmParam documentRegNumbers = new MdmParam()
            .setId(KnownMdmParams.DOCUMENT_REG_NUMBER)
            .setXslName("-")
            .setTitle("Рег. номер документа")
            .setValueType(MdmParamValueType.STRING);
        MdmParam manufacturer = new MdmParam()
            .setId(KnownMdmParams.MANUFACTURER)
            .setXslName("-")
            .setTitle("Производитель")
            .setValueType(MdmParamValueType.STRING);

        MdmParam price = new MdmParam()
            .setId(KnownMdmParams.PRICE)
            .setXslName("-")
            .setTitle("Капитан Прайс")
            .setValueType(MdmParamValueType.NUMERIC);
        MdmParam preciousGood = new MdmParam()
            .setId(KnownMdmParams.PRECIOUS_GOOD)
            .setXslName("-")
            .setTitle("Дорогостоящее добро")
            .setValueType(MdmParamValueType.MBO_BOOL);
        MdmParam datacampMDVersion = new MdmParam()
            .setId(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION)
            .setXslName("datacampMDVersion")
            .setTitle("Лагерьданных господин данной версии")
            .setValueType(MdmParamValueType.NUMERIC);
        MdmParam isTraceable = new MdmParam()
            .setId(KnownMdmParams.IS_TRACEABLE)
            .setXslName("-")
            .setTitle("big brother is watching you")
            .setValueType(MdmParamValueType.MBO_BOOL);
        MdmParam useInMercury = new MdmParam()
            .setId(KnownMdmParams.USE_IN_MERCURY)
            .setXslName("-")
            .setTitle("использование в ртути")
            .setValueType(MdmParamValueType.BOOL);

        return List.of(
            shelfLife, shelfLifeUnit, shelfLifeComment,
            lifeTime, lifeTimeUnit, lifeTimeComment,
            guaranteePeriod, guaranteePeriodUnit, guaranteePeriodComment,
            vatRate,
            expirDate, expirationDatesApply,
            customsCommCode, customsCommCodePrefix,
            heavyGood, heavyGood20,
            vetisGuids, gtin,
            length, width, height,
            weightGross, weightNet, weightTare,
            imeiControl, imeiMask, serialNumberControl, serialNumberMask,
            hideShelfLife, hideLifeTime, hideGuaranteePeriod,
            boxCount, deliveryTime, minShipment, quantumOfSupply, transportUnitSize, quantityInPack,
            manufacturerCountries, documentRegNumbers, manufacturer,
            price, preciousGood, datacampMDVersion, isTraceable, useInMercury
        );
    }

    public static MdmParam createMdmParam(long mdmParamId,
                                          String paramName,
                                          MdmParamValueType valueType,
                                          List<MdmParamOption> options,
                                          Map<Long, String> optionRenders,
                                          boolean isMultivalue) {
        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setXslName("-")
            .setTitle(paramName)
            .setValueType(valueType)
            .setMultivalue(isMultivalue)
            .setExternals(new MdmParamExternals().setMboParamXslName("-"));

        if (options != null) {
            param.setOptions(options);
        }

        if (optionRenders != null) {
            param.getExternals().setOptionRenders(optionRenders);
        }

        return param;
    }

    @SuppressWarnings("checkstyle:parameterNumber")
    public static MskuParamValue createMskuParamValue(long mdmParamId,
                                                      long mskuId,
                                                      Boolean boolValue,
                                                      Double number,
                                                      String string,
                                                      MdmParamOption option,
                                                      MasterDataSourceType modificationSource,
                                                      Instant updatedTs) {
        var value = new MskuParamValue();
        value.setMskuId(mskuId);
        createMdmParamValue(mdmParamId, boolValue, number, string, option, modificationSource, updatedTs).copyTo(value);
        return value;
    }

    @SuppressWarnings("checkstyle:parameterNumber")
    public static SskuParamValue createSskuParamValue(long mdmParamId,
                                                      ShopSkuKey shopSkuKey,
                                                      Boolean boolValue,
                                                      Double number,
                                                      String string,
                                                      MdmParamOption option,
                                                      MasterDataSourceType modificationSource,
                                                      Instant updatedTs) {
        var value = new SskuParamValue();
        value.setShopSkuKey(shopSkuKey);
        createMdmParamValue(mdmParamId, boolValue, number, string, option, modificationSource, updatedTs).copyTo(value);
        return value;
    }

    @SuppressWarnings("checkstyle:parameterNumber")
    public static SskuParamValue createSskuParamValue(long mdmParamId,
                                                      ShopSkuKey shopSkuKey,
                                                      List<Double> numbers,
                                                      List<String> strings,
                                                      List<MdmParamOption> options,
                                                      MasterDataSourceType modificationSource,
                                                      Instant updatedTs) {
        var value = new SskuParamValue();
        value.setShopSkuKey(shopSkuKey);
        createMdmParamValue(mdmParamId, numbers, strings, options, modificationSource, updatedTs)
            .copyTo(value);
        return value;
    }

    public static SskuParamValue createSskuParamValue(long paramId,
                                                      ShopSkuKey shopSkuKey,
                                                      String xslName,
                                                      Object value
    ) {
        var paramValue = new SskuParamValue();
        paramValue.setMdmParamId(paramId);
        paramValue.setShopSkuKey(shopSkuKey);
        paramValue.setXslName(xslName);


        if (value instanceof Integer) {
            paramValue.setNumeric(new BigDecimal((Integer) value));
        } else if (value instanceof Long) {
            paramValue.setNumeric(new BigDecimal((Long) value));
        } else if (value instanceof String) {
            paramValue.setString((String) value);
        } else if (value instanceof Boolean) {
            paramValue.setBool((Boolean) value);
        } else if (value instanceof MdmParamOption) {
            paramValue.setOption((MdmParamOption) value);
        } else {
            throw new UnsupportedOperationException("Not supported value of type " + value.getClass());
        }

        return paramValue;
    }

    @SuppressWarnings("checkstyle:parameterNumber")
    public static CategoryParamValue createCategoryParamValue(long mdmParamId,
                                                              long categoryId,
                                                              Boolean boolValue,
                                                              Double number,
                                                              String string,
                                                              MdmParamOption option,
                                                              MasterDataSourceType modificationSource,
                                                              Instant updatedTs) {
        var value = new CategoryParamValue();
        value.setCategoryId(categoryId);
        createMdmParamValue(mdmParamId, boolValue, number, string, option, modificationSource, updatedTs).copyTo(value);
        return value;
    }

    @SuppressWarnings("checkstyle:parameterNumber")
    private static MdmParamValue createMdmParamValue(long mdmParamId,
                                                     Boolean boolValue,
                                                     Double number,
                                                     String string,
                                                     MdmParamOption option,
                                                     MasterDataSourceType modificationSource,
                                                     Instant updatedTs) {
        var value = new MdmParamValue();
        value.setMdmParamId(mdmParamId);
        value.setXslName(createDefaultKnownMdmParams().stream()
            .filter(p -> p.getId() == mdmParamId)
            .findFirst()
            .map(MdmParam::getXslName)
            .orElse("-"));

        if (boolValue != null) {
            value.setBool(boolValue);
        }
        if (number != null) {
            value.setNumeric(new BigDecimal(number, MdmProperties.MATH_CONTEXT).stripTrailingZeros());
        }
        if (string != null) {
            value.setString(string);
        }
        if (option != null) {
            value.setOption(option);
        }

        var modifInfo = new MdmModificationInfo();
        modifInfo.setUpdatedTsAndSourceUpdatedTs(updatedTs);
        modifInfo.setMasterDataSourceType(modificationSource);
        value.setModificationInfo(modifInfo);
        return value;
    }

    @SuppressWarnings("checkstyle:parameterNumber")
    private static MdmParamValue createMdmParamValue(long mdmParamId,
                                                     List<Double> numbers,
                                                     List<String> strings,
                                                     List<MdmParamOption> options,
                                                     MasterDataSourceType modificationSource,
                                                     Instant updatedTs) {
        var value = new MdmParamValue();
        value.setMdmParamId(mdmParamId);
        value.setXslName(createDefaultKnownMdmParams().stream()
            .filter(p -> p.getId() == mdmParamId)
            .findFirst()
            .map(MdmParam::getXslName)
            .orElse("-"));

        if (numbers != null) {
            value.setNumerics(
                numbers.stream()
                    .map(num -> new BigDecimal(num, MdmProperties.MATH_CONTEXT).stripTrailingZeros())
                    .collect(Collectors.toList())
            );
        }
        if (strings != null) {
            value.setStrings(strings);
        }
        if (options != null) {
            value.setOptions(options);
        }

        var modifInfo = new MdmModificationInfo();
        modifInfo.setUpdatedTsAndSourceUpdatedTs(updatedTs);
        modifInfo.setMasterDataSourceType(modificationSource);
        value.setModificationInfo(modifInfo);
        return value;
    }

    private static void addExpirationDatesApplyOptionRenders(MdmParam param) {
        Map<Long, String> renders = KnownMdmParams.EXPIRATION_DATES_APPLY_OPTIONS;
        param.getExternals().setOptionRenders(renders);
    }

    private static void addTimeUnitsOptions(MdmParam param) {
        List<MdmParamOption> options = KnownMdmParams.TIME_UNITS_OPTIONS.entrySet().stream()
            .map(entry -> new MdmParamOption(entry.getKey()).setRenderedValue(entry.getValue().toString()))
            .collect(Collectors.toList());
        param.setOptions(options);
    }

    private static void addTimeUnitOptionRenders(MdmParam param) {
        Map<Long, TimeUnit> units = KnownMdmParams.TIME_UNITS_OPTIONS;
        Map<Long, String> renders = new HashMap<>();
        units.forEach((id, unit) -> renders.put(id, unit.toString()));
        param.getExternals().setOptionRenders(renders);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static SskuSilverParamValue createSskuSilverParamValue(long paramId, String xslName,
                                                                  Object value,
                                                                  MasterDataSourceType sourceType, String sourceId,
                                                                  ShopSkuKey shopSkuKey,
                                                                  Long datacampMasterDataVersion,
                                                                  SskuSilverParamValue.SskuSilverTransportType source) {
        var paramValue = new SskuSilverParamValue();
        paramValue.setMdmParamId(paramId);
        paramValue.setXslName(xslName);
        paramValue.setShopSkuKey(shopSkuKey);
        paramValue.setXslName(xslName);
        paramValue.setMasterDataSourceType(sourceType);
        paramValue.setMasterDataSourceId(sourceId);
        paramValue.setDatacampMasterDataVersion(datacampMasterDataVersion);
        paramValue.setSskuSilverTransport(source);

        if (value instanceof Integer) {
            paramValue.setNumeric(new BigDecimal((Integer) value));
        } else if (value instanceof Long) {
            paramValue.setNumeric(new BigDecimal((Long) value));
        } else if (value instanceof String) {
            paramValue.setString((String) value);
        } else if (value instanceof Boolean) {
            paramValue.setBool((Boolean) value);
        } else if (value instanceof MdmParamOption) {
            paramValue.setOption((MdmParamOption) value);
        } else {
            throw new UnsupportedOperationException("Not supported value of type " + value.getClass());
        }

        return paramValue;
    }

    public static MdmParamCacheMock createParamCacheMock() {
        List<MdmParam> defaultParams = createDefaultKnownMdmParams();
        return createParamCacheMock(defaultParams);
    }

    public static MdmParamCacheMock createParamCacheMock(List<MdmParam> params) {
        return new MdmParamCacheMock(params);
    }

    public static MdmLmsCargoTypeCacheMock createCargoTypeCacheMock(List<CargoType> params) {
        return new MdmLmsCargoTypeCacheMock(params);
    }

    public static Map<Long, MdmParamValue> createRandomMdmParamValues(Random random, Collection<MdmParam> mdmParams) {
        return mdmParams.stream()
            .map(param -> createRandomMdmParamValue(random, param))
            .collect(Collectors.toMap(
                MdmParamValue::getMdmParamId,
                Function.identity(),
                (a, b) -> {
                    throw new RuntimeException();
                },
                LinkedHashMap::new
            ));
    }

    public static MdmParamValue createRandomMdmParamValue(Random random, MdmParam mdmParam) {
        int valuesNumber = mdmParam.isMultivalue() ? 1 + random.nextInt(15) : 1;
        MasterDataSourceType sourceType =
            MasterDataSourceType.values()[random.nextInt(MasterDataSourceType.values().length)];
        String sourceId = randomAlphanumericString(random);
        long updatedTs = (1420059600L + random.nextInt(157766400)) * 1000L; // 01.01.2015 - 01.01.2020
        MdmParamValue result = new MdmParamValue()
            .setMdmParamId(mdmParam.getId())
            .setXslName(mdmParam.getXslName())
            .setMasterDataSourceType(sourceType)
            .setMasterDataSourceId(sourceId)
            .setUpdatedTs(Instant.ofEpochMilli(updatedTs));
        switch (mdmParam.getValueType()) {
            case NUMERIC:
                List<BigDecimal> numericValues = random.longs(valuesNumber, 0, 0x100000)
                    .mapToObj(BigDecimal::new)
                    .collect(Collectors.toList());
                result.setNumerics(numericValues);
                break;
            case ENUM:
            case MBO_ENUM:
            case MBO_NUMERIC_ENUM:
                List<MdmParamOption> allAvailableOptions = new ArrayList<>(mdmParam.getOptions());
                int resultingOptionsNumber = Math.min(valuesNumber, allAvailableOptions.size());
                List<MdmParamOption> enumValues = new ArrayList<>();
                while (enumValues.size() < resultingOptionsNumber) {
                    MdmParamOption randomOption =
                        allAvailableOptions.remove(random.nextInt(allAvailableOptions.size()));
                    enumValues.add(randomOption);
                }
                result.setOptions(enumValues);
                break;
            case STRING:
                List<String> stringValues = IntStream.rangeClosed(1, valuesNumber)
                    .mapToObj(i -> randomAlphanumericString(random))
                    .collect(Collectors.toList());
                result.setStrings(stringValues);
                break;
            case MBO_BOOL:
            case BOOL:
                List<Boolean> booleanValues = IntStream.rangeClosed(1, valuesNumber)
                    .mapToObj(i -> random.nextBoolean())
                    .collect(Collectors.toList());
                result.setBools(booleanValues);
                break;
            default:
                throw new RuntimeException();
        }
        return result;
    }

    private static String randomAlphanumericString(Random random) {
        return RandomStringUtils.random(1 + random.nextInt(15), 0, 0, true, true, null, random);
    }

    /**
     * В случае отсутствия префикса ТН ВЭД или разметки в дереве ТН ВЭД, для этих параметров автоматически генерятся
     * false param values. Иногда они очень мешают, и их хочется почистить. Этот метод именно этим и занимается.
     */
    public static <T extends MdmParamValue> List<T> filterCisCargoTypes(Collection<T> paramValues) {
        return paramValues.stream()
            .filter(pv -> !KnownMdmParams.MERCURY_CARGOTYPES.contains(pv.getMdmParamId()))
            .filter(pv -> !KnownMdmParams.HONEST_SIGN_CARGOTYPES.contains(pv.getMdmParamId()))
            .collect(Collectors.toList());
    }

    public static CommonMsku filterCisCargoTypes(CommonMsku commonMsku) {
        return new CommonMsku(commonMsku.getKey(), filterCisCargoTypes(commonMsku.getValues()));
    }

    public static <T extends MdmParamValue> Map<Long, T> fixUnlimitedValues(Map<Long, T> paramValues) {
        KnownMdmParams.TIME_UNIT_BY_VALUE.forEach((valueId, unitId) -> {
            boolean isUnlimited = Optional.of(unitId)
                .map(paramValues::get)
                .flatMap(MdmParamValue::getOption)
                .map(MdmParamOption::getId)
                .map(KnownMdmParams.TIME_UNITS_OPTIONS::get)
                .filter(unit -> unit == TimeUnit.UNLIMITED)
                .isPresent();
            if (isUnlimited && paramValues.containsKey(valueId)) {
                if (KnownMdmParams.NUMERIC_STRING_PARAM_IDS.contains(valueId)) {
                    paramValues.get(valueId).setString("1");
                } else {
                    paramValues.get(valueId).setNumeric(BigDecimal.ONE);
                }
            }
        });
        return paramValues;
    }
}
