/* eslint-disable */

import { InvariantViolation } from '@yandex-market/invariant';
import {pick} from '../';
import {expectType} from "../helpers";

describe('pick', function () {
    const args = ['a', 'c'] as const;
    const object = {a: 1, b: 2, c: 3, d: 4};
    const nested = {'b.c': 1, b: {c: 2, d: 3}};

    it('правильно типизирован', () => {
        const w = pick(['a'], {a: 1, b: 2, c: 3});
        const x = pick(['a', 'b'], {a: 1, b: 2, c: 3});
        const y = pick(['a', 'c'], {a: 1, b: 2, c: 3});
        const z = pick([], {a: 1, b: 2, c: 3});

        expectType<{a: number}>(w);

        expectType<{a: number, b: number}>(x);
        // @ts-expect-error
        expectType<{a: number, b: number, c: number}>(x);

        expectType<{a: number, c: number}>(y);
        // @ts-expect-error
        expectType<{a: number, b: number, c: number}>(y);

        expectType<{}>(z);
        // @ts-expect-error
        expectType<{a: number, b: number, c: number}>(z);

        const wConst = pick(['a'], {a: 1, b: 2, c: 3} as const);
        const xConst = pick(['a', 'b'], {a: 1, b: 2, c: 3} as const);
        const yConst = pick(['a', 'c'], {a: 1, b: 2, c: 3} as const);
        const zConst = pick([], {a: 1, b: 2, c: 3} as const);

        expectType<{a: 1}>(wConst);

        expectType<{a: 1, b: 2}>(xConst);
        // @ts-expect-error
        expectType<{a: 1, b: 2, c: 3}>(xConst);

        expectType<{a: 1, c: 3}>(yConst);
        // @ts-expect-error
        expectType<{a: 1, b: 2, c: 3}>(yConst);

        expectType<{}>(zConst);
        // @ts-expect-error
        expectType<{a: 1, b: 2, c: 3}>(zConst);

    });

    it('accepts string literals as well', () => {
        const stringLiteral: ["a", "b"] = ['a', 'b'];
        // No error here
        pick(stringLiteral, {a: 1, b: 2});
    })

    it('принимает массив и объект в качестве аргументов', () => {
        expect(pick(args, object)).toEqual({ 'a': 1, 'c': 3 });
        expect(pick(['b.c'], nested)).toEqual({'b.c': 1})
    });

    it('работает с объектами-обертками над примитивами. Но не на уровне типов', () => {
        expect(
            pick(['slice'], new String(''))
        ).toEqual({'slice': ''.slice});
    });

    it('не поддерживает arguments вместо массива', () => {
        (function(...paths) {
            // @ts-expect-error
            expect(() => pick(arguments, object)).toThrow(InvariantViolation);
        })(...args);
    });

    it('бросает InvariantViolation исключение, если в аргументы переданы не массив или не объект', () => {
        // @ts-expect-error
        expect(() => pick('a', object)).toThrow(InvariantViolation);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => pick(null, object)).toThrow(InvariantViolation);
        // @ts-expect-error
        expect(() => pick(42, object)).toThrow(InvariantViolation);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
        expect(() => pick(['a'], null)).toThrow(InvariantViolation);
        // @ts-expect-error
        expect(() => pick(['a'], '')).toThrow(InvariantViolation);
    });

    it('запрещает на уровне типов использование не строк в массиве', () => {
        // @ts-expect-error
        pick([0], { '0': 'a', '1': 'b' })
    });
});
