import { AppConfig } from '@yandex-int/yandex-config';

const config: AppConfig = {
    blackbox: {
        api: 'http://blackbox-test.yandex.net',
    },
    csrf: {
        key: 'ukCMcYccWRWLVAAc',
    },
    geocoder: {
        origin: 'http://addrs-testing.search.yandex.net',
        path: '/search/stable/yandsearch',
    },
    persAddress: {
        origin: 'http://pers-address.tst.vs.market.yandex.net',
    },
    uatraits: {
        server: 'http://uatraits-test.qloud.yandex.ru',
    },
};

module.exports = config;
