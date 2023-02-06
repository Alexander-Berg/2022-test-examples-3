/* eslint-disable no-unused-expressions */

import {clamp} from '..';

describe('clamp', () => {
    it('returns clamped number', () => {
        expect(clamp(-10, -5, 5)).toEqual(-5);
        expect(clamp(10, -5, 5)).toEqual(5);
        expect(clamp(1, -5, 5)).toEqual(1);
    });

    it('properly typed', () => {
        (clamp(10, 5, 15) as number);

        // @ts-expect-error
        (clamp(10, 5, 15) as string);

        function clampWithValueAsString() {
            // @ts-expect-error
            (clamp('foo', 1, 1));
        }

        function clampWithLowerAsString() {
            // @ts-expect-error
            (clamp(1, 'foo', 1));
        }

        function clampWithUpperAsString() {
            // @ts-expect-error
            (clamp(1, 1, 'foo'));
        }

        expect(clampWithValueAsString).toThrow();
        expect(clampWithLowerAsString).toThrow();
        expect(clampWithUpperAsString).toThrow();
    });
});
