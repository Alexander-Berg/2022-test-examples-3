import {isInteger} from '..';

describe('isInteger', () => {
    it('выполняет проверку того, что входящее значение - целое число', () => {
        expect(isInteger(42)).toBe(true);
        expect(isInteger(42.0001)).toBe(false);
        expect(isInteger(NaN)).toBe(false);
        expect(isInteger(Infinity)).toBe(false);
        expect(isInteger(-Infinity)).toBe(false);

        expect(isInteger(undefined)).toBe(false);
        expect(isInteger(null)).toBe(false);
        expect(isInteger(true)).toBe(false);
        expect(isInteger(false)).toBe(false);
        expect(isInteger({})).toBe(false);
        expect(isInteger('42')).toBe(false);
        expect(isInteger(Symbol('42'))).toBe(false);
        expect(isInteger([4, 2])).toBe(false);
        expect(isInteger(() => {})).toBe(false);
        // eslint-disable-next-line
        expect(isInteger(function () {})).toBe(false);
    });

    it('покрыт типами', () => {
        /* eslint-disable no-unused-expressions */
        /* eslint-disable no-lone-blocks */
        (isInteger(undefined) as boolean);
        (isInteger(42) as boolean);

        {
            const num: number | undefined | null = 42;
            if (isInteger(num)) {
                (num as number);
            }
            if (!isInteger(num)) {
                (num as number | null | void);
            }
            if (!isInteger(num)) {
                // @ts-expect-error
                (num as null | void);
            }
        }

        {
            const num: number = 42.001;
            if (isInteger(num)) {
                (num as number);
            } else {
                (num as number);
            }
        }

        /* eslint-enable no-lone-blocks */
        /* eslint-enable no-unused-expressions */
    });
});
