const COMMON_PART_OF_MOCK = {
    hostname: 'https://api.content.market.yandex.ru',
    path: /\/v1\/search\.json/,
    query: {
        text: /.*/
    }
};

const EMPTY = Object.assign({
    result: require('./../../external-api-responses/market').SEARCH.EMPTY_BY_URL
}, COMMON_PART_OF_MOCK);

const MOUSE_PAD = Object.assign({
    result: require('./../../external-api-responses/market').SEARCH.MOUSE_PAD_BY_TEXT
}, COMMON_PART_OF_MOCK);

const TABLET = Object.assign({
    result: require('./../../external-api-responses/market').SEARCH.TABLET_BY_TEXT
}, COMMON_PART_OF_MOCK);

const ADAPTER = Object.assign({
    result: require('./../../external-api-responses/market').SEARCH.ADAPTER_BY_TEXT
}, COMMON_PART_OF_MOCK);

const BEKKER_BK_4033 = Object.assign({
    result: require('./../../external-api-responses/market').SEARCH.BEKKER_BK_4033_BY_TEXT
}, COMMON_PART_OF_MOCK);

const BEKKER_BK_4033_WITH_INVALID_CATEGORY_ID = Object.assign({
    result: require('./../../external-api-responses/market').SEARCH.BEKKER_BK_4033_BY_TEXT_WITH_INVALID_CATEGORY_ID
}, COMMON_PART_OF_MOCK);

const TRANSCEND = Object.assign({
    result: require('./../../external-api-responses/market').SEARCH.TRANSCEND
}, COMMON_PART_OF_MOCK);

module.exports = {
    EMPTY,
    MOUSE_PAD,
    TABLET,
    ADAPTER,
    BEKKER_BK_4033,
    BEKKER_BK_4033_WITH_INVALID_CATEGORY_ID,
    TRANSCEND
};
