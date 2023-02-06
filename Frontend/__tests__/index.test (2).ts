import express, { NextFunction, Request, Response } from 'express';
import got from 'got';
import nock from 'nock';
import request from 'supertest';

import ExpressTvmMiddleware from '../src/index';
import { ITickets } from '../src/interfaces';

interface IHeaders {
    [name: string]: string;
}

interface INockOptions {
    query: nock.DataMatcherMap;
    headers?: IHeaders;
}

function nockTvmServer({ headers, query }: INockOptions, status: number, result?: ITickets) {
    const serverUrl = 'http://localhost:1/';
    const reqheaders = {
        accept: 'application/json',
        authorization: 'tvmtool-development-access-token',
        ...headers,
    };

    nock(serverUrl, { reqheaders })
        .persist()
        .get('/tvm/tickets')
        .query(query)
        .reply(status, result);
}

describe('Express-tvm', function() {
    beforeEach(() => {
        nockTvmServer({ query: { dsts: 'blackbox,geobase', src: 'commentator' } }, 200, {
            blackbox: {
                ticket: 'blackbox-ticket',
                tvm_id: 224, // eslint-disable-line camelcase
            },
            geobase: {
                ticket: 'geobase-ticket',
                tvm_id: 225, // eslint-disable-line camelcase
            },
        });

        nockTvmServer({ query: { dsts: 'blackbox', src: 'commentator' } }, 200, {
            blackbox: {
                ticket: 'blackbox-ticket',
                tvm_id: 224, // eslint-disable-line camelcase
            },
        });

        nockTvmServer({ query: { dsts: 'blackbox', src: 'commentator', customQueryParam: 'a' } }, 200, {
            blackbox: {
                ticket: 'blackbox-ticket',
                tvm_id: 224, // eslint-disable-line camelcase
            },
        });

        nockTvmServer({ query: { dsts: 'blackbox', src: 'commentator', customQueryParam: 'b' } }, 200, {
            blackbox: {
                ticket: 'blackbox-ticket',
                tvm_id: 224, // eslint-disable-line camelcase
            },
        });

        nockTvmServer({ query: { dsts: 'blackbox', src: 'commentator', customQueryParam: 'a', cacheableCustomQueryParam: 'a' } }, 200, {
            blackbox: {
                ticket: 'blackbox-ticket',
                tvm_id: 224, // eslint-disable-line camelcase
            },
        });

        nockTvmServer({ query: { dsts: 'blackbox', src: 'commentator', customQueryParam: 'a', cacheableCustomQueryParam: 'b' } }, 200, {
            blackbox: {
                ticket: 'blackbox-ticket',
                tvm_id: 224, // eslint-disable-line camelcase
            },
        });

        nockTvmServer({ query: { dsts: 'blackbox', src: 'commentator', customQueryParam: 'b', cacheableCustomQueryParam: 'a' } }, 200, {
            blackbox: {
                ticket: 'blackbox-ticket',
                tvm_id: 224, // eslint-disable-line camelcase
            },
        });

        nockTvmServer({ query: { dsts: 'blackbox', src: 'commentator', customQueryParam: 'b', cacheableCustomQueryParam: 'b' } }, 200, {
            blackbox: {
                ticket: 'blackbox-ticket',
                tvm_id: 224, // eslint-disable-line camelcase
            },
        });

        nockTvmServer({ query: { dsts: 'geobase', src: 'commentator' } }, 200, {
            geobase: {
                ticket: 'geobase-ticket',
                tvm_id: 225, // eslint-disable-line camelcase
            },
        });

        nockTvmServer({ query: { dsts: '', src: 'commentator' } }, 400);

        nockTvmServer({
            headers: { header: 'value' },
            query: { dsts: 'someBackend', src: 'commentator', custom: '42' },
        }, 200, {
            someBackend: {
                ticket: 'foo',
                tvm_id: 42, // eslint-disable-line camelcase
            },
        });
    });

    test('Should support custom options from requestOptions()', async() => {
        const expressTvmMiddleware = ExpressTvmMiddleware({
            clientId: 'commentator',
            destinations: ['someBackend'],
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
            requestOptions(req: Request) {
                return {
                    query: {
                        custom: String(req.query.foo),
                    },
                    headers: {
                        header: 'value',
                    },
                };
            },
        });

        const app = express()
            .use(expressTvmMiddleware)
            .use((req: Request, res: Response) => {
                expect(req.tvm && req.tvm.commentator).toEqual({
                    tickets: {
                        someBackend: {
                            ticket: 'foo',
                            tvm_id: 42, // eslint-disable-line camelcase
                        },
                    },
                });

                res.sendStatus(200);
            });

        await request(app)
            .get('/?foo=42')
            .expect(200);
    });

    test('Should support custom options from requestOptions object', async() => {
        const expressTvmMiddleware = ExpressTvmMiddleware({
            clientId: 'commentator',
            destinations: ['someBackend'],
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
            requestOptions: {
                query: 'custom=42',
                headers: { header: 'value' },
            },
        });

        const app = express()
            .use(expressTvmMiddleware)
            .use((req: Request, res: Response) => {
                expect(req.tvm && req.tvm.commentator).toEqual({
                    tickets: {
                        someBackend: {
                            ticket: 'foo',
                            tvm_id: 42, // eslint-disable-line camelcase
                        },
                    },
                });

                res.sendStatus(200);
            });

        await request(app)
            .get('/')
            .expect(200);
    });

    test('should get tickets from tvm daemon', async() => {
        const expressTvmMiddleware = ExpressTvmMiddleware({
            clientId: 'commentator',
            destinations: ['blackbox'],
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
        });

        const app = express()
            .use(expressTvmMiddleware)
            .use((req: Request, res: Response) => {
                expect(req.tvm && req.tvm.commentator).toEqual({
                    tickets: {
                        blackbox: {
                            ticket: 'blackbox-ticket',
                            tvm_id: 224, // eslint-disable-line camelcase
                        },
                    },
                });

                res.sendStatus(200);
            });

        await request(app)
            .get('/')
            .expect(200);
    });

    test('should save empty tickets map if destinations list is empty', async() => {
        const spy = jest.spyOn(got, 'get');

        const expressTvmMiddleware = ExpressTvmMiddleware({
            clientId: 'commentator',
            destinations: [],
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
        });

        const app = express()
            .use(expressTvmMiddleware)
            .use((req: Request, res: Response) => {
                expect(req.tvm && req.tvm.commentator).toEqual({
                    tickets: {},
                });

                res.sendStatus(200);
            });

        await request(app)
            .get('/')
            .expect(200);

        expect(spy).not.toHaveBeenCalled();
    });

    test(`should accept destinations as map,
which keys are service names and values are true/false`, async() => {
        const expressTvmMiddleware = ExpressTvmMiddleware({
            clientId: 'commentator',
            destinations: {
                blackbox: true,
                geobase: false,
            },
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
        });

        const app = express()
            .use(expressTvmMiddleware)
            .use((req: Request, res: Response) => {
                expect(req.tvm && req.tvm.commentator).toEqual({
                    tickets: {
                        blackbox: {
                            ticket: 'blackbox-ticket',
                            tvm_id: 224, // eslint-disable-line camelcase
                        },
                    },
                });

                res.sendStatus(200);
            });

        await request(app)
            .get('/')
            .expect(200);
    });

    test(`should accept destinations as map,
which keys are service names and values are functions`, async() => {
        const expressTvmMiddleware = ExpressTvmMiddleware({
            clientId: 'commentator',
            destinations: {
                blackbox: () => true,
                geobase: () => false,
            },
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
        });

        const app = express()
            .use(expressTvmMiddleware)
            .use((req: Request, res: Response) => {
                expect(req.tvm && req.tvm.commentator).toEqual({
                    tickets: {
                        blackbox: {
                            ticket: 'blackbox-ticket',
                            tvm_id: 224, // eslint-disable-line camelcase
                        },
                    },
                });

                res.sendStatus(200);
            });

        await request(app)
            .get('/')
            .expect(200);
    });

    test(`should pass Request and Response Express objects,
if destinations options is map and which values are functions`, async() => {
        const expressTvmMiddleware = ExpressTvmMiddleware({
            clientId: 'commentator',
            destinations: {
                blackbox: (req, res) => {
                    expect(req && req.cookies).toEqual('foobar');
                    expect(res && res.locals.foo).toEqual('bar');

                    return true;
                },
            },
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
        });

        const app = express()
            .use((req: Request, res: Response, next: NextFunction) => {
                req.cookies = 'foobar';
                res.locals.foo = 'bar';

                next();
            })
            .use(expressTvmMiddleware)
            .use((_req: Request, res: Response) => {
                res.sendStatus(200);
            });

        await request(app)
            .get('/')
            .expect(200);
    });

    test('should cache results according to the cacheMaxAge option', async() => {
        jest.useFakeTimers();

        const spy = jest.spyOn(got, 'get');

        const expressTvmMiddleware = ExpressTvmMiddleware({
            clientId: 'commentator',
            cacheMaxAge: 1000,
            destinations: ['blackbox'],
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
        });

        const app = express()
            .use(expressTvmMiddleware)
            .use((_req: Request, res: Response) => res.sendStatus(200));

        await request(app)
            .get('/')
            .expect(200);

        await request(app)
            .get('/')
            .expect(200);

        jest.advanceTimersByTime(1001);

        await request(app)
            .get('/')
            .expect(200);

        expect(spy).toHaveBeenCalledTimes(2);
    });

    test('should not cache results if cacheMaxAge is zero', async() => {
        jest.useFakeTimers();

        const spy = jest.spyOn(got, 'get');

        const expressTvmMiddleware = ExpressTvmMiddleware({
            cacheMaxAge: 0,
            clientId: 'commentator',
            destinations: ['blackbox'],
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
        });

        const app = express()
            .use(expressTvmMiddleware)
            .use((_req: Request, res: Response) => res.sendStatus(200));

        await request(app)
            .get('/')
            .expect(200);

        jest.advanceTimersByTime(1);

        await request(app)
            .get('/')
            .expect(200);

        expect(spy).toHaveBeenCalledTimes(2);
    });

    test('should cache results separately for each destinations sets', async() => {
        jest.useFakeTimers();

        const spy = jest.spyOn(got, 'get');

        const expressTvmMiddleware = ExpressTvmMiddleware({
            clientId: 'commentator',
            cacheMaxAge: 1000,
            destinations: {
                blackbox: req => Boolean(req && req.query && req.query.blackbox),
                geobase: req => Boolean(req && req.query && req.query.geobase),
            },
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
        });

        const app = express()
            .use(expressTvmMiddleware)
            .use((_req: Request, res: Response) => res.sendStatus(200));

        await request(app)
            .get('/?blackbox=true')
            .expect(200);

        await request(app)
            .get('/?geobase=true')
            .expect(200);

        await request(app)
            .get('/?blackbox=true')
            .expect(200);

        await request(app)
            .get('/?geobase=true')
            .expect(200);

        expect(spy).toHaveBeenCalledTimes(2);
    });

    test('should consider custom query and headers for a cache key', async() => {
        jest.useFakeTimers();

        const spy = jest.spyOn(got, 'get');

        const expressTvmMiddleware = ExpressTvmMiddleware({
            clientId: 'commentator',
            cacheMaxAge: 1000,
            destinations: ['blackbox'],
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
            requestOptions: req => ({
                headers: {
                    'X-Custom-Header': String(req.query.customHeaderValue),
                },
                query: {
                    customQueryParam: String(req.query.customQueryParam),
                },
                timeout: Number(req.query.timeout),
            }),
        });

        const app = express()
            .use(expressTvmMiddleware)
            .use((_req: Request, res: Response) => res.sendStatus(200));

        await request(app)
            .get('/?customHeaderValue=a&customQueryParam=a')
            .expect(200);

        await request(app)
            .get('/?customHeaderValue=b&customQueryParam=a')
            .expect(200);

        await request(app)
            .get('/?customHeaderValue=a&customQueryParam=b')
            .expect(200);

        expect(spy).toHaveBeenCalledTimes(3);
    });

    test('should use only clientId, destinations, custom query and headers for a cache key', async() => {
        jest.useFakeTimers();

        const spy = jest.spyOn(got, 'get');

        const expressTvmMiddleware = ExpressTvmMiddleware({
            clientId: 'commentator',
            cacheMaxAge: 1000,
            destinations: ['blackbox'],
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
            requestOptions: req => ({
                headers: {
                    'X-Custom-Header': String(req.query.customHeaderValue),
                },
                timeout: Number(req.query.timeout),
            }),
        });

        const app = express()
            .use(expressTvmMiddleware)
            .use((_req: Request, res: Response) => res.sendStatus(200));

        await request(app)
            .get('/?customHeaderValue=a&timeout=1')
            .expect(200);

        await request(app)
            .get('/?customHeaderValue=a&timeout=2')
            .expect(200);

        await request(app)
            .get('/?customHeaderValue=b&timeout=1')
            .expect(200);

        await request(app)
            .get('/?customHeaderValue=b&timeout=2')
            .expect(200);

        expect(spy).toHaveBeenCalledTimes(2);
    });

    test('should remove custom header from a cache key not listed in the cacheKeyHeaders', async() => {
        jest.useFakeTimers();

        const spy = jest.spyOn(got, 'get');

        const expressTvmMiddleware = ExpressTvmMiddleware({
            cacheKeyHeaders: ['X-Cacheable-Custom-Header'],
            clientId: 'commentator',
            cacheMaxAge: 1000,
            destinations: ['blackbox'],
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
            requestOptions: req => ({
                headers: {
                    'X-Custom-Header': String(req.query.customHeaderValue),
                    'X-Cacheable-Custom-Header': String(req.query.cachableCustomHeaderValue),
                },
            }),
        });

        const app = express()
            .use(expressTvmMiddleware)
            .use((_req: Request, res: Response) => res.sendStatus(200));

        await request(app)
            .get('/?customHeaderValue=a&cachableCustomHeaderValue=a')
            .expect(200);

        await request(app)
            .get('/?customHeaderValue=a&cachableCustomHeaderValue=b')
            .expect(200);

        await request(app)
            .get('/?customHeaderValue=b&cachableCustomHeaderValue=a')
            .expect(200);

        await request(app)
            .get('/?customHeaderValue=b&cachableCustomHeaderValue=b')
            .expect(200);

        expect(spy).toHaveBeenCalledTimes(2);
    });

    test('should remove all custom header from a cache key if cacheKeyHeaders is empty list', async() => {
        jest.useFakeTimers();

        const spy = jest.spyOn(got, 'get');

        const expressTvmMiddleware = ExpressTvmMiddleware({
            cacheKeyHeaders: [],
            clientId: 'commentator',
            cacheMaxAge: 1000,
            destinations: ['blackbox'],
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
            requestOptions: req => ({
                headers: {
                    'X-Custom-Header': String(req.query.customHeaderValue),
                    'X-Cacheable-Custom-Header': String(req.query.cachableCustomHeaderValue),
                },
            }),
        });

        const app = express()
            .use(expressTvmMiddleware)
            .use((_req: Request, res: Response) => res.sendStatus(200));

        await request(app)
            .get('/?customHeaderValue=a&cachableCustomHeaderValue=a')
            .expect(200);

        await request(app)
            .get('/?customHeaderValue=a&cachableCustomHeaderValue=b')
            .expect(200);

        await request(app)
            .get('/?customHeaderValue=b&cachableCustomHeaderValue=a')
            .expect(200);

        await request(app)
            .get('/?customHeaderValue=b&cachableCustomHeaderValue=b')
            .expect(200);

        expect(spy).toHaveBeenCalledTimes(1);
    });

    test('should remove custom query from a cache key not listed in the cacheKeyQueryParams', async() => {
        jest.useFakeTimers();

        const spy = jest.spyOn(got, 'get');

        const expressTvmMiddleware = ExpressTvmMiddleware({
            cacheKeyQueryParams: ['cacheableCustomQueryParam'],
            clientId: 'commentator',
            cacheMaxAge: 1000,
            destinations: ['blackbox'],
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
            requestOptions: req => ({
                query: {
                    customQueryParam: String(req.query.customQueryParamValue),
                    cacheableCustomQueryParam: String(req.query.cacheableCustomQueryParamValue),
                },
            }),
        });

        const app = express()
            .use(expressTvmMiddleware)
            .use((_req: Request, res: Response) => res.sendStatus(200));

        await request(app)
            .get('/?customQueryParamValue=a&cacheableCustomQueryParamValue=a')
            .expect(200);

        await request(app)
            .get('/?customQueryParamValue=a&cacheableCustomQueryParamValue=b')
            .expect(200);

        await request(app)
            .get('/?customQueryParamValue=b&cacheableCustomQueryParamValue=a')
            .expect(200);

        await request(app)
            .get('/?customQueryParamValue=b&cacheableCustomQueryParamValue=b')
            .expect(200);

        expect(spy).toHaveBeenCalledTimes(2);
    });

    test('should remove all custom query from a cache key if cacheKeyQueryParams is empty list', async() => {
        jest.useFakeTimers();

        const spy = jest.spyOn(got, 'get');

        const expressTvmMiddleware = ExpressTvmMiddleware({
            cacheKeyQueryParams: [],
            clientId: 'commentator',
            cacheMaxAge: 1000,
            destinations: ['blackbox'],
            serverUrl: 'http://localhost:1',
            token: 'tvmtool-development-access-token',
            requestOptions: req => ({
                query: {
                    customQueryParam: String(req.query.customQueryParamValue),
                    cacheableCustomQueryParam: String(req.query.cacheableCustomQueryParamValue),
                },
            }),
        });

        const app = express()
            .use(expressTvmMiddleware)
            .use((_req: Request, res: Response) => res.sendStatus(200));

        await request(app)
            .get('/?customQueryParamValue=a&cacheableCustomQueryParamValue=a')
            .expect(200);

        await request(app)
            .get('/?customQueryParamValue=a&cacheableCustomQueryParamValue=b')
            .expect(200);

        await request(app)
            .get('/?customQueryParamValue=b&cacheableCustomQueryParamValue=a')
            .expect(200);

        await request(app)
            .get('/?customQueryParamValue=b&cacheableCustomQueryParamValue=b')
            .expect(200);

        expect(spy).toHaveBeenCalledTimes(1);
    });

    afterEach(() => {
        jest.useRealTimers();
        jest.restoreAllMocks();
        nock.cleanAll();
    });
});
