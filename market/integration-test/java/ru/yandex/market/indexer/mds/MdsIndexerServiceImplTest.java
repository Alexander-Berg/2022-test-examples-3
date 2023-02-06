package ru.yandex.market.indexer.mds;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Iterators;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.Magics;
import ru.yandex.market.common.mds.s3.client.content.factory.ContentProviderFactory;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feed.model.FeedSiteType;
import ru.yandex.market.core.indexer.db.meta.GenerationMetaService;
import ru.yandex.market.core.indexer.feedlog.FeedLogHelper;
import ru.yandex.market.core.indexer.model.ColoredFeedProcessingResult;
import ru.yandex.market.core.indexer.model.GenerationMeta;
import ru.yandex.market.core.util.io.Protobuf;
import ru.yandex.market.mds.AbstractBillingMdsS3Test;
import ru.yandex.market.proto.indexer.v2.FeedLog;
import ru.yandex.market.protobuf.readers.MessageReader;
import ru.yandex.market.protobuf.tools.MessageIterator;

@Disabled
class MdsIndexerServiceImplTest extends AbstractBillingMdsS3Test {
    private static final int FEEDS_COUNT = 5;
    private MdsIndexerService mdsIndexerService;

    @Autowired
    private GenerationMetaService generationMetaService;

    @BeforeEach
    void init() {
        onBefore();
        mdsIndexerService = new MdsIndexerServiceImpl(locationFactory, mdsS3Client, generationMetaService);
    }

    @AfterEach
    void finish() {
        onAfter();
    }

    @Test
    @DbUnitDataSet(before = "/testWorkflow.metaInfo.before.csv", after = "/testWorkflow.metaInfo.after.csv")
    void testWorkflow() throws IOException {
        uploadTestData();

        Set<GenerationMeta> metas = generationMetaService.getNotImportedBySiteType(FeedSiteType.YELLOW_MARKET);
        Assertions.assertEquals(2, metas.size());

        for (GenerationMeta meta : metas) {
            File feedlog = mdsIndexerService.getFeedlogFile(meta);
            Assertions.assertNotNull(feedlog);
            try (InputStream in = new FileInputStream(feedlog)) {
                MessageReader<FeedLog.Feed> feedlogReader =
                        Protobuf.preparePbSnMessageReader(Magics.MagicConstants.FLOG.name(), FeedLog.Feed.PARSER, in);
                Iterator<FeedLog.Feed> feedIterator = new MessageIterator<>(feedlogReader);
                Iterator<ColoredFeedProcessingResult> processedFeedIterator =
                        Iterators.transform(feedIterator, feed -> new ColoredFeedProcessingResult(feed, meta));

                int cnt = 0;
                FeedLog.Feed[] expectedFeeds = getExpectedFeeds();
                while (processedFeedIterator.hasNext()) {
                    ColoredFeedProcessingResult feedProcessingResult = processedFeedIterator.next();
                    Assertions.assertEquals(expectedFeeds[cnt].getFeedId(), feedProcessingResult.getFeedId());
                    Assertions.assertEquals(expectedFeeds[cnt].getShopId(), feedProcessingResult.getShopId());
                    Assertions.assertEquals(expectedFeeds[cnt].getFeedUrl(), feedProcessingResult.getFeedUrl());
                    cnt++;
                }
                Assertions.assertEquals(FEEDS_COUNT, cnt);
            }
            generationMetaService.markImported(meta.getId());
        }
    }

    /**
     * Для каждого мета-файла из {@code SHOPS_WEB.GENERATION_META} заливаем фидлог.
     */
    private void uploadTestData() throws IOException {
        Set<GenerationMeta> metaInfos = new HashSet<>();
        for (FeedSiteType siteType : FeedSiteType.values()) {
            metaInfos.addAll(generationMetaService.getNotImportedBySiteType(siteType));
        }
        metaInfos.addAll(generationMetaService.getByImported(true));

        for (GenerationMeta meta : metaInfos) {
            String key = FeedLogHelper.FEEDLOG + KeyGenerator.DELIMITER_FOLDER +
                    String.format("%s.%s.%d.pbuf.sn", meta.getMitype(),
                            meta.getGenerationType().getName(), meta.getReleaseDate().getEpochSecond());
            ResourceLocation location = locationFactory.createLocation(key);
            try (InputStream stream = generateProtoBufData()) {
                mdsS3Client.upload(location, ContentProviderFactory.stream(stream));
            }
        }
    }

    private InputStream generateProtoBufData() throws IOException {
        byte[] outBytes =
                Protobuf.messagesSnappyLenvalStreamBytes(Magics.MagicConstants.FLOG.name(), getExpectedFeeds());
        return new ByteArrayInputStream(outBytes);
    }

    private FeedLog.Feed[] getExpectedFeeds() {
        FeedLog.Feed[] feeds = new FeedLog.Feed[FEEDS_COUNT];
        for (int i = 0; i < FEEDS_COUNT; i++) {
            feeds[i] = FeedLog.Feed.newBuilder()
                    .setFeedId(i + 1)
                    .setShopId(i + 10)
                    .setFeedUrl("feed-url-" + i)
                    .build();
        }
        return feeds;
    }
}
