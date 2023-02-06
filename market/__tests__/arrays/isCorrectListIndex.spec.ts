import {isCorrectListIndex} from '../../arrays';

describe('Тестирование утилиты списка: isCorrectListIndex', () => {
    it('Возвращает false при передаче индекса меньше нуля', () => {
        expect(isCorrectListIndex([], -1)).toBeFalsy();
        expect(isCorrectListIndex([1, 2, 3], -10)).toBeFalsy();
        expect(isCorrectListIndex(['test', 'test2'], -2)).toBeFalsy();
    });

    it('Возвращает false при передаче индекса больше или равным длинны списка', () => {
        expect(isCorrectListIndex([], 0)).toBeFalsy();
        expect(isCorrectListIndex([], 10)).toBeFalsy();
        expect(isCorrectListIndex([1, 2, 3], 3)).toBeFalsy();
        expect(isCorrectListIndex(['test', 'test2'], 2)).toBeFalsy();
    });

    it('Возвращает true при передаче корректного индекса', () => {
        expect(isCorrectListIndex([1, 2, 3], 0)).toBeTruthy();
        expect(isCorrectListIndex([1, 2, 3], 1)).toBeTruthy();
        expect(isCorrectListIndex([1, 2, 3], 2)).toBeTruthy();
        expect(isCorrectListIndex(['test', 'test2'], 0)).toBeTruthy();
        expect(isCorrectListIndex(['test', 'test2'], 1)).toBeTruthy();
    });
});
