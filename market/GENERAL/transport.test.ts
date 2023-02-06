/* eslint-disable max-classes-per-file */

import {Transport} from './transport';
import {Context} from './context';

test('setup', function () {
    class SomeTransport extends Transport.setup({
        field: 'value',
    }) {}

    const transport = SomeTransport.factory(new Context(null));

    expect(transport.options).toEqual({field: 'value'});
    expect(transport).toBeInstanceOf(Transport);
});

test('flow', async function () {
    const mock = jest.fn();

    let firstRequest = true;

    class SomeTransport extends Transport.setup({
        field: 'value',
        retry: 2,
    }) {
        public async send(params) {
            mock('send');
            const result = await super.send(params);
            mock('sendEnd');
            return result;
        }

        protected prepareParams(params) {
            mock('prepareParams');
            return super.prepareParams(params);
        }

        protected prepareRequest(params) {
            mock('prepareRequest');
            return super.prepareRequest(params);
        }

        protected async prepareResponse(response) {
            mock('prepareResponse');
            response.success = true;
            return response;
        }

        protected request(request) {
            mock('request');

            if (firstRequest) {
                firstRequest = false;
                throw new Error('fail');
            }

            return super.request(request);
        }

        protected beforeSend(params, request, stats) {
            mock('beforeSend');
            return super.beforeSend(params, request, stats);
        }

        protected afterResponse(params, request, stats, response) {
            mock('afterResponse');
            return super.afterResponse(params, request, stats, response);
        }

        protected afterError(params, request, stats, error, willRetry) {
            mock('afterError');
            return super.afterError(params, request, stats, error, willRetry);
        }

        protected retryAllowed(params, error) {
            mock('retryAllowed');
            return super.retryAllowed(params, error);
        }
    }

    const transport = SomeTransport.factory(new Context(null));

    const result = await transport.send({a: 1});

    expect(result).toEqual({success: true, requestId: 'none'});

    const callSeq = mock.mock.calls.map(call => call[0]);
    expect(callSeq).toEqual([
        'send',

        'prepareParams',
        'prepareRequest',
        'beforeSend',
        'request',

        'retryAllowed',
        'afterError',

        'prepareParams',
        'prepareRequest',
        'beforeSend',
        'request',

        'prepareResponse',
        'afterResponse',

        'sendEnd',
    ]);
});

test('success', async function () {
    class SomeTransport extends Transport.setup({}) {
        protected async prepareResponse(request, response) {
            return super.prepareResponse(request, {success: true, ...response});
        }
    }

    const transport = SomeTransport.factory(new Context(null));

    const result = await transport.send({a: 1});

    expect(result).toEqual({success: true, contentSize: 0});
});

test('fail', async function () {
    const fn = jest.fn();

    class SomeTransport extends Transport.setup({
        retry: 2,
    }) {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        protected async request(request): Promise<any> {
            fn();
            throw new Error('failed');
        }
    }

    const transport = SomeTransport.factory(new Context(null));

    await expect(transport.send({a: 1})).rejects.toEqual(new Error('failed'));
    expect(fn).toHaveBeenCalledTimes(3);
});

test('fail on prepare', async function () {
    const fn = jest.fn();

    class SomeTransport extends Transport.setup({
        retry: 2,
    }) {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        protected async prepareParams(params): Promise<any> {
            fn();
            throw new Error('failed');
        }
    }

    const transport = SomeTransport.factory(new Context(null));

    await expect(transport.send({a: 1})).rejects.toEqual(new Error('failed'));
    expect(fn).toHaveBeenCalledTimes(3);
});

describe('retryDelay as number', function () {
    test('default retryDelay', async function () {
        class SomeTransport extends Transport.setup({
            retry: 1,
        }) {
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            protected async request(request): Promise<any> {
                throw new Error('failed');
            }
        }

        const transport = SomeTransport.factory(new Context(null));

        const ts = Date.now();
        await expect(transport.send({a: 1})).rejects.toEqual(new Error('failed'));

        const duration = Date.now() - ts;
        expect(duration).toBeGreaterThanOrEqual(100);
        expect(duration).toBeLessThanOrEqual(110);
    });

    test('retryDelay from options', async function () {
        class SomeTransport extends Transport.setup({
            retry: 1,
            retryDelay: 50,
        }) {
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            protected async request(request): Promise<any> {
                throw new Error('failed');
            }
        }

        const transport = SomeTransport.factory(new Context(null));
        const ts = Date.now();
        await expect(transport.send({a: 1})).rejects.toEqual(new Error('failed'));

        const duration = Date.now() - ts;
        expect(duration).toBeGreaterThanOrEqual(50);
        expect(duration).toBeLessThanOrEqual(60);
    });

    test('retryDelay from options 0ms allowed', async function () {
        class SomeTransport extends Transport.setup({
            retry: 1,
            retryDelay: 0,
        }) {
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            protected async request(request): Promise<any> {
                throw new Error('failed');
            }
        }

        const transport = SomeTransport.factory(new Context(null));
        const ts = Date.now();
        await expect(transport.send({a: 1})).rejects.toEqual(new Error('failed'));

        const duration = Date.now() - ts;
        expect(duration).toBeGreaterThanOrEqual(0);
        expect(duration).toBeLessThanOrEqual(15);
    });

    test('retryDelay from params', async function () {
        class SomeTransport extends Transport.setup({
            retry: 1,
        }) {
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            protected async request(request): Promise<any> {
                throw new Error('failed');
            }
        }

        const transport = SomeTransport.factory(new Context(null));
        const ts = Date.now();
        await expect(transport.send({a: 1, retryDelay: 50})).rejects.toEqual(new Error('failed'));

        const duration = Date.now() - ts;
        expect(duration).toBeGreaterThanOrEqual(50);
        expect(duration).toBeLessThanOrEqual(65);
    });

    test('retryDelay from params with higher priority', async function () {
        class SomeTransport extends Transport.setup({
            retry: 1,
            retryDelay: 50,
        }) {
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            protected async request(request): Promise<any> {
                throw new Error('failed');
            }
        }

        const transport = SomeTransport.factory(new Context(null));
        const ts = Date.now();
        await expect(transport.send({a: 1, retryDelay: 120})).rejects.toEqual(new Error('failed'));

        const duration = Date.now() - ts;
        expect(duration).toBeGreaterThanOrEqual(120);
        expect(duration).toBeLessThanOrEqual(135);
    });

    test('retryDelay from params 0ms allowed', async function () {
        class SomeTransport extends Transport.setup({
            retry: 1,
            retryDelay: 50,
        }) {
            // eslint-disable-next-line @typescript-eslint/no-unused-vars
            protected async request(request): Promise<any> {
                throw new Error('failed');
            }
        }

        const transport = SomeTransport.factory(new Context(null));
        const ts = Date.now();
        await expect(transport.send({a: 1, retryDelay: 0})).rejects.toEqual(new Error('failed'));

        const duration = Date.now() - ts;
        expect(duration).toBeGreaterThanOrEqual(0);
        expect(duration).toBeLessThanOrEqual(15);
    });
});

