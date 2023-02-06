const COMMON_PART_OF_MOCK = {
    hostname: 'https://api.content.market.yandex.ru',
    path: /\/v1\/category\/\d+\.json/
};

const TABLET = Object.assign({
    result: require('./../../external-api-responses/market').CATEGORY.TABLET
}, COMMON_PART_OF_MOCK);

const SHOES = Object.assign({
    result: require('./../../external-api-responses/market').CATEGORY.SHOES
}, COMMON_PART_OF_MOCK);

const THERMOS = Object.assign({
    result: require('./../../external-api-responses/market').CATEGORY.THERMOS
}, COMMON_PART_OF_MOCK);

const FEATHER_ROSES = Object.assign({
    result: require('./../../external-api-responses/market').CATEGORY.FEATHER_ROSES
}, COMMON_PART_OF_MOCK);

const EMPTY = Object.assign({
    result: require('./../../external-api-responses/market').CATEGORY.EMPTY
}, COMMON_PART_OF_MOCK);

module.exports = {
    TABLET,
    SHOES,
    THERMOS,
    FEATHER_ROSES,
    EMPTY
};
