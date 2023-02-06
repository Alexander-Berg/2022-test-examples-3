/* eslint-disable max-classes-per-file */

import {Agent as HttpAgent} from 'http';
import {Agent as HttpsAgent} from 'https';
import path from 'path';
import {PassThrough} from 'stream';

// @ts-ignore: используется гот моканный...
import {findCalls} from 'got';
import protobuf from 'protobufjs';
import HttpAgentKeepAlive from 'agentkeepalive';

import {
    httpTransportDefaultOptions,
    HttpTransport,
    protoRoot,
    HttpTransportPrepareError,
    HttpTransportParseError,
    HttpTransportRequestError,
    HttpTransportResponseError, isHttpAgent, isHttpsAgent,
} from './transport';
import {Context} from '../base/context';
import {HttpTransportParams, SerializableSimple} from './transport-types';

const HttpsAgentKeepAlive = HttpAgentKeepAlive.HttpsAgent;

describe('setup', function () {
    test('setup - defaults', function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://host.ru',
        }) {}

        const transport = SomeTransport.factory(new Context(null));
        expect(transport.options).toEqual({
            host: 'http://host.ru',
            ...httpTransportDefaultOptions,
        });
    });

    test('setup - host param', function () {
        /* eslint-disable @typescript-eslint/no-unused-vars */

        expect(() => {
            class SomeTransport extends HttpTransport.setup({}) {}
        }).toThrow('Host must be specified');

        expect(() => {
            class SomeTransport extends HttpTransport.setup({host: 'pure-name'}) {}
        }).toThrow('Protocol must be specified at host property');

        expect(() => {
            class SomeTransport extends HttpTransport.setup({host: 'bad://protocol'}) {}
        }).toThrow('Unsupported protocol specified at host property');

        expect(() => {
            class SomeTransport extends HttpTransport.setup({host: 'http:///nohost'}) {}
        }).toThrow('Hostname must be specified at host property');

        expect(() => {
            class SomeTransport extends HttpTransport.setup({host: 'http://host/somePath'}) {}
        }).toThrow('Host must not contain any path. Use pathname and query property instead');

        expect(() => {
            class SomeTransport extends HttpTransport.setup({host: 'http://host?some=query'}) {}
        }).toThrow('Host must not contain any path. Use pathname and query property instead');

        expect(() => {
            class SomeTransport extends HttpTransport.setup({host: 'http://host'}) {}
        }).not.toThrow();

        expect(() => {
            class SomeTransport extends HttpTransport.setup({host: 'http://host/'}) {}
        }).not.toThrow();

        expect(() => {
            class SomeTransport extends HttpTransport.setup({host: 'http://host:12345'}) {}
        }).not.toThrow();

        /* eslint-enable @typescript-eslint/no-unused-vars */
    });

    test('setup - protobuf usage', () => {
        const loadSyncSpy = jest.spyOn(protobuf, 'loadSync');

        /* eslint-disable @typescript-eslint/no-unused-vars */
        class SomeTransport1 extends HttpTransport.setup({
            host: 'http://some.host',
            encodeBody: 'protobuf',
            requestProto: {
                path: path.resolve(__dirname, 'proto/test.proto'),
                package: 'bcm.proto.test.RequestMetadata',
            },
            responseProto: {
                path: path.resolve(__dirname, 'proto/test.proto'),
                package: 'bcm.proto.test.RequestMetadata',
            },
        }) {}

        class SomeTransport2 extends HttpTransport.setup({
            host: 'http://some.host',
            encodeBody: 'protobuf',
            requestProto: {
                path: path.resolve(__dirname, 'proto/test.proto'),
                package: 'bcm.proto.test.RequestMetadata',
            },
            responseProto: {
                path: path.resolve(__dirname, 'proto/test.proto'),
                package: 'bcm.proto.test.RequestMetadata',
            },
        }) {}

        /* eslint-enable @typescript-eslint/no-unused-vars */

        expect(loadSyncSpy).toHaveBeenCalledTimes(4); // twice for each transport
        // @ts-ignore
        expect(protoRoot.files.length).toBe(1); // cuz same file
        loadSyncSpy.mockRestore();
    });
});

