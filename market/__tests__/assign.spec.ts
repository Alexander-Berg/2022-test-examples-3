/* eslint-disable no-unused-expressions */
/* eslint-disable no-unused-vars */

import {assign} from '..';
import {expectType} from '../helpers';

describe('assign', () => {
    it('takes two objects, assigns their own properties and returns a new object', () => {
        const a = {w: 1, x: 2};
        const b = {y: 3, z: 4};

        expect(assign(a, b)).toEqual({
            w: 1,
            x: 2,
            y: 3,
            z: 4,
        });
    });

    it('overrides properties in the first object with properties in the second object', () => {
        const a = {w: 1, x: 2};
        const b = {w: 100, y: 3, z: 4};

        expect(assign(a, b)).toEqual({
            w: 100,
            x: 2,
            y: 3,
            z: 4,
        });
    });

    it('is not destructive', () => {
        const a = {w: 1, x: 2};
        const res = assign(a, {x: 5});

        expect(res).toEqual({w: 1, x: 5});
    });

    it('is not mutable', () => {
        const a = {w: 1, x: 2};
        assign(a, {x: 5});

        expect(a).toEqual({w: 1, x: 2});
    });

    it('reports only own properties', () => {
        const a = {w: 1, x: 2};

        function Cla() {}
        Cla.prototype.x = 5;

        expect(assign(new Cla(), a)).toEqual({w: 1, x: 2});
        expect(assign(a, new Cla())).toEqual({w: 1, x: 2});
    });

    it('keeps typings for literal properties', () => {
        const threeLiterals = assign({a: 333, c: 555} as const, {b: 's', c: 'rewrite it!'} as const);

        expectType<{a: 333, b: 's', c: 'rewrite it!'}>(threeLiterals);

        // @ts-expect-error
        expectType<{a: 222, b: 'ss'}>(threeLiterals);

        // @ts-expect-error
        expectType<{a: 333, b: 's', c: 555}>(threeLiterals);

        const obj: Record<string, string> = {b: 'aaa'};
        const literalAndObject = assign(obj, {a: 333} as const);

        // @ts-expect-error https://st.yandex-team.ru/MARKETFRONTECH-2258
        expectType<{[x: string]: string, a: 333}>(literalAndObject);
        // @ts-expect-error
        expectType<{a: 222, [x: string]: string}>(literalAndObject);

        const literalAndObjectReversed = assign({a: 333} as const, obj);
        // With reverse object we can't be sure "a' will be 333
        expectType<{[x: string]: string}>(literalAndObjectReversed);
        // @ts-expect-error
        expectType<{a: 333, [x: string]: string}>(literalAndObjectReversed);
    });
});
