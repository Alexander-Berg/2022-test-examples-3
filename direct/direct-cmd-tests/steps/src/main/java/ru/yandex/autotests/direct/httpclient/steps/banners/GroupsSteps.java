package ru.yandex.autotests.direct.httpclient.steps.banners;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.DynamicGroupsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.CmdBeans.GroupsCmdBean;
import ru.yandex.autotests.direct.httpclient.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.httpclient.data.banners.lite.EasyGroupParameters;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPathJSONPopulater;
import ru.yandex.qatools.allure.annotations.Step;

/**
 * Created with IntelliJ IDEA.
 * User: shmykov
 * 23.09.14
 */
public class GroupsSteps extends DirectBackEndSteps {

    @Step("Открываем страницу 1-го шага мультиредактирования")
    public DirectResponse openShowCampMultiEdit(CSRFToken token, GroupsParameters params) {
        return execute(getRequestBuilder().get(CMD.SHOW_CAMP_MULTI_EDIT, token, params));
    }

    public GroupsCmdBean openShowCampMultiEdit(String cid, String adGroupID, String login) {
        GroupsParameters params = new GroupsParameters();
        params.setUlogin(login);
        params.setCid(cid);
        params.setAdgroupIds(adGroupID);
        params.setBannerStatus("all");
        return JsonPathJSONPopulater.evaluateResponse(
                execute(getRequestBuilder().get(CMD.SHOW_CAMP_MULTI_EDIT, params)), new GroupsCmdBean());
    }


    @Step("Вызываем ")
    public DirectResponse addBannerMultiEdit(CSRFToken token, GroupsParameters params) {
        return execute(getRequestBuilder().get(CMD.ADD_BANNER_MULTI_EDIT, token, params));
    }

    @Step("Открываем страницу редактирования легкого объявления")
    public DirectResponse openEditBannerEasy(CSRFToken token, EasyGroupParameters params) {
        return execute(getRequestBuilder().get(CMD.EDIT_BANNER_EASY, token, params));
    }

    @Step("Возвращаемся со второго шага редактирования на первый")
    public DirectResponse goBackShowCampMultiEdit(CSRFToken token, GroupsParameters params) {
        return execute(getRequestBuilder().post(CMD.SHOW_CAMP_MULTI_EDIT, token, params));
    }

    @Step("Открываем страницу редактирования только текстов объявлений")
    public DirectResponse openShowCampMultiEditLight(CSRFToken token, GroupsParameters params) {
        return execute(getRequestBuilder().get(CMD.SHOW_CAMP_MULTI_EDIT_LIGHT, token, params));
    }

    @Step("Сохраняем группы")
    public DirectResponse saveGroups(CSRFToken token, GroupsParameters params) {
        return execute(getRequestBuilder().post(CMD.SAVE_TEXT_ADGROUPS, token, params));
    }

    @Step("Сохраняем мобильную группу")
    public DirectResponse saveMobileGroups(CSRFToken token, GroupsParameters params) {
        return execute(getRequestBuilder().post(CMD.SAVE_MOBILE_ADGROUPS, token, params));
    }

    @Step("Сохраняем группы в легком интерфейсе")
    public DirectResponse saveEasyGroups(CSRFToken token, EasyGroupParameters params) {
        return execute(getRequestBuilder().post(CMD.SAVE_BANNER_EASY, token, params));
    }

    @Step("создание нового мобильного баннера")
    public DirectResponse openAddAdgroupsMobileContent(CSRFToken token, GroupsParameters params) {
        return execute(getRequestBuilder().post(CMD.ADD_ADGROUPS_MOBILE_CONTENT, token, params));
    }

    @Step("Открываем страницу создания динамического баннера")
    public DirectResponse addAdDynamicAdGroups(CSRFToken token, GroupsParameters params) {
        return execute(getRequestBuilder().get(CMD.ADD_DYNAMIC_AD_GROUPS, token, params));
    }

    @Step("Сохраняем динамический баннер")
    public DirectResponse saveDynamicAdGroups(CSRFToken token, GroupsParameters params) {
        return execute(getRequestBuilder().post(CMD.SAVE_DYNAMIC_AD_GROUPS, token, params));
    }

    public DirectResponse editDynamicAdGroups(CSRFToken token, GroupsParameters params) {
        return execute(getRequestBuilder().post(CMD.EDIT_DYNAMIC_AD_GROUPS, token, params));
    }


    @Step("Редактируем динамический баннер")
    public DynamicGroupsCmdBean editDynamicAdGroups(GroupsParameters params) {
        return JsonPathJSONPopulater.evaluateResponse(execute(getRequestBuilder()
                .post(CMD.EDIT_DYNAMIC_AD_GROUPS, params)), new DynamicGroupsCmdBean());
    }

    public DynamicGroupsCmdBean editDynamicAdGroups(String cid, String adGroupID, String login) {
        GroupsParameters params = new GroupsParameters();
        params.setUlogin(login);
        params.setCid(cid);
        params.setAdgroupIds(adGroupID);
        params.setBannerStatus("all");
        return editDynamicAdGroups(params);
    }
}
