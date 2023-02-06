const basePolicies = require('../csp/basePolicies');
const mergePolicies = require('../csp/mergePolicies');

module.exports = mergePolicies(basePolicies, {
    'script-src': [
        // Для работы webpack-dev-server
        'https://localhost:*',
        'betastatic.yastatic.net',
        '*.yandex.ru',
    ],

    'style-src': [
        // Для работы webpack-dev-server
        'https://localhost:*',
        'betastatic.yastatic.net',
        '*.yandex.ru',
    ],

    'img-src': [
        // Для работы webpack-dev-server
        'https://localhost:*',
        'betastatic.yastatic.net',
        '*.yandex.ru',
    ],

    'font-src': ['betastatic.yastatic.net', '*.yandex.ru'],
    'prefetch-src': [
        // Для работы webpack-dev-server
        'https://localhost:*',
        // Чат поддержки (Yandex Messenger)
        'renderer-chat-dev.hamster.yandex.ru',
        '*.yandex.ru',
    ],
    'child-src': [
        // Для работы webpack-dev-server
        'https://localhost:*',
        '*.yandex.ru',
    ],
    'frame-src': [
        // Для работы webpack-dev-server
        'https://localhost:*',
        // Чат поддержки (Yandex Messenger)
        'renderer-chat-dev.hamster.yandex.ru',
        '*.yandex.ru',
    ],
    'form-action': [
        // FIXME: хотелось бы брать из конфига
        'user-balance.greed-tc.paysys.yandex.ru',
        'user-balance.greed-ts.paysys.yandex.ru',
        'user-balance.greed-tm.paysys.yandex.ru',
        '*.yandex.ru',
    ],
    'connect-src': [
        // Для работы webpack-dev-server
        'https://localhost:*',
        'wss://localhost:*',
        '*.yandex.ru',

        // статика
        '*.cdn.yandex.net',
    ],
});
