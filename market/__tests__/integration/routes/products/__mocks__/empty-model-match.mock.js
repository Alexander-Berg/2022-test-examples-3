const HOST = 'https://api.content.market.yandex.ru';

const ROUTE = /\/v[0-9.]+\/models\/match/;

const RESPONSE = {
    status: 'OK',
    context: {
        region: {
            id: 213,
            name: 'Москва',
            type: 'CITY',
            childCount: 14,
            country: 225,
        },
        currency: {
            id: 'RUR',
            name: 'руб.',
        },
        processingOptions: {},
        id: '1571780017394/fdda653cdafec2e82e149a8d86950500',
        time: '2019-10-23T00:33:37.453+03:00',
        marketUrl: 'https://market.yandex.ru?pp=1002&clid=2210590&distr_type=4',
    },
    models: [],
};

module.exports = {
    host: HOST,
    route: ROUTE,
    response: RESPONSE,
};
