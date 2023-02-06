import { getExpectedPrice } from '..';

describe('getExpectedPrice', () => {
    it('должен вернуть значение поискового параметра в виде числа', () => {
        const expectedPrice = 193850;
        const url = new URL(`https://yandex.ru/products/product/1447472428/sku/101495940729/?expected_price=${expectedPrice}`);
        expect(getExpectedPrice(url)).toEqual(expectedPrice);
    });

    it('должен вернуть undefined, если параметра нет', () => {
        const url = new URL('https://yandex.ru/products/product/1447472428/sku/101495940729/');
        expect(getExpectedPrice(url)).toEqual(undefined);
    });

    it('должен вернуть undefined, если значение параметра нельзя привести к числу', () => {
        const url = new URL('https://yandex.ru/products/product/1447472428/sku/101495940729/?expected_price=abc');
        expect(getExpectedPrice(url)).toEqual(undefined);
    });

    it('должен вернуть undefined, если значение параметра равно 0', () => {
        const url = new URL('https://yandex.ru/products/product/1447472428/sku/101495940729/?expected_price=0');
        expect(getExpectedPrice((url))).toEqual(undefined);
    });
});
