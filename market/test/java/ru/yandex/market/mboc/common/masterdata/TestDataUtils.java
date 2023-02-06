package ru.yandex.market.mboc.common.masterdata;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Descriptors;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.AbstractRandomizer;
import io.github.benas.randombeans.randomizers.range.DoubleRangeRandomizer;
import io.github.benas.randombeans.randomizers.range.IntegerRangeRandomizer;
import io.github.benas.randombeans.randomizers.range.LongRangeRandomizer;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.BoxCountValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.DeliveryTimeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.GuaranteePeriodBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.LifeTimeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.MinShipmentBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.QuantumOfSupplyBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.ShelfLifeBlockValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.TransportUnitBlockValidator;
import ru.yandex.market.mbo.mdm.common.util.GtinUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.model.SupplyEvent;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.model.metadata.MdmSource;
import ru.yandex.market.mboc.common.masterdata.parsing.QualityDocumentValidation;
import ru.yandex.market.mboc.common.masterdata.parsing.utils.GeobaseCountryProvider;
import ru.yandex.market.mboc.common.masterdata.services.iris.proto.ProtobufRandomizerRegistry;
import ru.yandex.market.mboc.common.models.HasId;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.http.MboMappings;

/**
 * @author jkt on 07.09.18.
 */
public class TestDataUtils {

    public static final int MAX_STRING_LENGTH = 20;
    private static final int MIN_STRING_LENGTH = 2;
    private static final int MIN_COLLECTION_SIZE = 2;
    private static final int MAX_COLLECTION_SIZE = 30;
    private static final int MIN_PROTO_COLLECTION_SIZE = 1;
    private static final int MAX_PROTO_COLLECTION_SIZE = 4;
    private static final long MIN_LONG = 1L;
    private static final long MAX_LONG = 1000000L;
    private static final int MIN_INTEGER = 1;
    private static final int MAX_INTEGER = 1000000;
    private static final double MIN_DOUBLE = 1.0;
    private static final double MAX_DOUBLE = 100.0;
    private static final int MAX_DATE_RANGE = 1000;
    private static final String VALID_COUNTRY = "Россия";
    private static final int MAX_MODEL_ID = 10000;

    // Valid size is between 10 and 20 cm, density 1g/cm3 -> weight between 1 and 8 kg (also valid)
    private static final BigDecimal SIZE_MIN = new BigDecimal(10); // cm
    private static final BigDecimal SIZE_MAX = new BigDecimal(20); // cm
    private static final double VALID_DENSITY = 1e-9; // 1 g/cm3 = 1e-9 mg/um3
    private static final double WEIGHT_NET_MULTIPLIER = 0.75;

    public static final int GUARANTEE_PERIOD_DAYS_MIN = GuaranteePeriodBlockValidator.DEFAULT_TIME_LIMIT
        .getMinValue().getTimeInUnit(TimeInUnits.TimeUnit.DAY);
    public static final int GUARANTEE_PERIOD_DAYS_MAX = GuaranteePeriodBlockValidator.DEFAULT_TIME_LIMIT
        .getMaxValue().getTimeInUnit(TimeInUnits.TimeUnit.DAY);
    public static final int SHELF_LIFE_DAYS_MIN = ShelfLifeBlockValidator.DEFAULT_TIME_LIMIT
        .getMinValue().getTimeInUnit(TimeInUnits.TimeUnit.DAY);
    public static final int SHELF_LIFE_DAYS_MAX = ShelfLifeBlockValidator.DEFAULT_TIME_LIMIT
        .getMaxValue().getTimeInUnit(TimeInUnits.TimeUnit.DAY);
    public static final int LIFE_TIME_DAYS_MIN = LifeTimeBlockValidator.DEFAULT_TIME_LIMIT
        .getMinValue().getTimeInUnit(TimeInUnits.TimeUnit.DAY);
    public static final int LIFE_TIME_DAYS_MAX = LifeTimeBlockValidator.DEFAULT_TIME_LIMIT
        .getMaxValue().getTimeInUnit(TimeInUnits.TimeUnit.DAY);

