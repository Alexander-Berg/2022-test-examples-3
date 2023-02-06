'use strict';

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
const NODE_PORT = process.env.NODE_PORT;

const SECRETS_DIR = process.env.SECRETS_DIR || '/etc/datasources';

/**
 * Переменная окружения вида <addr>:<port>
 * Приложение поддерживает несколько memcached инстансов, но
 * формат в этом случае отличается,
 * сейчас нет необходимости в нескольких инстансах memcached.
 * @type {String}
 */
const MEMCACHED_SERVERS = process.env.MEMCACHED_SERVERS || '127.0.0.1:11389';
const {WEB_ACCESS_TOKEN} = require('../secrets');

module.exports = {
    service: 'marketcorp',
    env: 'testing',

    tvm: {
        env: 'market_front_corpsite-testing-mimino',
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
            capacity: 10000,
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

    // значение sk, которое всегда будет проходить валидацию (используем для нагрузочного тестирования)
    sk: '4d4a9485a03224c7347cd0cb74bd1712',

    // для клиентского доступа
    hosts: {
        static: 'test.yastat.net',
        self: 'marketgroup.tst.vs.market.yandex.net',
        yandex: 'yandex.ru',
        passport: 'passport-rc.yandex.ru',
        authSubdomain: 'pass-rc',
        yandexAuthPassport: 'pass-rc.yandex.ru',
        social: 'social-rc.yandex.ru',
        yandexForms: 'forms.yandex.ru',
        yablog: 'yablogs-api.yandex.net',
    },

    cspReportUrlEnv: 'test',

    // Белый список хостов для формирования заголовка Content Security Policy
    cspPolicies: {
        'default-src': ['\'none\''],
        'script-src': [
            '\'self\'',
            '\'unsafe-eval\'', // require, susanin, etc.
            '<nonce>',

            '*.yandex.ru',
            '*.yandex.ua',
            '*.yandex.net',
            'yandex.ru',
            'yastatic.net',
            'yastat.net',
            'test.yastat.net',

            'yandex.st',
            'betastatic.yastatic.net',
            'social.yandex.ru',
            'social-rc.yandex.ru',
            'social-test.yandex.ru',

            'js-agent.newrelic.com',
            'bam.nr-data.net',
        ],
        'img-src': [
            '\'self\'',
            'data:',

            '*.yandex.ru',
            '*.yandex.net',
            'yandex.ru',

            'mc.yandex.ru',
            'mc.beru.ru',
            'mc.yandex.ua',
            'mc.yandex.by',
            'mc.yandex.kz',
            'mc.yandex.com.tr',
            'mc.yandex.com',
            'mc.webvisor.org',
            'mc.webvisor.com',

            'yandex.st',
            'yastatic.net',
            'betastatic.yastatic.net',
            'yastat.net',
            'test.yastat.net',

            'yandexgaby.hit.gemius.pl',
            'yandexgaua.hit.gemius.pl',
            'www.tns-counter.ru',
            'ar.tns-counter.ru',
            'ad.doubleclick.net',
            'www.google.com',
            'www.google.ru',
            'adservice.google.com',
            'bid.g.doubleclick.net',

            'ads.adfox.ru',
            'ads6.adfox.ru',
            'banners.adfox.ru',
            'matchid.adfox.yandex.ru',
            'fenek.beru.ru',
            'fox.beru.ru',

            'bam.nr-data.net',
            'avatars.mds.yandex.net',
            'via.placeholder.com',
        ],
        'style-src': [
            '\'self\'',
            '\'unsafe-inline\'',
            'blob:',
            'yastatic.net',
            'yandex.st',
            'betastatic.yastatic.net',
            'yastat.net',
            'test.yastat.net',
        ],
        'connect-src': [
            '\'self\'',
            'data:',
            'redmarket.yandex.net',

            '*.yandex.ru',
            'yandex.ru',

            'yandex.st',
            'yastatic.net',
            'yastat.net',
            'test.yastat.net',

            'mc.yandex.ru',
            'mc.yandex.ua',
            'mc.yandex.by',
            'mc.yandex.kz',
            'mc.yandex.com.tr',
            'mc.yandex.com',

            'mail.yandex.ru',

            'social.yandex.ru',
            'social-rc.yandex.ru',
            'social-test.yandex.ru',

            'bam.nr-data.net',
        ],
        'frame-src': [
            '\'self\'',
            'blob:',
            '*.yandex.ru',
            'yandex.ru',

            '*.beru.ru',
            'beru.ru',

            'betastatic.yandex.net',
            'awaps.yandex.net',
            'yandexadexchange.net',
            '*.yandexadexchange.net',
            'yastatic.net',
            'yastat.net',
            'test.yastat.net',

            'kiks.yandex.ru',
            'www.youtube-nocookie.com',
            'www.youtube.com',
        ],
        'font-src': [
            '\'self\'',
            'data:',
            'yastatic.net',
            'dealer.s3.yandex.net',
            'betastatic.yastatic.net',
            'yastat.net',
            'test.yastat.net',
        ],
        'media-src': [
            '*.yandex.net',
            'yandex.st',
            'yastatic.net',
            'yastat.net',
            'test.yastat.net',
        ],
    },

    // Для серверного доступа
    servant: {
        passport: {
            // Больше инфорации по адресу https://wiki.yandex-team.ru/market/verstka/services/passport
            host: 'blackbox-stress.yandex.net',
            data: {
                traceServiceId: 'passport_blackbox',
            },
        },
        passportInternal: {
            host: 'passport-rc-internal.yandex.ru',
            data: {
                traceServiceId: 'passport_blackbox',
            },
        },
        blackbox: {
            /*
             Хост совпадает с хостом паспорта.
             В зависимости от параметров вызова берётся тестовый или обычный
             */
            path: '/blackbox/',
            data: {
                traceServiceId: 'passport_blackbox',
            },
        },
        ichwill: {
            path: '/api-v1/dealer',
            host: 'ichwill-web.tst.vs.market.yandex.net',
            protocol: 'http:',
            maxRetries: 1,
            timeout: 4500,
            data: {
                traceServiceId: 'redmarket-api',
            },
            headers: {
                'access-token': WEB_ACCESS_TOKEN,
            },
        },
        suggestAddress: {
            path: '/suggest-geo',
            host: 'suggest-maps.yandex.ru',
            protocol: 'https:',
            maxRetries: 1,
            timeout: 3000,
            query: {
                search_type: 'addr',
                lang: 'ru_RU',
                countries: 'ru',
                client_id: 'bringly',
                v: 8,
                n: 5,
            },
        },
        postalCodeSearch: {
            path: '/search/wizardsjson',
            host: 'yandex.ru',
            protocol: 'https:',
            maxRetries: 1,
            timeout: 3000,
            query: {
                type: 'postal_codes',
            },
        },
        postalCodeGeocode: {
            path: '/1.x/',
            host: 'geocode-maps.yandex.ru',
            protocol: 'https:',
            maxRetries: 1,
            timeout: 3000,
            query: {
                format: 'json',
            },
        },
        doccenter: {
            path: '/v1/documents/load/bringly',
            host: 'support.daas-backend.locdoc-test.yandex.net',
            protocol: 'http:',
            maxRetries: 1,
            timeout: 3000,
            data: {
                traceServiceId: 'doccenter',
            },
        },
        social: {
            host: 'api.social-test.yandex.ru',
            port: 80,
            data: {
                traceServiceId: 'passport_social-api',
            },
        },
        sberlog: {
            host: 'sberlog.tst.vs.market.yandex.net',
            protocol: 'https:',
            timeout: 1000,
            data: {
                traceServiceId: 'market_sberlog',
            },
        },
        yablog: {
            host: 'yablogs-api.yandex.net',
            protocol: 'http:',
            port: 80,
            timeout: 1000,
            data: {
                traceServiceId: 'market_yablog',
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

    templates: {
        error: __dirname + '/../../desktop.bundles/error/error.yate',
    },

    skipWarmUp: false,

    reloadableTemplates: false,

    geobase: {
        fallback: {
            timeout: 200,
        },
        native: {
            path: `${__dirname}/../geobase6.conf`,
        },
    },

    experiments: {
        // Имя дефолтного эксперимента, т.е. эталонной версии
        defaultName: 'etalon',
    },

    heapdumps: {
        path: '/var/tmp/cores/yandex-market-heapdumps',
        format: '[skubi]_YYYY-MM-DDTHH:mm:ss',
    },

    traceLogger: {
        socket: process.env.TRACE_LOG_SOCK,
    },

    /**
     * Файл с настройками TVM.
     * https://st.yandex-team.ru/CSADMIN-13370
     */
    tvmFile: `${SECRETS_DIR}/market_front_desktop-tvm.json`,

    /**
     * Файл с солью для сберлоги
     * @refs https://github.yandex-team.ru/market/marketplace/pull/2429/files
     */
    saltFile: `${SECRETS_DIR}/salt.json`,

    useMdaRedirect: false,

    ticketGetter: {
        requestInterval: 60 * 1000, // 1 min
        asker: {
            host: 'tvm-api.yandex.net',
            protocol: 'https:',
            port: 443,
            timeout: 3000,
        },
        tvmFile: `${SECRETS_DIR}/market_front_desktop-tvm.json`,
    },

    isTest: false,

    abt: {
        contextIds: ['DESKTOP_RED', 'TOUCH_RED', 'MARKET_RED', 'MARKET_ALL'],
        flagsAllowedDir: 'configs/flags_allowed.d',
    },

    kadavrAvailable: true,
    kadavrHost: process.env.KADAVR_HOST || 'kadavr.tst.vs.market.yandex.net',
    kadavrPort: process.env.KADAVR_PORT || 80,

    ipregDataPath: '/var/cache/geobase/ipreg-layout.json',

    yablog: {
        id: '5da40719aef349dcac73627c',
    },

    errorBooster: {
        project: 'market_front_corpsite',
        platform: 'desktop',
    },
};

const extEnv = process.env.EXT_ENV;

if (extEnv) {
    const _ = require('lodash');
    // eslint-disable-next-line no-undef
    _.merge(module.exports, require(`./${extEnv}`));
}
