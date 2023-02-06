package ru.yandex.autotests.direct.cmd.data.savecamp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeBy;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.impl.ValueToJsonSerializer;

// todo что делать с тем, что этот бин "используется в контроллерах saveCamp, saveNewCamp, saveCampEasy"
public class SaveCampRequest extends BasicDirectRequest {

    public static SaveCampRequest fillContactInfo(ContactInfo contactInfo) {
        Gson gson = new Gson();
        SaveCampRequest request = gson.fromJson(gson.toJson(contactInfo), SaveCampRequest.class);
        request.setCi_name(contactInfo.getCompanyName());
        return request;
    }

    @SerializeKey("cid")
    private String cid;
    @SerializeKey("interface_type")
    private String interfaceType;
    @SerializeKey("client_country")
    private String clientCountry;
    @SerializeKey("currency")
    private String currency;
    @SerializeKey("mediaType")
    private String mediaType;
    @SerializeKey("campaign_select")
    private String campaign_select;
    @SerializeKey("for_agency")
    private String for_agency;
    @SerializeKey("name")
    private String name;
    @SerializeKey("fio")
    private String fio;
    @SerializeKey("start_date")
    private String start_date;
    @SerializeKey("finish_date")
    private String finish_date;
    @SerializeKey("email")
    private String email;
    @SerializeKey("email_notify_paused_by_day_budget")
    private String email_notify_paused_by_day_budget;
    @SerializeKey("money_warning_value")
    private String money_warning_value;
    @SerializeKey("offlineStatNotice")
    private String offlineStatNotice;
    @SerializeKey("sendAccNews")
    private String sendAccNews;
    @SerializeKey("sendWarn")
    private String sendWarn;
    @SerializeKey("warnPlaceInterval")
    private String warnPlaceInterval;
    @SerializeKey("active_orders_money_out_sms")
    private String active_orders_money_out_sms;
    @SerializeKey("camp_finished_sms")
    private String camp_finished_sms;
    @SerializeKey("moderate_result_sms")
    private String moderate_result_sms;
    @SerializeKey("notify_metrica_control_sms")
    private String notify_metrica_control_sms;
    @SerializeKey("notify_order_money_in_sms")
    private String notify_order_money_in_sms;
    @SerializeKey("sms_time_hour_from")
    private String sms_time_hour_from;
    @SerializeKey("sms_time_hour_to")
    private String sms_time_hour_to;
    @SerializeKey("sms_time_min_from")
    private String sms_time_min_from;
    @SerializeKey("sms_time_min_to")
    private String sms_time_min_to;

    @SerializeKey("strategy")
    private String strategy;

    @SerializeKey("json_strategy")
    @SerializeBy(ValueToJsonSerializer.class)
    private CampaignStrategy jsonStrategy;


    @SerializeKey("mobile_app_id")
    private Long mobileAppId;

    @SerializeKey("mobile_app_goal")
    private String mobileAppGoal;

