package ru.yandex.direct.useractionlog;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.junit.Test;

@ParametersAreNonnullByDefault
public class GtidTest {
    /**
     * Проверка корректной обработки случая, который однажды уже произошёл. DIRECT-76117
     */
    @Test
    public void fromGtidSet() {
        String realCase = ""
                + "885b60a2-81d9-cca8-4371-f7909c8af65f:1-2459732885,\n"
                + "8be3b029-e454-4aec-a534-de630c8ae6f3:1,\n"
                + "e46271a8-13a3-4f63-4aad-90531893f29b:1-2210746015,\n"
                + "eeb7e443-9026-dadf-29b6-63161f62d147:1-58338559";
        Assertions.assertThat(Gtid.fromGtidSet(realCase))
                .containsExactly(
                        new Gtid("885b60a2-81d9-cca8-4371-f7909c8af65f", 2459732885L),
                        new Gtid("8be3b029-e454-4aec-a534-de630c8ae6f3", 1),
                        new Gtid("e46271a8-13a3-4f63-4aad-90531893f29b", 2210746015L),
                        new Gtid("eeb7e443-9026-dadf-29b6-63161f62d147", 58338559));
    }
}
