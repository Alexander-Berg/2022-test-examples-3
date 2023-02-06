const path = require('path');

const LOGS_DIR = process.env.LOGS_DIR;

if (!LOGS_DIR) {
    throw new Error('Environment variable LOGS_DIR is not defined or empty.');
}

const logs = path.resolve(LOGS_DIR);

const PLATFORM = process.env.APP_PLATFORM || process.env.PLATFORM;

const LOGS_PREFIX = PLATFORM === 'api' ? 'market_front_touch_api' : 'market_front_touch';

const workerCount = Number(process.env.NODE_WORKERS || 4);

require('@self/platform/app/node_modules/logClusterEvents');


module.exports = {
    app: require.resolve('@self/platform/dist/server/app'),

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
                resource: path.join(logs, `${LOGS_PREFIX}-trace.log`),
                timers: path.join(logs, `${LOGS_PREFIX}-timers.log`),
                errors: path.join(logs, `${LOGS_PREFIX}-errors.log`),
                errorBooster: path.join(logs, 'error-booster.log'),
                fapiRecommendations: path.join(logs, 'fapi-recommendations.log'),
            },
            socket: process.env.TRACE_LOG_SOCK,
        },
        '@yandex-market/luster-messaging': {},
        '@yandex-market/luster-metrics': {
            socket: process.env.LUSTER_METRICS_SOCK,
            workers: workerCount,
            blacklist: ['nodejs.gc.duration.milliseconds', 'backends.requests.status', 'nodejs.memory', 'nodejs.heap_space', 'backends.requests.timings',
                'market.front.robot.rps', 'market.front.rps', 'nodejs.boredom', 'nodejs.render', 'nodejs.errors', 'market.front.ttlb'],
        },
    },
};
