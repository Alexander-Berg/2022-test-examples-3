import { KeyValueCacheStrategy } from '../KeyValueCacheStrategy';
import { CacheStrategy } from '../CacheStrategy';

describe('KeyValueCacheStrategy', () => {
    describe('#compare', () => {
        it('comparator should work properly', () => {
            const cache = new KeyValueCacheStrategy<number[], number>(
                (args) => args[2],
                [0, 1],
            );

            cache.reset([1, 2, 3]);

            expect(cache.compare([1, 2, 3])).toBeTruthy();
            expect(cache.compare([1, 2, 4])).toBeTruthy();
            expect(cache.compare([1, 3, 3, 5])).toBeFalsy();
            expect(cache.compare([])).toBeFalsy();

            cache.reset([]);

            expect(cache.compare([])).toBeTruthy();
        });
    });

    describe('#update', () => {
        it('values update should work properly', () => {
            const cache = new KeyValueCacheStrategy<number[], number>(
                (args) => args[2],
                [0, 1],
            );

            cache.reset([1, 2, 3]);

            expect(cache.get()).toBe(CacheStrategy.UNSET);

            cache.update(4);

            expect(cache.get()).toBe(4);

            cache.update(3);

            expect(cache.get()).toBe(3);

            cache.reset([2, 5, 3]);

            expect(cache.get()).toBe(CacheStrategy.UNSET);

            cache.update(5);

            expect(cache.get()).toBe(5);
        });
    });
});
