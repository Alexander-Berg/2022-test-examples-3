package ru.yandex.autotests.direct.httpclient.data;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public enum CMD {
    CLAIM_EMAIL("claimEmail"),
    CHOOSE_INTERFACE_TYPE("chooseInterfaceType"),
    CHOOSE_COUNTRY_CURRENCY("chooseCountryCurrency"),
    SWITCH_EASINESS("switchEasiness"),
    SHOW_CAMPS("showCamps"),
    EDIT_CAMP("editCamp"),
    SAVE_NEW_CAMP("saveNewCamp"),
    SHOW_CAMP("showCamp"),
    SHOW_CAMP_MULTI_EDIT("showCampMultiEdit"),
    SHOW_CAMP_MULTI_EDIT_LIGHT("showCampMultiEditLight"),
    ADD_BANNER_MULTI_EDIT("addBannerMultiEdit"),
    ADD_DYNAMIC_AD_GROUPS("addDynamicAdGroups"),
    SAVE_DYNAMIC_AD_GROUPS("saveDynamicAdGroups"),
    EDIT_DYNAMIC_AD_GROUPS("editDynamicAdGroups"),
    SEND_OPTIMIZE("sendOptimize"),
    SAVE_TEXT_ADGROUPS("saveTextAdGroups"),
    ADD_ADGROUPS_MOBILE_CONTENT("addAdGroupsMobileContent"),
    SAVE_MOBILE_ADGROUPS("saveMobileAdGroups"),
    EDIT_ADGROUPS_MOBILE_CONTENT("editAdGroupsMobileContent"),
    SAVE_BANNER_EASY("saveBannerEasy"),
    SHOW_CLIENTS("showClients"),
    ADD_AGENCY_CLIENT_RELATION("addAgencyClientRelation"),
    REMOVE_AGENCY_CLIENT_RELATION("removeAgencyClientRelation"),
    SEND_CAMPAIGN_OPTIMIZING_ORDER("sendCampaignOptimizingOrder"),
    SHOW_MANAGER_MY_CLIENTS("showManagerMyClients"),
    SHOW_USER_EMAILS("showUserEmails"),
    AG_SEARCH("agSearch"),
    CHANGE_MANAGER_OF_CLIENT("changeManagerOfClient"),
    CHANGE_MANAGER_OF_AGENCY("changeManagerOfAgency"),
    MODIFY_USER("modifyUser"),
    SHOW_STAFF("showStaff"),
    MANAGE_TEAMLEADERS("manageTeamleaders"),
    MANAGE_S_TEAMLEADERS("manageSTeamleaders"),
    API_SANDBOX_SETTINGS("apiSandboxSettings"),
    AJAX_DROP_SANDBOX_USER("ajaxDropSandboxUser"),
    AJAX_INIT_SANDBOX_USER("ajaxInitSandboxUser"),
    AJAX_SAVE_AUTOBUDGET("ajaxSaveAutobudget"), //ajax save strategy and day budget
    AJAX_SAVE_DAY_BUDGET("ajaxSaveDayBudget"),//save day budget (using at popup on showCampsPage)
    AJAX_SAVE_RETARGETING_COND("ajaxSaveRetargetingCond"),
    AJAX_DELETE_RETARGETING_COND("ajaxDeleteRetargetingCond"),
    AJAX_GET_GOALS_FOR_RETARGETING("ajaxGetGoalsForRetargeting"),
    AJAX_VALIDATE_LOGIN("ajaxValidateLogin"),
    AJAX_SUGGEST_LOGIN("ajaxSuggestLogin"),
    AJAX_VALIDATE_PASSWORD("ajaxValidatePassword"),
    AJAX_REGISTER_LOGIN("ajaxRegisterLogin"),
    ADVERTIZE("advertize"),
    SHOW_SEARCH_PAGE("showSearchPage"),
    EDIT_BANNER_EASY("editBannerEasy"),
    SHOW_REGISTER_LOGIN_PAGE("showRegisterLoginPage"),
    AJAX_STOP_RESUME_CAMP("ajaxStopResumeCamp"),
    AJAX_SAVE_CAMP_DESCRIPTION("ajaxSaveCampDescription"),
    USER_SETTINGS("userSettings"),
    SAVE_SETTINGS("saveSettings"),
    SAVE_CAMP("saveCamp"),
    STEP_ZERO("stepZero"),
    STEP_ZERO_PROCESS("stepZeroProcess"),
    SHOW_RETARGETING_COND("showRetargetingCond"),
    AJAX_CHECK_BANNERS_MINUS_WORDS("ajaxCheckBannersMinusWords"),
    SAVE_ADGROUP_TAGS("saveAdGroupTags"),
    ERROR("error"),
    ERROR_RBAC("errorRbac"),
    SAVE_CAMP_EASY("saveCampEasy"),
    NEW_CAMP_TYPE("newCampType"),
    COPY_CAMP("copyCamp"),
    MANAGE_VCARDS("manageVCards"),
    CAMP_UNARC("campUnarc"),
    SHOW_CONTACT_INFO("showContactInfo"),
    SHOW_MEDIAPLAN("showMediaplan"),
    COMPLETE_OPTIMIZING("completeOptimizing"),
    ACCEPT_OPTIMIZE("acceptOptimize"),
    ACCEPT_OPTIMIZE_STEP2("acceptOptimize_step2"),
    PROVE_NEW_AGENCY_CLIENTS("proveNewAgencyClients"),
    AJAX_UPDATE_SHOW_CONDITIONS("ajaxUpdateShowConditions"),
    AJAX_EDIT_ADGROUP_DYNAMIC_CONDITIONS("ajaxEditDynamicConditions"),
    AJAX_APPLY_REJECT_CORRECTION("ajaxApplyRejectCorrection"),
    AJAX_GET_FEEDS("ajaxGetFeeds"),
    AJAX_DELETE_FEEDS("ajaxDeleteFeeds"),
    SAVE_FEED("saveFeed"),
    SHOW_FEEDS("showFeeds"),
    DEL_CAMP("delCamp"),
    AJAX_GET_BANNERS_COUNT("ajaxGetBannersCount"),
    AJAX_GET_TRANSITIONS_BY_PHRASES("ajaxGetTransitionsByPhrases"),
    SAVE_MEDIA_BANNER("saveMediaBanner"),
    SHOW_MEDIA_CAMP("showMediaCamp"),
    PAY("pay"),
    SEARCH_BANNERS("searchBanners"),
    PAY_FOR_ALL("payforall"),
    TRANSFER("transfer"),
    TRANSFER_DONE("transfer_done"),
    SET_AUTO_PRICE_AJAX("setAutoPriceAjax"),
    SEARCH("search"),
    GET_AD_GROUP("getAdGroup"),
    AJAX_DATA_FOR_BUDGET_FORECAST("ajaxDataForBudgetForecast");
    public static final String KEY = "cmd";

    CMD(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return name;
    }

    public NameValuePair asPair() {
        return new BasicNameValuePair(KEY, getName());
    }

    public String toString() {
        return name;
    }
}