    @SerializeKey("extend_switcher")
    private String extend_switcher;
    @SerializeKey("holidays_radio_1")
    private String holidays_radio_1;
    @SerializeKey("timeTarget")
    private String timeTarget;
    @SerializeKey("timeTargetMode")
    private String timeTargetMode;
    @SerializeKey("time_target_holiday")
    private String time_target_holiday;
    @SerializeKey("time_target_holiday_coef")
    private String time_target_holiday_coef;
    @SerializeKey("time_target_holiday_dont_show")
    private String time_target_holiday_dont_show;
    @SerializeKey("time_target_holiday_from")
    private String time_target_holiday_from;
    @SerializeKey("time_target_holiday_to")
    private String time_target_holiday_to;
    @SerializeKey("time_target_preset")
    private String time_target_preset;
    @SerializeKey("time_target_working_holiday")
    private String time_target_working_holiday;
    @SerializeKey("timezone_id")
    private String timezone_id;
    @SerializeKey("timezone_text")
    private String timezone_text;
    @SerializeKey("geo")
    private String geo;
    @SerializeKey("geo_text")
    private String geo_text;
    @SerializeKey("camp_with_common_ci")
    private String camp_with_common_ci;
    @SerializeKey("country")
    private String country;
    @SerializeKey("city")
    private String city;
    @SerializeKey("geo_id")
    private String geo_id;
    @SerializeKey("country_code")
    private String country_code;
    @SerializeKey("city_code")
    private String city_code;
    @SerializeKey("phone")
    private String phone;
    @SerializeKey("ext")
    private String ext;
    @SerializeKey("ci_name")
    private String ci_name;
    @SerializeKey("contactperson")
    private String contactperson;
    @SerializeKey("worktime")
    private String worktime;
    @SerializeKey("street")
    private String street;
    @SerializeKey("geo_flag")
    private String geo_flag;
    @SerializeKey("house")
    private String house;
    @SerializeKey("build")
    private String build;
    @SerializeKey("apart")
    private String apart;
    @SerializeKey("metro")
    private String metro;
    @SerializeKey("auto_bounds")
    private String auto_bounds;
    @SerializeKey("auto_point")
    private String auto_point;
    @SerializeKey("auto_precision")
    private String auto_precision;
    @SerializeKey("manual_bounds")
    private String manual_bounds;
    @SerializeKey("manual_point")
    private String manual_point;
    @SerializeKey("contact_email")
    private String contact_email;
    @SerializeKey("im_client")
    private String im_client;
    @SerializeKey("im_login")
    private String im_login;
    @SerializeKey("extra_message")
    private String extra_message;
    @SerializeKey("_ogrn_ignore")
    private String _ogrn_ignore;
    @SerializeKey("ogrn")
    private String ogrn;
    @SerializeKey("json_campaign_minus_words")
    @SerializeBy(ValueToJsonSerializer.class)
    private List<String> jsonCampaignMinusWords;
    @SerializeKey("ContextLimit")
    private String contextLimit;
    @SerializeKey("ContextPriceCoef")
    private String contextPriceCoef;
    @SerializeKey("disableBehavior")
    private String disableBehavior;
    @SerializeKey("broad_match_flag")
    private String broad_match_flag;
    @SerializeKey("broad_match_limit")
    private String broad_match_limit;
    @SerializeKey("broad_match_rate")
    private String broadMatchRate;
    @SerializeKey("device_targeting")
    private String device_targeting;
    @SerializeKey("new_pages")
    private String new_pages;
    @SerializeKey("DontShow")
    private String dontShow;
    @SerializeKey("banners_per_page")
    private String banners_per_page;
    @SerializeKey("competitors_domains")
    private String competitors_domains;
    @SerializeKey("new_IP")
    private String new_IP;
    @SerializeKey("disabledIps")
    private String disabledIps;
    @SerializeKey("autoOptimization")
    private String autoOptimization;
    @SerializeKey("metrika_counters")
    private String metrika_counters;
    @SerializeKey("status_click_track")
    private String status_click_track;
    @SerializeKey("use_favorite_camp")
    private String use_favorite_camp;
    @SerializeKey("enable_cpc_hold")
    private String enableCpcPriceHold;
    @SerializeKey("content_lang")
    private String contentLang;

    @SerializedName("place_id")
    @SerializeKey("place_id")
    private Integer placeId;

    @SerializedName("is_mobile")
    @SerializeKey("is_mobile")
    private Integer isMobile;

    @SerializedName("page_ids")
    @SerializeKey("page_ids")
    private String pageIds;

    @SerializedName("restriction_type")
    @SerializeKey("restriction_type")
    private String restrictionType;

    @SerializedName("restriction_value")
    @SerializeKey("restriction_value")
    private Integer restrictionValue;

    @SerializedName("rotation_goal_id")
    @SerializeKey("rotation_goal_id")
    private Long rotationGoalId;


    @SerializeKey("json_hierarchical_multipliers")
    @SerializeBy(ValueToJsonSerializer.class)
    private HierarchicalMultipliers hierarchicalMultipliers = new HierarchicalMultipliers();

    @SerializeKey("json_geo_multipliers")
    @SerializeBy(ValueToJsonSerializer.class)
    private Map<String, String> geoMultipliers;

    @SerializeKey("geo_multipliers_enabled")
    private Integer geoMultipliersEnabled;

