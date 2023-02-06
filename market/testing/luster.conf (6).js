const path = require('path');

const LOGS_DIR = process.env.LOGS_DIR;

if (!LOGS_DIR) {
    throw new Error('Environment variable LOGS_DIR is not defined or empty.');
}

const logs = path.resolve(LOGS_DIR);

const workerCount = Number(process.env.NODE_WORKERS || 4);

module.exports = {
    app: require.resolve('../../lib/app'),

    workers: workerCount,

    control: {
        forkTimeout: 3 * 1000, // 3 sec
        stopTimeout: 10 * 1000, // 10 sec
        // Если за 5 секунд воркер умер более 10 раз - не перезапускать
        exitThreshold: 5 * 1000, // 5 sec
        allowedSequentialDeaths: 10,
        triggerReadyStateManually: true,
    },

    // TODO: узнать подробности у @cornholio
    // Расширения luster
    extensions: {
        'luster-log-file': {
            stdout: `${logs}/app.stdout.log`,
            stderr: `${logs}/app.stderr.log`,
        },
        'luster-ctrl': {
            log: 'true',
            timeout: 100 * 1000,
            port: process.env.LUSTER_CTRL_PORT || 'luster-ctrl.sock',
        },
        'luster-trace-log': {
            markers: {
                resource: `${logs}/market_front_partner-trace.log`,
                timers: `${logs}/market_front_partner-timers.log`,
                errors: `${logs}/market_front_partner-errors.log`,
                errorBooster: `${logs}/error-booster.log`,
            },
            socket: process.env.TRACE_LOG_SOCK,
        },
        '@yandex-market/luster-messaging': {},
    },
};
