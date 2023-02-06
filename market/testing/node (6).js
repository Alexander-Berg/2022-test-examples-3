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

const cmsPreviewTokenFile = `${SECRETS_DIR}/cms_preview_token.json`;

const mbiPartnerEsPartnersCredsFile = `${SECRETS_DIR}/mbi-partner-es-partners_creds.json`;

const SALT_FILE = `${SECRETS_DIR}/salt.json`;

const balalaykaCert = {
    cert: `${SECRETS_DIR}/balalayka-test.base64.cer`,
    key: `${SECRETS_DIR}/balalayka-test.key`,
};

const CAPTCHA_TOKEN_FILE = `${SECRETS_DIR}/smart_captcha_token.json`;

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
        marketTouch: 'default.market-exp-touch.fslb.yandex.<tld>',
        beru: 'desktop.bluemarket.fslb.beru.<tld>',
        yandex: 'yandex.<tld>',
        static: STATIC_SELF ? null : 'yastatic.net',
        tech: 'tech.yandex.<tld>',
    },

    cspReportUrlEnv: 'testing',

    cspPolicies,

    payoneerProgramId: '100101370',

    tankerHelpUrl: 'https://wiki.yandex-team.ru/Market/frontend/Partner/docs/integration/tanker/',

    // Для серверного доступа
    servant: {
        personal: {
            host: 'personal-market.taxi.tst.yandex.net',
            protocol: 'http',
            data: {
                traceServiceId: 'personal',
            },
        },
        dwh: {
            host: 'mbi-dwh-api.tst.vs.market.yandex.net',
            protocol: 'https:',
            data: {
                traceServiceId: 'dwh',
            },
        },
        integrationNPD: {
            host: 'integration-npd.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'integration_npd',
            },
        },
        B2BMonetization: {
            host: 'b2bmarketmonetization.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'b2b_monetization',
            },
        },
        advContentManager: {
            host: 'adv-content-manager.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'market_adv_content_manager',
            },
        },
        'tvm-daemon': {
            timeout: 300,
        },
        uzedo: {
            host: 'unstable.uzedo.yandex-team.ru',
            path: '/yandex_test',
            protocol: 'https:',
            data: {
                traceServiceId: 'uzedo_unstable',
            },
        },
        banks: {
            host: 'banks-b2b-int.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'market_banks_b2b_int',
            },
        },
        yablog: {
            host: 'hidden-api.common.yandex.ru',
            data: {
                traceServiceId: 'yablog_hidden_api',
            },
        },
        lms: {
            host: 'lms.tst.vs.market.yandex.net',
            protocol: 'http:',
            data: {
                traceServiceId: 'market_lms',
            },
        },
        balalayka: {
            path: '/xmlrpc',
            protocol: 'https:',
            port: 6443,
            host: 'balalayka-test.paysys.yandex-team.ru',
            data: {
                traceServiceId: 'balalayka_test',
            },
        },
        cocon: {
            host: 'cocon.tst.vs.market.yandex.net',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'cocon',
            },
            timeout: 1100,
        },
        analytics: {
            host: 'analytics.tst.vs.market.yandex.net',
            protocol: 'https:',
            data: {
                traceServiceId: 'analytics_platform_api',
            },
        },
        autoorder: {
            host: 'autoorder.tst.vs.market.yandex.net',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'market_autoorder',
            },
        },
        s3mds: {
            host: 's3.mds.yandex.net',
            protocol: 'https:',
            data: {
                traceServiceId: 's3mds',
            },
        },
        staff: {
            host: 'staff-api.test.yandex-team.ru',
            protocol: 'https:',
            data: {
                traceServiceId: 'staff_api',
            },
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
            data: {
                traceServiceId: 'bunker_api',
            },
        },
        tanker: {
            host: 'tanker-api.yandex-team.ru',
            protocol: 'https:',
            data: {
                traceServiceId: 'tanker_api',
            },
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
            data: {
                traceServiceId: 'market_mbi_log_processor',
            },
        },
        mbiPartner: {
            host: 'mbi-partner.tst.vs.market.yandex.net',
            port: 38271,
            data: {
                traceServiceId: 'mbi_partner',
            },
        },
        mbiPartnerEsPartners: {
            protocol: 'http:',
            host: 'mbi-partner-es-campaigns.tst.vs.market.yandex.net',
            port: 9200,
            data: {
                traceServiceId: 'mbi_partner_es_partners',
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
        mbiHealthApi: {
            host: 'mbi-health-api.tst.vs.market.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'mbi_health_api',
            },
        },
        mbiOebsService: {
            host: 'mbi-oebs-service.tst.vs.market.yandex.net',
            port: 80,
        },
        partnerBanners: {
            host: 'partner-banners.tst.vs.market.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'partner_banners',
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
            data: {
                traceServiceId: 'market_checkouter_sand',
            },
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
        aboBpmn: {
            host: 'abo-bpmn.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'market_abo_bpmn',
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
            data: {
                traceServiceId: 'yandex_oauth',
            },
        },
        owCommon: {
            protocol: 'https:',
            host: 'ow.tst.market.yandex-team.ru',
            data: {
                traceServiceId: 'market_ow',
            },
        },
        owCommon2: {
            protocol: 'https:',
            host: 'ow2.tst.market.yandex-team.ru',
            data: {
                traceServiceId: 'market_ow2',
            },
        },
        ow: {
            protocol: 'https:',
            host: 'ow.tst.market.yandex-team.ru',
            path: '/api/tvm',
            data: {
                traceServiceId: 'market_ow_with_tvm',
            },
        },
        ow2: {
            protocol: 'https:',
            host: 'ow2.tst.market.yandex-team.ru',
            path: '/api/tvm',
            data: {
                traceServiceId: 'market_ow2_with_tvm',
            },
        },
        yandexMetrika: {
            protocol: 'https:',
            host: 'api-metrika.yandex.ru',
            port: 443,
            data: {
                traceServiceId: 'yandex_metrika',
            },
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
            data: {
                traceServiceId: 'market_report_sand',
            },
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
            data: {
                traceServiceId: 'market_fulfillment_workflow',
            },
        },
        ff4Shops: {
            protocol: 'https:',
            host: 'ff4shops.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'market_ff4shops',
            },
        },
        nesu: {
            // логистический фасад для Яндекс.Доставки 3.0
            protocol: 'https:',
            host: 'nesu.tst.vs.market.yandex.net',
            port: 443,
            data: {
                traceServiceId: 'market_nesu',
            },
        },
        sanitizer: {
            // Поставляемый Яндекс.Почтой сервис очистки html от потенциально опасного user-generated содержимого
            protocol: 'https:',
            host: 'sanitizer-test.pers.yandex.net',
            port: 443,
            data: {
                traceServiceId: 'sanitizer',
            },
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
            data: {
                traceServiceId: 'market_cataloger',
            },
        },
        persGrade: {
            protocol: 'http:',
            host: 'pers-grade.tst.vs.market.yandex.net',
            port: 35824,
            data: {
                traceServiceId: 'market_pers_grade',
            },
        },
        persQA: {
            protocol: 'http:',
            host: 'pers-qa.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'market_pers_qa',
            },
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
            data: {
                traceServiceId: 'yandex_suggest',
            },
        },
        testBalance: {
            protocol: 'http:',
            host: 'greed-ts.paysys.yandex.ru',
            port: 30702,
            path: '/xmlrpc',
            data: {
                traceServiceId: 'test_balance',
            },
        },
        sberlog: {
            protocol: 'https:',
            host: 'sberlog.tst.vs.market.yandex.net',
            port: 443,
            data: {
                traceServiceId: 'market_sberlog',
            },
        },
        balance: {
            protocol: 'http:',
            host: 'greed-ts.paysys.yandex.ru',
            port: 8002,
            path: '/xmlrpc',
            data: {
                traceServiceId: 'balance',
            },
        },
        mbiPartnerStat: {
            protocol: 'https:',
            host: 'mbi-partner-stat.tst.vs.market.yandex.net',
            port: 443,
            data: {
                traceServiceId: 'market_mbi_partner_stat',
            },
        },
        tarifficator: {
            protocol: 'https:',
            host: 'tarifficator.tst.vs.market.yandex.net',
            port: 443,
        },
        mbiOrderService: {
            protocol: 'http:',
            host: 'mbi-order-service.tst.vs.market.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'market_mbi_order_service',
            },
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
            data: {
                traceServiceId: 'market_autogen_api',
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
        dataCampPictures: {
            port: 80,
            protocol: 'http:',
            host: 'datacamp-pictures.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'dataCampPictures',
            },
        },
        geoSuggest: {
            port: 443,
            protocol: 'https:',
            host: 'suggest-maps-test.n.yandex-team.ru',
            data: {
                traceServiceId: 'yandex_geo_suggest',
            },
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
                traceServiceId: 'market_sorting_center_int',
            },
        },
        priceLabs: {
            protocol: 'https:',
            host: 'pricelabs-api2.tst.vs.market.yandex.net',
            port: 443,
            path: '/api/v1/public',
            data: {
                traceServiceId: 'market_pricelabs_api',
            },
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
            data: {
                traceServiceId: 'market_utilizer',
            },
        },
        refs: {
            host: 'refs-test.paysys.yandex.net',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'refs_test',
            },
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
            data: {
                traceServiceId: 'market_tpl_partner_carrier',
            },
        },
        mbiBpmn: {
            protocol: 'http:',
            host: 'mbi-bpmn.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'market_mbi_bpmn',
            },
        },
        marketBillingApi: {
            protocol: 'https:',
            host: 'market-billing-api.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'market_billing_api',
            },
        },
        partner1p: {
            protocol: 'http',
            host: 'partner-1p.tst.vs.market.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'market_partner_1p',
            },
        },
        mbiPartnerRegistration: {
            protocol: 'http:',
            port: 80,
            host: 'mbi-partner-registration.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'mbi_partner_registration',
            },
        },
        nonAdvPromo: {
            host: 'market-adv-promo.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'adv_promo',
            },
        },
        yadoc: {
            protocol: 'https:',
            path: '/public/api',
            host: 'yadoc-test.mba.yandex-team.ru',
            data: {
                traceServiceId: 'yadoc_test',
            },
        },
        cabinet1p: {
            protocol: 'https:',
            host: 'cabinet1p.tst.market.yandex-team.ru',
            data: {
                traceServiceId: 'market_cabinet1p',
            },
        },
        feedProcessor: {
            protocol: 'http:',
            host: 'feed-processor.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'feed-processor',
            },
        },
        logistics4shops: {
            protocol: 'http:',
            host: 'logistics4shops.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'logistics4shops',
            },
        },
        taxiFeeds: {
            protocol: 'http:',
            host: 'feeds.taxi.tst.yandex.net',
            data: {
                traceServiceId: 'taxi_feeds',
            },
        },
        combinator: {
            protocol: 'http:',
            host: 'combinator.tst.vs.market.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'combinator',
            },
        },
        yard: {
            protocol: 'https:',
            host: 'yard.tst.vs.market.yandex-team.ru',
            port: 443,
            data: {
                traceServiceId: 'yard',
            },
        },
        supportDaas: {
            protocol: 'http:',
            host: 'support.daas-backend.yandex.net',
            data: {
                traceServiceId: 'support-daas',
            },
        },
        smartCaptcha: {
            host: 'captcha-api.yandex.ru',
            data: {
                traceServiceId: 'smart_captcha',
            },
        },
        partnerNotification: {
            protocol: 'https:',
            host: 'partner-notification.tst.vs.market.yandex.net',
            port: 443,
            data: {
                traceServiceId: 'partner-notification',
            },
        },
        communicationProxy: {
            protocol: 'http:',
            host: 'communication-proxy.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'communication-proxy',
            },
        },
        chatterbox: {
            host: 'chatterbox.taxi.yandex.net',
            data: {
                traceServiceId: 'chatterbox',
            },
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
        env: 'market_front_partner-testing-mimino',
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
            id: '29618685',
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
            iframeUrlParams: {config: 'development'},
        },
        SUPPLIER: {
            serviceId: 12,
            guid: '50f63800-62ea-9764-7714-7eb30d1ac242',
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

    bunkerRootNode: '/market-partner',

    cmsPreviewTokenFile,

    mbiPartnerEsPartnersCredsFile,

    smartCaptcha: {
        tokenFile: CAPTCHA_TOKEN_FILE,
        clientKey: 'dbg_fJskLdjs3Jsdd3Sd',
    },
};

if (extEnv) {
    /* eslint-disable global-require */
    const _ = require('lodash');

    _.merge(module.exports, require(`./${extEnv}`));
    /* eslint-enable global-require */
}
