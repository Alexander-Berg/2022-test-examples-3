package ru.yandex.market.volva.dao;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.volva.annotations.DaoLayerTest;
import ru.yandex.market.volva.conf.LiquibaseConfiguration;
import ru.yandex.market.volva.entity.IdType;
import ru.yandex.market.volva.entity.Node;
import ru.yandex.market.volva.entity.crypta.PostProcessingQueueEntry;
import ru.yandex.market.volva.entity.crypta.PostProcessingRequestStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
@DaoLayerTest
@Import({LiquibaseConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class PgPostProcessingQueueDaoTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    private PostProcessingQueueDao postProcessingQueueDao;

    @Before
    public void init() {
        postProcessingQueueDao = new PgPostProcessingQueueDao(jdbcTemplate);
    }

    @Test
    public void getEntries() {
        PostProcessingQueueEntry entry = PostProcessingQueueEntry.builder()
            .nodeValue("2")
            .nodeType(IdType.PUID)
            .status(PostProcessingRequestStatus.QUEUED)
            .addedAt(Instant.ofEpochSecond(1612297000L))
            .build();
        postProcessingQueueDao.saveEntry(entry);
        List<PostProcessingQueueEntry> entries = postProcessingQueueDao.getEntries(PostProcessingRequestStatus.QUEUED, 10);
        assertThat(entries).isNotEmpty();
    }

    @Test
    public void markEntriesForProcessing() {
        int limit = 100;
        PostProcessingQueueEntry entry = PostProcessingQueueEntry.builder()
            .nodeValue("9")
            .nodeType(IdType.PUID)
            .status(PostProcessingRequestStatus.QUEUED)
            .addedAt(Instant.ofEpochSecond(1612297000L))
            .build();
        postProcessingQueueDao.saveEntry(entry);
        assertThat(postProcessingQueueDao.getEntries(PostProcessingRequestStatus.PROCESSING, "test-host", limit)).isEmpty();
        postProcessingQueueDao.markEntriesForProcessing("test-host", 10);
        assertThat(postProcessingQueueDao.getEntries(PostProcessingRequestStatus.PROCESSING, "test-host", limit)).isNotEmpty();
    }

    @Test
    public void updateStatuses() {
        PostProcessingQueueEntry entry = PostProcessingQueueEntry.builder()
            .nodeValue("9")
            .nodeType(IdType.PUID)
            .status(PostProcessingRequestStatus.QUEUED)
            .addedAt(Instant.ofEpochSecond(1612297000L))
            .build();
        postProcessingQueueDao.saveEntry(entry);
        List<PostProcessingQueueEntry> entries = postProcessingQueueDao.getEntries(PostProcessingRequestStatus.QUEUED, 10);
        assertThat(entries).isNotEmpty();
        assertThat(postProcessingQueueDao.getEntries(PostProcessingRequestStatus.PROCESSED, 10)).isEmpty();
        postProcessingQueueDao.updateStatuses(entries, PostProcessingRequestStatus.PROCESSED);
        assertThat(postProcessingQueueDao.getEntries(PostProcessingRequestStatus.QUEUED, 10)).isEmpty();
        assertThat(postProcessingQueueDao.getEntries(PostProcessingRequestStatus.PROCESSED, 10)).isNotEmpty();
    }

    @Test
    public void removeOldQueries() {
        Instant now = Instant.now();
        PostProcessingQueueEntry entry1 = PostProcessingQueueEntry.builder()
            .nodeValue("11")
            .nodeType(IdType.PUID)
            .status(PostProcessingRequestStatus.QUEUED)
            .addedAt(now.minus(2L, ChronoUnit.DAYS))
            .build();
        PostProcessingQueueEntry entry2 = PostProcessingQueueEntry.builder()
            .nodeValue("12")
            .nodeType(IdType.PUID)
            .status(PostProcessingRequestStatus.PROCESSED)
            .addedAt(now.minus(2L, ChronoUnit.DAYS))
            .build();
        PostProcessingQueueEntry entry3 = PostProcessingQueueEntry.builder()
            .nodeValue("13")
            .nodeType(IdType.PUID)
            .status(PostProcessingRequestStatus.PROCESSED)
            .addedAt(now.minus(2L, ChronoUnit.HOURS))
            .build();
        postProcessingQueueDao.saveEntries(List.of(entry1, entry2, entry3));
        var queued = postProcessingQueueDao.getEntries(PostProcessingRequestStatus.QUEUED, 100);
        assertThat(queued)
            .extracting(PostProcessingQueueEntry::getNode)
            .contains(new Node("11", IdType.PUID));
        var processed = postProcessingQueueDao.getEntries(PostProcessingRequestStatus.PROCESSED, 100);
        assertThat(processed)
            .extracting(PostProcessingQueueEntry::getNode)
            .contains(new Node("12", IdType.PUID), new Node("13", IdType.PUID));
        postProcessingQueueDao.removeOldQueries(PostProcessingRequestStatus.PROCESSED, now.minus(1L, ChronoUnit.DAYS));
        assertThat(postProcessingQueueDao.getEntries(PostProcessingRequestStatus.QUEUED, 100))
            .extracting(PostProcessingQueueEntry::getNode)
            .contains(new Node("11", IdType.PUID));
        assertThat(postProcessingQueueDao.getEntries(PostProcessingRequestStatus.PROCESSED, 100))
            .extracting(PostProcessingQueueEntry::getNode)
            .contains(new Node("13", IdType.PUID))
            .doesNotContain(new Node("12", IdType.PUID));
    }
}