    private static final long INTERNAL_SEED = 100500;
    private static final EnhancedRandom INTERNAL_RANDOM = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(INTERNAL_SEED)
        .build();

    private TestDataUtils() {
    }

    private static List<SupplyEvent> generateRandomSupplyEvents(Random random) {
        final int daysInWeek = 7;
        List<Integer> integers = IntStream.rangeClosed(1, daysInWeek).boxed().collect(Collectors.toList());
        Collections.shuffle(integers, random);
        final int count = random.nextInt(1 + integers.size());
        return integers.stream()
            .limit(count)
            .map(DayOfWeek::of)
            .map(SupplyEvent::new)
            .collect(Collectors.toList());
    }

    public static MasterData generateMasterData(ShopSkuKey key, EnhancedRandom random, QualityDocument... documents) {
        return generateMasterData(key.getShopSku(), key.getSupplierId(), random, documents);
    }

    public static MasterData generateMasterData(String shopSku, int supplierId,
                                                EnhancedRandom random, QualityDocument... documents) {
        MasterData masterData = generateValidMasterData(shopSku, supplierId, random);
        masterData.getQualityDocuments().clear();
        masterData.addAllQualityDocuments(Arrays.asList(documents));
        masterData.getManufacturerCountries().clear();
        masterData.addManufacturerCountry(getRandomRuCountryName(random));
        return masterData;
    }

    /**
     * EnhancedRandom random must randomize LocalizedString class.
     */
    public static Model generateValidModel(EnhancedRandom random) {
        Model model = random.nextObject(Model.class, "parameterValues");
        if (model.getId() <= 0) {
            model.setId(1 + random.nextInt(MAX_MODEL_ID));
        }
        return model;
    }

    public static int nextInt(Random random, int min, int max) {
        return min + random.nextInt(max - min);
    }

    public static String nextString(EnhancedRandom random, int minLength, int maxLength) {
        StringBuilder sb = new StringBuilder(random.nextObject(String.class));
        while (sb.length() < maxLength) {
            sb.append(random.nextObject(String.class));
        }
        return sb.substring(0, nextInt(random, minLength, maxLength));
    }

