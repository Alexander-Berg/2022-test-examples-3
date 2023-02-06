package ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources;

import org.springframework.data.annotation.PersistenceConstructor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 11.05.2017
 */
public class Res2 implements Resource {
    private final String s;

    @PersistenceConstructor
    @JsonCreator
    public Res2(String s) {
        this.s = s;
    }

    public String getS() {
        return s;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Res2 res2 = (Res2) o;
        return Objects.equals(s, res2.s);
    }

    @Override
    public int hashCode() {
        return Objects.hash(s);
    }

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("a44b32d5-3e2f-4e48-a8c3-586dd8944f0b");
    }
}
