import { getIntervals } from '../getIntervals';

const arr1 = [4, 5, 6];
const arr2 = [6, 7, 8];
const arr3 = [21, 22, 23, 24];
const arr4 = [31, 32, 33, 34];

describe('getIntervals', () => {
    it('Корректный результат для пустого массива', () => {
        expect(getIntervals([])).toEqual([]);
    });

    it('Корректный результат для подряд идущих чисел', () => {
        expect(getIntervals(arr1)).toEqual([arr1]);
    });

    it('Корректный результат для неупорядоченного массива', () => {
        expect(getIntervals(
            [...arr1].sort(() => Math.random() - 0.5),
        )).toEqual([arr1]);
    });

    it('Корректный результат для интервала из одного числа', () => {
        expect(getIntervals(
            [...arr1, 9],
        )).toEqual([arr1, [9]]);
    });

    it('Корректный результат для массивов с пересечениями', () => {
        expect(getIntervals(
            [...arr1, ...arr2],
        )).toEqual([
            [...arr1, ...arr2].filter((v, i, a) => a.indexOf(v) === i),
        ]);
    });

    it('Корректный результат для двух непересекающихся интервалов', () => {
        expect(getIntervals([
            ...arr1,
            ...arr3,
        ])).toEqual([
            arr1,
            arr3,
        ]);
    });

    it('Корректный результат для трёх непересекающихся интервалов', () => {
        expect(getIntervals([
            ...arr1,
            ...arr3,
            ...arr4,
        ])).toEqual([
            arr1,
            arr3,
            arr4,
        ]);
    });
});
