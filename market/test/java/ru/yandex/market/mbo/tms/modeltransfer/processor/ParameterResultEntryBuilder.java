package ru.yandex.market.mbo.tms.modeltransfer.processor;

import ru.yandex.market.mbo.gwt.models.transfer.step.ParameterInfo;
import ru.yandex.market.mbo.gwt.models.transfer.step.ParameterResultEntry;
import ru.yandex.market.mbo.gwt.models.transfer.step.ResultEntry;

/**
 * @author danfertev
 * @since 05.10.2018
 */
public class ParameterResultEntryBuilder {
    private ParameterResultEntry result;

    public static ParameterResultEntryBuilder newBuilder() {
        ParameterResultEntryBuilder builder = new ParameterResultEntryBuilder();
        builder.result = new ParameterResultEntry();
        return builder;
    }

    public ParameterResultEntryBuilder sourceCategory(long id, long paramId, String xslName, String paramName) {
        this.result.setSourceParameter(new ParameterInfo(id, paramId, xslName, paramName));
        return this;
    }

    public ParameterResultEntryBuilder destinationCategory(long id, long paramId, String xslName, String paramName) {
        this.result.setDestinationParameter(new ParameterInfo(id, paramId, xslName, paramName));
        return this;
    }

    public ParameterResultEntryBuilder status(ResultEntry.Status status) {
        this.result.setStatus(status);
        return this;
    }

    public ParameterResultEntryBuilder statusMessage(String status) {
        this.result.setStatusMessage(status);
        return this;
    }

    public ParameterResultEntry build() {
        return result;
    }
}
