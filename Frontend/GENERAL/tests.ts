import { AppConfig } from '@yandex-int/yandex-config';

const config: AppConfig = {
    blackbox: {
        api: 'http://blackbox-test.yandex.net',
    },
    csrf: {
        key: 'CPGHKcPGYOhyCgEu',
    },
    geobase: {
        origin: 'http://geobase-test.qloud.yandex.ru',
        isHttp: true,
        nocache: true,
    },
    geocoder: {
        origin: 'http://addrs-testing.search.yandex.net',
        path: '/search/stable/yandsearch',
    },
    logger: {
        streams: {
            stdout: process.env.ENABLE_LOGS === '1',
            deploy: false,
            errorBooster: false,
        },
    },
    persAddress: {
        origin: 'http://pers-address.tst.vs.market.yandex.net',
    },
    tvm: {
        serverUrl: 'http://localhost:8001',
        cacheMaxAge: 0,
    },
    uatraits: {
        server: 'http://uatraits-test.qloud.yandex.ru',
    },
};

module.exports = config;
