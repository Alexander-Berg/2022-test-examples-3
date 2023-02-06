import {swapTwoElementsInList} from '../../arrays';

const testObj1 = {test: '1'};
const testObj2 = {test: '2'};

const testNumbersList = [1, 2, 3];
const testObjectsList = [testObj1, testObj2];

describe('Тестирование утилиты списка: swapTwoElementsInList', () => {
    it('Возвращает начальный список при передаче некорректного индекса первого элемента', () => {
        expect(swapTwoElementsInList(testNumbersList, -1, 1)).toEqual(testNumbersList);
        expect(swapTwoElementsInList(testNumbersList, -10, 1)).toEqual(testNumbersList);
        expect(swapTwoElementsInList(testNumbersList, 3, 1)).toEqual(testNumbersList);
        expect(swapTwoElementsInList(testNumbersList, 10, 1)).toEqual(testNumbersList);

        expect(swapTwoElementsInList(testObjectsList, -1, 1)).toEqual(testObjectsList);
        expect(swapTwoElementsInList(testObjectsList, -10, 1)).toEqual(testObjectsList);
        expect(swapTwoElementsInList(testObjectsList, 2, 1)).toEqual(testObjectsList);
        expect(swapTwoElementsInList(testObjectsList, 10, 1)).toEqual(testObjectsList);
    });

    it('Возвращает начальный список при передаче некорректного индекса второго элемента', () => {
        expect(swapTwoElementsInList(testNumbersList, 1, -1)).toEqual(testNumbersList);
        expect(swapTwoElementsInList(testNumbersList, 1, -10)).toEqual(testNumbersList);
        expect(swapTwoElementsInList(testNumbersList, 1, 3)).toEqual(testNumbersList);
        expect(swapTwoElementsInList(testNumbersList, 1, 10)).toEqual(testNumbersList);

        expect(swapTwoElementsInList(testObjectsList, 1, -1)).toEqual(testObjectsList);
        expect(swapTwoElementsInList(testObjectsList, 1, -10)).toEqual(testObjectsList);
        expect(swapTwoElementsInList(testObjectsList, 1, 2)).toEqual(testObjectsList);
        expect(swapTwoElementsInList(testObjectsList, 1, 10)).toEqual(testObjectsList);
    });

    it('Возвращает начальный список при передаче корректных одинаковых индексов', () => {
        expect(swapTwoElementsInList(testNumbersList, 0, 0)).toEqual(testNumbersList);
        expect(swapTwoElementsInList(testNumbersList, 1, 1)).toEqual(testNumbersList);
        expect(swapTwoElementsInList(testNumbersList, 2, 2)).toEqual(testNumbersList);

        expect(swapTwoElementsInList(testObjectsList, 0, 0)).toEqual(testObjectsList);
        expect(swapTwoElementsInList(testObjectsList, 1, 1)).toEqual(testObjectsList);
    });

    it('Возвращает измененный список при передаче корректных не одинаковых индексов', () => {
        expect(swapTwoElementsInList(testNumbersList, 0, 2)).toEqual([3, 2, 1]);
        expect(swapTwoElementsInList(testNumbersList, 0, 1)).toEqual([2, 1, 3]);
        expect(swapTwoElementsInList(testNumbersList, 1, 0)).toEqual([2, 1, 3]);
        expect(swapTwoElementsInList(testNumbersList, 1, 2)).toEqual([1, 3, 2]);
        expect(swapTwoElementsInList(testNumbersList, 2, 0)).toEqual([3, 2, 1]);
        expect(swapTwoElementsInList(testNumbersList, 2, 1)).toEqual([1, 3, 2]);

        expect(swapTwoElementsInList(testObjectsList, 0, 1)).toEqual([testObj2, testObj1]);
        expect(swapTwoElementsInList(testObjectsList, 1, 0)).toEqual([testObj2, testObj1]);
    });
});
