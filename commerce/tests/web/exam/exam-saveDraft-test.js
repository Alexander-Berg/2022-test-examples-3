require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const nock = require('nock');

const { Draft } = require('db/postgres');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const dbHelper = require('tests/helpers/clear');

const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');

const correctDraft = require('tests/models/data/json/correctDraft.json');
const wrongDraft = require('tests/models/data/json/wrongDraft.json');

describe('Exam save draft controller', () => {
    before(() => {
        nockBlackbox({
            response:
                {
                    uid: { value: '1234567890' },
                    login: 'sinseveria'
                }
        });
    });

    after(nock.cleanAll);

    beforeEach(function *() {
        yield dbHelper.clear();

        yield trialTemplatesFactory.createWithRelations({ id: 2, slug: 'test' });
    });

    function *createAdmin() {
        const role = { code: 'admin' };
        const admin = { id: 7, uid: 1234567890, login: 'sinseveria' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should success save draft', function *() {
        yield createAdmin();

        yield request
            .post('/v1/exam/2/draft')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ draft: correctDraft })
            .expect(204)
            .end();

        const actualSaved = yield Draft.findAll({
            attributes: ['adminId', 'trialTemplateId', 'exam'],
            raw: true
        });

        const expected = {
            adminId: 7,
            trialTemplateId: 2,
            exam: correctDraft
        };

        expect(actualSaved).to.deep.equal([expected]);
    });

    it('should throw 400 when exam identity is invalid', function *() {
        yield createAdmin();

        yield request
            .post('/v1/exam/inv@lid/draft')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect({
                message: 'Exam identity is invalid',
                internalCode: '400_EII',
                identity: 'inv@lid'
            })
            .end();
    });

    it('should throw 400 when check by schema failed', function *() {
        yield createAdmin();

        yield request
            .post('/v1/exam/2/draft')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ draft: wrongDraft })
            .expect(400)
            .expect(({ body }) => {
                expect(body.message).to.equal('Draft check by schema failed');
                expect(body.internalCode).to.equal('400_CSF');
                expect(body.errors.length).to.equal(1);
                expect(body.errors[0].dataPath).to.equal(
                    '.answers[2].questionId.sectionId.serviceId'
                );
                expect(body.errors[0].message).to.equal('should be >= 0');
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/exam/2/draft')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no editor access', function *() {
        yield request
            .post('/v1/exam/2/draft')
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
            .post('/v1/exam/3/draft')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect({
                message: 'Test not found',
                internalCode: '404_TNF'
            })
            .end();
    });
});
