package ru.yandex.ir.common.features.extractors.articles;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.junit.jupiter.api.Test;

import ru.yandex.ir.classify.om.be.RequestType;
import ru.yandex.ir.common.features.extractors.Query;
import ru.yandex.ir.common.snapshot.text.ModelsTextSnapshot;
import ru.yandex.ir.common.snapshot.text.TokenizedModelSnapshot;
import ru.yandex.ir.entities.ModelObjectIndex;
import ru.yandex.ir.entities.TokenizedModel;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArticleFeaturesExtractorTest {

    @Test
    void calculateFeatures() {
        TokenizedModelSnapshot snapshot = createSnapshot();
        ArticleFeaturesExtractor<Query> extractor = new ArticleFeaturesExtractor<>(snapshot,
                new ModelsTextSnapshot(new ModelObjectIndex<>()));
        List<float[]> features = extractor.calculateFeatures(
                new Query(RequestType.SKU_LIKE, "Тушь 8ABC1113", "", Arrays.asList(0L, 1L, 2L))
        );
        double[][] expected = new double[][]{
                {1.0, 1.0, 1.0, 0.0, 1.0, 10.0, 1.0, 1.0, 1.0, 0.0},
                {0.0, 1.0, 0.0, 0.0, 0.4, 4.0, 0.33333334, 0.16000001, 0.11111112, 0.0},
                {0.0, 1.0, 0.0, 0.0, 0.9, 9.0, 0.6666667, 0.80999994, 0.44444448, 0.0}
        };
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < expected[i].length; ++j) {
                assertEquals(features.get(i)[j], expected[i][j], 1e-6);
            }
        }
    }

    private TokenizedModelSnapshot createSnapshot() {
        Long2ObjectOpenHashMap<TokenizedModel> modelsContent = new Long2ObjectOpenHashMap<>();
        modelsContent.put(0, buildModel(0, "8 ABC 1113", new int[]{56, 96354, 1508418}));
        modelsContent.put(1, buildModel(0, "8 ABB 1113", new int[]{56, 96353, 1508418}));
        modelsContent.put(2, buildModel(0, "8 ABC 1117", new int[]{56, 96354, 1508422}));
        return new TokenizedModelSnapshot(new ModelObjectIndex<>(
                new EnumMap<>(RequestType.class) {{
                    put(RequestType.SKU_LIKE, modelsContent);
                }}
        ));
    }

    private TokenizedModel buildModel(long id, String article, int[] tokens) {
        return new TokenizedModel(
                id,
                null, null, null, null,
                null, null,
                article, tokens
        );
    }

}
