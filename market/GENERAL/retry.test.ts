import {constantRetry, quadraticRetry, logariphmicRetry, exponentialRetry, manualRetry} from './retry';

function runTest(fn) {
    const result = [];
    for (let i = 0; i < 5; i += 1) {
        result.push(fn(i));
    }
    return result;
}

test('constantRetry', function () {
    expect(runTest(constantRetry(10))).toEqual([10, 10, 10, 10, 10]);
});

test('quadraticRetry', function () {
    expect(runTest(quadraticRetry(1, 2, 3))).toEqual([3, 6, 11, 18, 27]);
});

test('logariphmicRetry', function () {
    expect(runTest(logariphmicRetry(10, 10, 2))).toEqual([
        20, 25.849625007211557, 30, 33.219280948873624, 35.84962500721156,
    ]);
});

test('exponentialRetry', function () {
    expect(runTest(exponentialRetry(10, 10, 2, 2))).toEqual([
        20, 50, 170, 650, 2570,
    ]);
});

test('manualRetry', function () {
    expect(runTest(manualRetry([777, 42, 282], 100))).toEqual([777, 42, 282, 100, 100]);
});
