import { deduplicateArray } from '../deduplicateArray';

describe('deduplicateArray', () => {
    const arrWithDuplicates = [0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 6, 7, 8, 9, 8];
    const arrWithoutDuplicates = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9];

    it('Должен убирать дубликаты из массива', () => {
        expect(deduplicateArray(arrWithDuplicates)).toEqual(arrWithoutDuplicates);
    });

    it('При получении пустого массива должен возвращать путой массив', () => {
        expect(deduplicateArray([])).toEqual([]);
    });

    it('При получении массива без дубликатов, должен возвращать массив с теми же значениями', () => {
        expect(deduplicateArray(arrWithoutDuplicates)).toEqual(arrWithoutDuplicates);
    });
});
