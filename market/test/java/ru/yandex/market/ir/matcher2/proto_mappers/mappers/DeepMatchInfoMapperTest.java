package ru.yandex.market.ir.matcher2.proto_mappers.mappers;

import java.util.Arrays;

import org.junit.Test;

import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.product.Match;
import ru.yandex.market.ir.matcher2.proto.ShardMatcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeepMatchInfoMapperTest {

    private DeepMatchInfoMapper mapper = new DeepMatchInfoMapper();

    @Test
    public void deepMatchInfoMap_ShardMatcherToMatchTest() {
        final ShardMatcher.DeepMatchInfo deepMatchInfo = ShardMatcher.DeepMatchInfo.newBuilder()
                .setDeepMatch(true)
                .setDeepMatchConfidence(0.1)
                .setDeepMatchedId(15)
                .setDeepMatchTrashScore(0.2)
                .addAllDeepMatchProblems(Arrays.asList(
                        getDeepMatchProblem("test1"),
                        getDeepMatchProblem("test2")))
                .build();
        final Match.DeepMatchInfo info = mapper.map(deepMatchInfo);
        assertTrue(info.isDeepMatch());
        assertEquals(0.1, info.getDeepMatchConfidence(), 0.0001);
        assertEquals(15, info.getDeepMatchedId());
        assertEquals(0.2, info.getDeepMatchTrashScore(), 0.0001);
        assertEquals(2, info.getDeepMatchProblems().size());
    }

    private Matcher.DeepMatchProblem getDeepMatchProblem(String message) {
        return Matcher.DeepMatchProblem.newBuilder()
                .setMessage(message)
                .setProblemType(Matcher.DeepMatchProblem.DeepMatchProblemType.NO_SUCH_MODEL_IN_CATEGORY)
                .build();
    }

}
