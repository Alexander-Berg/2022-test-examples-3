require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const nock = require('nock');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const dbHelper = require('tests/helpers/clear');

const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const draftsFactory = require('tests/factory/draftsFactory');

const correctDraft = require('tests/models/data/json/correctDraft.json');

describe('Exam find draft controller', () => {
    before(() => {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });
    });

    after(nock.cleanAll);

    beforeEach(dbHelper.clear);

    function *createAdmin() {
        const role = { code: 'admin' };
        const admin = { uid: 1234567890 };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should success find draft', function *() {
        yield createAdmin();

        yield draftsFactory.createWithRelations(
            { exam: correctDraft, status: 'on_moderation' },
            { trialTemplate: { id: 2 } }
        );

        yield request
            .get('/v1/exam/2/draft')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({
                exam: correctDraft,
                status: 'on_moderation'
            })
            .end();
    });

    it('should throw 400 when exam identity is invalid', function *() {
        yield createAdmin();

        yield request
            .get('/v1/exam/inv@lid/draft')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect({
                message: 'Exam id is invalid',
                internalCode: '400_EII',
                examId: 'inv@lid'
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .get('/v1/exam/2/draft')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no editor access', function *() {
        yield request
            .get('/v1/exam/2/draft')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User has no editor access',
                internalCode: '403_UEA'
            })
            .end();
    });
});
