/* eslint-disable global-require */

'use strict';

const {mergeDeepRight} = require('ramda');

const {METRIKA_TEST_COUNTER_ID} = require('../constants');
const {cspExtensionPolicies, cspWebEmbedPolicies, cspWebLocalPolicies} = require('./cspPolicies');

/**
 * @see http://stackoverflow.com/questions/10888610/ignore-invalid-self-signed-ssl-certificate-in-node-js-with-https-request
 */
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

/**
 * Порт для прослушивания нашим приложением.
 * Может быть обычным числовым значением или указывать на путь
 * по файловой системе сервера (в случае использования UNIX Socket).
 *
 * Например:
 *  - 1337
 *  - /var/run/yandex-service/ololo.sock
 */
const NODE_PORT = process.env.NODE_PORT;

if (!NODE_PORT) {
    throw new Error('Environment variable NODE_PORT is not defined or empty.');
}

const STATIC_SELF = Boolean(process.env.STATIC_SELF);

const SECRETS_DIR = process.env.SECRETS_DIR || '/etc/datasources';

const saltFile = `${SECRETS_DIR}/salt.json`;

const MEMCACHED_SERVERS = 'front-cache.tst.vs.market.yandex.net:11226';

module.exports = {
    env: 'testing',

    service: 'affiliate',

    /**
     * @todo
     * @see https://st.yandex-team.ru/MARKETAFFILIATE-5189
     *
     * Пока не используется.
     * Возможно в будущем будем читать значение из `package.json`,
     * и скорее всего эта штука будет как-то фигурировать в чувствительных к версии фронта данных.
     */
    version: '1',

    server: NODE_PORT,

    /**
     * Добавляет в ответ debug-информацию (например, об ошибках).
     */
    debug: true,

    cspExtensionPolicies,
    cspWebEmbedPolicies,
    cspWebLocalPolicies,
    cspReporterParams: {
        project: 'market_affiliate',
        env: 'testing',
    },

    /**
     * Значение по умолчанию для Top Level Domain (TLD),
     * который является «домашним» для проекта (Яндекс — российская компания, поэтому `ru`).
     */
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
    },

    tracerType: 'console',
    tracer: {
        format: ['{{title}}\t{{message}}'],
        dateformat: 'yyyy/mm/dd HH:MM:ss',
        level: 'info',
    },

    /**
     * Значение sk, которое всегда будет проходить валидацию (используется для нагрузочного тестирования).
     */
    sk: '4d4a9485a03224c7347cd0cb74bd1712',

    /**
     * @todo: Почистить.
     * @see https://st.yandex-team.ru/MARKETAFFILIATE-5190.
     *
     * Для клиентского доступа.
     */
    hosts: {
        self: 'market.yandex.ru',
        yandex: 'yandex.ru',
        passport: 'passport-rc.yandex.ru',
        social: 'social-test.yandex.ru',
        clickDaemon: 'market-click2-testing.yandex.ru',
        suggest: 'yandex.ru',
        static: STATIC_SELF ? null : 'yastatic.net',
        tune: 'tune.yandex.ru',
        phoneValidator: 'phone-passport-test.yandex.ru',
        emailValidator: 'validator.yandex.ru',
        avatarsHost: 'avatars.mds.yandex.net',
        staticMaps: 'static-maps.yandex.ru',
        apiMaps: 'api-maps.yandex.ru',
        returnsPromo: 'yandex.ru/vozvrat',
        priceChartGen: 'pricechart-gen.yandex.net',
        money: 'money.yandex.ru',
        feedback: 'bug.yandex.ru',
    },

    /**
     * @todo: Почистить.
     * @see https://st.yandex-team.ru/MARKETAFFILIATE-5190.
     */
    hostsTld: {
        self: 'market.yandex.<tld>',
        yandex: 'yandex.<tld>',
        passport: 'passport-rc.yandex.<tld>',
        social: 'social-rc.yandex.<tld>',
        socialTest: 'social-test.yandex.<tld>',
        phoneValidator: 'phone-passport.yandex.<tld>',
        phoneValidatorTest: 'phone-passport-test.yandex.<tld>',
        emailValidator: 'validator.yandex.<tld>',
        emailValidatorTest: 'validator-test.yandex.<tld>',
        tune: 'tune-rc.yandex.<tld>',
        tuneTest: 'tune-test.yandex.<tld>',
        avatarsHost: 'avatars.mds.yandex.net',

        clck: 'clck.yandex.<tld>',
        export: 'export.yandex.<tld>',
        suggest: 'yandex.<tld>',
        static: STATIC_SELF ? null : 'yastatic.net',
        staticMaps: 'static-maps.yandex.ru',
        apiMaps: 'api-maps.yandex.ru',
    },

    /**
     * Для серверного доступа.
     */
    servant: {
        affiliatePromo: {
            protocol: 'http:',
            host: 'affiliate-promo.tst.vs.market.yandex.net',
            port: 80,
            path: '/api/v1/',
            data: {
                traceServiceId: 'market_affiliate_promo',
            },
        },
        apiReport: {
            protocol: 'http:',
            host: 'report.tst.vs.market.yandex.net',
            port: 17051,
            path: '/yandsearch',
            data: {
                traceServiceId: 'market_report',
            },
        },
        blackbox: {
            /**
             * Хост совпадает с хостом Паспорта.
             * В зависимости от параметров окружения хост берётся тестовый или обычный.
             */
            path: '/blackbox',
            data: {
                traceServiceId: 'passport_blackbox',
            },
        },
        bunker: {
            host: 'bunker-api-dot.yandex.net',
            port: 80,
            path: '/v1',
            version: 'latest',
            data: {
                traceServiceId: 'bunker_api',
            },
        },
        carter: {
            protocol: 'http:',
            host: 'carter.tst.vs.market.yandex.net',
            port: 35803,
            data: {
                traceServiceId: 'market_carter',
            },
        },
        contentApi: {
            protocol: 'https:',
            host: 'content-api.tst.vs.market.yandex.net',
            port: 443,
            secretPath: `${SECRETS_DIR}/market_front_affiliate-content_api.json`,
            data: {
                traceServiceId: 'market_content_api',
            },
        },
        distribution: {
            protocol: 'https:',
            host: 'distribution-test.yandex.net',
            port: 443,
            path: '/api/v2',
            data: {
                traceServiceId: 'yandex_distribution',
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
        marketS3: {
            protocol: 'https:',
            host: 's3.mds.yandex.net',
            port: 443,
        },
        /**
         * У саджестов Маркета нет ручки в тестинге.
         */
        marketSuggest: {
            protocol: 'https:',
            host: 'yandex.ru',
            port: 443,
            path: '/suggest-market',
            data: {
                traceServiceId: 'suggest-market',
            },
        },
        marketUtils: {
            protocol: 'http:',
            host: 'market-utils.tst.vs.market.yandex.net',
            port: 35826,
            data: {
                traceServiceId: 'market_utils',
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
        nesuApi: {
            protocol: 'https:',
            host: 'nesu.tst.vs.market.yandex.net',
            port: 443,
            path: '/api',
            data: {
                traceServiceId: 'nesu_api',
            },
        },
        passport: {
            /**
             * Больше информации:
             * @see https://wiki.yandex-team.ru/market/verstka/services/passport
             */
            protocol: 'http:',
            host: 'pass-stress-m1.sezam.yandex.net', // Для нагрузочных стрельб.
            port: 80,
            data: {
                traceServiceId: 'passport_blackbox',
            },
        },
        persBasket: {
            protocol: 'http:',
            host: 'pers-basket.tst.vs.market.yandex.net',
            port: 34510,
            path: '/widget/items',
            data: {
                traceServiceId: 'market_pers_basket',
            },
        },
        priceChart: {
            protocol: 'http:',
            host: 'pricechart-info.tst.vs.market.yandex.net',
            port: 34531,
            path: '/api',
            data: {
                traceServiceId: 'market_abo_pricechart',
            },
        },
        persStatic: {
            protocol: 'http:',
            host: 'pers-static.tst.vs.market.yandex.net',
            port: 34522,
            path: '/api',
            data: {
                traceServiceId: 'market_pers_static',
            },
        },
        s3: {
            protocol: 'https:',
            host: 'market-affiliate.s3.mds.yandex.net',
            port: 443,
        },
        sberlog: {
            protocol: 'https:',
            host: 'sberlog.tst.vs.market.yandex.net',
            port: 443,
        },
        social: {
            protocol: 'http:',
            host: 'api.social-test.yandex.ru',
            port: 80,
            data: {
                traceServiceId: 'passport_social-api',
            },
        },
        yandexClicker: {
            protocol: 'https:',
            host: 'api.ya.cc',
            port: 443,
            data: {
                traceServiceId: 'yandex_clicker',
            },
        },
        yandexCollections: {
            protocol: 'https:',
            host: 'l7test.yandex.ru',
            port: 443,
            path: '/collections',
            data: {
                traceServiceId: 'yandex_collections',
            },
        },
    },

    busboy: {
        limits: {
            files: 1,
            fileSize: 16777216, // 16 MB.
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
        native: {
            path: `${__dirname}/../geobase6.conf`,
        },
    },

    experiments: {
        /**
         * Имя дефолтного эксперимента, т.е. эталонной версии.
         */
        defaultName: 'etalon',
    },

    /**
     * Идентификатор Маркета на сервисе получения
     * фидбека от пользователей aka "жучок".
     *
     * @see https://wiki.yandex-team.ru/bug/
     * @see https://st.yandex-team.ru/TOOLSUP-12999
     */
    feedbackStandId: 'market_desktop',

    heapdumps: {
        path: '/var/tmp/cores/yandex-market-heapdumps',
        format: '[skubi]_YYYY-MM-DDTHH:mm:ss',
    },

    traceLogger: {
        socket: process.env.TRACE_LOG_SOCK,
    },

    /**
     * Новая реализация для TVM.
     */
    tvm: {
        env: 'market_front_affiliate-testing-stress',
        configPath: '../../tvm-daemon/tvmtool.conf',
        authPath: '../../tvm-daemon/local.auth',
    },

    saltFile,

    isTest: false,

    abt: {
        contextIds: ['AFFILIATE'],
        flagsAllowed: require('../flags_allowed.json'),
    },

    metrika: {
        partnerWidgetsCounterId: METRIKA_TEST_COUNTER_ID,
        toolkitWidgetsCounterId: METRIKA_TEST_COUNTER_ID,
        extensionCounterId: METRIKA_TEST_COUNTER_ID,
    },

    ipregDataPath: '/var/cache/geobase/ipreg-layout.json',

    errorBooster: {
        project: 'market_front_affiliate',
        platform: 'desktop',
    },
};

/**
 * Расширяемые конфигурации.
 * @see https://st.yandex-team.ru/MARKETVERSTKA-15009
 */
const extEnv = process.env.EXT_ENV;

if (extEnv) {
    // eslint-disable-next-line no-undef
    module.exports = mergeDeepRight(module.exports, require(`./${extEnv}`));
}

/* eslint-enable global-require */
