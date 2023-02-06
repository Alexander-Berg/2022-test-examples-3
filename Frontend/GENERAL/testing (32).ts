import { AppConfig } from '@yandex-int/yandex-cfg';

import { testingCspPresets } from './csp/testing';

const config: AppConfig = {
    blackbox: {
        api: 'blackbox.yandex-team.ru',
    },

    csp: {
        presets: testingCspPresets,
    },

    uatraits: {
        server: 'http://uatraits-test.qloud.yandex.ru',
    },

    tvm: {
        serverUrl: 'http://localhost:2',
    },

    yandexRootCAPath: '/app/cert/YandexInternalRootCA.crt',

    redis: {
        sentinels: [
            { host: 'sas-21iyylwvna0asitb.db.yandex.net', port: 26379 },
            { host: 'sas-g2qsfpx4b5z0g5z0.db.yandex.net', port: 26379 },
            { host: 'vla-ztm3rfbw61igcq01.db.yandex.net', port: 26379 },
        ],
        name: 'seos-sessions-test',
    },

    s3: {
        docsPath: 'seos_test/docs',
    },

    solomon: {
        cluster: 'testing',
    },

    ws: {
        allowOrigins: [
            'https://seos.test.yandex-team.ru',
            'https://interview.test.in.yandex-team.ru',
        ],
    },
};

module.exports = config;
