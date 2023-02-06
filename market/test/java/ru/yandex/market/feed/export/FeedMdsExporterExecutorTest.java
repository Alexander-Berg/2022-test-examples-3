package ru.yandex.market.feed.export;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.feed.processor.parsing.FeedUpdateTaskOuterClass;
import ru.yandex.market.protobuf.readers.LEDelimerMessageReader;
import ru.yandex.market.shop.FunctionalTest;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FeedMdsExporterExecutorTest extends FunctionalTest {

    @Autowired
    private FeedMdsExporterExecutor executor;

    @Test
    @DbUnitDataSet(before = "FeedMdsExporterExecutorTest.before.csv")
    void testExport() throws IOException {
        executor.doJob(mock(JobExecutionContext.class));
        var tmp = Files.createTempFile("feeds-test", ".pbuf.sn");
        var out = Files.newOutputStream(tmp);
        executor.exportFeeds(out);
        out.close();
        var reader = new LEDelimerMessageReader<>(
                FeedUpdateTaskOuterClass.FeedUpdateTask.parser(),
                Files.newInputStream(tmp));
        var result = Stream.generate(() -> {
            try {
                return reader.read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).takeWhile(Objects::nonNull).collect(Collectors.toList());
        assertThat(result)
                .map(task -> Pair.of(task.getFeed().getShopId(), task.getFeed().getBusinessId()))
                .containsExactlyInAnyOrder(
                        Pair.of(1001L, 666666L), Pair.of(1002L, 666667L), Pair.of(1003L, 666668L),
                        Pair.of(1004L, 666669L), Pair.of(1005L, 666670L), Pair.of(1006L, 666671L),
                        Pair.of(1007L, 666672L), Pair.of(1008L, 666673L), Pair.of(1009L, 0L),
                        Pair.of(1010L, 0L), Pair.of(1011L, 0L), Pair.of(666666L, 666666L)
                );
        assertThat(result)
                .map(task -> task.getFeed().getFeedId())
                .filteredOn(id -> id != 0)
                .containsExactlyInAnyOrder(101L, 102L, 103L, 104L, 105L, 1509290L, 591638L, 404343L, 106L);
        assertThat(result)
                .map(task -> task.getFeed().getOriginalFileName())
                .filteredOn(name -> !name.isEmpty())
                .containsExactlyInAnyOrder("shop.yml", "supplier-feed.xlsx", "utility_feed.xml");
        assertThat(result)
                .filteredOn(task -> task.getFeed().getShopId() == 1001L)
                .map(task -> task.getFeed().getFeedType())
                .containsExactly(SamovarContextOuterClass.FeedInfo.FeedType.PRICES);
    }
}
