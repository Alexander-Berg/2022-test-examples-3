import {isNull} from '..';

describe('isNull', () => {
    it('выполняет проверку того, что входящее значение - null', () => {
        expect(isNull(null)).toBe(true);

        expect(isNull(undefined)).toBe(false);
        expect(isNull(true)).toBe(false);
        expect(isNull(false)).toBe(false);
        expect(isNull(42)).toBe(false);
        expect(isNull({})).toBe(false);
        expect(isNull('42')).toBe(false);
        expect(isNull(Symbol('42'))).toBe(false);
        expect(isNull([])).toBe(false);
        expect(isNull(() => {})).toBe(false);
        // eslint-disable-next-line
        expect(isNull(function () {})).toBe(false);
    });

    it('покрыт типами', () => {
        /* eslint-disable no-unused-expressions */
        (isNull(null) as boolean);
        (isNull(42) as boolean);

        const maybeObj: {
            readonly val: number;
        } | null = {val: 42};

        if (!isNull(maybeObj)) {
            (maybeObj.val as number);
        }
        if (isNull(maybeObj)) {
            // @ts-expect-error
            (maybeObj.val as number);
        }
        if (isNull(maybeObj)) {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            (maybeObj as null);
        }
        if (isNull(maybeObj)) {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            (maybeObj as any);
        }

        /* eslint-enable no-unused-expressions */
    });
});
