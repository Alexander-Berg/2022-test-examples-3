'use strict';

module.exports = {
    env: 'testing',
    frontend: {
        startrekUrl: '//st.test.yandex-team.ru',
        sudgestAnswerUrl: '//search.test.yandex-team.ru/_api/api/v1/suggest-answer/',
    },

    backend: {
        abovemeta: {
            ru: {
                hostname: process.env.BACKEND_HOST || 'search-back.test.yandex-team.ru',
            },
            net: {
                hostname: process.env.BACKEND_HOST || 'search-back.test.yandex-team.ru',
            },
        },
        availability: {
            hostname: 'calendar-api.calendar-prestable.yandex-team.ru',
        },
        invite: {
            hostname: 'calendar-api.calendar-prestable.yandex-team.ru',
        },
        avatars: {
            hostname: 'avatars.mdst.yandex.net',
        },
        yapic: {
            hostname: 'yapic-test.yandex.ru',
        },
        accounts: {
            ru: {
                hostname: 'pass-test.yandex.ru',
            },
            com: {
                hostname: 'pass-test.yandex.com',
            },
        },
        yamb: {
            ru: {
                hostname: 'yamb-test.yandex.ru',
            },
            com: {
                hostname: 'yamb-test.yandex.com',
            },
        },
        directory: {
            hostname: 'api-internal-test.directory.ws.yandex.net',
        },
        connect: {
            ru: {
                hostname: 'connect-test.ws.yandex.ru',
            },
            com: {
                hostname: 'connect-test.ws.yandex.com',
            },
        },
        notifier: {
            hostname: 'api-stable.dst.yandex-team.ru',
        },
    },

    logging: {
        console: {
            level: 'debug',
        },
    },
};
