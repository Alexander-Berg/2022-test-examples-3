package ru.yandex.market.mbo.mdm.common.service.bmdm;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.io.Resources;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.commons.lang.RandomStringUtils;
import org.assertj.core.api.Assertions;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmEntityTypeWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.MetadataProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util.I18nStringUtils;
import ru.yandex.market.mdm.http.MdmAttributeValue;
import ru.yandex.market.mdm.http.MdmAttributeValues;
import ru.yandex.market.mdm.http.MdmBase;
import ru.yandex.market.mdm.http.MdmEntity;
import ru.yandex.market.mdm.http.external_references.MdmExternalReferenceList;

public class TestBmdmUtils {
    //VGH
    public static final long VGH_ENTITY_TYPE_ID = 35307;
    public static final long LENGTH_ATTRIBUTE_ID = 35347;
    public static final long WIDTH_ATTRIBUTE_ID = 35432;
    public static final long HEIGHT_ATTRIBUTE_ID = 35309;
    public static final long WEIGHT_GROSS_ATTRIBUTE_ID = 35434;
    public static final long WEIGHT_NET_ATTRIBUTE_ID = 35436;
    public static final long WEIGHT_TARE_ATTRIBUTE_ID = 35438;
    public static final MdmBase.MdmEntityType VGH_ENTITY_TYPE = createDefaultVghEntityType();
    //TIME
    public static final long TIME_ENTITY_TYPE_ID = 35430;
    public static final long TIME_VALUE_ATTRIBUTE_ID = 35440;
    public static final MdmBase.MdmAttribute TIME_ENTITY_VALUE_ATTRIBUTE = createTimeEntityValueAttribute();
    public static final long TIME_UNIT_ATTRIBUTE_ID = 35311;
    public static final MdmBase.MdmAttribute TIME_UNIT_ATTRIBUTE = timeUnitAttribute();
    public static final long TIME_UNIT_YEAR_OPTION_ID = 35387;
    public static final long TIME_UNIT_HOUR_OPTION_ID = 35351;
    public static final long TIME_UNIT_MONTH_OPTION_ID = 35443;
    public static final long TIME_UNIT_WEEK_OPTION_ID = 35350;
    public static final long TIME_UNIT_DAY_OPTION_ID = 35444;
    public static final long TIME_UNIT_UNLIMITED_OPTION_ID = 155704716;
    public static final long TIME_COMMENT_ATTRIBUTE_ID = 35353;
    public static final long TIME_UNLIMITED_ATTRIBUTE_ID = 35526;
    public static final MdmBase.MdmAttribute TIME_UNLIMITED_ATTRIBUTE = timeUnlimitedAttribute();
    public static final long TIME_HIDDEN_ATTRIBUTE_ID = 35355L;
    public static final MdmBase.MdmAttribute TIME_HIDDEN_ATTRIBUTE = timeHiddenAttribute();
    public static final MdmBase.MdmEntityType TIME_ENTITY_TYPE = createDefaultTimeEntityType();
    //GOLDEN MSKU
    public static final long GOLD_MSKU_ID_ATTRIBUTE_ID = 35428;
    public static final long GOLD_MSKU_VGH_ATTRIBUTE_ID = 35288;
    public static final long GOLD_MSKU_SHELF_LIFE_ATTRIBUTE_ID = 35359;
    public static final MdmBase.MdmAttribute GOLD_MSKU_SHELF_LIFE_ATTRIBUTE = goldMskuShelfLifeAttribute();
    public static final long GOLD_MSKU_LIFE_TIME_ATTRIBUTE_ID = 35361;
    public static final long GOLD_MSKU_GUARANTEE_PERIOD_ATTRIBUTE_ID = 35528;
    public static final MdmBase.MdmEntityType GOLDEN_MSKU_ENTITY_TYPE = createGoldMskuEntityType();
    public static final MdmBase.MdmEntityType FLAT_GOLD_MSKU_ENTITY_TYPE =
        parseEntityTypeFromJsonFile("bmdm-entities/FlatGoldMsku.json");
    public static final MdmBase.MdmEntityType GOLDEN_SSKU_RESOLUTION_ET =
        parseEntityTypeFromJsonFile("bmdm-entities/golden_ssku_resolution.json");
    public static final MdmBase.MdmEntityType SILVER_SSKU_RESOLUTION_ET =
        parseEntityTypeFromJsonFile("bmdm-entities/silver_ssku_resolution.json");
    public static final MdmBase.MdmEntityType VERDICT_ET =
        parseEntityTypeFromJsonFile("bmdm-entities/verdict.json");
    public static final List<MdmBase.MdmExternalReference> GOLD_MSKU_EXTERNAL_REFERENCES =
        parseExternalReferencesFromJsonFile("bmdm-entities/GoldMskuReferences.json");
    public static final List<MdmBase.MdmExternalReference> FLAT_GOLD_MSKU_EXTERNAL_REFERENCES =
        parseExternalReferencesFromJsonFile("bmdm-entities/FlatGoldMskuReferences.json");

