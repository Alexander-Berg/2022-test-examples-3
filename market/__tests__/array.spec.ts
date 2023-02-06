import { reduction } from '../array';

describe('reduction', () => {
    it('должен правильно отрабатывать', () => {
        const sumTillNegative = reduction(
            (sum, el: number, next, finish) => (el < 0 ? finish(sum) : next(sum + el)),
            0,
        );

        expect(sumTillNegative([1, 2, 5])).toBe(8);
        expect(sumTillNegative([1, 2, -3, 5])).toBe(3);
        expect(sumTillNegative([-1, 2, -3, 5])).toBe(0);
    });

    it('должен вести себя как обычный reduce, если не вызывать finish', () => {
        const sumAll = reduction((sum, el: number, next) => next(sum + el), 0);

        expect(sumAll([1, 2])).toBe(3);
        expect(sumAll([1, 2, -3])).toBe(0);
        expect(sumAll([1, 2, -2])).toBe(1);
    });
    it('должен прерывать прохождение по массиву при вызове finish', () => {
        const iterator = jest.fn((sum, el, next, finish) => (el < 0 ? finish(sum) : next(sum + el)));
        const sumTillNegative = reduction(iterator, 0);

        sumTillNegative([1, 2, -3, 3, 12]);
        expect(iterator).toBeCalledTimes(3);

        iterator.mockClear();

        sumTillNegative([-1, 2, -3]);
        expect(iterator).toBeCalledTimes(1);
    });
});
