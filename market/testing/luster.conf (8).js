const path = require('path');

const LOGS_DIR = process.env.LOGS_DIR;

if (!LOGS_DIR) {
    throw new Error('Environment variable LOGS_DIR is not defined or empty.');
}

const logs = path.resolve(LOGS_DIR);

const workerCount = Number(process.env.NODE_WORKERS || 2);

// eslint-disable-next-line import/extensions,import/no-unresolved
require('../../lib/app/extensions/logClusterEvents');

module.exports = {
    app: require.resolve('../../lib/app'),

    workers: workerCount,

    control: {
        forkTimeout: 3 * 1000, // 3 sec
        stopTimeout: 20 * 1000, // 20 sec
        // Если за 5 секунд воркер умер более 10 раз - не перезапускать
        exitThreshold: 5 * 1000, // 5 sec
        allowedSequentialDeaths: 10,
        triggerReadyStateManually: true,
    },

    // Расширения luster
    extensions: {
        'luster-log-file': {
            stdout: `${logs}/app.stdout.log`,
            stderr: `${logs}/app.stderr.log`,
        },
        'luster-ctrl': {
            log: 'true',
            port: process.env.LUSTER_CTRL_PORT || 'luster-ctrl.sock',
        },
        'luster-trace-log': {
            markers: {
                resource: path.join(logs, 'market_front_vendors-trace.log'),
                timers: path.join(logs, 'market_front_vendors-timers.log'),
                errors: path.join(logs, 'market_front_vendors-errors.log'),
                errorBooster: path.join(logs, 'error-booster.log'),
            },
            socket: process.env.TRACE_LOG_SOCK,
        },
        '@yandex-market/luster-messaging': {},
    },
};