    private TestBmdmUtils() {
    }

    /**
     * Удобно получить из БМДМ через ручку (например так https://paste.yandex-team.ru/5383435, CallMdmGrpcTool)
     * или скопировать из mdm.mdm_entity_type_projection.
     */
    @SuppressWarnings("UnstableApiUsage")
    public static MdmBase.MdmEntityType parseEntityTypeFromJsonFile(String path) {
        try {
            URL url = Resources.getResource(path);
            String jsonAsString = Resources.toString(url, StandardCharsets.UTF_8);
            MdmEntityTypeWrapper entityTypeWrapper = new MdmEntityTypeWrapper();
            entityTypeWrapper.setMdmEntityTypeFromRep(jsonAsString);
            return entityTypeWrapper.getMdmEntityType();
        } catch (Exception e) {
            throw new RuntimeException("Error while loading entity type from file. Path: %.", e);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static List<MdmBase.MdmExternalReference> parseExternalReferencesFromJsonFile(String path) {
        try {
            URL url = Resources.getResource(path);
            MdmExternalReferenceList.Builder builder = MdmExternalReferenceList.newBuilder();
            JsonFormat.merge(new InputStreamReader(url.openStream()), builder);
            return builder.getMdmExternalReferencesList();
        } catch (Exception e) {
            throw new RuntimeException("Error while loading entity type from file. Path: %.", e);
        }
    }

    public static Optional<MdmEntity> createFullRandomEntity(long entityTypeId,
                                                             Random random,
                                                             MetadataProvider metadataProvider) {
        return createFullRandomEntity(entityTypeId, random, metadataProvider, Map.of());
    }

    public static Optional<MdmEntity> createFullRandomEntity(
        long entityTypeId,
        Random random,
        MetadataProvider metadataProvider,
        Map<Long, BiFunction<Random, MetadataProvider, Optional<MdmEntity>>> customStructCreators
    ) {
        if (customStructCreators.containsKey(entityTypeId)) {
            return customStructCreators.get(entityTypeId).apply(random, metadataProvider);
        }
        return metadataProvider.findEntityType(entityTypeId)
            .map(MdmBase.MdmEntityType::getAttributesList)
            .map(attributes -> attributes.stream()
                .map(MdmBase.MdmAttribute::getMdmId)
                .map(attributeId ->
                    createRandomAttributeValues(attributeId, random, metadataProvider, customStructCreators))
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(MdmAttributeValues::getMdmAttributeId, Function.identity())))
            .map(attributeValues -> MdmEntity.newBuilder()
                .setMdmEntityTypeId(entityTypeId)
                .putAllMdmAttributeValues(attributeValues)
                .build());
    }

    public static Optional<MdmAttributeValues> createRandomAttributeValues(
        long attributeId,
        Random random,
        MetadataProvider metadataProvider
    ) {
        return createRandomAttributeValues(attributeId, random, metadataProvider, Map.of());
    }

    public static Optional<MdmAttributeValues> createRandomAttributeValues(
        long attributeId,
        Random random,
        MetadataProvider metadataProvider,
        Map<Long, BiFunction<Random, MetadataProvider, Optional<MdmEntity>>> customStructCreators
    ) {
        return metadataProvider.findAttribute(attributeId)
            .map(attribute -> {
                int valuesNumber = attribute.getIsMultivalue() ? 1 + random.nextInt(15) : 1;
                MasterDataSourceType sourceType =
                    MasterDataSourceType.values()[random.nextInt(MasterDataSourceType.values().length)];
                String sourceId = randomAlphanumericString(random);
                long from = (1420059600L + random.nextInt(157766400)) * 1000L; // 01.01.2015 - 01.01.2020
                switch (attribute.getDataType()) {
                    case INT64:
                        List<Long> longs = random.longs(valuesNumber, 1, 0x10000) // [1, 65536)
                            .boxed()
                            .collect(Collectors.toList());
                        return createInt64Values(attributeId, longs, sourceType.name(), sourceId, from);
                    case STRING:
                        List<String> strings = IntStream.rangeClosed(1, valuesNumber)
                            .mapToObj(i -> randomAlphanumericString(random))
                            .collect(Collectors.toList());
                        return createStringValues(attributeId, strings, sourceType.name(), sourceId, from);
                    case NUMERIC:
                        List<String> numerics = random.doubles(valuesNumber)
                            .mapToObj(BigDecimal::valueOf)
                            .map(BigDecimal::toPlainString)
                            .collect(Collectors.toList());
                        return createNumericValues(attributeId, numerics, sourceType.name(), sourceId, from);
                    case BOOLEAN:
                        List<Boolean> booleans = IntStream.rangeClosed(1, valuesNumber)
                            .mapToObj(i -> random.nextBoolean())
                            .collect(Collectors.toList());
                        return createBooleanValues(attributeId, booleans, sourceType.name(), sourceId, from);
                    case ENUM:
                        List<Long> options = random.ints(valuesNumber, 0, attribute.getOptionsCount())
                            .mapToObj(attribute::getOptions)
                            .map(MdmBase.MdmEnumOption::getMdmId)
                            .collect(Collectors.toList());
                        return createEnumValues(attributeId, options, sourceType.name(), sourceId, from);
                    case STRUCT:
                        long structTypeId = attribute.getStructTypeId();
                        List<MdmEntity> entities = IntStream.rangeClosed(1, valuesNumber)
                            .mapToObj(i ->
                                createFullRandomEntity(structTypeId, random, metadataProvider, customStructCreators))
                            .flatMap(Optional::stream)
                            .collect(Collectors.toList());
                        return createStructValues(attributeId, entities);
                    default:
                        return MdmAttributeValues.getDefaultInstance();
                }
            })
            .filter(attributeValues -> attributeValues.getValuesCount() > 0);
    }

    private static String randomAlphanumericString(Random random) {
        return RandomStringUtils.random(1 + random.nextInt(15), 0, 0, true, true, null, random);
    }

    public static MdmAttributeValues createSingleStructValue(long attributeId,
                                                             MdmEntity mdmEntity) {
        return createStructValues(attributeId, List.of(mdmEntity));
    }

    public static MdmAttributeValues createStructValues(long attributeId,
                                                        Collection<MdmEntity> entities) {
        List<MdmAttributeValue> values = entities.stream()
            .map(entity -> MdmAttributeValue.newBuilder().setStruct(entity).build())
            .collect(Collectors.toList());
        return MdmAttributeValues.newBuilder()
            .setMdmAttributeId(attributeId)
            .addAllValues(values)
            .build();
    }

    public static MdmAttributeValues createSingleInt64Value(long attributeId,
                                                            long value,
                                                            String sourceType,
                                                            String sourceId,
                                                            long from) {
        return createInt64Values(attributeId, List.of(value), sourceType, sourceId, from);
    }

    public static MdmAttributeValues createInt64Values(long attributeId,
                                                       Collection<Long> values,
                                                       String sourceType,
                                                       String sourceId,
                                                       long from) {
        return createValues(attributeId, values, MdmAttributeValue.Builder::setInt64, sourceType, sourceId, from);
    }

    public static MdmAttributeValues createSingleBooleanValue(long attributeId,
                                                              boolean value,
                                                              String sourceType,
                                                              String sourceId,
                                                              long from) {
        return createBooleanValues(attributeId, List.of(value), sourceType, sourceId, from);
    }

    public static MdmAttributeValues createBooleanValues(long attributeId,
                                                         Collection<Boolean> values,
                                                         String sourceType,
                                                         String sourceId,
                                                         long from) {
        return createValues(attributeId, values, MdmAttributeValue.Builder::setBool, sourceType, sourceId, from);
    }

    public static MdmAttributeValues createSingleStringValue(long attributeId,
                                                             String value,
                                                             String sourceType,
                                                             String sourceId,
                                                             long from) {
        return createStringValues(attributeId, List.of(value), sourceType, sourceId, from);
    }

    public static MdmAttributeValues createStringValues(long attributeId,
                                                        Collection<String> values,
                                                        String sourceType,
                                                        String sourceId,
                                                        long from) {
        List<MdmBase.I18nStrings> i18nStrings = values.stream()
            .map(I18nStringUtils::fromSingleRuString)
            .collect(Collectors.toList());
        return createValues(attributeId, i18nStrings, MdmAttributeValue.Builder::setString, sourceType, sourceId, from);
    }

    public static MdmAttributeValues createSingleEnumValue(long attributeId,
                                                           long optionId,
                                                           String sourceType,
                                                           String sourceId,
                                                           long from) {
        return createEnumValues(attributeId, List.of(optionId), sourceType, sourceId, from);
    }

    public static MdmAttributeValues createEnumValues(long attributeId,
                                                      Collection<Long> optionIds,
                                                      String sourceType,
                                                      String sourceId,
                                                      long from) {
        return createValues(attributeId, optionIds, MdmAttributeValue.Builder::setOption, sourceType, sourceId, from);
    }

    public static MdmAttributeValues createSingleNumericValue(long attributeId,
                                                              String value,
                                                              String sourceType,
                                                              String sourceId,
                                                              long from) {
        return createNumericValues(attributeId, List.of(value), sourceType, sourceId, from);
    }

    public static MdmAttributeValues createNumericValues(long attributeId,
                                                         Collection<String> values,
                                                         String sourceType,
                                                         String sourceId,
                                                         long from) {
        return createValues(attributeId, values, MdmAttributeValue.Builder::setNumeric, sourceType, sourceId, from);
    }

    public static <T> MdmAttributeValues createValues(long attributeId,
                                                      Collection<T> values,
                                                      BiConsumer<MdmAttributeValue.Builder, T> valueSetter,
                                                      String sourceType,
                                                      String sourceId,
                                                      long from) {
        List<MdmAttributeValue> attributeValues = values.stream()
            .map(value -> {
                MdmAttributeValue.Builder builder = MdmAttributeValue.newBuilder();
                valueSetter.accept(builder, value);
                return builder.build();
            }).collect(Collectors.toList());

        return MdmAttributeValues.newBuilder()
            .setMdmAttributeId(attributeId)
            .addAllValues(attributeValues)
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder()
                .setFrom(from)
                .build())
            .setMdmSourceMeta(MdmBase.MdmSourceMeta.newBuilder()
                .setSourceType(sourceType)
                .setSourceId(sourceId)
                .build())
            .build();
    }

