const merge = require('lodash/merge');

// Вынесли hosts отдельно чтобы не подключать конфиг целиком в configs/route
const hosts = require('./hosts');

// http://stackoverflow.com/questions/10888610/ignore-invalid-self-signed-ssl-certificate-in-node-js-with-https-request
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

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
 * @type{String}
 */
const NODE_PORT = process.env.NODE_PORT;

if (!NODE_PORT) {
    throw new Error('Environment variable NODE_PORT is not defined or empty.');
}

const SECRETS_DIR = process.env.SECRETS_DIR || '/etc/datasources';

const saltFile = `${SECRETS_DIR}/salt.json`;

const MEMCACHED_SERVERS = 'front-cache.tst.vs.market.yandex.net:11226';

module.exports = {
    service: 'vendors',
    env: 'testing',
    isTest: false,

    // Пока не используется
    // Возможно в будущем будем читать это из package.json
    // И скорее всего эта штука как-то будет фигурировать в чувствительных к версии фронта данных
    version: '1',

    server: NODE_PORT,

    // Добавляет в ответ debug-информацию (например. об ошибках)
    debug: true,

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
        format: [
            '{{title}}\t{{message}}',
        ],
        dateformat: 'yyyy/mm/dd HH:MM:ss',
        level: 'info',
    },

    // значение sk, которое всегда будет проходить валидацию
    sk: '4d4a9485a03224c7347cd0cb74bd1712',

    // Для клиентского доступа
    hosts,

    // Для серверного доступа
    servant: {
        analytics: {
            host: 'analytics.tst.vs.market.yandex.net',
            protocol: 'https:',
            data: {
                traceServiceId: 'analytics_platform_api',
            },
        },
        passportMda: {
            host: 'pass-rc.yandex.ru',
            data: {
                traceServiceId: 'passport_mda',
            },
        },
        passport: {
            // Больше информации по адресу https://wiki.yandex-team.ru/market/verstka/services/passport
            host: 'blackbox-mimino.yandex.net',
            data: {
                traceServiceId: 'passport_blackbox',
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
            host: 'marketcataloger.tst.vs.market.yandex.net',
            port: 29302,
            path: '/cataloger/',
            data: {
                traceServiceId: 'market_kgb_cataloger',
            },
        },
        report: {
            host: 'report.tst.vs.market.yandex.net',
            port: 17051,
            data: {
                traceServiceId: 'market_report',
            },
        },
        recommender: {
            host: 'report.tst.vs.market.yandex.net',
            port: 17051,
            data: {
                traceServiceId: 'market_report-int',
            },
        },
        bunker: {
            host: 'bunker-api-dot.yandex.net',
            port: 80,
            version: 'latest',
            data: {
                traceServiceId: 'bunker',
            },
        },
        social: {
            host: 'social-test.yandex.ru',
            port: 80,
            data: {
                traceServiceId: 'passport_social',
            },
        },
        priceLabs: {
            protocol: 'https:',
            host: 'pricelabs-api2.tst.vs.market.yandex.net',
            port: 443,
            path: '/api/v1/public',
            data: {
                traceServiceId: 'price_labs',
            },
        },
        advertisingIncut: {
            host: 'adv-incut.tst.vs.market.yandex.net',
            port: 80,
            path: '/api/v1',
            data: {
                traceServiceId: 'advertising_incut',
            },
        },
    },

    busboy: {
        limits: {
            files: 1,
            fileSize: 1024 * 1024 * 40, // 40MB
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

    heapdumps: {
        path: '/var/tmp/cores/yandex-market-heapdumps',
        format: '[vendors]_YYYY-MM-DDTHH:mm:ss',
    },

    traceLogger: {
        socket: process.env.TRACE_LOG_SOCK,
    },

    tvm: {
        env: 'market_front_vendors-testing-mimino',
        configPath: '../../tvm-daemon/tvmtool.conf',
        authPath: '../../tvm-daemon/local.auth',
    },

    saltFile,

    kadavrAvailable: true,
    kadavrHost: process.env.KADAVR_HOST || 'kadavr.vs.market.yandex.net',
    kadavrPort: process.env.KADAVR_PORT || 80,

    ipregDataPath: '/var/cache/geobase/ipreg-layout.json',

    errorBooster: {
        project: 'market_front_vendors',
        platform: 'desktop',
    },
};

const extEnv = process.env.EXT_ENV;

if (extEnv) {
    // eslint-disable-next-line global-require
    merge(module.exports, require(`./${extEnv}`));
}

// подмешиваем настройки политики безопасности
merge(module.exports, require('./csp.conf'));