    @SerializeKey("json_geo_changes")
    @SerializeBy(ValueToJsonSerializer.class)
    private Map<String, Object> geoChanges;

    @SerializeKey("fairAuction")
    private String fairAuction;
    @SerializeKey("show")
    private String show;
    //авторасширение фраз
    @SerializeKey("is_related_keywords_enabled")
    private String isRelatedKeywordsEnabled;
    @SerializeKey("enabled_sms_easy")
    private String enabledSmsEasy;

    @SerializeKey("device_type_targeting")
    private String deviceTypeTargeting;

    @SerializeKey("network_targeting")
    private String networkTargeting;

    //собираем значения полей sms_time в одно
    private String smsTime;

    @SerializeKey("broad_match_goal_id")
    private String broadMatchGoalId;

    @SerializeKey("extended_geotargeting")
    private Integer extendedGeotargeting;

    @SerializeKey("has_probabilistic_auction")
    private Integer hasProbalisticAuction;

    @SerializeKey("attribution_model")
    private String attributionModel;

    @SerializeKey("json_ab_sections_statistic")
    @SerializeBy(ValueToJsonSerializer.class)
    private List<Long> abSectionsStat;

    @SerializeKey("json_ab_segments_retargeting")
    @SerializeBy(ValueToJsonSerializer.class)
    private List<Long> abSegmentsRetargeting;

    public SaveCampRequest withAttributionModel(String attributionModel) {
        this.attributionModel = attributionModel;
        return this;
    }

    public Integer getHasProbalisticAuction() {
        return hasProbalisticAuction;
    }

    public Integer getGeoMultipliersEnabled() {
        return geoMultipliersEnabled;
    }

    public SaveCampRequest withGeoMultipliersEnabled(Integer geoMultipliersEnabled) {
        this.geoMultipliersEnabled = geoMultipliersEnabled;
        return this;
    }

    public SaveCampRequest withHasProbalisticAuction(Integer hasProbalisticAuction) {
        this.hasProbalisticAuction = hasProbalisticAuction;
        return this;
    }

    public String getBroadMatchGoalId() {
        return broadMatchGoalId;
    }