describe('agent', function () {
    const HTTP_HOST = 'http://some.host';
    const HTTPS_HOST = 'https://some.host';

    const HTTP_ERROR = 'Incompatible Agent: http protocol must use HttpAgent instance';
    const HTTPS_ERROR = 'Incompatible Agent: https protocol must use HttpsAgent instance';

    function testImplicitAgent(http, options) {
        class SomeTransport extends HttpTransport.setup({host: HTTP_HOST}) {}

        const agent1 = SomeTransport.agent(http ? HTTP_HOST : HTTPS_HOST, options);
        const agent2 = SomeTransport.agent(http ? HTTP_HOST : HTTPS_HOST, options);

        expect(agent1).toBe(agent2);
        expect((http ? isHttpAgent : isHttpsAgent)(agent1)).toBe(true);
        expect(agent1.maxSockets).toBe(5);

        return agent1;
    }

    function testExplicitAgent(http, agent) {
        class SomeTransport extends HttpTransport.setup({host: HTTP_HOST}) {}

        const agent1 = SomeTransport.agent(http ? HTTP_HOST : HTTPS_HOST, agent);
        expect(agent1).toBe(agent);

        expect(() => {
            SomeTransport.agent(http ? HTTPS_HOST : HTTP_HOST, agent);
        }).toThrow(http ? HTTPS_ERROR : HTTP_ERROR);
    }

    test('agent - options, native, http', function () {
        testImplicitAgent(true, {name: 'some-name', maxSockets: 5});
    });

    test('agent - options, keepAlive, http', function () {
        const agent = testImplicitAgent(true, {name: 'some-name-ka', keepAliveEnhanced: true, maxSockets: 5});
        expect(agent).toBeInstanceOf(HttpAgentKeepAlive);
    });

    test('agent - options, native, https', function () {
        testImplicitAgent(false, {name: 'some-name', maxSockets: 5});
    });

    test('agent - options, keepAlive, https', function () {
        const agent = testImplicitAgent(false, {name: 'some-name-ka', keepAliveEnhanced: true, maxSockets: 5});
        expect(agent).toBeInstanceOf(HttpsAgentKeepAlive);
    });

    test('agent - explicit, native, http', function () {
        testExplicitAgent(true, new HttpAgent({maxSockets: 5}));
    });

    test('agent - explicit, keepAlive, http', function () {
        testExplicitAgent(true, new HttpAgentKeepAlive({maxSockets: 5}));
    });

    test('agent - explicit, native, https', function () {
        testExplicitAgent(false, new HttpsAgent({maxSockets: 5}));
    });

    test('agent - explicit, keepAlive, https', function () {
        testExplicitAgent(false, new HttpsAgentKeepAlive({maxSockets: 5}));
    });
});

