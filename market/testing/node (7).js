const cspPolicies = require('./cspPolicies');
const hosts = require('./hosts');

// Убрали в рамках MARKETPARTNER-24213, чтобы в БТ не зависеть от betastatic
// const STATIC_SELF = Boolean(process.env.STATIC_SELF);
const STATIC_SELF = true;

/**
 * Порт для прослушивания нашим приложением
 * Может быть обычным числовым значением или указывать на путь
 * по файловой системе сервера(в случае использования UNIX Socket)
 *
 * Например:
 *  - 1337
 *  - /var/run/yandex-service/ololo.sock
 *
 * Но, не смотря на значение, это всё ещё переменная окружения, так что её тип - строковый
 * @type {String}
 */
const NODE_PORT = process.env.NODE_PORT;

if (!NODE_PORT) {
    throw new Error('Environment variable NODE_PORT is not defined or empty.');
}

const SECRETS_DIR = process.env.SECRETS_DIR || '/etc/datasources';

const SALT_FILE = `${SECRETS_DIR}/salt.json`;

const balalaykaCert = {
    cert: `${SECRETS_DIR}/balalayka-test.base64.cer`,
    key: `${SECRETS_DIR}/balalayka-test.key`,
};

const DEFAULT_KADAVR_HOST = 'kadavr.vs.market.yandex.net';
const DEFAULT_KADAVR_PORT = 80;

const oauthApplication = {
    // @see {@link https://oauth.yandex.ru/client/b8c9a29dec8d49449af8e834f01a4334}
    clientId: 'b8c9a29dec8d49449af8e834f01a4334',
    clientSecret: '83910438203f48279f8c1712a3edeb6f',
};

// загрузка фидов через эксель
const feedOauthApplication = {
    scope: 'market:partner-api',
    oauthUrl: 'https://oauth.yandex.ru/authorize',
    clientId: 'bb4fc27e6d10470bbfd53155639e93a7', // Должно совпадать с аналогичным значением в x5
};

const rupostApplication = {
    clientId: 'xX4k5QPhADnefOMuXOfbSOXE7YEa',
};

const MEMCACHED_SERVERS = 'partner-front-cache.tst.vs.market.yandex.net:11239';

process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0'; // http://stackoverflow.com/questions/10888610/ignore-invalid-self-signed-ssl-certificate-in-node-js-with-https-request

const extEnv = process.env.EXT_ENV;

