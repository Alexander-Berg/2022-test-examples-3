/* eslint-env mocha */
/* eslint-disable camelcase */
'use strict';

const os = require('os');
const zlib = require('zlib');
const assert = require('assert');
const sinon = require('sinon');

const yandexLogger = require('../../..');
const sentryStream = require('../../../streams/sentry');

describe('YandexLogger. Streams. Sentry', () => {
    let dsn;
    let transport;
    let clientOptions;
    let clock;
    let setTimeout;

    beforeEach(() => {
        dsn = 'https://key:secret@sentry.host:9000/2';
        transport = { send: sinon.stub() };
        clientOptions = { transport };

        setTimeout = global.setTimeout;
        clock = sinon.useFakeTimers(1000);
    });

    afterEach(() => {
        clock.restore();
    });

    it('должен парсить dsn и принимать транспорт', () => {
        let logger = createLogger({ dsn, clientOptions });

        logger.info('message');

        return wait().then(() => {
            sinon.assert.calledOnce(transport.send);

            let client = transport.send.args[0][0];
            assert.deepStrictEqual(client.dsn, {
                protocol: 'https',
                public_key: 'key',
                host: 'sentry.host',
                private_key: 'secret',
                path: '/',
                project_id: '2',
                port: 9000,
            });
        });
    });

    it('должен принимать опции Raven', () => {
        clientOptions = {
            transport,
            release: '1.2.3',
            environment: 'production',
            name: 'server-name',
            logger: 'ignored',
            tags: {
                tag1: 'value1',
                tag2: 'value2',
            },
            extra: {
                prop1: 'value1',
                prop2: 'value2',
            },
        };
        let logger = createLogger({ dsn, clientOptions });

        logger.info({
            tags: {
                tag2: 'value3',
            },
            data: {
                prop2: 'value3',
            },
        }, 'message');

        return wait().then(() => {
            let message = transport.send.args[0][1];
            assertMessage(message, {
                level: 'info',
                message: 'message',
                checksum: 'message',
                release: '1.2.3',
                environment: 'production',
                server_name: 'server-name',
                tags: {
                    tag1: 'value1',
                    tag2: 'value3',
                },
                extra: {
                    node: process.version,
                    prop1: 'value1',
                    prop2: 'value3',
                },
            });
        });
    });

    it('должен логировать с уровнем fatal', () => {
        let logger = createLogger({ dsn, clientOptions });

        logger.fatal('message');

        return wait().then(() => {
            let message = transport.send.args[0][1];
            assertMessage(message, {
                level: 'fatal',
                message: 'message',
                checksum: 'message',
            });
        });
    });

    it('должен логировать с уровнем error', () => {
        let logger = createLogger({ dsn, clientOptions });

        logger.error('message');

        return wait().then(() => {
            let message = transport.send.args[0][1];
            assertMessage(message, {
                level: 'error',
                message: 'message',
                checksum: 'message',
            });
        });
    });

    it('должен логировать с уровнем warning', () => {
        let logger = createLogger({ dsn, clientOptions });

        logger.warn('message');

        return wait().then(() => {
            let message = transport.send.args[0][1];
            assertMessage(message, {
                level: 'warning',
                message: 'message',
                checksum: 'message',
            });
        });
    });

    it('должен логировать с уровнем info', () => {
        let logger = createLogger({ dsn, clientOptions });

        logger.info('message');

        return wait().then(() => {
            let message = transport.send.args[0][1];
            assertMessage(message, {
                level: 'info',
                message: 'message',
                checksum: 'message',
            });
        });
    });

    it('должен логировать с уровнем debug', () => {
        let logger = createLogger({ dsn, clientOptions });

        logger.debug('message');

        return wait().then(() => {
            let message = transport.send.args[0][1];
            assertMessage(message, {
                level: 'debug',
                message: 'message',
                checksum: 'message',
            });
        });
    });

    it('не должен логировать с уровнем trace', () => {
        let logger = createLogger({ dsn, clientOptions });

        logger.trace('message');

        return wait().then(() => {
            sinon.assert.notCalled(transport.send);
        });
    });

    it('должен логировать простые строки', () => {
        let logger = createLogger({ dsn, clientOptions });

        logger.info('message');

        return wait().then(() => {
            let message = transport.send.args[0][1];
            assertMessage(message, {
                level: 'info',
                message: 'message',
                checksum: 'message',
            });
        });
    });

    it('должен логировать строки с параметрами', () => {
        let logger = createLogger({ dsn, clientOptions });

        logger.info('message: %s %d', 'param', 123);

        return wait().then(() => {
            let message = transport.send.args[0][1];
            assertMessage(message, {
                level: 'info',
                message: 'message: param 123',
                checksum: 'message: %s %d',
            });
        });
    });

    it('должен логировать исключения', () => {
        let logger = createLogger({ dsn, clientOptions });

        let error = new TypeError('Something bad happened!');
        error.stack = 'TypeError: Something bad happened!\n' +
            '    at Context.it (/sentry/sentry.js:162:21)\n' +
            '    at callFn (/lib/runnable.js:360:21)';

        logger.info(error);

        return wait().then(() => {
            let message = transport.send.args[0][1];
            assertMessage(message, {
                level: 'info',
                message: 'TypeError: Something bad happened!',
                culprit: 'Something bad happened!',
                exception: [
                    {
                        type: 'TypeError',
                        value: 'Something bad happened!',
                        stacktrace: {
                            frames: [
                                {
                                    filename: '/lib/runnable.js',
                                    lineno: 360,
                                    colno: 21,
                                    module: 'runnable',
                                    function: 'callFn',
                                    in_app: true,
                                },
                                {
                                    filename: '/sentry/sentry.js',
                                    lineno: 162,
                                    colno: 21,
                                    module: 'sentry',
                                    function: 'Context.it',
                                    in_app: true,
                                },
                            ],
                        },
                    },
                ],
            });
        });
    });

    it('должен логировать сложные объекты', () => {
        let logger = createLogger({ dsn, clientOptions });

        logger.info({
            release: '1.2.3',
            environment: 'production',
            tags: {
                tag1: 'foo',
                tag2: 'bar',
            },
            data: {
                custom: {
                    inner: 'data',
                },
            },
            sentry: {
                bypass: 'prop',
            },
            unknownProp: 'value',
        }, 'message');

        return wait().then(() => {
            let message = transport.send.args[0][1];
            assertMessage(message, {
                level: 'info',
                message: 'message',
                checksum: 'message',
                environment: 'production',
                tags: {
                    tag1: 'foo',
                    tag2: 'bar',
                },
                extra: {
                    node: process.version,
                    custom: {
                        inner: 'data',
                    },
                },
                bypass: 'prop',
            });
        });
    });

    it('должен логировать запрос из record.req', () => {
        let logger = createLogger({ dsn, clientOptions });

        let req = {
            method: 'POST',
            protocol: 'https',
            hostname: 'afisha.yandex.ru',
            url: '/event/id?source=yamain&param=value',
            headers: {
                'user-agent': 'Awesome browser',
                cookie: 'yandexuid=123; foo=bar',
            },
            body: '[data]',
            remoteAddress: '192.168.1.1',
            remotePort: 12341,
        };

        logger.info({ req }, 'message');

        return wait().then(() => {
            let message = transport.send.args[0][1];
            assertMessage(message, {
                level: 'info',
                message: 'message',
                checksum: 'message',
                request: {
                    method: 'POST',
                    url: 'https://afisha.yandex.ru/event/id?source=yamain&param=value',
                    query_string: {
                        source: 'yamain',
                        param: 'value',
                    },
                    headers: {
                        cookie: 'yandexuid=123; foo=bar',
                        'user-agent': 'Awesome browser',
                    },
                    cookies: {
                        foo: 'bar',
                        yandexuid: '123',
                    },
                    data: '[data]',
                },
            });
        });
    });

    it('должен логировать пользователя из record.user', () => {
        let logger = createLogger({ dsn, clientOptions });

        let user = {
            uid: 'user-uid',
            login: 'user-login',
            email: 'user-email',
            param: 'user-param',
        };

        logger.info({ user }, 'message');

        return wait().then(() => {
            let message = transport.send.args[0][1];
            assertMessage(message, {
                level: 'info',
                message: 'message',
                checksum: 'message',
                user: {
                    id: 'user-uid',
                    username: 'user-login',
                    email: 'user-email',
                    param: 'user-param',
                },
            });
        });
    });

    it('должен группировать текстовые сообщения', () => {
        let logger = createLogger({ dsn, clientOptions });

        logger.info({ group: 'sentry-group' }, 'message');

        return wait().then(() => {
            let message = transport.send.args[0][1];
            assertMessage(message, {
                level: 'info',
                message: 'message',
                culprit: 'sentry-group',
                checksum: 'sentry-group',
            });
        });
    });

    it('должен группировать исключения', () => {
        let logger = createLogger({ dsn, clientOptions });
        let err = new Error('message');
        err.stack = null;

        logger.info({ err, group: 'sentry-group' });

        return wait().then(() => {
            let message = transport.send.args[0][1];
            assertMessage(message, {
                level: 'info',
                message: 'Error: message',
                culprit: 'sentry-group',
                checksum: 'sentry-group',
                exception: [
                    {
                        type: 'Error',
                        value: 'message',
                        stacktrace: {
                            frames: [],
                        },
                    },
                ],
            });
        });
    });

    function createLogger(config) {
        return yandexLogger({
            streams: [
                {
                    level: 'trace',
                    stream: sentryStream(config),
                },
            ],
        });
    }

    /**
     * @returns {Promise}
     */
    function wait() {
        return new Promise(resolve => {
            setTimeout(resolve, 200);
        });
    }

    /**
     * @param {String} message
     * @returns {Object}
     */
    function parseMessage(message) {
        let buffer = Buffer.from(message, 'base64');
        let decoded = zlib.inflateSync(buffer);
        return JSON.parse(decoded.toString('utf-8'));
    }

    function assertMessage(message, expected) {
        let data = parseMessage(message);

        assert.ok(typeof data.modules === 'object' && data.modules !== null, 'message.modules must be an object');
        assert.ok(typeof data.event_id === 'string', 'message.event_id must be a string');

        delete data.modules;
        delete data.event_id;

        expected = Object.assign({
            timestamp: '1970-01-01T00:00:01',
            extra: {
                node: process.version,
            },
            logger: 'yandex-logger',
            platform: 'node',
            project: '2',
            request: {},
            server_name: os.hostname(),
            tags: {},
            user: {},
            environment: 'development',
            breadcrumbs: { values: [] },
        }, expected);

        assert.deepStrictEqual(data, expected);
    }
});
