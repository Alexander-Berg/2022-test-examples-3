const basePolicies = require('../csp/basePolicies');
const mergePolicies = require('../csp/mergePolicies');

module.exports = mergePolicies(basePolicies, {
    'script-src': ['betastatic.yastatic.net'],

    'style-src': ['betastatic.yastatic.net'],

    'img-src': ['betastatic.yastatic.net'],

    'font-src': ['betastatic.yastatic.net'],
    'prefetch-src': [
        // Чат поддержки (Yandex Messenger)
        'renderer-chat-dev.hamster.yandex.ru',
    ],
    'child-src': [],
    'frame-src': [
        // Чат поддержки (Yandex Messenger)
        'renderer-chat-dev.hamster.yandex.ru',
    ],
    'form-action': [
        // FIXME: хотелось бы брать из конфига
        'user-balance.greed-tc.paysys.yandex.ru',
        'user-balance.greed-ts.paysys.yandex.ru',
        'user-balance.greed-tm.paysys.yandex.ru',
    ],
});
