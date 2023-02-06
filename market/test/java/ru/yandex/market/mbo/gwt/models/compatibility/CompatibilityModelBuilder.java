package ru.yandex.market.mbo.gwt.models.compatibility;

/**
 * @author s-ermakov
 */
public class CompatibilityModelBuilder {

    private long modelId;
    private String modelTitle;

    private CompatibilityModel.Direction direction = CompatibilityModel.Direction.NONE;

    public static CompatibilityModelBuilder newBuilder() {
        return new CompatibilityModelBuilder();
    }

    public CompatibilityModelBuilder setModel(long modelId, String modelTitle) {
        this.modelId = modelId;
        this.modelTitle = modelTitle;
        return this;
    }

    public CompatibilityModelBuilder setDirection(CompatibilityModel.Direction direction) {
        this.direction = direction;
        return this;
    }

    public CompatibilityModel create() {
        CompatibilityModel compatibilityModel = new CompatibilityModel();
        compatibilityModel.setDirection(direction);
        compatibilityModel.setModelId(modelId);
        compatibilityModel.setModelTitle(modelTitle);
        return compatibilityModel;
    }
}
