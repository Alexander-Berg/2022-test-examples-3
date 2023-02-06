import { AppConfig } from '@yandex-int/yandex-cfg';

import testingPreset from 'src/server/configs/csp/testing';

const config: AppConfig = {
    blackbox: {
        api: 'blackbox.yandex-team.ru',
    },

    contest: {
        retry: 0,
        timeout: 30000,
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

    yabox: {
        api: 'blackbox-mimino.yandex.net',
    },
};

module.exports = config;
