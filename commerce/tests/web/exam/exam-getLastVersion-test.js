require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const nock = require('nock');
const { expect } = require('chai');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const dbHelper = require('tests/helpers/clear');

const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const answersFactory = require('tests/factory/answersFactory');
const categoriesFactory = require('tests/factory/categoriesFactory');
const draftsFactory = require('tests/factory/draftsFactory');
const trialTemplateToSectionsFactory = require('tests/factory/trialTemplateToSectionsFactory');
const trialTemplateAllowedFailsFactory = require('tests/factory/trialTemplateAllowedFailsFactory');

const correctDraft = require('tests/models/data/json/correctDraft.json');

describe('Exam getLastVersion controller', () => {
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

    it('should return draft when it is not published', function *() {
        yield createAdmin();

        yield draftsFactory.createWithRelations(
            { exam: correctDraft, status: 'on_moderation' },
            { trialTemplate: { id: 2 } }
        );

        yield request
            .get('/v1/exam/2/lastVersion')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect(correctDraft)
            .end();
    });

    it('should return exam version from production when draft does not exist', function *() {
        yield createAdmin();

        const trialTemplate = { id: 2 };
        const service = { id: 3 };
        const category = { id: 1, difficulty: 1, timeLimit: 10 };
        const section = { id: 4, code: 'math', title: 'mathematics' };
        const question = { id: 101, active: 1, text: 'question_101', type: 0, categoryId: 1 };

        yield categoriesFactory.create(category);

        yield answersFactory.createWithRelations(
            { id: 1000, text: 'correct_101', correct: 1, active: 1 },
            { question, section, service, trialTemplate }
        );

        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 1 },
            { section, trialTemplate, service }
        );

        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 2, categoryId: 1 },
            { section, trialTemplate, service }
        );

        const expectedSection = {
            serviceId: 3,
            code: 'math',
            title: 'mathematics'
        };
        const expectedQuestion = {
            id: 101,
            active: 1,
            sectionId: expectedSection,
            categoryId: category,
            text: 'question_101',
            type: 0
        };

        const expected = {
            sections: [expectedSection],
            categories: [category],
            trialTemplateAllowedFails: [
                {
                    trialTemplateId: 2,
                    sectionId: expectedSection,
                    allowedFails: 1
                }
            ],
            trialTemplateToSections: [
                {
                    trialTemplateId: 2,
                    sectionId: expectedSection,
                    categoryId: category,
                    quantity: 2
                }
            ],
            questions: [expectedQuestion],
            answers: [
                {
                    id: 1000,
                    questionId: expectedQuestion,
                    correct: 1,
                    text: 'correct_101',
                    active: 1
                }
            ]
        };

        yield request
            .get('/v1/exam/2/lastVersion')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect(expected)
            .end();
    });

    it('should return exam version from production when last draft is published', function *() {
        yield createAdmin();

        const trialTemplate = { id: 2 };

        yield draftsFactory.createWithRelations(
            { exam: correctDraft, status: 'published' },
            { trialTemplate }
        );

        const section = { id: 12, code: 'books', title: 'Books' };
        const service = { id: 7 };

        yield answersFactory.createWithRelations(
            { id: 1000, active: 1 },
            { question: { id: 12, active: 1 }, section, trialTemplate, service }
        );

        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 1 },
            { section, trialTemplate, service }
        );

        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 2, categoryId: 1 },
            { section, trialTemplate, service }
        );

        yield request
            .get('/v1/exam/2/lastVersion')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect(({ body: { sections } }) => {
                expect(sections).to.deep.equal([{
                    serviceId: 7,
                    code: 'books',
                    title: 'Books'
                }]);
            })
            .end();
    });

    it('should return exam version from production when last draft is ignored', function *() {
        yield createAdmin();

        const trialTemplate = { id: 2 };

        yield draftsFactory.createWithRelations(
            { exam: correctDraft, status: 'ignored' },
            { trialTemplate }
        );

        const section = { id: 12, code: 'books', title: 'Books' };
        const service = { id: 7 };

        yield answersFactory.createWithRelations(
            { id: 1000, active: 1 },
            { question: { id: 12, active: 1 }, section, trialTemplate, service }
        );

        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 1 },
            { section, trialTemplate, service }
        );

        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 2, categoryId: 1 },
            { section, trialTemplate, service }
        );

        yield request
            .get('/v1/exam/2/lastVersion')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect(({ body: { sections } }) => {
                expect(sections).to.deep.equal([{
                    serviceId: 7,
                    code: 'books',
                    title: 'Books'
                }]);
            })
            .end();
    });

    it('should throw 400 when exam identity is invalid', function *() {
        yield createAdmin();

        yield request
            .get('/v1/exam/inv@lid/lastVersion')
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
            .get('/v1/exam/2/lastVersion')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no editor access', function *() {
        yield request
            .get('/v1/exam/2/lastVersion')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User has no editor access',
                internalCode: '403_UEA'
            })
            .end();
    });

    it('should return 404 when exam not found', function *() {
        yield createAdmin();

        yield request
            .get('/v1/exam/2/lastVersion')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect('Content-Type', /json/)
            .expect({
                message: 'Test not found',
                internalCode: '404_TNF'
            })
            .end();
    });
});
