package ru.yandex.market.tsum.pipe.engine.runtime.test_data.resource_injection.resources;

import org.springframework.data.annotation.PersistenceConstructor;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.UUID;

public class DerivedResource451 extends Resource451 {
    @PersistenceConstructor
    @JsonCreator
    public DerivedResource451(Integer value) {
        super(value);
    }

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("f293db27-b2ae-4e37-bceb-4c2f29cb11d4");
    }
}
