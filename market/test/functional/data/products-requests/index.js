'use strict';

module.exports = {
    SOVETNIK: {
        IPHONE: {query: require('./iphone-sovetnik.json')},
        IPHONE_YABRO: {query: require('./iphone-yabro.json')},
        IPHONE_CLID_AURORA: {query: require('./iphone-sovetnik-clid-aurora.json')},
        IPHONE_WITHOUT_SETTINGS: {query: require('./iphone-sovetnik-without-settings.json')},
        OFFERS_MOUSE_PAD: {query: require('./offers-mouse-pad.json')},
        SEARCH_RESULT_MODELS: {query: require('./searchresult-tablet.json')},
        SEARCH_RESULT_OFFERS: {query: require('./searchresult-adapter.json')},
        CLOTHES_WITH_BUTTON: {query: require('./clothes-with-button.json')},
        CLOTHES_WITHOUT_BUTTON: {query: require('./clothes-without-button.json')},
        ELDORADO_REQUEST: {query: require('./eldorado-request.json')},
        IPHONE_WITH_VID: {query: require('./iphone-with-vid.json')},
        IPHONE_BTN_SS: {query: require('./iphone-with-button-ss.json')},
        BEKKER_BK_4033: {query: require('./bekker-BK-4033.json')}
    },
    SAVEFROM: {
        IPHONE: {query: require('./iphone-savefrom.json')},
        IPHONE_WITHOUT_SETTINGS: {query: require('./iphone-savefrom-without-settings.json')},
        IPHONE_OLD_FORMAT: {query: require('./iphone-savefrom-old-format.json')}
    },
    S2S: {
        ELECTROLUX_OTZOVIK: {query: require('./s2s/electrolux-otzovik.json')}
    },
    JS_INTEGRATION: {
        TRANSCEND: {query: require('./transcend-js-integration.json')},
        FEATHER_ROSES: {query: require('./feather-roses-js-integration.json')}
    },
    BUTTON_EXTENSION: {
        TRANSCEND: {query: require('./transcend-button-extension.json')}
    },
    YCLID: {
        FULL: {query: require('./yclid/request-with-yclid-and-referrer.json')},
        WITHOUT_REFERRER: {query: require('./yclid/request-with-yclid-and-without-referrer.json')},
        INVALID_REFERRER: {query: require('./yclid/request-with-yclid-and-invalid-referrer.json')},
        INVALID_YCLID: {query: require('./yclid/request-with-invalid-yclid-and-valid-referrer.json')}
    },
    YMCLID: {
        FULL: {query: require('./ymclid/request-with-ymclid-and-referrer.json')},
        SHOP_ERROR: {query: require('./ymclid/request-with-shop-error.json')},
        INVALID_REFERRER: {query: require('./ymclid/request-with-valid-ymclid-and-invalid-referrer.json')}
    }
};
