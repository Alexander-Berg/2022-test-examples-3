jest.disableAutomock();

import {getAlternateLanguageLinks} from '../../altLinks';

const page = {
    location: {
        protocol: 'https:',
        path: '/trains',
        pathname: '/trains',
    },
};

describe('getAlternateLanguageLinks', () => {
    it('Альтернативные ссылки на национальные домена', () => {
        expect(getAlternateLanguageLinks({page})).toEqual([
            {
                href: 'https://rasp.yandex.ru/trains',
                hreflang: 'ru',
                rel: 'alternate',
            },
            {
                href: 'https://rasp.yandex.ua/trains',
                hreflang: 'uk',
                rel: 'alternate',
            },
            {
                href: 'https://rasp.yandex.ua/trains?lang=ru',
                hreflang: 'ru-UA',
                rel: 'alternate',
            },
            {
                href: 'https://rasp.yandex.by/trains',
                hreflang: 'ru-BY',
                rel: 'alternate',
            },
            {
                href: 'https://rasp.yandex.kz/trains',
                hreflang: 'ru-KZ',
                rel: 'alternate',
            },
            {
                href: 'https://rasp.yandex.uz/trains',
                hreflang: 'ru-UZ',
                rel: 'alternate',
            },
        ]);
    });

    it('Альтернативные ссылки на национальные домена. Явно передан path', () => {
        expect(getAlternateLanguageLinks({page, path: '/thread/uid'})).toEqual([
            {
                href: 'https://rasp.yandex.ru/thread/uid',
                hreflang: 'ru',
                rel: 'alternate',
            },
            {
                href: 'https://rasp.yandex.ua/thread/uid',
                hreflang: 'uk',
                rel: 'alternate',
            },
            {
                href: 'https://rasp.yandex.ua/thread/uid?lang=ru',
                hreflang: 'ru-UA',
                rel: 'alternate',
            },
            {
                href: 'https://rasp.yandex.by/thread/uid',
                hreflang: 'ru-BY',
                rel: 'alternate',
            },
            {
                href: 'https://rasp.yandex.kz/thread/uid',
                hreflang: 'ru-KZ',
                rel: 'alternate',
            },
            {
                href: 'https://rasp.yandex.uz/thread/uid',
                hreflang: 'ru-UZ',
                rel: 'alternate',
            },
        ]);
    });
});
