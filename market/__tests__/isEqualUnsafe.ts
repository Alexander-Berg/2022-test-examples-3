/* eslint-disable no-unused-expressions */
/* eslint-disable no-unused-vars */

import isEqualUnsafe from '../isEqualUnsafe';
import isEqual from '../isEqual/isEqual';

describe('isEqual function', () => {
    // as long as isEqualUnsafe has same impl as isEqual, we can rely on it's own tests
    it('is the same function ambar/isEqual', () => {
        expect(isEqualUnsafe).toEqual(isEqual);
    });

    it('is curried by default', () => {
        expect(isEqualUnsafe({})({})).toEqual(true);
        expect(isEqualUnsafe({a: 1})({a: 0})).toEqual(false);
    });

    it('allows any kind of comparisons', () => {
        isEqualUnsafe({})({});
        isEqualUnsafe({}, {});

        isEqualUnsafe({})(null);
        isEqualUnsafe({}, null);

        isEqualUnsafe(null, {});
        isEqualUnsafe(null)({});

        const a: unknown = 1;
        const b: number = 2;

        isEqualUnsafe(a, b);
        isEqualUnsafe(a)(b);
    });
});
