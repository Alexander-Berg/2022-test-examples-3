require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const dbHelper = require('tests/helpers/clear');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const answersFactory = require('tests/factory/answersFactory');
const questionsFactory = require('tests/factory/questionsFactory');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const trialTemplateToSections = require('tests/factory/trialTemplateToSectionsFactory');

describe('Admin parse XLSX controller', () => {
    before(() => {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });
    });

    after(() => {
        require('nock').cleanAll();
    });

    beforeEach(function *() {
        yield dbHelper.clear();
    });

    function *createAdmin() {
        const role = { code: 'admin' };
        const admin = { uid: 1234567890 };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    const section = { id: 7 };

    it('should success parse xlsx', function *() {
        yield trialTemplatesFactory.createWithRelations({ id: 2 });

        let question = { id: 3, version: 2, text: 'old text' };

        yield answersFactory.createWithRelations({ id: 5 }, { question, section });
        yield answersFactory.createWithRelations({ id: 6 }, { question, section });
        yield answersFactory.createWithRelations({ id: 7 }, { question, section });

        question = { id: 4, version: 2, text: 'old text' };
        yield answersFactory.createWithRelations({ id: 8 }, { question, section });
        yield answersFactory.createWithRelations({ id: 9 }, { question, section });

        yield createAdmin();

        const res = yield request
            .post('/v1/admin/parseXLSX')
            .set('Cookie', ['Session_id=user_session_id'])
            .attach('base.xlsx', 'tests/models/data/xlsx/base.xlsx')
            .expect(200)
            .end();
        const actual = res.body;

        expect(actual).not.be.empty;
        expect(actual.sections).to.have.length(2);
        expect(actual.categories).to.have.length(2);
        expect(actual.trialTemplateAllowedFails).to.have.length(2);
        expect(actual.trialTemplateToSections).to.have.length(3);
        expect(actual.questions).to.have.length(6);
        expect(actual.answers).to.have.length(20);
    });

    it('should throw 400 when file is invalid', function *() {
        yield createAdmin();
        yield request
            .post('/v1/admin/parseXLSX')
            .set('Cookie', ['Session_id=user_session_id'])
            .attach('base.xlsx', 'index.js')
            .expect(400)
            .expect({
                message: 'Parse failed',
                details: 'Unsupported file 114',
                internalCode: '400_PFD'
            })
            .end();
    });

    it('should throw 400 when wrong count of correct answers', function *() {
        const otherSection = { id: 21, code: 'lit' };
        const question = { id: 1 };

        yield trialTemplateToSections.createWithRelations({}, { trialTemplate: { id: 2 }, section: otherSection });
        yield answersFactory.createWithRelations({ id: 11 }, { question, section: otherSection });
        yield answersFactory.createWithRelations({ id: 12 }, { question, section: otherSection });
        yield createAdmin();

        yield request
            .post('/v1/admin/parseXLSX')
            .set('Cookie', ['Session_id=user_session_id'])
            .attach('wrongCorrectCount.xlsx', 'tests/models/data/xlsx/wrongCorrectCount.xlsx')
            .expect(400)
            .expect({
                message: 'Wrong number of correct answers',
                internalCode: '400_WCA',
                questionText: 'Third question from section1 and category1'
            })
            .end();
    });

    it('should throw 400 when column name is invalid', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/parseXLSX')
            .set('Cookie', ['Session_id=user_session_id'])
            .attach('wrongColumnName.xlsx', 'tests/models/data/xlsx/wrongColumnName.xlsx')
            .expect(400)
            .expect({
                message: 'Invalid column name',
                internalCode: '400_ICN',
                columnName: 'examId'
            })
            .end();
    });

    it('should throw 400 when value type is invalid', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/parseXLSX')
            .set('Cookie', ['Session_id=user_session_id'])
            .attach('wrongCellValueType.xlsx', 'tests/models/data/xlsx/wrongCellValueType.xlsx')
            .expect(400)
            .expect({
                message: 'Invalid value type',
                internalCode: '400_IVT',
                columnName: 'quantity',
                value: 'two',
                rowNumber: 3
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/admin/parseXLSX')
            .attach('base.xlsx', 'tests/models/data/xlsx/base.xlsx')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no editor access', function *() {
        yield request
            .post('/v1/admin/parseXLSX')
            .set('Cookie', ['Session_id=user_session_id'])
            .attach('base.xlsx', 'tests/models/data/xlsx/base.xlsx')
            .expect(403)
            .expect({
                message: 'User has no editor access',
                internalCode: '403_UEA'
            })
            .end();
    });

    it('should throw 404 when exam not found', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/parseXLSX')
            .set('Cookie', ['Session_id=user_session_id'])
            .attach('base.xlsx', 'tests/models/data/xlsx/base.xlsx')
            .expect(404)
            .expect({
                message: 'Exam not found',
                internalCode: '404_ENF',
                id: 2
            })
            .end();
    });

    it('should throw 404 when question not found', function *() {
        yield createAdmin();
        yield trialTemplatesFactory.createWithRelations({ id: 2 });

        yield request
            .post('/v1/admin/parseXLSX')
            .set('Cookie', ['Session_id=user_session_id'])
            .attach('base.xlsx', 'tests/models/data/xlsx/base.xlsx')
            .expect(404)
            .expect({
                message: 'Question not found',
                internalCode: '404_QNF',
                id: 3
            })
            .end();
    });

    it('should throw 404 when answer not found', function *() {
        yield createAdmin();
        yield trialTemplatesFactory.createWithRelations({ id: 2 });

        yield questionsFactory.createWithRelations({ id: 3 }, { section });
        yield questionsFactory.createWithRelations({ id: 4 }, { section });

        yield request
            .post('/v1/admin/parseXLSX')
            .set('Cookie', ['Session_id=user_session_id'])
            .attach('base.xlsx', 'tests/models/data/xlsx/base.xlsx')
            .expect(404)
            .expect({
                message: 'Answer not found',
                internalCode: '404_ANF',
                id: 5
            })
            .end();
    });
});
