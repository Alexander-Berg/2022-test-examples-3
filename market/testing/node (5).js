const contentSecurityPolicy = require('./content-security-policy');
const cspPaymentPolicies = require('./content-security-policy-payment');
const eatsKit = require('./eatsKit');

// http://stackoverflow.com/questions/10888610/ignore-invalid-self-signed-ssl-certificate-in-node-js-with-https-request
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
const {NODE_PORT} = process.env;

const STATIC_SELF = Boolean(process.env.STATIC_SELF);

const SECRETS_DIR = process.env.SECRETS_DIR || '/etc/datasources';

const enableTurboProxyFile = `${process.cwd()}/../../turboproxy-enable.json`;

/**
 * Файл с настройками TVM.
 * https://st.yandex-team.ru/CSADMIN-13370
 */
const saltFile = `${SECRETS_DIR}/salt.json`;

const MEMCACHED_SERVERS = 'front-cache.tst.vs.market.yandex.net:11226';

module.exports = {
    service: 'touch',
    env: 'testing',

    // Пока не используется
    // Возможно в будущем будем читать это из package.json
    // И скорее всего эта штука как-то будет фигурировать в чувствительных к версии фронта данных
    version: '1',

    server: NODE_PORT,

    // добавляет в ответ debug-информацию (например. об ошибках)
    debug: true,

    cache: {
        worker: {
            capacity: 500,
        },

        shared: {
            locations: MEMCACHED_SERVERS.split(';'),
            generation: '1',
            idle: 20 * 1000,
            retry: 1 * 1000,
            reconnect: 5 * 60 * 1000,
        },

        // TODO: выпилить после полного отказа от кеширования ресурсов.
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

    solomonMetrics: {
        ttlb: true,
        errors: true,
        backendsRps: true,
        backendsTimings: true,
    },

    tracerType: 'console',
    tracer: {
        format: ['{{title}}\t{{message}}'],
        dateformat: 'yyyy/mm/dd HH:MM:ss',
        level: 'info',
    },

    // значение sk, которое всегда будет проходить валидацию (используем для нагрузочного тестирования)
    sk: '4d4a9485a03224c7347cd0cb74bd1712',

    // для клиентского доступа
    hosts: {
        self: 'm.market.yandex.ru',
        desktop: 'market.yandex.ru',
        yandex: 'yandex.ru',
        passport: 'passport-rc.yandex.ru',
        passportBase: 'passport-rc.yandex.',
        forms: 'forms.yandex.ru',
        passportTest: 'passport-test.yandex.ru',
        social: 'social-rc.yandex.ru',
        clickDaemon: 'market-click2-testing.yandex.ru',
        suggest: 'yandex.ru',
        static: STATIC_SELF ? null : 'betastatic.yastatic.net',
        exportStatic: 'betastatic.yastatic.net/market-export/_/',
        tune: 'm.tune.yandex.ru',
        phoneValidator: 'phone-passport-test.yandex.ru',
        emailValidator: 'validator.yandex.ru',
        avatarsHost: 'avatars.mds.yandex.net',
        currentAvatarsHost: 'avatars.mdst.yandex.net',
        suggestMaps: 'suggest-maps.yandex.ru',
        staticMaps: 'static-maps.yandex.ru',
        apiMaps: 'api-maps.yandex.ru',
        youtubecom: 'youtube.com',
        youtube: 'youtu.be',
        csp: 'csp.yandex.net',
        // Первый продакшен хост Пифии @see -> https://wiki.yandex-team.ru/pythia/rukovodstvo-po-api/
        pythia: 'yandex.ru',
        // Второй продакшен хост Пифии @see -> https://wiki.yandex-team.ru/pythia/rukovodstvo-po-api/
        odnaco: 'odna.co',
        beru: 'touch.pokupki.fslb.market.yandex.ru',
        yablog: 'yablogs-api-hidden.yandex.net',
        checkout: 'touch.checkout.market.fslb.yandex.ru',
        cart: 'touch.pokupki.fslb.market.yandex.ru',
        staff: 'staff-api.test.yandex-team.ru',
        yandex_music: 'music.mt.yandex.ru', // работает только с passport-test
        yandexHelp: 'helpnearby.taxi.tst.yandex.ru',
        yandexHelpLanding: 'helpnearby.taxi.tst.yandex.ru/mobile-constructor',
        yaplus: 'plus.yandex.ru',
        trustApi: 'trust-test.yandex.ru',
        creditBroker: 'credit-broker.tst.vs.market.yandex.net',
        otrace: 'http://active.idxapi.tst.vs.market.yandex.net:29334/v1/otrace',
        lms: 'https://lms-admin.tst.market.yandex-team.ru',
        wiki: 'https://wiki.yandex-team.ru',
        adminMarketLoyalty: 'admin.market-loyalty.tst.market.yandex-team.ru',
        eatsRetailIntegration: 'eats-retail-market-integration.eda.tst.yandex.net',
        eatsAuthproxy: 'eats-authproxy.eda.tst.yandex.net',
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
        ycombo: 'ycombo.tst.vs.market.yandex.net',
        // сервис Еда
        eatsCheckout: 'https://testing.eda.tst.yandex.ru/market/cart',
        eatsOrder: 'https://testing.eda.tst.yandex.ru/market/order',
        paymentWidget: 'https://payment-widget.ott.yandex.ru',
        personal: 'http://personal-market.taxi.tst.yandex.net',
        kotopes: 'kotopes.tst.vs.market.yandex.net',
        /**
         * @expFlag all_credit_broker
         * @ticket MARKETFRONT-72243
         * @start
         */
        authDataProvider: 'https://autofill-test.yandex.ru',
        authDataProviderSource: 'https://yastatic.net/s3/passport-static',
        /**
         * @expFlag all_credit_broker
         * @ticket MARKETFRONT-72243
         * @end
         */
    },

    hostsTld: {
        self: 'm.market.yandex.<tld>',
        desktop: 'market.yandex.<tld>',
        yandex: 'yandex.<tld>',
        passport: 'passport-rc.yandex.<tld>',
        suggest: 'yandex.<tld>',
        clickDaemon: 'market-click2-testing.yandex.<tld>',

        apiMaps: 'api-maps.yandex.<tld>',
        suggestMaps: 'suggest-maps.yandex.<tld>',
    },

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
        passportTest: {
            // используется для *-test.hostname.yandex.ru хостов
            host: 'pass-test.yandex.ru',
            data: {
                traceServiceId: 'passport_blackbox',
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
        serpSearchWizard: {
            host: 'yandex.ru',
            path: '/search/wizardsjson', // с завершающим слэшом не работает!
            data: {
                traceServiceId: 'market_serp-search-wizard',
            },
        },
        yaSMS: {
            host: 'phone-passport-test.yandex.ru',
            protocol: 'https:',
            data: {
                traceServiceId: 'passport_sms',
            },
        },
        shopInfo: {
            host: 'shopinfo.tst.vs.market.yandex.net',
            port: 38110,
            protocol: 'http:',
            data: {
                traceServiceId: 'market_mbi_shopinfo',
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
        social: {
            host: 'social-test.yandex.ru',
            port: 80,
            consumer: 'market_front_white-testing-mimino',
            data: {
                traceServiceId: 'passport_social-api',
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
        checkouterPayment: {
            host: 'checkouter.tst.vs.market.yandex.net',
            port: 39011,
            protocol: 'https:',
            data: {
                traceServiceId: 'market_checkouter',
            },
        },
        checkout: {
            host: 'checkouter.tst.vs.market.yandex.net',
            port: 39001,
            protocol: 'http:',
            data: {
                traceServiceId: 'market_checkouter',
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
            changeable: true, // возможность переключать хост и порт с помощью query-параметра backend
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
        fenek: {
            host: 'ads6.adfox.ru',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'market_banners_adfox',
            },
        },
        notifications: {
            host: 'market-utils.tst.vs.market.yandex.net',
            port: 35826,
            data: {
                traceServiceId: 'market_utils',
            },
        },
        persGradeSrc: {
            host: 'pers-grade.tst.vs.market.yandex.net',
            port: 35824,
            data: {
                traceServiceId: 'market_pers_grade',
            },
        },
        grades: {
            host: 'pers-grade.tst.vs.market.yandex.net',
            port: 35824,
            data: {
                traceServiceId: 'market_pers_grade',
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
            host: 'blackbox-mimino.yandex.net',
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
        mboLite: {
            host: 'mbo-lite.tst.vs.market.yandex.net',
            port: 33402,
            data: {
                traceServiceId: 'mboLite',
            },
        },
        cms: {
            host: 'mbo-cms-api.tst.vs.market.yandex.net',
            data: {
                traceServiceId: 'market_cms',
            },
        },
        reviews: {
            host: 'pers-static.tst.vs.market.yandex.net',
            port: 34522,
            data: {
                traceServiceId: 'market_pers_static',
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
            data: {
                traceServiceId: 'market_banners_adfox',
            },
            headers: {
                host: 'ads.adfox.ru',
            },
        },
        billboard: {
            host: 'awaps.yandex.ru',
            data: {
                traceServiceId: 'awaps',
            },
        },
        experiments: {
            host: 'market.fslb01ht.yandex.ru',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'market_front_touch',
            },
        },
        dataSyncAddresses: {
            host: 'api-mimino01h.dst.yandex.net',
            port: 8080,
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
        clickDaemon: {
            host: 'market-click2-testing.yandex.ru',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'market-click2',
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
        tyres: {
            host: 'guruass.tst.vs.market.yandex.net',
            port: 29327,
            protocol: 'http:',
            data: {
                traceServiceId: 'market_kgb_guruass',
            },
        },
        comments: {
            host: 'market-utils.tst.vs.market.yandex.net',
            port: 35826,
            path: '/api/comment/',
            data: {
                traceServiceId: 'market_comments',
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
        picsearch: {
            host: 'yandex.ru',
            path: '/images/api/v1/cbir/market',
            data: {
                traceServiceId: 'picsearch',
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
        marketUtils: {
            host: 'market-utils.tst.vs.market.yandex.net',
            port: 35826,
            data: {
                traceServiceId: 'market_utils',
            },
        },
        staff: {
            host: 'staff-api.test.yandex-team.ru',
            path: '/v3',
            data: {
                traceServiceId: 'staff',
            },
        },
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
        articleViews: {
            host: 'pers-history.tst.vs.market.yandex.net',
            port: 38602,
            path: '/views/article',
            data: {
                traceServiceId: 'market_pers_article_views',
            },
        },
        persHistory: {
            host: 'pers-history.tst.vs.market.yandex.net',
            port: 38602,
            data: {
                traceServiceId: 'persHistory',
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
        persGrade: {
            host: 'pers-grade.tst.vs.market.yandex.net',
            port: 35824,
            data: {
                traceServiceId: 'market_pers_grade',
            },
        },
        persStatic: {
            host: 'pers-static.tst.vs.market.yandex.net',
            port: 34522,
            data: {
                traceServiceId: 'market_pers_static',
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
        vhUgc: {
            protocol: 'https:',
            host: 'vh.test.yandex.ru',
            port: 443,
            data: {
                traceServiceId: 'vhUgs',
            },
        },
        s3mds: {
            host: 'marketfront.s3.mdst.yandex.net',
            port: 80,
            timeout: 3000,
            envHosts: {
                testing: 'marketfront.s3.mdst.yandex.net',
                production: 'marketfront.s3.mds.yandex.net',
            },
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
        djRecommender: {
            host: 'dj-recommender.tst.vs.market.yandex.net',
            protocol: 'http:',
            port: 80,
            data: {
                traceServiceId: 'dj_recommender',
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
        xiva: {
            host: 'push-sandbox.yandex.ru',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'xiva',
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
        mobileValidator: {
            host: 'narwhal.yandex.net',
            port: 443,
            protocol: 'https:',
            data: {
                traceServiceId: 'mobileValidator',
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
        vhFrontend: {
            host: 'internal.vh.yandex.net',
            protocol: 'https:',
            data: {
                traceServiceId: 'vhFrontend',
            },
        },
        messenger: {
            host: 'messenger-internal.alpha.yandex.net',
            port: 443,
            protocol: 'https:',
            data: {
                traceServiceId: 'messenger',
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
        boxBot: {
            host: 'localhost',
            data: {
                traceServiceId: 'boxBot',
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
        marketLogistics: {
            host: 'tpl-int.tst.vs.market.yandex.net',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'tpl-int',
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
        /**
         * @expFlag all_electronic-recipe
         * @ticket MARKETFRONT-69901
         * @start
         */
        checkErxService: {
            host: 'check-erx-service.tst.vs.market.yandex.net',
        },
        medicata: {
            host: 'my.test.e-rx.ru',
        },
        /**
         * @expFlag all_electronic-recipe
         * @ticket MARKETFRONT-69901
         * @end
         */
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

    skipWarmUp: false,


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

    templates: {
        error: `${__dirname}/../../touch.bundles/error/error.yate`,
    },

    saltFile,
    enableTurboProxyFile,

    traceLogger: {
        socket: process.env.TRACE_LOG_SOCK,
    },

    abt: {
        contextIds: ['TOUCH', 'TOUCH_BLUE', 'MARKET', 'MARKET_BLUE', 'MARKET_ALL'],
        flagsAllowedDir: 'configs/flags_allowed.d',
    },

    // Коллекция флагов, которые включают небезопасные инструменты в окружении данной конфигурации
    allowedFeatureFlags: {
        // Клиентский инспектор для мобильных браузеров без собственного инспектора (например, Uc Web)
        // @see https://st.yandex-team.ru/MOBMARKET-3371
        eruda: true,

        // Возможность отключать заданные ресурсы полностью или отдельные методы точечно
        // @see https://st.yandex-team.ru/MOBMARKET-4435
        rejectResources: true,

        // Включение debug режима для yandex-apps-api
        yandexAppsApiDebug: true,

        // Дебаг подключания GTM
        gtmDebug: true,
    },

    tvm: {
        env: 'market_front_white-testing-mimino',
        configPath: '../../tvm-daemon/tvmtool.conf',
        authPath: '../../tvm-daemon/local.auth',
    },

    reduxDevtoolsEnabled: true,

    cspPolicies: contentSecurityPolicy,
    // дополнительные параметры отправляемые в репортер
    cspReporterParams: {
        project: 'market_white',
        platform: 'touch',
        env: 'testing',
    },
    cspPaymentPolicies,

    serverTemplate: 'touch.bundles/server/server.yate',

    ipregDataPath: '/var/cache/geobase/ipreg-layout.json',

    kadavrAvailable: true,
    kadavrHost: process.env.KADAVR_HOST || 'kadavr.vs.market.yandex.net',
    kadavrPort: process.env.KADAVR_PORT || 80,

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

    disablePlatformRedirects: true,

    doubleClickManagerEnabled: false,

    errorBooster: {
        project: 'market_front',
        platform: 'touch',
    },

    // platform: api
    handlersDir: './api/app/handlers',

    eatsKit,
    offerDiagnosticServiceUrl: 'https://doctor-testing.market.yandex-team.ru/offer-diagnostic',
};

const extEnv = process.env.EXT_ENV;

if (extEnv) {
    // eslint-disable-next-line no-restricted-modules, global-require
    const _ = require('lodash');
    // eslint-disable-next-line global-require
    _.merge(module.exports, require(`./${extEnv}`));
}
