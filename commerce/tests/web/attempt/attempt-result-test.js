require('co-mocha');

let api = require('api');
let request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const moment = require('moment');
const mockery = require('mockery');
const nock = require('nock');

const { convert } = require('tests/helpers/dateHelper');
const MdsModel = require('models/mds');

const { Trial, Certificate, User } = require('db/postgres');

const dbHelper = require('tests/helpers/clear');
const nockBlackbox = require('tests/helpers/blackbox').nockExtBlackbox;
const nockAvatars = require('tests/helpers/mdsServices').avatars;
const nockProtocol = require('tests/helpers/proctorEdu').protocol;
const trialToQuestionsFactory = require('tests/factory/trialToQuestionsFactory');
const trialTemplateToSectionsFactory = require('tests/factory/trialTemplateToSectionsFactory');
const trialTemplateAllowedFailsFactory = require('tests/factory/trialTemplateAllowedFailsFactory');
const usersFactory = require('tests/factory/usersFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const certificatesFactory = require('tests/factory/certificatesFactory');
const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');

describe('Attempt result controller', () => {
    before(() => {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });
        nockAvatars.success();
        nockProtocol({ openId: 'correct-open-id', code: 200, response: { evaluation: 93 } });
    });

    after(nock.cleanAll);

    const now = new Date();
    const trial = {
        id: 3,
        expired: 1,
        passed: 0,
        timeLimit: 100000,
        started: moment(now).subtract(1, 'hour'),
        finished: moment(now)
    };
    const trialTemplate = {
        id: 2,
        timeLimit: 100000,
        periodBeforeCertificateReset: '1M',
        delays: '1M, 2M, 3M',
        isProctoring: false
    };
    const service = {
        id: 19,
        code: 'direct',
        title: 'Yandex.Direct'
    };
    const type = {
        id: 13,
        code: 'cert',
        title: 'Certification'
    };
    const firstSection = {
        id: 5,
        code: 'show_strategy',
        title: 'Strategy'
    };
    const secondSection = {
        id: 6,
        code: 'keywords',
        title: 'Key words'
    };
    const user = {
        id: 23456,
        uid: 1234567890,
        firstname: 'Petr',
        lastname: 'Ivanov'
    };
    const authType = { id: 2, code: 'web' };
    const oldAgency = { id: 3, login: 'old-agency' };

    beforeEach(function *() {
        yield dbHelper.clear();
        yield trialsFactory.createWithRelations(
            trial,
            { trialTemplate, service, type, user, agency: oldAgency, authType }
        );
        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 2 },
            { trialTemplate, section: firstSection }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 1 },
            { trialTemplate, section: firstSection }
        );
        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 2 },
            { trialTemplate, section: secondSection }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 1 },
            { trialTemplate, section: secondSection }
        );
    });

    it('should return correct certificate data when certificate exist in web', function *() {
        yield certificatesFactory.createWithRelations(
            {
                id: 18,
                firstname: 'Andrey',
                lastname: 'Petrov',
                dueDate: moment(now).add(2, 'month').startOf('day').toDate(),
                confirmedDate: moment(now),
                active: 1,
                imagePath: '414/75648393_18'
            },
            { trial, trialTemplate, service, type, user, authType }
        );

        const res = yield request
            .post('/v1/attempt/3/result')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();

        const actual = res.body;

        expect(actual.certId).to.equal(18);
        expect(actual.firstname).to.equal('Andrey');
        expect(actual.lastname).to.equal('Petrov');
        expect(actual.dueDate).to.deep.equal(convert(moment(now).add(2, 'month')));
        expect(actual.active).to.equal(1);
        expect(actual.imagePath).to.equal(MdsModel.getAvatarsPath('414/75648393_18'));
        expect(actual.hashedUserId).to.equal('7bX4uf9cpA');
        expect(actual.isProctoring).to.be.false;
    });

    it('should return correct certificate data when certificate exist in telegram', function *() {
        const otherUser = { id: 5342, uid: 7972349 };
        const otherAuthType = { id: 3, code: 'telegram' };
        const otherTrial = {
            id: 9,
            expired: 1,
            passed: 1,
            timeLimit: 100000,
            started: moment(now).subtract(1, 'hour'),
            finished: moment(now)
        };

        yield certificatesFactory.createWithRelations(
            {
                id: 98,
                firstname: 'Andrey',
                lastname: 'Petrov',
                dueDate: moment(now).add(2, 'month').startOf('day').toDate(),
                confirmedDate: moment(now),
                active: 1,
                imagePath: '414/75648393_18'
            },
            {
                trial: otherTrial,
                user: otherUser,
                authType: otherAuthType,
                trialTemplate,
                service,
                type
            }
        );

        const res = yield request
            .post('/telegram/attempt/9/result')
            .set('Cookie', ['uid=7972349;firstname=Andrey;'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();

        const actual = res.body;

        expect(actual.certId).to.equal(98);
        expect(actual.firstname).to.equal('Andrey');
        expect(actual.lastname).to.equal('Petrov');
        expect(actual.dueDate).to.deep.equal(convert(moment(now).add(2, 'month')));
        expect(actual.active).to.equal(1);
        expect(actual.imagePath).to.equal(MdsModel.getAvatarsPath('414/75648393_18'));
        expect(actual.hashedUserId).to.equal('9p9bX4p');
        expect(actual.isProctoring).to.be.false;
    });

    // eslint-disable-next-line max-len
    it('should return correct data without certificate data when certificate does not exist', function *() {
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            { trial, question: { id: 1 }, section: firstSection, trialTemplate }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 4, answered: 1, correct: 0 },
            { trial, question: { id: 2 }, section: firstSection, trialTemplate }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 2, answered: 1, correct: 1 },
            { trial, question: { id: 3 }, section: secondSection, trialTemplate }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 3, answered: 1, correct: 0 },
            { trial, question: { id: 4 }, section: secondSection, trialTemplate }
        );

        const res = yield request
            .post('/v1/attempt/3/result')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();

        const actual = res.body;

        expect(actual.certId).to.be.undefined;
        expect(actual.firstname).to.be.undefined;
        expect(actual.lastname).to.be.undefined;
        expect(actual.dueDate).to.be.undefined;
        expect(actual.active).to.be.undefined;
        expect(actual.imagePath).to.be.undefined;

        expect(actual.sections.length).to.equal(2);
        expect(actual.total.passed).to.equal(1);
        expect(actual.total.correctCount).to.equal(2);
        expect(actual.total.totalCount).to.equal(4);
        expect(actual.service).to.deep.equal(service);
        expect(actual.type).to.deep.equal(type);
        expect(actual.availabilityDate).to.deep.equal(convert(moment(now).add(1, 'month')));
        expect(actual.finished).to.deep.equal(convert(now, { withTime: true }));
        expect(actual.hashedUserId).to.equal('7bX4uf9cpA');
        expect(actual.isProctoring).to.be.false;
    });

    describe('with proctoring', () => {
        // eslint-disable-next-line max-len
        it('should return correct data for exam with proctoring when certificate does not exist', function *() {
            const proTrial = {
                id: 30,
                expired: 1,
                passed: 1,
                started: moment(now).subtract(1, 'hour'),
                finished: moment(now)
            };

            const proTrialTemplate = {
                id: 20,
                isProctoring: true,
                delays: '2M'
            };

            yield trialsFactory.createWithRelations(
                proTrial,
                {
                    trialTemplate: proTrialTemplate,
                    service,
                    type,
                    user,
                    agency: oldAgency,
                    authType
                }
            );

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 2 },
                { trialTemplate: proTrialTemplate, section: firstSection }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 1 },
                { trialTemplate: proTrialTemplate, section: firstSection }
            );

            yield trialToQuestionsFactory.createWithRelations(
                { seq: 1, answered: 1, correct: 1 },
                {
                    trial: proTrial,
                    question: { id: 1 },
                    section: firstSection,
                    trialTemplate: proTrialTemplate
                }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 4, answered: 1, correct: 0 },
                {
                    trial: proTrial,
                    question: { id: 2 },
                    section: firstSection,
                    trialTemplate: proTrialTemplate
                }
            );

            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'failed',
                isRevisionRequested: true,
                time: now,
                isLast: false
            }, { trial: proTrial });

            yield proctoringResponsesFactory.createWithRelations({
                source: 'toloka',
                verdict: 'failed',
                isRevisionRequested: false,
                time: moment(now).add(1, 'month'),
                isLast: true
            }, { trial: proTrial });

            const res = yield request
                .post('/v1/attempt/30/result')
                .set('Cookie', ['Session_id=user_session_id'])
                .expect(200)
                .expect('Content-Type', /json/)
                .end();

            const actual = res.body;

            expect(actual.certId).to.be.undefined;
            expect(actual.total).to.deep.equal({
                passed: 1,
                correctCount: 1,
                totalCount: 2,
                lastVerdict: 'failed',
                lastSource: 'toloka',
                isRevisionRequested: true
            });
            expect(actual.availabilityDate).to.deep.equal(convert(moment(now).add(2, 'month')));
            expect(actual.finished).to.deep.equal(convert(now, { withTime: true }));
            expect(actual.isProctoring).to.be.true;
        });
    });

    describe('`complete`', () => {
        beforeEach(function *() {
            yield Trial.update(
                { expired: 0, finished: null, passed: 0 },
                { where: { id: trial.id } }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 1, answered: 1, correct: 1 },
                { trial, question: { id: 1 }, section: firstSection, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 4, answered: 1, correct: 0 },
                { trial, question: { id: 2 }, section: firstSection, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 2, answered: 1, correct: 1 },
                { trial, question: { id: 3 }, section: secondSection, trialTemplate }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 3, answered: 1, correct: 0 },
                { trial, question: { id: 4 }, section: secondSection, trialTemplate }
            );
        });

        it('should finish attempt', function *() {
            yield request
                .post('/v1/attempt/3/result')
                .set('Cookie', ['Session_id=user_session_id'])
                .expect(200)
                .expect('Content-Type', /json/)
                .end();

            const actualTrial = yield Trial.findOne({
                where: { id: 3 }
            });

            expect(actualTrial.expired).to.equal(1);
            expect(actualTrial.finished).to.not.be.null;
            expect(actualTrial.passed).to.equal(1);
        });

        it('should return certificate data', function *() {
            const result = yield request
                .post('/v1/attempt/3/result')
                .set('Cookie', ['Session_id=user_session_id'])
                .expect(200)
                .expect('Content-Type', /json/)
                .end();
            const actual = result.body;
            const certificateData = yield Certificate.findOne({ trialId: 3 });

            expect(actual.firstname).to.equal('Petr');
            expect(actual.lastname).to.equal('Ivanov');
            expect(actual.active).to.equal(1);
            expect(actual.dueDate).to.equal(convert(certificateData.get('dueDate')));
            expect(actual.imagePath).to.equal(MdsModel.getAvatarsPath('603/1468925144742_555555'));
        });

        it('should create certificate without proctoring', function *() {
            yield request
                .post('/v1/attempt/3/result')
                .set('Cookie', ['Session_id=user_session_id'])
                .expect(200)
                .expect('Content-Type', /json/)
                .end();
            const certificateData = yield Certificate.findOne({ trialId: 3 });
            const actual = certificateData.toJSON();

            expect(actual.trialId).to.equal(3);
            expect(actual.firstname).to.equal('Petr');
            expect(actual.lastname).to.equal('Ivanov');
            expect(actual.confirmedDate).to.be.a('date');
            expect(actual.dueDate).to.be.a('date');
            expect(actual.active).to.equal(1);
            expect(actual.confirmed).to.equal(1);
            expect(actual.imagePath).to.equal('603/1468925144742_555555');
        });

        // EXPERTDEV-519: Завершение тестирования с прокторингом в API
        it('should create certificate with proctoring', function *() {
            const trialTemplatePro = { id: 7, isProctoring: true };
            const trialPro = { id: 17, expired: 0, openId: 'correct-open-id' };

            yield trialTemplateToSectionsFactory.createWithRelations(
                { quantity: 1 },
                { trialTemplate: trialTemplatePro, section: firstSection }
            );
            yield trialTemplateAllowedFailsFactory.createWithRelations(
                { allowedFails: 0 },
                { trialTemplate: trialTemplatePro, section: firstSection }
            );
            yield trialToQuestionsFactory.createWithRelations(
                { seq: 1, answered: 1, correct: 1 },
                {
                    trial: trialPro,
                    trialTemplate: trialTemplatePro,
                    question: { id: 27 },
                    section: firstSection,
                    user,
                    authType
                }
            );

            const res = yield request
                .post('/v1/attempt/17/result')
                .set('Cookie', ['Session_id=user_session_id'])
                .expect(200)
                .expect('Content-Type', /json/)
                .end();

            const actual = yield Certificate.findAll({
                where: { trialId: 17 },
                attributes: ['id'],
                raw: true
            });

            expect(actual.length).to.equal(1);
            expect(res.body.certId).to.equal(actual[0].id);
        });
    });

    it('should return 400 when attempt id is invalid', function *() {
        yield request
            .post('/v1/attempt/abc/result')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect({
                message: 'Attempt id is invalid',
                internalCode: '400_AII',
                attemptId: 'abc'
            })
            .end();
    });

    it('should return 401 when user is not authorized', function *() {
        yield request
            .post('/v1/attempt/3/result')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 404 when attempt not found', function *() {
        yield request
            .post('/v1/attempt/4/result')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect({
                message: 'Attempt not found',
                internalCode: '404_ATF'
            })
            .end();
    });

    it('should throw 403 when user not author of attempt', function *() {
        yield usersFactory.createWithRelations(
            { id: 234, uid: 11111111111 },
            { role: { id: 1 }, authType }
        );
        yield Trial.update({ userId: 234 }, { where: { id: trial.id } });

        yield request
            .post('/v1/attempt/3/result')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'Illegal user for attempt',
                internalCode: '403_IUA'
            })
            .end();
    });

    it('should throw 404 when user not found', function *() {
        yield User.update({ uid: 12345 }, { where: { id: 23456 } });

        yield request
            .post('/v1/attempt/3/result')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect({
                message: 'User not found',
                internalCode: '404_UNF',
                uid: 1234567890,
                authType: 'web'
            })
            .end();
    });

    // EXPERTDEV-518: Обернуть в транзакцию завершение попытки
    describe('Fail transaction', () => {
        beforeEach(() => {
            const create = function () {
                throw new Error('Unable to create certificate');
            };

            mockery.registerMock('models/certificate', { create });
            mockery.enable({
                useCleanCache: true,
                warnOnReplace: false,
                warnOnUnregistered: false
            });

            api = require('api');
            request = require('co-supertest').agent(api.callback());
        });

        afterEach(() => {
            mockery.disable();
        });

        it('should not get result when certificate was not created', function *() {
            yield Trial.update(
                { expired: 0, passed: 0, finished: null },
                { where: { userId: user.id } }
            );

            yield request
                .post('/v1/attempt/3/result')
                .set('Cookie', ['Session_id=user_session_id'])
                .expect(500)
                .end();

            const actual = yield Trial.findOne({
                where: { id: 3 },
                attributes: ['passed', 'expired', 'finished'],
                raw: true
            });

            expect(actual.expired).to.equal(0);
            expect(actual.passed).to.equal(0);
            expect(actual.finished).to.be.null;
        });
    });
});
