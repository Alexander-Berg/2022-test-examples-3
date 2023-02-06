package ru.yandex.autotests.direct.cmd.steps.base;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.CSRFToken;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.KeyValueBean;

@KeyValueBean()
public class DirectBeanWrapper {

    private static final String GET_VARS_FALSE = null;
    private static final String GET_VARS_TRUE = "1";

    @SerializeKey
    private CMD cmd;

    @SerializeKey("csrf_token")
    private CSRFToken token;

    @SerializeKey("get_vars")
    private String getVars = GET_VARS_TRUE;

    @KeyValueBean
    private Object wrappedBean;

    public DirectBeanWrapper(CMD cmd, CSRFToken token, Object wrappedBean) {
        this.cmd = cmd;
        this.token = token;
        this.wrappedBean = wrappedBean;
    }

    public DirectBeanWrapper withGetVars(boolean getVars) {
        this.getVars = getVars ? GET_VARS_TRUE : GET_VARS_FALSE;
        return this;
    }
}
