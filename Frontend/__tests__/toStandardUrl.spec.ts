import { toStandardUrl } from '../toStandardUrl';

describe('utils/toStandardUrl', () => {
    const base = 'base.url';

    it('Only affects relative urls', () => {
        const urls = [
            'https://github.com',
            'https://ya.ru',
            'https://ya.ru/',
            'ws://sample.bla.cla/',
            'http://to',
            'https://to',
            'https://127.0.0.1/',
        ];

        for (const url of urls) {
            expect(toStandardUrl(base, url)).toBe(url);
        }
    });

    it('Prepends baseUrl', () => {
        const path = '/service/pi/search/';
        const path2 = 'service/pi/search/';
        const base2 = 'base.url/';

        expect(toStandardUrl(base, path)).toBe(`${base}${path}`);
        expect(toStandardUrl(base, path2)).toBe(`${base}/${path2}`);
        expect(toStandardUrl(base2, path)).toBe(`${base}${path}`);
    });

    it('Ensures tail / before query in urls', () => {
        const path = 'service/pi/search/';
        const path2 = 'service/pi/search';
        const pathQ = 'service/pi/search/?q=abc';
        const pathQ2 = 'service/pi/search?q=abc';

        expect(toStandardUrl(base, path)).toBe(`${base}/${path}`);
        expect(toStandardUrl(base, path2)).toBe(`${base}/${path2}/`);
        expect(toStandardUrl(base, pathQ)).toBe(`${base}/${pathQ}`);
        expect(toStandardUrl(base, pathQ2)).toBe(`${base}/${pathQ}`); // pathQ is with /
    });

    it('Returns base url if nothing is specified', () => {
        expect(toStandardUrl(base, '')).toBe(`${base}/`);
        expect(toStandardUrl(base, '')).toBe(`${base}/`);
    });
});
