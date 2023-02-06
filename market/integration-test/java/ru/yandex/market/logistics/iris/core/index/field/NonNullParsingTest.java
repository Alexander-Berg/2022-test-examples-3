package ru.yandex.market.logistics.iris.core.index.field;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import ru.yandex.market.logistics.iris.core.index.ChangeTrackingReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.ReferenceIndexerTestFactory;
import ru.yandex.market.logistics.iris.core.index.complex.Barcode;
import ru.yandex.market.logistics.iris.core.index.complex.BarcodeSource;
import ru.yandex.market.logistics.iris.core.index.complex.Barcodes;
import ru.yandex.market.logistics.iris.core.index.complex.Dimension;
import ru.yandex.market.logistics.iris.core.index.complex.Dimensions;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.logistics.iris.core.index.JsonMatchers.matchingJson;

@RunWith(BlockJUnit4ClassRunner.class)
public class NonNullParsingTest {

    private static final String LIFETIME_JSON = "{\"lifetime\":{\"value\":15,\"utcTimestamp\":\"2016-01-23T12:34:56\"}}";
    private static final Integer LIFETIME_VALUE = 15;

    private static final String WAD_JSON_VALUE = "12.02";
    private static final String WEIGHT_GROSS_JSON = createWeightsAndDimensionsJson(PredefinedFields.WEIGHT_GROSS_FIELD_NAME, WAD_JSON_VALUE);
    private static final String WEIGHT_NETT_JSON = createWeightsAndDimensionsJson(PredefinedFields.WEIGHT_NETT_FIELD_NAME, WAD_JSON_VALUE);
    private static final String WEIGHT_TARE_JSON = createWeightsAndDimensionsJson(PredefinedFields.WEIGHT_TARE_FIELD_NAME, WAD_JSON_VALUE);

    private static final String BARCODES_JSON = "{\"barcodes\":{\"value\":[" +
        "{\"code\":\"code1\",\"source\":\"PARTNER\",\"type\":\"type1\"}," +
        "{\"code\":\"code2\",\"source\":\"PARTNER\",\"type\":\"type2\"}" +
        "], \"utcTimestamp\":\"2016-01-23T12:34:56\"}}";

    private static final Dimension WAD_VALUE = Dimension.of(new BigDecimal(12.020d).setScale(3, RoundingMode.UP), 30);

    private static final Barcodes BARCODES_VALUE = Barcodes.of(Arrays.asList(
        new Barcode("code1", "type1", BarcodeSource.PARTNER),
        new Barcode("code2", "type2", BarcodeSource.PARTNER)
    ));

    private static final Dimensions DIMENSIONS = new Dimensions(
        Dimension.of(new BigDecimal(12.020d).setScale(3,RoundingMode.UP), 10),
            Dimension.of(new BigDecimal(13.030d).setScale(3, RoundingMode.UP), 20),
            Dimension.of(new BigDecimal(14.040d).setScale(3, RoundingMode.UP), 25)
    );

    private static final String DIMENSION_JSON =
            "{\"dimensions\":" +
                    "{\"value\":" +
                        "{\"width\":{\"value\":12.02,\"data_quality_score\":10}," +
                        "\"height\":{\"value\":13.03,\"data_quality_score\":20}," +
                        "\"length\":{\"value\":14.04,\"data_quality_score\":25}}," +
                        "\"utcTimestamp\":\"2016-01-23T12:34:56\"}}";

    private static String createWeightsAndDimensionsJson(String fieldName, String value) {
        return "{\"" + fieldName + "\":{\"value\":{\"value\":" + value + ", \"data_quality_score\":30}, \"utcTimestamp\":\"2016-01-23T12:34:56\"}}";
    }


