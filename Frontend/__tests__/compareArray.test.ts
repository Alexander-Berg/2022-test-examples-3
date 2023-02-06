import { compareArray } from '../compareArray';

describe('#compareArray', () => {
    it('Дожен возвращать false из-за разнице в размере', () => {
        const arr1 = [1, 2, 3];
        const arr2 = [1, 2, 3, 4, 5];

        expect(compareArray(arr1, arr2)).toBeFalsy();
    });

    it('Должен возвращать false из-за несовпадения значения в массиве', () => {
        const arr1 = [1, 2, 3];
        const arr2 = [1, 2, 2];

        expect(compareArray(arr1, arr2)).toBeFalsy();
    });

    it('Должен возвращать true если оба массива пусты', () => {
        const arr1 = [];
        const arr2 = [];

        expect(compareArray(arr1, arr2)).toBeTruthy();
    });

    it('Должен возвращать true если значения и размеры массивов одинаковы', () => {
        const arr1 = [1, 2, 3];
        const arr2 = [1, 2, 3];

        expect(compareArray(arr1, arr2)).toBeTruthy();
    });
});
