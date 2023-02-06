/* eslint-env mocha */
'use strict';

const assert = require('assert');
const sinon = require('sinon');
const nock = require('nock');
const proxyquire = require('proxyquire');

const Transport = require('../../../streams/sentry/transport');

describe('YandexLogger. Streams. Sentry Transport', () => {
    let client;
    let headers;
    let message;

    beforeEach(() => {
        client = {
            dsn: {
                protocol: 'https',
                host: 'host.yandex',
                port: 9000,
                path: '/sentry/',
                // eslint-disable-next-line camelcase
                project_id: '2',
            },
            emit: sinon.stub(),
            ca: 'ca-cert',
            sendTimeout: 5,
        };

        headers = {
            authorization: 'Basic Auth',
        };

        message = '[body]';
    });

    it('должен отправлять соощение в Sentry', () => {
        let api = nock('https://host.yandex:9000')
            .matchHeader('authorization', 'Basic Auth')
            .post('/sentry/api/2/store/', message)
            .reply(200);

        let transport = new Transport();
        let cb = sinon.stub();

        return transport
            .send(client, message, headers, 'event-id', cb)
            .then(() => {
                api.done();

                sinon.assert.calledWithExactly(cb, null, 'event-id');
                sinon.assert.calledOnce(cb);

                sinon.assert.calledWithExactly(client.emit, 'logged', 'event-id');
                sinon.assert.calledOnce(client.emit);
            });
    });

    it('должен принимать опции для got', () => {
        let got = sinon.stub().resolves();
        let Transport = proxyquire('../../../streams/sentry/transport', { got });
        let transport = new Transport({ rejectUnauthorized: false });

        transport.send(client, message, headers, 'event-id');

        sinon.assert.calledWithExactly(got, 'https://host.yandex:9000/sentry/api/2/store/', {
            method: 'POST',
            body: '[body]',
            headers: {
                authorization: 'Basic Auth',
            },
            ca: 'ca-cert',
            rejectUnauthorized: false,
            timeout: 5000,
        });
        sinon.assert.calledOnce(got);
    });

    it('должен обрабатывать http ошибки', () => {
        let api = nock('https://host.yandex:9000')
            .post('/sentry/api/2/store/', message)
            .reply(503, 'Internal Error', {
                'x-sentry-error': 'error reason',
            });

        let transport = new Transport();
        let cb = sinon.stub();

        return transport
            .send(client, message, headers, 'event-id', cb)
            .then(() => {
                api.done();

                sinon.assert.calledWithExactly(cb, sinon.match.instanceOf(Error));
                sinon.assert.calledOnce(cb);

                sinon.assert.calledWithExactly(client.emit, 'error', sinon.match.instanceOf(Error));
                sinon.assert.calledOnce(client.emit);

                let error = cb.args[0][0];
                assert.strictEqual(error.message, 'HTTP Error (503): error reason');
                assert.strictEqual(error.eventId, 'event-id');
                assert.strictEqual(error.reason, 'error reason');
                assert.strictEqual(error.statusCode, 503);
                assert.strictEqual(error.sendMessage, message);
                assert.deepStrictEqual(error.requestHeaders, headers);
            });
    });
});
