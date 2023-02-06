const { expect } = require('chai');
const nock = require('nock');

const api = require('api');
const request = require('co-supertest').agent(api.callback());

const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const tvmClientsFactory = require('tests/factory/tvmClientsFactory');

const dbHelper = require('tests/helpers/clear');
const nockTvm = require('tests/helpers/nockTvm');

describe('Idm get all roles controller', () => {
    beforeEach(function *() {
        yield dbHelper.clear();

        nockTvm.checkTicket({ src: 1234 });
    });

    afterEach(nock.cleanAll);

    it('should success return all roles', function *() {
        const admin = { login: 'mokosha' };
        const role = { code: 'developer', title: 'Developer' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
        yield tvmClientsFactory.create({ clientId: 1234, name: 'idm' });

        const res = yield request
            .get('/v1/idm/get-all-roles/')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .set('Host', 'expert-admin.yandex-team.ru')
            .expect(200)
            .end();

        const expected = {
            code: 0,
            users: [
                {
                    login: 'mokosha',
                    roles: [{ expert: 'developer' }]
                }
            ]
        };

        expect(res.body).to.deep.equal(expected);
    });

    it('should return error when domain is not yandex-team.ru', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'idm' });

        const res = yield request
            .get('/v1/idm/get-all-roles/')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .set('Host', 'expert-admin.yandex.ru')
            .expect(200)
            .end();

        const expected = {
            code: 403,
            fatal: 'Application should locate on yandex-team.ru domain'
        };

        expect(res.body).to.deep.equal(expected);
    });

    it('should return error when client is not idm', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'someone' });

        const res = yield request
            .get('/v1/idm/get-all-roles/')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .set('Host', 'expert-admin.yandex-team.ru')
            .expect(200)
            .end();

        const expected = {
            code: 403,
            fatal: 'Client has no access'
        };

        expect(res.body).to.deep.equal(expected);
    });
});
