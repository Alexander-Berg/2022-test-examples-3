package ru.yandex.market.aliasmaker.meta.be;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * @author apluhin
 * @created 4/27/22
 */
public class ShardSetStateTest {

    @Test
    public void testMarkShardAsDead() {
        Instant time = Instant.now().minus(5, ChronoUnit.MINUTES);
        ShardSetState state = new ShardSetState.Builder()
                .updateShard("1", mockShard("1", time, true))
                .updateShard("2", mockShard("2", time, true))
                .updateShard("3", mockShard("3", time, true)).build();

        ShardSetState newState = state.toBuilder().markDead(
                List.of(new ShardKey("1", "1", 80, "1"))).build();
        Assertions.assertThat(newState.getShardInfo()).containsOnlyElementsOf(
                new ShardSetState.Builder()
                        .updateShard("1", mockShard("1", time, false))
                        .updateShard("2", mockShard("2", time, true))
                        .updateShard("3", mockShard("3", time, true))
                        .build().getShardInfo()
        );
    }

    private ShardInfo mockShard(String serviceName, Instant hb, boolean hbResult) {
        return new ShardInfo(
                new CurrentStateResponse.InstancePart(serviceName, null, serviceName, null, null, 80),
                serviceName,
                hb.getEpochSecond(),
                hbResult,
                null
        );
    }
}
