package ru.yandex.market.mbi.msapi.logbroker;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.logbroker.loader.models.ChunkMeta;
import ru.yandex.market.mbi.msapi.logbroker.config.InitConfigOld;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mbi.msapi.logbroker.ChunkStatus.RECEIVED;
import static ru.yandex.market.mbi.msapi.logbroker.ChunkStatus.RECOVERED;
import static ru.yandex.market.mbi.msapi.logbroker.ChunkStatus.SAVED_TO_STASH;

/**
 * @author aostrikov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {InitConfigOld.class})
@ActiveProfiles({"integration-tests"})
public class MetadataRepositoryTest {

    private static final String DEFAULT_USER_LOG_TYPE = "clicks";

    private static final EnumSet<ChunkStatus> STASH_ONLY = EnumSet.of(SAVED_TO_STASH);

    @Autowired
    private MetadataRepository metadataRepository;

    private static String someReceiver() {
        return UUID.randomUUID().toString();
    }

    private static ChunkMeta meta(String topic, long offset) {
        return ChunkMeta.builder()
                .topic(topic)
                .offset(offset)
                .build();
    }

    @Test
    public void shouldRetrieveNothingForUnknownReceiver() {
        assertThat(metadataRepository.getLastOffsets(someReceiver(), "topic"), is(new HashMap<>()));
    }

    @Test
    public void shouldInsertNewChunkAndCheckIt() {
        ChunkMeta meta = meta("topic", 1L);

        long chunkId = metadataRepository.insertNewChunk(someReceiver(), meta, DEFAULT_USER_LOG_TYPE, 0);

        assertTrue(chunkId > 0);
    }

    @Test
    public void shouldInsertAndGetOffsetsForChunk() {
        String receiver = someReceiver();
        ChunkMeta meta = meta("topic3", 350L);

        long id = metadataRepository.insertNewChunk(receiver, meta, DEFAULT_USER_LOG_TYPE, 1);
        metadataRepository.updateChunkStatus(id, Instant.now(), RECEIVED, 0);

        assertThat(metadataRepository.getLastOffsets(receiver, "topic"), is(ImmutableMap.of("topic3", 350L)));
    }

    @Test
    public void shouldStashAndGetOffsetsForChunk() {
        String receiver = someReceiver();
        ChunkMeta meta = meta("topic3", 350L);

        long id = metadataRepository.insertNewChunk(receiver, meta, DEFAULT_USER_LOG_TYPE, 1);
        metadataRepository.updateChunkStatus(id, Instant.now(), SAVED_TO_STASH, 1);

        assertThat(metadataRepository.getStashedChunks(receiver, "topic", STASH_ONLY).size(), is(1));
        assertThat(metadataRepository.getLastOffsets(receiver, "topic"), is(ImmutableMap.of("topic3", 350L)));
    }

    @Test
    public void shouldStashAndRecoverLater() {
        String receiver = someReceiver();
        ChunkMeta meta = meta("topic3", 350L);

        long id = metadataRepository.insertNewChunk(receiver, meta, DEFAULT_USER_LOG_TYPE, 1);
        metadataRepository.updateChunkStatus(id, Instant.now(), SAVED_TO_STASH, 1);
        assertThat(metadataRepository.getStashedChunks(receiver, "topic", STASH_ONLY).size(), is(1));

        metadataRepository.updateChunkStatus(id, Instant.now(), RECOVERED, 1);
        assertThat(metadataRepository.getStashedChunks(receiver, "topic", STASH_ONLY).size(), is(0));
    }

    @Test(expected = IncorrectResultSizeDataAccessException.class)
    public void shouldReturnZeroRowsFromStashForUnknownId() {
        metadataRepository.getChunkContentFromStash(-1L);
    }

    @Test
    public void shouldNotFailOnClearingStashForUnknownId() {
        metadataRepository.removeRecordFromStash(-1L);
    }
}
