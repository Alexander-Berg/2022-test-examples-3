package ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.resources;

import org.springframework.data.annotation.PersistenceConstructor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.UUID;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 22.03.17
 */
public class Resource451 implements Resource {
    private Integer value;

    @PersistenceConstructor
    @JsonCreator
    public Resource451(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("18d16b1a-c465-48a4-84a0-1ad7bb230e8a");
    }
}
