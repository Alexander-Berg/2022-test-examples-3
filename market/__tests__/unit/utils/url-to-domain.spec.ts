import urlToDomain from '../../../src/utils/url-to-domain';

describe('url to domain', () => {
    it("should return normalized domain from url or undefined value if url isn't valid", () => {
        const testData = [
            { url: 'зоополюс.рф', expected: 'xn--g1aiiaafq7h.xn--p1ai' },
            { url: 'www.зоополюс.рф', expected: 'xn--g1aiiaafq7h.xn--p1ai' },
            { url: 'staff.yandex-team.ru', expected: undefined },
            { url: 'yandex-team.ru', expected: undefined },
            { url: 'news.yandex.ru', expected: undefined },
            { url: 'yandex.ru', expected: undefined },
            { url: 'abakan.rozetka.com.ua', expected: 'rozetka.com.ua' },
            { url: 'www.abakan.rozetka.com.ua', expected: 'rozetka.com.ua' },
            { url: 'yekaterinburg.com.ua', expected: 'com.ua' },
            { url: 'www.example.com:8080', expected: undefined },
            { url: 'example.com:8080', expected: undefined },
            { url: 'www.example.com', expected: 'example.com' },
            { url: 'example.com', expected: 'example.com' },
            { url: 'http://www.example.com:8080', expected: undefined },
            { url: 'https://www.example.com', expected: 'example.com' },
            { url: 'https://www.example.com/path', expected: 'example.com' },
            { url: 'https://example.com/path?a=a', expected: 'example.com' },
        ];

        testData.forEach(({ url, expected }) => {
            const actual = urlToDomain(url);

            expect(actual).toBe(expected);
        });
    });
});
