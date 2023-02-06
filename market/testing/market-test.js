'use strict';

/**
 * Файл с настройками TVM.
 * Здесь лежат настройки для market-test.* хостов.
 * @see https://st.yandex-team.ru/CSADMIN-13370
 *
 * @type {{id: integer, secret: string, consumer: string}}
 */

module.exports = {
    isTest: true,

    tvm: {
        env: 'market_front_affiliate-testing-testing',
    },

    hosts: {
        passport: 'passport-test.yandex.ru',
        avatarsHost: 'avatars.mdst.yandex.net',
    },

    hostsTld: {
        passport: 'passport-test.yandex.<tld>',
        avatarsHost: 'avatars.mdst.yandex.net',
    },

    servant: {
        passport: {
            host: 'pass-test.yandex.ru',
        },
    },
};
