/* eslint-disable no-unused-expressions, no-unused-vars */

import {compact} from '..';
import {expectType} from '../helpers';

describe('функция compact', () => {
    it('отбрасывает все falsy-значения в массиве', () => {
        const exampleArray = ['wow', false, NaN, true, null, 0, '', undefined, 1, {} as object, []];

        const compactedArray = compact(exampleArray);
        expect(compactedArray).toEqual(['wow', true, 1, {}, []]);

        // Корректно выводит типы из литералов
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expectType<('wow' | true | number | {} | [])[]>(exampleArray);

        // В отличии от оригинального массива, каст к массиву с не-nullable значением проходит
        expectType<('wow' | true | number | {} | [])[]>(compactedArray);

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expectType<(boolean | string | number | {})[]>(compactedArray);
        // @ts-expect-error
        expectType<(string)[]>(compactedArray);
        // @ts-expect-error
        expectType<(never)[]>(compactedArray);
    });

    it('корректно выводит типы для обычных массивов', () => {
        const maybeNumber: Array<number | undefined | null> = [1, 2, 3, 0];

        const sureNumber = compact(maybeNumber);
        expectType<number[]>(sureNumber as number[]);

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expectType<(number | undefined | null)[]>(sureNumber);

        const stringsAndNumbers: Array<string | undefined | null | number> = [1, 'a', 0, ''];
        expectType<(string | number)[]>(compact(stringsAndNumbers));

        // @ts-expect-error
        expectType<(string)[]>(compact(stringsAndNumbers));
    });

    it('выводит ошибку, если аргумент не массив', () => {
        function compactWithObject() {
            // @ts-expect-error
            compact({});
        }
        function compactWithNull() {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            compact(null);
        }
        function compactWithUndefined() {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            compact(undefined);
        }
        function compactWithString() {
            // @ts-expect-error
            compact('abc');
        }

        expect(compactWithObject).toThrow();
        expect(compactWithNull).toThrow();
        expect(compactWithUndefined).toThrow();
        expect(compactWithString).toThrow();
    });
});
