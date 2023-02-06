import {add, divide, multiply, subtract, sumValues} from './calculator';

const overflow = 10_000_000_000_000_000.0;

describe('calculator', () => {
    describe('add', () => {
        const a = 10;
        const b = 15;

        it('should add numbers', () => {
            const actual = add(a, b);

            expect(actual).toBe(25);
        });

        it('should add numbers with certain precision', () => {
            const actual = add(2.345, 3.217, 2);

            expect(actual).toBe(5.56);
        });

        it('should add number and overflow number', () => {
            const first = add(overflow, b);
            const second = add(a, overflow);
            const third = add(overflow, overflow);

            expect(first).toBe(Infinity);
            expect(second).toBe(Infinity);
            expect(third).toBe(Infinity);
        });
    });

    describe('subtract', () => {
        const a = 15;
        const b = 10;

        it('should subtract numbers', () => {
            const actual = subtract(a, b);

            expect(actual).toBe(5);
        });

        it('should subtract numbers with certain precision', () => {
            const actual = subtract(2.345, 3.217, 2);

            expect(actual).toBe(-0.87);
        });

        it('should subtract number and overflow', () => {
            const first = subtract(overflow, b);
            const second = subtract(a, overflow);

            expect(first).toBe(Infinity);
            expect(second).toBe(Infinity);
        });
    });

    describe('multiply', () => {
        const a = 10;
        const b = 15;

        it('should multiply numbers', () => {
            const actual = multiply(a, b);

            expect(actual).toBe(150);
        });

        it('should multiply numbers with certain precision', () => {
            const actual = multiply(0.333, 2, 2);

            expect(actual).toBe(0.67);
        });

        it('should multiply number and overflow', () => {
            const first = multiply(overflow, b);
            const second = multiply(a, overflow);
            const third = multiply(overflow, overflow);

            expect(first).toBe(Infinity);
            expect(second).toBe(Infinity);
            expect(third).toBe(Infinity);
        });
    });

    describe('divide', () => {
        it('should divide numbers', () => {
            const actual = divide(10, 5);

            expect(actual).toBe(2);
        });

        it('should divide numbers with certain precision', () => {
            const actual = divide(10, 3, 4);

            expect(actual).toBe(3.3333);
        });

        it('should divide number and overflow', () => {
            const first = divide(overflow, 5);
            const second = divide(10, overflow);

            expect(first).toBe(Infinity);
            expect(second).toBe(0);
        });

        it('should divide number and zero', () => {
            const actual = divide(10, 0);

            expect(actual).toBe(Infinity);
        });
    });

    describe('sum', () => {
        it('should sum array of numbers', () => {
            const actual = sumValues([1, 2, 3, 4, 5]);

            expect(actual).toBe(15);
        });

        it('should sum array of numbers with certain precision', () => {
            const actual = sumValues([1.11, 1.1111, 1.111]);

            expect(actual).toBe(3.33);
        });

        it('should sum array of numbers with different signs', () => {
            const actual = sumValues([1, -1, 0]);

            expect(actual).toBe(0);
        });

        it('should return Infinity if has overflow', () => {
            const actual = sumValues([1, 2, 3, 4, overflow]);

            expect(actual).toBe(Infinity);
        });

        it('should return Infinity if sum is overflow', () => {
            const actual = sumValues([1, overflow - 1]);

            expect(actual).toBe(Infinity);
        });

        it('should ignore undefined in array', () => {
            const actual = sumValues([1, undefined, 2]);

            expect(actual).toBe(3);
        });

        it('should sum array of items with path to each value', () => {
            const items = [{value: 10}, {value: 20}, {value: 40}];
            const actual = sumValues(items, item => item.value);

            expect(actual).toBe(70);
        });
    });
});
