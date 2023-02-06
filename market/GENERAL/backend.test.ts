/* eslint-disable max-classes-per-file */

import {HttpTransport, httpTransportDefaultOptions} from '../http/transport';
import {Backend} from './backend';
import {Context} from './context';
import {Transport} from './transport';
import {backendsConfig} from './backends-config';

test('setup', function () {
    const options = {host: 'http://awesome'};

    class SomeBackend extends Backend.setup(HttpTransport, 'Awesome', options) {}

    const backend = SomeBackend.factory(new Context(null));

    expect(SomeBackend.backendName).toBe('Awesome');
    expect(SomeBackend.options).toEqual(options);

    expect(backend.name).toBe('Awesome');
    expect(backend.transport.options.host).toBe('http://awesome');
    expect(backend.transport).toBeInstanceOf(HttpTransport);

    expect(backend.transport.options).toEqual({
        host: 'http://awesome',
        ...httpTransportDefaultOptions,
    });
});

test('setup - failed', function () {
    expect(() => class SomeBackend extends Backend
        .setup(HttpTransport, 'Awesome', {}) {}).toThrow('Host must be specified');
});

test('setup - backendsConfig + local config', function () {
    const options = {host: 'http://awesome', custom: 'backendsConfig'};

    backendsConfig.setup({Awesome: options});

    class SomeBackend extends Backend.setup(HttpTransport, 'Awesome', {custom: 'local'}) {}

    const backend = SomeBackend.factory(new Context(null));

    expect(SomeBackend.backendName).toBe('Awesome');
    expect(SomeBackend.options).toEqual(options);

    expect(backend.name).toBe('Awesome');
    expect(backend.transport.options.host).toBe('http://awesome');
    expect(backend.transport).toBeInstanceOf(HttpTransport);

    expect(backend.transport.options).toEqual({
        host: 'http://awesome',
        custom: 'backendsConfig',
        ...httpTransportDefaultOptions,
    });
});

test('setup - backendsConfig', function () {
    const options = {host: 'http://awesome', custom: 'backendsConfig'};
    backendsConfig.setup({Awesome: options});

    class SomeBackend extends Backend.setup(HttpTransport, 'Awesome') {}

    const backend = SomeBackend.factory(new Context(null));

    expect(SomeBackend.backendName).toBe('Awesome');
    expect(SomeBackend.options).toEqual(options);

    expect(backend.name).toBe('Awesome');
    expect(backend.transport.options.host).toBe('http://awesome');
    expect(backend.transport).toBeInstanceOf(HttpTransport);

    expect(backend.transport.options).toEqual({
        host: 'http://awesome',
        custom: 'backendsConfig',
        ...httpTransportDefaultOptions,
    });
});

test('fetch', async function () {
    const mock = jest.fn();

    class NullTransport extends Transport {
        public async send(params) {
            mock('send');
            return params;
        }
    }

    class SomeBackend extends Backend.setup(NullTransport, 'SomeBackend', {
        host: 'awesome',
    }) {
        public async fetch(params) {
            mock('fetch');
            const result = await super.fetch(params);
            mock('fetchEnd');
            return result;
        }

        public async prepareRequest(params) {
            mock('prepareRequest');
            return super.prepareRequest(params);
        }

        public async prepareResponse(response, request) {
            mock('prepareResponse');
            return super.prepareResponse(response, request);
        }
    }

    const backend = SomeBackend.factory(new Context(null));

    const result = await backend.fetch({a: 1});

    expect(result).toEqual({
        a: 1,
        backend: 'SomeBackend',
    });

    const callSeq = mock.mock.calls.map(call => call[0]);
    expect(callSeq).toEqual([
        'fetch',
        'prepareRequest',
        'send',
        'prepareResponse',
        'fetchEnd',
    ]);
});
