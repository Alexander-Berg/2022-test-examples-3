package ru.yandex.market.delivery.transport_manager.repository.mappers;

import java.time.Instant;
import java.time.Month;
import java.time.ZonedDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.checkpoint_log.CheckpointLog;
import ru.yandex.market.delivery.transport_manager.util.TimeUtil;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@DatabaseSetup({
    "/repository/checkpoint_log/processed_checkpoints.xml",
})
class CheckpointLogMapperTest extends AbstractContextualTest {

    public static final Instant CHECKPOINT_DATE =
        ZonedDateTime.of(2022, Month.APRIL.getValue(), 8, 12, 30, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET).toInstant();
    @Autowired
    private CheckpointLogMapper mapper;

    @Test
    @ExpectedDatabase(
        value = "/repository/checkpoint_log/expected/after_modification.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void replace() {
        mapper.replace(
            EntityType.MOVEMENT,
            "10",
            "CHP1",
            CHECKPOINT_DATE
        );
        mapper.replace(
            EntityType.MOVEMENT,
            "11",
            "CHP2",
            CHECKPOINT_DATE
        );
    }

    @Test
    void find() {
        mapper.replace(
            EntityType.MOVEMENT,
            "10",
            "CHP1",
            CHECKPOINT_DATE
        );

        CheckpointLog checkpointLog = mapper.find(EntityType.MOVEMENT, "10");

        softly.assertThat(checkpointLog.getCheckpointDate()).isEqualTo(CHECKPOINT_DATE);

        softly.assertThat(mapper.find(EntityType.MOVEMENT, "100500")).isNull();
    }
}
