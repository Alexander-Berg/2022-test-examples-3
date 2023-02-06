import {InvariantViolation} from '@yandex-market/invariant';

import {differenceWith} from '..';

describe('differenceWith', () => {
    const eq = (a, b) => a.foo === b.foo;
    const a = {foo: 'a'};
    const b = {foo: 'b'};
    const array = [a, b];
    const values = [a];

    it('returns new array of filtered values', () => {
        expect(differenceWith(eq, array, values)).toEqual([b]);
        expect(differenceWith(eq, array, [])).toEqual(array);
        expect(differenceWith(eq, [], values)).toEqual([]);
        expect(differenceWith(eq, [a], values)).toEqual([]);
        expect(array).toEqual([a, b]);
    });

    it('is curried', () => {
        expect(differenceWith(eq, array)(values)).toEqual([b]);
        expect(differenceWith(eq)(array)(values)).toEqual([b]);
        expect(differenceWith(eq)(array, values)).toEqual([b]);
    });

    it('is properly typed', () => {
        (differenceWith(() => true, array, values));
        // @ts-expect-error
        (differenceWith(() => {}, array, values));

        function differenceWithStringArray() {
            // @ts-expect-error
            (differenceWith(eq, 'a', ['a']));
        }
        expect(differenceWithStringArray).toThrow(InvariantViolation);

        function differenceWithStringValues() {
            // @ts-expect-error
            (differenceWith(eq, ['a'], 'a'));
        }
        expect(differenceWithStringValues).toThrow(InvariantViolation);

        function differenceWithStringComparator() {
            // @ts-expect-error
            (differenceWith('a', ['a'], ['a']));
        }
        expect(differenceWithStringComparator).toThrow(InvariantViolation);

        /* eslint-disable no-unused-expressions */
        (differenceWith(eq, array)(values) as Array<{
            foo: string
        }>);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error curry
        (differenceWith(eq, array)(values) as Array<number>);

        (differenceWith(eq)(array)(values) as Array<{
            foo: string
        }>);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error curry
        (differenceWith(eq)(array)(values) as Array<number>);

        (differenceWith(eq, array, values) as Array<{
            foo: string
        }>);
        // @ts-expect-error
        (differenceWith(eq, array, values) as Array<number>);

        (differenceWith(eq)(array, values) as Array<{
            foo: string
        }>);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error curry
        (differenceWith(eq)(array, values) as Array<number>);
        /* eslint-enable no-unused-expressions */
    });
});
