const proxyquire = require('proxyquire');

const errors = require('../../../src/server/errors');
const oauth = require('../../../src/server/oauth');

const express = require('../fixtures/express');
const user = require('../fixtures/models/user');

const OAuthController = proxyquire.load('../../../src/server/controllers/oauth', {
    '../models/user': user,
});

describe('controllers/oauth', function() {
    let controller, req, res, sandbox;

    beforeEach(function() {
        req = {
            body: {},
            query: {},
            user: {},
        };

        res = express.getRes();

        sandbox = sinon.createSandbox();
        controller = new OAuthController(req, res);
    });

    afterEach(function() {
        sandbox.restore();
    });

    describe('extractTokenAndRedirectToService', function() {
        it('должен извлекать времемный код из урла, получать токен по нему, сохранять, направлять пользователя на /', function() {
            const code = 777777;
            const token = 'AJs73f(392bjesb82bksAD873f';
            const login = 'ensuetina';
            const clientSecret = '****************************';
            const servicePathToRedirect = '/';

            req.query.code = code;
            req.user.login = login;

            sandbox.stub(oauth, 'getTokenInExchangeForCode').returns(Promise.resolve(token));
            sandbox.stub(user, 'set').returns(Promise.resolve());
            sandbox.spy(res, 'redirect');

            return controller.extractTokenAndRedirectToService(clientSecret).then(() => {
                assert.calledWith(oauth.getTokenInExchangeForCode, code, clientSecret);
                assert.calledWith(user.set, login, token);
                assert.calledWith(res.redirect, servicePathToRedirect);
            });
        });

        it('должен генерировать соответствующую ошибку, если передан параметр error и error_description', function() {
            /* eslint camelcase: 0 */
            const error = 'invalid_grant';
            const error_description = 'error_description';
            const login = 'ensuetina';
            const clientSecret = '****************************';

            req.query.error = error;
            req.query.error_description = error_description;
            req.user.login = login;

            sandbox.spy(errors, 'getFailedToGetTokenFromOAuthServiceError');
            sandbox.stub(oauth, 'getTokenInExchangeForCode').returns(Promise.resolve());

            return controller.extractTokenAndRedirectToService(clientSecret).then(() => {
                assert.calledWith(errors.getFailedToGetTokenFromOAuthServiceError, login, error, error_description);
                assert.notCalled(oauth.getTokenInExchangeForCode);
            });
        });

        it('должен генерировать соответствующую ошибку, если не передан ни временный код (code), ни параметр c кодом ошибки (error)', function() {
            const login = 'ensuetina';
            const clientSecret = '****************************';

            req.user.login = login;

            sandbox.spy(errors, 'getOAuthRouteBadRequestError');
            sandbox.stub(oauth, 'getTokenInExchangeForCode').returns(Promise.resolve());

            return controller.extractTokenAndRedirectToService(clientSecret).then(() => {
                assert.calledWith(errors.getOAuthRouteBadRequestError, req.query);
                assert.notCalled(oauth.getTokenInExchangeForCode);
            });
        });

        it('должен генерировать соответствующую ошибку, если обменять временный код на токен не удалось', function() {
            /* eslint camelcase: 0 */
            const code = 777777;
            const error = 'invalid_grant';
            const error_description = 'error_description';
            const login = 'ensuetina';
            const clientSecret = '****************************';

            req.query.code = code;
            req.user.login = login;

            sandbox.spy(errors, 'getFailedToGetTokenFromOAuthServiceError');
            sandbox.stub(oauth, 'getTokenInExchangeForCode').returns(Promise.reject({
                response: {
                    statusCode: 403,
                    body: {
                        error,
                        error_description,
                    },
                },
            }));

            return controller.extractTokenAndRedirectToService(clientSecret).then(() => {
                assert.calledWith(errors.getFailedToGetTokenFromOAuthServiceError, login, error, error_description);
            });
        });

        it('должен генерировать соответствующую ошибку, если не удалось обменять временный код на токен, распарсить ответ с описанием ошибки от сервиса OAuth', function() {
            const code = 777777;
            const login = 'ensuetina';
            const clientSecret = '****************************';
            const unknownResponseObject = { unknown_error: true };

            req.query.code = code;
            req.user.login = login;

            sandbox.spy(errors, 'getOAuthUnknownResponseError');
            sandbox.stub(oauth, 'getTokenInExchangeForCode').returns(Promise.reject(unknownResponseObject));

            return controller.extractTokenAndRedirectToService(clientSecret).then(() => {
                assert.calledWith(errors.getOAuthUnknownResponseError, unknownResponseObject);
            });
        });
    });
});
