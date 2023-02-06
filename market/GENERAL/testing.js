const fs = require('fs');
const path = require('path');

const csp = require('express-yandex-csp');

const tvmTokenPath = path.resolve(__dirname, '../../tvm-daemon/local.auth');
const tvmToken = fs.readFileSync(tvmTokenPath, 'utf8').replace(/\s/g, '');

if (!tvmToken) {
    throw new Error(`tvmToken should not be empty. Check ${tvmTokenPath} file.`);
}

module.exports = {
    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        version: 'latest',
        updateInterval: 30000
    },

    passport: {
        host: {
            ru: 'passport.yandex.ru',
            com: 'passport.yandex.com',
            'com.tr': 'passport.yandex.com.tr',
            by: 'passport.yandex.by',
            kz: 'passport.yandex.kz'
        }
    },

    blackbox: {
        api: 'blackbox-mimino.yandex.net'
    },

    mail: {
        smtp: 'outbound-relay.yandex.net'
    },

    uatraits: {
        server: 'http://uatraits-test.qloud.yandex.ru'
    },

    csp: {
        extend: {
            'style-src': [csp.SELF, 'yastatic.net'],
            'font-src': [csp.SELF],
            'frame-src': ['adv-source-testing.qloud.yandex.net']
        }
    },

    // host: 'https://partner-adv.tst.vs.market.yandex.',
    host: 'https://market.fslb.yandex.',

    sources: {
        url: 'https://adv-source-testing.qloud.yandex.net'
    },

    subscribe: {
        auth: '478a66deb0864f5e852a426ecfd40df1',
        hostname: 'test.sender.yandex-team.ru/api/0'
    },

    tvm: {
        token: tvmToken
    }
};
