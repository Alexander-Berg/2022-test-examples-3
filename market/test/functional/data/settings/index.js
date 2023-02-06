'use strict';

module.exports = {
    PARTNERS: {
        SOVETNIK: require('./partners/sovetnik.json'),
        SAVE_FROM: require('./partners/save-from.json'),
        MOBILE: require('./partners/mobile.json'),
        SOVETNIK_WITH_VID: require('./partners/sovetnik-with-vid.json'),

        /**
         * Настройки для партнера, который интегрирует Советника с помощью прямой JS-интеграции
         * https://st.yandex-team.ru/SOVETNIK-10322
         */
        JS_INTEGRATION: require('./partners/js-intergration.json'),
        SOVETNIK_YABRO: require('./partners/yabro.json')
    },
    USERS: {
        SOVETNIK: require('./users/sovetnik.json'),
        SAVE_FROM: require('./users/save-from.json'),
        DOMAIN_DISABLED: require('./users/eldorado-domain-disabled.json'),
        SOVETNIK_WITH_VID: require('./users/sovetnik-with-vid.json'),
        SOVETNIK_YABRO: require('./users/yabro.json'),
        WITH_PRICEBAR_CLOSINGS_COUNT: require('./users/with-pricebar-closings-count.json'),
        ONE_TIME_CLOSED_PRICEBAR: require('./users/one-time-closed-pricebar.json')
    }
};
