/**
* Настройки для расширенного окружения blackbox-stress FAPI
* В этом окружении тестинг FAPI может авторизовывать приложения по продовым oAuth токенам
*/
module.exports = {
    isTest: true,
    tvm: {
        env: 'market_front_blue-testing-stress',
    },
    hosts: {
        passport: 'passport-rc.yandex.ru',
        authPassport: 'pass-rc.beru.ru',
        yandexAuthPassport: 'pass-rc.yandex.ru',
        authSubdomain: 'pass-rc',
        social: 'social-rc.yandex.ru',
        avatarsHost: 'avatars.mds.yandex.net',
        // Для демостендов ходим за статикой локально
        static: null,
    },
    servant: {
        passport: {
            host: 'blackbox-stress.yandex.net',
        },
        passportInternal: {
            host: 'passport-rc-internal.yandex.ru',
        },
        blackbox: {
            host: 'blackbox-stress.yandex.net',
        },
        avatars: {
            host: 'avatars-int.mdst.yandex.net',
        },
        social: {
            host: 'api.social-test.yandex.ru',
            consumer: 'market_front_blue-testing-stress',
        },
        ocrm: {
            host: 'ow.tst.market.yandex-team.ru',
            consumer: 'market_front_blue-testing-stress',
        },
    },
    cspPolicies: {
        'img-src': [
            'avatars.mdst.yandex.net',
        ],
    },
};
