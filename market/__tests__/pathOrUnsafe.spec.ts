/* eslint-disable no-unused-expressions */
/* eslint-disable no-unused-vars */

import {InvariantViolation} from '@yandex-market/invariant';

import {pathOrUnsafe} from '..';
import {expectType} from '../helpers';

describe('pathOrUnsafe', () => {
    it('takes default value, path and an object and returns the val at the path or default otherwise', () => {
        const obj = {
            a: {
                b: {
                    c: 100,
                    d: 200,
                },
                e: {
                    f: [100, 101, 102],
                    g: 'G',
                },
                h: 'H',
                f: [100, 101, 102],
            },
            i: 'I',
            j: ['J'],
        };

        expect(pathOrUnsafe(null, ['a', 'e', 'f', 1], obj)).toEqual(101);
        expect(pathOrUnsafe('wow', ['a', 'e', 'NO_KEY'], obj)).toEqual('wow');

        expect(pathOrUnsafe(null, ['a', 'b', 'c'], obj)).toEqual(100);

        expect(pathOrUnsafe(null, ['j', 0], obj)).toEqual('J');
        expect(pathOrUnsafe('wow', ['j', 1], obj)).toEqual('wow');
    });

    it('returns defaultValue type if there is no value for sure', () => {
        const result = pathOrUnsafe('noPropForSure', ['a', 'b'] as const, {b: {a: 333}} as const);

        expectType<'noPropForSure'>(result);

        // @ts-expect-error
        expectType<333>(result);
        // @ts-expect-error
        expectType<void>(result);
    });

    it('throws an exception if second argument is not an array', () => {
        function stringpathOrUnsafe() {
            // @ts-expect-error
            pathOrUnsafe(null, 'b.a', {b: {a: 333}});
        }
        function objectpathOrUnsafe() {
            // @ts-expect-error
            pathOrUnsafe(null, {b: 'a'}, {b: {a: 333}});
        }
        function nullpathOrUnsafe() {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            pathOrUnsafe(null, null, {b: {a: 333}});
        }

        expect(stringpathOrUnsafe).toThrow(InvariantViolation);
        expect(objectpathOrUnsafe).toThrow(InvariantViolation);
        expect(nullpathOrUnsafe).toThrow(InvariantViolation);
    });

    it('returns defaultValue if there is nil value on the path', () => {
        const obj = {
            a: {
                b: null,
                c: {
                    val: 333,
                },
            },
        };
        expect(pathOrUnsafe('niled!', ['a', 'b', 'val'], obj)).toEqual('niled!');
        expect(pathOrUnsafe('niled!')(['a', 'b', 'val'], obj)).toEqual('niled!');
        expect(pathOrUnsafe('niled!', ['a', 'b', 'val'])(obj)).toEqual('niled!');
    });

    it('returns defaultValue if path leads to undefined value', () => {
        const obj = {
            a: {
                b: undefined,
            },
        } as const;
        expect(pathOrUnsafe('default', ['a', 'b'], obj)).toEqual('default');
        expect(pathOrUnsafe('default')(['a', 'b'], obj)).toEqual('default');
        expect(pathOrUnsafe('default', ['a', 'b'])(obj)).toEqual('default');
    });

    it('returns value in path even if it null', () => {
        const obj = {
            a: {
                b: null,
            },
        };
        expect(pathOrUnsafe('niled!', ['a', 'b'], obj)).toEqual(null);
        expect(pathOrUnsafe('niled!')(['a', 'b'], obj)).toEqual(null);
        expect(pathOrUnsafe('niled!', ['a', 'b'])(obj)).toEqual(null);
    });

    it('takes first prop type and works with different objects', () => {
        const obj = {a: 123} as const;

        expectType<123>(pathOrUnsafe('absent', ['a'] as const, obj));
        expectType<123>(pathOrUnsafe('absent', ['a'] as const)(obj));

        expectType<'absent'>(pathOrUnsafe('absent', ['b'] as const, obj));
        expectType<'absent'>(pathOrUnsafe('absent' as const, ['b'] as const)(obj));
    });

    it('takes second prop type', () => {
        const obj = {a: {a: 333}} as const;

        expectType<333>(pathOrUnsafe('absent', ['a', 'a'] as const, obj));
        expectType<333>(pathOrUnsafe('absent', ['a', 'a'] as const)(obj));
        // @ts-expect-error
        expectType<222>(pathOrUnsafe('absent', ['a', 'a'] as const, obj));
        // @ts-expect-error
        expectType<222>(pathOrUnsafe('absent' as const, ['a', 'a'] as const)(obj));

        expectType<'absent'>(pathOrUnsafe('absent', ['a', 'b'] as const, obj));
        expectType<'absent'>(pathOrUnsafe('absent' as const, ['a', 'b'] as const)(obj));

        // @ts-expect-error
        expectType<'different-str'>(pathOrUnsafe('absent', ['a', 'b'] as const, obj));
        // @ts-expect-error
        expectType<'different-str'>(pathOrUnsafe('absent' as const, ['a', 'b'] as const)(obj));
    });

    it('takes third prop type', () => {
        const obj = {a: {a: {a: 333}}} as const;

        expectType<333>(pathOrUnsafe('absent', ['a', 'a', 'a'] as const, obj));
        expectType<333>(pathOrUnsafe('absent', ['a', 'a', 'a'] as const)(obj));
        // @ts-expect-error
        expectType<222>(pathOrUnsafe('absent', ['a', 'a', 'a'] as const, obj));
        // @ts-expect-error
        expectType<222>(pathOrUnsafe('absent' as const, ['a', 'a', 'a'] as const)(obj));

        expectType<'absent'>(pathOrUnsafe('absent', ['a', 'a', 'b'] as const, obj));
        expectType<'absent'>(pathOrUnsafe('absent' as const, ['a', 'a', 'b'] as const)(obj));

        // @ts-expect-error
        expectType<'different-str'>(pathOrUnsafe('absent', ['a', 'a', 'b'] as const, obj));
        // @ts-expect-error
        expectType<'different-str'>(pathOrUnsafe('absent' as const, ['a', 'a', 'b'] as const)(obj));
    });

    it('takes fourth prop type', () => {
        const obj = {a: {a: {a: {a: 333}}}} as const;

        expectType<333>(pathOrUnsafe('absent', ['a', 'a', 'a', 'a'] as const, obj));
        expectType<333>(pathOrUnsafe('absent' as const, ['a', 'a', 'a', 'a'] as const)(obj));
        // @ts-expect-error
        expectType<222>(pathOrUnsafe('absent', ['a', 'a', 'a', 'a'] as const, obj));
        // @ts-expect-error
        expectType<222>(pathOrUnsafe('absent', ['a', 'a', 'a', 'a'] as const)(obj));

        expectType<'absent'>(pathOrUnsafe('absent', ['a', 'a', 'a', 'b'] as const, obj));
        expectType<'absent'>(pathOrUnsafe('absent' as const, ['a', 'a', 'a', 'b'] as const)(obj));

        // @ts-expect-error
        expectType<'different-str'>(pathOrUnsafe('absent', ['a', 'a', 'a', 'b'] as const, obj));
        // @ts-expect-error
        expectType<'different-str'>(pathOrUnsafe('absent', ['a', 'a', 'a', 'b'] as const)(obj));
    });

    it('takes fifth prop type', () => {
        const obj = {a: {a: {a: {a: {a: 333}}}}} as const;

        expectType<333>(pathOrUnsafe('absent', ['a', 'a', 'a', 'a', 'a'] as const, obj));
        expectType<333>(pathOrUnsafe('absent' as const, ['a', 'a', 'a', 'a', 'a'] as const)(obj));

        // @ts-expect-error
        expectType<222>(pathOrUnsafe('absent', ['a', 'a', 'a', 'a', 'a'] as const, obj));
        // @ts-expect-error
        expectType<222>(pathOrUnsafe('absent' as const, ['a', 'a', 'a', 'a', 'a'] as const)(obj));

        expectType<'absent'>(pathOrUnsafe('absent', ['a', 'a', 'a', 'a', 'b'] as const, obj));
        expectType<'absent'>(pathOrUnsafe('absent' as const, ['a', 'a', 'a', 'a', 'b'] as const)(obj));

        // @ts-expect-error
        expectType<'different-str'>(pathOrUnsafe('absent', ['a', 'a', 'a', 'a', 'b'] as const, obj));
        // @ts-expect-error
        expectType<'different-str'>(pathOrUnsafe('absent', ['a', 'a', 'a', 'a', 'b'] as const)(obj));
    });

    it('won\'t show error if source itself is nullable', () => {
        const obj: {
            a: 123
        } | undefined | null = {a: 123};

        const valSimple = pathOrUnsafe('fallback', ['a'] as const, obj);

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error lgtm
        expectType<123>(valSimple);

        // @ts-expect-error
        expectType<'fallback'>(valSimple);

        expectType<'fallback' | 123>(valSimple);

        const valCurried = pathOrUnsafe('fallback' as const, ['a'] as const)(obj);

        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error lgtm
        expectType<123>(valCurried);

        // @ts-expect-error
        expectType<'fallback'>(valCurried);

        expectType<'fallback' | 123>(valCurried);
    });
});
