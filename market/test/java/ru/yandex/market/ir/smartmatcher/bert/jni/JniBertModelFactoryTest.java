package ru.yandex.market.ir.smartmatcher.bert.jni;

import NDict.NBert.Api;
import NDict.NBert.Config;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class JniBertModelFactoryTest {
    public static final float FLOAT_EPSILON = 0.0001f;

    static {
        //Load native library
        System.loadLibrary("jni-bert-native");
    }

    @Test
    public void testSplitModelBottom() {
        int leftLen = 40;
        int rightLen = 88;
        SplitModelBottom bottomLeftModel = LoadSplitModelBottom(L("bottom_left_model.npz"), leftLen);
        SplitModelBottom bottomRightModel = LoadSplitModelBottom(L("bottom_right_model.npz"), rightLen);
        SplitModelTop topModel = LoadSplitModelTop(L("top_model.npz"), leftLen + rightLen);

        List<String> leftSentences = List.of(
                "", // Yep, Market for some inexplicable reasons has empty model titles
                "Комбайн KitchenAid 5KCF0104 морозный жемчуг",
                "Блокнот ErichKrause The Champions, А6, 60 листов (46696)",
                "Светильник Arte Lamp для зеркал Aqua A2470AP-2SS"
        );

        Api.TBatchedPredictRequest.Builder leftBuilder = Api.TBatchedPredictRequest.newBuilder();
        leftSentences.forEach(s ->
                leftBuilder.addBatch(Api.TPredictRequest.newBuilder().addSegments(s).setPadEmbeddingsToLength(leftLen))
        );
        Api.TBatchedPredictResponse leftEmbeddings = bottomLeftModel.encode(leftBuilder.build());


        List<String> rightSentences = List.of(
                "Портер М. \"Конкурентная стратегия: Методика анализа отраслей и конкурентов\"",
                "Кулинарный процессор KitchenAid Artisan 5KCF0104ECA (Caramel Apple)",
                "Блокнот Kroyter Офис А4 60 листов разноцветный в клетку на спирали (207х297 мм, 00092)",
                "Подсветка для зеркал Arte Lamp Aqua A1208AP-2CC"
        );

        Api.TBatchedPredictRequest.Builder rightBuilder = Api.TBatchedPredictRequest.newBuilder();
        rightSentences.forEach(s ->
                rightBuilder.addBatch(Api.TPredictRequest.newBuilder().addSegments(s).setTokenType(1))
        );
        Api.TBatchedPredictResponse rightEmbeddings = bottomRightModel.encode(rightBuilder.build());


        Api.TSplitBertTopRequest.Builder topReqBuilder = Api.TSplitBertTopRequest.newBuilder();
        for (int idx = 0; idx < leftSentences.size(); ++idx) {
            topReqBuilder.addEmbeddingsPairs(
                    Api.TSplitBertBottomEmbeddingsPair.newBuilder()
                            .setLeftPart(leftEmbeddings.getBatch(idx).getSplitBertBottomEmbeddings())
                            .setRightPart(rightEmbeddings.getBatch(idx).getSplitBertBottomEmbeddings())
            );
        }


        Api.TBatchedPredictResponse expectedResponse = LoadExpectedSplitResponse();
        Api.TBatchedPredictResponse actualResponse = topModel.predict(topReqBuilder.build());
        assertPredictResponse(actualResponse, expectedResponse);
    }

    private void assertPredictResponse(Api.TBatchedPredictResponse actual, Api.TBatchedPredictResponse expected) {
        assertThat(actual.getBatchCount()).isEqualTo(expected.getBatchCount());
        for (int i = 0; i < actual.getBatchList().size(); i++) {
            Map<String, Api.TPredictResponse.TClassificationHead> actualMap = actual.getBatch(i).getClassificationHeadsMap();
            Map<String, Api.TPredictResponse.TClassificationHead> expectedMap = expected.getBatch(i).getClassificationHeadsMap();

            assertThat(actualMap.keySet()).containsExactlyInAnyOrderElementsOf(expectedMap.keySet());

            actualMap.forEach((key, actualValue) -> {
                Api.TPredictResponse.TClassificationHead expectedValue = expectedMap.get(key);
                Map<String, Float> actualScoresMap = actualValue.getClassScoresMap();
                Map<String, Float> expectedScoresMap = expectedValue.getClassScoresMap();
                assertThat(actualScoresMap.keySet()).containsExactlyInAnyOrderElementsOf(expectedScoresMap.keySet());
                actualScoresMap.forEach((scoreKey, actualFloat) -> {
                    Float expectedFload = expectedScoresMap.get(scoreKey);
                    assertThat(actualFloat).isEqualTo(expectedFload, within(FLOAT_EPSILON));
                });
            });
        }
    }

    private String L(String path) {
        //use only for local debug in idea
//        return "/home/a-shar/work/smartmatcher/" + path;
        return path;
    }

    private Api.TBatchedPredictResponse LoadExpectedSplitResponse() {
        return Api.TBatchedPredictResponse.newBuilder()
                .addBatch(Api.TPredictResponse.newBuilder()
                        .putClassificationHeads("output",
                                Api.TPredictResponse.TClassificationHead.newBuilder()
                                        .putClassScores("match", -5.03786f)
                                        .putClassScores("mismatch", 5.2547f)
                                        .build()
                        )
                )
                .addBatch(Api.TPredictResponse.newBuilder()
                        .putClassificationHeads("output",
                                Api.TPredictResponse.TClassificationHead.newBuilder()
                                        .putClassScores("match", -1.78555f)
                                        .putClassScores("mismatch", 2.03215f)
                                        .build()
                        )
                )
                .addBatch(Api.TPredictResponse.newBuilder()
                        .putClassificationHeads("output",
                                Api.TPredictResponse.TClassificationHead.newBuilder()
                                        .putClassScores("match", -3.94553f)
                                        .putClassScores("mismatch", 4.20091f)
                                        .build()
                        )
                )
                .addBatch(Api.TPredictResponse.newBuilder()
                        .putClassificationHeads("output",
                                Api.TPredictResponse.TClassificationHead.newBuilder()
                                        .putClassScores("match", -3.5331f)
                                        .putClassScores("mismatch", 4.23203f)
                                        .build()
                        )
                )
                .build();

    }

    private SplitModelTop LoadSplitModelTop(String modelFileName, int len) {
        Config.TModelConfig config = Config.TModelConfig.newBuilder()
                .setKey("WTF")
                .setType(Config.TModelConfig.EModelType.SplitTop)
                .setModelFile(modelFileName)
                .setVocabFile(L("vocab.tsv"))
                .setMaxInpLen(len)
                .setUseProbs(false)
                .setNumThreads(4)
                .setCpu(Config.TModelConfig.TCpuBackend.newBuilder()
                        .addGroups(0)
                )
                .build();
        return BertModelFactory.createSplitModelTop(config);
    }

    private SplitModelBottom LoadSplitModelBottom(String modelFileName, int len) {
        Config.TModelConfig config = Config.TModelConfig.newBuilder()
                .setKey("WTF")
                .setType(Config.TModelConfig.EModelType.SplitBottom)
                .setModelFile(modelFileName)
                .setVocabFile(L("vocab.tsv"))
                .setMaxInpLen(len + 1) // +1 for BOS
                .setLowercase(true)
                .setStripAccents(true)
                .setTruncateLongInputs(true)
                .setUseSegmentIds(true)
                .setNumThreads(4)
                .setCpu(Config.TModelConfig.TCpuBackend.newBuilder()
                        .addGroups(0)
                )
                .build();
        return BertModelFactory.createSplitModelBottom(config);
    }
}
