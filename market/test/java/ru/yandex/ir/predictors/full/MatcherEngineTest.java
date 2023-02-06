package ru.yandex.ir.predictors.full;

import ai.catboost.CatBoostError;
import org.junit.jupiter.api.Test;
import ru.yandex.ir.classify.om.be.ForcedCandidatesProvider;
import ru.yandex.ir.classify.om.be.Seedable;
import ru.yandex.ir.common.be.CommonTextContent;
import ru.yandex.ir.common.be.RankingRequest;
import ru.yandex.ir.common.features.FeatureInfo;
import ru.yandex.ir.common.knowledge.FeaturesExtractor;
import ru.yandex.ir.predictors.base.Candidator;
import ru.yandex.ir.predictors.base.CatboostRanker;
import ru.yandex.ir.predictors.base.Predictor;
import ru.yandex.ir.predictors.full.pickers.ElementsPickerStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MatcherEngineTest {
    @Test
    public void candidateTest() {
        int topCandidatesPick = 3;
        MatcherEngine<Long, Query, QueryWithCandidates> engine = new MatcherEngine<>(
            MatcherEngine.MatchMode.CANDIDATES_ONLY,
            new ConstantCandidator(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L)),
            new Combiner(),
            null,
            null,
            null,
            null,
            20,
            null,
            ElementsPickerStrategy.parseFrom("CANDIDATE_TOP:" + topCandidatesPick)
        );
        List<MatchedDocument<Long>> matches = engine.match(new Query(1));
        assertEquals(topCandidatesPick, matches.size());
        assertEquals(0L, matches.get(0).getDocument());
        assertEquals(1L, matches.get(1).getDocument());
        assertEquals(2L, matches.get(2).getDocument());
    }

    @Test
    public void candidateWithRandomTest() {
        int topCandidatesPick = 3;
        int randomPick = 4;
        MatcherEngine<Long, Query, QueryWithCandidates> engine = new MatcherEngine<>(
            MatcherEngine.MatchMode.CANDIDATES_ONLY,
            new ConstantCandidator(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L)),
            new Combiner(),
            null,
            null,
            null,
            null,
            20,
            null,
            ElementsPickerStrategy.parseFrom("CANDIDATE_TOP:" + topCandidatesPick + ",RANDOMLY:" + randomPick)
        );
        List<MatchedDocument<Long>> matches = engine.match(new Query(1));
        assertEquals(topCandidatesPick + randomPick, matches.size());
        assertEquals(0L, matches.get(0).getDocument());
        assertEquals(1L, matches.get(1).getDocument());
        assertEquals(2L, matches.get(2).getDocument());
        assertEquals(7L, matches.get(3).getDocument());
        assertEquals(3L, matches.get(4).getDocument());
        assertEquals(6L, matches.get(5).getDocument());
        assertEquals(5L, matches.get(6).getDocument());
    }

    @Test
    public void candidateWithFeaturesTest() {
        int topCandidatesPick = 3;
        MatcherEngine<Long, Query, QueryWithCandidates> engine = new MatcherEngine<>(
            MatcherEngine.MatchMode.CANDIDATES_AND_FEATURES,
            new ConstantCandidator(Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L)),
            new Combiner(),
            new IdAsFeaturesExtractor(),
            null,
            null,
            null,
            20,
            null,
            ElementsPickerStrategy.parseFrom("CANDIDATE_TOP:" + topCandidatesPick)
        );
        List<MatchedDocument<Long>> matches = engine.match(new Query(1));
        assertEquals(topCandidatesPick, matches.size());
        assertArrayEquals(new float[]{0.0f}, matches.get(0).getFeatures());
        assertArrayEquals(new float[]{1.0f}, matches.get(1).getFeatures());
        assertArrayEquals(new float[]{2.0f}, matches.get(2).getFeatures());
    }

    @Test
    public void candidateWithRankingTest() {
        int topCandidatesPick = 4;
        MatcherEngine<Long, Query, QueryWithCandidates> engine = new MatcherEngine<>(
            MatcherEngine.MatchMode.WITH_RANKING,
            new ConstantCandidator(Arrays.asList(0L, 4L, 5L, 6L, 7L, 8L, 1L, 2L, 3L)),
            new Combiner(),
            new IdAsFeaturesExtractor(),
            null,
            new CatboostRanker<>(new FirstFeaturePredictor(1)),
            null,
            20,
            null,
            ElementsPickerStrategy.parseFrom("RANKED_TOP:" + topCandidatesPick)
        );
        List<MatchedDocument<Long>> matches = engine.match(new Query(1));
        assertEquals(topCandidatesPick, matches.size());
        assertEquals(8L, matches.get(0).getDocument());
        assertEquals(7L, matches.get(1).getDocument());
        assertEquals(6L, matches.get(2).getDocument());
        assertEquals(5L, matches.get(3).getDocument());

        assertEquals(8.0, matches.get(0).getRankScore());
        assertEquals(7.0, matches.get(1).getRankScore());
        assertEquals(6.0, matches.get(2).getRankScore());
        assertEquals(5.0, matches.get(3).getRankScore());
    }

    @Test
    public void candidateWithRankingAndFFTest() {
        int topCandidatesPick = 4;
        MatcherEngine<Long, Query, QueryWithCandidates> engine = new MatcherEngine<>(
            MatcherEngine.MatchMode.WITH_RANKING_AND_FF,
            new ConstantCandidator(Arrays.asList(0L, 4L, 5L, 6L, 7L, 8L, 1L, 2L, 3L)),
            new Combiner(),
            new IdAsFeaturesExtractor(),
            null,
            new CatboostRanker<>(new FirstFeaturePredictor(1)),
            new CatboostRanker<>(new FirstFeaturePredictor(-1)),
            20,
            null,
            ElementsPickerStrategy.parseFrom("RANKED_TOP:" + topCandidatesPick)
        );
        List<MatchedDocument<Long>> matches = engine.match(new Query(1));
        assertEquals(topCandidatesPick, matches.size());
        assertEquals(8L, matches.get(0).getDocument());
        assertEquals(7L, matches.get(1).getDocument());
        assertEquals(6L, matches.get(2).getDocument());
        assertEquals(5L, matches.get(3).getDocument());

        assertEquals(8.0, matches.get(0).getRankScore());
        assertEquals(7.0, matches.get(1).getRankScore());
        assertEquals(6.0, matches.get(2).getRankScore());
        assertEquals(5.0, matches.get(3).getRankScore());

        assertEquals(-8.0, matches.get(0).getConfidence());
        assertEquals(-7.0, matches.get(1).getConfidence());
        assertEquals(-6.0, matches.get(2).getConfidence());
        assertEquals(-5.0, matches.get(3).getConfidence());
    }

    @Test
    public void candidateForcedTest() {
        int topCandidatesPick = 1;
        int forcedPick = 1;
        MatcherEngine<Long, Query, QueryWithCandidates> engine = new MatcherEngine<>(
            MatcherEngine.MatchMode.WITH_RANKING_AND_FF,
            new ConstantCandidator(Arrays.asList(0L, 4L, 5L, 6L, 7L, 8L, 1L, 2L, 3L), true),
            new Combiner(),
            new IdAsFeaturesExtractor(),
            null,
            new CatboostRanker<>(new FirstFeaturePredictor(1)),
            new CatboostRanker<>(new FirstFeaturePredictor(-1)),
            20,
            null,
            ElementsPickerStrategy.parseFrom("RANKED_TOP:" + topCandidatesPick + ",FORCED:" + forcedPick)
        );
        List<MatchedDocument<Long>> matches = engine.match(new Query(1, Collections.singletonList(4L)));
        assertEquals(8.0, matches.get(0).getRankScore());
        assertEquals(4.0, matches.get(1).getRankScore());

        assertEquals(-8.0, matches.get(0).getConfidence());
        assertEquals(-4.0, matches.get(1).getConfidence());
    }

    @Test
    public void candidateForcedNoDuplicatesTest() {
        int topCandidatesPick = 1;
        int forcedPick = 1;
        MatcherEngine<Long, Query, QueryWithCandidates> engine = new MatcherEngine<>(
            MatcherEngine.MatchMode.WITH_RANKING_AND_FF,
            new ConstantCandidator(Arrays.asList(0L, 4L, 5L, 6L, 7L, 8L, 1L, 2L, 3L), true),
            new Combiner(),
            new IdAsFeaturesExtractor(),
            null,
            new CatboostRanker<>(new FirstFeaturePredictor(1)),
            new CatboostRanker<>(new FirstFeaturePredictor(-1)),
            20,
            null,
            ElementsPickerStrategy.parseFrom("RANKED_TOP:" + topCandidatesPick + ",FORCED:" + forcedPick)
        );
        List<MatchedDocument<Long>> matches = engine.match(new Query(1, Collections.singletonList(8L)));
        assertEquals(1, matches.size());
        assertEquals(8.0, matches.get(0).getRankScore());
    }

    static class FirstFeaturePredictor implements Predictor {
        private final double multiplier;

        FirstFeaturePredictor(double multiplier) {
            this.multiplier = multiplier;
        }

        @Override
        public double[] predict(float[][] numericFeatures, int[][] catFeatureHashes) throws CatBoostError {
            double[] result = new double[numericFeatures.length];
            for (int i = 0; i < result.length; ++i) {
                result[i] = numericFeatures[i][0] * multiplier;
            }
            return result;
        }
    }

    static class IdAsFeaturesExtractor implements FeaturesExtractor<QueryWithCandidates> {

        @Override
        public List<float[]> calculateFeatures(QueryWithCandidates element) {
            return element.getDocuments().stream().map(x -> new float[]{(float) x}).collect(Collectors.toList());
        }

        @Override
        public FeatureInfo[] getFeatureInfoList() {
            return new FeatureInfo[]{new FeatureInfo("id_as_feature")};
        }
    }

    static class ConstantCandidator implements Candidator<Query, Long> {
        private final List<Long> candidates;
        private final boolean appendForced;

        ConstantCandidator(List<Long> candidates, boolean appendForced) {
            this.candidates = candidates;
            this.appendForced = appendForced;
        }

        ConstantCandidator(List<Long> candidates) {
            this(candidates, false);
        }

        @Override
        public List<Long> provideCandidates(Query baseElement, int takeCount) {
            ArrayList<Long> res = new ArrayList<>(candidates.subList(0, Math.min(candidates.size(), takeCount)));
            if (appendForced) {
                for (long forcedOne : baseElement.getForcedCandidates()) {
                    if (!res.contains(forcedOne)) {
                        res.add(forcedOne);
                    }
                }
            }
            return res;
        }
    }

    static class Combiner implements BiFunction<Query, List<Long>, QueryWithCandidates> {

        @Override
        public QueryWithCandidates apply(Query query, List<Long> longs) {
            return new QueryWithCandidates(query, longs);
        }
    }

    static class Query implements Seedable, ForcedCandidatesProvider<Long> {
        private final long id;
        private final List<Long> forcedCandidates;

        Query(long id) {
            this.id = id;
            this.forcedCandidates = Collections.emptyList();
        }

        Query(long id, List<Long> forcedCandidates) {
            this.id = id;
            this.forcedCandidates = forcedCandidates;
        }

        public long getId() {
            return id;
        }

        @Override
        public List<Long> getForcedCandidates() {
            return forcedCandidates;
        }

        @Override
        public int getInitialSeed() {
            return (int) id ^ 1432534;
        }
    }

    static class QueryWithCandidates implements RankingRequest<Long>, CommonTextContent {
        private final Query query;
        private final List<Long> documents;

        QueryWithCandidates(Query query, List<Long> documents) {
            this.query = query;
            this.documents = documents;
        }

        public Query getQuery() {
            return query;
        }

        @Override
        public List<Long> getDocuments() {
            return documents;
        }

        @Override
        public int documentsSize() {
            return documents.size();
        }

        @Override
        public String getTitle() {
            return "";
        }

        @Override
        public String getDescription() {
            return "";
        }
    }
}