describe('success', function () {
    test('defaults request', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            parseBody: 'raw',
            headers: {id: 'defaults-request'},
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const result = await transport.send({
            pathname: '/some/path',
            query: {param: 'value'},
        });

        expect(result).toEqual({
            statusCode: 200,
            statusMessage: 'OK',
            headers: {},
            body: '',
            duration: 10,
            rawBody: Buffer.alloc(0),
            contentSize: 0,
            parse: 1,
            phases: {
                total: 10,
                dns: 1,
                wait: 1,
                tcp: 1,
                tls: 1,
                request: 2,
                download: 2,
                firstByte: 4,
            },
        });

        const call = findCalls('defaults-request')[0];

        expect(call).toEqual([
            'http://some.host/some/path?param=value',
            {
                method: 'GET',
                headers: {accept: 'text/html', id: 'defaults-request'},
                agent: {
                    http: null,
                },
                responseType: 'buffer',
                retry: 0,
                throwHttpErrors: false,
                allowGetBody: false,
            },
        ]);
    });

    test('defaults json request', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            parseBody: 'json',
            headers: {id: 'defaults-json-request'},
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const result = await transport.send({
            pathname: '/some/path',
            query: {param: 'value'},
        });

        expect(result).toEqual({
            statusCode: 200,
            statusMessage: 'OK',
            headers: {},
            body: null,
            duration: 10,
            rawBody: Buffer.alloc(0),
            contentSize: 0,
            parse: 1,
            phases: {
                total: 10,
                dns: 1,
                wait: 1,
                tcp: 1,
                tls: 1,
                request: 2,
                download: 2,
                firstByte: 4,
            },
        });

        const call = findCalls('defaults-json-request')[0];

        expect(call).toEqual([
            'http://some.host/some/path?param=value',
            {
                method: 'GET',
                headers: {accept: 'application/json', id: 'defaults-json-request'},
                agent: {
                    http: null,
                },
                responseType: 'buffer',
                retry: 0,
                throwHttpErrors: false,
                allowGetBody: false,
            },
        ]);
    });

    test('simple request', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            parseBody: 'json',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = Buffer.from('{"status": true}', 'utf8');
        const headers = {
            id: 'simple request',
            statusCode: 200,
            statusMessage: 'OK',
            body: body as any,
        };

        const result = await transport.send({
            pathname: '/some/path',
            query: {param: 'value'},
            headers,
        });

        expect(result).toEqual({
            statusCode: 200,
            statusMessage: 'OK',
            headers: {},
            body: {status: true},
            duration: 10,
            rawBody: body,
            contentSize: 16,
            parse: 1,
            phases: {
                total: 10,
                dns: 1,
                wait: 1,
                tcp: 1,
                tls: 1,
                request: 2,
                download: 2,
                firstByte: 4,
            },
        });

        const call = findCalls('simple request')[0];

        expect(call).toEqual([
            'http://some.host/some/path?param=value',
            {
                method: 'GET',
                headers: {...headers, accept: 'application/json'},
                agent: {
                    http: null,
                },
                responseType: 'buffer',
                retry: 0,
                throwHttpErrors: false,
                allowGetBody: false,
            },
        ]);
    });

    test('complex request', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            pathname: '/basepath',
            query: {base: 'query', custom: 'options'},
            headers: {id: 'complex-request', custom: 'options'},
            parseBody: 'json',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = Buffer.from('{"status": true}', 'utf8');
        const headers = {
            statusCode: 200,
            statusMessage: 'OK',
            body: body as any,
            custom: 'params',
        };

        const result = await transport.send({
            pathname: '/some/path',
            query: {param: 'value', custom: 'params', array: ['param1', 'param2'], someBool: true},
            headers,
        });

        expect(result).toEqual({
            statusCode: 200,
            statusMessage: 'OK',
            headers: {},
            body: {status: true},
            duration: 10,
            rawBody: body,
            contentSize: 16,
            parse: 1,
            phases: {
                total: 10,
                dns: 1,
                wait: 1,
                tcp: 1,
                tls: 1,
                request: 2,
                download: 2,
                firstByte: 4,
            },
        });

        const call = findCalls('complex-request')[0];

        expect(call).toEqual([
            'http://some.host/basepath/some/path?base=query&custom=params&param=value&array=param1%2Cparam2&someBool=true',
            {
                method: 'GET',
                headers: {
                    ...headers,
                    id: 'complex-request',
                    accept: 'application/json',
                },
                agent: {
                    http: null,
                },
                responseType: 'buffer',
                retry: 0,
                throwHttpErrors: false,
                allowGetBody: false,
            },
        ]);
    });

    test('stream request', async function () {
        Date.now = () => 1567605157447;

        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            parseBody: 'stream',
            headers: {id: 'stream-request'},
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const result = await transport.send({
            pathname: '/stream/path',
            query: {param: 'value'},
        });

        expect(result).toEqual({
            statusCode: 200,
            statusMessage: 'OK',
            headers: {},
            body: expect.any(PassThrough),
            duration: 0,
            rawBody: expect.any(PassThrough),
            contentSize: 0,
            parse: 1,
            phases: {
                total: 0,
                dns: 1,
                wait: 1,
                tcp: 1,
                tls: 1,
                request: 2,
                download: 2,
                firstByte: 4,
            },
        });

        const call = findCalls('stream-request')[0];

        expect(call).toEqual([
            'http://some.host/stream/path?param=value',
            {
                method: 'GET',
                headers: {accept: 'application/octet-stream', id: 'stream-request'},
                agent: {
                    http: null,
                },
                responseType: 'buffer',
                retry: 0,
                throwHttpErrors: false,
                allowGetBody: false,
            },
        ]);
    });
});

describe('queryArrayFormat', function () {
    let transport: HttpTransport;

    function createTransport(queryArrayFormat?: string) {
        const extender: { [id: string]: string } = queryArrayFormat ? {queryArrayFormat} : {};

        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            ...extender,
        }) {}

        transport = SomeTransport.factory(new Context(null));
    }

    function getSendData(callId: string): HttpTransportParams {
        return {
            method: 'GET',
            headers: {
                id: callId,
                statusCode: 200,
                statusMessage: 'OK',
                body: Buffer.from('{"status": true}', 'utf8') as any,
            },
            query: {a: 1, d: 'str', b: [1, 2, 3]},
        };
    }

    test('default', async function () {
        createTransport();
        await transport.send(getSendData('queryArrayFormat-default'));

        expect(findCalls('queryArrayFormat-default')[0][0])
            .toEqual('http://some.host/?a=1&d=str&b=1%2C2%2C3');
    });

    test('bracket', async function () {
        createTransport('bracket');
        await transport.send(getSendData('queryArrayFormat-bracket'));

        expect(findCalls('queryArrayFormat-bracket')[0][0])
            .toEqual('http://some.host/?a=1&d=str&b[]=1&b[]=2&b[]=3');
    });

    test('index', async function () {
        createTransport('index');
        await transport.send(getSendData('queryArrayFormat-index'));

        expect(findCalls('queryArrayFormat-index')[0][0])
            .toEqual('http://some.host/?a=1&d=str&b[0]=1&b[1]=2&b[2]=3');
    });

    test('comma', async function () {
        createTransport('comma');
        await transport.send(getSendData('queryArrayFormat-comma'));

        expect(findCalls('queryArrayFormat-comma')[0][0])
            .toEqual('http://some.host/?a=1&d=str&b=1,2,3');
    });

    test('none', async function () {
        createTransport('none');
        await transport.send(getSendData('queryArrayFormat-none'));

        expect(findCalls('queryArrayFormat-none')[0][0])
            .toEqual('http://some.host/?a=1&d=str&b=1&b=2&b=3');
    });
});

