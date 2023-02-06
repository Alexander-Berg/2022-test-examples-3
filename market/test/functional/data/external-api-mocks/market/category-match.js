const COMMON_PART_OF_MOCK = {
    hostname: 'https://api.content.market.yandex.ru',
    path: '/v1/category/match.json'
};

const SSD_CATEGORIES = Object.assign({
    result: require('./../../external-api-responses/market').CATEGORY_MATCH.SSD_CATEGORIES
}, COMMON_PART_OF_MOCK);

const EMPTY = Object.assign({
    result: require('./../../external-api-responses/market').CATEGORY_MATCH.EMPTY
}, COMMON_PART_OF_MOCK);

const SHOES = Object.assign({
    result: require('./../../external-api-responses/market').CATEGORY_MATCH.SHOES
}, COMMON_PART_OF_MOCK);

const TABLET = Object.assign({
    result: require('./../../external-api-responses/market').CATEGORY_MATCH.TABLET
}, COMMON_PART_OF_MOCK);

const MOUSE_PAD = Object.assign({
    result: require('./../../external-api-responses/market').CATEGORY_MATCH.MOUSE_PAD
}, COMMON_PART_OF_MOCK);

const ADAPTER = Object.assign({
    result: require('./../../external-api-responses/market').CATEGORY_MATCH.ADAPTER
}, COMMON_PART_OF_MOCK);

const BEKKER_BK_4033 = Object.assign({
    result: require('./../../external-api-responses/market').CATEGORY_MATCH.BEKKER_BK_4033
}, COMMON_PART_OF_MOCK);

const TRANSCEND = Object.assign({
    result: require('./../../external-api-responses/market').CATEGORY_MATCH.TRANSCEND
}, COMMON_PART_OF_MOCK);

const FEATHER_ROSES = Object.assign({
    result: require('./../../external-api-responses/market').CATEGORY_MATCH.FEATHER_ROSES
}, COMMON_PART_OF_MOCK);

const IPHONE = Object.assign({
    result: require('./../../external-api-responses/market').CATEGORY_MATCH.IPHONE
}, COMMON_PART_OF_MOCK);

module.exports = {
    SSD_CATEGORIES,
    EMPTY,
    SHOES,
    TABLET,
    MOUSE_PAD,
    ADAPTER,
    BEKKER_BK_4033,
    TRANSCEND,
    FEATHER_ROSES,
    IPHONE
};
