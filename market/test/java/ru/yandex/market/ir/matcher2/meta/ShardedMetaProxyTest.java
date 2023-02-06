package ru.yandex.market.ir.matcher2.meta;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.mapstruct.factory.Mappers;

import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.ir.matcher2.matcher.alternate.load.protobuf.DimensionMapper;
import ru.yandex.market.ir.matcher2.proto.ShardMatcher;
import ru.yandex.market.ir.matcher2.proto_mappers.mappers.DeepMatchInfoMapper;
import ru.yandex.market.ir.matcher2.proto_mappers.mappers.EnumMapper;
import ru.yandex.market.ir.matcher2.proto_mappers.mappers.LevelMapper;
import ru.yandex.market.ir.matcher2.proto_mappers.mappers.MatchMapper;
import ru.yandex.market.ir.matcher2.proto_mappers.mappers.MatchResultMapper;
import ru.yandex.market.ir.matcher2.proto_mappers.mappers.ReloadMethodsMapper;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShardedMetaProxyTest {

    private ShardedMetaProxy shardedMetaProxy;
    private ShardWorkersClient shardWorkersClient;

    @Before
    public void setUp() {
        final DimensionMapper dimensionMapper = new DimensionMapper();
        final DeepMatchInfoMapper deepMatchInfoMapper = new DeepMatchInfoMapper();
        final LevelMapper levelMapper = Mappers.getMapper(LevelMapper.class);
        final EnumMapper enumMapper = Mappers.getMapper(EnumMapper.class);

        shardWorkersClient = mock(ShardWorkersClient.class);
        MatchMapper matchMapper = new MatchMapper(deepMatchInfoMapper, levelMapper, dimensionMapper, enumMapper);
        MatchResultMapper matchResultMapper = new MatchResultMapper(dimensionMapper);
        shardedMetaProxy = new ShardedMetaProxy(
                shardWorkersClient,
                matchMapper,
                new ReloadMethodsMapper(),
                matchResultMapper
        );
    }

    @Test
    public void matchBatch_resultWithNO_MATCH() throws ExecutionException, InterruptedException {
        when(shardWorkersClient.send(any())).thenReturn(Collections.singletonList(
                ShardMatcher.MatchResponse.newBuilder()
                        .addResult(ShardMatcher.MatchResult.newBuilder()
                                .setMatch(ShardMatcher.Match.newBuilder()
                                        .setMatchedType(Matcher.MatchType.PRICE_CONFLICT)
                                        .setMatchTarget(Matcher.MatchTarget.PUBLISHED_MODEL)
                                        .setGuruCategoryId(10)
                                        .build())
                                .build())
                        .addResult(ShardMatcher.MatchResult.newBuilder()
                                .setMatch(ShardMatcher.Match.newBuilder()
                                        .setMatchedType(Matcher.MatchType.NO_MATCH)
                                        .setMatchMethod(ShardMatcher.MatchMethod.NO_MATCH)
                                        .setDefinite(true)
                                        .build())
                                .build())
                        .build()
        ));

        Matcher.OfferBatch batch = Matcher.OfferBatch.newBuilder()
                .addOffer(Matcher.Offer.newBuilder().build())
                .build();
        final Matcher.MatchResponse matchResponse = shardedMetaProxy.matchBatch(batch).get();
        assertEquals(1, matchResponse.getResultCount());
        assertEquals(Matcher.MatchType.PRICE_CONFLICT, matchResponse.getResult(0).getMatchType());
    }

    @Test
    public void matchBatch_onlyNO_MATCH() throws ExecutionException, InterruptedException {
        when(shardWorkersClient.send(any())).thenReturn(Collections.singletonList(
                ShardMatcher.MatchResponse.newBuilder()
                        .addResult(ShardMatcher.MatchResult.newBuilder()
                                .setMatch(ShardMatcher.Match.newBuilder()
                                        .setMatchedType(Matcher.MatchType.NO_MATCH)
                                        .setMatchMethod(ShardMatcher.MatchMethod.NO_MATCH)
                                        .setDefinite(true)
                                        .build())
                                .build())
                        .addResult(ShardMatcher.MatchResult.newBuilder()
                                .setMatch(ShardMatcher.Match.newBuilder()
                                        .setMatchedType(Matcher.MatchType.NO_MATCH)
                                        .setMatchMethod(ShardMatcher.MatchMethod.NO_MATCH)
                                        .setDefinite(true)
                                        .build())
                                .build())
                        .build()
        ));

        Matcher.OfferBatch batch = Matcher.OfferBatch.newBuilder()
                .addOffer(Matcher.Offer.newBuilder().build())
                .build();
        final Matcher.MatchResponse matchResponse = shardedMetaProxy.matchBatch(batch).get();
        assertEquals(1, matchResponse.getResultCount());
        assertEquals(Matcher.MatchType.NO_MATCH, matchResponse.getResult(0).getMatchType());
    }

    @Test
    public void matchBatch_order() throws ExecutionException, InterruptedException {
        when(shardWorkersClient.send(any())).thenReturn(Collections.singletonList(
                ShardMatcher.MatchResponse.newBuilder()
                        .addResult(ShardMatcher.MatchResult.newBuilder()
                                .setMatch(ShardMatcher.Match.newBuilder()
                                        .setMatchedType(Matcher.MatchType.PRICE_CONFLICT)
                                        .setMatchTarget(Matcher.MatchTarget.PUBLISHED_MODEL)
                                        .setGuruCategoryId(10)
                                        .build())
                                .build())
                        .addResult(ShardMatcher.MatchResult.newBuilder()
                                .setMatch(ShardMatcher.Match.newBuilder()
                                        .setMatchedType(Matcher.MatchType.NO_MATCH)
                                        .setMatchMethod(ShardMatcher.MatchMethod.NO_MATCH)
                                        .setDefinite(true)
                                        .build())
                                .build())
                        .build()
        ));

        Matcher.OfferBatch batch = Matcher.OfferBatch.newBuilder()
                .addOffer(Matcher.Offer.newBuilder().build())
                .addOffer(Matcher.Offer.newBuilder().build())
                .build();
        final Matcher.MatchResponse matchResponse = shardedMetaProxy.matchBatch(batch).get();
        assertEquals(2, matchResponse.getResultCount());
        assertEquals(Matcher.MatchType.PRICE_CONFLICT, matchResponse.getResult(0).getMatchType());
        assertEquals(Matcher.MatchType.NO_MATCH, matchResponse.getResult(1).getMatchType());
    }

    @Test
    public void matchString_resultWithNO_MATCH() throws ExecutionException, InterruptedException {
        when(shardWorkersClient.send(any())).thenReturn(Collections.singletonList(
                ShardMatcher.MatchResponse.newBuilder()
                        .addResult(ShardMatcher.MatchResult.newBuilder()
                                .setMatch(ShardMatcher.Match.newBuilder()
                                        .setMatchedType(Matcher.MatchType.PRICE_CONFLICT)
                                        .setMatchTarget(Matcher.MatchTarget.PUBLISHED_MODEL)
                                        .setGuruCategoryId(10)
                                        .build())
                                .build())
                        .addResult(ShardMatcher.MatchResult.newBuilder()
                                .setMatch(ShardMatcher.Match.newBuilder()
                                        .setMatchedType(Matcher.MatchType.NO_MATCH)
                                        .setMatchMethod(ShardMatcher.MatchMethod.NO_MATCH)
                                        .setDefinite(true)
                                        .build())
                                .build())
                        .build()
        ));

        final Matcher.LocalizedText text = Matcher.LocalizedText.newBuilder().build();
        final Matcher.MatchResult result = shardedMetaProxy.matchString(text).get();
        assertEquals(Matcher.MatchType.PRICE_CONFLICT, result.getMatchType());
    }

    @Test
    public void multiMatch_order() throws ExecutionException, InterruptedException {
        when(shardWorkersClient.send(any())).thenReturn(Collections.singletonList(
                ShardMatcher.MatchResponse.newBuilder()
                        .addResult(ShardMatcher.MatchResult.newBuilder()
                                .setMatch(ShardMatcher.Match.newBuilder()
                                        .setMatchedType(Matcher.MatchType.MATCH_VENDOR)
                                        .setMatchTarget(Matcher.MatchTarget.VENDOR)
                                        .setDefinite(true)
                                        .build())
                                .build())
                        .addResult(ShardMatcher.MatchResult.newBuilder()
                                .setMatch(ShardMatcher.Match.newBuilder()
                                        .setMatchedType(Matcher.MatchType.MATCH_OK)
                                        .setMatchTarget(Matcher.MatchTarget.PUBLISHED_MODEL)
                                        .setDefinite(true)
                                        .build())
                                .build())
                        .build()
        ));

        final Matcher.Offer offer = Matcher.Offer.newBuilder().build();
        final Matcher.MatchResponse matchResponse = shardedMetaProxy.multiMatch(offer).get();
        assertEquals(2, matchResponse.getResultCount());
        assertEquals(Matcher.MatchType.MATCH_OK, matchResponse.getResult(0).getMatchType());
        assertEquals(Matcher.MatchType.MATCH_VENDOR, matchResponse.getResult(1).getMatchType());
    }
}
