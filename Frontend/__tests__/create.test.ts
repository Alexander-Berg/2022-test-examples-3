import { create } from '../create';

describe('memo.create', () => {
    it('should work properly', () => {
        const fn = jest.fn((a: number, b: number) => {
            return a * b;
        });

        const memoized = create(fn);

        expect(memoized(3, 5)).toBe(15);
        expect(memoized(3, 5)).toBe(15);

        expect(fn).toBeCalledTimes(1);

        expect(memoized(3, 2)).toBe(6);
        expect(memoized(3, 2)).toBe(6);

        expect(fn).toBeCalledTimes(2);

        expect(memoized(1, 2)).toBe(2);
        expect(memoized(2, 3)).toBe(6);

        expect(fn).toBeCalledTimes(4);
    });
});
