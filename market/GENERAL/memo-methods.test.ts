import {memoKey} from './memo-function';
import {memoMethods, dontCache} from './memo-methods';

test('cache hit', function () {
    const fn = jest.fn();
    const obj = {method: fn};
    const memoised = memoMethods(obj);

    memoised.method(1);
    memoised.method(1);

    expect(fn).toHaveBeenCalledTimes(1);
});

test('cache miss', function () {
    const fn = jest.fn();
    const obj = {method: fn};
    const memoised = memoMethods(obj);

    memoised.method(1);
    memoised.method(2);

    expect(fn).toHaveBeenCalledTimes(2);
});

test('dontCache', function () {
    const fn = jest.fn();

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const obj = {method: a => fn()};
    obj.method[dontCache] = true;

    const memoised = memoMethods(obj);

    memoised.method(1);
    memoised.method(1);

    expect(fn).toHaveBeenCalledTimes(2);
});

test('context - self, explicit', function () {
    const fn = jest.fn(function () { return this.value; });
    const obj = {method: fn, value: 123};

    const memoised = memoMethods(obj);

    expect(memoised.method()).toBe(123);
});

test('context - key, explicit', function () {
    const fn = jest.fn();

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const obj = {method: a => fn(), value: 123};
    obj.method[memoKey] = function () { return this.value; };

    const memoised = memoMethods(obj);

    memoised.method(123);
    memoised.method(321);

    expect(fn).toHaveBeenCalledTimes(1);
});

test('context - self, implicit', function () {
    const fn = jest.fn(function () { return this.value; });
    const obj = {method: fn};

    const memoised = memoMethods(obj);

    expect(memoised.method.call({value: 123})).toBe(123);
});

test('context - key, implicit', function () {
    const fn = jest.fn();

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const obj = {method: a => fn()};
    obj.method[memoKey] = function () { return this.value; };

    const memoised = memoMethods(obj);

    memoised.method.call({value: 123}, 123);
    memoised.method.call({value: 123}, 321);

    expect(fn).toHaveBeenCalledTimes(1);
});

test('dynamic methods', function () {
    const fn = jest.fn();

    const obj = {};
    const memoised: any = memoMethods(obj);

    memoised.method = fn;

    memoised.method(123);
    memoised.method(123);

    expect(fn).toHaveBeenCalledTimes(1);
});

test('getters', function () {
    const fn = jest.fn(() => 123);

    const obj = {get value() { return fn(); }};
    const memoised = memoMethods(obj);

    const results = [memoised.value, memoised.value];

    expect(results).toEqual([123, 123]);
    expect(fn).toHaveBeenCalledTimes(1);
});

test('properties', function () {
    const value = {a: 1};
    const obj = {value};
    const memoised = memoMethods(obj);

    expect(memoised.value).toBe(value);
});

test('Object.prototype', function () {
    const obj = {a: 1};
    const memoised: any = memoMethods(obj);

    // eslint-disable-next-line no-prototype-builtins
    expect(memoised.hasOwnProperty('a')).toBe(true);

    // eslint-disable-next-line no-prototype-builtins
    expect(memoised.hasOwnProperty('b')).toBe(false);

    memoised.b = 1;
    // eslint-disable-next-line no-prototype-builtins
    expect(memoised.hasOwnProperty('b')).toBe(true);
});
