const COMMON_PART_OF_MOCK = {
    hostname: 'https://api.content.market.yandex.ru',
    path: /\/v1\/filter\/\d+\.json/
};

const SHOES = Object.assign({
    result: require('./../../external-api-responses/market').FILTER.SHOES
}, COMMON_PART_OF_MOCK);

const TABLET = Object.assign({
    result: require('./../../external-api-responses/market').FILTER.TABLET
}, COMMON_PART_OF_MOCK);

const ADAPTER = Object.assign({
    result: require('./../../external-api-responses/market').FILTER.ADAPTER
}, COMMON_PART_OF_MOCK);

module.exports = {
    SHOES,
    TABLET,
    ADAPTER
};