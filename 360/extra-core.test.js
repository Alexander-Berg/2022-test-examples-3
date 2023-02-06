'use strict';

const ExtraCore = require('./extra-core.js');

let core;
let request;
let response;

beforeEach(function() {
    request = {
        cookies: {},
        headers: {
            'x-original-host': 'mail.yandex.ru',
            'x-original-uri': '/api/mobile/v1',
            'x-request-id': '12345',
            'x-real-ip': '2a02:6b8::25'
        },
        query: {
            uuid: 'deadbeef42',
            client_version: '10.0.3'
        },
        body: {}
    };

    response = {
        on: () => ({})
    };

    core = new ExtraCore(request, response);
});

describe('#httpCommonArgs', function() {
    it('прокидывает нужные заголовки', function() {
        const options = core.httpCommonArgs({ headers: { 'x-custom-header': 'privet' } });
        const headers = options.headers;

        expect(headers).toContainEntry([ 'x-real-ip', request.headers['x-real-ip'] ]);
        expect(headers).toContainEntry([ 'x-request-id', request.headers['x-request-id'] ]);
        expect(headers).toContainEntry([ 'x-custom-header', 'privet' ]);
    });

    it('добавляет project в http-лог', function() {
        expect(core.httpCommonArgs({})._http_log.project).toEqual('MOBILE-API');
    });

    describe('user-agent', function() {
        it('если нет в запросе, добавляет MOBILE-API', function() {
            const options = core.httpCommonArgs({});

            expect(options.headers).toContainEntry([ 'user-agent', 'MOBILE-API' ]);
        });

        it('если есть в запросе, добавляет его', function() {
            core.headers = {
                'user-agent': 'user-agent'
            };
            const options = core.httpCommonArgs({});

            expect(options.headers).toContainEntry([ 'user-agent', 'user-agent' ]);
        });
    });

    describe('типы и версии клиентов', function() {
        let getHeaders;

        beforeEach(function() {
            getHeaders = (client, clientVersion) => {
                core.params.client = client;
                core.params.client_version = clientVersion;
                const options = core.httpCommonArgs({ headers: { 'x-custom-header': 'privet' } });
                return options.headers;
            };
        });

        it('iphone', function() {
            const headers = getHeaders('iphone');
            expect(headers).toContainEntry([ 'x-yandex-clienttype', 'mobile-ios' ]);
            expect(headers).not.toHaveProperty('x-yandex-clientversion');
        });

        it('ipad', function() {
            const headers = getHeaders('ipad', '10.0.3');
            expect(headers).toContainEntry([ 'x-yandex-clienttype', 'tablet-ios' ]);
            expect(headers).toContainEntry([ 'x-yandex-clientversion', '10.0.3' ]);
        });

        it('aphone', function() {
            const headers = getHeaders('aphone', '4.4');
            expect(getHeaders('aphone')).toContainEntry([ 'x-yandex-clienttype', 'mobile-android' ]);
            expect(headers).toContainEntry([ 'x-yandex-clientversion', '4.4' ]);
        });

        it('apad', function() {
            const headers = getHeaders('apad');
            expect(headers).toContainEntry([ 'x-yandex-clienttype', 'tablet-android' ]);
            expect(headers).not.toHaveProperty('x-yandex-clientversion');
        });

        it('неизвестный client type', function() {
            expect(getHeaders('foo')).toContainEntry([ 'x-yandex-clienttype', 'mob-app-unknown' ]);
        });
    });
});

describe('#logCommonArgs', function() {
    it('содержит статус ответа апи', function() {
        expect(core.logCommonArgs()).toHaveProperty('api_status');
    });

    it('значение статуса норм', function() {
        core.res.mmapiStatus = 3;
        expect(core.logCommonArgs().api_status).toEqual(3);
    });
});
