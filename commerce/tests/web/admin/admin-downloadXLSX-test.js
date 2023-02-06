require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');

const Excel = require('models/excel');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const dbHelper = require('tests/helpers/clear');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const answersFactory = require('tests/factory/answersFactory');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const trialTemplateToSectionsFactory = require('tests/factory/trialTemplateToSectionsFactory');
const trialTemplateAllowedFailsFactory = require('tests/factory/trialTemplateAllowedFailsFactory');

describe('Admin download XLSX controller', () => {
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

    function *prepareDB() {
        const trialTemplate = { id: 2 };

        yield trialTemplatesFactory.createWithRelations(trialTemplate);

        const section = { id: 7 };

        let question = { id: 3, version: 2, text: 'first question', categoryId: 1 };

        yield answersFactory.createWithRelations({ id: 5, active: 1 }, { question, section });
        yield answersFactory.createWithRelations({ id: 6, active: 1 }, { question, section });

        question = { id: 4, version: 2, text: 'second question', categoryId: 1 };
        yield answersFactory.createWithRelations({ id: 7, active: 1 }, { question, section });
        yield answersFactory.createWithRelations({ id: 8, active: 1 }, { question, section });

        yield trialTemplateToSectionsFactory.createWithRelations(
            { id: 5, quantity: 2, categoryId: 1 },
            { trialTemplate, section }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { id: 6, allowedFails: 1 },
            { trialTemplate, section }
        );
    }

    it('should success download xlsx', function *() {
        yield prepareDB();
        yield createAdmin();

        const res = yield request
            .get('/v1/admin/downloadXLSX/2')
            .set('Cookie', ['Session_id=user_session_id'])
            .parse(require('tests/helpers/binaryParser'))
            .expect(200)
            .end();

        const excel = Excel.tryLoad(res.body);

        expect(res.type).to.equal('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        expect(excel.worksheet['!ref']).to.equal('A1:N6');
        expect(excel.getValue('examId', 3)).to.equal(2);
    });

    it('should throw 400 when exam id is invalid', function *() {
        yield createAdmin();

        yield request
            .get('/v1/admin/downloadXLSX/abc')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect({
                message: 'Exam id is invalid',
                examId: 'abc',
                internalCode: '400_EII'
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .get('/v1/admin/downloadXLSX/2')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no editor access', function *() {
        yield request
            .get('/v1/admin/downloadXLSX/2')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User has no editor access',
                internalCode: '403_UEA'
            })
            .end();
    });
});
