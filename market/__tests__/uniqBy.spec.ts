/* eslint-disable no-unused-expressions */

import {uniqBy} from '..';

const identity = x => x;

const eq = (a, b) => expect(a).toEqual(b);

describe('uniqBy', () => {
    it('returns a set from any array (i.e. purges duplicate elements)', () => {
        const list = [1, -2, 3, 1, 2, -3, -1, 2, 3];
        const uniqList = uniqBy(Math.abs, list);
        eq(uniqList, [1, -2, 3]);

        (uniqList as typeof list);

        // @ts-expect-error
        (uniqList as string[]);
    });

    it('keeps elements from the left', () => {
        eq(uniqBy(identity, [1, 2, 3, 4, 1]), [1, 2, 3, 4]);
    });

    it('returns an empty array for an empty array', () => {
        eq(uniqBy(identity, []), []);
        const emptyArr = uniqBy(identity, []);

        (emptyArr as Array<void>);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error seems like it is not error
        (emptyArr as Array<number>);
    });

    it('throws an exception if first argument is not a function', () => {
        function uniqByWithObject() {
            // @ts-expect-error
            uniqBy({}, []);
        }
        function uniqByWithNull() {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            uniqBy(null, []);
        }
        function uniqByWithUndefined() {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            uniqBy(undefined, []);
        }
        function uniqByWithString() {
            // @ts-expect-error
            uniqBy('abc', []);
        }

        expect(uniqByWithObject).toThrow();
        expect(uniqByWithNull).toThrow();
        expect(uniqByWithUndefined).toThrow();
        expect(uniqByWithString).toThrow();
    });
});
