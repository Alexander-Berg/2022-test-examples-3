const COMMON_PART_OF_MOCK = {
    hostname: 'https://api.content.market.yandex.ru',
    path: /\/v1\/model\/(?!10885077)\d+\.json/
};

const SURFACE_PRO = Object.assign({
    result: require('./../../external-api-responses/market').MODEL.SURFACE_PRO
}, COMMON_PART_OF_MOCK);

module.exports = {
    SURFACE_PRO
};
