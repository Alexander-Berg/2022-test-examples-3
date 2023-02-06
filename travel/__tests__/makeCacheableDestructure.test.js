/**
 * @jest-environment jsdom
 */
import {makeCacheable, makeCacheableDestructure} from '../index';

describe('makeCacheableDestructure', () => {
    it(
        'Функция с эквивалентными параметрами в объекте должна вызываться ' +
            'только один раз',
        () => {
            const fn = jest.fn(() => 0);
            const params = {a: 1, b: 2, c: [1, 2]};
            const equalParams = {...params};
            const mixParams = {b: 2, a: 1, c: params.c};
            const anotherParams = {...params, c: [1, 2]};

            const cachedFn = makeCacheableDestructure(fn);

            cachedFn(params);
            cachedFn(params);
            cachedFn(equalParams);
            cachedFn(equalParams);
            cachedFn(mixParams);
            cachedFn(mixParams);
            cachedFn(anotherParams);
            cachedFn(anotherParams);

            expect(fn.mock.calls).toEqual([[params], [anotherParams]]);
        },
    );

    it('Если функция возвращает undefined, то ее результ не кешируется', () => {
        const fn = jest.fn();
        const params = {a: 1, b: 2, c: [1, 2]};

        const cachedFn = makeCacheable(fn);

        cachedFn(params);
        cachedFn(params);
        cachedFn(params);

        expect(fn.mock.calls).toEqual([[params], [params], [params]]);
    });
});
