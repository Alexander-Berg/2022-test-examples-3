module.exports = {
    servant: {
        apple: {
            host: 'itunes.apple.com',
            port: 443,
            protocol: 'https:',
        },
        orderFeedback: {
            host: 'pers-feedback.tst.vs.market.yandex.net',
            port: 443,
            protocol: 'https:',
            data: {
                traceServiceId: 'orderFeedback',
            },
        },
        passport: {
            // Больше информации по адресу https://wiki.yandex-team.ru/market/verstka/services/passport
            host: 'pass-test.yandex.ru', // Для нагрузочных стрельб
            data: {
                traceServiceId: 'passport_blackbox',
            },
        },
        ipaInternal: {
            host: 'internal-ipa.tst.vs.market.yandex.net',
            protocol: 'https:',
            port: 443,
        },
        dyno: {
            host: 'dyno.tst.vs.market.yandex.net',
            protocol: 'http:',
            port: 80,
        },
        sovetnikML: {
            host: 'sovetnik-ml.vs.market.yandex.net',
            protocol: 'http:',
            port: 80,
        },
        mars: {
            host: 'mars.tst.vs.market.yandex.net',
            protocol: 'http:',
            port: 80,
            data: {
                traceServiceId: 'mars',
            },
        },
        bigb: {
            host: 'bigb-fast.yandex.ru',
            protocol: 'http:',
            data: {
                traceServiceId: 'bigb',
            },
        },
        blackbox: {
            // Больше информации по адресу https://wiki.yandex-team.ru/market/verstka/services/passport
            host: 'blackbox-test.yandex.net',
            protocol: 'http:',
            path: '/blackbox',
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
        social: {
            host: 'api.social-test.yandex.ru',
            port: 80,
            protocol: 'http:',
            consumer: 'market_front_blue-testing-testing',
            data: {
                traceServiceId: 'passport_social-api',
            },
        },
        reviews: {
            host: 'pers-static.tst.vs.market.yandex.net',
            port: 34522,
            data: {
                traceServiceId: 'market_pers_static',
            },
        },
        report: {
            host: 'report.tst.vs.market.yandex.net',
            port: 17051,
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

            changeable: true, // возможность переопределять параметры вызова через query или cookie
        },
        cataloger: {
            host: 'marketcataloger.tst.vs.market.yandex.net',
            port: 29302,
            path: '/cataloger/',
            data: {
                traceServiceId: 'market_kgb_cataloger',
            },
        },
        deprecatedCataloger: {
            host: 'marketcataloger.tst.vs.market.yandex.net',
            port: 29302,
            path: '/cataloger/',
            data: {
                traceServiceId: 'market_kgb_cataloger',
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
        startrek: {
            host: 'st-api.yandex-team.ru',
            protocol: 'https:',
            path: '/v2',
            port: 443,
        },
        bnpl: {
            host: 'bnpl.fintech.tst.yandex.net',
            protocol: 'http:',
            port: 80,
        },
        history: {
            host: 'pers-history.tst.vs.market.yandex.net',
            port: 38602,
            path: '/history/',
            data: {
                traceServiceId: 'market_pers_history',
            },
        },
        lavka: {
            host: 'grocery-market-gw.lavka.tst.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'lavka',
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
        lavkaAuthproxy: {
            host: 'grocery-authproxy.lavka.tst.yandex.net',
            port: 443,
            protocol: 'https:',
            data: {
                traceServiceId: 'lavka_authproxy',
            },
        },
        loyalty: {
            host: 'market-loyalty.tst.vs.market.yandex.net',
            port: 35815,
            data: {
                traceServiceId: 'market_loyalty',
            },
        },
        wishlist: {
            host: 'pers-basket.tst.vs.market.yandex.net',
            port: 34510,
            data: {
                traceServiceId: 'market_pers_basket',
            },
        },

        templator: {
            host: 'templator.tst.vs.market.yandex.net',
            port: 29338,
            data: {
                traceServiceId: 'market_kgb_templator',
            },
        },

        persAddress: {
            host: 'pers-address.tst.vs.market.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'pers_address',
            },
        },

        persAuthor: {
            host: 'pers-author.tst.vs.market.yandex.net',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'pers_author',
            },
        },
        persQA: {
            host: 'pers-qa.tst.vs.market.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'pers-qa',
            },
        },
        personal: {
            host: 'personal-market.taxi.tst.yandex.net',
            port: 80,
            data: {
                traceServiceId: 'market_personal',
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
        /**
         * @expFlag all_electronic-recipe
         * @ticket MARKETFRONT-69901
         * @end
         */
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
        sberlog: {
            host: 'sberlog.tst.vs.market.yandex.net',
            port: 443,
            protocol: 'https:',
            data: {},
        },
        bunker: {
            host: 'bunker-api-dot.yandex.net',
            port: 80,
            path: '/v1',
            version: 'latest',
            bunkerProject: 'beru',
            tankerField: 'test',
            data: {
                traceServiceId: 'market_bunker',
            },
        },
        carter: {
            host: 'carter.tst.vs.market.yandex.net',
            port: 35803,
            data: {
                traceServiceId: 'market_carter',
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
        clickDaemon: {
            host: 'market-click2-testing.yandex.ru',
            port: 443,
            protocol: 'https:',
            data: {
                traceServiceId: 'market-click2',
            },
        },
        djRecommender: {
            host: 'dj-recommender.tst.vs.market.yandex.net',
            protocol: 'http:',
            port: 80,
            data: {},
        },
        marketLogistics: {
            host: 'tpl-int.tst.vs.market.yandex.net',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'tpl-int',
            },
        },
        boxBot: {
            host: 'localhost',
            data: {
                traceServiceId: 'boxBot',
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
        comparison: {
            host: 'pers-comparison.tst.vs.market.yandex.net',
            port: 80,
            path: '/api/comparison/',
            data: {
                traceServiceId: 'pers_comparison',
            },
        },
        geocoder: {
            host: 'addrs-testing.search.yandex.net',
            path: '/search/stable/yandsearch',
            data: {
                traceServiceId: 'geocode',
            },
        },
        geoSuggest: {
            host: 'suggest-maps-test.n.yandex-team.ru',
            path: '/suggest-geo',
            data: {
                traceServiceId: 'geoSuggest',
            },
        },
        mobileValidator: {
            host: 'mobilevalidator.tst.vs.market.yandex.net',
            port: 443,
            protocol: 'https:',
            data: {
                traceServiceId: 'mobileValidator',
            },
        },
        /**
         * @rearr antirobot
         * @ticket MARKETFRONT-69107
         * start
         */
        mobileValidation: {
            host: 'narwhal.yandex.net',
            port: 443,
            protocol: 'https:',
            data: {
                traceServiceId: 'mobileValidator',
            },
        },
        /**
         * @rearr antirobot
         * @ticket MARKETFRONT-69107
         * end
         */
        s3mds: {
            host: 'marketfront.s3.mdst.yandex.net',
            timeout: 1000,
            port: 80,
            envHosts: {
                testing: 'marketfront.s3.mdst.yandex.net',
                production: 'marketfront.s3.mds.yandex.net',
            },
        },
        s3black: {
            host: 's3.mdst.yandex.net',
            protocol: 'https:',
            path: '/black-market/',
            timeout: 1000,
            data: {
                traceServiceId: 's3black',
            },
        },
        s3blackProd: {
            host: 'black-market.s3.yandex.net',
            protocol: 'https:',
            timeout: 1000,
            data: {
                traceServiceId: 's3black',
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
        vhFrontend: {
            host: 'internal.vh.yandex.net',
            protocol: 'https:',
            data: {
                traceServiceId: 'vhFrontend',
            },
        },
        ocrm: {
            host: 'ow2.tst.market.yandex-team.ru',
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
        perseyPayments: {
            host: 'persey-payments.taxi.tst.yandex.net',
            port: 80,
            protocol: 'http:',
            data: {
                traceServiceId: 'perseyPayments',
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
        appSuggest: {
            host: 'yandex.ru',
            protocol: 'https:',
            port: 443,
            data: {
                traceServiceId: 'appSuggest',
                overrides: [
                    {
                        newPath: '/suggest-market/suggest-market-endings-blue2',
                        matchParam: 'new_api',
                        matchParamValue: 1,
                    },
                ],
            },
        },
        marketUtils: {
            host: 'market-utils.tst.vs.market.yandex.net',
            port: 35826,
            data: {
                traceServiceId: 'market_utils',
            },
        },
        // Тестового хоста для этого сервиса нет
        uaas: {
            host: 'uaas.search.yandex.net',
            protocol: 'http:',
            path: '/bluemarketapps',
        },

        avatars: {
            host: 'avatars-int.mdst.yandex.net',
            port: 13000,
            protocol: 'http:',
            data: {
                traceServiceId: 'mds_avatars',
            },
        },
        vhUgc: {
            protocol: 'https:',
            host: 'vh.test.yandex.ru',
            port: 443,
            data: {
                traceServiceId: 'vhUgc',
            },
        },
        serpSearchWizard: {
            host: 'yandex.ru',
            path: '/search/wizardsjson', // с завершающим слэшом не работает!
            data: {
                traceServiceId: 'market_serp-search-wizard',
            },
        },
        picSearch: {
            host: 'yandex.ru',
            path: '/images/api/v1/cbir/market',
            data: {
                traceServiceId: 'picSearch',
            },
        },
        combinator: {
            host: 'combinator.tst.vs.market.yandex.net',
            protocol: 'http:',
            data: {
                traceServiceId: 'combinator',
            },
        },
        metrika: {
            host: 'mc.yandex.ru',
            protocol: 'https:',
            data: {
                traceServiceId: 'metrika',
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

    hosts: {
        self: 'ipa-test.market.yandex.ru',
        yandex: 'yandex.ru',
        passport: 'passport-test.yandex.ru',
        passportTest: 'passport-test.yandex.ru',
        avatarsHost: 'avatars.mds.yandex.net',
        whiteMarketDesktop: 'desktop.tst.market.yandex.ru',
    },
};
