const COMMON_PART_OF_MOCK = {
    hostname: 'https://api.content.market.yandex.ru',
    path: '/v1/redirect.json'
};

const SHOES = Object.assign({
    result: require('./../../external-api-responses/market').REDIRECT.SHOES
}, COMMON_PART_OF_MOCK);

const TABLET = Object.assign({
    result: require('./../../external-api-responses/market').REDIRECT.TABLET
}, COMMON_PART_OF_MOCK);
const ADAPTER = Object.assign({
    result: require('./../../external-api-responses/market').REDIRECT.ADAPTER
}, COMMON_PART_OF_MOCK);

module.exports = {
    SHOES,
    TABLET,
    ADAPTER
};