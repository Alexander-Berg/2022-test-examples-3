// настройки для market-test.* хостов
module.exports = {
    isTest: true,
    tvm: {
        env: 'market_front_vendors-testing-testing',
    },
    hosts: {
        passport: 'passport-test.yandex.ru',
        social: 'social-test.yandex.ru',
        avatarsHost: 'avatars.mdst.yandex.net',
        photosHost: 'avatars.mdst.yandex.net',
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
    },
    cspPolicies: {
        'img-src': [
            'avatars.mdst.yandex.net',
        ],
    },
};
