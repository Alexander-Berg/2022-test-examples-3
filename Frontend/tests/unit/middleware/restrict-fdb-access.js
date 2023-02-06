/**
 * Тесты middleware для запрета доступа к FDB внешним сотрудникам.
 */

const proxyquire = require('proxyquire');
const Logger = require('../fixtures/logger');
const errors = require('../../../src/server/errors');

const { FORBIDDEN } = require('http-status');

const RestrictFdbAccess = proxyquire.load(
    '../../../src/server/middleware/restrict-fdb-access',
    {
        '../logger': Logger,
    },
);

describe('Restrict FDB access middleware', function() {
    const sandbox = sinon.createSandbox();

    beforeEach(function() {});

    afterEach(function() {
        sandbox.restore();
    });

    it('должен продолжить цепочку, если пользователь имеет доступ к FDB (не внешний сотрудник)', function() {
        const req = { user: { canUseFdb: true }, params: 1 };
        const res = { status: sinon.spy(function() {
            return this;
        }), end: sinon.spy(function() {
            return this;
        }) };
        const next = sinon.spy();

        RestrictFdbAccess(req, res, next);

        return Promise.resolve().then(() => {
            assert.notCalled(res.end);
            assert.notCalled(res.status);
            assert.calledOnce(next);
        });
    });

    it('должен прервать цепочку статусом 403 и ошибкой, если пользователь – внешний сотрудник', function() {
        const req = {
            user: { canUseFdb: false, login: '123' },
            params: { id: 1 },
        };
        const res = { status: sinon.spy(function() {
            return this;
        }), json: sinon.spy(function() {
            return this;
        }) };
        const next = sinon.spy();
        const err = errors.getFdbUsageForbiddenError();

        RestrictFdbAccess(req, res, next);

        // TODO: использовать явные методы
        // для вызова кода после всех
        // тестовых промисов
        return Promise.resolve().then(() => {
            assert.calledWith(res.status, FORBIDDEN);
            assert.calledWith(res.json, err);
            assert.callOrder(res.status, res.json);
            assert.notCalled(next);
        });
    });
});
