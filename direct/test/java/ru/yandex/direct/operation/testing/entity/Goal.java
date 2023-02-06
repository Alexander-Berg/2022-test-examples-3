package ru.yandex.direct.operation.testing.entity;

import ru.yandex.direct.model.ModelWithId;

public class Goal implements ModelWithId {
    private Long id;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Goal withId(Long id) {
        this.id = id;
        return this;
    }
}