module.exports = {
    env: 'testing',
    service: 'partner',

    // Пока не используется
    // Возможно в будущем будем читать это из package.json
    // И скорее всего эта штука как-то будет фигурировать в чувствительных к версии фронта данных
    version: '1',

    server: NODE_PORT,

    // Добавляет в ответ debug-информацию (например. об ошибках)
    debug: true,

    // Включает безусловный показ внутреннего тулбара (трассировка, i18n, etc).
    showInternalToolbarAlways: true,

    /**
     * @type {'production' | 'testing'}
     */
    mdsFetcherSlug: 'testing',

    // Умолчательное значение для top level domain
    // Который является "домашним" для проекта(Яндекс - Российская компания, по этому ru)
    homeTLD: 'ru',

    cache: {
        type: 'memcache',
        options: {
            servers: MEMCACHED_SERVERS,
            generation: '1',
            timeouts: {
                connect: 100,
                idle: 10000,
                retries: 2,
                retry: 50,
                reconnect: 1000 * 60 * 5,
                operations: 10,
            },
            defaultKeyTTL: 1000 * 60 * 60,
            cacheTTL: 1000 * 60 * 60 * 24,
        },

        worker: {
            capacity: 500,
        },

        shared: {
            locations: MEMCACHED_SERVERS.split(';'),
            generation: '3',
            idle: 20000,
            retry: 300,
            reconnect: 1000,
            minTimeout: 100,
            maxTimeout: 200,
        },
    },

    tracerType: 'console',
    tracer: {
        format: ['{{title}}\t{{message}}'],
        dateformat: 'yyyy/mm/dd HH:MM:ss',
        level: 'info',
    },

    // Значение sk, которое всегда будет проходить валидацию (используем для нагрузочного тестирования)
    sk: '4d4a9485a03224c7347cd0cb74bd1712',

    // Для клиентского доступа
    hosts,

    hostsTld: {
        self: 'partner.market.yandex.<tld>',
        passport: 'passport.yandex.<tld>',
        tune: 'tune.yandex.<tld>',
        social: 'social-test.yandex.<tld>',
        market: 'default.exp.tst.market.yandex.<tld>',
        beru: 'desktop.bluemarket.fslb.beru.<tld>',
        yandex: 'yandex.<tld>',
        static: STATIC_SELF ? null : 'betastatic.yastatic.net',
        tech: 'tech.yandex.<tld>',
    },

    cspReportUrlEnv: 'testing',

    cspPolicies,

    payoneerProgramId: '100101370',

    tankerHelpUrl: 'https://wiki.yandex-team.ru/Market/frontend/Partner/docs/integration/tanker/',

    // Для серверного доступа
    servant: {
        advContentManager: {
            host: 'adv-content-manager.tst.vs.market.yandex.net',
        },
        'tvm-daemon': {
            timeout: 300,
        },
        banks: {
            host: 'banks-b2b-int.tst.vs.market.yandex.net',
        },
        lms: {
            host: 'lms.tst.vs.market.yandex.net',
            protocol: 'http:',
        },
        balalayka: {
            path: '/xmlrpc',
            protocol: 'https:',
            port: 6443,
            host: 'balalayka-test.paysys.yandex-team.ru',
        },
        cocon: {
            host: 'cocon.tst.vs.market.yandex.net',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'cocon',
            },
        },
        autoorder: {
            host: 'autoorder.tst.vs.market.yandex.net',
            protocol: 'https:',
            port: 443,
        },
        s3mds: {
            host: 's3.mds.yandex.net',
            protocol: 'https:',
        },
        staff: {
            host: 'staff-api.test.yandex-team.ru',
            protocol: 'https:',
        },
        yandexCharts: {
            host: 'market-test.charts.yandex.net',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'yandex_charts',
            },
        },
        _passport: {
            // Больше информации по адресу https://wiki.yandex-team.ru/market/verstka/services/passport
            // Для нагрузочных стрельб
            host: 'pass-stress-i1.sezam.yandex.net',
            data: {
                traceServiceId: 'passport_blackbox',
            },
        },
        passport: {
            host: 'blackbox-mimino.yandex.net',
            data: {
                traceServiceId: 'passport_blackbox',
            },
        },
        captcha: {
            host: 'api.captcha.yandex.net',
            data: {
                traceServiceId: 'passport_captcha',
            },
        },
        bunker: {
            host: 'bunker-api-dot.yandex.net',
            port: 80,
            path: '/v1',
            version: 'latest',
            timeout: 10 * 1000, // для тестинга таймаут поднят до 10 секунд
        },
        tanker: {
            host: 'tanker-api.yandex-team.ru',
            protocol: 'https:',
        },
        social: {
            host: 'social-test.yandex.ru',
            port: 80,
            data: {
                traceServiceId: 'passport_social',
            },
        },
        geocoder: {
            host: 'addrs-testing.search.yandex.net',
            path: '/search/stable/yandsearch',
            origin: 'market-partner',
            data: {
                traceServiceId: 'geocoder',
            },
        },
        marketId: {
            protocol: 'https:',
            host: 'marketid.tst.vs.market.yandex.net',
            port: 443,
            data: {
                traceServiceId: 'market_market_id',
            },
        },
        mbiLogProcessor: {
            host: 'mbi-log-processor.tst.vs.market.yandex.net',
        },
        mbiPartner: {
            host: 'mbi-partner.tst.vs.market.yandex.net',
            port: 38271,
            data: {
                traceServiceId: 'mbi_partner',
            },
        },
        mbiApi: {
            protocol: 'http:',
            host: 'mbi-back.tst.vs.market.yandex.net',
            port: 34820,
            data: {
                traceServiceId: 'market_mbi_api',
            },
        },
        mbiBilling: {
            host: 'mbi-billing.tst.vs.market.yandex.net',
            port: 34852,
            data: {
                traceServiceId: 'market_mbi_billing',
            },
        },
        checkouter: {
            protocol: 'http:',
            host: 'checkouter.tst.vs.market.yandex.net',
            port: 39001,
            data: {
                traceServiceId: 'market_checkouter',
            },
        },
        checkouterSand: {
            protocol: 'http:',
            host: 'checkouter.sand.tst.vs.market.yandex.net',
            port: 39001,
        },
        checkouterSecure: {
            protocol: 'https:',
            host: 'checkouter.tst.vs.market.yandex.net',
            port: 39011,
            data: {
                traceServiceId: 'market_checkouter',
            },
        },
        aboPublic: {
            host: 'abo-public.tst.vs.market.yandex.net',
            port: 38902,
            data: {
                traceServiceId: 'market_abo',
            },
        },
        _blackbox: {
            // Для нагрузочных стрельб
            host: 'pass-stress-i1.sezam.yandex.net',
            path: '/blackbox/',
            data: {
                traceServiceId: 'passport_blackbox',
            },
        },
        blackbox: {
            host: 'blackbox-mimino.yandex.net',
            path: '/blackbox/',
            data: {
                traceServiceId: 'passport_blackbox',
            },
        },
        passportMda: {
            protocol: 'https:',
            host: 'pass-rc.yandex.ru',
            data: {
                traceServiceId: 'passport_mda',
            },
        },
        proxy: {
            // http запрос на внутренний ресурс по url
        },
        priceCenter: {
            protocol: 'https:',
            host: 'price-center.tst.vs.market.yandex.net',
            path: '/report',
            port: 443,
            data: {
                traceServiceId: 'market_price-center_report',
            },
        },
        marketStatReports: {
            protocol: 'https:',
            host: 'market-reporting-api.tst.vs.market.yandex.net',
            path: '/reporting/v1',
            port: 443,
            data: {
                traceServiceId: 'market_mstat_reporting',
            },
        },
        gradesDistribution: {
            host: 'pers-static.tst.vs.market.yandex.net',
            port: 34522,
            data: {
                traceServiceId: 'market_pers_static',
            },
        },
        // TODO: зашить отладочный токен
        oauth: {
            protocol: 'https:',
            host: 'oauth.yandex.ru',
            port: 443,
            path: '/authorize',
        },
        owCommon: {
            protocol: 'https:',
            host: 'ow.tst.market.yandex-team.ru',
        },
        owCommon2: {
            protocol: 'https:',
            host: 'ow2.tst.market.yandex-team.ru',
        },
        ow: {
            protocol: 'https:',
            host: 'ow.tst.market.yandex-team.ru',
            path: '/api/tvm',
        },
        ow2: {
            protocol: 'https:',
            host: 'ow2.tst.market.yandex-team.ru',
            path: '/api/tvm',
        },
        yandexMetrika: {
            protocol: 'https:',
            host: 'api-metrika.yandex.ru',
            port: 443,
        },
        loyalty: {
            protocol: 'http:',
            host: 'market-loyalty.tst.vs.market.yandex.net',
            port: 35815,
            data: {
                traceServiceId: 'market_loyalty',
            },
        },
        referee: {
            protocol: 'http:',
            host: 'checkout-referee.tst.vs.market.yandex.net',
            port: 33484,
            data: {
                traceServiceId: 'market_checkout_referee',
            },
        },
        report: {
            protocol: 'http:',
            host: 'report.tst.vs.market.yandex.net',
            path: '/yandsearch',
            port: 17051,
            data: {
                traceServiceId: 'market_report',
            },
        },
        reportSand: {
            protocol: 'http:',
            host: 'ps.tst.vs.market.yandex.net',
            path: '/yandsearch',
            port: 17051,
        },
        reportInt: {
            protocol: 'http:',
            host: 'report.tst.vs.market.yandex.net',
            path: '/yandsearch',
            port: 17051,
            data: {
                traceServiceId: 'market_report',
            },
        },
        fulfillmentWorkflow: {
            // работа с логистикой в синем кабинете
            protocol: 'http:',
            host: 'ffw-api.tst.vs.market.yandex.net',
            port: 80,
        },
        ff4Shops: {
            protocol: 'https:',
            host: 'ff4shops.tst.vs.market.yandex.net',
        },
        nesu: {
            // логистический фасад для Яндекс Доставки 3.0
            protocol: 'https:',
            host: 'nesu.tst.vs.market.yandex.net',
            port: 443,
        },
        sanitizer: {
            // Поставляемый Яндекс Почтой сервис очистки html от потенциально опасного user-generated содержимого
            protocol: 'https:',
            host: 'sanitizer-test.pers.yandex.net',
            port: 443,
        },
        vendors: {
            host: 'vendor-partner.tst.vs.market.yandex.net',
            port: 34867,
            data: {
                traceServiceId: 'market_vendor-api',
            },
        },
        cataloger: {
            protocol: 'http:',
            host: 'marketcataloger.tst.vs.market.yandex.net',
            path: '/cataloger',
            port: 29302,
        },
        persGrade: {
            protocol: 'http:',
            host: 'pers-grade.tst.vs.market.yandex.net',
            port: 35824,
        },
        persQA: {
            protocol: 'http:',
            host: 'pers-qa.tst.vs.market.yandex.net',
        },
        buker: {
            protocol: 'http:',
            host: 'marketbuker.tst.vs.market.yandex.net',
            port: 29310,
            data: {
                traceServiceId: 'market_kgb_buker',
            },
        },
        shopInfo: {
            host: 'shopinfo.tst.vs.market.yandex.net',
            port: 38110,
            data: {
                traceServiceId: 'market_mbi_shopinfo',
            },
        },
        marketUtils: {
            host: 'market-utils.tst.vs.market.yandex.net',
            port: 35826,
            data: {
                traceServiceId: 'market_utils',
            },
        },
        suggest: {
            host: 'yandex.ru',
        },
        testBalance: {
            protocol: 'http:',
            host: 'greed-ts.paysys.yandex.ru',
            port: 30702,
            path: '/xmlrpc',
        },
        sberlog: {
            protocol: 'https:',
            host: 'sberlog.tst.vs.market.yandex.net',
            port: 443,
        },
        balance: {
            protocol: 'http:',
            host: 'greed-ts.paysys.yandex.ru',
            port: 8002,
            path: '/xmlrpc',
        },
        mbiPartnerStat: {
            protocol: 'https:',
            host: 'mbi-partner-stat.tst.vs.market.yandex.net',
            port: 443,
        },
        tpl: {
            protocol: 'https:',
            host: 'tpl-int.tst.vs.market.yandex.net',
            port: 443,
            data: {
                traceServiceId: 'tpl',
            },
        },
        autogenApi: {
            protocol: 'http:',
            host: 'autogen-api.tst.vs.market.yandex.net',
            port: 34540,
        },
        dataCampBlue: {
            port: 80,
            protocol: 'http:',
            host: 'datacamp.blue.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'dataCampBlue',
            },
        },
        dataCampWhite: {
            port: 80,
            protocol: 'http:',
            host: 'datacamp.white.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'dataCampWhite',
            },
        },
        geoSuggest: {
            port: 443,
            protocol: 'https:',
            host: 'suggest-maps-test.n.yandex-team.ru',
        },
        saas: {
            port: 80,
            protocol: 'http:',
            host: 'prestable-market-idx.saas.yandex.net',
            path: '/market_datacamp_shop',
            data: {
                traceServiceId: 'saas',
            },
        },
        saasBusiness: {
            port: 80,
            protocol: 'http:',
            host: 'prestable-market-idx.saas.yandex.net',
            path: '/market_datacamp',
            data: {
                traceServiceId: 'saasBusiness',
            },
        },
        sortingCenter: {
            protocol: 'https:',
            host: 'sc-int.tst.vs.market.yandex.net',
            port: 443,
            data: {
                traceServiceId: 'sortingCenter',
            },
        },
        priceLabs: {
            protocol: 'https:',
            host: 'pricelabs-api2.tst.vs.market.yandex.net',
            port: 443,
            path: '/api/v1/public',
        },
        tplOutlet: {
            protocol: 'https:',
            host: 'pvz-int.tst.vs.market.yandex.net',
            port: 443,
            data: {
                traceServiceId: 'tplOutlet',
            },
        },
        tplPartner: {
            protocol: 'https:',
            host: 'pvz-int.tst.vs.market.yandex.net',
            port: 443,
            data: {
                traceServiceId: 'tplPartner',
            },
        },
        tplBilling: {
            protocol: 'https:',
            host: 'tpl-billing.tst.vs.market.yandex.net',
            port: 443,
            data: {
                traceServiceId: 'tplBilling',
            },
        },
        arbiter: {
            host: 'arbiter.tst.vs.market.yandex.net',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'arbiter',
            },
        },
        mbo: {
            host: 'mbo-http-exporter.tst.vs.market.yandex.net',
            port: 8084,
            data: {
                traceServiceId: 'mbo',
            },
        },
        mboc: {
            protocol: 'https:',
            host: 'cm-testing.market.yandex-team.ru',
            port: 443,
            data: {
                traceServiceId: 'mboc',
            },
        },
        utilizer: {
            protocol: 'http:',
            host: 'utilizer.pre.vs.market.yandex.net',
        },
        refs: {
            host: 'refs-test.paysys.yandex.net',
            protocol: 'https:',
            port: 443,
        },
        supplier1p: {
            host: 'partner-marketing.tst.vs.market.yandex.net',
            protocol: 'http:',
            port: 80,
            path: '/api/v1',
            data: {
                traceServiceId: 'partnerMarketing',
            },
        },
        calendaringService: {
            host: 'calendaring-service.tst.vs.market.yandex.net',
            protocol: 'http:',
            port: 80,
            data: {
                traceServiceId: 'calendaringService',
            },
        },
        tplCarrier: {
            protocol: 'https:',
            host: 'tpl-partner-carrier.tst.vs.market.yandex.net',
            port: 443,
        },
        tsup: {
            protocol: 'http:',
            host: 'tsup.tst.vs.market.yandex.net',
            port: 80,
        },
        logistrator: {
            protocol: 'http:',
            host: 'logistrator.tst.vs.market.yandex.net',
            port: 80,
        },
        mbiBpmn: {
            protocol: 'http:',
            host: 'mbi-bpmn.tst.vs.market.yandex.net',
        },
        marketBillingApi: {
            protocol: 'https:',
            host: 'market-billing-api.tst.vs.market.yandex.net',
        },
    },

    // {@see {@link https://www.npmjs.com/package/busboy}}
    busboy: {
        limits: {
            files: 20,
            fileSize: 16777216, // 16MB
        },
    },

    bodyParser: {
        urlencoded: {
            limit: '2MB',
            extended: false,
        },
        json: {
            limit: '2MB',
        },
    },

    skipWarmUp: false,

    geobase: {
        fallback: {
            timeout: 200,
        },
        remote: {
            path: '/var/run/node-init-cluster/market-vertis-geobase/geobase.sock',
            params: {
                poolMinDelay: 300,
                poolMaxDelay: 700,
                poolRetries: 2,
                poolSize: 20,
            },
        },
        native: {
            path: '/var/cache/geobase/geodata6.bin',
        },
    },

    heapdumps: {
        path: '/var/tmp/cores/yandex-market-partner-heapdumps',
        format: '[market-partner]_YYYY-MM-DDTHH:mm:ss',
    },

    traceLogger: {
        socket: process.env.TRACE_LOG_SOCK,
    },

    secretsDir: SECRETS_DIR,

    tvm: {
        env: 'market_tpl_front_testing-mimino',
        configPath: '../../tvm-daemon/tvmtool.conf',
        authPath: '../../tvm-daemon/local.auth',
    },

    saltFile: SALT_FILE,

    ipregDataPath: '/var/cache/geobase/ipreg-layout.json',

    isInternalNetworkConfigurable: true,
    isCoconCommitConfigurable: true,

    // счётчики
    counters: {
        MAIN: {
            id: '87751288',
            options: {
                tns: {
                    account: 'yandex_ru',
                    tmsec: 'yandex_market',
                },
            },
        },
    },

    oauthApplication,

    feedOauthApplication,

    rupostApplication,

    yaMessenger: {
        SHOP: {
            serviceId: 12,
            guid: '4af14900-ce09-9b13-427d-a57a25873bd8',
            iframeUrl: 'https://renderer-chat-dev.hamster.yandex.ru/chat?build=chamb',
            iframeUrlParams: {config: 'development'},
        },
        SUPPLIER: {
            serviceId: 12,
            guid: '50f63800-62ea-9764-7714-7eb30d1ac242',
            iframeUrl: 'https://renderer-chat-dev.hamster.yandex.ru/chat?build=chamb',
            iframeUrlParams: {config: 'development'},
        },
    },

    kadavrAvailable: true,
    kadavrHost: process.env.KADAVR_HOST || DEFAULT_KADAVR_HOST,
    kadavrPort: process.env.KADAVR_PORT || DEFAULT_KADAVR_PORT,

    balalaykaCert,

    errorBooster: {
        project: 'partner_market_front',
        platform: 'desktop',
    },

    // google tag manager
    gtmAvailable: false,

    bunkerRootNode: '/market-tpl',

    starTrack: {
        queues: {
            COMPLAINS: {
                name: 'TPLCOMPLAINSTST',
                localFieldId: '624c69295466ff5e8248637b',
            },
        },
    },
};

if (extEnv) {
    /* eslint-disable global-require */
    const _ = require('lodash');

    _.merge(module.exports, require(`./${extEnv}`));
    /* eslint-enable global-require */
}
