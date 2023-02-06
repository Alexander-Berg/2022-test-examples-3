import { getLinkData } from '@yandex-turbo/components/Link/utils/getLinkData';

describe('Link. Utils', () => {
    describe('getLinkData', () => {
        describe('turboSiteRe', () => {
            [
                'https://qwerty.turbo.site/',
                'http://qwerty.turbo.site/',
                '//qwerty.turbo.site/',
                '//qwerty.turbo.site/page12345',
                '//qwerty.turbo.site/custom_page_url',
                '//qwerty.turbo.site/custom_page_url?foo=bar',
                '//qwerty.turbo.site/?foo=bar',
            ].forEach(url => {
                it(`should return target="_self" for ${url}`, () => {
                    testUrl(url, '_self');
                });
            });

            [
                'https://turbo.site/',
                'http://turbo.site/',
                '//turbo.site/',
            ].forEach(url => {
                it(`should return target="_blank" for ${url}`, () => {
                    testUrl(url, '_blank');
                });
            });

            function testUrl(url: string, target: string) {
                expect(getLinkData({ url })).toEqual({ target, url });
            }
        });

        it('Должен добавлять параметры из searchParams', () => {
            const { url } = getLinkData({ url: '//ya.ru', searchParams: { foo: '1', bar: '2' } });
            expect(url).toBe('//ya.ru/?foo=1&bar=2');
        });

        it('Должен оставить якорь как есть', () => {
            const { url } = getLinkData({ url: '#anchor' });
            expect(url).toBe('#anchor');
        });
    });
});
