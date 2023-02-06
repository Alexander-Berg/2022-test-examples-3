const {METRICS} = require('../../constants');

module.exports = {
    // MARKETFRONTECH-822 для поддержки prefetch, пока не придёт время prefetch-src
    'default-src': [
        '\'self\'',
        'yastatic.net',
        'yastat.net',
        'test.yastat.net',
        'avatars.mdst.yandex.net',
        // Нужен для использования Blob данных в воркерах на АМП страницах в Safari.
        // Данный браузер не поддерживает стандартную директиву worker-src.
        'blob:',
    ],
    'object-src': ['\'none\''],
    'base-uri': ['\'self\'', 'yastatic.net'],
    // /MARKETFRONTECH-822
    'script-src': [
        '<nonce>',

        'yandex.ru',
        'yandex.<tld>',
        'yastatic.net',
        'yastat.net',
        'test.yastat.net',

        'yandex.st',
        'betastatic.yastatic.net',
        'social.yandex.<tld>',
        'social-rc.yandex.<tld>',
        'social-test.yandex.<tld>',

        'js-agent.newrelic.com',
        'bam.nr-data.net',

        'https://chat.s3.yandex.net',

        'mc.yandex.ru',
        'api-maps.yandex.ru',
        'tune.yandex.ru',

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
        '*.yandex.net',
        'yandex.ru',
        'yandex.<tld>',

        ...METRICS,
        'mc.webvisor.org',
        'mc.admetrica.ru',
        'ad.doubleclick.net',
        'mc.webvisor.org',

        'yandex.st',
        'yastatic.net',
        'yastat.net',
        'test.yastat.net',
        'betastatic.yastatic.net',
        'avatars.mds.yandex.net',

        'www.tns-counter.ru',
        'ar.tns-counter.ru',

        'ads.adfox.ru',
        'ads6.adfox.ru',
        'banners.adfox.ru',
        'matchid.adfox.yandex.ru',
        'fenek.market.yandex.ru',
        'fenek.market.yandex.<tld>',
        'fox.market.yandex.ru',
        'fox.market.yandex.<tld>',

        'bam.nr-data.net',

        'i.ytimg.com',

        'pic.rutube.ru',

        'strm.yandex.net',
        'strm.yandex.ru',
    ],
    'style-src': [
        '\'self\'',
        '\'unsafe-inline\'',
        'blob:',
        'yastatic.net',
        'yastat.net',
        'test.yastat.net',
        'yandex.st',
        'betastatic.yastatic.net',

        // Для работы remote-виджетов
        'https://*.beru.ru',
        'https://*.yandex.ru',

        'strm.yandex.ru',
    ],
    'connect-src': [
        '\'self\'',
        'http://market.yandex.ru',
        'data:',

        'yandex.ru',
        '*.yandex.ru',
        'yandex.st',
        'yastatic.net',
        'yastat.net',
        'test.yastat.net',

        'mc.yandex.ru',
        'mc.yandex.ua',
        'mc.yandex.by',
        'mc.yandex.kz',
        'mc.yandex.com.tr',
        'mc.yandex.com',
        'mc.webvisor.org',
        'mc.admetrica.ru',
        'ad.doubleclick.net',

        // Геосаджест на странице смены региона
        'https://suggest-maps.yandex.ru',
        'https://suggest-maps.yandex.<tld>',

        'mail.yandex.<tld>',

        'csp.yandex.net',

        'social.yandex.<tld>',
        'social-rc.yandex.<tld>',
        'social-test.yandex.<tld>',

        'bam.nr-data.net',

        // FIXME: @see {@link https://st.yandex-team.ru/MOBMARKET-6951}
        'http://market.yandex.ru',
        'http://market.yandex.<tld>',

        // синк метрик между тач версией сайта и нативным приложением
        'http://127.0.0.1:30102',
        'http://127.0.0.1:29009',
        'yandexmetrica.com:30103',
        'yandexmetrica.com:29010',

        // видеохостинг
        'https://*.cdn.yandex.net',
        'strm.yandex.net',
        '*.strm.yandex.net',
        'api-ext.vh.yandex.net',

        // проксирование метрики
        'mc.webvisor.org',

        // Для подключения AMP воркера
        'https://cdn.ampproject.org/rtv/',
    ],
    'frame-src': [
        '\'self\'',
        'blob:',
        'yandex.ru',
        '*.yandex.ru',

        /**
         * @expFlag all_uxfeedback_iframe_b2c
         * @ticket MARKETFRONT-60973
         * @nextLine
         */
        'static.yandex.net',

        'betastatic.yandex.net',
        'betastatic.yastatic.net', // Для iframe'а Траста в Чекауте
        'awaps.yandex.net',
        'yandexadexchange.net',
        '*.yandexadexchange.net',
        'yastatic.net',
        'yastat.net',
        'test.yastat.net',

        '3ds.yamoney.ru',

        /**
         * @expFlag all_credit_broker
         * @ticket MARKETFRONT-72243
         * @nextLine
         */
        'forma.tinkoff.ru',

        'kiks.yandex.<tld>',
        'www.youtube-nocookie.com',
        'www.youtube.com',
        'frontend.vh.yandex.ru',
        // нужно для реализации спецпроекта https://st.yandex-team.ru/SMARTREACH-481
        'anketa.alfabank.ru',
    ],
    'font-src': [
        '\'self\'',
        'data:',
        'yastatic.net',
        'betastatic.yastatic.net',
        'yastat.net',
        'test.yastat.net',
    ],
    'media-src': [
        '*.yandex.net',
        'yandex.st',
        'yastatic.net',
        'yastat.net',
        'test.yastat.net',
        'data:',

        'strm.yandex.ru',
        '*.strm.yandex.ru',
        'strm.yandex.net',
        '*.strm.yandex.net',
        'blob:',
    ],
    'prefetch-src': [
        '\'self\'',
        'yastatic.net',
        'yastat.net',
        'test.yastat.net',
        'avatars.mdst.yandex.net',
    ],
    'worker-src': [
        '\'self\'',

        'strm.yandex.ru',
        '*.strm.yandex.ru',
        'strm.yandex.net',
        '*.strm.yandex.net',
        'blob:',
        'data:',
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
        // Прогнозатор (promo embed)
        'yandex.ru',
        // Landing page constructor
        'lp-constructor.yandex-team.ru',
    ],
};
