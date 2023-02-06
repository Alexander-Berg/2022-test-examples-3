'use strict';

const redirHost = process.env.redirHost || process.env.REDIR_HOST || 'https://sovetnik.market.yandex.ru';

module.exports = {
    yandexApiBaseUrl: {
        v1: 'https://api.content.market.yandex.ru/v1/',
        'v2.0.0': 'https://api.content.market.yandex.ru/v2.0.0/',
        'v2.0.5': 'https://api.content.market.yandex.ru/v2.0.5/',
        'v2.1.0': 'https://api.content.market.yandex.ru/v2.1.0/',
    },
    yandexAviaApiBaseUrl: 'https://api.avia.yandex.net/sovetnik/v1.0/',
    redirUrl: redirHost + '/redir',
    currenciesApiUrl: 'http://stocks-dev.yandex.net/xmlhist/all.xml',
    directApiURL: 'http://bigb-fast.yandex.ru/bigb',
    statsDHost: 'localhost',
    cacheDir: '/tmp/sovetnik-backend',
    defaultGeoId: 213,
    socket: '/tmp/sovetnik-backend.sock',
    trafficCuttingPercent: 0,
};
