module.exports = {
    PRODUCTS: {
        S2S_OTZOVIK_ELECTROLUX: require('./product/s2s-otzovik-electrolux-schema.json'),
        CATEGORIES: require('./product/categories-schema.json'),
        OFFERS_CATEGORIES: require('./product/offers-categories-schema.json'),
    },
    AVIA: {
        SEARCH: {
            ERROR: require('./avia/avia-search-error-schema.json'),
            COMMON: require('./avia/common-schema.json'),
            START: require('./avia/avia-search-start-schema.json'),
            CHECK: require('./avia/avia-search-check-schema.json'),
        },
    },
    PP: {
        COMMON: require('./pp/pp-common-schema.json'),
        CHECK: require('./pp/pp-check-schema.json'),
    },
};
