// настройки для market-test.* хостов
module.exports = {
    isTest: true,
    tvm: {
        env: 'market_front_white-testing-testing',
    },
    hosts: {
        passport: 'passport-test.yandex.ru',
        passportBase: 'passport-test.yandex.',
        social: 'social-test.yandex.ru',
        avatarsHost: 'avatars.mdst.yandex.net',
        photosHost: 'avatars.mdst.yandex.net',
        // Для демостендов ходим за статикой локально
        static: null,
        paymentWidget: 'https://testing.payment-widget.ott.yandex.ru',
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
        passport: 'passport-test.yandex.<tld>',
        social: 'social-test.yandex.<tld>',
        avatarsHost: 'avatars.mdst.yandex.net',
        photosHost: 'avatars.mdst.yandex.net',
    },
    servant: {
        passport: {
            host: 'pass-test.yandex.ru',
        },
        avatars: {
            host: 'avatars-int.mdst.yandex.net',
        },
        dataSyncAddresses: {
            host: 'api-stable.dst.yandex.net',
        },
        social: {
            consumer: 'market_front_white-testing-testing',
        },
    },
    cspPolicies: {
        'img-src': [
            'avatars.mdst.yandex.net',
        ],
    },
};
