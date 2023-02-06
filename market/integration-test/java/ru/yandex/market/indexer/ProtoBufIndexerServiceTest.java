package ru.yandex.market.indexer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.common.mds.s3.client.content.factory.ContentProviderFactory;
import ru.yandex.market.common.mds.s3.client.content.provider.StreamContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceFileDescriptor;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator;
import ru.yandex.market.core.indexer.feedlog.FeedLogHelper;
import ru.yandex.market.core.indexer.model.GenerationInfo;
import ru.yandex.market.core.util.io.Protobuf;
import ru.yandex.market.indexer.IndexerService.IndexerType;
import ru.yandex.market.mds.AbstractBillingMdsS3Test;
import ru.yandex.market.proto.indexer.v2.FeedLog;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.common.mds.s3.client.content.factory.ContentCompressorFactory.none;

/**
 * Тесты для {@link ProtoBufIndexerService}.
 *
 * @author Vladislav Bauer
 */
//@Disabled // TODO: подключить после переезда в аркадию: st.yandex-team.ru/MBI-44677
public class ProtoBufIndexerServiceTest extends AbstractBillingMdsS3Test {

    private static final IndexerType INDEXER_TYPE = IndexerType.MAIN;
    private static final String TEST_FEEDLOG_META = "/feedlog-meta.meta";
    private static final String TEST_PROTO = "stratocaster.diff.1499063875.pbuf.sn";
    private static final int FEEDS_COUNT = 7;

    private IndexerService indexerService;

    @Value("#{'${indexer.yt.clusters}'.split(',')}")
    private List<String> clusters;

    @BeforeEach
    public void onBefore() {
        super.onBefore();
        indexerService = new ProtoBufIndexerService(
                locationFactory,
                mdsS3Client,
                INDEXER_TYPE,
                clusters);
    }

    @Test
    void testWorkflow() throws IOException {
        uploadTestData();

        Map<String, GenerationInfo> generations = checkGetNewGenerations();
        checkIterateFeedProcessingResults(generations);
    }


    private void uploadTestData() throws IOException {
        // Загружаем meta-файл
        ZonedDateTime now = ZonedDateTime.now().minusDays(10);
        ResourceFileDescriptor fileDescriptor =
                ResourceFileDescriptor.create(FeedLogHelper.FEEDLOG_META, FeedLogHelper.FEEDLOG_META_EXTENSION);

        try (InputStream stream = getClass().getResourceAsStream(TEST_FEEDLOG_META)) {
            StreamContentProvider contentProvider = ContentProviderFactory.stream(stream);
            mdsS3Client.upload(toPrefixedLocation(fileDescriptor, now), contentProvider);
        }

        // Загружаем файл protocol buffer
        final String key = FeedLogHelper.FEEDLOG + KeyGenerator.DELIMITER_FOLDER + TEST_PROTO;
        ResourceLocation location = locationFactory.createLocation(key);

        byte[] generatedData = generateProtoBufData();
        try (InputStream stream = new ByteArrayInputStream(generatedData)) {
            mdsS3Client.upload(location, ContentProviderFactory.stream(stream));
        }
    }

    private Map<String, GenerationInfo> checkGetNewGenerations() {
        // Проверяем что поколение загружено
        Map<String, GenerationInfo> generations = indexerService.getNewGenerations();
        assertThat(generations).hasSize(1);
        assertThat(indexerService.getType()).isEqualTo(INDEXER_TYPE);
        return generations;
    }

    private void checkIterateFeedProcessingResults(Map<String, GenerationInfo> generationMap) {
        // Проверяем что pbuf-файл корректно скачивается и разбирается
        AtomicInteger feeds = new AtomicInteger(0);

        Collection<GenerationInfo> generations = generationMap.values();
        for (GenerationInfo generation : generations) {
            indexerService.iterateFeedProcessingResults(generation, item -> {
                assertThat(item).isNotNull();
                assertThat(item.getFeedId()).isBetween(1L, 999L);
                assertThat(item.getShopId()).isBetween(1L, Long.valueOf(FEEDS_COUNT));
                feeds.incrementAndGet();
                return true;
            });
        }

        assertThat(feeds.get()).isEqualTo(FEEDS_COUNT);
    }

    private ResourceLocation toPrefixedLocation(ResourceFileDescriptor fileDescriptor, ZonedDateTime date) {
        String key = keyGenerator.generateForDate(fileDescriptor, none(), date);
        return locationFactory.createLocation(key);
    }

    private static byte[] generateProtoBufData() throws IOException {
        try (var out = new ByteArrayOutputStream()) {
            try (var snappedStream = Protobuf.<FeedLog.Feed>snappyLenvalOutputStream(ProtoBufIndexerService.MAGIC, out)) {
                for (int shopId = 1; shopId <= FEEDS_COUNT; shopId++) {
                    snappedStream.accept(FeedLog.Feed.newBuilder()
                            .setFeedId(RandomUtils.nextInt(1, 1000))
                            .setShopId(shopId)
                            .build());
                }
            }
            return out.toByteArray();
        }
    }
}
