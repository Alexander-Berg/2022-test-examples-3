import {isNumber} from '..';

describe('isNumber', () => {
    it('выполняет проверку того, что входящее значение - число', () => {
        expect(isNumber(42)).toBe(true);
        expect(isNumber(42.000000001)).toBe(true);
        expect(isNumber(NaN)).toBe(true);
        expect(isNumber(Infinity)).toBe(true);
        expect(isNumber(-Infinity)).toBe(true);

        expect(isNumber(undefined)).toBe(false);
        expect(isNumber(null)).toBe(false);
        expect(isNumber(true)).toBe(false);
        expect(isNumber(false)).toBe(false);
        expect(isNumber({})).toBe(false);
        expect(isNumber('42')).toBe(false);
        expect(isNumber(Symbol('42'))).toBe(false);
        expect(isNumber([4, 2])).toBe(false);
        expect(isNumber(() => {})).toBe(false);
        // eslint-disable-next-line
        expect(isNumber(function () {})).toBe(false);
    });

    it('покрыт типами', () => {
        /* eslint-disable no-unused-expressions */
        /* eslint-disable no-lone-blocks */
        (isNumber(undefined) as boolean);
        (isNumber(42) as boolean);

        {
            const num: number | undefined | null = 42;
            if (isNumber(num)) {
                (num.toFixed(2));
            }
            if (!isNumber(num)) {
                // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
                (num as null | void);
            }
        }

        {
            const num: number | undefined | null = null;
            if (!isNumber(num)) {
                // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
                (num as number);
            }
        }

        /* eslint-enable no-lone-blocks */
        /* eslint-enable no-unused-expressions */
    });
});
