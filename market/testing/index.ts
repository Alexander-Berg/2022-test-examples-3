import {Config} from '@yandex-int/yandex-cfg';
import loggerDeployStream from '@yandex-int/yandex-logger/streams/deploy';
import errorBusterStream from '@yandex-int/yandex-logger/streams/error-booster';
import loggerDefaultPreset from '@yandex-int/yandex-logger/middleware/preset-default';

import {testingPreset} from '../csp';

const config: Config = {
    serviceHost: 'https://supplier-test.market.yandex.ru',

    env: 'testing',

    server: {
        port: process.env.PORT || 80,
        mode: 'cluster',
        workerCount: 8,
    },

    blackbox: {
        api: 'blackbox-mimino.yandex.net',
        retries: 1,
        timeout: 200,
        multisession: 'yes',
        emails: 'getdefault',
        attributes: {
            login: '1008',
            email: '14',
        },
    },

    tvm: {
        serverUrl: 'http://localhost:2',
        destinations: ['blackbox', 'cabinet1p', 'autoorder', 'dataCampWhite'],
        token: process.env.TVMTOOL_LOCAL_AUTHTOKEN as string,
        clientId: 'fps-frontend',
    },

    passport: {
        host: 'https://passport.yandex.ru',
    },

    logger: {
        name: 'fps-frontend',
        fields: {
            environment: process.env.NODE_ENV,
            pid: process.pid,
        },
        streams: [
            {
                level: 'trace',
                stream: loggerDeployStream({
                    stream: process.stdout,
                }),
            },
            {
                level: 'warn',
                stream: errorBusterStream({
                    project: 'fps-frontend-testing',
                    socketType: 'http',
                    socketHost: process.env.ERROR_BOOSTER_HTTP_HOST || 'localhost',
                    socketPort: Number.parseInt(process.env.ERROR_BOOSTER_HTTP_PORT || '12522', 10),
                    socketPath: '/errorbooster',
                    maxTotalSockets: 8,
                }),
            },
        ],
        middleware: loggerDefaultPreset(),
    },

    csp: {
        presets: testingPreset,
        useDefaultReportUri: true,
        project: 'fps-frontend',
    },

    httpUatraits: {
        server: 'http://uatraits-test.qloud.yandex.ru',
        clientOptions: {
            timeout: 100,
        },
    },

    httpGeobase: {
        server: 'http://geobase-test.qloud.yandex.ru',
        clientOptions: {
            timeout: 100,
        },
    },

    httpLangdetect: {
        server: 'http://langdetect-test.qloud.yandex.ru',
        clientOptions: {
            timeout: 100,
        },
        defaultLanguage: 'ru',
    },

    secretKey: {
        version: 2,
        salt: process.env.CSRF_TOKEN_SALT,
    },

    helmet: {
        contentSecurityPolicy: false,
        xssFilter: false,
        crossOriginEmbedderPolicy: false,
        frameguard: false,
    },

    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        project: 'fps-frontend',
        updateInterval: 60000,
        timeout: 3000,
        version: 'latest',
    },

    api: {
        cabinet1p: {
            protocol: 'https',
            hostname: 'cabinet1p.tst.market.yandex-team.ru',
            port: 443,
        },
        supplier1pOld: {
            protocol: 'http',
            hostname: 'partner-marketing.tst.vs.market.yandex.net',
            port: 80,
        },
        captcha: {
            protocol: 'http',
            hostname: 'api.captcha.yandex.net',
            port: 80,
        },
        startrek: {
            protocol: 'https',
            hostname: 'st-api.test.yandex-team.ru',
        },
        autoorder: {
            hostname: 'autoorder.tst.vs.market.yandex.net',
            protocol: 'https',
            port: 443,
        },
        mds: {
            protocol: 'http',
            hostname: 'cabinet1p-tst.s3.mds.yandex.net',
        },
        dataCampWhite: {
            hostname: 'datacamp.white.tst.vs.market.yandex.net',
            protocol: 'http',
            port: 80,
        },
    },

    metrika: {
        scriptSrc: '/metrika-testing.js',
    },
};

module.exports = config;
