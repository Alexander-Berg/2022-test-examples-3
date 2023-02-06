'use strict';

const path = require('path');

const LOGS_DIR = process.env.LOGS_DIR;

if (!LOGS_DIR) {
    throw new Error('Environment variable LOGS_DIR is not defined or empty.');
}

const logs = path.resolve(LOGS_DIR);

// eslint-disable-next-line import/no-unresolved, import/extensions
require('../../app/main/server/helpers/logClusterEvents');

const workerCount = Number(process.env.NODE_WORKERS || 8);

module.exports = {
    app: require.resolve('../../start.app'),

    workers: workerCount,

    control: {
        forkTimeout: 3 * 1000, // 3 сек.
        stopTimeout: 20 * 1000, // 20 сек.
        /**
         * Если за 5 секунд воркер умер более 10 раз — не перезапускать.
         */
        exitThreshold: 5 * 1000, // 5 сек.
        allowedSequentialDeaths: 10,
        triggerReadyStateManually: true,
    },

    /**
     * Расширения для luster.
     */
    extensions: {
        'luster-log-file': {
            stdout: `${logs}/app.stdout.log`,
            stderr: `${logs}/app.stderr.log`,
        },
        'luster-ctrl': {
            log: 'true',
            timeout: 250 * 1000,
            port: process.env.LUSTER_CTRL_PORT || 'luster-ctrl.sock',
        },
        'luster-trace-log': {
            markers: {
                resource: `${logs}/market_front_affiliate-trace.log`,
                timers: `${logs}/market_front_affiliate-timers.log`,
                errors: `${logs}/market_front_affiliate-errors.log`,
                errorBooster: `${logs}/error-booster.log`,
            },
            socket: process.env.TRACE_LOG_SOCK,
        },
        '@yandex-market/luster-messaging': {},
    },
};
