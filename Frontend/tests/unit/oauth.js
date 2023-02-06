/**
 * Тесты для модуля работы с OAuth-токеном пользователя
 */

require('should-http');

const proxyquire = require('proxyquire');
const errors = require('../../src/server/errors');

const rp = require('./fixtures/request-promise');

const OAuth = proxyquire.load('../../src/server/oauth', {
    'request-promise': rp,
});

describe('OAuth', function() {
    const sandbox = sinon.createSandbox();

    afterEach(function() {
        sandbox.restore();
    });

    describe('getRequestAccessPageUrl', function() {
        it('должен возвращать адрес странички с разрешеием использовать токен', function() {
            const accessPageUrl = 'https://oauth.yandex-team.ru/authorize?response_type=code&client_id=740b7164ae56499b8f61fdc978c32bf3&login_hint=brazhenko&force_confirm=yes';
            const login = 'brazhenko';

            assert.equal(OAuth.getRequestAccessPageUrl(login), accessPageUrl);
        });
    });

    describe('getTokenInExchangeForCode', function() {
        const clientSecret = '****************************';
        const code = 777777;
        const token = 'AJs73f(392bjesb82bksAD873f';
        const rpArgs = {
            resolveWithFullResponse: true,
            uri: 'https://oauth.yandex-team.ru/token',
            form: {
                'grant_type': 'authorization_code',
                'client_id': '740b7164ae56499b8f61fdc978c32bf3',
                'client_secret': clientSecret,
                code,
            },
            method: 'POST',
            headers: {
                'Content-type': 'application/x-www-form-urlencoded',
            },
            json: true,
        };

        afterEach(function() {
            rp.resetBehavior();
        });

        it('должен возвращать токен', function() {
            rp.withArgs(rpArgs).returns(Promise.resolve({ body: { 'access_token': token } }));

            return OAuth.getTokenInExchangeForCode(code, clientSecret)
                .then((t) => assert.equal(t, token));
        });
    });

    describe('isTokenValid', function() {
        const testToken = 'test_token';
        const serviceTicket = '*******************';
        const testUserIP = '192.168.1.1';
        const rpArgs = {
            resolveWithFullResponse: true,
            headers: {
                'X-Ya-Service-Ticket': serviceTicket,
            },
            uri: 'http://blackbox.yandex-team.ru/blackbox',
            qs: {
                'method': 'oauth',
                'oauth_token': testToken,
                'userip': testUserIP,
                'format': 'json',
            },
            method: 'GET',
            json: true,
        };

        afterEach(function() {
            rp.resetBehavior();
        });

        it('должен возвращать true, если сервис Blackbox подтвердил валидность токена', function() {
            rp.withArgs(rpArgs).returns(Promise.resolve({ body: { status: { id: 0 } } })); // VALID

            return OAuth.isTokenValid(testToken, testUserIP, serviceTicket)
                .then((isTokenValid) => assert.isOk(isTokenValid));
        });

        it('должен возвращать false, если сервис Blackbox сообщил, что токен протух', function() {
            rp.withArgs(rpArgs).returns(Promise.resolve({ body: { status: { id: 5 } } })); // VALID

            return OAuth.isTokenValid(testToken, testUserIP, serviceTicket)
                .then((isTokenValid) => assert.isNotOk(isTokenValid));
        });

        it('должен вызывать исключение, если запрос к сервису Blackbox выполнился с ошибкой', function() {
            rp.withArgs(rpArgs).returns(Promise.reject({ body: { message: 'service unavailable' } }));
            sandbox.stub(errors, 'getBlackboxServiceError').returns({ message: 'blackbox_error', meta: {} });

            const expectedError = {
                message: 'blackbox_error',
                meta: {
                    blackboxError: {
                        message: 'service unavailable',
                    },
                },
            };

            return OAuth.isTokenValid(testToken, testUserIP, serviceTicket)
                .catch((error) => assert.deepEqual(error, expectedError));
        });

        it('должен вызывать исключение, если сервис Blackbox ответил в неподдерживаемом нами формате', function() {
            rp.withArgs(rpArgs).returns(Promise.resolve({ body: { noFormat: true } }));
            sandbox.stub(errors, 'getBlackboxServiceError').returns({ message: 'blackbox_error', meta: {} });

            const expectedError = {
                message: 'blackbox_error',
                meta: {
                    blackboxResponse: {
                        noFormat: true,
                    },
                },
            };

            return OAuth.isTokenValid(testToken, testUserIP, serviceTicket)
                .catch((error) => assert.deepEqual(error, expectedError));
        });
    });
});
