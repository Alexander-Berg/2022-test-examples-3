const path = require('path');
// eslint-disable-next-line no-restricted-modules
const _ = require('lodash');
const cspPolicies = require('./content-security-policy');
const cspPaymentPolicies = require('./content-security-policy-payment');

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

const STATIC_SELF = Boolean(process.env.STATIC_SELF);

const SECRETS_DIR = process.env.SECRETS_DIR || '/etc/datasources';

const saltFile = `${SECRETS_DIR}/salt.json`;

const MEMCACHED_SERVERS = 'front-cache.tst.vs.market.yandex.net:11226';

module.exports = {
    service: 'desktop',
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
        social: 'social-rc.yandex.ru',
        clickDaemon: 'market-click2-testing.yandex.ru',
        suggest: 'yandex.ru',
        static: STATIC_SELF ? null : 'yastatic.net',
        tune: 'tune.yandex.ru',
        phoneValidator: 'phone-passport-test.yandex.ru',
        emailValidator: 'validator.yandex.ru',
        avatarsHost: 'avatars.mds.yandex.net',
        photosStorage: 'avatars.mdst.yandex.net', // хост тестовый, чтобы не засорять фотками боевую аватарницу
        staticMaps: 'static-maps.yandex.ru',
        suggestMaps: 'suggest-maps.yandex.ru',
        currentAvatarsHost: 'avatars.mdst.yandex.net',
        apiMaps: 'api-maps.yandex.ru',
        returnsPromo: 'yandex.ru/vozvrat',
        priceChartGen: 'pricechart-gen.yandex.net',
        money: 'money.yandex.ru',
        mail: 'mail.yandex.ru',
        csp: 'csp.yandex.net',
        ugc: 'ugc-test.search.yandex.net',
        exportStatic: 'betastatic.yandex.net',
        forms: 'forms.yandex.ru',
        sovetnik: 'sovetnik.yandex.ru',
        // Первый продакшен хост Пифии @see -> https://wiki.yandex-team.ru/pythia/rukovodstvo-po-api/
        pythia: 'yandex.ru',
        // Второй продакшен хост Пифии @see -> https://wiki.yandex-team.ru/pythia/rukovodstvo-po-api/
        odnaco: 'odna.co',
        beru: 'desktop.pokupki.fslb.market.yandex.ru',
        mbo: 'mbo.market.yandex.ru',
        yablog: 'yablogs-api-hidden.yandex.net',
        clck: 'clck.ru',
        checkout: 'desktop.checkout.market.fslb.yandex.ru',
        cart: 'desktop.pokupki.fslb.market.yandex.ru',
        staff: 'staff-api.test.yandex-team.ru',
        /**
         * @expFlag all_electronic-recipe
         * @ticket MARKETFRONT-69901
         * @start
         */
        checkErxService: 'check-erx-service.tst.vs.market.yandex.net',
        medicata: 'my.test.e-rx.ru',
        /**
         * @expFlag all_electronic-recipe
         * @ticket MARKETFRONT-69901
         * @end
         */
        telemost: 'telemost.yandex.ru',
        yandex_music: 'music.mt.yandex.ru', // работает только с passport-test
        yandexHelp: 'helpnearby.taxi.tst.yandex.ru',
        yandexHelpLanding: 'helpnearby.taxi.tst.yandex.ru/lpc/action_test/roundup_market/index',
        yaplus: 'plus.yandex.ru',
        trustApi: 'trust-test.yandex.ru',
        creditBroker: 'credit-broker.tst.vs.market.yandex.net',
        otrace: 'http://active.idxapi.tst.vs.market.yandex.net:29334/v1/otrace',
        lms: 'https://lms-admin.tst.market.yandex-team.ru',
        wiki: 'https://wiki.yandex-team.ru',
        adminMarketLoyalty: 'admin.market-loyalty.tst.market.yandex-team.ru',
        eatsRetailIntegration: 'eats-retail-market-integration.eda.tst.yandex.net',
        eatsAuthproxy: 'eats-authproxy.eda.tst.yandex.net',
        ycombo: 'ycombo.tst.vs.market.yandex.net',
        // сервис Еда
        eatsCheckout: 'https://testing.eda.tst.yandex.ru/market/checkout',
        eatsOrder: 'https://testing.eda.tst.yandex.ru/market/tracking',
        paymentWidget: 'https://payment-widget.ott.yandex.ru',
        personal: 'http://personal-market.taxi.tst.yandex.net',
        kotopes: 'kotopes.tst.vs.market.yandex.net',
    },

    hostsTld: {
        self: 'market.yandex.<tld>',
        yandex: 'yandex.<tld>',
        passport: 'passport-rc.yandex.<tld>',
        social: 'social-rc.yandex.<tld>',
        socialTest: 'social-test.yandex.<tld>',
        phoneValidator: 'phone-passport.yandex.<tld>',
        phoneValidatorTest: 'phone-passport-test.yandex.<tld>',
        emailValidator: 'validator.yandex.<tld>',
        emailValidatorTest: 'validator-test.yandex.<tld>',
        tune: 'tune-rc.yandex.<tld>',
        tuneTest: 'tune-test.yandex.<tld>',
        avatarsHost: 'avatars.mds.yandex.net',
        photosStorage: 'avatars.mdst.yandex.net',

        export: 'export.yandex.<tld>',
        suggest: 'yandex.<tld>',
        static: STATIC_SELF ? null : 'yastatic.net',
        clickDaemon: 'market-click2-testing.yandex.<tld>',

        apiMaps: 'api-maps.yandex.<tld>',
        suggestMaps: 'suggest-maps.yandex.<tld>',
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
        apple: {
            host: 'itunes.apple.com',
            port: 443,
            protocol: 'https:',
        },
        lavka: {
            host: 'grocery-market-gw.lavka.tst.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'lavka',
            },
        },
        lavkaAuthproxy: {
            host: 'grocery-authproxy.lavka.tst.yandex.net',
            port: 443,
            protocol: 'https:',
            data: {
                traceServiceId: 'lavka_authproxy',
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
        serpSearchWizard: {
            host: 'yandex.ru',
            path: '/search/wizardsjson', // с завершающим слэшом не работает!
            data: {
                traceServiceId: 'market_serp-search-wizard',
            },
        },
        vhUgc: {
            protocol: 'https:',
            host: 'vh.test.yandex.ru',
            port: 443,
            data: {
                traceServiceId: 'vhUgs',
            },
        },
        yaSMS: {
            host: 'phone-passport-test.yandex.ru',
            protocol: 'http:',
            data: {
                traceServiceId: 'passport_sms',
            },
        },
        shopInfo: {
            host: 'shopinfo.tst.vs.market.yandex.net',
            port: 38110,
            path: '/',
            protocol: 'http:',
            data: {
                traceServiceId: 'market_mbi_shopinfo',
            },
        },
        social: {
            host: 'api.social-test.yandex.ru',
            port: 80,
            consumer: 'market_front_white-testing-mimino',
            data: {
                traceServiceId: 'passport_social-api',
            },
        },
        cataloger: {
            host: 'marketcataloger.tst.vs.market.yandex.net',
            port: 29302,
            path: '/cataloger/',
            data: {
                traceServiceId: 'market_kgb_cataloger',
            },
        },
        report: {
            host: 'report.tst.vs.market.yandex.net',
            port: 17051,
            changeable: true, // возможность переопределять параметры вызова через query или cookie
            changeableParams: [
                'allow-collapsing',
                'credit-template-id',
                'dynamic-filters',
                'how',
                'no-random',
                'nocache',
                'promo-check-min-price',
                'rearr-factors',
                'timeout',
                'touch',
                'waitall',
            ],
            data: {
                traceServiceId: 'market_report',
                overrides: [
                    {
                        matchPageHosts: [
                            'marketfront-47826',
                        ],
                        newServantHost: 'rw.vs.market.yandex.net:80',
                    },
                ],
            },
        },
        contentStorage: {
            host: 'market-content-storage.tst.vs.market.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'content_storage',
            },
            changeable: true, // возможность переопределять параметры вызова через query или cookie
        },
        cpaNotifications: {
            host: 'market-utils.tst.vs.market.yandex.net',
            port: 35826,
            data: {
                traceServiceId: 'market_utils',
            },
        },
        notifications: {
            host: 'market-utils.tst.vs.market.yandex.net',
            port: 35826,
            path: '/api/settings/',
            data: {
                traceServiceId: 'market_utils',
            },
        },
        notificationsXml: {
            host: 'market-utils.tst.vs.market.yandex.net',
            port: 35826,
            data: {
                traceServiceId: 'market_utils',
            },
        },
        history: {
            host: 'pers-history.tst.vs.market.yandex.net',
            port: 38602,
            path: '/history/',
            data: {
                traceServiceId: 'market_pers_history',
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
        aboPublic: {
            host: 'abo-public.tst.vs.market.yandex.net',
            port: 38902,
            data: {
                traceServiceId: 'market_abo',
            },
        },
        userStorage: {
            host: 'pers-grade.tst.vs.market.yandex.net',
            port: 35824,
            data: {
                traceServiceId: 'market_pers_grade',
            },
        },
        buker: {
            host: 'marketbuker.tst.vs.market.yandex.net',
            port: 29310,
            data: {
                traceServiceId: 'market_kgb_buker',
            },
        },
        tarantino: {
            host: 'tarantino.tst.vs.market.yandex.net',
            port: 29328,
            path: '/tarantino/',
            data: {
                traceServiceId: 'market_kgb_tarantino',
            },
        },
        templator: {
            host: 'templator.tst.vs.market.yandex.net',
            port: 29338,
            data: {
                traceServiceId: 'market_kgb_templator',
            },
            contentPreviewOverrides: {
                productionPreview: {
                    host: 'templator.vs.market.yandex.net',
                    port: 29338,
                },
                query: {
                    templator_content: 'preview',
                },
            },
        },
        fenek: {
            host: 'ads6.adfox.ru',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'market_banners_adfox',
            },
        },
        spellchecker: {
            host: 'erratum-test.yandex.ru',
            port: 19036,
            data: {
                traceServiceId: 'spell-checker',
            },
        },
        tires: {
            host: 'guruass.tst.vs.market.yandex.net',
            port: 29327,
            data: {
                traceServiceId: 'market_kgb_guruass',
            },
        },
        subscription: {
            host: 'market-utils.tst.vs.market.yandex.net',
            port: 35826,
            data: {
                traceServiceId: 'market_utils',
            },
        },
        wishlist: {
            host: 'pers-basket.tst.vs.market.yandex.net',
            port: 34510,
            data: {
                traceServiceId: 'market_pers_basket',
            },
        },
        adfox: {
            host: 'ads6.adfox.ru',
            protocol: 'https:',
            port: 443,
            headers: {
                host: 'ads.adfox.ru',
            },
            data: {
                traceServiceId: 'adfox',
            },
        },
        experiments: {
            host: 'market.fslb01ht.yandex.ru',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'market_front_desktop',
            },
        },
        dataSyncAddresses: {
            host: 'api-mimino01h.dst.yandex.net',
            port: 8080,
            timeout: 1000,
            protocol: 'http:',
            data: {
                traceServiceId: 'disk_datasync',
            },
        },
        priceChart: {
            host: 'pricechart-info.tst.vs.market.yandex.net',
            path: '/api/',
            protocol: 'http:',
            port: 34531,
            timeout: 500,
            data: {
                traceServiceId: 'market_abo_pricechart',
            },
        },
        reviews: {
            host: 'pers-static.tst.vs.market.yandex.net',
            port: 34522,
            data: {
                traceServiceId: 'market_pers_static',
            },
        },
        persGrade: {
            host: 'pers-grade.tst.vs.market.yandex.net',
            port: 35824,
            data: {
                traceServiceId: 'market_pers_grade',
            },
        },
        persGradeSrc: {
            host: 'pers-grade.tst.vs.market.yandex.net',
            port: 35824,
            data: {
                traceServiceId: 'market_pers_grade',
            },
        },
        persAuthor: {
            protocol: 'https:',
            host: 'pers-author.tst.vs.market.yandex.net',
            port: 443,
            data: {
                traceServiceId: 'persAuthor',
            },
        },
        persAddress: {
            host: 'pers-address.tst.vs.market.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'pers_address',
            },
        },
        persGradePusher: {
            host: 'admin.pers.tst.market.yandex.ru',
            port: 443,
            protocol: 'https:',
            path: '/api/grade/pusher/',
            data: {
                traceServiceId: 'market_pers_grade_pusher',
            },
        },
        gradesDistribution: {
            host: 'pers-static.tst.vs.market.yandex.net',
            port: 34522,
            data: {
                traceServiceId: 'market_pers_static',
            },
        },
        mboLite: {
            host: 'mbo-lite.tst.vs.market.yandex.net',
            port: 33402,
            data: {
                traceServiceId: 'market_mbo_lite',
            },
        },
        cms: {
            host: 'mbo-cms-api.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'market_cms',
            },
        },
        clickDaemon: {
            host: 'market-click2-testing.yandex.ru',
            port: 443,
            protocol: 'https:',
            data: {
                traceServiceId: 'market-click2',
            },
        },
        yandexClickDaemon: {
            host: 'yandex.ru',
            path: '/clck',
            protocol: 'https:',
            data: {
                traceServiceId: 'yandex-clickDaemon',
            },
        },
        currencies: {
            host: 'gravicapa01ht.market.yandex.net',
            path: '/market-corba/currency_rates.xml',
            protocol: 'http:',
            data: {
                traceServiceId: 'market_mmbackends',
            },
        },
        comparison: {
            host: 'pers-comparison.tst.vs.market.yandex.net',
            port: 80,
            path: '/api/comparison/',
            data: {
                traceServiceId: 'pers_comparison',
            },
        },
        avatars: {
            host: 'avatars-int.mdst.yandex.net',
            port: 13000,
            protocol: 'http:',
            data: {
                traceServiceId: 'mds_avatars',
            },
        },
        comments: {
            host: 'market-utils.tst.vs.market.yandex.net',
            port: 35826,
            path: '/api/comment/',
            data: {
                traceServiceId: 'market_utils',
            },
        },
        ugc: {
            host: 'ugcdaemon.tst.vs.market.yandex.net',
            port: 13001,
            data: {
                traceServiceId: 'ugc',
            },
        },
        sberlog: {
            host: 'sberlog.tst.vs.market.yandex.net',
            port: 443,
            protocol: 'https:',
            data: {
                traceServiceId: 'sberlog',
            },
        },
        quiz: {
            host: 'market-loyalty.tst.vs.market.yandex.net',
            port: 35815,
            path: '/quiz/',
            data: {
                traceServiceId: 'quiz',
            },
        },
        loyalty: {
            host: 'market-loyalty.tst.vs.market.yandex.net',
            port: 35815,
            data: {
                traceServiceId: 'market_loyalty',
            },
        },
        /**
         * API конструктор форм.
         *
         * @see -> {https://wiki.yandex-team.ru/forms/yandex.tech-api/}
         */
        searchFeedback: {
            host: 'forms-ext-api.test.yandex.ru',
            protocol: 'https:',
            family: '6',
            path: '/v1/surveys',
            port: 443,
            data: {
                traceServiceId: 'searchFeedback',
            },
        },
        staff: {
            host: 'staff-api.test.yandex-team.ru',
            path: '/v3',
            data: {
                traceServiceId: 'staff',
            },
        },
        /**
         * @expFlag all_electronic-recipe
         * @ticket MARKETFRONT-69901
         * @start
         */
        checkErxService: {
            host: 'check-erx-service.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'check-erx-service',
            },
        },
        medicata: {
            host: 'my.test.e-rx.ru',
        },
        /**
         * @expFlag all_electronic-recipe
         * @ticket MARKETFRONT-69901
         * @end
         */
        startrek: {
            host: 'st-api.yandex-team.ru',
            protocol: 'https:',
            path: '/v2',
            port: 443,
            token: '',
            data: {
                traceServiceId: 'startrek',
            },
        },
        bnpl: {
            host: 'bnpl.fintech.tst.yandex.net',
            protocol: 'http:',
            port: 80,
            data: {
                traceServiceId: 'bnpl',
            },
        },
        creditBroker: {
            host: 'credit-broker.tst.vs.market.yandex.net',
            path: '/credit-broker/v1',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'creditBroker',
            },
        },
        sovetnik: {
            host: 'test.sovetnik.market.yandex.ru',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'sovetnik',
            },
        },
        dyno: {
            host: 'dyno.tst.vs.market.yandex.net',
            protocol: 'http:',
            port: 80,
            data: {
                traceServiceId: 'dyno-reviews',
            },
        },
        sovetnikML: {
            host: 'sovetnik-ml.vs.market.yandex.net',
            protocol: 'http:',
            port: 80,
            data: {
                traceServiceId: 'sovetnikML',
            },
        },
        mars: {
            host: 'mars.tst.vs.market.yandex.net',
            protocol: 'http:',
            port: 80,
            data: {
                traceServiceId: 'mars',
            },
        },
        articleViews: {
            host: 'pers-history.tst.vs.market.yandex.net',
            port: 38602,
            path: '/views/article',
            data: {
                traceServiceId: 'market_pers_article_views',
            },
        },
        pythia: {
            host: 'yandex.ru',
            protocol: 'https:',
            path: '/poll/api/v0/survey/',
            port: 443,
            data: {
                traceServiceId: 'pythia',
            },
        },
        questionsAnswers: {
            host: 'pers-qa.tst.vs.market.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'questionsAnswers',
            },
        },
        articleVotes: {
            host: 'pers-qa.tst.vs.market.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'articleVotes',
            },
        },
        versus: {
            host: 'pers-qa.tst.vs.market.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'versus',
            },
        },
        persQA: {
            host: 'pers-qa.tst.vs.market.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'pers-qa',
            },
        },
        persStatic: {
            host: 'pers-static.tst.vs.market.yandex.net',
            port: 34522,
            data: {
                traceServiceId: 'market_pers_static',
            },
        },
        persHistory: {
            host: 'pers-history.tst.vs.market.yandex.net',
            port: 38602,
            data: {
                traceServiceId: 'persHistory',
            },
        },
        personal: {
            host: 'personal-market.taxi.tst.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'market_personal',
            },
        },
        yaStatic: {
            host: 'yastatic.net',
            // Закомментированно, пока нет доступа
            // host: 'betastatic.yandex.net',
            protocol: 'http:',
            port: 80,
            path: '',
            data: {
                traceServiceId: 'yandex_static',
            },
        },
        s3mds: {
            host: 'marketfront.s3.mdst.yandex.net',
            port: 80,
            timeout: 1000,
            envHosts: {
                testing: 'marketfront.s3.mdst.yandex.net',
                production: 'marketfront.s3.mds.yandex.net',
            },
            endpoint: 's3.mdst.yandex.net',
            data: {
                traceServiceId: 's3mds',
            },
        },
        yablog: {
            host: 'yablogs-api-hidden.yandex.net',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'market_yablog',
            },
        },
        bigb: {
            host: 'bigb-fast.yandex.ru',
            protocol: 'http:',
            data: {
                traceServiceId: 'bigb',
            },
        },

        seoExp: {
            host: 'seo-exps.tst.vs.market.yandex.net',
            protocol: 'http:',
            port: '80',
            data: {
                traceServiceId: 'seoExp',
            },
        },

        checkouter: {
            host: 'checkouter.tst.vs.market.yandex.net',
            port: 39001,
            protocol: 'http:',
            data: {
                traceServiceId: 'market_checkouter',
            },
        },

        carter: {
            host: 'carter.tst.vs.market.yandex.net',
            port: 35803,
            data: {
                traceServiceId: 'market_carter',
            },
        },

        appInstall: {
            host: 'saas-searchproxy-prestable.yandex.net',
            port: 17000,
            protocol: 'http:',
            data: {
                traceServiceId: 'appInstall',
            },
        },

        vhFrontend: {
            host: 'internal.vh.yandex.net',
            protocol: 'https:',
            data: {
                traceServiceId: 'vhFrontend',
            },
        },

        orderFeedback: {
            host: 'pers-feedback.tst.vs.market.yandex.net',
            port: 443,
            protocol: 'https:',
            data: {
                traceServiceId: 'orderFeedback',
            },
        },

        persPay: {
            host: 'pers-pay.tst.vs.market.yandex.net',
            port: 443,
            protocol: 'https:',
            data: {
                traceServiceId: 'persPay',
            },
        },
        ocrm: {
            host: 'ow.tst.market.yandex-team.ru',
            port: 443,
            protocol: 'https:',
            path: '/api/tvm/',
            data: {
                traceServiceId: 'ocrm',
            },
        },
        tplPvz: {
            host: 'pvz-int.tst.vs.market.yandex.net',
            port: 443,
            protocol: 'https:',
            data: {
                traceServiceId: 'tplPvz',
            },
        },
        taxiB2B: {
            host: 'b2b.taxi.tst.yandex.net',
            port: 443,
            protocol: 'https:',
            data: {
                traceServiceId: 'taxiB2B',
            },
        },
        boxBotReturn: {
            host: 'return.boxbot.dev.market.yandex.net',
            protocol: 'https:',
            port: '443',
            data: {
                traceServiceId: 'boxBotReturn',
            },
        },
        perseyPayments: {
            host: 'persey-payments.taxi.tst.yandex.net',
            port: 80,
            protocol: 'http:',
            data: {
                traceServiceId: 'perseyPayments',
            },
        },
        djRecommender: {
            host: 'dj-recommender.tst.vs.market.yandex.net',
            protocol: 'http:',
            port: 80,
            data: {
                traceServiceId: 'djRecommender',
            },
        },
        marketLogistics: {
            host: 'tpl-int.tst.vs.market.yandex.net',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'tpl-int',
            },
        },
        marketUtils: {
            host: 'market-utils.tst.vs.market.yandex.net',
            port: 35826,
            data: {
                traceServiceId: 'market_utils',
            },
        },
        marketb2bclients: {
            host: 'marketb2bclients.tst.vs.market.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'marketb2bclients',
            },
        },

        b2boffice: {
            host: 'b2boffice.tst.market.yandex-team.ru',
            path: '/api/tvm2',
            port: 443,
            protocol: 'https:',
            data: {
                traceServiceId: 'b2boffice',
            },
        },

        eatsRetailIntegration: {
            host: 'eats-retail-market-integration.eda.tst.yandex.net',
            port: 80,
            protocol: 'http:',
            data: {
                traceServiceId: 'eats_retail_integration',
            },
        },
        eatsAuthproxy: {
            host: 'eats-authproxy.eda.tst.yandex.net',
            port: 80,
            protocol: 'http:',
            data: {
                traceServiceId: 'eats_authproxy',
            },
        },
        taxiCorpSuggest: {
            host: 'corp-suggest.taxi.tst.yandex.net',
            port: 80,
            protocol: 'http',
            data: {
                traceServiceId: 'taxi_corp_suggest',
            },
        },
        cashbackAnnihilator: {
            host: 'cashback-annihilator.taxi.tst.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'cashback_annihilator',
            },
        },

        kotopes: {
            host: 'kotopes.tst.vs.market.yandex.net',
            port: '80',
            protocol: 'http:',
            data: {
                traceServiceId: 'kotopes',
            },
        },
        /**
         * @expFlag all_checkout_chef
         * @ticket MARKETPROJECT-9717
         * start
         */
        chef: {
            host: 'chef.tst.vs.market.yandex.net',
            port: '80',
            data: {
                traceServiceId: 'chef',
            },
        },
        /**
         * @expFlag all_checkout_chef
         * @ticket MARKETPROJECT-9717
         * end
         */
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

    isTest: false,

    ipregDataPath: '/var/cache/geobase/ipreg-layout.json',

    abt: {
        contextIds: ['DESKTOP', 'DESKTOP_BLUE', 'MARKET', 'MARKET_BLUE', 'MARKET_ALL'],
        flagsAllowedDir: 'configs/flags_allowed.d',
    },

    // Коллекция флагов, которые включают небезопасные инструменты в окружении данной конфигурации
    allowedFeatureFlags: {
        // Дебаг подключания GTM
        gtmDebug: true,
    },

    // секретный ключ, передается в заголовках запроса к adfox
    ADFOX_S2S_KEY: '11607748271874771201',

    // идентификатор маркета, как клиента в adfox
    MARKET_ACCOUT_ID_WITHIN_ADFOX: '252616',

    // передается в запросе к adfox как обязательный параметер ps='cmou'
    MARKET_SITE_ID_WITHIN_ADFOX: 'cmou',

    kadavrAvailable: true,
    kadavrHost: process.env.KADAVR_HOST || 'kadavr.vs.market.yandex.net',
    kadavrPort: process.env.KADAVR_PORT || 80,

    /**
     * Id формы в конструкторе форм (ТЕСТИНГ).
     * Админка -> {https://admin-ext.forms.test.yandex-team.ru}.
     */
    searchFeedbackFormId: 6301,

    startrekComplainQueueName: 'TEST',

    yaMetrika: {
        market: {
            id: 44910898,
        },
    },

    yablog: {
        id: 'market-news-portal',
    },

    bugReport: {
        formId: 10014450,
    },

    errorBooster: {
        project: 'market_front',
        platform: 'desktop',
    },
    offerDiagnosticServiceUrl: 'https://doctor-testing.market.yandex-team.ru/offer-diagnostic',
};

const extEnv = process.env.EXT_ENV;

if (extEnv) {
    _.merge(module.exports, require(`./${extEnv}`));
}