describe('queryKeepNull', function () {
    let transport: HttpTransport;

    function createTransport(queryKeepNull?: boolean) {
        const extender: { [id: string]: boolean } = queryKeepNull ? {queryKeepNull} : {};

        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            ...extender,
        }) {}

        transport = SomeTransport.factory(new Context(null));
    }

    function getSendData(callId: string): HttpTransportParams {
        return {
            method: 'GET',
            headers: {
                id: callId,
                statusCode: 200,
                statusMessage: 'OK',
                body: Buffer.from('{"status": true}', 'utf8') as any,
            },
            query: {a: 1, d: 'str', b: [1, 2, 3], e: null, f: undefined},
        };
    }

    test('default', async function () {
        createTransport();
        await transport.send(getSendData('queryKeepNull-default'));

        expect(findCalls('queryKeepNull-default')[0][0])
            .toEqual('http://some.host/?a=1&d=str&b=1%2C2%2C3');
    });

    test('false', async function () {
        createTransport(false);
        await transport.send(getSendData('queryKeepNull-false'));

        expect(findCalls('queryKeepNull-false')[0][0])
            .toEqual('http://some.host/?a=1&d=str&b=1%2C2%2C3');
    });

    test('true', async function () {
        createTransport(true);
        await transport.send(getSendData('queryKeepNull-true'));

        expect(findCalls('queryKeepNull-true')[0][0])
            .toEqual('http://some.host/?a=1&d=str&b=1%2C2%2C3&e');
    });
});

describe('forceQueryString', function () {
    let transport: HttpTransport;

    function createTransport(forceQueryString?: string) {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            ...(forceQueryString ? {forceQueryString} : {}),
        }) {}

        transport = SomeTransport.factory(new Context(null));
    }

    function getSendData(callId: string, query: SerializableSimple, forceQueryString?: string): HttpTransportParams {
        return {
            method: 'GET',
            headers: {
                id: callId,
                statusCode: 200,
                statusMessage: 'OK',
                body: Buffer.from('{"status": true}', 'utf8') as any,
            },
            query,
            forceQueryString,
        };
    }

    test('default', async function () {
        createTransport();
        await transport.send(getSendData('forceQueryString-default', {a: 1, d: 'str', b: [1, 2, 3]}));

        expect(findCalls('forceQueryString-default')[0][0])
            .toEqual('http://some.host/?a=1&d=str&b=1%2C2%2C3');
    });

    test('options', async function () {
        createTransport('?a=1&e=foo');
        await transport.send(getSendData('forceQueryString-options', {a: 1, d: 'str', b: [1, 2, 3]}));

        expect(findCalls('forceQueryString-options')[0][0])
            .toEqual('http://some.host/?a=1&e=foo');
    });

    test('param', async function () {
        createTransport('?a=42');
        await transport.send(
            getSendData('forceQueryString-param', {a: 1, b: 'str'}, '?a=1&b=foo'),
        );

        expect(findCalls('forceQueryString-param')[0][0])
            .toEqual('http://some.host/?a=1&b=foo');
    });
});

