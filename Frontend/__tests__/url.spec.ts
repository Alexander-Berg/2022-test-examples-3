import { URLSearchParams } from 'url';
import {
    normalizeUrl,
    normalizeSearch,
    normalizePathname,
    appendQueryParamsToUrl,
    ALLOW_KEYS,
} from '../url';

describe('url', () => {
    it('normalizeSearch() отрабатывает с пустой поисковой строкой', () => {
        const params = new URLSearchParams('');
        expect(normalizeSearch(params)).toEqual('');
    });

    describe('normalizeSearch() удаляет параметры, которых нет в белом списке', () => {
        ALLOW_KEYS.forEach(key => {
            it(`normalizeSearch() оставляет только параметр ${key} в поисковой строке`, () => {
                const params = new URLSearchParams(`super-duper=1&${key}=value`);
                expect(normalizeSearch(params)).toEqual(`${key}=value`);
            });
        });
    });

    it('normalizeSearch() сортирует ключи параметров в алфавитном порядке', () => {
        const params = new URLSearchParams('text=1&category_id=2');
        expect(normalizeSearch(params)).toEqual('category_id=2&text=1');
    });

    it('normalizePathname() отрабатывает с пустой строкой', () => {
        expect(normalizePathname('')).toEqual('');
    });

    it('normalizePathname() возвращает исходный путь, если нет никаких совпадений с базовым', () => {
        expect(normalizePathname('/turbo')).toEqual('/turbo');
        expect(normalizePathname('/turbo/farkop.ru/n/')).toEqual('/turbo/farkop.ru/n/');
        expect(normalizePathname('/turbo/farkop.ru/s/product/in/original/123123/')).toEqual('/turbo/farkop.ru/s/product/in/original/123123/');
    });

    describe('normalizePathname() возвращает значимую часть пути', () => {
        const basePathVariants = ['yandexturbocatalog', 'listinghttps', 'yandexturbolisting', 'yandexturbocart'];

        basePathVariants.forEach(variant => {
            it(`Значимая часть пути для ${variant}`, () => {
                expect(
                    normalizePathname(
                        `/turbo/farkop.ru/n/${variant}/additional/path/`
                    )
                ).toEqual(
                    `/turbo/farkop.ru/n/${variant}/`
                );
            });
        });

        it('Значимая часть пути для формы оплаты в один клик', () => {
            expect(
                normalizePathname('/turbo/farkop.ru/n/payment/')
            ).toEqual('/turbo/farkop.ru/n/');
        });
    });

    it('normalizeUrl() нормализует ссылку', () => {
        expect(normalizeUrl('/turbo?super-duper=0&text=1&category_id=2'))
            .toEqual('/turbo?category_id=2&text=1');

        expect(normalizeUrl('/turbo?test=&text=mirm.ru%2Fyandexturbocatalog%2F&exp_flags=turbo-app-for-ecom&category_id=16953'))
            .toEqual('/turbo?category_id=16953&text=mirm.ru%2Fyandexturbocatalog%2F');

        expect(normalizeUrl('/turbo?test=&text=mirm.ru%2Fyandexturbocatalog%2F&exp_flags=turbo-app-for-ecom&category_id=111222333444555666777888999111222333'))
            .toEqual('/turbo?category_id=111222333444555666777888999111222333&text=mirm.ru%2Fyandexturbocatalog%2F');

        expect(normalizeUrl('/turbo/farkop.ru/n/yandexturbocatalog/about/?exp_flags=turbo-app-for-ecom&category_id=16953'))
            .toEqual('/turbo/farkop.ru/n/yandexturbocatalog/?category_id=16953');

        expect(normalizeUrl('/turbo/super01.ru/s/products/kostium-cheloveka-pauka?pcgi=size%3D136&exp_flags=turbo-app-for-ecom&category_id=16953'))
            .toEqual('/turbo/super01.ru/s/products/kostium-cheloveka-pauka?category_id=16953&pcgi=size%3D136');
    });

    it('appendQueryParamsToUrl', () => {
        const URL = 'https://yandex.ru/turbo';
        const params = { a: 1, b: 2 };

        expect(appendQueryParamsToUrl(URL, params)).toEqual('https://yandex.ru/turbo?a=1&b=2');
        expect(appendQueryParamsToUrl(URL + '?x=2', params)).toEqual('https://yandex.ru/turbo?x=2&a=1&b=2');
    });
});
