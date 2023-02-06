import { AppConfig } from 'yandex-cfg';

import testingCsp from './csp/testing';

// env.js генерится при в сандбоксе при сборке в CI и используется только в рантайме
// eslint-disable-next-line import/no-unresolved, global-require
const APP_VERSION = process.env.APP_VERSION || require('../env.js').APP_VERSION;

const config: AppConfig = {
    appVersion: APP_VERSION,

    blackbox: {
        api: 'blackbox-mimino.yandex.net',
    },

    errorCounter: {
        env: 'testing',
    },

    passport: {
        host: 'https://passport.yandex.{tld}',
        avatarHost: 'https://avatars.mds.yandex.net',
        passHost: 'https://pass.yandex.{tld}',
    },

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

    static: {
        baseUrl: '//yastatic.net/s3/events/static/events-front/',
        frozenPath: '_',
        version: APP_VERSION,
    },
};

module.exports = config;