describe('encode body', function () {
    test('encode body - form', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            encodeBody: 'form',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        await transport.send({
            method: 'POST',
            body: {first: '111', second: 222, third: [1, 2, 3]},
            headers: {
                id: 'encode-body-form',
                statusCode: 200,
                statusMessage: 'OK',
                body: Buffer.from('{"status": true}', 'utf8') as any,
            },
        });

        const call = findCalls('encode-body-form')[0];

        expect(call[1].body).toEqual('first=111&second=222&third=1%2C2%2C3');
    });

    test('encode body - json', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            encodeBody: 'json',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        await transport.send({
            method: 'POST',
            body: {first: '111', second: 222, third: [1, 2, 3]},
            headers: {
                id: 'encode-body-json',
                statusCode: 200,
                statusMessage: 'OK',
                body: Buffer.from('{"status": true}', 'utf8') as any,
            },
        });

        const call = findCalls('encode-body-json')[0];

        expect(call[1].body).toEqual(JSON.stringify({first: '111', second: 222, third: [1, 2, 3]}));
        expect(call[1].headers['content-type']).toEqual('application/json');
    });

    test('encode body - raw', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            encodeBody: 'raw',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        await expect(transport.send({
            method: 'POST',
            body: {first: '111', second: 222, third: [1, 2, 3]},
            headers: {
                id: 'encode-body-raw-fail',
                statusCode: 200,
                statusMessage: 'OK',
                body: Buffer.from('{"status": true}', 'utf8') as any,
            },
        })).rejects.toBeInstanceOf(HttpTransportPrepareError);

        await transport.send({
            method: 'POST',
            body: 'first=111&second=222&third=1%2C2%2C3',
            headers: {
                id: 'encode-body-raw',
                statusCode: 200,
                statusMessage: 'OK',
                body: Buffer.from('{"status": true}', 'utf8') as any,
            },
        });

        const call = findCalls('encode-body-raw')[0];

        expect(call[1].body).toEqual('first=111&second=222&third=1%2C2%2C3');
    });

    test('encode body - buffer', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            encodeBody: 'raw',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = Buffer.from('raw string', 'utf8');

        await transport.send({
            method: 'POST',
            body,
            headers: {
                id: 'encode-body-buffer',
                statusCode: 200,
                statusMessage: 'OK',
                body: Buffer.from('{"status": true}', 'utf8') as any,
            },
        });

        const call = findCalls('encode-body-buffer')[0];

        expect(call[1].body).toEqual(body);
    });

    describe('encode body - protobuf', () => {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            encodeBody: 'protobuf',
            requestProto: {
                path: path.resolve(__dirname, 'proto/test.proto'),
                package: 'bcm.proto.test.RequestMetadata',
            },
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        test('with setup configuration', async () => {
            const body = {lol: 'kek'};

            await transport.send({
                method: 'GET',
                body,
                headers: {
                    id: 'encode-body-protobuf',
                    statusCode: 200,
                    statusMessage: 'OK',
                    body: Buffer.from('{"status": true}', 'utf8') as any,
                },
            });
            const call = findCalls('encode-body-protobuf')[0];

            expect(call[1].body).toEqual(Buffer.from('CgNrZWs=', 'base64'));
        });

        test('with direct requestProto parameter', async () => {
            const requestProtoMock = new protobuf.Type('proto');

            // @ts-ignore
            requestProtoMock.encode = function encode() {
                return {
                    finish() {
                        return Buffer.from('encoded');
                    },
                };
            };
            // @ts-ignore
            requestProtoMock.create = function create(x) {
                return x;
            };
            // @ts-ignore
            requestProtoMock.verify = function verify() {
                return false;
            };

            await transport.send({
                body: {lol: 'kek'},
                headers: {
                    id: 'encode-body-protobuf1',
                    statusCode: 200,
                    statusMessage: 'OK',
                    body: Buffer.from('{"status": true}', 'utf8') as any,
                },
                requestProto: requestProtoMock,
            });

            const call = findCalls('encode-body-protobuf1')[0];

            expect(call[1].body).toEqual(Buffer.from('encoded'));
        });

        test('with params package override', async () => {
            const body = {kek: 'qwerty'};

            await transport.send({
                method: 'GET',
                body,
                requestProto: {
                    package: 'bcm.proto.test.OtherRequestMetadata',
                },
                headers: {
                    id: 'encode-body-protobuf2',
                    statusCode: 200,
                    statusMessage: 'OK',
                    body: Buffer.from('{"status": true}', 'utf8') as any,
                },
            });
            const call = findCalls('encode-body-protobuf2')[0];

            expect(call[1].body).toEqual(Buffer.from('CgZxd2VydHk=', 'base64'));
        });

        test('with full params override', async () => {
            const body = {wow: 'doge'};

            await transport.send({
                method: 'GET',
                body,
                requestProto: {
                    path: path.resolve(__dirname, 'proto/test2.proto'),
                    package: 'bcm.proto.test2.RequestMetadata',
                },
                headers: {
                    id: 'encode-body-protobuf3',
                    statusCode: 200,
                    statusMessage: 'OK',
                    body: Buffer.from('{"status": true}', 'utf8') as any,
                },
            });
            const call = findCalls('encode-body-protobuf3')[0];

            expect(call[1].body).toEqual(Buffer.from('CgRkb2dl', 'base64'));
        });
    });

    test('encode body - raw - from params', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            encodeBody: 'json',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = 'raw string';

        await transport.send({
            method: 'POST',
            body,
            headers: {
                id: 'encode-body-from-params',
                statusCode: 200,
                statusMessage: 'OK',
                body: Buffer.from('{"status": true}', 'utf8') as any,
            },
            encodeBody: 'raw',
        });

        const call = findCalls('encode-body-from-params')[0];

        expect(call[1].body).toEqual(body);
    });
});

