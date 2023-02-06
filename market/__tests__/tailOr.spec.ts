/* eslint-disable no-unused-expressions */

import {tailOr} from '..';

describe('tailOr', () => {
    it('should return last item for non-empty array', () => {
        const numberArray: number[] = [1, 2, 3];
        const last = tailOr('default', numberArray);

        expect(last).toEqual(3);

        (last as number | string);

        // @ts-expect-error
        (last as boolean);
    });

    it('should return default value for an empty array', () => {
        const undef = tailOr('default', []);

        expect(undef).toEqual('default');

        (undef as string);
    });

    it('should infer type for tuples with 6 or less items', () => {
        const one: [1] = [1];
        (tailOr('default', one) as 1);

        const two: [1, 2] = [1, 2];
        (tailOr('default', two) as 2);

        const three: [1, 2, 3] = [1, 2, 3];
        (tailOr('default', three) as 3);

        const four: [1, 2, 3, 4] = [1, 2, 3, 4];
        (tailOr('default', four) as 4);

        const five: [1, 2, 3, 4, 5] = [1, 2, 3, 4, 5];
        (tailOr('default', five) as 5);

        const six: [1, 2, 3, 4, 5, 6] = [1, 2, 3, 4, 5, 6];
        (tailOr('default', six) as 6);
    });

    it('should not infer type for tuples with size larger than 6', () => {
        const seven: [1, 2, 3, 4, 5, 6, 7] = [1, 2, 3, 4, 5, 6, 7];
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error fallback to arrays
        (tailOr('default', seven) as 7);
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error fallback to arrays
        (tailOr('default', seven) as 7 | string);
    });

    it('should throw an error if argument is not an array', () => {
        function tailOrWithObject() {
            // @ts-expect-error
            tailOr('default', {});
        }
        function tailOrWithNull() {
            tailOr('default', null);
        }
        function tailOrWithFunction() {
            // @ts-expect-error
            tailOr('default', () => ({}));
        }
        function tailOrWithSymbol() {
            // @ts-expect-error
            tailOr('default', Symbol('some test symbol'));
        }
        function tailOrWithBoolean() {
            // @ts-expect-error
            tailOr('default', true);
        }
        function tailOrWithString() {
            // @ts-expect-error
            tailOr('default', 'string');
        }

        expect(tailOrWithObject).toThrow();
        expect(tailOrWithNull).toThrow();
        expect(tailOrWithFunction).toThrow();
        expect(tailOrWithSymbol).toThrow();
        expect(tailOrWithBoolean).toThrow();
        expect(tailOrWithString).toThrow();
    });
});
