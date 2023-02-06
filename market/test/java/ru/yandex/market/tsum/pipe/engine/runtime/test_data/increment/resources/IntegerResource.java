package ru.yandex.market.tsum.pipe.engine.runtime.test_data.increment.resources;

import org.springframework.data.annotation.PersistenceConstructor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.UUID;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 16.03.17
 */
public class IntegerResource implements Resource {
    private Integer value;

    @PersistenceConstructor
    @JsonCreator
    public IntegerResource(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("3b99c64b-e532-4792-a45b-ea1aee063131");
    }
}
