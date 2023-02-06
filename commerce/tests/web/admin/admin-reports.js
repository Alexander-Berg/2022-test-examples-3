const { expect } = require('chai');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const BBHelper = require('tests/helpers/blackbox');
const nockBlackbox = BBHelper.nockIntBlackbox;
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');

describe('Admin reports controller', () => {
    before(() => {
        nockBlackbox({
            response: {
                uid: { value: '1234567890' },
                login: 'm-smirnov'
            }
        });
    });

    beforeEach(require('tests/helpers/clear').clear);

    after(() => BBHelper.cleanAll());

    it('should return all available reports', function *() {
        const admin = { uid: 1234567890 };
        const role = { code: 'analyst' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });

        const res = yield request
            .get('/v1/admin/reports')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .end();

        expect(res.body).to.be.an.instanceof(Array);
        expect(res.body.length).to.equal(7);
        expect(res.body[0]).to.deep.equal({
            type: 'certificate',
            description: 'Получение данных по номеру сертификата',
            fields: [{ name: 'certId', type: 'number', required: true }]
        });
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .get('/v1/admin/reports')
            .expect(401)
            .expect({ internalCode: '401_UNA', message: 'User not authorized' })
            .end();
    });
});
