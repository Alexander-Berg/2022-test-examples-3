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
const draftsFactory = require('tests/factory/draftsFactory');

const correctDraft = require('tests/models/data/json/correctDraft.json');

describe('Exam ignore draft controller', () => {
    before(() => {
        nockBlackbox({
            response:
                {
                    uid: { value: '1234567890' },
                    login: 'koto'
                }
        });
    });

    after(nock.cleanAll);

    beforeEach(function *() {
        yield dbHelper.clear();

        yield trialTemplatesFactory.createWithRelations({ id: 2, slug: 'test' });
    });

    function *createEditor() {
        const role = { code: 'editor' };
        const admin = { id: 7, uid: 1234567890, login: 'koto' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should create new record with status `ignored`', function *() {
        yield createEditor();
        yield draftsFactory.createWithRelations(
            { exam: correctDraft, saveDate: new Date(2017, 1, 1), status: 'saved' },
            { trialTemplate: { id: 2 }, admin: { id: 3, login: 'rinka' } }
        );

        yield request
            .delete('/v1/exam/2/draft')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(204)
            .end();

        const actualSaved = yield Draft.findAll({
            attributes: ['adminId', 'trialTemplateId', 'exam', 'status'],
            order: [['saveDate']],
            raw: true
        });

        const expected = [
            {
                adminId: 3,
                trialTemplateId: 2,
                exam: correctDraft,
                status: 'saved'
            },
            {
                adminId: 7,
                trialTemplateId: 2,
                exam: correctDraft,
                status: 'ignored'
            }
        ];

        expect(actualSaved).to.deep.equal(expected);
    });

    it('should throw 400 when exam identity is invalid', function *() {
        yield createEditor();

        yield request
            .delete('/v1/exam/inv@lid/draft')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect({
                message: 'Exam identity is invalid',
                internalCode: '400_EII',
                identity: 'inv@lid'
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .delete('/v1/exam/2/draft')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no editor access', function *() {
        yield request
            .delete('/v1/exam/2/draft')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User has no editor access',
                internalCode: '403_UEA'
            })
            .end();
    });

    it('should throw 404 when test not found', function *() {
        yield createEditor();

        yield request
            .delete('/v1/exam/3/draft')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect({
                message: 'Test not found',
                internalCode: '404_TNF'
            })
            .end();
    });

    it('should throw 404 when draft not found', function *() {
        yield createEditor();
        yield trialTemplatesFactory.createWithRelations({ id: 3 });

        yield request
            .delete('/v1/exam/3/draft')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect({
                message: 'Draft not found',
                internalCode: '404_DNF',
                examId: 3
            })
            .end();
    });
});
