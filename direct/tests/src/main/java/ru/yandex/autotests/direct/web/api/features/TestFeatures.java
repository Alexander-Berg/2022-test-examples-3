package ru.yandex.autotests.direct.web.api.features;

public interface TestFeatures {

    String RETARGETING = "Retargeting";
    String AUTO_OVERDRAFT = "AutoOverdraft";
    String MINUS_KEYWORD = "MinusKeyword";
    String KEYWORD = "Keyword";
    String CAMPAIGN = "Campaign";
    String DEAL = "Deal";
    String VCARD = "Vcard";
    String MOBILE_CONTENT = "MobileContent";
    String MEDIAREACH = "Mediareach";
    String FRONTPAGE = "YndxFrontpage";
    String AD_GROUPS = "AdGroups";
    String CONTENT_PROMOTION_VIDEO = "ContentPromotionVideo";

    interface Retargeting {
        String GOALS = "Goals";
        String CONDITIONS = "Conditions";
    }

    interface AutoOverdraft {
        String SET_AUTO_OVERDRAFT = "SetAutoOverdraft";
    }

    interface MinusKeyword {
        String CHECK_INCLUSION = "CheckInclusion";
        String ADD = "AddMinusKeywords";
    }

    interface Keyword {
        String STAT_SHOW = "keywordStatShows";
    }

    interface Deal {
        String ACTIVATE = "activateDeal";
        String ARCHIVE = "activateDeal";
        String COMPLETE = "completeDeal";
        String GET_DEAL_LIST = "getDealList";
        String GET_DEALS_DETAILS = "getDealsDetails";
    }

    interface Campaign {
        String CAMP_METRIKA_COUNTERS = "CampMetrikaCounters";
    }

    interface Vcard {
        String MANAGE_VCARDS = "manageVcards"; // "мастер визиток"
    }

    interface MobileContent {
        String GET_MOBILE_CONTENT = "getMobileContent";
    }

    interface Mediareach {
        String GET_CAMPAIGN_FORECAST = "getCampaignForecast";
    }
}
