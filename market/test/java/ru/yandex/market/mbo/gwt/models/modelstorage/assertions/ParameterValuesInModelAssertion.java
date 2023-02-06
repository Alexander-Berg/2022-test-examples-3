package ru.yandex.market.mbo.gwt.models.modelstorage.assertions;

import com.google.common.base.Preconditions;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

/**
 * @author s-ermakov
 */
public class ParameterValuesInModelAssertion extends BaseParameterValuesAssertion<ParameterValuesInModelAssertion> {

    private CommonModel model;
    private long paramId;
    private String xslName;

    public ParameterValuesInModelAssertion(CommonModel model, String xslName) {
        super(model.getParameterValues(xslName), ParameterValuesInModelAssertion.class);
        Preconditions.checkNotNull(model, "Expected not null model");
        Preconditions.checkNotNull(xslName, "Expected not null xslName");
        this.model = model;
        this.xslName = xslName;
    }

    public ParameterValuesInModelAssertion(CommonModel model, long paramId) {
        super(model.getParameterValues(paramId), ParameterValuesInModelAssertion.class);
        Preconditions.checkNotNull(model, "Expected not null model");
        this.model = model;
        this.paramId = paramId;
    }

    @Override
    protected String createSubFailMessage() {
        return String.format("%s param for model: %s",
            xslName != null ? "'" + xslName + "'" : String.valueOf(paramId),
            model);
    }
}