describe('parse body', function () {
    test('parse body - raw, encoded', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            parseBody: 'raw',
            encoding: 'utf8',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const result = await transport.send({
            headers: {
                id: 'parse-body-raw-encoded',
                statusCode: 200,
                statusMessage: 'OK',
                body: Buffer.from('some raw text', 'utf8') as any,
            },
        });

        expect(result.body).toBe('some raw text');

        const call = findCalls('parse-body-raw-encoded')[0];

        expect(call[1].headers.accept).toBe('text/html');
    });

    test('parse body - raw, unencoded', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            parseBody: 'raw',
            encoding: null,
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = Buffer.from('some raw text', 'utf8');
        const result = await transport.send({
            headers: {
                id: 'parse-body-raw-unencoded',
                statusCode: 200,
                statusMessage: 'OK',
                body: body as any,
            },
        });

        expect(result.body).toBe(body);

        const call = findCalls('parse-body-raw-unencoded')[0];

        expect(call[1].headers.accept).toBe('text/html');
    });

    test('parse body - json', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            parseBody: 'json',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = {success: true};
        const result = await transport.send({
            headers: {
                id: 'parse-body-json',
                statusCode: 200,
                statusMessage: 'OK',
                body: Buffer.from(JSON.stringify(body), 'utf8') as any,
            },
        });

        expect(result.body).toEqual(body);

        const call = findCalls('parse-body-json')[0];

        expect(call[1].headers.accept).toBe('application/json');
    });

    test('parse body - buffer', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            parseBody: 'buffer',
            encoding: 'utf8',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = Buffer.from('some raw text', 'utf8');
        const result = await transport.send({
            headers: {
                id: 'parse-body-buffer',
                statusCode: 200,
                statusMessage: 'OK',
                body: body as any,
            },
        });

        expect(result.body).toBe(body);

        const call = findCalls('parse-body-buffer')[0];

        expect(call[1].headers.accept).toBe('application/octet-stream');
    });

    describe('parse body - protobuf', () => {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            parseBody: 'protobuf',
            responseProto: {
                path: path.resolve(__dirname, 'proto/test.proto'),
                package: 'bcm.proto.test.ResponseMetadata',
            },
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        test('with setup configuration', async () => {
            const result = await transport.send({
                headers: {
                    id: 'parse-body-protobuf',
                    statusCode: 200,
                    statusMessage: 'OK',
                    body: Buffer.from('CAE=', 'base64') as any,
                },
            });

            expect(result.body).toEqual({success: true});
            expect(findCalls('parse-body-protobuf')[0][1].headers.accept).toBe('application/octet-stream');
        });

        test('with direct responseProto parameter', async () => {
            const responseProtoMock = new protobuf.Type('proto');
            const toObjectMock = jest.fn(function toObject(x) {
                return x;
            });
            const fromObjectMock = jest.fn(function fromObject(x) {
                return x;
            });

            // @ts-ignore
            responseProtoMock.decode = function decode() {
                return 'decoded';
            };
            // @ts-ignore
            responseProtoMock.toObject = toObjectMock;
            // @ts-ignore
            responseProtoMock.fromObject = fromObjectMock;
            // @ts-ignore
            responseProtoMock.verify = function verify() {
                return false;
            };
            const responseProtoOptions = {};
            const result = await transport.send({
                headers: {
                    id: 'parse-body-protobuf1',
                    statusCode: 200,
                    statusMessage: 'OK',
                    body: Buffer.from('CAE=', 'base64') as any,
                },
                responseProto: responseProtoMock,
                responseProtoOptions,
            });

            // @ts-ignore
            expect(toObjectMock.mock.calls[0][1]).toBe(responseProtoOptions);
            expect(result.body).toEqual('decoded');
        });

        test('with params package override', async () => {
            const result = await transport.send({
                headers: {
                    id: 'parse-body-protobuf2',
                    statusCode: 200,
                    statusMessage: 'OK',
                    body: Buffer.from('CAE=', 'base64') as any,
                },
                responseProto: {
                    package: 'bcm.proto.test.AnotherResponseMetadata',
                },
            });

            expect(result.body).toEqual({error: true});
        });

        test('with full params override', async () => {
            const result = await transport.send({
                headers: {
                    id: 'parse-body-protobuf3',
                    statusCode: 200,
                    statusMessage: 'OK',
                    body: Buffer.from('CAE=', 'base64') as any,
                },
                responseProto: {
                    path: path.resolve(__dirname, 'proto/test2.proto'),
                    package: 'bcm.proto.test.ResponseMetadata',
                },
            });

            expect(result.body).toEqual({success: true});
        });
    });

    test('parse body - json - from params', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            parseBody: 'raw',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = {success: true};
        const result = await transport.send({
            headers: {
                id: 'parse-body-json',
                statusCode: 200,
                statusMessage: 'OK',
                body: Buffer.from(JSON.stringify(body), 'utf8') as any,
            },
            parseBody: 'json',
        });

        expect(result.body).toEqual(body);

        const call = findCalls('parse-body-json')[0];

        expect(call[1].headers.accept).toBe('application/json');
    });

    test('parse body – stream', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            parseBody: 'stream',
            headers: {id: 'stream-request'},
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const result = await transport.send({
            pathname: '/stream/path',
            query: {param: 'value'},
        });

        expect(result.body).toEqual(expect.any(PassThrough));

        const call = findCalls('stream-request')[0];

        expect(call[1].headers.accept).toBe('application/octet-stream');
    });
});

