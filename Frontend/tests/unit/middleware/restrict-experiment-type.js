/**
 * Тесты middleware для проверки OAuth-токена пользователя.
 */

const proxyquire = require('proxyquire');
const Logger = require('../fixtures/logger');

const mockExperiment = {
    getById() {},
};

const RestrictExperimentType = proxyquire.load(
    '../../../src/server/middleware/restrict-experiment-type',
    {
        '../logger': Logger,
        '../models/experiment': mockExperiment,
    },
);

describe('Restrict experiment type middleware', function() {
    const sandbox = sinon.createSandbox();

    beforeEach(function() {});

    afterEach(function() {
        sandbox.restore();
    });

    it('должен продолжать цепочку если пользователь не external', function() {
        const req = { user: { isExternal: false } };
        const res = { status: sinon.spy(), end: sinon.spy() };
        const next = sinon.spy();

        RestrictExperimentType('')(req, res, next);

        assert.notCalled(res.end);
        assert.notCalled(res.status);
        assert.calledOnce(next);
    });

    it('должен продолжать цепочку если тип эксперимента не совпадает с настройкой', function() {
        const req = {
            user: { isExternal: true },
            body: { type: 'test' },
        };
        const res = { status: sinon.spy(), json: sinon.spy() };
        const next = sinon.spy();

        RestrictExperimentType('serp')(req, res, next);

        assert.notCalled(res.json);
        assert.notCalled(res.status);
        assert.calledOnce(next);
    });

    it('должен обрывать цепочку если тип эксперимента совпадает с настройкой', function() {
        const req = {
            user: { isExternal: true },
            body: { type: 'serp' },
        };
        const res = { status: sinon.spy(), json: sinon.spy() };
        const next = sinon.spy();

        RestrictExperimentType('serp')(req, res, next);

        assert.calledOnce(res.status);
        assert.calledOnce(res.json);
        assert.callOrder(res.status, res.json);
        assert.notCalled(next);
    });
});
