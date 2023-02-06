require('co-mocha');

const config = require('yandex-config');
const api = require('api');
const request = require('co-supertest').agent(api.callback());

const { expect } = require('chai');
const ip = require('ip');
const nock = require('nock');

const dbHelper = require('tests/helpers/clear');
const blackboxHelper = require('tests/helpers/blackbox');

const certificatesFactory = require('tests/factory/certificatesFactory');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const { Certificate } = require('db/postgres');

describe('Certificate nullify controller', () => {
    before(() => {
        blackboxHelper.nockIntBlackbox({
            response: {
                uid: { value: '1234567890' }
            }
        });

        blackboxHelper.nockExtSeveralUids({
            uid: 111,
            userip: ip.address(),
            response: {
                users: [
                    {
                        uid: { value: 111 },
                        'address-list': [
                            { address: 'example@yandex.ru' }
                        ]
                    }
                ]
            }
        });

        nock(config.sender.host)
            .post(/\/api\/0\/sales\/transactional\/[A-Z0-9-]+\/send/)
            .query(true)
            .times(Infinity)
            .reply(200, {});
    });

    after(nock.cleanAll);

    beforeEach(dbHelper.clear);

    function *createAdmin() {
        const admin = { uid: 1234567890 };
        const role = { code: 'admin' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should nullify certificates', function *() {
        yield createAdmin();

        const now = Date.now();
        const trialTemplate = { id: 12, language: 0 };
        const otherTrialTemplate = { id: 13, language: 1 };
        const service = { id: 2, code: 'market' };
        const otherService = { id: 3, code: 'cpm' };

        yield certificatesFactory.createWithRelations(
            { id: 123, active: 1, deactivateReason: null, deactivateDate: null },
            { user: { id: 1, uid: 111 }, trialTemplate, service }
        );
        yield certificatesFactory.createWithRelations(
            { id: 456, active: 1, deactivateReason: null, deactivateDate: null },
            {
                user: { id: 2, uid: 222 },
                trialTemplate: otherTrialTemplate,
                service: otherService
            }
        );

        yield request
            .post('/v1/certificates/nullify')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ certIds: [123] })
            .expect(204)
            .end();

        const actual = yield Certificate.findAll({
            attributes: ['active', 'deactivateReason', 'deactivateDate'],
            order: [['id']],
            raw: true
        });

        expect(actual.length).to.equal(2);

        expect(actual[0].active).to.equal(0);
        expect(actual[0].deactivateReason).to.equal('rules');
        expect(actual[0].deactivateDate).to.be.at.least(now);

        expect(actual[1].active).to.equal(1);
        expect(actual[1].deactivateReason).to.be.null;
        expect(actual[1].deactivateDate).to.be.null;
    });

    it('should return 400 when certIds is not array', function *() {
        yield createAdmin();

        yield request
            .post('/v1/certificates/nullify')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ certIds: 123 })
            .expect(400, {
                internalCode: '400_CSA',
                message: 'certIds is required and should be array',
                certIds: 123
            })
            .end();
    });

    it('should return 400 when certificate id is invalid', function *() {
        yield createAdmin();

        yield request
            .post('/v1/certificates/nullify')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ certIds: [123, 'incorrect'] })
            .expect(400, {
                internalCode: '400_CII',
                message: 'Certificate id is invalid',
                certId: 'incorrect'
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/certificates/nullify')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no support access', function *() {
        yield request
            .post('/v1/certificates/nullify')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ certIds: [123] })
            .expect(403, {
                message: 'User has no support access',
                internalCode: '403_UNS'
            })
            .end();
    });
});
