const chalk = require('chalk');
const path = require('path');
const servant = require('@yandex-market/market-shared/src/configs/testing/servant');
const hosts = require('@yandex-market/market-shared/src/configs/testing/hosts');
const hostsTld = require('@yandex-market/market-shared/src/configs/testing/hostsTld');
const os = require('os');
const {merge} = require("lodash");

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
 */
const NODE_PORT = process.env.NODE_PORT;

const SECRETS_DIR = process.env.SECRETS_DIR || '/etc/datasources';

const saltFile = `${SECRETS_DIR}/salt.json`;

const MEMCACHED_SERVERS = 'front-cache.tst.vs.market.yandex.net:11226';

module.exports = {
    service: 'touch',
    env: 'testing',
    // дополнительные параметры отправляемые в репортер
    // TODO: исправить параметры репортера
    cspReporterParams: {
        project: 'market_white',
        platform: 'touch',
        env: 'development',
    },
    hosts: {
        ...hosts,
        // добавить свои при необходимости
    },
    hostsTld: {
        ...hostsTld,
        // добавить свои при необходимости
    },

    // Для серверного доступа
    servant: {
        ...servant,
        'template-stout-apiary': {
            host: `${os.hostname()}`,
            port: NODE_PORT,
            protocol: 'https:',
            data: {
                traceServiceId: 'template-stout-apiary',
            },
        },
    },

    tvm: {
        env: 'market-backbone-desktop-testing-mimino',
        configPath: '../../tvm-daemon/tvmtool.conf',
        authPath: '../../tvm-daemon/local.auth',
    },

    server: NODE_PORT,
    // Умолчательное значение для top level domain
    // Который является "домашним" для проекта(Яндекс - Российская компания, по этому ru)
    homeTLD: 'ru',
    solomonMetrics: {
        ttlb: true,
        errors: true,
        backendsRps: true,
        backendsTimings: true,
    },

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

    // Добавляет в ответ debug-информацию (например. об ошибках)
    debug: true,

    tracerType: 'console',
    tracer: {
        format: [
            `${chalk.gray('{{title}}')}\t{{message}}`,
            {
                info: `${chalk.white('{{title}}')}\t{{message}}`,
                error: `${chalk.bold.red('{{title}}')}\t{{message}}`,
                warn: `${chalk.yellow('{{title}}')}\t{{message}}`,
            },
        ],
        dateformat: 'yyyy/mm/dd HH:MM:ss',
        level: 'debug', // Поставьте 'trace' для более подробного вывода
    },
    traceLogger: {
        socket: process.env.TRACE_LOG_SOCK,
    },



    // TODO: выпилить геобазу из мандреля и пуска
    geobase: {
        fallback: {
            timeout: 200,
        },
        native: {
            path: path.join(process.cwd(), 'configs/geobase6.conf'),
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
    experiments: {
        // Имя дефолтного эксперимента, т.е. эталонной версии
        defaultName: 'etalon',
    },
    manifestPaths: {
        default: 'dist/server/browser.manifest.json',
    },
    saltFile
};


const extEnv = process.env.EXT_ENV;

if (extEnv) {
    try {
        const ext_env_overrides = require(`./ext_env_overrides/${extEnv}`);
        merge(module.exports, ext_env_overrides);
    } catch(e){
        console.log(e)
    }
}
