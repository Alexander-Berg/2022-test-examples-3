/**
 * @jest-environment jsdom
 */
import {makeCacheable} from '../index';

describe('makeCacheable', () => {
    it(
        'Функция с эквивалентными параметрами должна вызываться ' +
            'только один раз',
        () => {
            const fn = jest.fn(() => 0);
            const params = [1, 2, [1, 2]];
            const equalParams = [...params];
            const anotherParams = [1, 2, [1, 2]];

            const cachedFn = makeCacheable(fn);

            cachedFn(...params);
            cachedFn(...params);
            cachedFn(...equalParams);
            cachedFn(...equalParams);
            cachedFn(...anotherParams);
            cachedFn(...anotherParams);

            expect(fn.mock.calls).toEqual([params, anotherParams]);
        },
    );

    it('Если функция возвращает undefined, то ее результа не кешируется', () => {
        const fn = jest.fn();
        const params = [1, 2, [1, 2]];

        const cachedFn = makeCacheable(fn);

        cachedFn(...params);
        cachedFn(...params);
        cachedFn(...params);

        expect(fn.mock.calls).toEqual([params, params, params]);
    });
});
