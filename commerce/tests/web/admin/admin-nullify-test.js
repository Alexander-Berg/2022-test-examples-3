const api = require('api');
const { expect } = require('chai');
const nock = require('nock');
const request = require('co-supertest').agent(api.callback());

const dbHelper = require('tests/helpers/clear');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const { Trial } = require('db/postgres');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;

describe('Admin nullify controller', () => {
    beforeEach(function *() {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });

        yield dbHelper.clear();
    });

    afterEach(nock.cleanAll);

    function *createAdmin() {
        const admin = { uid: 1234567890 };
        const role = { code: 'admin' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should nullify attempts', function *() {
        const trial = { id: 123, nullified: 0 };
        const otherTrial = { id: 124, nullified: 0 };

        yield trialsFactory.createWithRelations(trial, { });
        yield trialsFactory.createWithRelations(otherTrial, { });

        yield createAdmin();
        yield request
            .post('/v1/admin/attempts/nullify')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptIds: [123, 124] })
            .expect(200);

        const attempt = yield Trial.findAll({
            attributes: ['nullified'],
            order: ['id'],
            raw: true
        });

        expect(attempt[0].nullified).to.be.equal(1);
        expect(attempt[1].nullified).to.be.equal(1);
    });

    it('should return not found attempts', function *() {
        yield createAdmin();
        const res = yield request
            .post('/v1/admin/attempts/nullify')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptIds: [123, 124] })
            .expect(200);

        const actual = res.body;

        expect(actual).to.deep.equal({ notFoundAttempts: [123, 124] });
    });

    it('should return 400 when attempt ids is not array', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/attempts/nullify')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptIds: 1 })
            .expect(400, {
                internalCode: '400_ASA',
                message: 'attemptIds should be array',
                attemptIds: 1
            });
    });

    it('should return 400 when attempt id is invalid', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/attempts/nullify')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptIds: ['not-a-number'] })
            .expect(400, {
                internalCode: '400_AII',
                message: 'Attempt id is invalid',
                attemptId: 'not-a-number'
            });
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/admin/attempts/nullify')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no support access', function *() {
        yield request
            .post('/v1/admin/attempts/nullify')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User has no support access',
                internalCode: '403_UNS'
            })
            .end();
    });
});
