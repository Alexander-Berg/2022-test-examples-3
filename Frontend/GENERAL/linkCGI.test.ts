import { getWhitelistedParams, addParams } from './linkCGI';

describe('modifyLinks', () => {
    it('Получаем все параметры, которые надо сохранить', () => {
        expect(getWhitelistedParams('', ['a', 'b', 'c'])).toEqual({});
        expect(getWhitelistedParams('?text=phone', ['a', 'b', 'c'])).toEqual({});
        expect(getWhitelistedParams('?text=phone', ['a', 'b', 'c'])).toEqual({});
        expect(getWhitelistedParams('?text=phone&a=enable_sorting=1', ['a', 'b', 'c'])).toEqual({ a: 'enable_sorting=1' });
        expect(getWhitelistedParams('?text=phone&a=enable_sorting=1&c=qwe', ['a', 'b', 'c'])).toEqual({ a: 'enable_sorting=1', c: 'qwe' });
        expect(getWhitelistedParams('?text=phone&a=enable_sorting=1&one=1&two=2', ['a', 'b', 'c'])).toEqual({ a: 'enable_sorting=1' });
    });

    it('Параметры для сохранения переносятся с текущей ссылки на целевую', () => {
        const WHITELIST_FOR_TEST = [
            'exp_flags',
            'test-id',
            'test-mode',
            'promo',
        ];

        expect(addParams('/products?text=phone', '?exp_flags=enable_sorting=1&one=1&two=2', WHITELIST_FOR_TEST)).toBe('/products?text=phone&exp_flags=enable_sorting%3D1');
        expect(addParams('/products', '?exp_flags=enable_sorting=1&one=1&two=2', WHITELIST_FOR_TEST)).toBe('/products?exp_flags=enable_sorting%3D1');
        expect(addParams('/products', '?exp_flags=enable_sorting=1&one=1&two=2')).toBe('/products?exp_flags=enable_sorting%3D1');
        expect(addParams('/products', '?text=phone', WHITELIST_FOR_TEST)).toBe('/products');
        expect(addParams('/products?text=qwe', '', WHITELIST_FOR_TEST)).toBe('/products?text=qwe');
        expect(addParams('/products', '', WHITELIST_FOR_TEST)).toBe('/products');
    });
});
