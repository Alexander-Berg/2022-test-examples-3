package ru.yandex.autotests.direct.httpclient.steps;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpPost;

import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.Strategy;
import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.direct.httpclient.data.CMD;
import ru.yandex.autotests.direct.httpclient.data.CSRFToken;
import ru.yandex.autotests.direct.httpclient.data.banners.SaveTextAdGroupsParameters;
import ru.yandex.autotests.direct.httpclient.data.campaigns.SaveCampParameters;
import ru.yandex.autotests.direct.httpclient.data.clients.SwitchEasinessParameters;
import ru.yandex.autotests.direct.httpclient.data.clients.SwitchEasinessParametersBuilder;
import ru.yandex.autotests.direct.httpclient.data.clients.chooseinterfacetype.ChooseInterfaceTypeParameters;
import ru.yandex.autotests.direct.httpclient.steps.banners.GroupsSteps;
import ru.yandex.autotests.direct.httpclient.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.httpclient.steps.campaigns.CampaignsSteps;
import ru.yandex.qatools.allure.annotations.Step;

import static org.hamcrest.Matchers.not;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class ClientSteps extends DirectBackEndSteps {

    public CommonSteps commonSteps() {
        return getInstance(CommonSteps.class, config);
    }

    public GroupsSteps groupSteps() {
        return getInstance(GroupsSteps.class, config);
    }

    public CampaignsSteps campaignsSteps() {
        return getInstance(CampaignsSteps.class, config);
    }

    @Step
    public DirectResponse selectCountryAndInterface(String country, String currency, String interfaceType) {
        DirectResponse resp = openChooseInterface();

        SwitchEasinessParameters easinessParameters =
                new SwitchEasinessParametersBuilder().createSwitchEasinessParameters();
        easinessParameters.ignoreEmptyParameters(true);
        easinessParameters.setCurrency(currency);
        easinessParameters.setInterfaceType(interfaceType);
        easinessParameters.setClientCountry(country);
//        easinessParameters.setUniq14074211562671(country);

        resp = chooseInterface(resp.getCSRFToken(), easinessParameters);

        return openRedirect(resp);
    }

    public SaveCampParameters getDefaultSaveNewCampParameters() {
        SaveCampParameters params = new SaveCampParameters();
        CampaignStrategy campaignStrategy = new CampaignStrategy();
        campaignStrategy.setSearch(new Strategy().withName("default"));
        campaignStrategy.setNet(new Strategy().withName("default"));
        campaignStrategy.setName("");
        campaignStrategy.setIsNetStop("");
        params.setJsonStartegy(campaignStrategy);
        params.setCamp_with_common_ci("0");
        params.setHolidays_radio_1("0");
        params.setTime_target_holiday_dont_show("0");
        params.setTime_target_holiday("1");
        params.setAutoOptimization("0");
        params.setEmail_notify_paused_by_day_budget("1");
        params.setOfflineStatNotice("1");
        params.setTime_target_working_holiday("1");
        params.setBroad_match_flag("1");
//        params.setBroadMatchRate("optimal");
        params.setSendWarn("1");
        params.setCampaign_select("1");
        params.setSendAccNews("1");
        params.setTime_target_holiday_coef("100");
        params.setContextPriceCoef("100");
        params.setContextLimit("100");
        params.setBroad_match_limit("100");
        params.setTimezone_id("130");
        params.setTimeTarget("1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDE" +
                "FGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQR" +
                "STUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7ABCDEFGHIJKLMNOPQRSTUVWX");
        params.setMoney_warning_value("20");
        params.setTime_target_holiday_to("20");
        params.setStart_date("2014-02-07");
        params.setSms_time_hour_to("21");

        params.setWarnPlaceInterval("60");
        params.setTime_target_holiday_from("8");
        params.setSms_time_hour_from("9");
        params.setFio("Pupkin Vasily");
        params.setExtend_switcher("simple");
        params.setTimeTargetMode("simple");
//        params.setInterface_type("std");
        params.setMediaType("text");
        params.setEmail("yandex-team7294314385@yandex.ru");
        params.setGeo_text("Единый регион не задан. У каждого объявления может быть свой регион показа.");
        params.setTimezone_text("Москва");
        params.setName("Новая кампания, созданная с помощь java http client");
//        params.setIsRelatedKeywordsEnabled("1");
        return params;
    }

    @Step("Выбираем интерфейс")
    public DirectResponse chooseInterface(CSRFToken token, SwitchEasinessParameters parameters) {
        HttpPost post = getRequestBuilder().post(CMD.SWITCH_EASINESS, token, parameters);
        post.setHeader(HttpHeaders.REFERER, "https://test-direct.yandex.ru/registered/main.pl?cmd=chooseInterfaceType");
        return execute(post);
    }

    @Step("Открываем выбор интерфейса")
    public DirectResponse openChooseInterface(ChooseInterfaceTypeParameters chooseInterfaceTypeParameters) {
        return execute(getRequestBuilder().get(CMD.CHOOSE_INTERFACE_TYPE, chooseInterfaceTypeParameters));
    }

    public DirectResponse openChooseInterface() {
        return openChooseInterface(new ChooseInterfaceTypeParameters());
    }

    @Step("Сохраняем новую кампанию")
    public DirectResponse saveNewCampaign(CSRFToken token, SaveCampParameters params) {
        return execute(getRequestBuilder().post(CMD.SAVE_NEW_CAMP, token, params));
    }

    @Step("Сохраняем ставки баннеров")
    public DirectResponse saveTextAdGroups(CSRFToken token, SaveTextAdGroupsParameters params) {
        return execute(getRequestBuilder().post(CMD.SAVE_TEXT_ADGROUPS, token, params));
    }
}
