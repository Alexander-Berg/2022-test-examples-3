const path = require('path');

// eslint-disable-next-line no-restricted-modules
const _ = require('lodash');

const cspPolicies = require('./content-security-policy');
const cspPaymentPolicies = require('./content-security-policy-payment');
const setupDevMFHost = require('../development/setupDevMFHost');

/** @see http://stackoverflow.com/questions/10888610/ignore-invalid-self-signed-ssl-certificate-in-node-js-with-https-request */
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
const {NODE_PORT} = process.env;

const STATIC_SELF = Boolean(process.env.STATIC_SELF);

const SECRETS_DIR = process.env.SECRETS_DIR || '/etc/datasources';

const saltFile = `${SECRETS_DIR}/salt.json`;

const awsS3File = `${SECRETS_DIR}/aws-s3.json`;

const MEMCACHED_SERVERS = 'front-cache.tst.vs.market.yandex.net:11226';

module.exports = {
    service: 'core',
    env: 'testing',

    tvm: {
        env: 'market_front_white-testing-mimino',
        configPath: '../../tvm-daemon/tvmtool.conf',
        authPath: '../../tvm-daemon/local.auth',
    },

    // Пока не используется
    // Возможно в будущем будем читать это из package.json
    // И скорее всего эта штука как-то будет фигурировать в чувствительных к версии фронта данных
    version: '1',

    server: NODE_PORT,

    // добавляет в ответ debug-информацию (например. об ошибках)
    debug: true,

    // Умолчательное значение для top level domain
    // Который является "домашним" для проекта(Яндекс - Российская компания, по этому ru)
    homeTLD: 'ru',

    cache: {
        type: 'memcache',
        options: {
            servers: MEMCACHED_SERVERS,
            generation: '2',
            timeouts: {
                connect: 100,
                idle: 10000,
                retries: 2,
                retry: 50,
                reconnect: 1000 * 60 * 5,
                operations: 50,
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
            maxValue: 1024 * 1024 * 4, // 4MB
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

    // значение sk, которое всегда будет проходить валидацию (используем для нагрузочного тестирования)
    sk: '4d4a9485a03224c7347cd0cb74bd1712',

    // для клиентского доступа
    hosts: {
        self: 'market.yandex.ru',
        yandex: 'yandex.ru',
        passport: 'passport-rc.yandex.ru',
        passportBase: 'passport-rc.yandex.',
        csp: 'csp.yandex.net',
    },

    hostsTld: {
        self: 'market.yandex.<tld>',
        yandex: 'yandex.<tld>',
        passport: 'passport-rc.yandex.<tld>',
    },

    // Белый список хостов для формирования заголовка Content Security Policy
    cspPolicies,
    // дополнительные параметры отправляемые в репортер
    cspReporterParams: {
        project: 'market_white',
        platform: 'desktop',
        env: 'testing',
    },
    cspPaymentPolicies,

    // Для серверного доступа
    servant: {

        marketfront: {
            host: 'default.exp.tst.market.yandex.ru',
            protocol: 'http:',
            data: {
                traceServiceId: 'marketfront',
                // isMicroFront: true,
            },
        },
        proxy_marketfront: {
            host: 'default.exp.tst.market.yandex.ru',
            protocol: 'https:',
            data: {
                traceServiceId: 'marketfront',
                // isMicroFront: true,
            },
        },

        passport: {
            // Больше информации по адресу https://wiki.yandex-team.ru/market/verstka/services/passport
            host: 'blackbox-mimino.yandex.net',
            data: {
                traceServiceId: 'passport_blackbox',
                accounts: true,
            },
        },
        geocoder: {
            host: 'addrs-testing.search.yandex.net',
            protocol: 'http:',
            path: '/search/stable/yandsearch',
            data: {
                traceServiceId: 'geocode',
            },
        },

        blackbox: {
            // Хост совпадает с хостом паспорта.
            // В зависимости от параметров вызова берётся тестовый или обычный.
            path: '/blackbox/',
            data: {
                traceServiceId: 'passport_blackbox',
            },
        },
    },

    busboy: {
        limits: {
            files: 1,
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

    templates: {},

    skipWarmUp: false,

    geobase: {
        fallback: {
            timeout: 200,
        },
        native: {
            path: path.join(__dirname, '../geobase6.conf'),
        },
    },

    experiments: {
        // Имя дефолтного эксперимента, т.е. эталонной версии
        defaultName: 'etalon',
    },

    traceLogger: {
        socket: process.env.TRACE_LOG_SOCK,
    },

    saltFile,

    awsS3File,

    isTest: false,

    ipregDataPath: '/var/cache/geobase/ipreg-layout.json',

    abt: {
        contextIds: ['DESKTOP', 'DESKTOP_BLUE', 'MARKET', 'MARKET_BLUE', 'MARKET_ALL'],
        flagsAllowedDir: 'configs/flags_allowed.d',
    },

    errorBooster: {
        project: 'market_front_core',
        platform: 'desktop',
    },
};

const extEnv = process.env.EXT_ENV;

if (extEnv) {
    _.merge(module.exports, require(`./${extEnv}`));
}
