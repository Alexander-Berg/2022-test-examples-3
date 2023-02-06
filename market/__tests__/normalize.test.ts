import { normalize } from '../src';

describe('normalize', () => {
    const testData = [
        { url: '', expected: undefined },
        { url: 'зоополюс.рф', expected: 'xn--g1aiiaafq7h.xn--p1ai' },
        { url: 'www.зоополюс.рф', expected: 'xn--g1aiiaafq7h.xn--p1ai' },
        { url: 'staff.yandex-team.ru', expected: undefined },
        { url: 'yandex-team.ru', expected: undefined },
        { url: 'news.yandex.ru', expected: undefined },
        { url: 'yandex.ru', expected: undefined },
        { url: 'abakan.rozetka.com.ua', expected: 'rozetka.com.ua' },
        { url: 'www.abakan.rozetka.com.ua', expected: 'rozetka.com.ua' },
        { url: 'yekaterinburg.mts.com.ua', expected: 'mts.com.ua' },
        { url: 'www.example.com:8080', expected: undefined },
        { url: 'example.com:8080', expected: undefined },
        { url: 'www.example.com', expected: 'example.com' },
        { url: 'example.com', expected: 'example.com' },
        { url: 'm.example.com', expected: 'm.example.com' },
        { url: 'm.beru.ru', expected: 'm.beru.ru' },
        { url: 'beru.ru', expected: 'beru.ru' },
        { url: 'm.mail.ru', expected: 'm.mail.ru' },
        { url: 'hi-tech.mail.ru', expected: 'hi-tech.mail.ru' },
        { url: 'mail.ru', expected: 'mail.ru' },
        { url: 'http://www.example.com:8080', expected: undefined },
        { url: 'https://www.example.com', expected: 'example.com' },
        { url: 'https://www.example.com/path', expected: 'example.com' },
        { url: 'https://example.com/path?a=a', expected: 'example.com' },
    ];

    testData.forEach(({ url, expected }) => {
        test(`url: "${url}" => ${expected}`, () => {
            const actual = normalize(url);

            expect(actual).toBe(expected);
        });
    });
});
