import { AppConfig } from '@yandex-int/yandex-cfg';

import { Domain } from 'src/common/types/domain';

import testingPreset from 'src/configs/csp/testing';

const config: AppConfig = {
    api: {
        hostname: 'api-testing.icm2022.yandex.net',
        pathname: '/internal/v1',
        protocol: 'https',
        timeout: 15000,
    },

    avatars: {
        protocol: 'https',
        hostname: 'avatars.mds.yandex.net',
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

    // mediaplatform: {
    //     protocol: 'https',
    //     hostname: 'meduza-api-development.common.yandex.net',
    //     pathname: '/v1',
    //     timeout: 15000,
    // },
    mediaplatform: {
        protocol: 'https',
        hostname: 'meduza-api.common.yandex.net',
        pathname: '/v1',
        timeout: 15000,
    },

    oauth: {
        yandex: {
            clientID: 'eea639d078a248249c9b8fa08283e7e5',
            clientSecret: process.env.YANDEX_CLIENT_SECRET,
            authorizationURL: 'https://oauth.yandex.com/authorize',
            tokenURL: 'https://oauth.yandex.com/token',
        },
    },

    passport: {
        protocol: 'https',
        hostname: 'passport.yandex.com',
    },

    tldToCallbackUrl: {
        [Domain.RU]: {
            protocol: 'https',
            hostname: 'testing.icm2022.ru',
        },
        [Domain.ORG]: {
            protocol: 'https',
            hostname: 'testing.icm2022.org',
        },
        [Domain.NET]: {
            protocol: 'https',
            hostname: 'testing.icm2022.yandex.net',
        }
    },

    tvm: {
        serverUrl: 'http://localhost:2',
    },

    registrationForm: {
        url: 'https://forms.yandex.com/surveys/10031421.88fde73d384f656a01db721811466861f5dbf2b6/?lang=en',
    },

    registrationFormMassMedia: {
        url: 'https://forms.yandex.com/surveys/11293676.f9e92738e32dd0dd54e88e72f1ef5750b7dab0b8/?lang=en',
    },

    grantsForm: {
        url: 'https://forms.yandex.com/surveys/10025954.c5f27d2370af0f1a4b79fbf98cdc6f2bce07d738/?lang=en',
    },

    wm2Form: {
        url:
            'https://forms.yandex.com/surveys/10033082.386127cef2f6bf72253af0dd7b8eba95df57d7be/?required_wm=True&lang=en',
    },

    s3: {
        bucket: 'icm-members-files',
        client: {
            credentials: {
                accessKeyId: process.env.AWS_ACCESS_KEY_ID,
                secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
            },
            endpoint: 'https://s3.mdst.yandex.net',
        },
    },
};

module.exports = config;