    private static MasterData generateValidMasterData(String shopSku, int supplierId, EnhancedRandom seedRandom) {
        MasterData masterData = TestDataUtils.generate(MasterData.class, seedRandom,
            "categoryId", "qualityDocuments", "regNumbers", "measurementState");
        masterData.setShopSku(shopSku);
        masterData.setSupplierId(supplierId);
        masterData.setSupplySchedule(generateRandomSupplyEvents(seedRandom));

        SplittableRandom random = new SplittableRandom(seedRandom.nextLong());
        masterData.setManufacturerCountries(Collections.singletonList(VALID_COUNTRY));
        masterData.setShelfLife(
            random.nextInt(SHELF_LIFE_DAYS_MIN, SHELF_LIFE_DAYS_MAX),
            TimeInUnits.TimeUnit.DAY
        );

        int guaranteePeriodDays = random.nextInt(GUARANTEE_PERIOD_DAYS_MIN, GUARANTEE_PERIOD_DAYS_MAX);
        int lifeTimeDays = random.nextInt(Math.max(LIFE_TIME_DAYS_MIN, guaranteePeriodDays), LIFE_TIME_DAYS_MAX);
        masterData.setLifeTime(lifeTimeDays, TimeInUnits.TimeUnit.DAY);
        masterData.setGuaranteePeriod(guaranteePeriodDays, TimeInUnits.TimeUnit.DAY);
        masterData.setMinShipment(
            random.nextInt(MinShipmentBlockValidator.MIN_SHIPMENT_MIN, MinShipmentBlockValidator.MIN_SHIPMENT_MAX)
        );
        masterData.setQuantityInPack(
            random.nextInt(TransportUnitBlockValidator.TRANSPORT_UNIT_SIZE_MIN,
                TransportUnitBlockValidator.TRANSPORT_UNIT_SIZE_MAX)
        );
        masterData.setTransportUnitSize(1);
        masterData.setDeliveryTime(
            random.nextInt(DeliveryTimeBlockValidator.DELIVERY_DAYS_MIN, DeliveryTimeBlockValidator.DELIVERY_DAYS_MAX)
        );
        masterData.setBoxCount(
            random.nextInt(BoxCountValidator.BOX_COUNT_MIN, BoxCountValidator.BOX_COUNT_MAX)
        );
        masterData.setQuantumOfSupply(
            random.nextInt(QuantumOfSupplyBlockValidator.QUANTUM_MIN, QuantumOfSupplyBlockValidator.QUANTUM_MAX)
        );
        masterData.setCustomsCommodityCode("1234567890");

        masterData.setItemShippingUnit(generateValidShippingUnit(random));
        masterData.setVetisGuids(Collections.singletonList(seedRandom.nextObject(UUID.class).toString()));
        masterData.setGtins(Collections.singletonList(generateValidGTIN(random)));
        masterData.setSurplusHandleMode(generateValidSurplusHandleMode(random));
        masterData.setCisHandleMode(generateValidCisHandleMode(random));
        masterData.setMeasurementState(generateValidMeasurementState(random));

        return masterData;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    public static String generateValidGTIN(SplittableRandom random) {
        List<Integer> allowedFormats = new ArrayList<>(GtinUtils.GTIN_FORMAT_GRIDS.keySet());
        int formatLength = allowedFormats.get(random.nextInt(allowedFormats.size()));
        long bound = BigInteger.TEN.pow(formatLength - 1).longValue();
        long gtin = random.nextLong(bound, bound * 10);
        gtin = gtin - (gtin % 10);

        int checkSum = GtinUtils.computeChecksum(gtin);
        gtin += checkSum;
        return String.valueOf(gtin);
    }

    private static String getRandomRuCountryName(Random random) {
        ImmutableList<String> strings = GeobaseCountryProvider.readCountryList().asList();
        return strings.get(random.nextInt(strings.size()));
    }

    private static MdmIrisPayload.ShippingUnit generateValidShippingUnit(SplittableRandom random) {
        Long randomWidth = getRandomSize(random);
        Long randomHeight = getRandomSize(random);
        Long randomLength = getRandomSize(random);
        long randomWeightGross = Math.round(VALID_DENSITY * randomWidth * randomHeight * randomLength);
        long randomWeightNet = Math.round(VALID_DENSITY * WEIGHT_NET_MULTIPLIER *
            randomWidth * randomHeight * randomLength);

        return MdmIrisPayload.ShippingUnit.newBuilder()
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(randomWidth))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(randomHeight))
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(randomLength))
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(randomWeightGross))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(randomWeightNet))
            .build();
    }

    private static Long getRandomSize(SplittableRandom random) {
        return random.nextLong(
            SIZE_MIN.divide(MdmProperties.CM_IN_UM, RoundingMode.HALF_UP).longValue(),
            SIZE_MAX.divide(MdmProperties.CM_IN_UM, RoundingMode.HALF_UP).longValue());
    }

    private static MdmIrisPayload.SurplusHandleMode generateValidSurplusHandleMode(SplittableRandom random) {
        var validValues = Arrays.stream(MdmIrisPayload.SurplusHandleMode.values())
            .filter(v -> v != MdmIrisPayload.SurplusHandleMode.UNRECOGNIZED)
            .collect(Collectors.toList());

        return validValues.get(random.nextInt(validValues.size()));
    }

    private static MdmIrisPayload.MeasurementState generateValidMeasurementState(SplittableRandom random) {
        return MdmIrisPayload.MeasurementState.newBuilder()
            .setIsMeasured(true)
            .setLastMeasurementTs(random.nextLong(
                Instant.parse("2007-12-03T10:15:30.00Z").toEpochMilli(),
                Instant.parse("2021-12-03T10:15:30.00Z").toEpochMilli()
            ))
            .build();
    }

    private static MdmIrisPayload.CisHandleMode generateValidCisHandleMode(SplittableRandom random) {
        var validValues = Arrays.stream(MdmIrisPayload.CisHandleMode.values())
            .filter(v -> v != MdmIrisPayload.CisHandleMode.UNRECOGNIZED)
            .collect(Collectors.toList());

        return validValues.get(random.nextInt(validValues.size()));
    }

    public static QualityDocument generateIdenticalDocument(QualityDocument qualityDocument) {
        return qualityDocument.copy().setId(HasId.EMPTY_ID);
    }

    public static QualityDocument generateDocument(EnhancedRandom random) {
        QualityDocument document = random.nextObject(QualityDocument.class);
        document.setId(HasId.EMPTY_ID);
        document.setPictures(new ArrayList<>());
        document.getMetadata().setDeleted(false);
        document.getMetadata().setDeleteDate(null);
        return document;
    }

    public static QualityDocument generateCorrectDocument(EnhancedRandom random) {
        QualityDocument document = random.nextObject(QualityDocument.class);
        document.setId(HasId.EMPTY_ID);
        document.setMetadata(random.nextObject(QualityDocument.Metadata.class));
        document.getMetadata().setDeleted(false);
        document.getMetadata().setDeleteDate(null);

        document.setPictures(Collections.singletonList(random.nextObject(String.class)));
        document.setCustomsCommodityCodes(Collections.singletonList(random.nextObject(String.class)));

        document.setStartDate(LocalDate.now().minusMonths(1).minusDays(random.nextInt(MAX_DATE_RANGE)));
        document.setEndDate(LocalDate.now().plusMonths(1).plusDays(random.nextInt(MAX_DATE_RANGE)));

        QualityDocument.QualityDocumentType docType = random.nextObject(QualityDocument.QualityDocumentType.class);
        if (docType == QualityDocument.QualityDocumentType.EXEMPTION_LETTER) {
            document.setCertificationOrgRegNumber(nextString(
                random,
                QualityDocumentValidation.REGISTRATION_NUMBER_FOR_EXEMPTION_LETTER_MIN_LENGTH,
                MAX_STRING_LENGTH
            ));
            document.setSerialNumber(random.nextObject(String.class));
            document.setRegistrationNumber(document.getCertificationOrgRegNumber() + '_' + document.getSerialNumber());
        } else {
            document.setRegistrationNumber(nextString(
                random,
                QualityDocumentValidation.REGISTRATION_NUMBER_MIN_LENGTH,
                MAX_STRING_LENGTH
            ));
        }
        return document;
    }

    public static EnhancedRandom defaultRandom(long seed) {
        return defaultRandomBuilder(seed).build();
    }

    /**
     * see {@link QualityDocumentValidation}.
     *
     * @param seed
     * @return
     */
    public static EnhancedRandom qualityDocumentsRandom(long seed) {
        Random randomSeed = new Random(seed);
        return internalRandomBuilder(randomSeed)
            .stringLengthRange(QualityDocumentValidation.REGISTRATION_NUMBER_MIN_LENGTH, MAX_STRING_LENGTH)
            .build();
    }

    public static EnhancedRandomBuilder defaultRandomBuilder(long seed) {
        Random randomSeed = new Random(seed);
        EnhancedRandomBuilder randomBuilder = internalRandomBuilder(randomSeed);

        EnhancedRandom innerRandom = randomBuilder
            .collectionSizeRange(MIN_PROTO_COLLECTION_SIZE, MAX_PROTO_COLLECTION_SIZE)
            .build();
        ProtobufRandomizerRegistry protobufRandomizerRegistry = new ProtobufRandomizerRegistry(innerRandom);
        protobufRandomizerRegistry.registerProtoRandomizer(
            f -> f.getName().equals("updated_ts") && f.getJavaType() == Descriptors.FieldDescriptor.JavaType.LONG,
            () -> innerRandom.nextObject(Instant.class).toEpochMilli());
        return internalRandomBuilder(randomSeed)
            .overrideDefaultInitialization(true)
            .registerRandomizerRegistry(protobufRandomizerRegistry);
    }

    private static EnhancedRandomBuilder internalRandomBuilder(Random randomSeed) {
        return EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(randomSeed.nextLong())
            .stringLengthRange(MIN_STRING_LENGTH, MAX_STRING_LENGTH)
            .collectionSizeRange(MIN_COLLECTION_SIZE, MAX_COLLECTION_SIZE)
            .randomize(Long.class, new LongRangeRandomizer(MIN_LONG, MAX_LONG, randomSeed.nextLong()))
            .randomize(Integer.class, new IntegerRangeRandomizer(MIN_INTEGER, MAX_INTEGER, randomSeed.nextLong()))
            .randomize(Double.class, new DoubleRangeRandomizer(MIN_DOUBLE, MAX_DOUBLE, randomSeed.nextLong()))
            .randomize(LocalDateTime.class, (Supplier<LocalDateTime>) () ->
                INTERNAL_RANDOM.nextObject(LocalDateTime.class)
                    .withNano(0)
            )
            .randomize(Instant.class, (Supplier<Instant>) () ->
                INTERNAL_RANDOM.nextObject(Instant.class)
                    .with(ChronoField.NANO_OF_SECOND, 0)
            )
            .randomize(TimeInUnits.class, new TimeInUnitsRandomizer(randomSeed.nextLong()));
    }

    public static <T> T generate(Class<T> clazz, EnhancedRandom random) {
        return random.nextObject(clazz);
    }

    public static <T> T generate(Class<T> clazz, EnhancedRandom random, String... excludedFields) {
        return random.nextObject(clazz, excludedFields);
    }

    public static List<MasterData> generateSskuMsterData(int count, EnhancedRandom random) {
        List<ShopSkuKey> keys = random.objects(ShopSkuKey.class, count).collect(Collectors.toList());
        return keys.stream()
            .map(k -> generateMasterData(k, random, generateDocument(random)))
            .collect(Collectors.toList());
    }

    public static QualityDocument generateFullDocument(EnhancedRandom random) {
        return random.nextObject(QualityDocument.class)
            .setMetadata(new QualityDocument.Metadata()
                .setCreatedBy(random.nextInt())
                .setSource(MdmSource.SUPPLIER));
    }

    public static MboMappings.ApprovedMappingInfo.Builder generateCorrectApprovedMappingInfoBuilder(
        EnhancedRandom random
    ) {
        return MboMappings.ApprovedMappingInfo.newBuilder()
            .setSupplierId(random.nextInt())
            .setShopSku(random.nextObject(String.class))
            .setMarketCategoryId(random.nextLong())
            .setMarketSkuId(random.nextLong());
    }

    public static SilverCommonSsku wrapSilver(SskuSilverParamValue value) {
        return new SilverCommonSsku(value.getSilverSskuKey())
            .addBaseValue(value);
    }

    public static SilverCommonSsku wrapSilver(Collection<SskuSilverParamValue> values) {
        return new SilverCommonSsku(values.iterator().next().getSilverSskuKey())
            .addBaseValues(values);
    }

    private static class TimeInUnitsRandomizer extends AbstractRandomizer<TimeInUnits> {

        TimeInUnitsRandomizer(long seed) {
            super(seed);
        }

        @Override
        public TimeInUnits getRandomValue() {
            int time = super.random.nextInt(Integer.MAX_VALUE);
            TimeInUnits.TimeUnit[] timeUnits = TimeInUnits.TimeUnit.values();
            TimeInUnits.TimeUnit timeUnit = timeUnits[random.nextInt(timeUnits.length)];
            return new TimeInUnits(time, timeUnit);
        }
    }
}
