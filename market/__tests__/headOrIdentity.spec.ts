/* eslint-disable no-unused-expressions */

import {headOrIdentity} from '..';

describe('функция headOrIdentity', () => {
    it('возвращает первый элемент массива для непустого массива', () => {
        const numberArray: number[] = [1, 2, 3];
        const n = headOrIdentity(numberArray);

        expect(n).toEqual(1);

        // Массивы неизвестной длины возвращают maybe тип
        (n as number | undefined | null);

        // @ts-expect-error
        (n as string | undefined | null);
    });

    it('returns empty for an empty array', () => {
        // 'cause it is equivalent to `arr = []; arr[0]`
        const undef = headOrIdentity([]);

        expect(undef).toEqual(undefined);

        (undef as never);
    });

    it('правильно выводит тип для кортежей вплоть до 6-ой размерности', () => {
        const one: [1] = [1];
        (headOrIdentity(one) as 1);

        const two: [1, 2] = [1, 2];
        (headOrIdentity(two) as 1);

        const three: [1, 2, 3] = [1, 2, 3];
        (headOrIdentity(three) as 1);

        const four: [1, 2, 3, 4] = [1, 2, 3, 4];
        (headOrIdentity(four) as 1);

        const five: [1, 2, 3, 4, 5] = [1, 2, 3, 4, 5];
        (headOrIdentity(five) as 1);

        const six: [1, 2, 3, 4, 5, 6] = [1, 2, 3, 4, 5, 6];
        (headOrIdentity(six) as 1);

        const seven: [1, 2, 3, 4, 5, 6, 7] = [1, 2, 3, 4, 5, 6, 7];
        (headOrIdentity(seven) as 1);
    });

    it('возвращает переданный аргумент, если передан не массив', () => {
        const obj = {};
        const returnedObj = headOrIdentity(obj);
        expect(returnedObj).toBe(obj);
        (obj as {});
        // @ts-expect-error
        (obj as void);

        const n = headOrIdentity(null); // type: never
        expect(n).toBe(null);
        (n as null);
        (n as void);

        const someFunc = () => ({});
        const fn = headOrIdentity(someFunc);
        expect(fn).toBe(someFunc);
        (fn as typeof someFunc);
        // @ts-expect-error
        (fn as void);

        const symbol = Symbol('some test symbol');
        const returnedSymbol = headOrIdentity(symbol);
        expect(returnedSymbol).toBe(symbol);
        (returnedSymbol as typeof symbol);
        // @ts-expect-error
        (returnedSymbol as void);

        const bool = headOrIdentity(true);
        expect(bool).toBe(true);
        (bool as boolean);
        // @ts-expect-error
        (bool as void);

        const string = headOrIdentity('string');
        expect(string).toBe('string');
        (string as string);
        // @ts-expect-error
        (string as void);
    });
});
