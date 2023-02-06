'use strict';

const BatchRunner = require('./batch-runner.js');
const { HTTP_ERROR } = require('@yandex-int/duffman').errors;
const { AKITA_ERROR } = require('@ps-int/mail-lib').errors;

const httpError = (statusCode) => new HTTP_ERROR({ statusCode });

const fakeEnvelopes = (count) => Array(count).fill().map(() => ({
    mid: 'FAKE_MID',
    fid: 'FAKE_FID'
}));

const fakeDate = () => parseInt(Date.now() / 1000 - Math.random() * 1000, 10).toString();

let core;
let meta;
let runner;

beforeEach(() => {
    meta = jest.fn();
    core = {
        services: {
            meta
        },
        service: jest.fn().mockReturnValue(meta),
        getServiceOptions: jest.fn()
    };
});

test('throws without core', () => {
    expect(() => {
        runner = new BatchRunner();
    }).toThrow('core missing');
});

test('throws without method', () => {
    expect(() => {
        runner = new BatchRunner(core);
    }).toThrow('method missing');
});

test('throws without params', () => {
    expect(() => {
        runner = new BatchRunner(core, '/method');
    }).toThrow('params missing');
});

describe('#run', () => {
    beforeEach(() => {
        const params = {
            fid: '1',
            since: fakeDate()
        };

        const options = {
            batchMaxSize: 5
        };

        runner = new BatchRunner(core, '/method', params, options);

        meta.mockResolvedValueOnce({ envelopes: fakeEnvelopes(5) })
            .mockResolvedValueOnce({ envelopes: fakeEnvelopes(5) })
            .mockResolvedValueOnce({ envelopes: fakeEnvelopes(2) });
    });

    it('empty when `since` is in future', async () => {
        const since = parseInt(Date.now() / 1000 + 10, 10).toString();
        runner.params = { fid: '1', since };

        const res = await runner.run();

        expect(res.envelopes).toBeArrayOfSize(0);
    });

    it('collects envelopes', async () => {
        const res = await runner.run();

        expect(res.envelopes).toBeArrayOfSize(12);
    });

    it('works with threads_by_folder', async () => {
        meta.mockRestore();
        meta.mockResolvedValueOnce({
            threads_by_folder: {
                envelopes: fakeEnvelopes(4)
            }
        });

        await runner.run();

        expect(runner.envelopes).toBeArrayOfSize(4);
    });

    it('adds extra request with and_more and without till params', async () => {
        meta.mockResolvedValueOnce({ envelopes: fakeEnvelopes(3) });
        runner.params = {
            fid: '1',
            since: fakeDate(),
            and_more: 3
        };

        await runner.run();

        expect(runner.envelopes).toBeArrayOfSize(15);
    });

    it('uses delay', async () => {
        const spy = jest.spyOn(runner, 'delay');
        runner.options = { batchMaxSize: 5, delayAfter: 1, delayStep: 1 };
        meta.mockRestore();
        meta.mockResolvedValueOnce({ envelopes: fakeEnvelopes(5) })
            .mockResolvedValueOnce({ envelopes: fakeEnvelopes(5) })
            .mockResolvedValueOnce({ envelopes: fakeEnvelopes(5) })
            .mockResolvedValueOnce({ envelopes: fakeEnvelopes(5) })
            .mockResolvedValueOnce({ envelopes: fakeEnvelopes(2) });

        await runner.run();

        expect(spy.mock.calls.join(',')).toBe('1,2,3,4');
    });

    it('uses time limit', async () => {
        runner.options = { batchMaxSize: 5, executionTimelimit: 5 };

        meta.mockRestore();
        meta.mockImplementation(() => new Promise((resolve) => {
            setTimeout(() => {
                return resolve({ envelopes: fakeEnvelopes(5) });
            }, 10);
        }));

        await runner.run();

        expect(meta).toHaveBeenCalledTimes(1);
    });

    describe('errors', () => {
        beforeEach(() => {
            meta.mockRestore();
        });

        it('resolves with http error', async () => {
            meta.mockRejectedValueOnce(httpError(400));

            const res = await runner.run();

            expect(res).toEqual(httpError(400));
        });

        it('resolves with custom error', async () => {
            meta.mockRejectedValueOnce(new AKITA_ERROR({ error: 'some error' }));

            const res = await runner.run();

            expect(res).toEqual({ error: 'some error' });
        });
    });
});
