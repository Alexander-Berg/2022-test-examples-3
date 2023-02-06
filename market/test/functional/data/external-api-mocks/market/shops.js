const COMMON_PART_OF_MOCK = {
    hostname: 'https://api.content.market.yandex.ru',
    path: '/v1/shops.json'
};

const OZON = Object.assign({
    result: require('./../../external-api-responses/market').SHOPS.OZON
}, COMMON_PART_OF_MOCK);

const EMPTY = Object.assign({
    result: require('./../../external-api-responses/market').SHOPS.EMPTY
}, COMMON_PART_OF_MOCK);

const LAMODA = Object.assign({
    result: require('./../../external-api-responses/market').SHOPS.LAMODA
}, COMMON_PART_OF_MOCK);

const E_KATALOG = Object.assign({
    result: require('./../../external-api-responses/market').SHOPS.E_KATALOG
}, COMMON_PART_OF_MOCK);

module.exports = {
    OZON,
    EMPTY,
    LAMODA,
    E_KATALOG
};
