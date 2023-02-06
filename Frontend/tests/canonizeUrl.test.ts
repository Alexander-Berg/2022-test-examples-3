import { EPage } from '@src/typings/router';
import { canonizeUrl } from '../canonizeUrl';

describe('canonizeUrl', () => {
    it('Ничего не делает с url-ом без параметров', () => {
        const url = 'https://yandex.ru/products/search/';
        const canonicalUrl = canonizeUrl(url, EPage.SEARCH);
        expect(canonicalUrl).toBe(url);
    });

    it('Вырезает лишние параметры из url-а', () => {
        const url = 'https://yandex.ru/products/search?a=1&b=2&text=iphone&c=3';
        const canonicalUrl = canonizeUrl(url, EPage.SEARCH);
        expect(canonicalUrl).toBe('https://yandex.ru/products/search?text=iphone');
    });
});
