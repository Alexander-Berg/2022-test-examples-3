const {METRICS} = require('../constants');

module.exports = {
    'default-src': ['\'self\''],
    'object-src': ['\'none\''],
    'base-uri': ['\'none\''],
    'script-src': [
        '<nonce>',

        'yandex.ru',
        'yandex.<tld>',
        'yastatic.net',
        'yastat.net',
        'test.yastat.net',

        'yandex.st',
        'betastatic.yastatic.net',

        'mc.yandex.ru', // Яндекс.Метрика.
        'mc.yandex.<tld>',

        '\'self\'',
        // require, susanin, etc.
        '\'unsafe-eval\'',
        '\'unsafe-inline\'',
        // https://w3c.github.io/webappsec-csp/#strict-dynamic-usage
        '\'strict-dynamic\'',
    ],
    'manifest-src': [
        'yastatic.net',
    ],
    'img-src': [
        '\'self\'',
        'data:',

        '*.yandex.ru',
        '*.yandex.net',
        'yandex.ru',
        'yandex.<tld>',

        ...METRICS,

        'yandex.st',
        'yastatic.net',
        'yastat.net',
        'test.yastat.net',
        'betastatic.yastatic.net',
        'avatars.mds.yandex.net',
        'static.yandex.net',

        'mc.yandex.ru',
        'mc.admetrica.ru',

        'sso.passport.yandex.ru',
    ],
    'style-src': [
        '\'self\'',
        '\'unsafe-inline\'',

        'yastatic.net',
        'yastat.net',
        'test.yastat.net',
        'yandex.st',
        'betastatic.yastatic.net',
    ],
    'connect-src': [
        '\'self\'',

        'yandex.ru',
        '*.yandex.ru',
        'yandex.st',
        'yastatic.net',
        'yastat.net',
        'test.yastat.net',

        ...METRICS,

        'mc.yandex.ru',
        'csp.yandex.net',
    ],
    'frame-src': [
        '\'self\'',

        'yandex.ru',
        '*.yandex.ru',

        'trust.yandex.ru',
        'trust-test.yandex.ru',
        'test.bnpl.yandex.ru',

        'yastatic.net',
        'yastat.net',
        'test.yastat.net',
        'betastatic.yandex.net',
        'betastatic.yastatic.net',

        'mc.yandex.ru',
        'sso.passport.yandex.ru',
    ],
    'child-src': [
        '\'self\'',

        'trust.yandex.ru',
        'trust-test.yandex.ru',
        'test.bnpl.yandex.ru',

        'yastatic.net',
        'yastat.net',
        'test.yastat.net',
        'betastatic.yandex.net',
        'betastatic.yastatic.net',

        'mc.yandex.ru',
        'sso.passport.yandex.ru',
    ],
    'font-src': [
        '\'self\'',
        'data:',

        'yastatic.net',
        'betastatic.yastatic.net',
        'betastatic.yandex.net',
        'yastat.net',
        'test.yastat.net',
    ],
    'frame-ancestors': ['\'none\''],
};