    private static MdmBase.MdmEntityType createDefaultVghEntityType() {
        MdmBase.MdmAttribute length = MdmBase.MdmAttribute.newBuilder()
            .setMdmId(LENGTH_ATTRIBUTE_ID)
            .setMdmEntityTypeId(VGH_ENTITY_TYPE_ID)
            .setInternalName("length_cm")
            .setDataType(MdmBase.MdmAttribute.DataType.NUMERIC)
            .setIsMultivalue(false)
            .setRuTitle("Длина, см")
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();

        MdmBase.MdmAttribute width = MdmBase.MdmAttribute.newBuilder()
            .setMdmId(WIDTH_ATTRIBUTE_ID)
            .setMdmEntityTypeId(VGH_ENTITY_TYPE_ID)
            .setInternalName("width_cm")
            .setDataType(MdmBase.MdmAttribute.DataType.NUMERIC)
            .setIsMultivalue(false)
            .setRuTitle("Ширина, см")
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();

        MdmBase.MdmAttribute height = MdmBase.MdmAttribute.newBuilder()
            .setMdmId(HEIGHT_ATTRIBUTE_ID)
            .setMdmEntityTypeId(VGH_ENTITY_TYPE_ID)
            .setInternalName("height_cm")
            .setDataType(MdmBase.MdmAttribute.DataType.NUMERIC)
            .setIsMultivalue(false)
            .setRuTitle("Высота, см")
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();

        MdmBase.MdmAttribute weightGross = MdmBase.MdmAttribute.newBuilder()
            .setMdmId(WEIGHT_GROSS_ATTRIBUTE_ID)
            .setMdmEntityTypeId(VGH_ENTITY_TYPE_ID)
            .setInternalName("weight_gross_kg")
            .setDataType(MdmBase.MdmAttribute.DataType.NUMERIC)
            .setIsMultivalue(false)
            .setRuTitle("Вес брутто, кг")
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();

        MdmBase.MdmAttribute weightNet = MdmBase.MdmAttribute.newBuilder()
            .setMdmId(WEIGHT_NET_ATTRIBUTE_ID)
            .setMdmEntityTypeId(VGH_ENTITY_TYPE_ID)
            .setInternalName("weight_net_kg")
            .setDataType(MdmBase.MdmAttribute.DataType.NUMERIC)
            .setIsMultivalue(false)
            .setRuTitle("Вес нетто, кг")
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();

        MdmBase.MdmAttribute weightTare = MdmBase.MdmAttribute.newBuilder()
            .setMdmId(WEIGHT_TARE_ATTRIBUTE_ID)
            .setMdmEntityTypeId(VGH_ENTITY_TYPE_ID)
            .setInternalName("weight_tare_kg")
            .setDataType(MdmBase.MdmAttribute.DataType.NUMERIC)
            .setIsMultivalue(false)
            .setRuTitle("Вес тары, кг")
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();

        return MdmBase.MdmEntityType.newBuilder()
            .setMdmId(VGH_ENTITY_TYPE_ID)
            .setInternalName("vgh")
            .setMdmEntityKind(MdmBase.MdmEntityType.EntityKind.STRUCT)
            .setRuTitle("Весогабаритные характеристики в сантиметрах и килограммах.")
            .addAttributes(length)
            .addAttributes(width)
            .addAttributes(height)
            .addAttributes(weightGross)
            .addAttributes(weightNet)
            .addAttributes(weightTare)
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();
    }

