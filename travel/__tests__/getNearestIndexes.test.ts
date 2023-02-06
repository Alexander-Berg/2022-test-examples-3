import getNearestIndexes from 'projects/avia/lib/dynamic/getNearestIndexes';

const arr = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

describe('getNearestIndexes', () => {
    it('Вернет n ближайших индекса', () => {
        expect(getNearestIndexes(arr, 5, 0)).toEqual({start: 5, end: 5});
        expect(getNearestIndexes(arr, 5, 1)).toEqual({start: 4, end: 6});
        expect(getNearestIndexes(arr, 5, 2)).toEqual({start: 3, end: 7});
        expect(getNearestIndexes(arr, 5, 3)).toEqual({start: 2, end: 8});
        expect(getNearestIndexes(arr, 5, 10)).toEqual({start: 0, end: 10});
    });

    it('Добавит к правому индексу недостающие слева', () => {
        expect(getNearestIndexes(arr, 0, 3)).toEqual({start: 0, end: 6});
        expect(getNearestIndexes(arr, 1, 3)).toEqual({start: 0, end: 6});
        expect(getNearestIndexes(arr, 2, 3)).toEqual({start: 0, end: 6});
    });

    it('Добавит элементов слева, если их нет справа', () => {
        expect(getNearestIndexes(arr, 10, 3)).toEqual({start: 4, end: 10});
        expect(getNearestIndexes(arr, 9, 3)).toEqual({start: 4, end: 10});
        expect(getNearestIndexes(arr, 8, 3)).toEqual({start: 4, end: 10});
    });

    it('Не уходит за границы массива', () => {
        expect(getNearestIndexes(arr, 10, 30)).toEqual({start: 0, end: 10});
        expect(getNearestIndexes(arr, 0, 30)).toEqual({start: 0, end: 10});
        expect(getNearestIndexes(arr, 5, 30)).toEqual({start: 0, end: 10});
        expect(getNearestIndexes(arr, 7, 1, 6)).toEqual({start: 3, end: 10});
    });

    it('Корректно обрабатывает ситуация с передачей несуществующего индекса', () => {
        expect(getNearestIndexes(arr, -1, 3)).toEqual({start: 0, end: 6});
        expect(getNearestIndexes(arr, 20, 3)).toEqual({start: 4, end: 10});
    });
});
