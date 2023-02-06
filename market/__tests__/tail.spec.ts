/* eslint-disable no-unused-expressions */

import {tail} from '..';

describe('tail', () => {
    it('should return last item for non-empty array', () => {
        const numberArray: number[] = [1, 2, 3];
        const last = tail(numberArray);

        expect(last).toEqual(3);

        (last as number);

        // @ts-expect-error
        (last as string);
    });

    it('should return undefined for an empty array', () => {
        // 'cause it is equivalent to `arr = []; arr[0]`
        const undef = tail([]);

        expect(undef).toEqual(undefined);
        // todo: убрать, в тс же это не особо смысл делает
        (undef as never);
    });

    it('should infer type for tuples with 6 or less items', () => {
        const one: [1] = [1];
        (tail(one) as 1);

        const two: [1, 2] = [1, 2];
        (tail(two) as 2);

        const three: [1, 2, 3] = [1, 2, 3];
        (tail(three) as 3);

        const four: [1, 2, 3, 4] = [1, 2, 3, 4];
        (tail(four) as 4);

        const five: [1, 2, 3, 4, 5] = [1, 2, 3, 4, 5];
        (tail(five) as 5);

        const six: [1, 2, 3, 4, 5, 6] = [1, 2, 3, 4, 5, 6];
        (tail(six) as 6);
    });

    it('should not infer type for tuples with size larger than 6', () => {
        const seven: [1, 2, 3, 4, 5, 6, 7] = [1, 2, 3, 4, 5, 6, 7];
        // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error fallback to arrays
        (tail(seven) as 7);
    });

    it('should throw an error if argument is not an array', () => {
        function tailWithObject() {
            // @ts-expect-error
            tail({});
        }
        function tailWithNull() {
            tail(null);
        }
        function tailWithFunction() {
            // @ts-expect-error
            tail(() => ({}));
        }
        function tailWithSymbol() {
            // @ts-expect-error
            tail(Symbol('some test symbol'));
        }
        function tailWithBoolean() {
            // @ts-expect-error
            tail(true);
        }
        function tailWithString() {
            // @ts-expect-error
            tail('string');
        }

        expect(tailWithObject).toThrow();
        expect(tailWithNull).toThrow();
        expect(tailWithFunction).toThrow();
        expect(tailWithSymbol).toThrow();
        expect(tailWithBoolean).toThrow();
        expect(tailWithString).toThrow();
    });
});
