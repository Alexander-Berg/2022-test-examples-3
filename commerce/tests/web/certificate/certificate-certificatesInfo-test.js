const api = require('api');
const { expect } = require('chai');
const nock = require('nock');
const request = require('co-supertest').agent(api.callback());

const dbHelper = require('tests/helpers/clear');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const certificatesFactory = require('tests/factory/certificatesFactory');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;

describe('Certificate certificatesInfo controller', () => {
    beforeEach(function *() {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });

        yield dbHelper.clear();
    });

    afterEach(nock.cleanAll);

    function *createSupport() {
        const admin = { uid: 1234567890 };
        const role = { code: 'support' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should return certificates info', function *() {
        yield createSupport();

        const confirmedDate = new Date(1, 1, 1);
        const dueDate = new Date(2, 2, 2);

        yield certificatesFactory.createWithRelations({
            id: 1,
            active: 1,
            confirmedDate,
            dueDate
        }, {
            trial: { id: 123, nullified: 0 },
            trialTemplate: { id: 1, slug: 'direct' },
            user: { id: 11, uid: 11111, login: 'user' }
        });

        yield certificatesFactory.createWithRelations({
            id: 2,
            active: 0,
            confirmedDate: new Date(3, 3, 3),
            dueDate: new Date(4, 4, 4)
        }, {
            trial: { id: 456, nullified: 1 },
            trialTemplate: { id: 2, slug: 'cpm' },
            user: { id: 22, uid: 22222, login: 'second' }
        });

        const res = yield request
            .post('/v1/certificates/info')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ certIds: [1] })
            .expect(200);

        expect(res.body).to.deep.equal({
            certificatesInfo: [
                {
                    id: 1,
                    isActive: true,
                    confirmedDate: confirmedDate.toISOString(),
                    dueDate: dueDate.toISOString(),
                    trialId: 123,
                    isTrialNullified: false,
                    login: 'user',
                    examSlug: 'direct'
                }
            ]
        });
    });

    it('should return 400 when certificates` ids is not array', function *() {
        yield createSupport();

        yield request
            .post('/v1/certificates/info')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ certIds: 1 })
            .expect(400, {
                internalCode: '400_CSA',
                message: 'certIds is required and should be array',
                certIds: 1
            });
    });

    it('should return 400 when certificate id is invalid', function *() {
        yield createSupport();

        yield request
            .post('/v1/certificates/info')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ certIds: ['not-a-number'] })
            .expect(400, {
                internalCode: '400_CII',
                message: 'Certificate id is invalid',
                certId: 'not-a-number'
            });
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/certificates/info')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no support access', function *() {
        yield request
            .post('/v1/certificates/info')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User has no support access',
                internalCode: '403_UNS'
            })
            .end();
    });
});
