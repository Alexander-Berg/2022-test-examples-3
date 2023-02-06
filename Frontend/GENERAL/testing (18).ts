import { AppConfig } from 'yandex-cfg';

import testingCsp from './csp/testing';

// env.js генерится при в сандбоксе при сборке в CI и используется только в рантайме
// eslint-disable-next-line import/no-unresolved, global-require
const APP_VERSION = process.env.APP_VERSION || require('../env.js').APP_VERSION;

const config: AppConfig = {
    csp: {
        presets: testingCsp,
    },

    httpGeobase: {
        server: 'http://geobase-test.qloud.yandex.ru',
    },

    httpLangdetect: {
        server: 'http://langdetect-test.qloud.yandex.ru',
    },

    httpUatraits: {
        server: 'http://uatraits-test.qloud.yandex.ru',
    },

    logger: {
        fields: {
            release: APP_VERSION,
        },
    },

    static: {
        version: APP_VERSION,
    },
};

module.exports = config;
