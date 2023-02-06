const {resolve} = require('path');

const envify = require('../utilities/envify');

module.exports = {
    socket: resolve(__dirname, '../run/node.sock'),
    adFox: {
        ownerId: 357839,
        siteId: 'ejlu',
    },
    passport: {
        blackbox: {
            api: 'pass-test.yandex.ru',
            emails: 'getdefault',
            getphones: 'bound',
            phone_attributes: '1',
            multisession: 'yes',
            attributes: {login: '1008', lang: '34', plus: '1015'},
            fields: {
                yandexoid: 'subscription.login.669',
                betatest: 'subscription.login.668',
            },
        },
        avatarPath: 'https://avatars.mdst.yandex.net/',
        passportPath: 'https://passport-test.yandex.ru',
        authorizePath:
            'https://passport-test.yandex.ru/passport?mode=auth&retpath=',
        resignPath: 'https://passport-test.yandex.ru/auth/update?&retpath=',
    },
    environmentConfig: {
        secureIFramePath: 'https://3ds.travel-test.yandex.net/3ds',
        uxfeedbackIframeSrc:
            'https://yastat.net/s3/travel/static/_/uxfeedback.html',
    },
    servicesAPI: envify('BASE_URL_', {
        travel: 'https://api.travel-balancer-test.yandex.net/api',
        aviaFront: 'https://front.avia.tst.yandex.ru/epp-api',
        avia: 'https://api.avia.tst.yandex.net',
        aviaGateway: 'http://api-gateway.testing.avia.yandex.net/v1',
        flightStorage: 'http://api-gateway.testing.avia.yandex.net/v1/flight',
        trains: 'https://testing.train-api.rasp.internal.yandex.net',
        geoBase: 'http://geobase-test.qloud.yandex.ru',
        uatraits: 'http://uatraits-test.qloud.yandex.ru',
        travellers: 'https://travelers.testing.avia.yandex.net',
        rasp: 'https://testing.morda-backend.rasp.yandex.net',
        raspCache:
            'https://testing.http-proxy-cache.internal.rasp.yandex.net/morda_backend',
        raspCrossLinks: 'https://testing.crosslink.internal.rasp.yandex.net',
        raspTransfers:
            'https://testing.pathfinder-proxy.rasp.common.yandex.net',
        raspSuggests: 'https://testing.suggests.rasp.common.yandex.net',
        trainsOfferStorage:
            'https://testing.offer-storage.internal.rasp.yandex.net',
        aviaBookingOrders: 'http://booking-service.testing.avia.yandex.net',
        passengerExperience:
            'http://api-gateway.testing.avia.yandex.net/v1/flight-extras',
        aviaTicketDaemon:
            'http://api-gateway.testing.avia.yandex.net/v1/ticket-daemon-api',
        aviaBackend: 'http://backend.testing.avia.yandex.net',
        aviaSuggests: 'https://suggests.avia.tst.yandex.net',
        aviaPriceIndex:
            'http://api-gateway.testing.avia.yandex.net/v1/price-index/',
        aviaFeatures:
            'http://api-gateway.testing.avia.yandex.net/v1/avia-features',
        tinyUrl: 'http://tinyurl-test.yandex.ru/tiny',
        busesGeo: 'https://testing.geo-api.internal.bus.yandex.net',
        buses: 'https://testing.backend.internal.bus.yandex.net/api',
        bunker: 'http://bunker-api-dot.yandex.net',
        imageGenerator: 'https://travel-tools-test.yandex.net/image-generator',
        templator: 'http://templator.vs.market.yandex.net:29338/tarantino',
        reviews: 'https://rasp.s3.yandex.net/reviews',
        seoExps: 'https://seo-exps.tst.market.yandex-team.ru',
    }),
    experiments: {
        // Common

        // Trains
        skipSalesCheck: {
            type: Boolean,
            defaultValue: false,
            denied: false,
        },
        trainsAllowInternationalRoutes: {
            type: Boolean,
            defaultValue: false,
            denied: false,
        },

        // Avia
        enablePartnersFilter: {
            type: Boolean,
            defaultValue: true,
            denied: false,
        },
        isAviaPortal: {
            type: Boolean,
            defaultValue: false,
            denied: false,
        },

        // Hotels
        enableDebugHotelProdOffers: {
            type: Boolean,
            defaultValue: true,
            denied: false,
        },
    },
    features: {
        enableFeaturesPage: false,
    },
    orders: {
        canUseMockPayment: true,
        canUseTestContext: true,
    },
    uaas: {
        uri: 'http://uaas.search.yandex.net/travel',
        handler: 'TRAVEL',
        env: 'testing',
    },
    csrf: {
        secret: 'CSRF_TESTING_TOKEN',
        lifetime: 7 * 24 * 60 * 60,
    },
    telemetryClickHouseEnvTable: 'env_testing',
    travelTracing: {
        config: {
            serviceName: 'travel-front',
            disable: false,
            sampler: {
                type: 'probabilistic',
                param: 0.001,
            },
        },
        meta: {
            forceDebugTag: true,
        },
    },
};
