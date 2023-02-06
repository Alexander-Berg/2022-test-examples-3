import '../../noscript';
import '../../../../components/models/queues/queue-request';

describe('queueRequest', () => {
    let queueRequest;

    beforeEach(() => {
        queueRequest = ns.Model.get('queueRequest');
        queueRequest.TIMEOUT_RETRY_REQUEST = 50;
    });

    afterEach(() => {
        queueRequest.destroy();
    });

    it('В случае успешных запросов push должен возвращать зарезолвленный промис', (done) => {
        ns.Model.define('test-model', {
            methods: {
                request() {
                    this.setData({ field: 'value' });
                    return Vow.fulfill();
                }
            }
        });

        queueRequest.push({ id: 'test-model', params: {} }).always((promise) => {
            const model = promise.valueOf();
            expect(promise.isFulfilled());
            expect(model.getData()).toEqual({ field: 'value' });
            ns.Model.destroy(model);
            done();
        });
    });

    it('do-модели не должны ретраиться', (done) => {
        ns.Model.define('do-test-model', {
            methods: {
                request: jest.fn(function() {
                    this.setError({ id: 'HTTP_500' });
                    return Vow.reject();
                })
            }
        });

        queueRequest.push({ id: 'do-test-model', params: {} }).always((promise) => {
            const model = promise.valueOf();
            expect(model.request).toBeCalledTimes(1);
            expect(promise.isRejected());
            expect(model.getError()).toEqual({ id: 'HTTP_500' });
            ns.Model.destroy(model);
            done();
        });
    });

    it('Модели с методом shouldRetryFails должны ретраиться 3 раза', (done) => {
        ns.Model.define('do-test-model-with-retry', {
            methods: {
                shouldRetryFails: () => true,
                request: jest.fn(function() {
                    this.setError({ id: 'HTTP_500' });
                    return Vow.reject();
                })
            }
        });

        queueRequest.push({ id: 'do-test-model-with-retry', params: {} }).always((promise) => {
            const model = promise.valueOf();
            expect(model.request).toBeCalledTimes(4);
            expect(promise.isRejected());
            expect(model.getError()).toEqual({ id: 'HTTP_500' });
            ns.Model.destroy(model);
            done();
        });
    });

    it('Модели с методом retryOffline должны ждать выхода в online перед ретраями', (done) => {
        let requestCalls = 0;
        ns.Model.define('do-test-model-with-retry-offline', {
            methods: {
                retryOffline: () => true,
                request: jest.fn(function() {
                    requestCalls++;
                    if (requestCalls === 1) {
                        this.setError({ id: 'HTTP_0' });
                        return Vow.reject();
                    }

                    if (requestCalls === 2) {
                        this.setData({ field: 'value' });
                        return Vow.fulfill();
                    }
                })
            }
        });

        const originalFetch = global.fetch;
        global.fetch = jest.fn(() => Promise.resolve());

        queueRequest.push({ id: 'do-test-model-with-retry-offline', params: {} }).always((promise) => {
            const model = promise.valueOf();
            expect(model.request).toBeCalledTimes(2);
            expect(global.fetch).toBeCalledWith('/check-online');
            expect(promise.isFulfilled());
            expect(model.getData()).toEqual({ field: 'value' });
            ns.Model.destroy(model);
            global.fetch = originalFetch;
            done();
        });
    });
});
