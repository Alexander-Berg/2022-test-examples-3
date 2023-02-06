import { AppConfig } from '@yandex-int/yandex-cfg';
import testingPreset from 'src/server/configs/csp/testing';

const config: AppConfig = {
    api: {
        'meduza-api': {
            host: 'http://meduza-api-testing.common.yandex.net',
        },
        'media-platform-api': {
            host: 'http://api-testing.media-platform-internal.yandex.net',
        },
    },

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
    sender: {
        key: '6f7babd8f5a04c2b89087490d2d463c6',
        host: 'https://test.sender.yandex-team.ru/',
        maillist: '8YX3LV73-HSJ',
    },
};

module.exports = config;
