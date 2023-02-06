/**
 * @jest-environment jsdom
 */
import {bindWithCache} from '../index';

describe('bindWithCache', () => {
    it('Функции нет - венуть null', () => {
        expect(bindWithCache(123, {}, 22, 11)).toBe(null);
    });

    it('Вернуть закэшированную, забинженную функцию', () => {
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        const fn = () => {};
        const self = {};

        expect(bindWithCache(fn, self, 22, 11)).toBe(
            bindWithCache(fn, self, 22, 11),
        );
        expect(
            bindWithCache(fn, self, 22, 11) !== bindWithCache(fn, self, 21, 11),
        ).toBe(true);
        expect(
            bindWithCache(fn, self, 22, 11) !== bindWithCache(fn, self, 21),
        ).toBe(true);
        expect(
            bindWithCache(fn, self, 22, 11) !==
                // eslint-disable-next-line @typescript-eslint/no-empty-function
                bindWithCache(() => {}, self, 22, 11),
        ).toBe(true);
    });
});
