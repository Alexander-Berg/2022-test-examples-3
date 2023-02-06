'use strict';

const PromiseTimedCache = require('./promise-timed-cache.js');

let cache;
let promise1;
let promise2;

beforeEach(() => {
    cache = new PromiseTimedCache();
    promise1 = Promise.resolve('1');
    promise2 = Promise.resolve('2');
    jest.useFakeTimers();
});

afterEach(() => {
    jest.useRealTimers();
});

test('должен возвращать закешированный промис', () => {
    cache.set('AA', promise1);
    cache.set('BB', promise2);

    expect(cache.get('AA')).toBe(promise1);
    expect(cache.get('BB')).toBe(promise2);
});

test('должен работать метод remove', () => {
    cache.set('AA', promise1);
    cache.set('BB', promise2);
    cache.remove('AA');
    expect(cache.get('AA')).toBeNull();
    expect(cache.get('BB')).toBe(promise2);
});

test('должен работать метод clear', () => {
    cache.set('AA', promise1);
    cache.set('BB', promise2);
    cache.clear();
    expect(cache.get('AA')).toBeNull();
    expect(cache.get('BB')).toBeNull();
});

test('должен работать таймер', async () => {
    cache.set('AA', promise1, 1);
    cache.set('BB', promise2, 2);

    expect(cache.get('AA')).toBe(promise1);
    expect(cache.get('BB')).toBe(promise2);

    await promise1;
    await promise2;

    jest.advanceTimersByTime(1500);

    expect(cache.get('AA')).toBeNull();
    expect(cache.get('BB')).toBe(promise2);

    jest.advanceTimersByTime(1000);

    expect(cache.get('AA')).toBeNull();
    expect(cache.get('BB')).toBeNull();
});

test('должен удалять значение если промис не прошел', async () => {
    const p = Promise.reject('1');
    cache.set('AA', p, 1);
    expect(cache.get('AA')).toBe(p);

    await p.catch(() => {});

    expect(cache.get('AA')).toBeNull();
});

test('должен по умолчанию удалять промис на следующий тик', async () => {
    cache.set('AA', promise1);
    expect(cache.get('AA')).toBe(promise1);

    await promise1;

    expect(cache.get('AA')).toBeNull();
});

test('должен работать ttl по умолчанию', async () => {
    const cache = new PromiseTimedCache(1);
    cache.set('AA', promise1);

    await promise1;

    jest.advanceTimersByTime(500);

    expect(cache.get('AA')).toBe(promise1);

    jest.advanceTimersByTime(1000);

    expect(cache.get('AA')).toBeNull();
});
