const { expect } = require('chai');
const api = require('api');
const request = require('co-supertest').agent(api.callback());
const ip = require('ip');
const _ = require('lodash');

const BBHelper = require('tests/helpers/blackbox');
const nockBlackbox = BBHelper.nockIntBlackbox;
const nockSeveralUids = BBHelper.nockExtSeveralUids;
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const certificatesFactory = require('tests/factory/certificatesFactory');
const trialToQuestionsFactory = require('tests/factory/trialToQuestionsFactory');
const questionsFactory = require('tests/factory/questionsFactory');
const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');
const bansFactory = require('tests/factory/bansFactory');
const usersFactory = require('tests/factory/usersFactory');

const Excel = require('models/excel');

describe('Admin report controller', () => {
    before(() => {
        nockBlackbox({
            response: {
                uid: { value: '1234567890' },
                login: 'm-smirnov'
            }
        });

        nockSeveralUids({
            uid: '1234,5678',
            userip: ip.address(),
            response: {
                users: [
                    {
                        uid: { value: 1234 },
                        'address-list': [
                            { address: 'email1@yandex.ru' }
                        ]
                    },
                    {
                        uid: { value: 5678 },
                        'address-list': [
                            { address: 'email2@yandex.ru' }
                        ]
                    }
                ]
            }
        });
    });

    beforeEach(require('tests/helpers/clear').clear);

    after(() => BBHelper.cleanAll());

    function *createAdmin(roleCode) {
        const admin = { uid: 1234567890 };
        const role = { code: roleCode };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should generate JSON report', function *() {
        yield createAdmin('analyst');
        yield certificatesFactory.createWithRelations({ id: 1 });

        const res = yield request
            .get('/v1/admin/report/certificate?certId=1')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .end();

        expect(res.body.certId).to.equal(1);
    });

    // EXPERTDEV-654: Проверить базу на наличие приватных данных
    it('should return email from blackbox in certificates report', function *() {
        yield createAdmin('analyst');

        const trialTemplate = { id: 1, slug: 'hello' };

        yield certificatesFactory.createWithRelations(
            { id: 4, confirmedDate: new Date(2017, 0, 20) },
            {
                user: { id: 2, uid: 1234 },
                trialTemplate,
                trial: { passed: 1, nullified: 0 }
            }
        );

        yield certificatesFactory.createWithRelations(
            { id: 5, confirmedDate: new Date(2017, 0, 25) },
            {
                user: { id: 3, uid: 5678 },
                trialTemplate,
                trial: { passed: 1, nullified: 0 }
            }
        );

        const from = new Date(2017, 0, 15).toISOString();
        const query = `from=${from}&slug=hello`;

        const res = yield request
            .get(`/v1/admin/report/certificates?${query}`)
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .end();

        expect(_.map(res.body, 'email')).to.deep.equal(['email1@yandex.ru', 'email2@yandex.ru']);
    });

    it('should parse query params', function *() {
        yield createAdmin('analyst');
        yield [
            { slug: 'direct', isProctoring: true },
            { slug: 'shim', isProctoring: false },
            { slug: 'hello', isProctoring: false }
        ].map((trialTemplate, i) => certificatesFactory.createWithRelations(
            { id: i + 1, active: 1 },
            { trialTemplate, trial: { id: i + 1, started: new Date(2017, 0, 20), nullified: 0, passed: 1 } }
        ));

        yield proctoringResponsesFactory.createWithRelations(
            { source: 'proctoring', verdict: 'correct' },
            { trial: { id: 1 } }
        );

        const from = new Date(2017, 0, 15).toISOString();
        const to = new Date(2017, 1, 1).toISOString();
        const query = `from=${from}&to=${to}&slug=direct&slug=hello`;

        const res = yield request
            .get(`/v1/admin/report/certificatesAggregation?${query}`)
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .end();

        const expected = [
            {
                from,
                to,
                slug: 'direct',
                passed: 1,
                total: 1,
                isProctoring: 1,
                correctAnswersCount: 1,
                failedAnswersCount: 0,
                sentToToloka: 0,
                passedWithCorrect: 1,
                passedWithFailed: 0,
                certificatesCount: 1
            },
            {
                from,
                to,
                slug: 'hello',
                passed: 1,
                total: 1,
                isProctoring: 0,
                correctAnswersCount: 0,
                failedAnswersCount: 0,
                sentToToloka: 0,
                passedWithCorrect: 0,
                passedWithFailed: 0,
                certificatesCount: 1
            }
        ];

        expect(res.body).to.deep.equal(expected);
    });

    it('should generate trial report', function *() {
        yield createAdmin('analyst');

        const trial = { id: 13 };
        const trialTemplate = { id: 2, title: 'Winter' };
        const firstSection = { id: 3, code: 'magic', title: 'Magic' };
        const secondSection = { id: 4, code: 'summer', title: 'Summer' };
        const user = { id: 23, uid: '1234567890' };

        yield [
            { id: 11, seq: 1, answered: 1, correct: 1 },
            { id: 12, seq: 2, answered: 1, correct: 1 },
            { id: 13, seq: 3, answered: 1, correct: 1 }
        ].map((trialToQuestion, i) => trialToQuestionsFactory.createWithRelations(
            trialToQuestion,
            {
                trial,
                trialTemplate,
                user,
                section: i <= 1 ? firstSection : secondSection,
                question: { id: i + 1, version: 1, text: `question${i + 1}.1` }
            }
        ));

        yield questionsFactory.createWithRelations(
            { id: 1, version: 2, text: 'question1.2' },
            { section: firstSection }
        );

        const res = yield request
            .get('/v1/admin/report/trial?trialId=13&format=xlsx')
            .set('Cookie', ['Session_id=user_session_id'])
            .parse(require('tests/helpers/binaryParser'))
            .expect(200)
            .end();

        const excel = Excel.tryLoad(res.body);

        expect(res.type).to.equal('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        expect(excel.worksheet['!ref']).to.equal('A1:G5');
    });

    // EXPERTDEV-1093:  Отчет "Получение данных о попытке"  для асессора
    it('should generate trial report for assessor', function *() {
        yield createAdmin('assessor');

        yield trialToQuestionsFactory.createWithRelations(
            { id: 11, seq: 1, answered: 1, correct: 1 },
            {
                trial: { id: 13 },
                trialTemplate: { id: 2, title: 'Winter' },
                user: { id: 23, uid: '1234567890' },
                section: { id: 3, code: 'magic', title: 'Magic' },
                question: { id: 19, version: 1, text: `question_text` }
            }
        );

        const res = yield request
            .get('/v1/admin/report/trial?trialId=13&format=xlsx')
            .set('Cookie', ['Session_id=user_session_id'])
            .parse(require('tests/helpers/binaryParser'))
            .expect(200)
            .end();

        const excel = Excel.tryLoad(res.body);

        expect(res.type).to.equal('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        expect(excel.worksheet['!ref']).to.equal('A1:F3');
    });

    // EXPERTDEV-414: выгрузка данных о попытках по логинам
    it('should generate trials report', function *() {
        yield createAdmin('analyst');

        const trialTemplate = { id: 34, slug: 'direct' };
        const authType = { code: 'web' };
        const user = { id: 7, login: 'zhigalov', uid: 5678 };
        const firstStarted = new Date(2017, 1, 7);
        const secondStarted = new Date(2017, 2, 7);
        const otherUser = { id: 8, login: 'm-smirnov', uid: 1234 };
        const otherStarted = new Date(2017, 3, 1);

        yield [
            { id: 12, started: firstStarted, nullified: 0, passed: 1 },
            { id: 13, started: secondStarted, nullified: 0, passed: 1 },
            { id: 14, started: otherStarted, nullified: 0, passed: 1 }
        ].map((trial, i) => certificatesFactory.createWithRelations(
            { id: i + 10 },
            {
                trial,
                trialTemplate,
                user: i < 2 ? user : otherUser,
                authType
            }
        ));

        const from = new Date(2017, 1, 1).toISOString();
        const to = new Date(2017, 3, 3).toISOString();
        const query = `slug=direct&from=${from}&to=${to}&login=zhigalov&login=m-smirnov&format=xlsx`;
        const res = yield request
            .get(`/v1/admin/report/trials?${query}`)
            .set('Cookie', ['Session_id=user_session_id'])
            .parse(require('tests/helpers/binaryParser'))
            .expect(200)
            .end();

        const excel = Excel.tryLoad(res.body);

        expect(res.type).to.equal('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        expect(excel.worksheet['!ref']).to.equal('A1:R5');
    });

    // EXPERTDEV-724: [API] Новый отчет про ответы на вопросы
    it('should generate questions report', function *() {
        const started = new Date(2017, 1, 7);
        const section = { id: 1, code: '1' };
        let question = { id: 1, version: 1, text: 'first question text', type: 0, active: 1 };

        yield createAdmin('analyst');

        const trialTemplate = { id: 34, slug: 'direct' };

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 0 },
            { question, trial: { id: 1, started }, trialTemplate, section }
        );

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 0 },
            { question, trial: { id: 2, started }, trialTemplate, section }
        );

        question = { id: 2, version: 4, text: 'second question text', type: 1, active: 1 };

        yield trialToQuestionsFactory.createWithRelations(
            { seq: 2, answered: 1, correct: 1 },
            { question, trial: { id: 1, started }, trialTemplate, section }
        );

        const from = new Date(2017, 1, 1).toISOString();
        const to = new Date(2017, 3, 3).toISOString();
        const query = `slug=direct&from=${from}&to=${to}&format=xlsx`;

        const res = yield request
            .get(`/v1/admin/report/questions?${query}`)
            .set('Cookie', ['Session_id=user_session_id'])
            .parse(require('tests/helpers/binaryParser'))
            .expect(200)
            .end();

        const excel = Excel.tryLoad(res.body);

        expect(res.type).to.equal('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        expect(excel.worksheet['!ref']).to.equal('A1:F4');
    });

    // EXPERTDEV-831: [API] Отчет о забаненных пользователях. Разбор кейса
    it('should generate bans by login report', function *() {
        yield createAdmin('admin');

        const firstTrialTemplate = { id: 1, slug: 'winter' };
        const secondTrialTemplate = { id: 2, slug: 'summer' };
        const authType = { id: 1, code: 'web' };
        const globalUser = {
            id: 4,
            actualLogin: 'second',
            isBanned: false
        };
        const firstUserAccount = { id: 1, uid: 123, login: 'first' };
        const secondUserAccount = { id: 2, uid: 456, login: 'second' };

        yield usersFactory.createWithRelations(firstUserAccount, { globalUser, authType });
        yield usersFactory.createWithRelations(secondUserAccount, { globalUser, authType });
        yield bansFactory.createWithRelations({
            id: 1,
            action: 'ban',
            startedDate: new Date(1, 1, 1),
            expiredDate: new Date(5, 5, 5),
            reason: 'it is rainy',
            userLogin: 'first'
        }, {
            globalUser,
            trialTemplate: firstTrialTemplate,
            admin: { id: 1, login: 'dotokoto' }
        });
        yield bansFactory.createWithRelations({
            id: 2,
            action: 'ban',
            startedDate: new Date(4, 4, 4),
            expiredDate: new Date(7, 7, 7),
            reason: 'it is sunny',
            userLogin: 'second'
        }, {
            globalUser,
            trialTemplate: secondTrialTemplate,
            admin: { id: 2, login: 'rinka' }
        });

        const query = `login=first&format=xlsx`;

        const res = yield request
            .get(`/v1/admin/report/bansByLogin?${query}`)
            .set('Cookie', ['Session_id=user_session_id'])
            .parse(require('tests/helpers/binaryParser'))
            .expect(200)
            .end();

        const excel = Excel.tryLoad(res.body);

        expect(res.type).to.equal('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        expect(excel.worksheet['!ref']).to.equal('A1:K4');
    });

    // EXPERTDEV-989: [API] Отчет по банам за период и по группе тестов. Управленческая отчетность
    it('should generate bans aggregation report', function *() {
        yield createAdmin('admin');

        const started = new Date(2017, 0, 20);

        yield [
            { id: 1, slug: 'direct' },
            { id: 2, slug: 'market' }
        ].map(trialTemplate => certificatesFactory.createWithRelations(
            { active: 1 },
            {
                trialTemplate,
                trial: { nullified: 0, started }
            }
        ));

        const from = new Date(2017, 0, 15);
        const to = new Date(2017, 1, 1);
        const query = `from=${from}&to=${to}&slug=direct&slug=market&format=xlsx`;

        const res = yield request
            .get(`/v1/admin/report/bansAggregation?${query}`)
            .set('Cookie', ['Session_id=user_session_id'])
            .parse(require('tests/helpers/binaryParser'))
            .expect(200)
            .end();

        const excel = Excel.tryLoad(res.body);

        expect(res.type).to.equal('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        expect(excel.worksheet['!ref']).to.equal('A1:H4');
    });

    it('should generate XLSX report', function *() {
        yield createAdmin('analyst');
        yield certificatesFactory.createWithRelations({ id: 1 }, { trial: { passed: 1 } });

        const res = yield request
            .get('/v1/admin/report/certificate?certId=1&format=xlsx')
            .set('Cookie', ['Session_id=user_session_id'])
            .parse(require('tests/helpers/binaryParser'))
            .expect(200)
            .end();

        const excel = Excel.tryLoad(res.body);

        expect(res.type).to.equal('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        expect(excel.worksheet['!ref']).to.equal('A1:X3');
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .get('/v1/admin/report/certificate?certId=1')
            .expect(401)
            .expect({ internalCode: '401_UNA', message: 'User not authorized' })
            .end();
    });

    it('should throw 403 when user has no access to report', function *() {
        yield request
            .get('/v1/admin/report/certificate?certId=1')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({ internalCode: '403_HNA', message: 'User has no access to report' })
            .end();
    });

    it('should throw 400 when reporter not found', function *() {
        yield createAdmin('analyst');

        yield request
            .get('/v1/admin/report/unknown')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect({
                internalCode: '400_NRF',
                message: 'No reporter for type',
                type: 'unknown'
            })
            .end();
    });
});