    public void setBroadMatchGoalId(String broadMatchGoalId) {
        this.broadMatchGoalId = broadMatchGoalId;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public String getClientCountry() {
        return clientCountry;
    }

    public void setClientCountry(String clientCountry) {
        this.clientCountry = clientCountry;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getCampaign_select() {
        return campaign_select;
    }

    public void setCampaign_select(String campaign_select) {
        this.campaign_select = campaign_select;
    }

    public String getFor_agency() {
        return for_agency;
    }

    public void setFor_agency(String for_agency) {
        this.for_agency = for_agency;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFio() {
        return fio;
    }

    public void setFio(String fio) {
        this.fio = fio;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public Long getMobileAppId() {
        return mobileAppId;
    }

    public void setMobileAppId(Long mobileAppId) {
        this.mobileAppId = mobileAppId;
    }

    public String getMobileAppGoal() {
        return mobileAppGoal;
    }

    public void setMobileAppGoal(String mobileAppGoal) {
        this.mobileAppGoal = mobileAppGoal;
    }

    public String getStart_date() {
        return start_date;
    }

    public void setStart_date(String start_date) {
        this.start_date = start_date;
    }

    public String getFinish_date() {
        return finish_date;
    }

    public void setFinish_date(String finish_date) {
        this.finish_date = finish_date;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail_notify_paused_by_day_budget() {
        return email_notify_paused_by_day_budget;
    }

    public void setEmail_notify_paused_by_day_budget(String email_notify_paused_by_day_budget) {
        this.email_notify_paused_by_day_budget = email_notify_paused_by_day_budget;
    }

    public String getMoney_warning_value() {
        return money_warning_value;
    }

    public void setMoney_warning_value(String money_warning_value) {
        this.money_warning_value = money_warning_value;
    }

    public String getOfflineStatNotice() {
        return offlineStatNotice;
    }

    public void setOfflineStatNotice(String offlineStatNotice) {
        this.offlineStatNotice = offlineStatNotice;
    }

    public String getSendAccNews() {
        return sendAccNews;
    }

    public void setSendAccNews(String sendAccNews) {
        this.sendAccNews = sendAccNews;
    }

    public String getSendWarn() {
        return sendWarn;
    }

    public void setSendWarn(String sendWarn) {
        this.sendWarn = sendWarn;
    }

    public String getWarnPlaceInterval() {
        return warnPlaceInterval;
    }

    public void setWarnPlaceInterval(String warnPlaceInterval) {
        this.warnPlaceInterval = warnPlaceInterval;
    }

    public String getActive_orders_money_out_sms() {
        return active_orders_money_out_sms;
    }

    public void setActive_orders_money_out_sms(String active_orders_money_out_sms) {
        this.active_orders_money_out_sms = active_orders_money_out_sms;
    }

    public String getCamp_finished_sms() {
        return camp_finished_sms;
    }

    public void setCamp_finished_sms(String camp_finished_sms) {
        this.camp_finished_sms = camp_finished_sms;
    }

    public String getModerate_result_sms() {
        return moderate_result_sms;
    }

    public void setModerate_result_sms(String moderate_result_sms) {
        this.moderate_result_sms = moderate_result_sms;
    }

    public String getNotify_metrica_control_sms() {
        return notify_metrica_control_sms;
    }

    public void setNotify_metrica_control_sms(String notify_metrica_control_sms) {
        this.notify_metrica_control_sms = notify_metrica_control_sms;
    }

    public String getNotify_order_money_in_sms() {
        return notify_order_money_in_sms;
    }

    public void setNotify_order_money_in_sms(String notify_order_money_in_sms) {
        this.notify_order_money_in_sms = notify_order_money_in_sms;
    }

    public String getSms_time_hour_from() {
        return sms_time_hour_from;
    }

    public void setSms_time_hour_from(String sms_time_hour_from) {
        this.sms_time_hour_from = sms_time_hour_from;
    }

    public String getSms_time_hour_to() {
        return sms_time_hour_to;
    }

    public void setSms_time_hour_to(String sms_time_hour_to) {
        this.sms_time_hour_to = sms_time_hour_to;
    }

    public String getSms_time_min_from() {
        return sms_time_min_from;
    }

    public void setSms_time_min_from(String sms_time_min_from) {
        this.sms_time_min_from = sms_time_min_from;
    }

    public String getSms_time_min_to() {
        return sms_time_min_to;
    }

    public void setSms_time_min_to(String sms_time_min_to) {
        this.sms_time_min_to = sms_time_min_to;
    }

    public String getExtend_switcher() {
        return extend_switcher;
    }

    public void setExtend_switcher(String extend_switcher) {
        this.extend_switcher = extend_switcher;
    }

    public String getHolidays_radio_1() {
        return holidays_radio_1;
    }

    public void setHolidays_radio_1(String holidays_radio_1) {
        this.holidays_radio_1 = holidays_radio_1;
    }

    public String getTimeTarget() {
        return timeTarget;
    }

    public void setTimeTarget(String timeTarget) {
        this.timeTarget = timeTarget;
    }

    public String getTimeTargetMode() {
        return timeTargetMode;
    }

    public void setTimeTargetMode(String timeTargetMode) {
        this.timeTargetMode = timeTargetMode;
    }

    public String getTime_target_holiday() {
        return time_target_holiday;
    }

    public void setTime_target_holiday(String time_target_holiday) {
        this.time_target_holiday = time_target_holiday;
    }

    public String getTime_target_holiday_coef() {
        return time_target_holiday_coef;
    }

    public void setTime_target_holiday_coef(String time_target_holiday_coef) {
        this.time_target_holiday_coef = time_target_holiday_coef;
    }

    public String getTime_target_holiday_dont_show() {
        return time_target_holiday_dont_show;
    }

    public void setTime_target_holiday_dont_show(String time_target_holiday_dont_show) {
        this.time_target_holiday_dont_show = time_target_holiday_dont_show;
    }

    public String getTime_target_holiday_from() {
        return time_target_holiday_from;
    }

    public void setTime_target_holiday_from(String time_target_holiday_from) {
        this.time_target_holiday_from = time_target_holiday_from;
    }

    public String getTime_target_holiday_to() {
        return time_target_holiday_to;
    }

    public void setTime_target_holiday_to(String time_target_holiday_to) {
        this.time_target_holiday_to = time_target_holiday_to;
    }

    public String getTime_target_preset() {
        return time_target_preset;
    }

    public void setTime_target_preset(String time_target_preset) {
        this.time_target_preset = time_target_preset;
    }

    public String getTime_target_working_holiday() {
        return time_target_working_holiday;
    }

    public void setTime_target_working_holiday(String time_target_working_holiday) {
        this.time_target_working_holiday = time_target_working_holiday;
    }

    public String getTimezone_id() {
        return timezone_id;
    }

    public void setTimezone_id(String timezone_id) {
        this.timezone_id = timezone_id;
    }

    public String getTimezone_text() {
        return timezone_text;
    }

    public void setTimezone_text(String timezone_text) {
        this.timezone_text = timezone_text;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public String getGeo_text() {
        return geo_text;
    }

    public void setGeo_text(String geo_text) {
        this.geo_text = geo_text;
    }

    public String getCamp_with_common_ci() {
        return camp_with_common_ci;
    }

    public void setCamp_with_common_ci(String camp_with_common_ci) {
        this.camp_with_common_ci = camp_with_common_ci;
    }

    public SaveCampRequest withCamp_with_common_ci(String camp_with_common_ci) {
        this.camp_with_common_ci = camp_with_common_ci;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getGeo_id() {
        return geo_id;
    }

    public void setGeo_id(String geo_id) {
        this.geo_id = geo_id;
    }

    public String getCountry_code() {
        return country_code;
    }

    public void setCountry_code(String country_code) {
        this.country_code = country_code;
    }

    public String getCity_code() {
        return city_code;
    }

    public void setCity_code(String city_code) {
        this.city_code = city_code;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getCi_name() {
        return ci_name;
    }

    public void setCi_name(String ci_name) {
        this.ci_name = ci_name;
    }

    public String getContactperson() {
        return contactperson;
    }

    public void setContactperson(String contactperson) {
        this.contactperson = contactperson;
    }

    public String getWorktime() {
        return worktime;
    }

    public void setWorktime(String worktime) {
        this.worktime = worktime;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getGeo_flag() {
        return geo_flag;
    }

    public void setGeo_flag(String geo_flag) {
        this.geo_flag = geo_flag;
    }

    public String getHouse() {
        return house;
    }

    public void setHouse(String house) {
        this.house = house;
    }

    public String getBuild() {
        return build;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public String getApart() {
        return apart;
    }

    public void setApart(String apart) {
        this.apart = apart;
    }

    public String getMetro() {
        return metro;
    }

    public void setMetro(String metro) {
        this.metro = metro;
    }

    public String getAuto_bounds() {
        return auto_bounds;
    }

    public void setAuto_bounds(String auto_bounds) {
        this.auto_bounds = auto_bounds;
    }

    public String getAuto_point() {
        return auto_point;
    }

    public void setAuto_point(String auto_point) {
        this.auto_point = auto_point;
    }

    public String getAuto_precision() {
        return auto_precision;
    }

    public void setAuto_precision(String auto_precision) {
        this.auto_precision = auto_precision;
    }

    public String getManual_bounds() {
        return manual_bounds;
    }

    public void setManual_bounds(String manual_bounds) {
        this.manual_bounds = manual_bounds;
    }

    public String getManual_point() {
        return manual_point;
    }

    public void setManual_point(String manual_point) {
        this.manual_point = manual_point;
    }

    public String getContact_email() {
        return contact_email;
    }

    public void setContact_email(String contact_email) {
        this.contact_email = contact_email;
    }

    public String getIm_client() {
        return im_client;
    }

    public void setIm_client(String im_client) {
        this.im_client = im_client;
    }

    public String getIm_login() {
        return im_login;
    }

    public void setIm_login(String im_login) {
        this.im_login = im_login;
    }

    public String getExtra_message() {
        return extra_message;
    }

    public void setExtra_message(String extra_message) {
        this.extra_message = extra_message;
    }

    public String get_ogrn_ignore() {
        return _ogrn_ignore;
    }

    public void set_ogrn_ignore(String _ogrn_ignore) {
        this._ogrn_ignore = _ogrn_ignore;
    }

    public String getOgrn() {
        return ogrn;
    }

    public void setOgrn(String ogrn) {
        this.ogrn = ogrn;
    }

    public List<String> getJsonCampaignMinusWords() {
        return jsonCampaignMinusWords;
    }

    public void setJsonCampaignMinusWords(List<String> jsonCampaignMinusWords) {
        this.jsonCampaignMinusWords = jsonCampaignMinusWords;
    }

    public String getContextLimit() {
        return contextLimit;
    }

    public void setContextLimit(String contextLimit) {
        this.contextLimit = contextLimit;
    }

    public String getContextPriceCoef() {
        return contextPriceCoef;
    }

    public void setContextPriceCoef(String contextPriceCoef) {
        this.contextPriceCoef = contextPriceCoef;
    }

    public SaveCampRequest withContextPriceCoef(String contextPriceCoef) {
        this.contextPriceCoef = contextPriceCoef;
        return this;
    }

    public String getDisableBehavior() {
        return disableBehavior;
    }

    public void setDisableBehavior(String disableBehavior) {
        this.disableBehavior = disableBehavior;
    }

    public String getBroad_match_flag() {
        return broad_match_flag;
    }

    public void setBroad_match_flag(String broad_match_flag) {
        this.broad_match_flag = broad_match_flag;
    }

    public String getBroad_match_limit() {
        return broad_match_limit;
    }

    public void setBroad_match_limit(String broad_match_limit) {
        this.broad_match_limit = broad_match_limit;
    }

    public String getBroadMatchRate() {
        return broadMatchRate;
    }

    public void setBroadMatchRate(String broadMatchRate) {
        this.broadMatchRate = broadMatchRate;
    }

    public String getDevice_targeting() {
        return device_targeting;
    }

    public void setDevice_targeting(String device_targeting) {
        this.device_targeting = device_targeting;
    }

    public String getNew_pages() {
        return new_pages;
    }

    public void setNew_pages(String new_pages) {
        this.new_pages = new_pages;
    }

    public String getDontShow() {
        return dontShow;
    }

    public void setDontShow(String dontShow) {
        this.dontShow = dontShow;
    }

    public String getBanners_per_page() {
        return banners_per_page;
    }

    public void setBanners_per_page(String banners_per_page) {
        this.banners_per_page = banners_per_page;
    }

    public String getCompetitors_domains() {
        return competitors_domains;
    }

    public void setCompetitors_domains(String competitors_domains) {
        this.competitors_domains = competitors_domains;
    }

    public String getNew_IP() {
        return new_IP;
    }

    public void setNew_IP(String new_IP) {
        this.new_IP = new_IP;
    }

    public String getDisabledIps() {
        return disabledIps;
    }

    public void setDisabledIps(String disabledIps) {
        this.disabledIps = disabledIps;
    }

    public String getAutoOptimization() {
        return autoOptimization;
    }

    public void setAutoOptimization(String autoOptimization) {
        this.autoOptimization = autoOptimization;
    }

    public SaveCampRequest withAutoOptimization(String autoOptimization) {
        this.autoOptimization = autoOptimization;
        return this;
    }

    public String getMetrika_counters() {
        return metrika_counters;
    }

    public void setMetrika_counters(String metrika_counters) {
        this.metrika_counters = metrika_counters;
    }

    public SaveCampRequest withMetrika_counters(String metrika_counters) {
        this.metrika_counters = metrika_counters;
        return this;
    }

    public String getStatus_click_track() {
        return status_click_track;
    }

    public void setStatus_click_track(String status_click_track) {
        this.status_click_track = status_click_track;
    }

    public String getUse_favorite_camp() {
        return use_favorite_camp;
    }

    public void setUse_favorite_camp(String use_favorite_camp) {
        this.use_favorite_camp = use_favorite_camp;
    }

    public CampaignStrategy getJsonStrategy() {
        return jsonStrategy;
    }

    public SaveCampRequest withJsonStrategy(CampaignStrategy strategy) {
        this.jsonStrategy = strategy;
        return this;
    }

    public void setJsonStrategy(CampaignStrategy jsonStrategy) {
        this.jsonStrategy = jsonStrategy;
    }

    public HierarchicalMultipliers getHierarchicalMultipliers() {
        return hierarchicalMultipliers;
    }

    public void setHierarchicalMultipliers(HierarchicalMultipliers hierarchicalMultipliers) {
        this.hierarchicalMultipliers = hierarchicalMultipliers;
    }

    public SaveCampRequest withHierarchicalMultipliers(HierarchicalMultipliers hierarchicalMultipliers) {
        this.hierarchicalMultipliers = hierarchicalMultipliers;
        return this;
    }

    public String getFairAuction() {
        return fairAuction;
    }

    public void setFairAuction(String fairAuction) {
        this.fairAuction = fairAuction;
    }

    public String getShow() {
        return show;
    }

    public void setShow(String show) {
        this.show = show;
    }

    public String getIsRelatedKeywordsEnabled() {
        return isRelatedKeywordsEnabled;
    }

    public void setIsRelatedKeywordsEnabled(String isRelatedKeywordsEnabled) {
        this.isRelatedKeywordsEnabled = isRelatedKeywordsEnabled;
    }

    public SaveCampRequest withIsRelatedKeywordsEnabled(String isRelatedKeywordsEnabled) {
        this.isRelatedKeywordsEnabled = isRelatedKeywordsEnabled;
        return this;
    }

    public String getEnabledSmsEasy() {
        return enabledSmsEasy;
    }

    public void setEnabledSmsEasy(String enabledSmsEasy) {
        this.enabledSmsEasy = enabledSmsEasy;
    }

    public String getEnableCpcPriceHold() {
        return enableCpcPriceHold;
    }

    public void setEnableCpcPriceHold(String enableCpcPriceHold) {
        this.enableCpcPriceHold = enableCpcPriceHold;
    }

    public String getSmsTime() {

        if (sms_time_hour_from != null && sms_time_min_from == null) {
            sms_time_min_from = "00";
        }
        if (sms_time_hour_to != null && sms_time_min_to == null) {
            sms_time_min_to = "00";
        }
        if (sms_time_hour_from != null && sms_time_hour_from.length() == 1) {
            sms_time_hour_from = "0" + sms_time_hour_from;
        }
        if (sms_time_hour_to != null && sms_time_hour_to.length() == 1) {
            sms_time_hour_to = "0" + sms_time_hour_to;
        }
        if (sms_time_hour_to != null && sms_time_hour_from != null) {
            smsTime = sms_time_hour_from + ":" + sms_time_min_from + ":" + sms_time_hour_to + ":" + sms_time_min_to;
        }
        return smsTime;
    }

    public String getDeviceTypeTargeting() {
        return deviceTypeTargeting;
    }

    public void setDeviceTypeTargeting(String deviceTypeTargeting) {
        this.deviceTypeTargeting = deviceTypeTargeting;
    }

    public String getNetworkTargeting() {
        return networkTargeting;
    }

    public void setNetworkTargeting(String networkTargeting) {
        this.networkTargeting = networkTargeting;
    }

    public String getContentLang() {
        return contentLang;
    }

    public void setContentLang(String contentLang) {
        this.contentLang = contentLang;
    }

    public SaveCampRequest withContentLang(String contentLang) {
        this.contentLang = contentLang;
        return this;
    }

    public Integer getExtendedGeotargeting() {
        return extendedGeotargeting;
    }

    public void setExtendedGeotargeting(Integer extendedGeotargeting) {
        this.extendedGeotargeting = extendedGeotargeting;
    }

    public SaveCampRequest withMediaType(String mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public SaveCampRequest withExtendedGeotargeting(Integer extendedGeotargeting) {
        this.extendedGeotargeting = extendedGeotargeting;
        return this;
    }

    public SaveCampRequest withCid(String cid) {
        this.cid = cid;
        return this;
    }

    public SaveCampRequest withHierarhicalMultipliers(HierarchicalMultipliers hierarhicalMultipliers) {
        this.hierarchicalMultipliers = hierarhicalMultipliers;
        return this;
    }

    public SaveCampRequest withGeo(String geo) {
        this.geo = geo;
        return this;
    }

    public SaveCampRequest withFor_agency(String for_agency) {
        this.for_agency = for_agency;
        return this;
    }

    public SaveCampRequest withDontShow(String dontShow) {
        this.dontShow = dontShow;
        return this;
    }

    public SaveCampRequest withJsonCampaignMinusWords(List<String> jsonCampaignMinusWords) {
        this.jsonCampaignMinusWords = jsonCampaignMinusWords;
        return this;
    }

    public SaveCampRequest withGeoMultipliers(Map<String, String> geoMultipliers) {
        this.geoMultipliers = geoMultipliers;
        return this;
    }

    public SaveCampRequest withGeoChanges(Map<String, ? extends Object> geoChanges) {
        this.geoChanges = new HashMap<>(geoChanges);
        return this;
    }


    public Map<String, String> getGeoMultipliers() {
        return geoMultipliers;
    }

    public void setGeoMultipliers(Map<String, String> geoMultipliers) {
        this.geoMultipliers = geoMultipliers;
    }

    public Map<String, Object> getGeoChanges() {
        return geoChanges;
    }

    public void setGeoChanges(Map<String, Object> geoChanges) {
        this.geoChanges = geoChanges;
    }

    public List<Long> getAbSectionsStat() {
        return abSectionsStat;
    }

    public void setAbSectionsStat(List<Long> abSectionsStat) {
        this.abSectionsStat = abSectionsStat;
    }

    public SaveCampRequest withAbSectionsStat(List<Long> abSectionsStat) {
        this.abSectionsStat = abSectionsStat;
        return this;
    }

    public List<Long> getAbSegmentsRetargeting() {
        return abSegmentsRetargeting;
    }

    public void setAbSegmentsRetargeting(List<Long> abSegmentsRetargeting) {
        this.abSegmentsRetargeting = abSegmentsRetargeting;
    }

    public SaveCampRequest withAbSegmentsRetargeting(List<Long> abSegmentsRetargeting) {
        this.abSegmentsRetargeting = abSegmentsRetargeting;
        return this;
    }

    public SaveCampRequest withMobileAppId(Long mobileAppId) {
        this.mobileAppId = mobileAppId;
        return this;
    }

    public Integer getPlaceId() {
        return placeId;
    }

    public void setPlaceId(Integer placeId) {
        this.placeId = placeId;
    }

    public SaveCampRequest withPlaceId(Integer placeId) {
        this.placeId = placeId;
        return this;
    }

    public Integer getIsMobile() {
        return isMobile;
    }

    public void setIsMobile(Integer isMobile) {
        this.isMobile = isMobile;
    }

    public SaveCampRequest withIsMobile(Integer isMobile) {
        this.isMobile = isMobile;
        return this;
    }

    public String getPageIds() {
        return pageIds;
    }

    public void setPageIds(String pageIds) {
        this.pageIds = pageIds;
    }

    public SaveCampRequest withPageIds(String pageIds) {
        this.pageIds = pageIds;
        return this;
    }

    public String getRestrictionType() {
        return restrictionType;
    }

    public void setRestrictionType(String restrictionType) {
        this.restrictionType = restrictionType;
    }

    public SaveCampRequest withRestrictionType(String restrictionType) {
        this.restrictionType = restrictionType;
        return this;
    }

    public Integer getRestrictionValue() {
        return restrictionValue;
    }

    public void setRestrictionValue(Integer restrictionValue) {
        this.restrictionValue = restrictionValue;
    }

    public SaveCampRequest withRestrictionValue(Integer restrictionValue) {
        this.restrictionValue = restrictionValue;
        return this;
    }

    public void setRotationGoalId(Long rotationGoalId) {
        this.rotationGoalId = rotationGoalId;
    }

    public SaveCampRequest withRotationGoalId(Long rotationGoalId) {
        this.rotationGoalId = rotationGoalId;
        return this;
    }
}
