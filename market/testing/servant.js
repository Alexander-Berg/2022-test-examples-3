module.exports = {
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
    blackbox: {
        // Хост совпадает с хостом паспорта.
        // В зависимости от параметров вызова берётся тестовый или обычный.
        path: '/blackbox/',
        data: {
            traceServiceId: 'passport_blackbox',
        },
    },

    staff: {
        host: 'staff-api.test.yandex-team.ru',
        path: '/v3',
        data: {
            traceServiceId: 'staff',
        },
    },
};
