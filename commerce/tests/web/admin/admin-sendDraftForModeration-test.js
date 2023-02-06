require('co-mocha');

let api = require('api');
let request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const mockery = require('mockery');
const { Draft } = require('db/postgres');

const mockMailer = require('tests/helpers/mailer');
const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const dbHelper = require('tests/helpers/clear');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const correctDraft = require('tests/models/data/json/correctDraft.json');
const wrongDraft = require('tests/models/data/json/wrongDraft.json');

describe('Admin sendDraftForModeration controller', () => {
    before(() => {
        mockMailer();

        api = require('api');
        request = require('co-supertest').agent(api.callback());

        nockBlackbox({ response: { uid: { value: '1234567890' }, login: 'anyok' } });
    });

    after(() => {
        mockery.disable();
        mockery.deregisterAll();
        require('nock').cleanAll();
    });

    beforeEach(function *() {
        yield dbHelper.clear();

        yield trialTemplatesFactory.createWithRelations({ id: 2, slug: 'hello' });
    });

    function *createAdmin() {
        const role = { code: 'admin' };
        const admin = { uid: 1234567890, login: 'anyok' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should send draft for moderation', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/moderation/exam')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ draft: correctDraft, examIdentity: 'hello' })
            .expect(204)
            .end();

        const drafts = yield Draft.findAll({
            attributes: ['status', 'exam'],
            raw: true
        });

        expect(drafts).to.deep.equal([{
            status: 'on_moderation',
            exam: correctDraft
        }]);
    });

    it('should throw 400 when exam id is invalid', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/moderation/exam')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ draft: correctDraft, examIdentity: 'notValidExam$$$' })
            .expect(400)
            .expect({
                message: 'Exam identity is invalid',
                internalCode: '400_EII',
                identity: 'notValidExam$$$'
            })
            .end();
    });

    it('should throw 400 when draft check by schema failed', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/moderation/exam')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ draft: wrongDraft, examIdentity: 'hello' })
            .expect(400)
            .expect(({ body }) => {
                expect(body.message).to.equal('Draft check by schema failed');
                expect(body.internalCode).to.equal('400_CSF');
                expect(body.errors.length).to.equal(1);
                expect(body.errors[0].dataPath).to.equal('.answers[2].questionId.sectionId.serviceId');
                expect(body.errors[0].message).to.equal('should be >= 0');
            })
            .end();
    });

    it('should throw 404 when test not found', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/moderation/exam')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ draft: correctDraft, examIdentity: 'notexist' })
            .expect(404)
            .expect({
                message: 'Test not found',
                internalCode: '404_TNF'
            });
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/admin/moderation/exam')
            .send({ draft: correctDraft, examIdentity: 'hello' })
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no editor access', function *() {
        yield request
            .post('/v1/admin/moderation/exam')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ draft: correctDraft, examIdentity: 'hello' })
            .expect(403)
            .expect({
                message: 'User has no editor access',
                internalCode: '403_UEA'
            })
            .end();
    });
});
