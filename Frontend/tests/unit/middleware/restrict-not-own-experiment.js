/**
 * Тесты middleware для проверки OAuth-токена пользователя.
 */

const proxyquire = require('proxyquire');
const Logger = require('../fixtures/logger');
const errors = require('../../../src/server/errors');

const { FORBIDDEN } = require('http-status');

const mockExperiment = {
    getById() {
        return Promise.resolve({ login: '', params: { owners: [] } });
    },
};

const RestrictNotOwnExperiment = proxyquire.load(
    '../../../src/server/middleware/restrict-not-own-experiment',
    {
        '../logger': Logger,
        '../models/experiment': mockExperiment,
    },
);

describe('Restrict not own experiment middleware', function() {
    const sandbox = sinon.createSandbox();

    beforeEach(function() {});

    afterEach(function() {
        sandbox.restore();
    });

    it('должен продолжить цепочку если пользователь может видеть не только свои эксперименты', function() {
        const req = { user: { canSeeOnlyOwnExperiments: false }, params: 1 };
        const res = { status: sinon.spy(function() {
            return this;
        }), end: sinon.spy(function() {
            return this;
        }) };
        const next = sinon.spy();

        RestrictNotOwnExperiment(req, res, next);

        return Promise.resolve().then(() => {
            assert.notCalled(res.end);
            assert.notCalled(res.status);
            assert.calledOnce(next);
        });
    });

    it('должен получить чужой эксперимент по ID и прервать цепочку статусом 403 и ошибкой', function() {
        const req = {
            user: { canSeeOnlyOwnExperiments: true, login: '123' },
            params: { id: 1 },
        };
        const res = { status: sinon.spy(function() {
            return this;
        }), json: sinon.spy(function() {
            return this;
        }) };
        const next = sinon.spy();
        const stub = sandbox.stub(mockExperiment, 'getById');
        const stubWithArgs = stub.withArgs({ id: req.params.id, fields: ['login', 'params.owners'] });
        stubWithArgs.returns(Promise.resolve({ login: '12313', params: { owners: '' } }));
        const expErr = errors.experimentAccessIsForbiddenError();

        RestrictNotOwnExperiment(req, res, next);

        // TODO: использовать явные методы
        // для вызова кода после всех
        // тестовых промисов
        return Promise.resolve().then(() => {
            assert.calledWith(mockExperiment.getById, { id: req.params.id, fields: ['login', 'params.owners'] });
            assert.calledWith(res.status, FORBIDDEN);
            assert.calledWith(res.json, expErr);
            assert.callOrder(res.status, res.json);
            assert.notCalled(next);
        });
    });

    it('должен получить свой эксперимент по ID и продолжить цепочку', function() {
        const req = {
            user: { canSeeOnlyOwnExperiments: true, login: '123' },
            params: { id: 1 },
        };
        const res = { status: sinon.spy(), json: sinon.spy() };
        const next = sinon.spy();
        const stub = sandbox.stub(mockExperiment, 'getById');
        const stubWithArgs = stub.withArgs({ id: req.params.id, fields: ['login', 'params.owners'] });
        stubWithArgs.returns(Promise.resolve({ login: req.user.login }));

        RestrictNotOwnExperiment(req, res, next);

        // TODO: использовать явные методы
        // для вызова кода после всех
        // тестовых промисов
        return Promise.resolve().then(() => {
            assert.calledWith(mockExperiment.getById, { id: req.params.id, fields: ['login', 'params.owners'] });
            assert.notCalled(res.status);
            assert.notCalled(res.json);
            assert.calledOnce(next);
        });
    });
});
