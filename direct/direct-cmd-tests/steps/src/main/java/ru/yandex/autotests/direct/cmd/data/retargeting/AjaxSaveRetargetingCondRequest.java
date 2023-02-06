package ru.yandex.autotests.direct.cmd.data.retargeting;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;

public class AjaxSaveRetargetingCondRequest extends BasicDirectRequest {

    @SerializeKey("json_retargeting_condition")
    @SerializeBy(ValueToJsonSerializer.class)
    private RetargetingCondition jsonRetargetingCondition;

    public AjaxSaveRetargetingCondRequest withJsonRetargetingCondition(RetargetingCondition jsonRetargetingCondition) {
        this.jsonRetargetingCondition = jsonRetargetingCondition;
        return this;
    }
}
