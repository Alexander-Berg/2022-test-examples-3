const COMMON_PART_OF_MOCK = {
    hostname: 'https://api.content.market.yandex.ru',
    path: '/v1/model/match.json'
};

const TRANSCEND = Object.assign({
    result: require('./../../external-api-responses/market').MODEL_MATCH.TRANSCEND
}, COMMON_PART_OF_MOCK);

const IPHONE = Object.assign({
    result: require('./../../external-api-responses/market').MODEL_MATCH.IPHONE
}, COMMON_PART_OF_MOCK);

const ASUS = Object.assign({
    result: require('./../../external-api-responses/market').MODEL_MATCH.ASUS
}, COMMON_PART_OF_MOCK);

const EMPTY = Object.assign({
    result: require('./../../external-api-responses/market').MODEL_MATCH.EMPTY
}, COMMON_PART_OF_MOCK);

module.exports = {
    TRANSCEND,
    IPHONE,
    ASUS,
    EMPTY
};
