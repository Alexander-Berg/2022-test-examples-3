package ru.yandex.autotests.direct.cmd.steps.retargeting;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.commons.group.RetargetingCondition;
import ru.yandex.autotests.direct.cmd.data.retargeting.AjaxDeleteRetargetingCondResponse;
import ru.yandex.autotests.direct.cmd.data.retargeting.AjaxGetGoalsForRetargetingItem;
import ru.yandex.autotests.direct.cmd.data.retargeting.AjaxGetRetCondWithGoalsResponse;
import ru.yandex.autotests.direct.cmd.data.retargeting.AjaxReplaceGoalsInRetargetingsRequest;
import ru.yandex.autotests.direct.cmd.data.retargeting.AjaxSaveRetargetingCondRequest;
import ru.yandex.autotests.direct.cmd.data.retargeting.AjaxSaveRetargetingCondResponse;
import ru.yandex.autotests.direct.cmd.data.retargeting.CommonAjaxRetConditionResponse;
import ru.yandex.autotests.direct.cmd.data.retargeting.CommonRetargetingCondRequest;
import ru.yandex.autotests.direct.cmd.data.retargeting.ShowRetargetingCondResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.direct.cmd.data.retargeting.CommonAjaxRetConditionResponse.RESULT_OK;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class RetargetingSteps extends DirectBackEndSteps {

    @Step("POST cmd = ajaxSaveRetargetingCond (сохранение условия ретаргетинга)")
    public AjaxSaveRetargetingCondResponse postSaveRetargetingCond(AjaxSaveRetargetingCondRequest request) {
        return post(CMD.AJAX_SAVE_RETARGETING_COND, request, AjaxSaveRetargetingCondResponse.class);
    }

    @Step("Сохранение условия ретаргетинга (cmd = ajaxSaveRetargetingCond)")
    public AjaxSaveRetargetingCondResponse saveRetargetingCondition(RetargetingCondition retCondition, String uLogin) {
        return postSaveRetargetingCond(new AjaxSaveRetargetingCondRequest().
                withJsonRetargetingCondition(retCondition).
                withUlogin(uLogin));
    }

    @Step("Сохранение условия ретаргетинга с проверкой положительного ответа (cmd = ajaxSaveRetargetingCond)")
    public Long saveRetargetingConditionWithAssumption(RetargetingCondition retCondition, String uLogin) {
        AjaxSaveRetargetingCondResponse response = saveRetargetingCondition(retCondition, uLogin);
        assumeThat("ответ ручки положительный", response.getResult(), equalTo(RESULT_OK));
        assumeThat("ответ содержит id созданного условия ретаргетинга", response.getRetCondId(), notNullValue());
        return response.getRetCondId();
    }

    @Step("POST cmd = ajaxDeleteRetargetingCond (удаление условия ретаргетинга)")
    public AjaxDeleteRetargetingCondResponse postDeleteRetargetingCond(CommonRetargetingCondRequest request) {
        return post(CMD.AJAX_DELETE_RETARGETING_COND, request, AjaxDeleteRetargetingCondResponse.class);
    }

    @Step("Удаление условия ретаргетинга (ret_cond_id: {0}; ulogin: {1}")
    public AjaxDeleteRetargetingCondResponse deleteRetargetingCondition(Long retCondId, String ulogin) {
        return postDeleteRetargetingCond(
                new CommonRetargetingCondRequest().
                        withRetCondId(retCondId != null ? String.valueOf(retCondId) : null).
                        withUlogin(ulogin));
    }

    @Step("Удаление нескольких условий ретаргетинга (ret_cond_id: {1}; ulogin: {0}")
    public AjaxDeleteRetargetingCondResponse deleteRetargetingConditions(String ulogin, Long... retCondIds) {
        String retCondIdsStr = StringUtils.join(retCondIds, ",");
        return postDeleteRetargetingCond(
                new CommonRetargetingCondRequest().
                        withRetCondId(retCondIdsStr).
                        withUlogin(ulogin));
    }

    @Step("GET cmd = showRetargetingCond (получение всех условий ретаргетинга) (ulogin: {0})")
    public Map<Long, RetargetingCondition> getShowRetargetingCond(String ulogin) {
        return get(CMD.SHOW_RETARGETING_COND,
                new BasicDirectRequest().withUlogin(ulogin),
                ShowRetargetingCondResponse.class).getAllRetargetingConditions();
    }

    @Step("GET cmd = ajaxGetGoalsForRetargeting (получение всех целей) (ulogin: {0})")
    public List<AjaxGetGoalsForRetargetingItem> getAjaxGetGoalsForRetargeting(String ulogin) {
        AjaxGetGoalsForRetargetingItem[] resp = get(CMD.AJAX_GET_GOALS_FOR_RETARGETING,
                new BasicDirectRequest().withUlogin(ulogin),
                AjaxGetGoalsForRetargetingItem[].class);
        return Arrays.asList(resp);
    }

    @Step("POST cmd = ajaxReplaceGoalsInRetargetings (замена целей в ретаргетинге)")
    public CommonAjaxRetConditionResponse postReplaceGoalsInRetargetings(AjaxReplaceGoalsInRetargetingsRequest request) {
        return post(CMD.AJAX_REPLACE_GOALS_IN_RETARGETING, request, CommonAjaxRetConditionResponse.class);
    }

    @Step("POST cmd = ajaxGetRetCondWithGoals (получение  условий ретаргетинга с данными целей)")
    public AjaxGetRetCondWithGoalsResponse getRetCondWithGoals(String login, Long... retargetingConditionIds) {
        return get(CMD.AJAX_GET_RET_COND_WITH_GOALS,  new CommonRetargetingCondRequest()
                        .withRetCondId(StringUtils.join(retargetingConditionIds, ","))
                        .withUlogin(login),
                AjaxGetRetCondWithGoalsResponse.class);
    }
}
