import { AppConfig } from '@yandex-int/yandex-cfg';

import testingPreset from 'src/server/configs/csp/testing';

const config: AppConfig = {
    blackbox: {
        api: 'blackbox-mimino.yandex.net',
    },

    csp: {
        presets: testingPreset,
    },

    dev: false,

    httpGeobase: {
        server: 'http://geobase-test.qloud.yandex.ru',
    },

    httpLangdetect: {
        server: 'http://langdetect-test.qloud.yandex.ru',
    },

    httpUatraits: {
        server: 'http://uatraits-test.qloud.yandex.ru',
    },

    tvm: {
        serverUrl: 'http://localhost:2',
    },
};

module.exports = config;