    private static final List<Scenario<?>> SCENARIOS = Arrays.asList(
        new Scenario<>(PredefinedFields.LIFETIME_DAYS_FIELD, LIFETIME_VALUE, LIFETIME_JSON),
        new Scenario<>(PredefinedFields.BARCODES, BARCODES_VALUE, BARCODES_JSON),
        new Scenario<>(PredefinedFields.WEIGHT_GROSS, WAD_VALUE, WEIGHT_GROSS_JSON),
        new Scenario<>(PredefinedFields.WEIGHT_NETT, WAD_VALUE, WEIGHT_NETT_JSON),
        new Scenario<>(PredefinedFields.WEIGHT_TARE, WAD_VALUE, WEIGHT_TARE_JSON),
        new Scenario<>(PredefinedFields.DIMENSIONS, DIMENSIONS, DIMENSION_JSON)
    );

    private static final String DIMENSIONS_WITHOUT_DQ_SCORE_JSON =
            "{\"dimensions\":" +
                    "{\"value\":" +
                    "{\"width\":12.020,\"height\":13.03,\"length\":14.040}," +
                    "\"utcTimestamp\":\"2016-01-23T12:34:56\"}}";

    private static final Dimensions DIMENSIONS_WITHOUT_DQ_SCORE = new Dimensions(
            Dimension.of(new BigDecimal(12.020d).setScale(3,RoundingMode.UP)),
            Dimension.of(new BigDecimal(13.030d).setScale(3, RoundingMode.UP)),
            Dimension.of(new BigDecimal(14.040d).setScale(3, RoundingMode.UP))
    );

    private static final String WEIGHT_GROSS_WITHOUT_DQ_SCORE_JSON =
            "{\"weight_gross\":{\"value\":12.020, \"utcTimestamp\":\"2016-01-23T12:34:56\"}}";

    private static final Dimension WAD_WITHOUT_DQ_SCORE_VALUE = Dimension.of(new BigDecimal(12.020d).setScale(3, RoundingMode.UP), 0);

    private static final List<Scenario<?>> SCENARIOS_WITHOUT_DQ_SCORE = Arrays.asList(
            new Scenario<>(PredefinedFields.WEIGHT_GROSS, WAD_WITHOUT_DQ_SCORE_VALUE, WEIGHT_GROSS_WITHOUT_DQ_SCORE_JSON),
            new Scenario<>(PredefinedFields.DIMENSIONS, DIMENSIONS_WITHOUT_DQ_SCORE, DIMENSIONS_WITHOUT_DQ_SCORE_JSON)
    );

    private ChangeTrackingReferenceIndexer referenceIndexer = ReferenceIndexerTestFactory.getIndexer();

    /**
     * Проверяем работу десериализации на основе сценария.
     */
    @Test
    public void deserializeNonNullValue() {
        SCENARIOS.forEach(scenario -> {
            ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(scenario.json);

            assertThat(index.get(scenario.field))
                .as("Executing deserialization scenario for [" + scenario.field + "]")
                .isEqualTo(scenario.value);
        });
    }

    /**
     * Проверяем работу сериализации на основе сценария.
     */
    @Test
    public void serializeNonNullValue() {
        LocalDateTime testDateTime = LocalDateTime.of(2016, 1, 23, 12, 34, 56);
        ZonedDateTime testZonedDateTime = ZonedDateTime.of(testDateTime, UTC);
        SCENARIOS.forEach(scenario -> {
            ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();

            scenario.field.populate(index, scenario.value, testZonedDateTime);

            String jsonIndex = index.toJson(referenceIndexer);

            assertThat(jsonIndex)
                .as("Executing serialization scenario for [" + scenario.field + "]")
                .is(matchingJson(scenario.json));
        });
    }

    /**
     * Проверяем работу сериализации без рейтинга по старому формату.
     */
    @Test
    public void serializeWithoutDataQualityScore() {
        SCENARIOS_WITHOUT_DQ_SCORE.forEach(scenario -> {
            ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(scenario.json);

            assertThat(index.get(scenario.field))
                    .as("Executing deserialization scenario for [" + scenario.field + "]")
                    .isEqualTo(scenario.value);
        });
    }

    private static class Scenario<T> {
        public final ru.yandex.market.logistics.iris.core.index.field.Field<T> field;
        public final T value;
        public final String json;

        private Scenario(Field<T> field, T value, String json) {
            this.field = field;
            this.value = value;
            this.json = json;
        }
    }

}
