const {COMMON_INTERNAL_RESOUCES, METRICS} = require('../constants');

module.exports = {
    'default-src': [
        // Нужен для использования Blob данных в воркерах воркерах векторной карты в Safari.
        // Данный браузер не поддерживает стандартную директиву worker-src.
        'blob:',
    ],
    'base-uri': ['\'none\''],
    'script-src': [
        '<nonce>',

        'yandex.ru',
        'yandex.<tld>',

        'yastatic.net',
        'yastat.net',

        'yandex.st',
        'betastatic.yastatic.net',
        'social.yandex.<tld>',
        'social-rc.yandex.<tld>',
        'social-test.yandex.<tld>',

        'mc.yandex.ru',
        'api-maps.yandex.ru',
        'tune.yandex.ru',
        'suggest-maps.yandex.ru',
        'an.yandex.ru',

        'mc.yandex.<tld>',
        'api-maps.yandex.<tld>',
        'tune.yandex.<tld>',
        'suggest-maps.yandex.<tld>',
        'an.yandex.<tld>',

        'chat.s3.yandex.net',

        '\'self\'',
        '\'unsafe-eval\'', // require, susanin, etc.

        // https://w3c.github.io/webappsec-csp/#strict-dynamic-usage
        '\'strict-dynamic\'',
    ],
    'img-src': [
        '\'self\'',
        'data:',
        'blob:',

        '*.yandex.ru',
        '*.yandex.<tld>',
        '*.yandex.net',
        'yandex.ru',
        'yandex.<tld>',

        ...METRICS,
        'mc.webvisor.org',
        'mc.webvisor.com',

        'yandex.st',
        'yastatic.net',
        'yastat.net',
        'betastatic.yastatic.net',
        'avatars.mds.yandex.net',

        'www.tns-counter.ru',
        'ar.tns-counter.ru',

        'ads.adfox.ru',
        'ads6.adfox.ru',
        'banners.adfox.ru',
        'matchid.adfox.yandex.ru',
        'fenek.market.yandex.ru',
        'fox.market.yandex.ru',

        'ads.adfox.<tld>',
        'ads6.adfox.<tld>',
        'banners.adfox.<tld>',
        'matchid.adfox.yandex.<tld>',
        'fenek.market.yandex.<tld>',
        'fox.market.yandex.<tld>',

        'bam.nr-data.net',

        'mc.admetrica.ru',
        'ad.doubleclick.net',
        /**
         * @expFlag all_uxfeedback_iframe_b2c
         * @ticket MARKETFRONT-60973
         * @start
         */
        'widget.uxfeedback.ru',
        /**
         * @expFlag all_uxfeedback_iframe_b2c
         * @ticket MARKETFRONT-60973
         * @end
         */
    ],
    'style-src': [
        '\'self\'',
        '\'unsafe-inline\'',
        'blob:',
        'yastatic.net',
        'yastat.net',
        'yandex.st',
        'betastatic.yastatic.net',

        // Для работы remote-виджетов
        'https://*.beru.ru',
        'https://*.yandex.ru',
        'https://*.beru.<tld>',
        'https://*.yandex.<tld>',
    ],
    'connect-src': [
        '\'self\'',
        'data:',

        ...COMMON_INTERNAL_RESOUCES,

        'yandex.st',
        'yastatic.net',
        'yastat.net',
        'betastatic.yandex.net',

        ...METRICS,

        'mail.yandex.<tld>',

        'csp.yandex.net',

        'api-ext.vh.yandex.net',

        'social.yandex.<tld>',
        'social-rc.yandex.<tld>',
        'social-test.yandex.<tld>',

        'bam.nr-data.net',

        'mc.admetrica.ru',
        'ad.doubleclick.net',

        // проксирование метрики
        'mc.webvisor.org',
        /**
         * @expFlag all_uxfeedback_iframe_b2c
         * @ticket MARKETFRONT-60973
         * @start
         */
        'api-ym.uxfeedback.ru',
        /**
         * @expFlag all_uxfeedback_iframe_b2c
         * @ticket MARKETFRONT-60973
         * @end
         */
    ],
    'frame-src': [
        '\'self\'',
        'blob:',

        ...COMMON_INTERNAL_RESOUCES,

        'betastatic.yandex.net',
        'awaps.yandex.net',
        'yandexadexchange.net',
        '*.yandexadexchange.net',
        'yastatic.net',
        'yastat.net',

        // Для проекта Кредитного брокера MARKETFRONT-72243
        'forma.tinkoff.ru',

        'kiks.yandex.<tld>',
        'www.youtube-nocookie.com',
        'www.youtube.com',
        'odna.co',
        // нужно для реализации спецпроекта https://st.yandex-team.ru/SMARTREACH-481
        'anketa.alfabank.ru',
    ],
    'font-src': [
        '\'self\'',
        'data:',
        'yastatic.net',
        'yastat.net',
        'betastatic.yastatic.net',
    ],
    'media-src': [
        '*.yandex.net',
        'yandex.st',
        'yastatic.net',
        'yastat.net',
    ],
    'frame-ancestors': [
        'metrika.yandex.ru',
        'metrika.yandex.by',
        'metrica.yandex.com',
        'metrica.yandex.com.tr',
        'market.yandex.ru',
        '*.market.yandex.ru',
        'webvisor.com',
        '*.webvisor.com',
        'http://webvisor.com',
        '*.mtproxy.yandex.net',
        // Заявка в Кредитном брокере MARKETFRONT-72243
        '*.market.yandex.ru',
        'trust-test.yandex.ru',
        // Прогнозатор (promo embed)
        'yandex.ru',
        // Landing page constructor
        'lp-constructor.yandex-team.ru',
    ],
};
