/**
 * Тесты middleware для проверки OAuth-токена пользователя.
 */

const proxyquire = require('proxyquire');
const OAuth = require('../../../src/server/oauth');
const Logger = require('../fixtures/logger');
const errors = require('../../../src/server/errors');
const errorRenders = require('../../../src/server/helpers/errors-renders');

const OAuthMiddleware = proxyquire.load('../../../src/server/middleware/oauth', {
    '../logger': Logger,
});

describe('OAuth middleware', function() {
    const sandbox = sinon.createSandbox();
    const testLogin = 'test_login';
    const testToken = 'test_token';
    const testIP = '192.168.1.1';
    const testRequestAccessUrl = 'http://requestaccess.url';
    const serviceTicket = '*******************';
    const defaultReq = {
        id: '1',
        user: {
            login: testLogin,
            token: testToken,
        },
        ip: testIP,
        tvm: {
            tickets: {
                blackbox: {
                    ticket: serviceTicket,
                },
            },
        },
    };
    const defaultRes = {};
    const defaultNext = () => {};
    let req = defaultReq, res = defaultRes, next = defaultNext;

    beforeEach(function() {
        sandbox.stub(OAuth, 'getRequestAccessPageUrl').withArgs(testLogin).returns(testRequestAccessUrl);
    });

    afterEach(function() {
        req = defaultReq;
        res = defaultRes;
        next = defaultNext;
        sandbox.restore();
    });

    it('должен передавать запрос следующему middleware, если у нас есть токен и он валидный', function(done) {
        sandbox.stub(OAuth, 'isTokenValid').withArgs(testToken, testIP, serviceTicket).returns(Promise.resolve(true));

        next = () => done();

        new OAuthMiddleware({ noRedirectToAccessPage: false, req, res, next }).process();
    });

    it('должен делать редирект, если у нас нет токена пользователя и запрос был не в API', function(done) {
        sandbox.stub(errors, 'getUnauthorizedError')
            .returns({ message: 'getUnauthorizedError' });
        req = {
            id: '1',
            user: {
                login: testLogin,
            },
        };
        res = {
            redirect: (redirectUrl) => {
                assert.equal(redirectUrl, testRequestAccessUrl);
                done();
            },
        };

        new OAuthMiddleware({ noRedirectToAccessPage: false, req, res, next }).process();
    });

    it('должен возвращать ошибку, если у нас нет токена пользователя и запрос был в API', function(done) {
        const unauthorizedError = { message: 'getUnauthorizedError' };
        sandbox.stub(errors, 'getUnauthorizedError')
            .returns(unauthorizedError);
        req = {
            id: '1',
            user: {
                login: 'test_login',
            },
        };
        const renderJsonErrorStub = sandbox.stub(errorRenders, 'renderJsonError').callsFake(function() {
            assert.calledOnce(renderJsonErrorStub);
            assert.calledWith(renderJsonErrorStub, req, res, unauthorizedError, sinon.match.instanceOf(Logger));
            done();
        });

        new OAuthMiddleware({ noRedirectToAccessPage: true, req, res, next }).process();
    });

    it('должен делать редирект, если токен пользователя не валиден и запрос был не в API', function(done) {
        sandbox.stub(OAuth, 'isTokenValid').withArgs(testToken, testIP, serviceTicket).returns(Promise.resolve(false));
        const invalidOAuthTokenError = { message: 'invalidOAuthTokenError' };
        sandbox.stub(errors, 'getInvalidOAuthTokenError')
            .withArgs(testRequestAccessUrl)
            .returns(invalidOAuthTokenError);

        res = {
            redirect: (redirectUrl) => {
                assert.equal(redirectUrl, testRequestAccessUrl);
                done();
            },
        };

        new OAuthMiddleware({ noRedirectToAccessPage: false, req, res, next }).process();
    });

    it('должен возвращать ошибку, если токен пользователя не валиден и запрос был в API', function(done) {
        sandbox.stub(OAuth, 'isTokenValid').withArgs(testToken, testIP, serviceTicket).returns(Promise.resolve(false));
        const invalidOAuthTokenError = { message: 'invalidOAuthTokenError' };
        sandbox.stub(errors, 'getInvalidOAuthTokenError')
            .withArgs(testRequestAccessUrl)
            .returns(invalidOAuthTokenError);

        const renderJsonErrorStub = sandbox.stub(errorRenders, 'renderJsonError').callsFake(function() {
            assert.calledOnce(renderJsonErrorStub);
            assert.calledWith(renderJsonErrorStub, req, res, invalidOAuthTokenError, sinon.match.instanceOf(Logger));
            done();
        });

        new OAuthMiddleware({ noRedirectToAccessPage: true, req, res, next }).process();
    });

    it('должен возвращать ошибку, если при определении валидности токена произошло исключение', function(done) {
        const oauthError = { messge: 'oauthError' };
        sandbox.stub(OAuth, 'isTokenValid').withArgs(testToken, testIP, serviceTicket).returns(Promise.reject(oauthError));

        const renderJsonErrorStub = sandbox.stub(errorRenders, 'renderJsonError').callsFake(function() {
            assert.calledOnce(renderJsonErrorStub);
            assert.calledWith(renderJsonErrorStub, req, res, oauthError, sinon.match.instanceOf(Logger));
            done();
        });

        new OAuthMiddleware({ noRedirectToAccessPage: false, req, res, next }).process();
    });
});
