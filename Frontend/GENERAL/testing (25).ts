import { AppConfig } from '@yandex-int/yandex-cfg';

import testingPreset from 'src/server/configs/csp/testing';

const config: AppConfig = {
    api: {
        hostname: 'admin.api-testing.icm2022.org',
        pathname: '/internal/admin/v1',
        protocol: 'https',
        timeout: 15000,
    },

    blackbox: {
        api: 'blackbox.yandex-team.ru',
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