test('proxy headers', async function () {
    class SomeTransport extends HttpTransport.setup({
        host: 'http://some.host',
        proxyHeaders: ['first', 'missing'],
    }) {}

    const transport = SomeTransport.factory(new Context({
        headers: {
            first: '111',
            second: '222',
            third: '333',
        },
    } as any));

    const headers = {
        id: 'proxy-headers',
        statusCode: 200,
        statusMessage: 'OK',
        body: Buffer.from('{"status": true}', 'utf8') as any,
    };

    await transport.send({
        method: 'POST',
        body: {first: '111', second: 222, third: [1, 2, 3]},
        proxyHeaders: ['second', 'absent'],
        headers,
    });

    const call = findCalls('proxy-headers')[0];

    expect(call[1].headers).toEqual({
        ...headers,
        accept: 'application/json',
        first: '111',
        second: '222',
    });
});

describe('retry', function () {
    test('non-retriable', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://host',
            retry: 2,
            encodeBody: 'raw',
            parseBody: 'json',
            retryAllowed: () => true,
            headers: {id: 'non-retriable'},
            agent: new HttpAgent({maxSockets: 5}),
        }) {
            protected afterError(params, request, stats, error, willRetry: boolean) {
                expect(willRetry).toBe(false);
                return super.afterError(params, request, stats, error, willRetry);
            }
        }

        const transport = SomeTransport.factory(new Context(null));

        await expect(transport.send({body: {}, requestId: '1234'})).rejects.toBeInstanceOf(HttpTransportPrepareError);
        await expect(transport.send({
            headers: {body: Buffer.from('non-valid json', 'utf8') as any},
        })).rejects.toBeInstanceOf(HttpTransportParseError);
    });

    test('retriable', async function () {
        const retry = jest.fn();

        class SomeTransport extends HttpTransport.setup({
            host: 'http://host',
            retry: 2,
            encodeBody: 'raw',
            parseBody: 'json',
            retryAllowed: () => true,
            headers: {id: 'retriable'},
        }) {
            protected afterError(params, request, stats, error, willRetry: boolean) {
                if (willRetry) { retry(); }
                return super.afterError(params, request, stats, error, willRetry);
            }
        }

        const transport = SomeTransport.factory(new Context(null));

        await expect(transport.send({
            headers: {
                fail: 'request error',
                statusMessage: 'EAGAIN',
            },
        })).rejects.toBeInstanceOf(HttpTransportRequestError);

        await expect(transport.send({
            headers: {statusCode: 500},
        })).rejects.toBeInstanceOf(HttpTransportResponseError);

        expect(retry).toHaveBeenCalledTimes(4);
    });

    test('default', async function () {
        const retry = jest.fn();

        class SomeTransport extends HttpTransport.setup({
            host: 'http://host',
            retry: 2,
            encodeBody: 'raw',
            parseBody: 'json',
        }) {
            protected afterError(params, request, stats, error, willRetry: boolean) {
                if (willRetry) { retry(); }
                return super.afterError(params, request, stats, error, willRetry);
            }
        }

        const transport = SomeTransport.factory(new Context(null));

        await expect(transport.send({
            headers: {
                fail: 'request error',
                statusMessage: 'EUNKNOWN',
            },
        })).rejects.toBeInstanceOf(HttpTransportRequestError);

        expect(retry).toHaveBeenCalledTimes(0);

        await expect(transport.send({
            headers: {statusCode: 400},
        })).rejects.toBeInstanceOf(HttpTransportResponseError);

        expect(retry).toHaveBeenCalledTimes(0);

        await expect(transport.send({
            headers: {statusCode: 500},
        })).rejects.toBeInstanceOf(HttpTransportResponseError);

        expect(retry).toHaveBeenCalledTimes(2);

        await expect(transport.send({
            headers: {
                fail: 'request error',
                statusMessage: 'ENETUNREACH',
            },
        })).rejects.toBeInstanceOf(HttpTransportRequestError);

        expect(retry).toHaveBeenCalledTimes(4);
    });
});

describe('status code', function () {
    test('defaults', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://host',
            parseBody: 'raw',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        await expect(transport.send({headers: {statusCode: 500}}))
            .rejects.toBeInstanceOf(HttpTransportResponseError);

        await expect(transport.send({headers: {statusCode: 400}}))
            .rejects.toBeInstanceOf(HttpTransportResponseError);

        await expect(transport.send({headers: {statusCode: 300}}))
            .resolves.toMatchObject({body: ''});

        await expect(transport.send({headers: {statusCode: 200}}))
            .resolves.toMatchObject({body: ''});
    });

    test('custom', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://host',
            parseBody: 'raw',
            acceptStatusCode: statusCode => [200, 521].includes(statusCode),
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        await expect(transport.send({headers: {statusCode: 500}}))
            .rejects.toBeInstanceOf(HttpTransportResponseError);

        await expect(transport.send({headers: {statusCode: 400}}))
            .rejects.toBeInstanceOf(HttpTransportResponseError);

        await expect(transport.send({headers: {statusCode: 300}}))
            .rejects.toBeInstanceOf(HttpTransportResponseError);

        await expect(transport.send({headers: {statusCode: 200}}))
            .resolves.toMatchObject({body: ''});

        await expect(transport.send({headers: {statusCode: 521}}))
            .resolves.toMatchObject({body: ''});
    });
});

