const endpoints = require('./endpoints');

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

const SECRETS_DIR = process.env.SECRETS_DIR || '/etc/datasources';

/** @see http://stackoverflow.com/questions/10888610/ignore-invalid-self-signed-ssl-certificate-in-node-js-with-https-request */
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

const MEMCACHED_SERVERS = 'front-cache.tst.vs.market.yandex.net:11226';

module.exports = {
    service: 'market_front_blue_api',
    env: 'testing',

    server: NODE_PORT,

    debug: true,

    skipWarmUp: true,

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

    traceLogger: {
        socket: process.env.TRACE_LOG_SOCK,
    },

    // Для серверного доступа
    servant: endpoints.servant,

    // Для клиентского доступа
    hosts: endpoints.hosts,

    geobase: {
        fallback: {
            timeout: 200,
        },
        native: {
            path: `${__dirname}/../geobase6.conf`,
        },
    },

    startrekComplainQueueName: 'TEST',

    ipregDataPath: '/var/cache/geobase/ipreg-layout.json',

    kadavrAvailable: true,
    kadavrHost: process.env.KADAVR_HOST || 'kadavr.vs.market.yandex.net',
    kadavrPort: process.env.KADAVR_PORT || 80,

    saltFile: `${SECRETS_DIR}/salt.json`,

    bodyParser: {
        urlencoded: {
            limit: '2MB',
            extended: false,
        },
        json: {
            limit: '2MB',
        },
    },

    // platform: api
    handlersDir: './app/handlers',

    tvm: {
        env: 'market_front_blue-testing-testing',
        configPath: '../../tvm-daemon/tvmtool.conf',
        authPath: '../../tvm-daemon/local.auth',
    },

    allowedTvmConsumers: [
        2012138, // search
        2028048,
        2017859, // yandex go eda-shortcuts
        2014194, // yandex go orderhistory
        2013636, // yandex go orderhistory dev
        2023716, // yandex go cargo-c2c
        2016867, // едадил
        2028441, 2028443, // pers-author
        2013864, // taxitvmapiproxytesting
        2014720, 2014716, 2014718, 2014714, // self
        2032740, // mapi
        2026238, // lavka
    ],

    // @TODO Удалить после реализации MARKETFRONTECH-2304
    supportAccountLinks: false,

    errorBooster: {
        project: 'market_front_blue',
        platform: 'api',
    },
};

const extEnv = process.env.EXT_ENV;

if (extEnv) {
    // С ambar.mergeRight работает некорректно
    // eslint-disable-next-line no-restricted-modules, global-require
    const _ = require('lodash');
    // eslint-disable-next-line global-require
    _.merge(module.exports, require(`./${extEnv}`));
}

