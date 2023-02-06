/* eslint-disable no-unused-expressions */

import {headOr} from '..';

describe('headOr', () => {
    it('should return first item for non-empty array', () => {
        const numberArray: number[] = [1, 2, 3];
        const first = headOr('default', numberArray);

        expect(first).toEqual(1);

        (first as number | string);

        // @ts-expect-error
        (first as boolean);
    });

    it('should return default value for an empty array', () => {
        const undef = headOr('default', []);

        expect(undef).toEqual('default');

        (undef as string);
    });

    it('should infer type for tuples with 6 or less items', () => {
        const one: [1] = [1];
        (headOr('default', one) as 1);

        const two: [1, 2] = [1, 2];
        (headOr('default', two) as 1);

        const three: [1, 2, 3] = [1, 2, 3];
        (headOr('default', three) as 1);

        const four: [1, 2, 3, 4] = [1, 2, 3, 4];
        (headOr('default', four) as 1);

        const five: [1, 2, 3, 4, 5] = [1, 2, 3, 4, 5];
        (headOr('default', five) as 1);

        const six: [1, 2, 3, 4, 5, 6] = [1, 2, 3, 4, 5, 6];
        (headOr('default', six) as 1);
    });

    it('should infer union type for tuples with size larger than 6', () => {
        const seven: [1, 2, 3, 4, 5, 6, 7] = [1, 2, 3, 4, 5, 6, 7];
        (headOr('default', seven) as 1 | string);

        const eight: [1, 2, 3, 4, 5, 6, 7, 8] = [1, 2, 3, 4, 5, 6, 7, 8];
        (headOr('default', eight) as 1 | string);
    });

    it('should throw an error if argument is not an array', () => {
        function headOrWithObject() {
            // @ts-expect-error
            headOr('default', {});
        }
        function headOrWithNull() {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            headOr('default', null);
        }
        function headOrWithFunction() {
            // @ts-expect-error
            headOr('default', () => ({}));
        }
        function headOrWithSymbol() {
            // @ts-expect-error
            headOr('default', Symbol('some test symbol'));
        }
        function headOrWithBoolean() {
            // @ts-expect-error
            headOr('default', true);
        }
        function headOrWithString() {
            // @ts-expect-error
            headOr('default', 'string');
        }

        expect(headOrWithObject).toThrow();
        expect(headOrWithNull).toThrow();
        expect(headOrWithFunction).toThrow();
        expect(headOrWithSymbol).toThrow();
        expect(headOrWithBoolean).toThrow();
        expect(headOrWithString).toThrow();
    });
});
