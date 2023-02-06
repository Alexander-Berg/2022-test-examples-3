jest.useFakeTimers();

import {CacheByParamsInMap} from '../CacheByParamsInMap';

describe('CacheByParamsInMap', () => {
    beforeEach(() => {
        jest.clearAllTimers();
    });

    it('Должны быть обязательно указаны парамтеры size и lifetime', () => {
        expect(() => new CacheByParamsInMap({lifetime: 10})).toThrow();
        expect(() => new CacheByParamsInMap({size: 10})).toThrow();
        expect(
            () => new CacheByParamsInMap({lifetime: 10, size: 10}),
        ).not.toThrow();
    });

    it('В качестве params может быть только объект', () => {
        const cache = new CacheByParamsInMap({size: 5, lifetime: 10});

        expect(() => cache.set(1, true)).toThrow();
        expect(() => cache.set('a', true)).toThrow();
        expect(() => cache.set(true, true)).toThrow();
        expect(cache.set([1], true)).toBeUndefined();
        expect(cache.get([1])).toBe(true);
        expect(cache.set({a: 1}, true)).toBeUndefined();
        expect(cache.get({a: 1})).toBe(true);
    });

    it('Кэш должен работать для эквивалентных объектов', () => {
        const cache = new CacheByParamsInMap({size: 5, lifetime: 10});
        const a = {a: 1, b: 2, c: [1, 2], l: [{first: 'fir', second: 'sec'}]};
        const b = {b: 2, a: 1, l: [{second: 'sec', first: 'fir'}], c: [1, 2]};

        cache.set(a, true);

        expect(cache.get(b)).toBe(true);
    });

    it(
        'Кэш должен иметь определенную длину и удалять более старые значения ' +
            'при достижении максимальной длины',
        () => {
            const cache = new CacheByParamsInMap({size: 2, lifetime: 10});

            cache.set([1], 1);
            cache.set([2], 2);
            cache.set([3], 3);

            expect(cache.get([1])).toBeUndefined();
            expect(cache.get([2])).toBe(2);
            expect(cache.get([3])).toBe(3);
        },
    );

    it('У значений есть время жизни, после которого значения удаляются', () => {
        const cache = new CacheByParamsInMap({size: 2, lifetime: 10});

        cache.set([1], 1);

        expect(cache.get([1])).toBe(1);

        jest.runTimersToTime(5);

        expect(cache.get([1])).toBe(1);

        jest.runTimersToTime(11);

        expect(cache.get([1])).toBeUndefined();
    });

    it('Должна быть возможность удалить значение из кеша', () => {
        const cache = new CacheByParamsInMap({size: 2, lifetime: 10});

        cache.set([1], 1);
        cache.set([2], 2);

        expect(cache.get([1])).toBe(1);

        cache.del([1]);

        expect(cache.get([1])).toBeUndefined();
        expect(cache.get([2])).toBe(2);

        cache.del([3]);

        expect(cache.get([1])).toBeUndefined();
        expect(cache.get([2])).toBe(2);
    });

    describe('Возможно указать неограниченный размер для кеша', () => {
        const cache = new CacheByParamsInMap({size: -1, lifetime: 1000});

        const numbers = new Array(100).fill(0).map((value, key) => key);

        numbers.forEach(number => cache.set([number], number));

        numbers.forEach(number => {
            it(`check cache number ${number}`, () => {
                expect(cache.get([number])).toBe(number);
            });
        });
    });

    it('Возможно указать неограчиненный срок жизни данных в кеше', () => {
        const cache = new CacheByParamsInMap({size: 1, lifetime: -1});

        cache.set([1], 1);

        // мотаем на момент смерти Солнца
        jest.runTimersToTime(5000000000 * 365 * 24 * 60 * 1000);

        // опаньки, Солнца нет, а данные в кэше еще есть
        expect(cache.get([1])).toBe(1);
    });
});
