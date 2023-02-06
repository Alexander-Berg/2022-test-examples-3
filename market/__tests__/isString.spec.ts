import {isString} from '..';

describe('isString', () => {
    it('выполняет проверку того, что входящее значение - строка', () => {
        expect(isString('42')).toBe(true);

        expect(isString(undefined)).toBe(false);
        expect(isString(null)).toBe(false);
        expect(isString(true)).toBe(false);
        expect(isString(false)).toBe(false);
        expect(isString({})).toBe(false);
        expect(isString(42)).toBe(false);
        expect(isString(Symbol('42'))).toBe(false);
        expect(isString([4, 2])).toBe(false);
        expect(isString(() => {})).toBe(false);
        // eslint-disable-next-line
        expect(isString(function () {})).toBe(false);
    });

    it('покрыт типами', () => {
        /* eslint-disable no-unused-expressions */
        /* eslint-disable no-lone-blocks */
        (isString(undefined) as boolean);
        (isString('42') as boolean);

        {
            const str: string | undefined | null = '42';
            if (isString(str)) {
                (str as string);
            }
            if (!isString(str)) {
                // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error
                (str as null | void);
            }
        }

        {
            const str: string | undefined | null = null;
            if (!isString(str)) {
                // https://st.yandex-team.ru/MARKETFRONTECH-2258 @ts-expect-error type here is `never`
                (str as string);
            }
        }

        /* eslint-enable no-lone-blocks */
        /* eslint-enable no-unused-expressions */
    });
});
