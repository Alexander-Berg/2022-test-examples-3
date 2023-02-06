/* eslint-disable @typescript-eslint/no-unused-vars,max-classes-per-file */
import {MemoClass, memoClass} from './memo-class';
import {dontCache} from './memo-methods';

test('one instance - cache hit', function () {
    const fn = jest.fn();

    class A {public method(a) { fn(); }}

    const Memoised = memoClass(A);
    const memoised = new Memoised();

    memoised.method(1);
    memoised.method(1);

    expect(fn).toHaveBeenCalledTimes(1);
});

test('one instance - cache miss', function () {
    const fn = jest.fn();

    class A {public method(a) { fn(); }}

    const Memoised = memoClass(A);
    const memoised = new Memoised();

    memoised.method(1);
    memoised.method(2);

    expect(fn).toHaveBeenCalledTimes(2);
});

test('two instances', function () {
    const fn = jest.fn();

    class A {public method(a) { fn(); }}

    const Memoised = memoClass(A);
    const memoised1 = new Memoised();
    const memoised2 = new Memoised();

    memoised1.method(1);
    memoised2.method(1);

    expect(fn).toHaveBeenCalledTimes(2);
});

test('dontCache', function () {
    const fn = jest.fn();

    class A {public method(a) { fn(); }}

    A.prototype.method[dontCache] = true;

    const Memoised = memoClass(A);
    const memoised = new Memoised();

    memoised.method(1);
    memoised.method(1);

    expect(fn).toHaveBeenCalledTimes(2);
});

test('inheritance', function () {
    const fn = jest.fn();

    class A extends MemoClass {public method(a) { fn(); }}

    const memoised = new A();

    memoised.method(1);
    memoised.method(1);

    expect(fn).toHaveBeenCalledTimes(1);
});

test('static methods', function () {
    const fn = jest.fn();

    class A {public static method(a) { fn(); }}

    const Memoised = memoClass(A);

    Memoised.method(1);
    Memoised.method(1);

    expect(fn).toHaveBeenCalledTimes(2);
});

test('dynamic methods', function () {
    const fn = jest.fn();

    class A {public method;}

    const Memoised = memoClass(A);
    const memoised = new Memoised();

    memoised.method = fn;

    memoised.method(123);
    memoised.method(123);

    expect(fn).toHaveBeenCalledTimes(1);
});

test('getters', function () {
    const fn = jest.fn(() => 123);

    class A {public get value() { return fn(); }}

    const Memoised = memoClass(A);

    const memoised = new Memoised();

    const results = [memoised.value, memoised.value];

    expect(results).toEqual([123, 123]);
    expect(fn).toHaveBeenCalledTimes(1);
});

test('properties', function () {
    const value = {a: 1};

    class A {public value = value;}

    const Memoised = memoClass(A);
    const memoised = new Memoised();

    expect(memoised.value).toBe(value);
});