    private static MdmBase.MdmEntityType createDefaultTimeEntityType() {
        MdmBase.MdmAttribute comment = MdmBase.MdmAttribute.newBuilder()
            .setMdmId(TIME_COMMENT_ATTRIBUTE_ID)
            .setMdmEntityTypeId(TIME_ENTITY_TYPE_ID)
            .setInternalName("comment")
            .setDataType(MdmBase.MdmAttribute.DataType.STRING)
            .setIsMultivalue(false)
            .setRuTitle("Комментарий")
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();

        return MdmBase.MdmEntityType.newBuilder()
            .setMdmId(TIME_ENTITY_TYPE_ID)
            .setInternalName("time_with_units")
            .setMdmEntityKind(MdmBase.MdmEntityType.EntityKind.STRUCT)
            .setRuTitle("Срок (гарантии, службы, годности) с указанием единиц измерения и допускающий спецзначения," +
                "например,\"не ограничен\", видимость и комментарии.")
            .addAttributes(createTimeEntityValueAttribute())
            .addAttributes(timeUnitAttribute())
            .addAttributes(comment)
            .addAttributes(timeUnlimitedAttribute())
            .addAttributes(timeHiddenAttribute())
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();
    }

    private static MdmBase.MdmAttribute createTimeEntityValueAttribute() {
        return MdmBase.MdmAttribute.newBuilder()
            .setMdmId(TIME_VALUE_ATTRIBUTE_ID)
            .setMdmEntityTypeId(TIME_ENTITY_TYPE_ID)
            .setInternalName("time_value")
            .setDataType(MdmBase.MdmAttribute.DataType.NUMERIC)
            .setIsMultivalue(false)
            .setRuTitle("Значение")
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();
    }

