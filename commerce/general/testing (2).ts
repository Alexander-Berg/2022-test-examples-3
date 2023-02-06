import { AppConfig } from 'yandex-cfg';

import testingCsp from './csp/testing';

const config: AppConfig = {
    csp: {
        presets: testingCsp
    },

    httpGeobase: {
        server: 'http://geobase-test.qloud.yandex.ru'
    },

    httpLangdetect: {
        server: 'http://langdetect-test.qloud.yandex.ru'
    },

    httpUatraits: {
        server: 'http://uatraits-test.qloud.yandex.ru'
    },

    static: {
        baseUrl: '/static',
        frozenPath: '/_',
        version: ''
    },

    api: {
        host: 'expert-api-office-testing.in.yandex-team.ru'
    },

    exams: [
        { id: 1, slug: 'direct', title: 'Сертификация специалистов по Яндекс.Директу' },
        { id: 4, slug: 'metrika', title: 'Сертификация специалистов по Яндекс.Метрике' },
        { id: 24, slug: 'simple-en', title: 'Yandex.Simple certification' },
        { id: 25, slug: 'simple', title: 'Сертификация специалистов simple' },
        { id: 26, slug: 'vip', title: 'Сертификация VIP PROCTORING' },
        { id: 35, slug: 'hello-pro', title: 'Hello c прокторингом' },
        {
            id: 37,
            slug: 'hello',
            title: 'Алгеброй гармонию проверил: тест для знакомства с интерфейсом'
        },
        { id: 38, slug: 'direct-en', title: 'Yandex.Direct certification' },
        { id: 39, slug: 'metrica-en', title: 'Become a Yandex.Metrica certified specialist' },
        { id: 40, slug: 'direct-cn', title: 'Yandex.Direct专家测试' },
        { id: 41, slug: 'market', title: 'Сертификация специалистов по Яндекс.Маркету' },
        { id: 42, slug: 'publisher', title: 'Сертификация специалистов по AdFox' },
        { id: 43, slug: 'rsya', title: 'Сертификация специалистов по РСЯ' },
        { id: 44, slug: 'direct-pro', title: 'Сертификация по Яндекс.Директу с прокторингом' },
        { id: 45, slug: 'cpm', title: 'Тестирование по медийной рекламе Яндекса' },
        { id: 46, slug: 'zen', title: 'Сертификация специалистов по Яндекс.Дзену' },
        { id: 47, slug: 'msp', title: 'Тест на понимание основ продвижения бизнеса в интернете' },
        { id: 48, slug: 'direct-base', title: 'Сертификация по Яндекс.Директу (тестирование)' },
        { id: 49, slug: 'market-base', title: 'Сертификация по Яндекс Маркету (тестирование)' },
        { id: 50, slug: 'rmp', title: 'Сертификация по рекламе приложений' }
    ]
};

module.exports = config;
