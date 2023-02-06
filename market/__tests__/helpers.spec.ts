import {getIndex, move} from '../helpers';

const ITEMS = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'];
const ITEMS_MOVED = ['0', '1', '2', '4', '5', '3', '6', '7', '8', '9'];
const ITEMS_MOVED_0 = ['3', '0', '1', '2', '4', '5', '6', '7', '8', '9'];
const ITEMS_MOVED_L = ['0', '1', '2', '4', '5', '6', '7', '8', '9', '3'];

describe('getIndex', () => {
    it('строка есть в массиве', () => {
        expect(getIndex(ITEMS, '3')).toEqual(3);
    });

    it('строки нет в массиве', () => {
        expect(getIndex(ITEMS, '13')).toEqual(-1);
    });
});

describe('move', () => {
    it('позиция from за пределами массива', () => {
        expect(move(13, 0, ITEMS)).toEqual(ITEMS);
    });

    it('перемещаем существующий элемент на существующую позицию', () => {
        expect(move(3, 5, ITEMS)).toEqual(ITEMS_MOVED);
    });

    it('позиция to < 0', () => {
        expect(move(3, -5, ITEMS)).toEqual(ITEMS_MOVED_0);
    });

    it('позиция to >= L', () => {
        expect(move(3, ITEMS.length + 5, ITEMS)).toEqual(ITEMS_MOVED_L);
    });
});