    public static MdmBase.MdmAttribute timeUnitAttribute() {
        return MdmBase.MdmAttribute.newBuilder()
            .setMdmId(TIME_UNIT_ATTRIBUTE_ID)
            .setMdmEntityTypeId(TIME_ENTITY_TYPE_ID)
            .setInternalName("time_unit")
            .setDataType(MdmBase.MdmAttribute.DataType.ENUM)
            .addOptions(MdmBase.MdmEnumOption.newBuilder()
                .setMdmAttributeId(TIME_UNIT_ATTRIBUTE_ID)
                .setMdmId(TIME_UNIT_HOUR_OPTION_ID)
                .setValue("часы")
                .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
                .build())
            .addOptions(MdmBase.MdmEnumOption.newBuilder()
                .setMdmAttributeId(TIME_UNIT_ATTRIBUTE_ID)
                .setMdmId(TIME_UNIT_DAY_OPTION_ID)
                .setValue("дни")
                .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
                .build())
            .addOptions(MdmBase.MdmEnumOption.newBuilder()
                .setMdmAttributeId(TIME_UNIT_ATTRIBUTE_ID)
                .setMdmId(TIME_UNIT_WEEK_OPTION_ID)
                .setValue("недели")
                .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
                .build())
            .addOptions(MdmBase.MdmEnumOption.newBuilder()
                .setMdmAttributeId(TIME_UNIT_ATTRIBUTE_ID)
                .setMdmId(TIME_UNIT_MONTH_OPTION_ID)
                .setValue("месяцы")
                .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
                .build())
            .addOptions(MdmBase.MdmEnumOption.newBuilder()
                .setMdmAttributeId(TIME_UNIT_ATTRIBUTE_ID)
                .setMdmId(TIME_UNIT_YEAR_OPTION_ID)
                .setValue("годы")
                .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
                .build())
            .addOptions(MdmBase.MdmEnumOption.newBuilder()
                .setMdmAttributeId(TIME_UNIT_ATTRIBUTE_ID)
                .setMdmId(TIME_UNIT_UNLIMITED_OPTION_ID)
                .setValue("не ограничен")
                .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
                .build())
            .setIsMultivalue(false)
            .setRuTitle("Единица измерения")
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();
    }

