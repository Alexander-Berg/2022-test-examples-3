package ru.yandex.autotests.direct.cmd.data.retargeting;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;

import java.util.Map;

public class ShowRetargetingCondResponse {

    @SerializedName("all_retargeting_conditions")
    private Map<Long, RetargetingCondition> allRetargetingConditions;

    public Map<Long, RetargetingCondition> getAllRetargetingConditions() {
        return allRetargetingConditions;
    }
}
