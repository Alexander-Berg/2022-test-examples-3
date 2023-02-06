package ru.yandex.market.indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.model.ResourceListing;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.indexer.feedlog.FeedLogHelper;
import ru.yandex.market.indexer.mds.MdsIndexerCleanerExecutor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

@ExtendWith(MockitoExtension.class)
public class MdsIndexerCleanerExecutorTest extends FunctionalTest {

    @Autowired
    private MdsS3Client mdsS3Client;
    @Autowired
    private ResourceLocationFactory resourceLocationFactory;
    @Autowired
    private MdsIndexerCleanerExecutor mdsIndexerCleanerExecutor;

    static List<String> getMetaKeys() {
        return new ArrayList<>(getMetas().keySet());
    }

    private static Map<String, String> getMetas() {
        return Map.of(
                //ее нет в generation meta, надо скачать и удалить как протухшую
                "feedlog-meta/1990-01-01/feedlog-meta_1990-01-01_22-22-22_b85e4a39-307c-409c-81d0-ca17f9872b0d.meta",
                "" +
                        "name=19900101_2222\n" +
                        "mitype=stratocaster\n" +
                        "type=full\n" +
                        "release_date=631152000\n",
                //есть в generation meta, но она еще не проимпортирована, не надо удалять
                "feedlog-meta/1998-01-01/feedlog-meta_1998-01-01_22-22-22_fd5ae08a-307c-409c-81d0-ca17f9872b0d.meta",
                "" +
                        "name=19980101_2222\n" +
                        "mitype=planeshift.stratocaster\n" +
                        "type=full\n" +
                        "release_date=883612800\n",
                //есть в generation meta, надо удалить как протухную не скачивая
                "feedlog-meta/1999-01-01/feedlog-meta_1999-01-01_22-22-22_e4f5e88f-4e4b-4d4c-9631-b85e4a397350.meta",
                "" +
                        "name=19990101_2222\n" +
                        "mitype=planeshift.stratocaster\n" +
                        "type=full\n" +
                        "release_date=915148800\n",
                //есть в generation meta, надо удалить как протухшую не скачивая
                "feedlog-meta/2000-01-01/feedlog-meta_2000-01-01_22-22-22_e4374d93-55e3-462f-b333-f86ee025caf1.meta",
                "" +
                        "name=20000101_2222\n" +
                        "mitype=stratocaster\n" +
                        "type=diff\n" +
                        "release_date=946684800\n",
                //нет в generation meta, надо скачать, но понять, что еще не протухла и не удалять
                "feedlog-meta/2040-01-01/feedlog-meta_2040-01-01_22-22-22_b13e7043-e35c-40ae-a776-3a67ee7ae149.meta",
                "" +
                        "name=20400101_2222\n" +
                        "mitype=stratocaster\n" +
                        "type=full\n" +
                        "release_date=2524608000\n"
        );
    }

    @BeforeEach
    void setup() {
        var metas = ResourceListing.create(resourceLocationFactory.getBucketName(), getMetaKeys(), List.of());
        var feedLogMetaLocation = resourceLocationFactory.createLocation(FeedLogHelper.FEEDLOG_META);
        Mockito.when(mdsS3Client.list(Mockito.eq(feedLogMetaLocation), Mockito.eq(true))).thenReturn(metas);
        Mockito.when(mdsS3Client.contains(Mockito.any())).thenReturn(true);
        for (Map.Entry<String, String> meta : getMetas().entrySet()) {
            var location = ResourceLocation.create(resourceLocationFactory.getBucketName(), meta.getKey());
            Mockito.when(mdsS3Client.download(Mockito.eq(location), Mockito.any())).thenReturn(meta.getValue());
        }
    }

    @Test
    @DbUnitDataSet(before = "MdsIndexerCleanerExecutorTest.before.csv")
    void testCleaner() {
        mdsIndexerCleanerExecutor.doJob(null);

        var booleanCapture = ArgumentCaptor.forClass(Boolean.class);
        var contentConsumerCapture = ArgumentCaptor.forClass(ContentConsumer.class);

        var listArgument = ArgumentCaptor.forClass(ResourceLocation.class);
        var downloadArgument = ArgumentCaptor.forClass(ResourceLocation.class);
        var deleteArgument = ArgumentCaptor.forClass(ResourceLocation.class);

        Mockito.verify(mdsS3Client, Mockito.times(1))
                .list(listArgument.capture(), booleanCapture.capture());
        Mockito.verify(mdsS3Client, Mockito.times(2))
                .download(downloadArgument.capture(), contentConsumerCapture.capture());
        Mockito.verify(mdsS3Client, Mockito.times(6))
                .delete(deleteArgument.capture());

        Assertions.assertEquals(listArgument.getValue().getKey(), FeedLogHelper.FEEDLOG_META);
        assertThat(downloadArgument.getAllValues().stream().map(ResourceLocation::getKey).collect(Collectors.toList()), containsInAnyOrder(
                "feedlog-meta/1990-01-01/feedlog-meta_1990-01-01_22-22-22_b85e4a39-307c-409c-81d0-ca17f9872b0d.meta",
                "feedlog-meta/2040-01-01/feedlog-meta_2040-01-01_22-22-22_b13e7043-e35c-40ae-a776-3a67ee7ae149.meta"
        ));
        assertThat(deleteArgument.getAllValues().stream().map(ResourceLocation::getKey).collect(Collectors.toList()),
                containsInAnyOrder(
                "feedlog/stratocaster.full.631152000.pbuf.sn",
                "feedlog-meta/1990-01-01/feedlog-meta_1990-01-01_22-22-22_b85e4a39-307c-409c-81d0-ca17f9872b0d.meta",
                "feedlog/planeshift.stratocaster.full.915112800.pbuf.sn",
                "feedlog-meta/1999-01-01/feedlog-meta_1999-01-01_22-22-22_e4f5e88f-4e4b-4d4c-9631-b85e4a397350.meta",
                "feedlog/stratocaster.diff.946648800.pbuf.sn",
                "feedlog-meta/2000-01-01/feedlog-meta_2000-01-01_22-22-22_e4374d93-55e3-462f-b333-f86ee025caf1.meta"
        ));
        System.out.println();
    }

}
