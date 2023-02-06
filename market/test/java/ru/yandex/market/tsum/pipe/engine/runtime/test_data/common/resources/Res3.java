package ru.yandex.market.tsum.pipe.engine.runtime.test_data.common.resources;

import org.springframework.data.annotation.PersistenceConstructor;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.UUID;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 11.05.2017
 */
public class Res3 implements Resource {
    private final String s;

    @PersistenceConstructor
    @JsonCreator
    public Res3(String s) {
        this.s = s;
    }

    public String getS() {
        return s;
    }

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("974db00d-fc3b-41e9-9453-9578f86a1e33");
    }
}
