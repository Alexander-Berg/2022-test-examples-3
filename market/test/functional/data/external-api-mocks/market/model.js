const COMMON_PART_OF_MOCK = {
    hostname: 'https://api.content.market.yandex.ru',
    path: /\/v1\/model\/\d+\.json/
};

const TRANSCEND = Object.assign({
    result: require('./../../external-api-responses/market').MODEL.TRANSCEND
}, COMMON_PART_OF_MOCK);

const IPHONE = Object.assign({
    result: require('./../../external-api-responses/market').MODEL.IPHONE
}, COMMON_PART_OF_MOCK);

const ASUS = Object.assign(
    {},
    COMMON_PART_OF_MOCK,
    {
        result: require('./../../external-api-responses/market').MODEL.ASUS,
        path: '/v1/model/10885077.json'
    }
);

const EXCLUDED_SURFACE_PRO = Object.assign(
    {},
    COMMON_PART_OF_MOCK,
    {
        result: require('./../../external-api-responses/market').MODEL.SURFACE_PRO,
        path: /\/v1\/model\/(?!10885077)\d+\.json/
    }
);

module.exports = {
    TRANSCEND,
    IPHONE,
    ASUS,
    EXCLUDED_SURFACE_PRO
};
