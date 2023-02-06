import {isUndefined} from '..';

describe('isUndefined', () => {
    it('выполняет проверку того, что входящее значение - undefined', () => {
        expect(isUndefined(undefined)).toBe(true);

        expect(isUndefined(null)).toBe(false);
        expect(isUndefined(true)).toBe(false);
        expect(isUndefined(false)).toBe(false);
        expect(isUndefined(42)).toBe(false);
        expect(isUndefined('42')).toBe(false);
        expect(isUndefined(Symbol('42'))).toBe(false);
        expect(isUndefined({})).toBe(false);
        expect(isUndefined([4, 2])).toBe(false);
        expect(isUndefined(() => {})).toBe(false);
        // eslint-disable-next-line
        expect(isUndefined(function () {})).toBe(false);
    });

    it('покрыт типами', () => {
        /* eslint-disable no-unused-expressions */
        (isUndefined(undefined) as boolean);
        (isUndefined(42) as boolean);

        const maybeObj: {
            readonly val: number
        } | void = {val: 42};

        if (!isUndefined(maybeObj)) {
            (maybeObj.val as number);
        }
        if (isUndefined(maybeObj)) {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error seems ok
            (maybeObj as void);
        }
        if (isUndefined(maybeObj)) {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error seems ok
            (maybeObj as any);
        }

        /* eslint-enable no-unused-expressions */
    });
});
