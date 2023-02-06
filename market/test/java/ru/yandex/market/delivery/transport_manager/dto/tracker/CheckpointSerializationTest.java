package ru.yandex.market.delivery.transport_manager.dto.tracker;

import java.time.Instant;
import java.util.Date;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CheckpointSerializationTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @DisplayName("Парсинг и сериализация")
    @Test
    @SneakyThrows
    void testSerialziationAndDeserizlization() {
        Checkpoint expected = checkpoint();

        String json = objectMapper.writeValueAsString(expected);
        Checkpoint actual = objectMapper.readValue(json, Checkpoint.class);
        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @DisplayName("Проверяем, что старый формат с Date эквивалентен новому: сериализуем старое значение, парсим новое")
    @Test
    @SneakyThrows
    void testOldToNew() {
        OldCheckpoint expected = oldCheckpoint();

        String json = objectMapper.writeValueAsString(expected);
        Checkpoint actual = objectMapper.readValue(json, Checkpoint.class);
        Assertions.assertThat(actual).isEqualTo(checkpoint());
    }

    @DisplayName("Проверяем, что старый формат с Date эквивалентен новому: сериализуем новое значение, парсим старое")
    @Test
    @SneakyThrows
    void testNewToOld() {
        Checkpoint expected = checkpoint();

        String json = objectMapper.writeValueAsString(expected);
        OldCheckpoint actual = objectMapper.readValue(json, OldCheckpoint.class);
        Assertions.assertThat(actual).isEqualTo(oldCheckpoint());
    }

    @Nonnull
    private Checkpoint checkpoint() {
        Checkpoint expected = new Checkpoint();
        expected.setCheckpointDate(Instant.parse("2022-01-02T12:01:02.000Z"));
        expected.setCheckpointStatus(CheckpointStatus.INFO_RECEIVED);
        expected.setAcquiredByTrackerDate(Instant.parse("2022-01-02T12:02:03.100Z"));
        expected.setEntityType(EntityType.MOVEMENT);
        expected.setId(1L);
        expected.setDeliveryCheckpointStatus(100);
        expected.setTrackId(100L);
        expected.setMessage("Message");
        return expected;
    }

    @Nonnull
    @SneakyThrows
    private OldCheckpoint oldCheckpoint() {
        OldCheckpoint expected = new OldCheckpoint();
        expected.setCheckpointDate(Date.from(Instant.parse("2022-01-02T12:01:02.000Z")));
        expected.setCheckpointStatus(CheckpointStatus.INFO_RECEIVED);
        expected.setAcquiredByTrackerDate(Date.from(Instant.parse("2022-01-02T12:02:03.100Z")));
        expected.setEntityType(EntityType.MOVEMENT);
        expected.setId(1L);
        expected.setDeliveryCheckpointStatus(100);
        expected.setTrackId(100L);
        expected.setMessage("Message");
        return expected;
    }

    @Data
    static class OldCheckpoint {
        private Long id;
        private Integer deliveryCheckpointStatus;
        private Date checkpointDate;
        private Date acquiredByTrackerDate;
        private Long trackId;
        private String message;
        private CheckpointStatus checkpointStatus;
        private EntityType entityType;
    }

}
