package ru.yandex.ir.common.features.extractors.parameters;

import java.util.Arrays;
import java.util.List;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.ir.classify.om.be.RequestType;
import ru.yandex.ir.common.CommonContext;
import ru.yandex.ir.common.be.CommonTextContent;
import ru.yandex.ir.common.features.extractors.Query;
import ru.yandex.ir.common.features.extractors.Text;
import ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.AllSizesExtractor;
import ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.CompatibleUnitsMultipliers;
import ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.NumberWithCompatibleUnitsExtractor;
import ru.yandex.ir.common.features.extractors.parameters.valuesExtractors.ValuesExtractorWithName;
import ru.yandex.ir.common.knowledge.FeaturesExtractor;
import ru.yandex.ir.common.snapshot.SnapshotHolder;
import ru.yandex.ir.common.snapshot.SnapshotRegister;
import ru.yandex.ir.common.snapshot.text.ModelsTextSnapshot;
import ru.yandex.ir.entities.ModelObjectIndex;

public class ParametersFeatureExtractorTest {
    private static final String TITLE_1 = "Противень Демидово МТ043 средний алюминиевый\"";
    private static final String DESCRIPTION_1 = "Размеры: 320х350х36мм<br />\\nсредний<br />\\nобъем - 5л";

    private static final String TITLE_2 = "Противень 320*350*48мм средний МТ-043";
    private static final String DESCRIPTION_2 = "|Описание: Противень 320*350*48мм средний МТ-043";

    private static final String TITLE_3 = "Противень 5l";
    private static final String DESCRIPTION_3 = "";

    private static final Query[] TEST_QUERIES = {
            new Query(RequestType.SKU_LIKE, TITLE_1, DESCRIPTION_1, Arrays.asList(0L, 1L, 2L))
    };

    private static final float FIRST_SECOND_DELTA = 2 * 12f / (36 + 48);

    private static final float[][][] EXPECTED_FEATURES = {
            {
                    {
                            0, 0, 0, 0, 0, 0, 0, 0, 0, // title sizes
                            0, 0, 0, 0, 0, 0, 3, 3, 6,
                            0, 0, 0, 0, 0, 0, 0, 0, 0, // title volumes
                            0, 0, 0, 0, 0, 0, 1, 1, 2
                    },
                    {
                            0, 0, 0, 0, 0, 1, 0, 3, 3,
                            0, FIRST_SECOND_DELTA, FIRST_SECOND_DELTA / 3, FIRST_SECOND_DELTA, FIRST_SECOND_DELTA / 3, 0.5f, 3, 6, 9,
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 1, 1, 0, 1
                    },
                    {
                            0, 0, 0, 0, 0, 0, 0, 0, 0,
                            0, 0, 0, 0, 0, 1, 3, 0, 3,
                            0, 0, 0, 0, 0, 1, 0, 1, 1,
                            0, 0, 0, 0, 0, 0, 1, 1, 2
                    }
            }
    };

    private static final ParametersFeaturesExtractor.Configuration CONFIGURATION = new ParametersFeaturesExtractor.Configuration();
    private static final ValuesExtractorWithName<?>[] EXTRACTORS = {
            new ValuesExtractorWithName<>(new AllSizesExtractor(), "sizes"),
            new ValuesExtractorWithName<>(new NumberWithCompatibleUnitsExtractor(CompatibleUnitsMultipliers.volumes()), "volumes")
    };

    public static int[] getIds() {
        int[] result = new int[EXPECTED_FEATURES.length];
        for (int i = 0; i < EXPECTED_FEATURES.length; ++i) {
            result[i] = i;
        }
        return result;
    }

    @ParameterizedTest
    @MethodSource("getIds")
    public void testExtractFeatures(int id) {
        ParametersFeaturesExtractor.Factory factory = new ParametersFeaturesExtractor.Factory(EXTRACTORS);
        ModelsTextSnapshot snapshot = createSnapshot();
        SnapshotHolder snapshotHolder = new SnapshotHolder();
        snapshotHolder.put(SnapshotRegister.MODELS_TEXT, snapshot);
        FeaturesExtractor<Text> parametersFeaturesExtractor = factory.getInstance(CONFIGURATION, new CommonContext(),
                snapshotHolder);

        Query query = TEST_QUERIES[id];
        float[][] expected = EXPECTED_FEATURES[id];
        List<float[]> actual = parametersFeaturesExtractor.calculateFeatures(query);
        for (int i = 0; i < expected.length; ++i) {
            assertFeatureArraysEqual(expected[i], actual.get(i), i);
        }
    }

    private ModelsTextSnapshot createSnapshot() {
        Long2ObjectOpenHashMap<CommonTextContent> modelsContent = new Long2ObjectOpenHashMap<>();
        modelsContent.put(0, new Text(TITLE_1, DESCRIPTION_1));
        modelsContent.put(1, new Text(TITLE_2, DESCRIPTION_2));
        modelsContent.put(2, new Text(TITLE_3, DESCRIPTION_3));
        ModelObjectIndex<CommonTextContent> index = new ModelObjectIndex<>() {{
            put(RequestType.SKU_LIKE, modelsContent);
        }};
        return new ModelsTextSnapshot(index);
    }

    private void assertFeatureArraysEqual(float[] expected, float[] actual, int arrNum) {
        Assertions.assertArrayEquals(expected, actual, 1e-6f, "DocId: " + arrNum);
    }
}
