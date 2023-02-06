package ru.yandex.market.core.indexer.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import com.google.common.collect.Iterators;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.Magics;
import ru.yandex.market.core.indexer.feedlog.FeedProcessingResultImpl;
import ru.yandex.market.core.indexer.model.FeedProcessingResult;
import ru.yandex.market.core.indexer.model.GenerationInfo;
import ru.yandex.market.core.util.io.Protobuf;
import ru.yandex.market.proto.indexer.v2.FeedLog;
import ru.yandex.market.stream.IndexerProtocolHelper;

class FeedLogParserTest {

    private static final String TYPE = Magics.MagicConstants.FLOG.name();

    @Test
    void testFeedLogParsing() throws IOException {
        GenerationInfo generation = new GenerationInfo();
        FeedLog.Feed feedLog = prepareFeedLog();
        byte[] pbuf = prepareProto(feedLog);
        InputStream stream = new ByteArrayInputStream(pbuf);
        final Iterator<FeedLog.Feed> fi = IndexerProtocolHelper.iterateSnappy(stream, FeedLog.Feed.parser(), TYPE);
        Iterator<FeedProcessingResult> it =
                Iterators.transform(fi, feed -> new FeedProcessingResultImpl(generation, feed));
        Assertions.assertTrue(it.hasNext());
        FeedProcessingResult feedResultActual = it.next();
        FeedProcessingResult feedResultExpected = new FeedProcessingResultImpl(generation, feedLog);
        Assertions.assertEquals(feedResultExpected.getFeedId(), feedResultActual.getFeedId());
        Assertions.assertEquals(feedResultExpected.getDownloadDate(), feedResultActual.getDownloadDate());
        Assertions.assertEquals(feedResultExpected.getStartDate(), feedResultActual.getStartDate());
        Assertions.assertEquals(feedResultExpected.getParseReturnCode(), feedResultActual.getParseReturnCode());
        Assertions.assertEquals(feedResultExpected.getPublishedSession(), feedResultActual.getPublishedSession());
        Assertions.assertEquals(feedResultExpected.getFeedUrl(), feedResultActual.getFeedUrl());
        Assertions.assertEquals(feedResultExpected.getYmlDate(), feedResultActual.getYmlDate());
        Assertions.assertEquals(feedResultExpected.getTotalOffersCount(), feedResultActual.getTotalOffersCount());
        Assertions.assertEquals(feedResultExpected.getValidOffersCount(), feedResultActual.getValidOffersCount());
    }

    private byte[] prepareProto(FeedLog.Feed feed) throws IOException {
        return Protobuf.singleMessageSnappyLenvalStreamBytes(TYPE, feed);
    }

    private FeedLog.Feed prepareFeedLog() {
        return FeedLog.Feed.newBuilder()
                .setFeedId(494173)
                .setLastSession(
                        FeedLog.RobotFeedSession.newBuilder()
                                .setSessionName("20180729_0347")
                                .setStartDate(1532836064)
                                .setDownloadDate(1532836064)
                                .setParseRetcode(0)
                                .build()
                )
                .setPublishedSession("20180729_0347")
                .setFeedUrl("https://www.centr-igr.ru/marketplace/37994.xml")
                .setYmlDate("2018-07-29 06:47:00+03")
                .setParseStats(FeedLog.ParseStats.newBuilder().setTotalOffers(334).setValidOffers(332).build())
                .build();
    }
}
