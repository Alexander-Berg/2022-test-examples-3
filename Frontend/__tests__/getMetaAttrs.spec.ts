import { getMetaDescription } from '../getMetaDescription';
import { getMetaTitle } from '../getMetaTitle';
import { getCanonicalUrl } from '../getCanonicalUrl';

describe('getMetaDescription', () => {
    it('Должен вернуть дескрипшн, соответствующий типу', () => {
        const moduleUnderTest = getMetaDescription('Название_книги', {
            nominative: 'книга',
            accusative: 'книгу',
            prepositional: 'книге',
            genitive: 'книги',
        });

        expect(moduleUnderTest).toEqual('Выберите книгу Название_книги в интернет-магазине' +
            ' по отзывам, характеристикам, ценам и стоимости доставки по России.' +
            ' Купите книгу Название_книги на Яндекс.Маркете выгодно!');
    });
});

describe('getMetaTitle', () => {
    it('Должен вернуть title, соответствующий типу', () => {
        const moduleUnderTest = getMetaTitle('Название_товара', {
            nominative: 'товар',
            accusative: 'товар',
            prepositional: 'товаре',
            genitive: 'товара',
        });

        expect(moduleUnderTest).toEqual('Купить товар Название_товара по' +
            ' низкой цене с доставкой из Яндекс.Маркета');
    });
});

describe('getCanonicalUrl', () => {
    it('Должен вернуть canonical', () => {
        const moduleUnderTest = getCanonicalUrl('123', 'telefon-iphone');

        expect(moduleUnderTest).toEqual(
            'https://pokupki.market.yandex.ru/product/telefon-iphone/123'
        );
    });
});
