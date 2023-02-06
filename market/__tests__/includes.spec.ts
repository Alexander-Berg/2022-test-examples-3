import {InvariantViolation} from '@yandex-market/invariant';

import {includes} from '..';

const eq = (a, b) => expect(a).toEqual(b);

describe('includes', () => {
    it('returns true if an element is in a list', () => {
        eq(includes(7, [1, 2, 3, 9, 8, 7, 100, 200, 300]), true);
    });

    it('returns false if an element is not in a list', () => {
        eq(includes(99, [1, 2, 3, 9, 8, 7, 100, 200, 300]), false);
    });

    it('returns false for the empty list', () => {
        eq(includes(1, []), false);
    });

    it('has SameValueZero semantics', () => {
        eq(includes(0, [-0]), true);
        eq(includes(-0, [0]), true);
        eq(includes(NaN, [NaN]), true);
    });

    it('returns true if substring is part of string', () => {
        eq(includes('ba', 'banana'), true);
    });

    it('does accept only strings and arrays as second argument and throws exception otherwise', () => {
        function withNull() {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            includes(1, null);
        }
        function withObject() {
            // @ts-expect-error
            includes(1, {});
        }

        expect(withNull).toThrow(InvariantViolation);
        expect(withObject).toThrow(InvariantViolation);
    });

    it('checks first argument is compatable with second and throws exception otherwise', () => {
        function nonStringInString() {
            // @ts-expect-error
            includes(1, '321');
        }

        expect(nonStringInString).toThrow(InvariantViolation);
    });
});
