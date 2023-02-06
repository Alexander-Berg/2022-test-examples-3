package ru.yandex.autotests.direct.cmd.data.retargeting;


import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;

import java.util.List;

public class AjaxReplaceGoalsInRetargetingsRequest extends BasicDirectRequest {

    @SerializeKey("json_replace_goals")
    @SerializeBy(ValueToJsonSerializer.class)
    private List<ReplaceGoal> replaceGoals;

    public List<ReplaceGoal> getReplaceGoals() {
        return replaceGoals;
    }

    public AjaxReplaceGoalsInRetargetingsRequest withReplaceGoals(List<ReplaceGoal> replaceGoals) {
        this.replaceGoals = replaceGoals;
        return this;
    }
}
