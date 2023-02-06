const proxyquire = require('proxyquire');

const asker = require('../fixtures/asker');
const logger = require('../fixtures/logger-stub');
const BaseAdapter = proxyquire.load('../../../src/server/adapters/base', {
    asker,
}).default;

const mockHost = 'https://mock-host.com';
const defaultTimeout = 2000;

describe('adapters/base', function() {
    let baseAdapter;

    beforeEach(() => {
        baseAdapter = new BaseAdapter('test', 'test', mockHost, logger);
    });

    afterEach(function() {
        asker.resetBehavior();
        asker.reset();
        logger.resetBehavior();
        logger.reset();
    });

    describe('getRequest', function() {
        const mockPath = '/some/test';
        const requestOptions = {};
        const desiredAskerOptions = {
            url: `${mockHost}${mockPath}`,
            query: undefined,
            headers: undefined,
            method: 'GET',
            bodyEncoding: undefined,
            body: undefined,
            timeout: defaultTimeout,
        };

        const response = {
            statusCode: 200,
            headers: {},
            meta: {
                time: {
                    network: 20,
                    total: 21,
                },
                options: requestOptions,
                retries: {
                    used: 0,
                    limit: Number.POSITIVE_INFINITY,
                },
            },
            data: null,
        };

        it('Должен формировать корректный GET запрос', function() {
            asker.withArgs(requestOptions).returns(Promise.resolve(response));
            logger.info.returns();
            baseAdapter.request('GET', mockPath, requestOptions);
            assert.calledWith(asker, desiredAskerOptions);
            assert.notCalled(logger.info);
            assert.notCalled(logger.warn);
            assert.notCalled(logger.error);
        });

        it('Должен логировать запрос', function() {
            const ro = { ...requestOptions, log: true };
            asker.withArgs(ro).returns(Promise.resolve(response));
            logger.info.returns();
            baseAdapter.request('GET', mockPath, ro);
            assert.calledWith(asker, desiredAskerOptions);
            assert.called(logger.info);
            assert.notCalled(logger.warn);
            assert.notCalled(logger.error);
        });

        it('Должен устанавливать bodyEncoding', function() {
            const ro = { ...requestOptions, json: true };
            asker.withArgs(ro).returns(Promise.resolve(response));
            baseAdapter.request('GET', mockPath, ro);
            assert.calledWith(asker, {
                ...desiredAskerOptions,
                bodyEncoding: 'json',
            });
        });

        it('Должен устанавливать хедеры', function() {
            const mockHeaders = { Authorization: 'Bearer token' };
            const ro = { ...requestOptions, headers: mockHeaders };
            asker.withArgs(ro).returns(Promise.resolve(response));
            baseAdapter.request('GET', mockPath, ro);
            assert.calledWith(asker, {
                ...desiredAskerOptions,
                headers: mockHeaders,
            });
        });

        it('Должен устанавливать query params', function() {
            const mockQueryParams = { search: 'name' };
            const ro = { ...requestOptions, query: mockQueryParams };
            asker.withArgs(ro).returns(Promise.resolve(response));
            baseAdapter.request('GET', mockPath, ro);
            assert.calledWith(asker, {
                ...desiredAskerOptions,
                query: mockQueryParams,
            });
        });
    });

    describe('postRequest', function() {
        const mockPath = '/some/test';
        const requestBody = { search: '123' };
        const requestOptions = { json: true, body: requestBody };
        const desiredAskerOptions = {
            url: `${mockHost}${mockPath}`,
            query: undefined,
            headers: undefined,
            method: 'POST',
            bodyEncoding: 'json',
            body: requestBody,
            timeout: defaultTimeout,
        };

        const responseBodyObj = {
            token: '123',
        };

        const responseBody = Buffer.from(JSON.stringify(responseBodyObj));

        const response = {
            statusCode: 201,
            headers: {},
            meta: {
                time: {
                    network: 20,
                    total: 21,
                },
                options: requestOptions,
                retries: {
                    used: 0,
                    limit: Number.POSITIVE_INFINITY,
                },
            },
            data: responseBody,
        };

        it('Должен формировать корректный POST запрос', function() {
            asker.withArgs(requestOptions).returns(Promise.resolve(response));
            logger.info.returns();
            baseAdapter.request('POST', mockPath, requestOptions);
            assert.calledWith(asker, desiredAskerOptions);
            assert.notCalled(logger.info);
            assert.notCalled(logger.warn);
            assert.notCalled(logger.error);
        });

        it('Должен возвращать Buffer в res.data', function(done) {
            asker.callsArgWith(1, null, response);
            logger.info.returns();
            baseAdapter
                .request('POST', mockPath, requestOptions)
                .then((res) => {
                    assert.deepEqual(res.data, responseBody);
                    assert.calledWith(asker, desiredAskerOptions);
                    assert.notCalled(logger.info);
                    assert.notCalled(logger.warn);
                    assert.notCalled(logger.error);
                    done();
                })
                .catch((err) => {
                    done(err);
                });
        });
    });
});
