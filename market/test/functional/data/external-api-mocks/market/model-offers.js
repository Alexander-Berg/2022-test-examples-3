const COMMON_PART_OF_MOCK = {
    hostname: 'https://api.content.market.yandex.ru',
    path: /v1\/model\/\d+\/offers.json/
};

const TRANSCEND = Object.assign({
    result: require('./../../external-api-responses/market').MODEL_OFFERS.TRANSCEND
}, COMMON_PART_OF_MOCK);
const EMPTY = Object.assign({
    result: require('./../../external-api-responses/market').MODEL_OFFERS.EMPTY
}, COMMON_PART_OF_MOCK);

const TRANSCEND_JSI = Object.assign({
    result: require('./../../external-api-responses/market').MODEL_OFFERS.TRANSCEND_JSI
}, COMMON_PART_OF_MOCK);

module.exports = {
    TRANSCEND,
    EMPTY,
    TRANSCEND_JSI
};
