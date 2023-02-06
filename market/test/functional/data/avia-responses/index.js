'use strict';

module.exports = {
    SEARCH: {
        ERROR_SCHEMA: require('./avia-search-error-schema.json'),
        START: {
            EMPTY_SCHEMA: require('./avia-search-start-empty-schema.json'),
            FULL_SCHEMA: require('./avia-search-start-schema.json'),
            WITHOUT_RETURN_DATE_SCHEMA: require('./avia-search-start-withot-return-date-schema.json')
        },
        CHECK: {
            EMPTY_SCHEMA: require('./avia-search-check-empty-schema.json'),
            FULL_SCHEMA: require('./avia-search-check-schema.json'),
            NOTIFICATION_DME_BCN_SCHEMA: require('./avia-search-check-notification-DME-BCN-schema.json')
        },
        CHECK_V2: {
            EMPTY_SCHEMA: require('./avia-search-check-v2.0-empty-schema.json'),
            FULL_SCHEMA: require('./avia-search-check-v2.0-schema.json')
        }
    },
    API_AVIA: {
        SEARCH: {
            MOW_LED: require('./api-avia-search-MOW-LED.json')
        },
        RESULTS: {
            EMPTY: require('./api-avia-results-empty.json'),
            MOW_LED: require('./api-avia-results-MOW-LED.json'),
            DME_BCN: require('./api-avia-results-DME-BCN.json'),
            DME_NUE: require('./api-avia-results-DME-NUE.json'),

            504: require('./api-avia-results-504.json'),

            WITHOUT_VARIANTS: require('./api-avia-results-without-variants.json'),
            FLIGHTS_KEY_ARE_NOT_CONSISTENCY: require('./api-avia-results-flights-key-are-not-consistency.json'),
            PARTNERS_KEY_ARE_NOT_CONSISTENCY: require('./api-avia-results-partners-not-consistency.json'),

            CURRENCIES: {
                BYR: require('./api-avia-results-BYR-currency.json')
            }
        }
    }
};