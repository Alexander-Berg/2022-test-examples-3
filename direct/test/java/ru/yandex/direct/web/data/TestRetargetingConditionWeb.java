package ru.yandex.direct.web.data;

import java.util.Collections;

import ru.yandex.direct.web.core.model.retargeting.RetargetingConditionWeb;

import static ru.yandex.direct.web.data.TestCondition.defaultCondition;


public class TestRetargetingConditionWeb {
    public static RetargetingConditionWeb defaultRetargetingConditionWeb() {
        RetargetingConditionWeb retargetingConditionWeb = new RetargetingConditionWeb();
        retargetingConditionWeb.setConditionName("name");
        retargetingConditionWeb.setConditionDescription("description");
        retargetingConditionWeb.setConditions(Collections.singletonList(defaultCondition()));
        return retargetingConditionWeb;
    }

    public static RetargetingConditionWeb defaultRetargetingConditionWebForEstimate() {
        RetargetingConditionWeb retargetingConditionWeb = new RetargetingConditionWeb();
        retargetingConditionWeb.setConditions(Collections.singletonList(defaultCondition()));
        return retargetingConditionWeb;
    }

}
