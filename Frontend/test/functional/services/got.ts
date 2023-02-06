/* eslint-disable */
import * as sinon from 'sinon';
import test from 'ava';
import * as proxyquire from 'proxyquire';
import { GotCounters, getCounterName } from '../../../services/got/counters';
import { Histograms, getHistogramName } from '../../../services/got/histograms';

const sourceName = 'test';

const fake = sinon.fake;

test.beforeEach(() => {
    sinon.restore();
});

test('Wrapper should pass correct path and mix baseUrl to params', async t => {
    const baseUrl = 'https://example.com';
    const spy = fake.resolves({
        statusCode: 200,
        timings: {
            phases: {},
        },
    });

    const testParams = {
        responseType: 'json',
    };

    const createGotWrapper = proxyquire('../../../services/got/utils', { got: { default: spy } })
        .createGotWrapper;

    const testGot = createGotWrapper({
        sourceName,
        baseUrl,
    });

    await testGot('test', testParams);

    t.true(
        spy.calledOnce
    );
});

test('Wrapper should not trim trailing slash', async t => {
    const baseUrl = 'https://example.com';
    const spy = fake.resolves({
        statusCode: 200,
        timings: {
            phases: {},
        },
    });

    const testParams = {
        json: true,
    };

    const createGotWrapper = proxyquire('../../../services/got/utils', { got: { default: spy }})
        .createGotWrapper;

    const testGot = createGotWrapper({
        sourceName,
        baseUrl,
    });

    await testGot('/', testParams);

    t.true(
        spy.calledOnce
    );
});

test('Wrapper should accept absolute url and do not mix baseUrl when not specified', async t => {
    const baseUrl = 'https://example.com';
    const spy = fake.resolves({
        statusCode: 200,
        timings: {
            phases: {},
        },
    });

    const testParams = {
        responseType: 'json',
    };

    const createGotWrapper = proxyquire('../../../services/got/utils', { got: { default: spy }})
        .createGotWrapper;

    const testGot = createGotWrapper({ sourceName });

    await testGot(baseUrl, testParams);

    t.true(spy.calledOnce);
});

test('Wrapper should increment counters when success', async t => {
    const spy = fake.resolves({
        statusCode: 200,
        timings: {
            phases: {},
        },
    });

    const testCounters = new GotCounters();

    const createGotWrapper = proxyquire('../../../services/got/utils', {
        './counters': {
            default: testCounters,
        },
        got: { default: spy },
    }).createGotWrapper;

    const testGot = createGotWrapper({
        sourceName,
        baseUrl: 'https://example.com',
    });

    await testGot('/test');

    t.is(testCounters.counters[getCounterName('test', 'total_summ')], 1);
    t.is(testCounters.counters[getCounterName('test', '2xx_summ')], 1);
    t.is(testCounters.counters[getCounterName('test', '3xx_summ')], 0);
    t.is(testCounters.counters[getCounterName('test', '4xx_summ')], 0);
    t.is(testCounters.counters[getCounterName('test', '5xx_summ')], 0);
    t.is(testCounters.counters[getCounterName('test', 'connection_error_summ')], 0);
    t.is(testCounters.counters[getCounterName('test', 'error_summ')], 0);
    t.is(testCounters.counters[getCounterName('test', 'other_error_summ')], 0);
    t.is(testCounters.counters[getCounterName('test', 'timeout_error_summ')], 0);
});

class MockHTTPError extends Error {
    constructor(public response: any) {
        super('MockError');
    }
}

class MockTimeoutError extends Error {
    constructor (public request: any) {
        super('MockError');
    }
}
class MockRequestError extends Error {
    code = 'ECONNRESET';
    options = {};
}

test('Wrapper should increment HTTPrror', async t => {
    const testCounters = new GotCounters();

    const createGotWrapper = proxyquire('../../../services/got/utils', {
        './counters': {
            default: testCounters,
        },
        got: {
            default: fake.throws(
                new MockHTTPError(
                    {
                        statusCode: 404,
                        statusMessage: 'Not found',
                    },
                ),
            ),
            HTTPError: MockHTTPError,
        },
    }).createGotWrapper;

    const testGot = createGotWrapper({
        sourceName,
        baseUrl: 'https://example.com',
    });

    await t.throwsAsync(testGot('/test'));

    t.is(testCounters.counters[getCounterName('test', '4xx_summ')], 1);
    t.is(testCounters.counters[getCounterName('test', 'error_summ')], 1);
});

test('Wrapper should log HTTPError', async t => {
    const infoSpy = fake();
    const errorLogSpy = fake();

    const createGotWrapper = proxyquire('../../../services/got/utils', {
        '../log': {
            default: {
                info: infoSpy,
                error: errorLogSpy,
            },
        },
        got: {
            default: fake.throws(
                new MockHTTPError(
                    {
                        statusCode: 404,
                        statusMessage: 'Not found',
                        url: 'http://example.com',
                    },
                ),
            ),
            HTTPError: MockHTTPError,
        },
    }).createGotWrapper;

    const testGot = createGotWrapper({
        sourceName,
        baseUrl: 'https://example.com',
    });

    const error = await t.throwsAsync(testGot('/test'));

    t.is(error instanceof MockHTTPError, true);
    t.true(infoSpy.called);
    t.true(errorLogSpy.called);
});

test('Wrapper should increment TimeoutError', async t => {
    const testCounters = new GotCounters();

    const createGotWrapper = proxyquire('../../../services/got/utils', {
        './counters': {
            default: testCounters,
        },
        got: {
            default: fake.throws(new MockTimeoutError({ options: {} })),
            TimeoutError: MockTimeoutError,
        },
    }).createGotWrapper;

    const testGot = createGotWrapper({
        sourceName,
        baseUrl: 'https://example.com',
    });

    await t.throwsAsync(testGot('/test'));

    t.is(testCounters.counters[getCounterName('test', 'timeout_error_summ')], 1);
    t.is(testCounters.counters[getCounterName('test', 'error_summ')], 1);
});

