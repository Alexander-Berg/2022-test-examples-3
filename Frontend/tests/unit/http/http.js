'use strict';

require('should-http');

const url = require('url');
const request = require('supertest');
const sinon = require('sinon');
const proxyquire = require('proxyquire');

const SUCCESS_STATUS = 200;
const FAILURE_STATUS = 503;

class DbStub {
    connect() {}
}

class ErrorDbStub {
    constructor() {
        this.error = new Error();
    }
    connect() {}
}

// TODO SBSDEV-8143: Починить unit-тесты на прохождение запроса через список известных слоев и включить unit-тесты в PR монорепы
describe.skip('HTTP API', function() {
    const middlewares = {};
    const extendedMiddlewaresNames = [''];
    const extendedMiddlewares = {};
    let sandbox, app, deps;

    beforeEach(function() {
        sandbox = sinon.createSandbox();

        ['csp', 'tvm', 'authenticate-user', 'authorize-user', 'user-props', 'oauth', 'csrf', 'dbCheck', 'restrict-not-own-experiment', 'check-if-exp-exists'].forEach((m) => {
            middlewares[m] = sandbox.spy(function(req, res, next) {
                return next();
            });
        });
        extendedMiddlewaresNames.forEach((m) => {
            extendedMiddlewares[m] = sandbox.spy(function(req, res, next) {
                return next();
            });
            middlewares[m] = sandbox.spy(function() {
                return extendedMiddlewares[m];
            });
        });
        middlewares.dbCheck = sandbox.spy(function(req, res, next, dbEntity) {
            if (dbEntity.error) {
                res.status(FAILURE_STATUS);
            }
            return next();
        });

        deps = {
            './middleware/csp': middlewares.csp,
            './middleware/authenticate-user': middlewares['authenticate-user'],
            './middleware/authorize-user': middlewares['authorize-user'],
            './middleware/env-oauth': middlewares.oauth,
            './middleware/user-props': middlewares['user-props'],
            './middleware/tvm': middlewares.tvm,
            './middleware/db-check': middlewares.dbCheck,
            './middleware/restrict-not-own-experiment': middlewares['restrict-not-own-experiment'],
            './middleware/check-if-exp-exists': middlewares['check-if-exp-exists'],
            csurf() {
                return middlewares.csrf;
            },
            './helpers/get-seed-html': () => Promise.resolve('<html></html>'),
            './db': DbStub,
            './controllers/debug-pages': require('../fixtures/controllers/debug-pages'),
            './controllers/experiments-api': require('../fixtures/controllers/experiments-api'),
            './controllers/nirvana': require('../fixtures/controllers/nirvana'),
            './controllers/meta-api': require('../fixtures/controllers/meta-api'),
            './controllers/user-api': require('../fixtures/controllers/user-api'),
            'express-http-uatraits': () => (req, res, next) => next(),
            './controllers/results': require('../fixtures/controllers/results'),
        };

        app = proxyquire.load('../../../src/server/index', deps);
    });

    afterEach(function() {
        sandbox.restore();
    });

    it('health-check-ручка возвращает корректный cтатус', function() {
        return request(app)
            .get('/ping')
            .then((res) => res.should.have.status(SUCCESS_STATUS));
    });

    describe('редиректы', function() {
        it('Старый урл админки → новый (/moderate → /moderate/main)', function(done) {
            request(app).get('/moderate')
                .then((res) => {
                    assert.strictEqual(url.parse(res.headers.location).pathname, '/moderate/main', '/moderate → /moderate/main');
                    done();
                });
        });
    });

    describe('при ошибке БД запросы должны возвращать 503 ошибку', function() {
        [
            // Experiments API
            { url: '/api/experiment/1/pool_validity', method: 'post' },

            // Pages
            { url: '/', method: 'get' },
            { url: '/moderate/main', method: 'get' },

            // Experiments API
            { url: '/api/experiment/1', method: 'put' },
            { url: '/api/experiment', method: 'post' },
            { url: '/api/experiment/1/workflow', method: 'post' },
            { url: '/api/experiment/1/clone', method: 'post' },
            { url: '/api/workflow/1/start', method: 'post' },
        ].forEach((t) => {
            const desc = `${t.method.toUpperCase()} ${t.url}`;
            it(desc, function(done) {
                deps['./db'] = ErrorDbStub;
                app = proxyquire.load('../../../src/server/index', deps);
                request(app)[t.method](t.url)
                    .expect(FAILURE_STATUS, done);
            });
        });
    });

    describe('слои csp dbCheck csrf authenticate-user authorize-user user-props oauth dbCheck', function() {
        const authenticateMiddlewares = ['authenticate-user', 'authorize-user', 'user-props'];
        const restrictNotOwnExpetimentMiddlewares = ['restrict-not-own-experiment'];
        const restrictExperimentTypeMiddlewares = ['restrict-experiment-type'];

        const baseExperimentsMiddlewares = ['tvm', 'authenticate-user', 'authorize-user', 'user-props', 'dbCheck', 'oauth'];

        const baseExperimentItemMiddlewares = [...baseExperimentsMiddlewares, ...restrictNotOwnExpetimentMiddlewares];

        [
            // Tricky experiments API without experiment in route
            //{ url: '/api/experiment/2/workflow', method: 'get', middlewares: baseExperimentItemMiddlewares },
            //{ url: '/api/get-current-workflow/1', method: 'get', middlewares: baseExperimentItemMiddlewares },
            //{ url: '/api/ab-export', method: 'get', middlewares: baseExperimentsMiddlewares },
            //{ url: '/api/export', method: 'get', middlewares: baseExperimentsMiddlewares },

            // Experiments API
            { url: '/api/experiment', method: 'post', middlewares: [...baseExperimentsMiddlewares, ...restrictExperimentTypeMiddlewares] },
            //{ url: '/api/experiment/ab-export', method: 'get', middlewares: baseExperimentsMiddlewares },
            { url: '/api/experiment/start', method: 'post', middlewares: [...baseExperimentsMiddlewares, ...restrictExperimentTypeMiddlewares] },
            { url: '/api/experiment/create-and-start', method: 'post', middlewares: [...baseExperimentsMiddlewares, ...restrictExperimentTypeMiddlewares] },
            { url: '/api/experiment/layouts', method: 'post', middlewares: baseExperimentsMiddlewares },
            { url: '/api/experiment/layouts_v2', method: 'post', middlewares: baseExperimentsMiddlewares },

            // Experiments item API
            // TODO: некоторые пути падают по таймауту
            // в тестах, поэтому закомменчены - нужно разрбраться
            // почему они падают
            { url: '/api/experiment/1', method: 'put', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/queries_as_file', method: 'get', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1', method: 'get', middlewares: baseExperimentItemMiddlewares },
            //{ url: '/api/experiment/1/upload-results-files', method: 'post', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/upload-plan-stats', method: 'post', middlewares: baseExperimentItemMiddlewares },
            //{ url: '/api/experiment/1/upload-exp-stats', method: 'post', middlewares: baseExperimentItemMiddlewares },
            //{ url: '/api/experiment/1/pools', method: 'post', middlewares: baseExperimentItemMiddlewares },
            //{ url: '/api/experiment/1/workflow/status', method: 'post', middlewares: baseExperimentItemMiddlewares },
            //{ url: '/api/experiment/1/2/pool_validity', method: 'get', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/2/statuses', method: 'get', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/config', method: 'get', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/config-override', method: 'get', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/queries', method: 'get', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/poll-data', method: 'get', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/scenario-data', method: 'get', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/layouts', method: 'get', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/filter', method: 'get', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/figma-template', method: 'get', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/workflow/create-and-start', method: 'post', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/start', method: 'post', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/stop', method: 'post', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/restart', method: 'post', middlewares: baseExperimentItemMiddlewares },
            { url: '/api/experiment/1/workflow', method: 'post', middlewares: baseExperimentItemMiddlewares },
            { url: '/api/experiment/1/clone', method: 'post', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/blank-clone', method: 'post', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/workflow/start', method: 'post', middlewares: baseExperimentItemMiddlewares },
            { url: '/api/experiment/1/:workflowId/call_analyst', method: 'post', middlewares: baseExperimentItemMiddlewares },
            { url: '/api/experiment/1/send_reaction', method: 'post', middlewares: baseExperimentItemMiddlewares },
            { url: '/api/experiment/1/pool_validity', method: 'post', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/results', method: 'get', middlewares: baseExperimentItemMiddlewares },
            { url: '/api/experiment/1/v2/results', method: 'get', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/results/queries_analysis', method: 'get', middlewares: baseExperimentItemMiddlewares },
            // { url: '/api/experiment/1/results/win-against-control-system', method: 'get', middlewares: baseExperimentItemMiddlewares },
            { url: '/api/experiment/1/results/ab-poll', method: 'get', middlewares: baseExperimentItemMiddlewares },

            // Workflow API
            //{ url: '/api/get-current-workflow/1', method: 'get', middlewares: baseExperimentItemMiddlewares },
            { url: '/api/workflow/1/start', method: 'post', middlewares: ['csp', 'tvm', ...authenticateMiddlewares, 'oauth', 'dbCheck'] },

            // Pages
            { url: '/', method: 'get', middlewares: ['csp', 'tvm', ...authenticateMiddlewares, 'oauth', 'dbCheck'] },
            { url: '/experiment', method: 'get', middlewares: ['csp', 'tvm', 'authenticate-user', 'authorize-user', 'user-props', 'oauth', 'dbCheck'] },
            { url: '/moderate/main', method: 'get', middlewares: ['csp', 'tvm', 'authenticate-user', 'authorize-user', 'user-props', 'oauth', 'dbCheck'] },
            { url: '/user/token', method: 'get', middlewares: ['csp', 'tvm', 'authenticate-user', 'authorize-user', 'user-props', 'oauth', 'dbCheck'] },

            // Meta API
            { url: '/api/meta/workflow/locked', method: 'post', middlewares: ['csp', 'tvm', 'authenticate-user', 'authorize-user', 'user-props', 'oauth', 'dbCheck'] },
            { url: '/api/meta/default-beta', method: 'post', middlewares: ['csp', 'tvm', 'authenticate-user', 'authorize-user', 'user-props', 'oauth', 'dbCheck'] },
            { url: '/api/meta/pools-list', method: 'post', middlewares: ['csp', 'tvm', 'authenticate-user', 'authorize-user', 'user-props', 'oauth', 'dbCheck'] },
            { url: '/api/meta/notification', method: 'post', middlewares: ['csp', 'tvm', 'authenticate-user', 'authorize-user', 'user-props', 'oauth', 'dbCheck'] },

            { url: '/api/user/notification', method: 'get', middlewares: ['csp', 'tvm', 'authenticate-user', 'authorize-user', 'user-props', 'oauth', 'csrf', 'dbCheck'] },
            { url: '/api/user/notification/disable', method: 'post', middlewares: ['csp', 'tvm', 'authenticate-user', 'authorize-user', 'user-props', 'oauth', 'dbCheck'] },
        ].forEach((t) => {
            const desc = `${t.method.toUpperCase()} ${t.url} middlewares: ${t.middlewares ? t.middlewares.join(' ') : ''}`;

            it(desc, function(done) {
                function check() {
                    Object.keys(middlewares).forEach((m) => {
                        const routeShouldGoThroughMiddleware = t.middlewares && t.middlewares.indexOf(m) !== -1;

                        if (extendedMiddlewaresNames.indexOf(m) !== -1) {
                            assert.equal(
                                middlewares[m]().callCount,
                                routeShouldGoThroughMiddleware ? 1 : 0,
                                `Запрос ${desc} ${routeShouldGoThroughMiddleware ? 'должен' : 'не должен'} проходить через слой ${m}`,
                            );
                        } else {
                            assert.equal(
                                middlewares[m].callCount,
                                routeShouldGoThroughMiddleware ? 1 : 0,
                                `Запрос ${desc} ${routeShouldGoThroughMiddleware ? 'должен' : 'не должен'} проходить через слой ${m}`,
                            );
                        }
                    });

                    done();
                }

                request(app)[t.method](t.url)
                    .end(() => {
                        check();
                    });
            });
        });
    });
});
