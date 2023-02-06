package ru.yandex.autotests.direct.cmd.steps.groups.dynamicconditions;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.group.DynamicCondition;
import ru.yandex.autotests.direct.cmd.data.dynamicconditions.AjaxEditDynamicConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.dynamicconditions.DynamicConditionMap;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class AjaxEditDynamicConditionsSteps extends DirectBackEndSteps {

    @Step("POST cmd = ajaxEditAdGroupDynamicConditions (сохранения условия ДТО)")
    public CommonResponse postAjaxEditDynamicConditions(AjaxEditDynamicConditionsRequest params) {
        return post(CMD.AJAX_EDIT_DYNAMIC_CONDITIONS, params, CommonResponse.class);
    }

    @Step("POST cmd = ajaxEditAdGroupDynamicConditions (сохранения условия ДТО с невалидными данными)")
    public HashMap<String, Map<String, List<String>>> postAjaxEditDynamicConditionsInvalidData(AjaxEditDynamicConditionsRequest params) {
        return post(CMD.AJAX_EDIT_DYNAMIC_CONDITIONS, params, HashMap.class);
    }

    @Step("изменение условий дто через ajaxEditAdGroupDynamicConditions")
    public void dynamicConditionsChangeWithAssumption(Long cid, Long groupId, DynamicCondition condition, String uLogin) {
        CommonResponse response = postAjaxEditDynamicConditions(AjaxEditDynamicConditionsRequest
                .fromDynamicCondition(condition.withAdGroupId(groupId))
                .withCid(String.valueOf(cid))
                .withUlogin(uLogin));
        assumeThat("динамическое условие изменено", response.getResult(), equalTo("ok"));
    }

    @Step("удаление условий дто через ajaxEditAdGroupDynamicConditions")
    public void dynamicConditionsDeleteWithAssumption(Long cid, Long groupId, String uLogin, Long... dynId) {
        CommonResponse response = postAjaxEditDynamicConditions(new AjaxEditDynamicConditionsRequest()
                .withCondition(groupId, new DynamicConditionMap()
                        .withDeleteIds(Arrays.asList(dynId)))
                .withCid(String.valueOf(cid))
                .withUlogin(uLogin));
        assumeThat("динамические условия удалены", response.getResult(), equalTo("ok"));
    }
}