test('Wrapper should log TimeoutError', async t => {
    const infoSpy = fake();
    const errorLogSpy = fake();

    const createGotWrapper = proxyquire('../../../services/got/utils', {
        '../log': {
            default: {
                info: infoSpy,
                error: errorLogSpy,
            },
        },
        got: { 
            default: fake.throws(new MockTimeoutError({ options: {} })),
            TimeoutError: MockTimeoutError,
        },
    }).createGotWrapper;

    const testGot = createGotWrapper({
        sourceName,
        baseUrl: 'https://example.com',
    });

    await t.throwsAsync(testGot('/test'), { instanceOf: MockTimeoutError });
    t.true(infoSpy.called);
    t.true(errorLogSpy.called);
});

test('Wrapper should increment connection error', async t => {
    const testCounters = new GotCounters();

    const createGotWrapper = proxyquire('../../../services/got/utils', {
        './counters': {
            default: testCounters,
        },
        got: { 
            default: fake.throws(
                new MockRequestError(),
            ),
            RequestError: MockRequestError,
        },
    }).createGotWrapper;

    const testGot = createGotWrapper({
        sourceName,
        baseUrl: 'https://example.com',
    });

    await t.throwsAsync(testGot('/test'));

    t.is(testCounters.counters[getCounterName('test', 'connection_error_summ')], 1);
    t.is(testCounters.counters[getCounterName('test', 'error_summ')], 1);
});

test('Wrapper should log connection error', async t => {
    const infoSpy = fake();
    const errorLogSpy = fake();

    const createGotWrapper = proxyquire('../../../services/got/utils', {
        '../log': {
            default: {
                info: infoSpy,
                error: errorLogSpy,
            },
        },
        got: {
            default: fake.throws(
                new MockRequestError()
            ),
            RequestError: MockRequestError,
        },
    }).createGotWrapper;

    const testGot = createGotWrapper({
        sourceName,
        baseUrl: 'https://example.com',
    });

    await t.throwsAsync(testGot('/test'), {
        instanceOf: MockRequestError,
    });
    t.true(infoSpy.called);
    t.true(errorLogSpy.called);
});

test('Wrapper should increment other Errors', async t => {
    const testCounters = new GotCounters();

    const createGotWrapper = proxyquire('../../../services/got/utils', {
        './counters': {
            default: testCounters,
        },
        got: { default: fake.throws(new Error('Test error')), },
    }).createGotWrapper;

    const testGot = createGotWrapper({
        sourceName,
        baseUrl: 'https://example.com',
    });

    await t.throwsAsync(testGot('/test'));

    t.is(testCounters.counters[getCounterName('test', 'other_error_summ')], 1);
    t.is(testCounters.counters[getCounterName('test', 'error_summ')], 1);
});

test('Wrapper should log other Errors', async t => {
    const infoSpy = fake();
    const errorLogSpy = fake();

    const createGotWrapper = proxyquire('../../../services/got/utils', {
        '../log': {
            default: {
                info: infoSpy,
                error: errorLogSpy,
            },
        },
        got: { default: fake.throws(new Error('Test error')), },
    }).createGotWrapper;

    const testGot = createGotWrapper({
        sourceName,
        baseUrl: 'https://example.com',
    });

    await t.throwsAsync(testGot('/test'), { instanceOf: Error });
    t.true(infoSpy.called);
    t.true(errorLogSpy.called);
});

test('Wrapper should write histograms', async t => {
    const spy = fake.resolves({
        statusCode: 200,
        timings: {
            phases: {},
        },
    });
    const testHistograms = new Histograms();

    const createGotWrapper = proxyquire('../../../services/got/utils', {
        './histograms': {
            default: testHistograms,
        },
        got: { default: spy, },
    }).createGotWrapper;

    const testGot = createGotWrapper({
        sourceName,
        baseUrl: 'https://example.com',
    });

    const initialHistogram = JSON.parse(
        JSON.stringify(testHistograms.histograms[getHistogramName('test')]),
    );

    await testGot('/test');

    t.notDeepEqual(initialHistogram, testHistograms.histograms[getHistogramName('test')]);
});

test('Wrapper should use tvm when flag is specified', async t => {
    const spy = fake.resolves({
        statusCode: 200,
        timings: {
            phases: {},
        },
    });
    const tvmSpy = fake.resolves({ [sourceName]: { ticket: '' } });

    const createGot = proxyquire('../../../services/got', {
        got: { default: spy, },
        '../tvm': { getServiceTickets: tvmSpy },
        './utils': { createGotWrapper: () => () => undefined },
    }).createGot;

    const testGot = createGot({
        sourceName,
        baseUrl: 'https://example.com',
        useTvm: true,
    });

    await testGot('/test');

    t.true(tvmSpy.calledOnce);
});

test('Wrapper should not use tvm when flag is omited', async t => {
    const spy = fake.resolves({
        statusCode: 200,
        timings: {
            phases: {},
        },
    });
    const tvmSpy = fake.resolves({ [sourceName]: { ticket: '' } });

    const createGot = proxyquire('../../../services/got', {
        got: { default: spy, },
        '../tvm': { getServiceTickets: tvmSpy },
        './utils': { createGotWrapper: () => () => undefined },
    }).createGot;

    const testGot = createGot({
        sourceName,
        baseUrl: 'https://example.com',
    });

    await testGot('/test');

    t.true(tvmSpy.notCalled);
});
