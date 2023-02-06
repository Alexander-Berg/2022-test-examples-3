import {isObject} from '..';

describe('isObject', () => {
    it('выполняет проверку того, что входящее значение - объект', () => {
        expect(isObject({})).toBe(true);
        expect(isObject([])).toBe(true);

        expect(isObject(null)).toBe(false);
        expect(isObject(undefined)).toBe(false);
        expect(isObject(true)).toBe(false);
        expect(isObject(false)).toBe(false);
        expect(isObject(42)).toBe(false);
        expect(isObject('42')).toBe(false);
        expect(isObject(Symbol('42'))).toBe(false);
        expect(isObject(() => {})).toBe(false);
        // eslint-disable-next-line
        expect(isObject(function () {})).toBe(false);
    });

    it('покрыт типами', () => {
        /* eslint-disable no-unused-expressions */
        (isObject(undefined) as boolean);
        (isObject({}) as boolean);

        const maybeObj: {
            readonly val: number;
        } | void = {val: 42};

        if (isObject(maybeObj)) {
            (maybeObj.val as number);
        }
        if (!isObject(maybeObj)) {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error type here is `never`
            (maybeObj as void);
        }
        if (!isObject(maybeObj)) {
            // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error type here is `never`
            (maybeObj as any);
        }

        /* eslint-enable no-unused-expressions */
    });
});
