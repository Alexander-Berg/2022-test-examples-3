package ru.yandex.direct.jobs.advq.offline.export;

import org.junit.jupiter.api.Test;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThatCode;

class OfflineAdvqMRSpecTest {

    @Test
    void checkSpecIsValid() {
        assertThatCode(() -> OfflineAdvqMRSpec.getSpec("//tmp/", singleton("в")))
                .doesNotThrowAnyException();
    }
}
