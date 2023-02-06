const {mergeRight} = require('ramda');

// http://stackoverflow.com/questions/10888610/ignore-invalid-self-signed-ssl-certificate-in-node-js-with-https-request
process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';

const {
    EXT_ENV,
    NODE_PORT,
    SECRETS_DIR = '/etc/datasources',
    MEMCACHED_SERVERS = 'front-cache.tst.vs.market.yandex.net:11226',
} = process.env;

const config = {
    service: 'b2b-fapi',
    env: 'testing',

    xiva: {
        service: 'market-partner-mobile',
    },

    yaMessenger: {
        SHOP: {
            guid: '4af14900-ce09-9b13-427d-a57a25873bd8',
        },
        SUPPLIER: {
            guid: '50f63800-62ea-9764-7714-7eb30d1ac242',
        },
    },

    handlersDir: './lib/app/handlers',

    server: NODE_PORT && Number.isNaN(NODE_PORT) ? NODE_PORT : {port: NODE_PORT || 1337},

    debug: true,
    tracerType: 'console',
    tracer: {
        format: ['{{title}}\t{{message}}'],
        dateformat: 'yyyy/mm/dd HH:MM:ss',
        level: 'info',
    },

    traceLogger: {
        socket: process.env.TRACE_LOG_SOCK,
    },

    // значение sk, которое всегда будет проходить валидацию
    sk: '4d4a9485a03224c7347cd0cb74bd1712',

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

    hosts: {
        market: 'default.exp.tst.market.yandex.ru',
        marketPartnerInterface: 'partner.market.fslb.yandex.ru',
    },

    servant: {
        passport: {
            host: 'blackbox-mimino.yandex.net',
            data: {
                traceServiceId: 'passport_blackbox',
            },
        },
        mobileValidator: {
            protocol: 'https:',
            host: 'mobilevalidator.tst.vs.market.yandex.net',
            port: 443,
            data: {
                traceServiceId: 'mobileValidator',
            },
        },
        mbiPartner: {
            host: 'mbi-partner.tst.vs.market.yandex.net',
            port: 38271,
            data: {
                traceServiceId: 'mbi_partner',
            },
        },
        nesu: {
            protocol: 'https:',
            host: 'nesu.tst.vs.market.yandex.net',
            port: 443,
            data: {
                traceServiceId: 'nesu',
            },
        },
        geoSuggest: {
            host: 'suggest-maps-test.n.yandex-team.ru',
            data: {
                traceServiceId: 'geoSuggest',
            },
        },
        cataloger: {
            protocol: 'http:',
            host: 'marketcataloger.tst.vs.market.yandex.net',
            port: 29302,
            data: {
                traceServiceId: 'cataloger',
            },
        },
        checkouter: {
            protocol: 'http:',
            host: 'checkouter.tst.vs.market.yandex.net',
            port: 39001,
            data: {
                traceServiceId: 'market_checkouter',
            },
        },
        ff4Shops: {
            protocol: 'https:',
            host: 'ff4shops.tst.vs.market.yandex.net',
            port: 443,
            data: {
                traceServiceId: 'ff4Shops',
            },
        },
        xiva: {
            protocol: 'https:',
            host: 'push-sandbox.yandex.ru',
            port: 443,
            data: {
                traceServiceId: 'xiva',
            },
        },
        datacamp: {
            port: 80,
            protocol: 'http:',
            host: 'datacamp.white.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'datacamp',
            },
        },
        marketIdxSaas: {
            port: 80,
            protocol: 'http:',
            host: 'prestable-market-idx.saas.yandex.net',
            data: {
                traceServiceId: 'marketIdxSaas',
            },
        },
        orderService: {
            port: 80,
            protocol: 'http:',
            host: 'mbi-order-service.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'orderService',
            },
        },
        partnerNotification: {
            port: 443,
            protocol: 'https:',
            host: 'partner-notification.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'partnerNotification',
            },
        },
        operatorWindow: {
            port: 443,
            protocol: 'https:',
            host: 'ow.tst.market.yandex-team.ru',
            path: '/api/tvm',
            data: {
                traceServiceId: 'operatorWindow',
            },
        },
        logistics4shops: {
            protocol: 'http:',
            host: 'logistics4shops.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'logistics4shops',
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

    skipWarmUp: true,

    geobase: {
        fallback: {
            timeout: 200,
        },
        native: {
            path: `${__dirname}/../geobase6.conf`,
        },
    },

    heapdumps: {
        path: '.',
        format: '[b2b-fapi]_YYYY-MM-DDTHH:mm:ss',
    },

    tvm: {
        env: 'market_front_b2b-fapi_testing-mimino',
        configPath: '../../tvm-daemon/tvmtool.conf',
        authPath: '../../tvm-daemon/local.auth',
    },

    saltFile: `${SECRETS_DIR}/salt.json`,
    constants: `${SECRETS_DIR}/constants.json`,

    ipregDataPath: '/var/cache/geobase/layout.json',
};

if (EXT_ENV) {
    // eslint-disable-next-line global-require
    mergeRight(module.exports, require(`./${EXT_ENV}`));
}

module.exports = config;
