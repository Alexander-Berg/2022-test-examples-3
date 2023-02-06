module.exports = {
    debug: true,

    hosts: {
        notifier: {
            hostname: 'api-stable.dst.yandex-team.ru',
        },
        abc: { rejectUnauthorized: false },
        d: {
            rejectUnauthorized: false,
            hostname: 'd.test.yandex-team.ru',
        },
        dispenser: {
            rejectUnauthorized: false,
            hostname: 'dispenser.test.yandex-team.ru',
        },
        tracker: {
            protocol: 'https:',
            hostname: 'st.test.yandex-team.ru',
        },
        bot: {
            protocol: 'https:',
            hostname: 'test.bot.yandex-team.ru',
        },
        staff: {
            protocol: 'https:',
            hostname: 'staff.test.yandex-team.ru',
        },
        search: {
            rejectUnauthorized: false,
            protocol: 'https:',
            hostname: 'search-back.test.yandex-team.ru',
            port: 443,
        },
        wfaas: {
            rejectUnauthorized: false,
            protocol: 'https:',
            hostname: 'wfaas.yandex-team.ru',
            port: 443,
        },
        wfaasClient: {
            rejectUnauthorized: false,
            protocol: 'https:',
            hostname: 'wfaas.yandex-team.ru',
            port: 443,
        },
        startrek: {
            protocol: 'https:',
            hostname: 'st-api.test.yandex-team.ru',
        },
    },

    rum: {
        settings: {
            env: 'testing',
            debug: true,
        }
    },

    errorCounter: {
        env: 'testing',
        debug: true,
    },
};
