package ru.yandex.market.tpl.core.domain.sort_center;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.tpl.core.domain.sc.OrderStatusUpdate;
import ru.yandex.market.tpl.core.domain.sc.ScManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kukabara
 */
class ScManagerResolveChangesTest {

    private static final Instant TIME = Instant.now();
    private static final Instant TIME_1 = TIME.plusSeconds(1L);
    private static final Instant TIME_2 = TIME.plusSeconds(2L);
    private static final Instant TIME_3 = TIME.plusSeconds(3L);

    @InjectMocks
    ScManager scManager;

    private static OrderStatusUpdate get(int i, Instant time) {
        return new OrderStatusUpdate(i, time);
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void empty() {
        assertThat(scManager.resolveChanges(List.of(), List.of())).isEmpty();
    }

    @Test
    void shouldReturnAllUpdatesWithoutDuplicates() {
        assertThat(scManager.resolveChanges(
                List.of(),
                List.of(get(100, TIME), get(101, TIME_2), get(100, TIME_1)))
        )
                .containsExactly(get(100, TIME), get(101, TIME_2));
    }

    @Test
    void shouldReturnNewUpdates() {
        assertThat(scManager.resolveChanges(
                List.of(get(100, TIME), get(101, TIME_1)),
                List.of(get(100, TIME), get(101, TIME_1), get(110, TIME_2)))
        )
                .containsExactly(get(110, TIME_2));
    }

    @Test
    void shouldReturnNoUpdates() {
        assertThat(scManager.resolveChanges(
                List.of(get(100, TIME), get(101, TIME_1), get(102, TIME_2)),
                List.of(get(100, TIME), get(101, TIME_1), get(102, TIME_2))
        ))
                .isEmpty();
    }

    @Test
    void shouldSkipAlreadyExisting() {
        assertThat(scManager.resolveChanges(
                List.of(get(100, TIME)),
                List.of(get(100, TIME_1), get(100, TIME_2), get(101, TIME_2), get(101, TIME_3))
        ))
                .containsExactly(get(101, TIME_2));
    }

}
