import { splitByColumns } from './Carousel';

describe('Функция splitByColumns', () => {
    const array = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

    it('должна возвращать ожидаемый результат, если количество повторений = 0', () => {
        expect(splitByColumns(array, 3, 1)).toEqual(array);
    });

    it('должна возвращать ожидаемый результат, если количество повторений = 1', () => {
        expect(splitByColumns(array, 3, 2)).toEqual([[1, 4], [2, 5], [3, 6], [7, 10], [8], [9]]);
    });

    it('должна возвращать ожидаемый результат, если количество повторений = 2', () => {
        expect(splitByColumns(array, 3, 3)).toEqual([[1, 4, 7], [2, 5, 8], [3, 6, 9], [10], [], []]);
    });
});
