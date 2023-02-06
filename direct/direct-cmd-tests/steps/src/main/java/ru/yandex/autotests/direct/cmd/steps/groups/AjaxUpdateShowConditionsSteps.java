package ru.yandex.autotests.direct.cmd.steps.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditions;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsGroup;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsObjects;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsRequest;
import ru.yandex.autotests.direct.cmd.data.ajaxupdatehowconditions.AjaxUpdateShowConditionsResponse;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class AjaxUpdateShowConditionsSteps extends DirectBackEndSteps {

    private final static Integer PHRASES_NUM_TO_CREATE = 12;

    @Step("POST cmd = ajaxUpdateShowConditions (обновление условий нацеливания)")
    public ErrorResponse postAjaxUpdateShowConditions(AjaxUpdateShowConditionsRequest request) {
        return post (CMD.AJAX_UPDATE_SHOW_CONDITIONS, request, ErrorResponse.class);
    }

    @Step("POST cmd = ajaxUpdateShowConditions (обновление условий нацеливания)")
    public ErrorResponse postAjaxUpdateShowConditionsAndExpectError(AjaxUpdateShowConditionsRequest request) {
        return post (CMD.AJAX_UPDATE_SHOW_CONDITIONS, request, ErrorResponse.class);
    }

    @Step("POST cmd = ajaxUpdateShowConditions (обновление условий нацеливания)")
    public AjaxUpdateShowConditionsResponse postAjaxUpdateShowConditions2(AjaxUpdateShowConditionsRequest request) {
        JsonObject result = post(CMD.AJAX_UPDATE_SHOW_CONDITIONS, request, JsonObject.class);
        result.remove("errors");
        result.remove("success");
        return new Gson().fromJson(result, AjaxUpdateShowConditionsResponse.class);
    }

    @Step("Обновляем условия нацеливания")
    public List<Phrase> postAjaxUpdateShowConditionsAndGetPhases(AjaxUpdateShowConditionsRequest request) {
        AjaxUpdateShowConditionsResponse response = postAjaxUpdateShowConditions2(request);
        assumeResponseCorrect(response);
        List<Phrase> result = new ArrayList<>();
        for (AjaxUpdateShowConditionsGroup ajaxUpdateShowConditionsGroup : response.getGroups()) {
            result.addAll(ajaxUpdateShowConditionsGroup.getPhrases().values());
        }
        return result;
    }

    @Step("Обновляем условия нацеливания с ожиданием ошибок по фразам")
    public List<String> postAjaxUpdateShowConditionsAndGetErrors(AjaxUpdateShowConditionsRequest request) {
        AjaxUpdateShowConditionsResponse response = postAjaxUpdateShowConditions2(request);
        List<String> result = new ArrayList<>();
        for (AjaxUpdateShowConditionsGroup group : response.getGroups()) {
            if(group.getErrors() != null) {
                result.addAll(group.getErrors());
            }
        }
        return result;
    }

    @Step("остановка фраз через у кампании {0} группы {1}")
    public void suspendPhrases(Long cid, Long groupId, String ulogin, String... phraseIds) {
        postAjaxUpdateShowConditions(new AjaxUpdateShowConditionsRequest()
                .withCid(cid.toString())
                .withGroupPhrases(groupId.toString(),
                        new AjaxUpdateShowConditionsObjects()
                                .withSuspended(phraseIds, true))
                .withUlogin(ulogin));
    }

    @Step("удаление фраз через у кампании {0} группы {1}")
    public void deletePhrases(Long cid, Long groupId, String ulogin, String... phraseIds) {
        postAjaxUpdateShowConditions(new AjaxUpdateShowConditionsRequest()
                .withCid(cid.toString())
                .withGroupPhrases(groupId.toString(),
                        new AjaxUpdateShowConditionsObjects()
                                .withDeleted(phraseIds))
                .withUlogin(ulogin));
    }

    @Step("остановка ретаргетингов через у кампании {0} группы {1}")
    public void suspendRetargetings(Long cid, Long groupId, String ulogin, String... phraseIds) {
        postAjaxUpdateShowConditions(new AjaxUpdateShowConditionsRequest()
                .withCid(cid.toString())
                .withRetargetings(groupId.toString(),
                        new AjaxUpdateShowConditionsObjects()
                                .withSuspended(phraseIds, true))
                .withUlogin(ulogin));
    }

    @Step("удаление ретаргетингов через у кампании {0} группы {1}")
    public void deleteRetargetings(Long cid, Long groupId, String ulogin, String... phraseIds) {
        postAjaxUpdateShowConditions(new AjaxUpdateShowConditionsRequest()
                .withCid(cid.toString())
                .withRetargetings(groupId.toString(),
                        new AjaxUpdateShowConditionsObjects()
                                .withDeleted(phraseIds))
                .withUlogin(ulogin));
    }

    @Step("остановка ретаргетингов через у кампании {0} группы {1}")
    public void suspendInterests(Long cid, Long groupId, String ulogin, String... interestIds) {
        postAjaxUpdateShowConditions(new AjaxUpdateShowConditionsRequest()
                .withCid(cid.toString())
                .withInterest(groupId.toString(),
                        new AjaxUpdateShowConditionsObjects()
                                .withSuspended(interestIds, true))
                .withUlogin(ulogin));
    }

    @Step("удаление ретаргетингов через у кампании {0} группы {1}")
    public void deleteInterests(Long cid, Long groupId, String ulogin, String... interestIds) {
        postAjaxUpdateShowConditions(new AjaxUpdateShowConditionsRequest()
                .withCid(cid.toString())
                .withInterest(groupId.toString(),
                        new AjaxUpdateShowConditionsObjects()
                                .withDeleted(interestIds))
                .withUlogin(ulogin));
    }

    @Step("создание тестовых фраз в группе у кампании {0} группы {1}")
    public void addTestPhrases(Long cid, Long groupId, String ulogin) {
        List<AjaxUpdateShowConditions> bidsConditions = IntStream.range(1, PHRASES_NUM_TO_CREATE).boxed()
                .map(i -> new AjaxUpdateShowConditions()
                        .withPrice(i.toString())
                        .withPhrase("test phrase " + i.toString())
                )
                .collect(toList());
        AjaxUpdateShowConditionsObjects bids = new AjaxUpdateShowConditionsObjects().withAdded(
                bidsConditions
        );
        AjaxUpdateShowConditionsRequest request = new AjaxUpdateShowConditionsRequest()
                .withCid(String.valueOf(cid))
                .withGroupPhrases(String.valueOf(groupId), bids)
                .withUlogin(ulogin);
        postAjaxUpdateShowConditions(request);
    }

    private void assumeResponseCorrect(AjaxUpdateShowConditionsResponse response) {
        assumeThat("В ответе присутствуют элементы", response.getGroups(), hasSize(greaterThan(0)));
        for (AjaxUpdateShowConditionsGroup group : response.getGroups()) {
            assumeThat("в ответе нет ошибок", group.getErrors(), equalTo(null));
            assumeThat("в ответе присутствуют фразы", group.getPhrases(), not(equalTo(null)));
        }
    }

}