describe('retryDelay as fn',  function () {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const fn = jest.fn((a, b) => 1);

    class SomeTransport extends Transport.setup({
        retry: 5,
        retryDelay: fn,
    }) {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        protected async request(request): Promise<any> {
            throw new Error('failed');
        }
    }

    afterEach(() => {
        fn.mockClear();
    });

    const transport = SomeTransport.factory(new Context(null));

    test('retryDelay from options', async function () {
        await expect(transport.send({a: 1})).rejects.toEqual(new Error('failed'));

        const callSeq = fn.mock.calls.map(call => call[0]);
        expect(callSeq).toEqual([0, 1, 2, 3, 4]);

        const callSecondArguments = fn.mock.calls.map(call => call[1]);
        expect(callSecondArguments).toEqual(Array(5).fill(new Error('failed')));
    });

    test('retryDelay from params with higher priority', async function () {
        const retryDelayViaParams = jest.fn();
        await expect(transport.send({a: 1, retryDelay: retryDelayViaParams})).rejects.toEqual(new Error('failed'));

        const callSeq = fn.mock.calls.map(call => call[0]);
        expect(callSeq).toEqual([]);

        const paramsRetryDelaycallSeq = retryDelayViaParams.mock.calls.map(call => call[0]);
        expect(paramsRetryDelaycallSeq).toEqual([0, 1, 2, 3, 4]);

        const callSecondArguments = retryDelayViaParams.mock.calls.map(call => call[1]);
        expect(callSecondArguments).toEqual(Array(5).fill(new Error('failed')));
    });
});

describe('retry',  function () {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const requestMock = jest.fn(async function (request) {
        throw new Error('failed');
    });

    class SomeTransport extends Transport.setup({
        retry: 5,
    }) {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        protected request = requestMock;
    }

    afterEach(() => {
        requestMock.mockClear();
    });

    const transport = SomeTransport.factory(new Context(null));

    test('retry from options', async function () {
        await expect(transport.send({a: 1})).rejects.toEqual(new Error('failed'));
        expect(requestMock).toHaveBeenCalledTimes(5 + 1);
    });

    test('retry from params with higher priority', async function () {
        const retryViaParams = 3;
        await expect(transport.send({a: 1, retry: retryViaParams})).rejects.toEqual(new Error('failed'));

        expect(requestMock).toHaveBeenCalledTimes(3 + 1);
    });
});

describe('retryAllowed', function () {
    let counter = 0;

    const requestMock = jest.fn(async function () {
        throw new CounterError('failed');
    });
    const retryAllowedMock = jest.fn((error: CounterError) => error.counter < 2);

    class CounterError extends Error {
        // eslint-disable-next-line no-plusplus
        public counter = counter++;
    }

    class SomeTransport extends Transport.setup({
        retry: 5,
        retryAllowed: retryAllowedMock,
    }) {
        protected request = requestMock;
    }

    const transport = SomeTransport.factory(new Context(null));

    afterEach(() => {
        counter = 0;
        requestMock.mockClear();
        retryAllowedMock.mockClear();
    });

    test('from options', async function () {
        await expect(transport.send({a: 1})).rejects.toEqual(new Error('failed'));
        expect(requestMock).toHaveBeenCalledTimes(3);
        expect(retryAllowedMock).toHaveBeenCalledTimes(3);
    });

    test('from params with higher priority', async function () {
        const retryAllowed = () => false;
        await expect(transport.send({a: 1, retryAllowed})).rejects.toEqual(new Error('failed'));
        expect(retryAllowedMock).not.toHaveBeenCalled();
        expect(requestMock).toHaveBeenCalledTimes(1);
    });
});
