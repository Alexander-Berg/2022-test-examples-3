import { getOffsetsToFetch, FetchCache, BULK_CAROUSELS_LIMIT } from '../useHomeFetch';

describe('Функция getOffsetsToFetch без балковой ручки', () => {
    it('Без балковой ручки в home загружать соседние', () => {
        expect(getOffsetsToFetch({ '-1': true }, 5, false, 'home')).toEqual([0]);
        expect(getOffsetsToFetch({ '0': true }, 5, false, 'home', 0)).toEqual([-1, 1]);
        expect(getOffsetsToFetch({ '-1': true, '0': true }, 5, false, 'home', 0)).toEqual([1]);
        expect(getOffsetsToFetch({ '1': true, '2': true, '3': true }, 5, false, 'home', 3)).toEqual([4]);
    });

    it('Без балковой ручки в home загружать параллельно', () => {
        expect(getOffsetsToFetch({}, 5, false, 'home')).toEqual([-1, 0]);
        expect(getOffsetsToFetch({ '1': true }, 5, false, 'home', 1)).toEqual([0, 2]);

        // Например, вернулись из плеера на карусель, при этом есть cacheHash, можно грузить параллельно
        expect(getOffsetsToFetch({}, 5, false, 'home', 2)).toEqual([1, 2, 3]);
    });

    it('Без балковой ручки загружать на 3 карусели вперёд', () => {
        expect(getOffsetsToFetch({ '1': true, '2': true, '3': true }, 5, false, 'home', 2)).toEqual([4]);
        expect(getOffsetsToFetch({ '0': true, '1': true, '2': true, '3': true }, 5, false, 'home', 1)).toEqual([4]);
    });

    it('Без балковой ручки не загружать, когда не надо', () => {
        expect(getOffsetsToFetch({ '1': true, '2': true, '3': true, '4': true }, 5, false, 'home', 3)).toEqual([]);
        expect(getOffsetsToFetch({ '1': true, '2': true, '3': true, '4': true }, 5, false, 'home', 4)).toEqual([]);
    });

    it('Без балковой ручки не в home загружать соседние', () => {
        expect(getOffsetsToFetch({}, 5, false, 'movie')).toEqual([0]);
        expect(getOffsetsToFetch({ '0': true }, 5, false, 'movie', 0)).toEqual([1]);
        expect(getOffsetsToFetch({ '1': true }, 5, false, 'movie', 1)).toEqual([0, 2]);
    });
});

describe('Функция getOffsetsToFetch', () => {
    it('В home загружать соседние', () => {
        let cache: FetchCache = { '-1': true };
        let expectedCache: FetchCache = { ...cache };
        let offset = -1;
        expect(getOffsetsToFetch(cache, 5, true, 'home', offset)).toEqual([0]);
        for (let i = 0; i < BULK_CAROUSELS_LIMIT; i++) {
            expectedCache[i] = true;
        }
        expect(cache).toEqual(expectedCache);

        cache = { '0': true };
        expect(getOffsetsToFetch(cache, 5, true, 'home')).toEqual([-1, 1]);
        expect(cache).toEqual({ '-1': true, '0': true, '1': true, '2': true, '3': true });
    });

    it('Не в home загружать соседние', () => {
        let cache: FetchCache = { '0': true };
        let expectedCache: FetchCache = { ...cache };
        let offset = 0;
        expect(getOffsetsToFetch(cache, 5, true, 'movie', offset)).toEqual([1]);
        for (let i = 0; i < BULK_CAROUSELS_LIMIT; i++) {
            expectedCache[1 + i] = true;
        }
        expect(cache).toEqual(expectedCache);

        cache = { '2': true };
        expectedCache = { ...cache };
        offset = 2;
        expect(getOffsetsToFetch(cache, 5, true, 'movie', offset)).toEqual([0, 3]);
        for (let i = 0; i < BULK_CAROUSELS_LIMIT * 2; i++) {
            expectedCache[i] = true;
        }
        expect(cache).toEqual(expectedCache);
    });

    it('Не загружать, когда не надо', () => {
        expect(getOffsetsToFetch({ '0': true, '1': true, '2': true, '3': true, '4': true }, 5, true, 'home', 3)).toEqual([]);
        expect(getOffsetsToFetch({ '0': true, '1': true, '2': true, '3': true, '4': true }, 5, true, 'home', 4)).toEqual([]);
    });
});
