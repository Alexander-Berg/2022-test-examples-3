import { getHash } from '../src';

describe('getHash', () => {
    const testData = [
        { url: 'mvideo.ru', expected: '226a6474064b6a65' },
        { url: 'www.mvideo.ru', expected: '226a6474064b6a65' },
        { url: 'ok.ru', expected: '190408bf6b9a4921' },
        { url: 'm.beru.ru', expected: '687881f7a226b30f' },
        { url: 'beru.ru', expected: '9467ae6e6eb28722' },
        { url: 'mts.ru', expected: '203d868fac1c024e' },
        { url: 'shop.mts.ru', expected: '7018fa4c3a1368e4' },
        { url: 'super.hello.shop.mts.ru', expected: '7018fa4c3a1368e4' },
        { url: 'mail.ru', expected: 'e94d6d1335dc023e' },
        { url: 'hi-tech.mail.ru', expected: 'f109d6b8a3f42d29' },
    ];

    test('should create correct domainHash', () => {
        testData.forEach(({ url, expected }) => {
            const actual = getHash(url);

            expect(actual).toBe(expected);
        });
    });
});
