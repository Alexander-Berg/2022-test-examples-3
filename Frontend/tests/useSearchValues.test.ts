import { findValues } from '../useSearchValues';

describe('findValues', () => {
    const values = [
        {
            id: '1',
            value: 'ASUS фирменный магазин',
            found: 1,
        },
        {
            id: '2',
            value: 'Boom-Room - Магазин Xiaomi',
            found: 1,
        },
        {
            id: '3',
            value: '-~/?Ё ё shop',
            found: 1,
        },
    ];

    it('должен возвращать корректный результат при полном совпадении', () => {
        expect(findValues(values, 'ASUS фирменный магазин')).toStrictEqual([
            {
                id: '1',
                value: 'ASUS фирменный магазин',
                found: 1,
            },
        ]);
    });

    it('должен возвращать корректный результат при частичном совпадении', () => {
        expect(findValues(values, 'магаз')).toStrictEqual([
            {
                id: '1',
                value: 'ASUS фирменный магазин',
                found: 1,
            },
            {
                id: '2',
                value: 'Boom-Room - Магазин Xiaomi',
                found: 1,
            },
        ]);
    });

    it('должен игнорировать пробелы', () => {
        expect(findValues(values, 'ыймаг')).toStrictEqual([
            {
                id: '1',
                value: 'ASUS фирменный магазин',
                found: 1,
            },
        ]);
    });

    it('должен учитывать букву Ё в поиске', () => {
        expect(findValues(values, 'ёЁ')).toStrictEqual([
            {
                id: '3',
                value: '-~/?Ё ё shop',
                found: 1,
            },
        ]);
    });

    it('должен возвращать корректный результат при несовпадении', () => {
        expect(findValues(values, 'sas')).toStrictEqual([]);
    });
});
