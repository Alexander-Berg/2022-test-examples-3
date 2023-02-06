require('../../register')();

const {kadavrHost, kadavrPort} = require('../node');
const path = require('path');

const CHROME = {
    desiredCapabilities: {
        browserName: 'chrome',
        version: '69.0',
    },
};

module.exports = {
    baseUrl: 'https://partner.market.fslb.yandex.ru/',

    hermione: {
        browsers: {
            chrome: CHROME,
        },
        system: {
            ctx: {
                services: {
                    mbiPartner: {
                        url: 'http://mbi-partner.tst.vs.market.yandex.net:38271',
                    },
                    mbiPartnerStat: {
                        url: 'https://mbi-partner-stat.tst.vs.market.yandex.net:443',
                    },
                    qaElliptics: {
                        url: 'http://qa-storage.yandex-team.ru/',
                    },
                    checkout: {
                        url: 'https://checkouter.tst.vs.market.yandex.net:39011',
                        credentials: {
                            user: 'checkouter-client',
                            password: 'aYh59J1b6Sn62H',
                        },
                    },
                    report: {
                        url: 'http://report.tst.vs.market.yandex.net:17051',
                    },
                    oauth: {
                        url: 'https://oauth-test.yandex.ru',
                    },
                    balance: {
                        url: 'https://pci-tf.fin.yandex.net',
                    },
                    referee: {
                        url: 'http://checkout-referee.tst.vs.market.yandex.net:33484',
                    },
                },
            },
            mochaOpts: {
                fgrep: /(Кабинет 3PL_ПВЗ|Кабинет 3PL|Кабинет СЦ|Боковое меню 3PL СЦ|Боковое меню 3PL|Кабинет ЦУП|Кабинет магистрального перевозчика)/,
            },
        },
    },

    kadavr: {
        host: kadavrHost,
        port: kadavrPort,
        pageBlank: '/ping',
        noSessionLog: true,
    },

    suiteManager: {
        caseFilter: {
            environment: 'testing|all',
        },
    },

    statReporter: {
        reporters: {
            json: {
                enabled: true,
                path: path.resolve(__dirname, '../../../stat-reporter.json'),
                statusIncludesBroken: true,
            },
        },
    },
};
