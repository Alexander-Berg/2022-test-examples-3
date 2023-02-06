'use strict';

module.exports = {
    SEARCH: {
        MOW_VVO: {
            query: require('./avia-search-start-MOW-VVO.json')
        },
        DME_AER: {
            query: require('./avia-search-start-DME-AER.json')
        }
    },
    CHECK: {
        COUNT_0: {
            query: require('./avia-search-check-count-0.json')
        },
        WITHOU_COUNT: {
            query: require('./avia-search-check-without-count.json')
        }
    }
};