    public static MdmBase.MdmAttribute timeUnlimitedAttribute() {
        return MdmBase.MdmAttribute.newBuilder()
            .setMdmId(TIME_UNLIMITED_ATTRIBUTE_ID)
            .setMdmEntityTypeId(TIME_ENTITY_TYPE_ID)
            .setInternalName("is_unlimited")
            .setDataType(MdmBase.MdmAttribute.DataType.BOOLEAN)
            .setIsMultivalue(false)
            .setRuTitle("Не ограничен")
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();
    }

    public static MdmBase.MdmAttribute timeHiddenAttribute() {
        return MdmBase.MdmAttribute.newBuilder()
            .setMdmId(TIME_HIDDEN_ATTRIBUTE_ID)
            .setMdmEntityTypeId(TIME_ENTITY_TYPE_ID)
            .setInternalName("is_hidden")
            .setDataType(MdmBase.MdmAttribute.DataType.BOOLEAN)
            .setIsMultivalue(false)
            .setRuTitle("Скрыт")
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();
    }

    public static MdmBase.MdmEntityType createGoldMskuEntityType() {
        MdmBase.MdmAttribute mskuId = MdmBase.MdmAttribute.newBuilder()
            .setMdmId(GOLD_MSKU_ID_ATTRIBUTE_ID)
            .setMdmEntityTypeId(KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID)
            .setInternalName("msku_id")
            .setDataType(MdmBase.MdmAttribute.DataType.INT64)
            .setIsMultivalue(false)
            .setRuTitle("MSKU ID")
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();

        MdmBase.MdmAttribute vgh = MdmBase.MdmAttribute.newBuilder()
            .setMdmId(GOLD_MSKU_VGH_ATTRIBUTE_ID)
            .setMdmEntityTypeId(KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID)
            .setInternalName("msku_vgh")
            .setDataType(MdmBase.MdmAttribute.DataType.STRUCT)
            .setIsMultivalue(false)
            .setRuTitle("ВГХ")
            .setStructTypeId(VGH_ENTITY_TYPE_ID)
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();

        MdmBase.MdmAttribute lifeTime = MdmBase.MdmAttribute.newBuilder()
            .setMdmId(GOLD_MSKU_LIFE_TIME_ATTRIBUTE_ID)
            .setMdmEntityTypeId(KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID)
            .setInternalName("life_time")
            .setDataType(MdmBase.MdmAttribute.DataType.STRUCT)
            .setIsMultivalue(false)
            .setRuTitle("Срок службы")
            .setStructTypeId(TIME_ENTITY_TYPE_ID)
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();

        MdmBase.MdmAttribute guaranteePeriod = MdmBase.MdmAttribute.newBuilder()
            .setMdmId(GOLD_MSKU_GUARANTEE_PERIOD_ATTRIBUTE_ID)
            .setMdmEntityTypeId(KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID)
            .setInternalName("warranty_period")
            .setDataType(MdmBase.MdmAttribute.DataType.STRUCT)
            .setIsMultivalue(false)
            .setRuTitle("Срок гарантии")
            .setStructTypeId(TIME_ENTITY_TYPE_ID)
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();

        return MdmBase.MdmEntityType.newBuilder()
            .setMdmId(KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID)
            .setInternalName("gold_msku")
            .setMdmEntityKind(MdmBase.MdmEntityType.EntityKind.SERVICE)
            .setRuTitle("Золотая MSKU")
            .setDescription("Вычисленная наилучшая MSKU")
            .addAttributes(mskuId)
            .addAttributes(vgh)
            .addAttributes(goldMskuShelfLifeAttribute())
            .addAttributes(lifeTime)
            .addAttributes(guaranteePeriod)
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();
    }

