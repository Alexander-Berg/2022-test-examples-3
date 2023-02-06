'use strict';

jest.spyOn(global, 'setTimeout');

const TimeCache = require('./time-cache.js');

test('new cache', () => {
    const cache = new TimeCache(10);

    expect(cache.isEmpty()).toBeTruthy();
    expect(cache.get()).toBeNull();
});

test('set / get', () => {
    jest.useFakeTimers('legacy');
    const cache = new TimeCache(100);
    cache.set('FOO');

    expect(setTimeout).toHaveBeenCalledTimes(1);
    expect(setTimeout).toHaveBeenLastCalledWith(expect.any(Function), 100);

    expect(cache.get()).toBe('FOO');
    expect(cache.isEmpty()).toBeFalsy();
});

test('set / wait / get', () => {
    jest.useFakeTimers('legacy');
    const cache = new TimeCache(2000);
    cache.set('FOO');

    expect(cache.get()).toBe('FOO');
    expect(cache.isEmpty()).toBeFalsy();

    jest.advanceTimersByTime(1000);
    expect(cache.isEmpty()).toBeFalsy();

    jest.advanceTimersByTime(1000);
    expect(cache.isEmpty()).toBeTruthy();
});
