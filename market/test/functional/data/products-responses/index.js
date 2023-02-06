'use strict';

/**
 *
 * @type {{IPHONE: {MODEL, SETTINGS, SEARCH_INFO, SHOP_INFO}, BODY: {FIRST_UPDATED_REQUEST, SECOND_SCRIPT}}}
 */
module.exports = {
    IPHONE: {
        MODEL: require('./iphone/iphone-model-schema-v1.json'),
        SETTINGS: require('./iphone/iphone-settings-schema-v1.json'),
        SEARCH_INFO: require('./iphone/iphone-search-info-schema-v1.json'),
        SHOP_INFO: require('./iphone/iphone-shop-info-schema-v2.json')
    },
    COMMON: {
        SECOND_SCRIPT: require('./common/second-script-schema.json'),
        OPT_IN_INTERVAL: require('./common/opt-in-interval-schema.json'),
        OFFERS_MOUSE_PAD: require('./common/offers-mouse-pad-schema-v1.json'),
        OFFERS_MOUSE_PAD_RAZER: require('./common/offers-mouse-pad-razer-schema.json'),
        SEARCH_RESULT: require('./common/search-result-schema.json'),
        CLOTHES_SHOWN: require('./common/clothes-shown-schema.json'),
        CLOTHES_SHOWN_SPECIFIED: require('./common/specified-clothes-shown-schema.json')
    },
    SETTINGS: {
        OPT_IN: require('./settings/opt-in-schema.json'),
        OPT_OUT: require('./settings/opt-out-schema.json'),
        LAST_OPT_IN_SHOW_TIME: require('./settings/last-opt-in-show-time.json')
    }
};

