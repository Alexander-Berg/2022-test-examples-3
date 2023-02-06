package ru.yandex.market.ir.matcher2.meta;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.ir.matcher2.proto.ShardMatcher;

import static ru.yandex.market.ir.matcher2.meta.ShardedMetaProxy.CROSSCAT_RESULT_COMPARATOR;

public class CrossCatComparatorTest {

    @Test
    public void matchWithoutBlackWordsWins() {
        var m1 = ShardMatcher.MatchResult.newBuilder()
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2).build())
                .build();
        var m2 = ShardMatcher.MatchResult.newBuilder()
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2)
                        .addBlockwords(Matcher.SubstrEntry.newBuilder().setNormalizedAlias("alias").build()).build())
                .build();

        Assert.assertTrue(CROSSCAT_RESULT_COMPARATOR.compare(m1, m2) < 0);
    }


    @Test
    public void barcodeFromBarcodeValueWins() {
        var m1 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setMatchedType(Matcher.MatchType.BARCODE_MATCH)
                        .build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2)
                        .setAlias(Matcher.SubstrEntry.newBuilder()
                                .setStrIndex(Matcher.SourceIndex.BARCODE_VALUE)
                                .build())
                        .build())
                .build();
        var m2 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setMatchedType(Matcher.MatchType.BARCODE_MATCH)
                        .build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2)
                        .setAlias(Matcher.SubstrEntry.newBuilder()
                                .setStrIndex(Matcher.SourceIndex.OFFER_VALUE)
                                .build())
                        .build())
                .build();

        Assert.assertTrue(CROSSCAT_RESULT_COMPARATOR.compare(m1, m2) < 0);
    }

    @Test
    public void barcodeFromYmlWinsDescription() {
        var m1 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setMatchedType(Matcher.MatchType.BARCODE_MATCH)
                        .build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2)
                        .setAlias(Matcher.SubstrEntry.newBuilder()
                                .setStrIndex(5)
                                .build())
                        .build())
                .build();
        var m2 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setMatchedType(Matcher.MatchType.BARCODE_MATCH)
                        .build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2)
                        .setAlias(Matcher.SubstrEntry.newBuilder()
                                .setStrIndex(Matcher.SourceIndex.DESCRIPTION_VALUE)
                                .build())
                        .build())
                .build();

        Assert.assertTrue(CROSSCAT_RESULT_COMPARATOR.compare(m1, m2) < 0);
    }

    @Test
    public void matchWithHigherFrequencyWins() {
        var m1 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setMatchedFrequency(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2).build())
                .build();
        var m2 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setMatchedFrequency(2).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2).build())
                .build();

        Assert.assertTrue(CROSSCAT_RESULT_COMPARATOR.compare(m1, m2) > 0);
    }


    @Test
    public void matchWithLowerModelIdWins() {
        var m1 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setHid(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2).build())
                .build();
        var m2 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setHid(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(0))
                .build();

        Assert.assertTrue(CROSSCAT_RESULT_COMPARATOR.compare(m1, m2) > 0);
    }


    @Test
    public void matchWithLowerHidWins() {
        var m1 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setHid(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2).build())
                .build();
        var m2 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setHid(2).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2))
                .build();

        Assert.assertTrue(CROSSCAT_RESULT_COMPARATOR.compare(m1, m2) < 0);
    }


    @Test
    public void matchWithMoreLevelsWins() {
        var m1 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setHid(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2).build())
                .build();
        var m2 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setHid(2).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .build();

        Assert.assertTrue(CROSSCAT_RESULT_COMPARATOR.compare(m1, m2) < 0);
    }


    @Test
    public void matchWithLessLevelButNoBlackWordWins() {
        var m1 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setHid(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1)
                        .addBlockwords(
                                Matcher.SubstrEntry.newBuilder().setNormalizedAlias("alias").build()
                        )
                        .build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2).build())
                .build();
        var m2 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setHid(2).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .build();

        Assert.assertTrue(CROSSCAT_RESULT_COMPARATOR.compare(m1, m2) > 0);
    }


    @Test
    public void matchWithMoreLevelButBlackwordInLastLevelLose() {
        var m1 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setHid(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2)
                        .addBlockwords(
                                Matcher.SubstrEntry.newBuilder().setNormalizedAlias("alias").build()
                        )
                        .build())
                .build();
        var m2 = ShardMatcher.MatchResult.newBuilder()
                .setMatch(ShardMatcher.Match.newBuilder().setHid(2).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .build();

        Assert.assertTrue(CROSSCAT_RESULT_COMPARATOR.compare(m1, m2) > 0);
    }


    @Test
    public void matchWithBlackWordInHigherLevelWins() {
        var m1 = ShardMatcher.MatchResult.newBuilder()
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).addBlockwords(
                        Matcher.SubstrEntry.newBuilder().setNormalizedAlias("alias").build()).build()
                )
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2).build())
                .build();
        var m2 = ShardMatcher.MatchResult.newBuilder()
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(1).build())
                .addMatchLevel(Matcher.MatchLevel.newBuilder().setMatchedId(2)
                        .addBlockwords(Matcher.SubstrEntry.newBuilder().setNormalizedAlias("alias").build()).build())
                .build();

        Assert.assertTrue(CROSSCAT_RESULT_COMPARATOR.compare(m1, m2) > 0);
    }
}
