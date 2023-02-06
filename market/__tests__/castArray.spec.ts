/* eslint-disable no-unused-expressions */

import {castArray} from '..';

describe('функция castArray', () => {
    it('возвращает массив без изменений, если был передан массив', () => {
        const numberArray = [1, 2, 3];
        const casted = castArray(numberArray);

        expect(casted).toEqual([1, 2, 3]);

        (casted as number[]);

        // @ts-expect-error
        (casted as string[]);
    });

    it('возвращает аргумент, обернутый в массив, если был передан не массив', () => {
        const number = 1;
        const casted = castArray(number);

        expect(casted).toEqual([1]);

        (casted as [1]);

        // @ts-expect-error
        (casted as []);
    });
});
