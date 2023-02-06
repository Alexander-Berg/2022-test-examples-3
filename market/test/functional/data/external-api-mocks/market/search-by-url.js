const COMMON_PART_OF_MOCK = {
    hostname: 'https://api.content.market.yandex.ru',
    path: /\/v1\/search\.json/,
    query: {
        text: /^url/
    }
};

const EMPTY = Object.assign({
    result: require('./../../external-api-responses/market').SEARCH.EMPTY_BY_URL
}, COMMON_PART_OF_MOCK);

module.exports = {
    EMPTY
};
