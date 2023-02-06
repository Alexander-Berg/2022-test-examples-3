const { expect } = require('chai');
const nock = require('nock');

const api = require('api');
const request = require('co-supertest').agent(api.callback());

const rolesFactory = require('tests/factory/rolesFactory');
const tvmClientsFactory = require('tests/factory/tvmClientsFactory');

const dbHelper = require('tests/helpers/clear');
const nockTvm = require('tests/helpers/nockTvm');

describe('Idm get info controller', () => {
    beforeEach(function *() {
        yield dbHelper.clear();

        nockTvm.checkTicket({ src: 1234 });
    });

    afterEach(nock.cleanAll);

    it('should return roles', function *() {
        yield [
            { code: 'developer', title: 'Developer' },
            { code: 'admin', title: 'Administrator' }
        ].map(rolesFactory.create);

        yield tvmClientsFactory.create({ clientId: 1234, name: 'idm' });

        const res = yield request
            .get('/v1/idm/info/')
            .set('x-ya-service-ticket', 'ticket')
            .set('force-tvm-check', 1)
            .set('Host', 'expert-admin.yandex-team.ru')
            .expect(200)
            .end();

        const expected = {
            code: 0,
            roles: {
                slug: 'expert',
                name: 'Эксперт',
                values: {
                    developer: 'Developer',
                    admin: 'Administrator'
                }
            }
        };

        expect(res.body).to.deep.equal(expected);
    });

    it('should return error when host is not yandex-team.ru', function *() {
        yield tvmClientsFactory.create({ clientId: 1234, name: 'idm' });

        const res = yield request
            .get('/v1/idm/info/')
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
        yield tvmClientsFactory.create({ clientId: 1234, name: 'who' });

        const res = yield request
            .get('/v1/idm/info/')
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