    private static MdmBase.MdmAttribute goldMskuShelfLifeAttribute() {
        return MdmBase.MdmAttribute.newBuilder()
            .setMdmId(GOLD_MSKU_SHELF_LIFE_ATTRIBUTE_ID)
            .setMdmEntityTypeId(KnownBmdmIds.GOLD_MSKU_ENTITY_TYPE_ID)
            .setInternalName("shelf_life")
            .setDataType(MdmBase.MdmAttribute.DataType.STRUCT)
            .setIsMultivalue(false)
            .setRuTitle("Срок годности")
            .setStructTypeId(TIME_ENTITY_TYPE_ID)
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(1627996414))
            .build();
    }

    public static MdmEntity clearMdmIdAndUpdateMeta(MdmEntity mdmEntity) {
        return mdmEntity.toBuilder()
            .clearMdmId()
            .clearMdmUpdateMeta()
            .build();
    }

    public static void assertEqualsWithoutMdmIdAndUpdateMeta(MdmEntity actual, MdmEntity expected) {
        Assertions.assertThat(clearMdmIdAndUpdateMeta(actual))
            .isEqualTo(clearMdmIdAndUpdateMeta(expected));
    }

    public static CommonMsku removeBmdmIdAndVersion(CommonMsku msku) {
        return new CommonMsku(
            msku.getKey(),
            msku.getValues().stream()
                .filter(pv -> pv.getMdmParamId() != KnownMdmParams.BMDM_ID)
                .collect(Collectors.toList())
        );
    }

    public static SilverCommonSsku removeBmdmIdAndVersion(SilverCommonSsku silverCommonSsku) {
        SilverServiceSsku newBaseValue = (SilverServiceSsku) new SilverServiceSsku(silverCommonSsku.getBusinessKey())
            .setMasterDataVersion(silverCommonSsku.getBaseSsku().getMasterDataVersion())
            .setParamValues(silverCommonSsku.getBaseValues().stream()
                .filter(pv -> pv.getMdmParamId() != KnownMdmParams.BMDM_ID)
                .collect(Collectors.toList()));

        return new SilverCommonSsku(silverCommonSsku.getBusinessKey())
            .setBaseSsku(newBaseValue)
            .putServiceSskus(silverCommonSsku.getServiceSskus().values());
    }

    public static CommonSsku removeBmdmIdAndVersion(CommonSsku commonSsku) {
        List<SskuParamValue> filteredBaseValues = commonSsku.getBaseValues().stream()
            .filter(pv -> pv.getMdmParamId() != KnownMdmParams.BMDM_ID)
            .collect(Collectors.toList());

        return new CommonSsku(commonSsku.getKey())
            .setBaseValues(filteredBaseValues)
            .setBaseVerdict(commonSsku.getBaseVerdict())
            .setBasePartnerVerdict(commonSsku.getBasePartnerVerdict())
            .setMasterDataVersion(commonSsku.getMasterDataVersion())
            .putServiceSskus(commonSsku.getServiceSskus().values());
    }

    public static MdmEntity recursivelyUpdateExistingSourceMeta(MdmEntity mdmEntity, MdmBase.MdmSourceMeta sourceMeta) {
        Map<Long, MdmAttributeValues> existingValues = mdmEntity.getMdmAttributeValuesMap();
        Map<Long, MdmAttributeValues> newValues = new LinkedHashMap<>();

        existingValues.forEach((attrId, values) -> {
            MdmAttributeValues.Builder builder = values.toBuilder().clearValues();
            for (MdmAttributeValue value: values.getValuesList()) {
                if (value.hasStruct()) {
                    MdmEntity updatedStruct = recursivelyUpdateExistingSourceMeta(value.getStruct(), sourceMeta);
                    builder.addValues(value.toBuilder().setStruct(updatedStruct).build());
                } else {
                    builder.addValues(value);
                }
            }
            if (values.hasMdmSourceMeta()) {
                builder.setMdmSourceMeta(sourceMeta);
            }
            newValues.put(attrId, builder.build());
        });

        return mdmEntity.toBuilder()
            .clearMdmAttributeValues()
            .putAllMdmAttributeValues(newValues)
            .build();
    }
}
