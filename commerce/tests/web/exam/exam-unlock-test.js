const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const nock = require('nock');

const { Lock } = require('db/postgres');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const dbHelper = require('tests/helpers/clear');

const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const locksFactory = require('tests/factory/locksFactory');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');

describe('Unlock exam controller', () => {
    before(() => {
        nockBlackbox({
            response:
                {
                    uid: { value: '1234567890' },
                    login: 'dotokoto'
                }
        });
    });

    after(nock.cleanAll);

    beforeEach(dbHelper.clear);

    function *createAdmin() {
        const role = { code: 'admin' };
        const admin = { id: 7, uid: 1234567890, login: 'dotokoto' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should success unlock exam', function *() {
        yield createAdmin();
        yield locksFactory.createWithRelations(
            { id: 3 },
            { trialTemplate: { id: 2 } }
        );

        yield request
            .post('/v1/exam/2/unlock')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(204)
            .end();

        const actual = yield Lock.findOne({
            where: { id: 3 },
            attributes: ['unlockDate']
        });

        expect(actual.unlockDate).to.not.be.null;
    });

    it('should throw 400 when exam identity is invalid', function *() {
        yield createAdmin();

        yield request
            .post('/v1/exam/inv@lid/unlock')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect({
                message: 'Exam identity is invalid',
                internalCode: '400_EII',
                identity: 'inv@lid'
            })
            .end();
    });

    it('should throw 400 when exam is not locked', function *() {
        yield createAdmin();

        yield trialTemplatesFactory.createWithRelations({ id: 2 });

        yield request
            .post('/v1/exam/2/unlock')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect({
                message: 'Exam is not locked',
                internalCode: '400_ENL',
                examId: 2
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/exam/2/unlock')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no editor access', function *() {
        yield request
            .post('/v1/exam/2/unlock')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User has no editor access',
                internalCode: '403_UEA'
            })
            .end();
    });

    it('should throw 404 when test not found', function *() {
        yield createAdmin();

        yield request
            .post('/v1/exam/3/unlock')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect({
                message: 'Test not found',
                internalCode: '404_TNF'
            })
            .end();
    });
});
