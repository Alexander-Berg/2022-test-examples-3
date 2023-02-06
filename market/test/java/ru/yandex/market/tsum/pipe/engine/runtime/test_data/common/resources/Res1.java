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
public class Res1 implements Resource {
    private final String s;

    @PersistenceConstructor
    @JsonCreator
    public Res1(String s) {
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
        Res1 res1 = (Res1) o;
        return Objects.equals(s, res1.s);
    }

    @Override
    public int hashCode() {
        return Objects.hash(s);
    }

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("692ba970-1038-400a-9f61-2ddfd7106618");
    }
}
