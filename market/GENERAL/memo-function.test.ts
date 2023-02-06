import {
    memoFunction,
    memoisable,
    memoStorage,
    memoKey,
    dropMemo,
    clearMemo,
} from './memo-function';

test('defaults - hit', function () {
    const fn = jest.fn((a: number, b: string, c: {}) => ({a, b, c}));
    const memoised = memoFunction(fn);

    const first = memoised(1, 'Qwer', {a: 1, b: 2});
    const second = memoised(1, 'Qwer', {b: 2, a: 1});

    expect(first).toBe(second);
    expect(fn).toHaveBeenCalledTimes(1);
});

test('defaults - miss', function () {
    const fn = jest.fn();
    const memoised = memoFunction(fn);

    memoised(1, 'Qwer', {a: 1, b: 2});
    memoised(1, 'Qwer', {a: 2, b: 1});

    expect(fn).toHaveBeenCalledTimes(2);
});

test('defaults - throw sync', function () {
    const fn = jest.fn((a: string) => { throw new Error(a); });
    const memoised = memoFunction(fn);

    expect(() => memoised('err')).toThrow('err');
    expect(() => memoised('err')).toThrow('err');

    expect(fn).toHaveBeenCalledTimes(2);
});

test('defaults - throw async', async function () {
    const fn = jest.fn((a: string) => Promise.reject(new Error(a)));
    const memoised = memoFunction(fn);

    memoised('err').catch(() => null);
    await expect(memoised('err')).rejects.toEqual(Error('err'));
    await expect(memoised('err')).rejects.toEqual(Error('err'));

    expect(fn).toHaveBeenCalledTimes(2);
});

test('defaults - dropMemo hit', function () {
    const fn = jest.fn();
    const memoised = memoFunction(fn);

    memoised(1, 'Qwer', {a: 1, b: 2});
    memoised(1, 'Qwer', {a: 1, b: 2});

    expect(fn).toHaveBeenCalledTimes(1);

    memoised[dropMemo](1, 'Qwer', {a: 1, b: 2});
    memoised(1, 'Qwer', {a: 1, b: 2});

    expect(fn).toHaveBeenCalledTimes(2);
});

test('defaults - dropMemo miss', function () {
    const fn = jest.fn();
    const memoised = memoFunction(fn);

    memoised(1, 'Qwer', {a: 1, b: 2});
    memoised(1, 'Qwer', {a: 1, b: 2});

    expect(fn).toHaveBeenCalledTimes(1);

    memoised[dropMemo](1, 'Qwer', {a: 2, b: 2});
    memoised(1, 'Qwer', {a: 1, b: 2});

    expect(fn).toHaveBeenCalledTimes(1);
});

test('defaults - clearMemo', function () {
    const fn = jest.fn();
    const memoised = memoFunction(fn);

    memoised(1, 'Qwer', {a: 1, b: 2});
    memoised(1, 'Qwer', {a: 1, b: 2});

    expect(fn).toHaveBeenCalledTimes(1);

    memoised[clearMemo]();
    memoised(1, 'Qwer', {a: 1, b: 2});

    expect(fn).toHaveBeenCalledTimes(2);
});

test('custom cache', function () {
    const cache = new Map();
    const fn = jest.fn();
    fn[memoStorage] = () => cache;

    const memoised = memoFunction(fn);

    memoised('123');
    memoised('123');

    expect(fn).toHaveBeenCalledTimes(1);

    const memoised2 = memoFunction(fn);
    memoised2('123');

    expect(fn).toHaveBeenCalledTimes(1);
});

test('custom key', function () {
    const fn = jest.fn((a: string, b: string) => a + b);
    fn[memoKey] = (a, b) => b;

    const memoised = memoFunction(fn);

    memoised('123', '1');
    memoised('24352', '1');

    expect(fn).toHaveBeenCalledTimes(1);

    memoised('123', '2');

    expect(fn).toHaveBeenCalledTimes(2);
});

test('memoisable', function () {
    const fn = jest.fn((a, b) => a + b);

    const cache = new Map();
    const fnm = memoisable(
        () => cache,
        (a, b) => b,
        fn,
    );

    const memoised = memoFunction(fnm);

    expect(memoised('123', 1)).toBe('1231');
    memoised('12353', 1);

    expect(fn).toHaveBeenCalledTimes(1);

    const memoised2 = memoFunction(fnm);
    memoised2('12s3', 1);

    expect(fn).toHaveBeenCalledTimes(1);

    memoised2('12312s3', 2);
    memoised('123dee', 2);

    expect(fn).toHaveBeenCalledTimes(2);
});

test('context - self', function () {
    const fn = jest.fn(function () { return this.value; });
    const memoised = memoFunction(fn);

    expect(memoised.call({value: 123})).toBe(123);
});

test('context - key', function () {
    const fn = jest.fn();
    fn[memoKey] = function () { return this.value; };

    const memoised = memoFunction(fn);

    memoised.call({value: 123}, 123);
    memoised.call({value: 123}, 321);

    expect(fn).toHaveBeenCalledTimes(1);
});
