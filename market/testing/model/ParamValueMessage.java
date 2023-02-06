package ru.yandex.market.partner.testing.model;

import ru.yandex.market.core.param.model.ParamValue;

/**
 * {@link ParamValue} в связке с {@link MessageDetails}.
 *
 * @author avetokhin 31/08/16.
 */
public class ParamValueMessage {
    private final ParamValue paramValue;
    private final MessageDetails message;

    public ParamValueMessage(final ParamValue paramValue, final MessageDetails message) {
        this.paramValue = paramValue;
        this.message = message;
    }

    public ParamValue getParamValue() {
        return paramValue;
    }

    public MessageDetails getMessage() {
        return message;
    }

}
