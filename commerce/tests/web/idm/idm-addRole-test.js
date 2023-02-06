const { expect } = require('chai');
const nock = require('nock');

const api = require('api');
const request = require('co-supertest').agent(api.callback());

const rolesFactory = require('tests/factory/rolesFactory');
const adminsFactory = require('tests/factory/adminsFactory');
const tvmClientsFactory = require('tests/factory/tvmClientsFactory');

const { AdminToRole } = require('db/postgres');

const dbHelper = require('tests/helpers/clear');
const nockTvm = require('tests/helpers/nockTvm');

describe('Idm add role controller', () => {
    beforeEach(function *() {
        yield dbHelper.clear();

        nockTvm.checkTicket({ src: 1234 });
    });

    afterEach(nock.cleanAll);

    it('should add new role to exists user', function *() {
        yield rolesFactory.create({ code: 'developer' });
        yield adminsFactory.create({ login: 'anyok' });
        yield tvmClientsFactory.create({ clientId: 1234, name: 'idm' });

        const res = yield request
            .post('/v1/idm/add-role/')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .set('Content-Type', 'application/x-www-form-urlencoded')
            .set('Host', 'expert-admin.yandex-team.ru')
            .send('login=anyok&role={"expert":"developer"}')
            .expect(200)
            .end();

        expect(res.body).to.deep.equal({ code: 0 });

        const stored = yield AdminToRole.findAll();

        expect(stored).to.have.length(1);
    });

    it('should return error when host is not yandex-team.ru', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'idm' });

        const res = yield request
            .post('/v1/idm/add-role/')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .set('Host', 'expert-admin.yandex.ru')
            .expect(200)
            .end();

        expect(res.body).to.deep.equal({
            code: 403,
            fatal: 'Application should locate on yandex-team.ru domain'
        });
    });

    it('should return error when client is not idm', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'takeout' });

        const res = yield request
            .post('/v1/idm/add-role/')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .set('Host', 'expert-admin.yandex-team.ru')
            .expect(200)
            .end();

        expect(res.body).to.deep.equal({
            code: 403,
            fatal: 'Client has no access'
        });
    });

    it('should return error when role not found', function *() {
        yield adminsFactory.create({ login: 'anyok' });
        yield tvmClientsFactory.create({ clientId: 1234, name: 'idm' });

        const res = yield request
            .post('/v1/idm/add-role/')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .set('Content-Type', 'application/x-www-form-urlencoded')
            .set('Host', 'expert-admin.yandex-team.ru')
            .send('login=anyok&role={"expert":"developer"}')
            .expect(200)
            .end();

        expect(res.body).to.deep.equal({ code: 404, fatal: 'Role not found' });
    });
});
