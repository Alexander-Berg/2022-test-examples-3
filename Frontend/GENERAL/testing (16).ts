import { AppConfig } from '@yandex-int/yandex-cfg';

import testingPreset from 'server/configs/csp/testing';

const config: AppConfig = {
    basePath: '/beta',

    blackbox: {
        api: 'blackbox-test.yandex.net',
    },

    csp: {
        presets: testingPreset,
    },

    uatraits: {
        server: 'http://uatraits-test.qloud.yandex.ru',
    },

    tvm: {
        serverUrl: 'http://localhost:2',
    },

    api: {
        url: 'http://backend-test.contest.yandex.net:80',
    },

    avatars: {
        host: 'https://avatars.mdst.yandex.net',
    },

    passport: {
        host: 'https://passport-test.yandex.ru',
    },
};

module.exports = config;
