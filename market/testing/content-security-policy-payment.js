const {COMMON_INTERNAL_RESOUCES, METRICS} = require('../constants');

module.exports = {
    'default-src': ['\'self\''],
    'object-src': ['\'none\''],
    'base-uri': ['\'none\''],
    'script-src': [
        '<nonce>',

        'yandex.ru',
        'yandex.<tld>',
        'yandex.st',

        'yastatic.net',
        'yastat.net',
        'betastatic.yastatic.net',
        'test.yastat.net',

        'mc.yandex.ru', // Яндекс.Метрика.
        'mc.yandex.<tld>',

        '\'self\'',
        // require, susanin, etc.
        '\'unsafe-eval\'',
        '\'unsafe-inline\'',
        // https://w3c.github.io/webappsec-csp/#strict-dynamic-usage
        '\'strict-dynamic\'',
    ],
    'img-src': [
        '\'self\'',

        'data:',

        '*.yandex.ru',
        '*.yandex.<tld>',
        '*.yandex.net',
        'yandex.ru',
        'yandex.<tld>',

        'yandex.st',
        'yastatic.net',
        'yastat.net',
        'betastatic.yastatic.net',
        'static.yandex.net',
        'test.yastat.net',
        'avatars.mds.yandex.net',

        ...METRICS,
        'mc.admetrica.ru',

        'sso.passport.yandex.ru',
    ],
    'style-src': [
        '\'self\'',
        '\'unsafe-inline\'',

        'yastatic.net',
        'yastat.net',
        'yandex.st',
        'betastatic.yastatic.net',
        'test.yastat.net',
    ],
    'connect-src': [
        '\'self\'',

        ...COMMON_INTERNAL_RESOUCES,

        ...METRICS,

        'yandex.st',
        'yastatic.net',
        'yastat.net',
        'betastatic.yandex.net',

        'csp.yandex.net',
    ],
    'frame-src': [
        '\'self\'',

        ...COMMON_INTERNAL_RESOUCES,
        ...METRICS,

        'trust.yandex.ru',
        'trust-test.yandex.ru',
        'test.bnpl.yandex.ru',

        'betastatic.yandex.net',
        'yastatic.net',
        'yastat.net',
        'test.yastat.net',

        'sso.passport.yandex.ru',
    ],
    'child-src': [
        '\'self\'',
        'trust.yandex.ru',
        'trust-test.yandex.ru',
        'test.bnpl.yandex.ru',

        'yastatic.net',
        'test.yastat.net',
        'betastatic.yastatic.net',
        ...METRICS,
        'sso.passport.yandex.ru',
    ],
    'font-src': [
        '\'self\'',
        'data:',
        'yastatic.net',
        'yastat.net',
        'test.yastat.net',
        'betastatic.yastatic.net',
    ],
    'frame-ancestors': ['\'none\''],
};
