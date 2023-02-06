const { expect } = require('chai');
const nock = require('nock');

const api = require('api');
const request = require('co-supertest').agent(api.callback());

const { AdminToRole } = require('db/postgres');

const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const adminsFactory = require('tests/factory/adminsFactory');
const tvmClientsFactory = require('tests/factory/tvmClientsFactory');

const dbHelper = require('tests/helpers/clear');
const nockTvm = require('tests/helpers/nockTvm');

describe('Idm remove role controller', () => {
    beforeEach(function *() {
        yield dbHelper.clear();

        nockTvm.checkTicket({ src: 1234 });
    });

    afterEach(nock.cleanAll);

    it('should success remove role', function *() {
        const admin = { login: 'akuv' };
        const role = { code: 'admin' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
        yield tvmClientsFactory.create({ clientId: 1234, name: 'idm' });

        const res = yield request
            .post('/v1/idm/remove-role/')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .set('Content-Type', 'application/x-www-form-urlencoded')
            .set('Host', 'expert-admin.yandex-team.ru')
            .send('login=akuv&role={"expert":"admin"}')
            .end();

        expect(res.body).to.deep.equal({ code: 0 });

        const stored = yield AdminToRole.findAll();

        expect(stored).to.be.empty;
    });

    it('should failed when host is not yandex-team.ru', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'idm' });

        const res = yield request
            .post('/v1/idm/remove-role/')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .set('Host', 'expert-admin.yandex.ru')
            .send({})
            .end();

        expect(res.body).to.deep.equal({
            code: 403,
            fatal: 'Application should locate on yandex-team.ru domain'
        });
    });

    it('should failed when client is not idm', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'chance' });

        const res = yield request
            .post('/v1/idm/remove-role/')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .set('Host', 'expert-admin.yandex-team.ru')
            .send({})
            .end();

        expect(res.body).to.deep.equal({
            code: 403,
            fatal: 'Client has no access'
        });
    });

    it('should failed when admin not found', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'idm' });

        const res = yield request
            .post('/v1/idm/remove-role/')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .set('Content-Type', 'application/x-www-form-urlencoded')
            .set('Host', 'expert-admin.yandex-team.ru')
            .send('login=akuv&role={"expert":"admin"}')
            .end();

        expect(res.body).to.deep.equal({
            code: 404,
            fatal: 'Admin not found'
        });
    });

    it('should failed when role not found', function *() {
        yield adminsFactory.create({ login: 'akuv' });
        yield tvmClientsFactory.create({ clientId: 1234, name: 'idm' });

        const res = yield request
            .post('/v1/idm/remove-role/')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .set('Content-Type', 'application/x-www-form-urlencoded')
            .set('Host', 'expert-admin.yandex-team.ru')
            .send('login=akuv&role={"expert":"admin"}')
            .end();

        expect(res.body).to.deep.equal({
            code: 404,
            fatal: 'Role not found'
        });
    });
});
