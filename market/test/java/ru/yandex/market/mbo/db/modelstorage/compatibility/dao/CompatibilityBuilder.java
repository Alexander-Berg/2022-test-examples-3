package ru.yandex.market.mbo.db.modelstorage.compatibility.dao;

import ru.yandex.market.mbo.db.modelstorage.compatibility.Compatibility;

public class CompatibilityBuilder {
    private long id;
    private long modelId1;
    private long modelId2;
    private Compatibility.Direction direction = Compatibility.Direction.NONE;

    public static CompatibilityBuilder newBuilder() {
        return new CompatibilityBuilder();
    }

    public static CompatibilityBuilder newBuilder(long modelId1, long modelId2, Compatibility.Direction direction) {
        return newBuilder()
                .setModelId1(modelId1)
                .setModelId2(modelId2)
                .setDirection(direction);
    }

    public CompatibilityBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public CompatibilityBuilder setModelId1(long modelId1) {
        this.modelId1 = modelId1;
        return this;
    }

    public CompatibilityBuilder setModelId2(long modelId2) {
        this.modelId2 = modelId2;
        return this;
    }

    public CompatibilityBuilder setDirection(Compatibility.Direction direction) {
        this.direction = direction;
        return this;
    }

    public Compatibility create() {
        Compatibility compatibility = new Compatibility();
        compatibility.setId(id);
        compatibility.setModelId1(modelId1);
        compatibility.setModelId2(modelId2);
        compatibility.setDirection(direction);
        return compatibility;
    }
}