describe('followRedirect', function () {
    test('defaults', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            followRedirect: false,
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        await transport.send({
            headers: {
                id: 'follow-redirect-defaults',
                statusCode: 302,
                statusMessage: 'FOUND',
                location: 'http://other.host',
            },
        });

        const call = findCalls('follow-redirect-defaults')[0];

        expect(call[1].followRedirect).toBe(false);
    });

    test('override', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            followRedirect: true,
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        await transport.send({
            headers: {
                id: 'follow-redirect-override',
                statusCode: 302,
                statusMessage: 'FOUND',
                location: 'http://other.host',
            },
            followRedirect: false,
        });

        const call = findCalls('follow-redirect-override')[0];

        expect(call[1].followRedirect).toBe(false);
    });
});

describe('timeouts', function () {
    test('defaults', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            timeout: 12345,
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = Buffer.from('{}', 'utf8');

        await transport.send({
            headers: {
                id: 'timeouts-defaults',
                statusCode: 200,
                statusMessage: 'OK',
                body: body as any,
            },
        });

        const call = findCalls('timeouts-defaults')[0];

        expect(call[1].timeout).toBe(12345);
    });

    test('override', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            timeout: 12345,
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = Buffer.from('{}', 'utf8');

        await transport.send({
            headers: {
                id: 'timeouts-override',
                statusCode: 200,
                statusMessage: 'OK',
                body: body as any,
            },
            timeout: 54321,
        });

        const call = findCalls('timeouts-override')[0];

        expect(call[1].timeout).toBe(54321);
    });
});

describe('dnsLookupIpVersion', function () {
    test('defaults', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = Buffer.from('{}', 'utf8');

        await transport.send({
            headers: {
                id: 'dns-lookup-ip-version-defaults',
                statusCode: 200,
                statusMessage: 'OK',
                body: body as any,
            },
        });

        const call = findCalls('dns-lookup-ip-version-defaults')[0];

        expect(call[1].dnsLookupIpVersion).toBe(undefined);
    });

    test('custom', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            dnsLookupIpVersion: 'ipv6',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = Buffer.from('{}', 'utf8');

        await transport.send({
            headers: {
                id: 'dns-lookup-ip-version-custom',
                statusCode: 200,
                statusMessage: 'OK',
                body: body as any,
            },
        });

        const call = findCalls('dns-lookup-ip-version-custom')[0];

        expect(call[1].dnsLookupIpVersion).toBe('ipv6');
    });

    test('override', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            dnsLookupIpVersion: 'ipv4',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = Buffer.from('{}', 'utf8');

        await transport.send({
            headers: {
                id: 'dns-lookup-ip-version-override',
                statusCode: 200,
                statusMessage: 'OK',
                body: body as any,
            },
            dnsLookupIpVersion: 'ipv6',
        });

        const call = findCalls('dns-lookup-ip-version-override')[0];

        expect(call[1].dnsLookupIpVersion).toBe('ipv6');
    });
});

describe('allowGetBody', function () {
    test('defaults', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = Buffer.from('{}', 'utf8');

        await transport.send({
            headers: {
                id: 'allow-get-body-defaults',
                statusCode: 200,
                statusMessage: 'OK',
                body: body as any,
            },
        });

        const call = findCalls('allow-get-body-defaults')[0];

        expect(call[1].allowGetBody).toBe(false);
    });

    test('custom', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            allowGetBody: false,
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = Buffer.from('{}', 'utf8');

        await transport.send({
            headers: {
                id: 'allow-get-body-custom',
                statusCode: 200,
                statusMessage: 'OK',
                body: body as any,
            },
        });

        const call = findCalls('allow-get-body-custom')[0];

        expect(call[1].allowGetBody).toBe(false);
    });

    test('override', async function () {
        class SomeTransport extends HttpTransport.setup({
            host: 'http://some.host',
            allowGetBody: false,
        }) {}

        const transport = SomeTransport.factory(new Context(null));

        const body = Buffer.from('{}', 'utf8');

        await transport.send({
            headers: {
                id: 'allow-get-body-override',
                statusCode: 200,
                statusMessage: 'OK',
                body: body as any,
            },
            allowGetBody: true,
        });

        const call = findCalls('allow-get-body-override')[0];

        expect(call[1].allowGetBody).toBe(true);
    });
});
