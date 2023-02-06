'use strict';

module.exports = {
    nodeName: require('os').hostname().split('.')[0],

    avatarOrigin: 'https://avatars.mdst.yandex.net',

    isCorp: false,
    forceAuth: false,
    blackbox: {
        host: 'pass-test.yandex.ru',
        path: '/blackbox'
    },
    backend: {
        hostname: 'docviewer.deploy.dst.yandex.net',
        port: 80
    },

    cryprox: {
        protocol: 'https:',
        hostname: 'cryprox.yandex.net',
        port: 443
    },

    cloudAPI: {
        hostname: 'api-stable.dst.yandex.net',
        // hostname: 'intapi.disk.yandex.net',
        port: 8080
    },

    protocols: require('./protocols.js'),

    // тестовые хосты заберуна и стораджей, используются для CSP (политика object-src)
    downloaderHostWithoutTld: 'downloader.dst.yandex.',
    storageHosts: ['*.mdst.yandex.net', '*.stm.yandex.net'],

    uaasService: 'disk',
    uaasHost: 'uaas.search.yandex.net',

    // домены, для которых разрешено встраивать DV в iframe
    allowedIframeDomains: [
        'demo.tracker.test.yandex.com',
        'demo.tracker.test.yandex.ru',
        'tools-b2b-tracker.crowdtest.yandex.com',
        'tools-b2b-tracker.crowdtest.yandex.ru',
        'tracker.test.yandex.com', 'tracker.test.yandex.ru',
        'tracker.local.yandex.com', 'tracker.local.yandex.ru',
        'tracker.demo.local.yandex.com',
        'tutor.hamster.yandex.ru',

        // https://st.yandex-team.ru/CHEMODAN-73380#5f3e4f430a30f5726c984bf9
        'wirth.dev.praktikum.yandex.ru',
        'wirth.dev.praktikum.yandex.com',
        'wirth.testing.praktikum.yandex.ru',
        'wirth.testing.praktikum.yandex.com',
        'uchitel.testing.praktikum.yandex.ru',
        'wirth.fast-testing.praktikum.yandex.ru',
        'wirth.fast-testing.praktikum.yandex.com',
        'wirth-admin.dev.praktikum.yandex.ru',
        'wirth-admin.testing.praktikum.yandex.ru',
        '*.common-dev.praktikum.yandex.ru',

        'docs.dst.yandex.ru',
        'docs2.dst.yandex.ru',
        '*.docs-dev.dsd.yandex.ru'
    ]
};
