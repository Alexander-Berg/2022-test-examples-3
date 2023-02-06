import { delay } from './time';
import { Cache, ICacheParams } from './Cache';

const params: ICacheParams = {
    storeName: 'store',
};

describe('Cache', () => {
    it('Should create IdbCache', () => {
        expect(new Cache(params)).toBeInstanceOf(Cache);
    });

    it('Should return undefined for non existent keys', async() => {
        const cache = new Cache<number>(params);

        expect(await cache.get('foo')).toBeUndefined();
    });

    it('Should set to storage', async() => {
        const cache = new Cache<number>(params);

        await cache.set('foo1xxx', 42);

        expect(await cache.get('foo1xxx')).toBe(42);
    });

    it('Should support ttl', async() => {
        const cache = new Cache<number>(params);

        await cache.set('foo2', 42, 100);
        expect(await cache.get('foo2')).toBe(42);

        await delay(500);

        expect(await cache.get('foo2')).toBeUndefined();
    });

    it('Should delete key', async() => {
        const cache = new Cache<number>(params);

        await cache.set('foo3', 42, 100);
        expect(await cache.get('foo3')).toBe(42);

        await cache.del('foo3');
        expect(await cache.get('foo3')).toBeUndefined();
    });

    it('Should not set item with invalid ttl', async() => {
        const cache = new Cache<number>({ storeName: 'invalid-ttl' });

        await cache.set('foo', 42, -1);
        expect(await cache.get('foo')).toBeUndefined();

        await cache.set('foo', 42, 0);
        expect(await cache.get('foo')).toBeUndefined();

        await cache.set('foo', 42, Infinity);
        expect(await cache.get('foo')).toBeUndefined();

        await cache.set('foo', 42, NaN);
        expect(await cache.get('foo')).toBeUndefined();
    });

    it('Should clear cache', async() => {
        const cacheFoo = new Cache<number>({ storeName: 'foo' });
        const cacheBar = new Cache<number>({ storeName: 'bar' });

        await cacheFoo.set('x', 1);
        await cacheFoo.set('y', 2);

        expect(await cacheFoo.get('x')).toBe(1);
        expect(await cacheFoo.get('y')).toBe(2);

        await cacheBar.set('x', 1);
        await cacheBar.set('y', 2);

        expect(await cacheBar.get('x')).toBe(1);
        expect(await cacheBar.get('y')).toBe(2);

        await cacheFoo.clear();

        expect(await cacheFoo.get('x')).toBeUndefined();
        expect(await cacheFoo.get('y')).toBeUndefined();

        expect(await cacheBar.get('x')).toBe(1);
        expect(await cacheBar.get('y')).toBe(2);

        await cacheBar.clear();

        expect(await cacheBar.get('x')).toBeUndefined();
        expect(await cacheBar.get('y')).toBeUndefined();
    });

    it('Should return keys', async() => {
        const cache = new Cache<number>({ storeName: 'keys' });

        await cache.set('foo', 1);
        await cache.set('bar', 2);

        expect(await cache.keys()).toEqual(['foo', 'bar']);
    });

    it('Should not throw error on get if engine throws', async() => {
        const cache = new Cache<number>({ storeName: 'fail-get' });

        Object.defineProperty(localStorage, 'fail-get:foo', {
            get() {
                throw new Error('Unexpected');
            },
        });

        await cache.get('foo');
    });

    it('Should not throw error on set if engine throws', async() => {
        const cache = new Cache<number>({ storeName: 'fail-set' });

        Object.defineProperty(localStorage, 'fail-set:foo', {
            set() {
                throw new Error('Unexpected');
            },
        });

        await cache.set('foo', 42);
    });

    it('Should not throw error on del if engine throws', async() => {
        const cache = new Cache<number>({ storeName: 'fail-del' });

        Object.defineProperty(localStorage, 'fail-del:foo', {
            configurable: false,
        });

        await cache.del('foo');
    });

    it('Should not throw error on keys if engine throws', async() => {
        const cache = new Cache<number>({ storeName: 'fail-keys' });

        Object.defineProperty(localStorage, 'fail-keys:foo', {
            enumerable: true,
            get() {
                throw new Error('Unexpected');
            },
        });

        await cache.keys();
    });

    it('Should not throw error on clear if engine throws', async() => {
        const cache = new Cache<number>({ storeName: 'fail-clear' });

        Object.defineProperty(localStorage, 'fail-clear:foo', {
            enumerable: true,
            get() {
                throw new Error('Unexpected');
            },
        });

        await cache.clear();
    });

    it('Should remove extra keys from engine after set', async() => {
        const cache = new Cache<number>({ storeName: 'cache-opt' });

        await cache.set('foo', 42, 10);

        expect(localStorage['cache-opt:foo']).not.toBeUndefined();

        await new Promise(resolve => {
            setTimeout(resolve, 1100);
        });

        expect(localStorage['cache-opt:foo']).toBeUndefined();
    });
});
