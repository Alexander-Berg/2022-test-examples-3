require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const db = require('db/postgres');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const dbHelper = require('tests/helpers/clear');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const answersFactory = require('tests/factory/answersFactory');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const trialTemplateAllowedFailsFactory = require('tests/factory/trialTemplateAllowedFailsFactory');
const trialTemplateToSectionsFactory = require('tests/factory/trialTemplateToSectionsFactory');
const wrongDraft = require('tests/models/data/json/wrongDraft.json');

describe('Admin load data controller', () => {
    const data = require('tests/models/data/json/base.json');

    before(() => {
        nockBlackbox({ response: { uid: { value: '1234567890' }, login: 'anyok' } });
    });

    after(() => {
        require('nock').cleanAll();
    });

    beforeEach(function *() {
        yield dbHelper.clear();

        const trialTemplate = { id: 2 };

        yield trialTemplatesFactory.createWithRelations(
            trialTemplate,
            { service: { id: 3 } }
        );

        const category = { id: 1 };
        const section = { id: 7, serviceId: 3, code: 'movie', title: 'old title' };

        // First question
        let question = { id: 3, version: 5, text: 'old text' };

        yield answersFactory.createWithRelations({ id: 5 }, { question, section, category });
        yield answersFactory.createWithRelations({ id: 6 }, { question, section, category });
        yield answersFactory.createWithRelations({ id: 7 }, { question, section, category });

        // Second question
        question = { id: 4, version: 1, text: 'Second question from section2 and category1' };
        yield answersFactory.createWithRelations({ id: 8 }, { question, section, category });
        yield answersFactory.createWithRelations({ id: 9 }, { question, section, category });

        // Third question
        question = { id: 5, version: 3, text: 'Not changed text' };
        yield answersFactory.createWithRelations({
            id: 10,
            active: 1,
            text: 'correct_7',
            correct: 1
        }, { question, section, category });
        yield answersFactory.createWithRelations({
            id: 11,
            active: 1,
            text: 'incorrect_7',
            correct: 0
        }, { question, section, category });

        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { id: 2, allowedFails: 2 },
            { trialTemplate, section }
        );
        yield trialTemplateToSectionsFactory.createWithRelations(
            { id: 2, quantity: 2 },
            { trialTemplate, section, category }
        );
    });

    function *createAdmin() {
        const role = { code: 'admin' };
        const admin = { uid: 1234567890, login: 'anyok' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should success load data', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/loadData')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ examId: 2, draft: data })
            .expect(204)
            .end();

        expect(yield db.Section.count()).to.equal(2);
        expect(yield db.Category.count()).to.equal(2);
        expect(yield db.TrialTemplateAllowedFails.count()).to.equal(2);
        expect(yield db.TrialTemplateToSection.count()).to.equal(3);
        expect(yield db.Question.count()).to.equal(8);
        expect(yield db.Answer.count()).to.equal(24);
    });

    describe('loadData fail', () => {
        before(() => {
            data.answers[19].questionId.text = 'Wrong text';
        });

        after(() => {
            data.answers[19].questionId.text = 'Second question from section2 and category1';
        });

        it('should throw 400 when examId is not number', function *() {
            yield createAdmin();

            yield request
                .post('/v1/admin/loadData')
                .set('Cookie', ['Session_id=user_session_id'])
                .send({ examId: 'hello' })
                .expect(400)
                .expect({
                    message: 'Exam id is invalid',
                    internalCode: '400_EII',
                    examId: 'hello'
                })
                .end();
        });

        it('should throw 400 when draft check by schema failed', function *() {
            yield createAdmin();

            yield request
                .post('/v1/admin/loadData')
                .set('Cookie', ['Session_id=user_session_id'])
                .send({ examId: 1, draft: wrongDraft })
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

        it('should throw 400 when exam ids not match', function *() {
            yield createAdmin();
            yield trialTemplatesFactory.createWithRelations({ id: 1 });

            yield request
                .post('/v1/admin/loadData')
                .set('Cookie', ['Session_id=user_session_id'])
                .send({ examId: 1, draft: data })
                .expect(400)
                .expect({
                    message: 'Exam ids not match',
                    internalCode: '400_ENM',
                    examId: 1,
                    trialTemplateId: 2
                })
                .end();
        });

        it('should throw 500 when json contain wrong data and rollback all changes', function *() {
            yield createAdmin();

            yield request
                .post('/v1/admin/loadData')
                .set('Cookie', ['Session_id=user_session_id'])
                .send({ examId: 2, draft: data })
                .expect(500)
                .expect({
                    message: 'Internal Server Error',
                    internalCode: '500_NMC'
                })
                .end();

            const actualQuestion = yield db.Question.findAll({
                where: { id: 3 },
                attributes: ['id', 'version', 'text']
            });

            expect(actualQuestion.length).to.equal(1);
            expect(actualQuestion[0].toJSON()).to.deep.equal({
                id: 3,
                version: 5,
                text: 'old text'
            });
        });
    });

    it('should throw 404 when answer not found', function *() {
        yield createAdmin();
        yield db.Answer.destroy({ where: {} });

        yield request
            .post('/v1/admin/loadData')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ examId: 2, draft: data })
            .expect(404)
            .expect({
                message: 'Answers not found',
                answerIds: [5, 6, 7],
                internalCode: '404_ANF'
            })
            .end();
    });

    it('should throw 404 when question not found', function *() {
        yield createAdmin();
        yield db.Answer.destroy({ where: {} });
        yield db.Question.destroy({ where: {} });

        yield request
            .post('/v1/admin/loadData')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ examId: 2, draft: data })
            .expect(404)
            .expect({
                message: 'Question not found',
                id: 3,
                internalCode: '404_QNF'
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/admin/loadData')
            .send({ examId: 2, draft: data })
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user not admin', function *() {
        yield request
            .post('/v1/admin/loadData')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ examId: 2, draft: data })
            .expect(403)
            .expect({
                message: 'User is not admin',
                internalCode: '403_NAD'
            })
            .end();
    });
});
