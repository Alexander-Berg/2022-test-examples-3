const _ = require('lodash');
const csp = require('express-yandex-csp');

module.exports = {
    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        version: 'latest',
        updateInterval: 30000
    },

    backend: {
        baseUrl: 'https://adv-backend-test.in.yandex-team.ru/v1/'
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

    langs: ['ru', 'en', 'be', 'zh-Hans', 'tr', 'uk'],

    blackbox: {
        api: 'blackbox-mimino.yandex.net',
        getServiceTicket: req => _.get(req, 'tvm.tickets.blackbox-mimino.ticket')
    },

    mail: {
        smtp: 'outbound-relay.yandex.net'
    },

    blogs: {
        host: 'https://adv-blogs-pumpkin-test.commerce-int.yandex-team.ru/v1',
        rss: 'https://yablogs-testing.common.yandex.ru/blog',
        commentsMigration: 'http://yablogs-api-test.common.yandex.ru/v1'
    },

    uatraits: {
        server: 'http://uatraits-test.qloud.yandex.ru'
    },

    static: {
        host: `https://yastatic.net/s3/vda/static/${process.env.APP_VERSION}`,
        dir: `${__dirname}/../out/public`
    },

    static2: {
        baseUrl: 'https://yastatic.net/s3/vda/static2/',
        frozenPath: '_',
        version: process.env.APP_VERSION
    },

    csp: {
        presets: {
            self: {
                'style-src': [csp.SELF, 'yastatic.net'],
                'font-src': [csp.SELF],
                'frame-src': ['vda-testing.commerce-int.yandex.net']
            },
            cmnt: require('./csp/presets/ya-cmnt-testing')
        }
    },

    direct: {
        hostname: 'intapi.test.direct.yandex.ru',
        port: '9000'
    },

    dashboard: {
        interest: 'http://back-test.advq.yandex.ru/advertising-categories/stats.json',
        cpc: 'http://bssoap-test01g.yandex.net/export/export_adv-stat.cgi'
    },

    host: 'https://l7test.yandex.',
    alternativeHost: 'https://www.l7test.yandex.',
    indexHost: 'adv-testing.commerce-int.yandex.',

    sources: {
        url: 'https://vda-testing.commerce-int.yandex.net'
    },

    subscribe: {
        auth: {
            default: process.env.SUBSCRIPTION_KEY_DEFAULT
        },
        hostname: 'test.sender.yandex-team.ru/api/0'
    },

    tvm: {
        destinations: [
            'blogs',
            'direct',
            'blackbox-mimino',
            'cmnt',
            'adv-backend'
        ]
    },

    regionsHosts: {
        cn: 'https://adv-testing.commerce-int.yandex-ad.cn'
    },

    caseCategory: {
        id: '5d88e194392d5b002f1b4c85',
        parentCategoryId: '5c98bf970825df766988df10'
    },

    cmnt: {
        loader: 'https://betastatic.yastatic.net/comments/v1/comments.js',
        api: 'http://httpadapter-dev-common.n.yandex-team.ru/cmnt/v1',
        maxPostsSlugs: 5
    },

    commonMetrikaCounters: [
        {
            id: '76132642',
            tld: ['ru']
        }
    ]
};
