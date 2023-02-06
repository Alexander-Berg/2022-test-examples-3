import {isNil, isNull} from '..';

describe('isUndefined', () => {
    it('выполняет проверку того, что входящее значение - undefined или null', () => {
        expect(isNil(undefined)).toBe(true);
        expect(isNil(null)).toBe(true);

        expect(isNil(true)).toBe(false);
        expect(isNil(false)).toBe(false);
        expect(isNil(42)).toBe(false);
        expect(isNil('42')).toBe(false);
        expect(isNil(Symbol('42'))).toBe(false);
        expect(isNil({})).toBe(false);
        expect(isNil([4, 2])).toBe(false);
        expect(isNil(() => {})).toBe(false);
        // eslint-disable-next-line
        expect(isNil(function () {})).toBe(false);
    });

    it('покрыт типами', () => {
        /* eslint-disable no-unused-expressions */
        (isNil(undefined) as boolean);
        (isNil(42) as boolean);

        const maybeObj: {
            readonly val: number;
        } | undefined | null = {val: 42};

        if (!isNil(maybeObj)) {
            (maybeObj.val as number);
        }
        if (isNil(maybeObj)) {
            // @ts-expect-error
            (maybeObj.val as number);
        }

        if (isNil(maybeObj)) {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            (maybeObj as void | null);
        }
        if (isNil(maybeObj) && !isNull(maybeObj)) {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error undefined can be casted to void
            (maybeObj as void);
        }
        if (isNil(maybeObj)) {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
            (maybeObj as any);
        }

        /* eslint-enable no-unused-expressions */
    });
});
