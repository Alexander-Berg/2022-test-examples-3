const COMMON_PART_OF_MOCK = {
    hostname: 'https://api.content.market.yandex.ru',
    path: /v1\/model\/\d+\/outlets.json/
};

const TRANSCEND = Object.assign({
    result: require('./../../external-api-responses/market').MODEL_OUTLETS.TRANSCEND
}, COMMON_PART_OF_MOCK);
const EMPTY = Object.assign({
    result: require('./../../external-api-responses/market').MODEL_OUTLETS.EMPTY
}, COMMON_PART_OF_MOCK);

module.exports = {
    TRANSCEND,
    EMPTY
};
