import {isFunction} from '..';

describe('isFunction', () => {
    it('выполняет проверку того, что входящее значение - функция', () => {
        expect(isFunction(() => {})).toBe(true);
        // eslint-disable-next-line
        expect(isFunction(function () {})).toBe(true);

        expect(isFunction([])).toBe(false);
        expect(isFunction(undefined)).toBe(false);
        expect(isFunction(null)).toBe(false);
        expect(isFunction(true)).toBe(false);
        expect(isFunction(false)).toBe(false);
        expect(isFunction({})).toBe(false);
        expect(isFunction(42)).toBe(false);
        expect(isFunction('42')).toBe(false);
        expect(isFunction(Symbol('42'))).toBe(false);
    });

    it('покрыт типами', () => {
        /* eslint-disable no-unused-expressions */
        /* eslint-disable no-lone-blocks */
        (isFunction(undefined) as boolean);
        (isFunction(() => {}) as boolean);

        {
            const func: (() => void) | null = () => {};
            if (isFunction(func)) {
                (func as Function);
            }
            if (!isFunction(func)) {
                (func as null);
            }
        }

        {
            const func: (() => void) | null = null;
            if (!isFunction(func)) {
                // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error it is typeguarded btw
                (func as Function);
            }
        }

        /* eslint-enable no-lone-blocks */
        /* eslint-enable no-unused-expressions */
    });
});
