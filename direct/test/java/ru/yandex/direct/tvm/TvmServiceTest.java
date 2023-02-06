package ru.yandex.direct.tvm;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class TvmServiceTest {
    private static final Set<TvmService> KNOWN_INVALID_SERVICES = ImmutableSet.of(TvmService.UNKNOWN,
            TvmService.DUMMY);

    /**
     * Технически этот тест лишний, так как уникальность ключей проверяется статически в ImmutableMap$Builder
     */
    @Test
    void idUnique() {
        long count = Arrays.stream(TvmService.values())
                .map(TvmService::getId)
                .distinct()
                .count();
        assertSoftly(softly -> softly.assertThat(count).isEqualTo(TvmService.values().length));
    }

    @Test
    void idValid() {
        List<TvmService> servicesWithInvalidId = Arrays.stream(TvmService.values())
                .filter(s -> !KNOWN_INVALID_SERVICES.contains(s))
                .filter(s -> s.getId() <= 0)
                .collect(Collectors.toList());
        assertSoftly(softly -> softly.assertThat(servicesWithInvalidId).isEmpty());
    }
}
