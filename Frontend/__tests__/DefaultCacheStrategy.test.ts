import { DefaultCacheStrategy } from '../DefaultCacheStrategy';
import { CacheStrategy } from '../CacheStrategy';

describe('DefaultCacheStrategy', () => {
    describe('#compare', () => {
        it('comparator should work properly', () => {
            const cache = new DefaultCacheStrategy<number[], number>();

            cache.reset([1, 2, 3]);

            expect(cache.compare([1, 2, 3])).toBeTruthy();
            expect(cache.compare([2, 3])).toBeFalsy();
            expect(cache.compare([1, 1, 2, 3])).toBeFalsy();
            expect(cache.compare([])).toBeFalsy();

            cache.reset([]);

            expect(cache.compare([])).toBeTruthy();
        });
    });

    describe('#update', () => {
        it('values update should work properly', () => {
            const cache = new DefaultCacheStrategy<number[], number>();

            cache.reset([1, 2, 3]);

            expect(cache.get()).toBe(CacheStrategy.UNSET);

            cache.update(4);

            expect(cache.get()).toBe(4);

            cache.update(3);

            expect(cache.get()).toBe(3);

            cache.reset([]);

            expect(cache.get()).toBe(CacheStrategy.UNSET);

            cache.update(5);

            expect(cache.get()).toBe(5);
        });
    });
});
