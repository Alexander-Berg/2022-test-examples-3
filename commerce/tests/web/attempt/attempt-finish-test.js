require('co-mocha');

let api = require('api');
const config = require('yandex-config');
let request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const mockery = require('mockery');
const nock = require('nock');

const {
    Trial,
    Certificate,
    User
} = require('db/postgres');

const Attempt = require('models/attempt');

const tvm = require('helpers/tvm');

const dbHelper = require('tests/helpers/clear');
const nockBlackbox = require('tests/helpers/blackbox').nockExtBlackbox;
const nockAvatars = require('tests/helpers/mdsServices').avatars;
const nockProtocol = require('tests/helpers/proctorEdu').protocol;
const nockTvm = require('tests/helpers/nockTvm');

const trialsFactory = require('tests/factory/trialsFactory');
const trialToQuestionsFactory = require('tests/factory/trialToQuestionsFactory');
const trialTemplateToSectionsFactory = require('tests/factory/trialTemplateToSectionsFactory');
const trialTemplateAllowedFailsFactory = require('tests/factory/trialTemplateAllowedFailsFactory');
const usersFactory = require('tests/factory/usersFactory');

describe('Attempt finish controller', () => {
    const section = { id: 2 };
    const trial = {
        id: 3,
        expired: 0,
        started: new Date(),
        timeLimit: 100000
    };
    const trialTemplate = { id: 2, slug: 'test', validityPeriod: '1y', isProctoring: false };
    const user = {
        id: 2345,
        uid: 1234567890,
        firstname: 'Petr',
        lastname: 'Ivanov'
    };
    const authType = { id: 2, code: 'web' };
    const oldAgency = { id: 13, login: 'old-agency' };

    const findTrialOptions = { attributes: ['nullified', 'nullifyReason'] };

    beforeEach(function *() {
        yield dbHelper.clear();

        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 2 },
            { trialTemplate, section }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 1 },
            { trialTemplate, section }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            {
                trial,
                question: { id: 4 },
                section,
                trialTemplate,
                user,
                agency: oldAgency,
                authType
            }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 2, answered: 1, correct: 1 },
            {
                trial,
                question: { id: 5 },
                section,
                trialTemplate,
                user,
                agency: oldAgency,
                authType
            }
        );

        nockBlackbox({ response: { uid: { value: '1234567890' } } });
        nockAvatars.success();
        nockProtocol({ openId: 'correct-open-id', code: 200, response: { evaluation: 93 } });
    });

    afterEach(nock.cleanAll);

    it('should success finish', function *() {
        let attempt = yield Attempt.findById(3);

        expect(attempt.get('expired')).to.equal(0);
        expect(attempt.get('passed')).to.equal(0);
        expect(attempt.get('finished')).to.be.null;

        yield request
            .post('/v1/attempt/3/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({ isNullified: false })
            .end();

        attempt = yield Attempt.findById(3);
        expect(attempt.get('expired')).to.equal(1);
        expect(attempt.get('passed')).to.equal(1);
        expect(attempt.get('finished')).to.not.be.null;
    });

    it('should create certificate without proctoring', function *() {
        yield request
            .post('/v1/attempt/3/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({ isNullified: false })
            .end();

        const certificate = yield Certificate.findOne({ where: { trialId: 3 } });
        const actual = certificate.toJSON();

        expect(actual.trialId).to.equal(3);
        expect(actual.firstname).to.equal('Petr');
        expect(actual.lastname).to.equal('Ivanov');
        expect(actual.confirmedDate).to.be.a('date');
        expect(actual.dueDate).to.be.a('date');
        expect(actual.active).to.equal(1);
        expect(actual.confirmed).to.equal(1);
    });

    // EXPERTDEV-519: Завершение тестирования с прокторингом в API
    it('should create certificate with proctoring', function *() {
        const trialTemplatePro = { id: 7, isProctoring: true };
        const trialPro = { id: 17, expired: 0, openId: 'correct-open-id' };

        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 1 },
            { trialTemplate: trialTemplatePro, section }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 0 },
            { trialTemplate: trialTemplatePro, section }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            {
                trial: trialPro,
                trialTemplate: trialTemplatePro,
                question: { id: 27 },
                section,
                user,
                authType
            }
        );

        yield request
            .post('/v1/attempt/17/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({ isNullified: false })
            .end();

        const actual = yield Certificate.count({ where: { trialId: 17 } });

        expect(actual).to.equal(1);
    });

    // EXPERTDEV-1086 [API] Завершать тест с новым флагом
    it('should not create certificate with proctoring when critical metrics were recorded', function *() {
        const trialTemplatePro = { id: 7, isProctoring: true };
        const trialPro = { id: 17, expired: 0, openId: 'correct-open-id' };

        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 1 },
            { trialTemplate: trialTemplatePro, section }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 0 },
            { trialTemplate: trialTemplatePro, section }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            {
                trial: trialPro,
                trialTemplate: trialTemplatePro,
                question: { id: 27 },
                section,
                user,
                authType
            }
        );

        yield request
            .post('/v1/attempt/17/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ isCritMetrics: true })
            .set('Accept', 'application/json')
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({ isNullified: false })
            .end();

        const certificatesCount = yield Certificate.count({ where: { trialId: 17 } });

        expect(certificatesCount).to.equal(0);
    });

    // EXPERTDEV-1128: Перенесли логику аннулирования в ручку finish
    it('should nullify attempt by metrics if requestNullify and critMetrics sent', function *() {
        const trialTemplatePro = { id: 3, isProctoring: true };
        const trialPro = { id: 13, expired: 0, openId: 'correct-open-id' };

        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 1 },
            { trialTemplate: trialTemplatePro, section }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 0 },
            { trialTemplate: trialTemplatePro, section }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            {
                trial: trialPro,
                trialTemplate: trialTemplatePro,
                question: { id: 27 },
                section,
                user,
                authType
            }
        );

        yield request
            .post('/v1/attempt/13/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ isCritMetrics: true, requestNullify: true })
            .set('Accept', 'application/json')
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({ isNullified: true })
            .end();

        const actualTrial = yield Trial.findById(13, findTrialOptions);
        const actual = actualTrial.toJSON();

        expect(actual.nullified).to.equal(1);
        expect(actual.nullifyReason).to.equal('metrics');
    });

    it('should nullify attempt manually if requestNullify sent without critMetrics', function *() {
        const trialTemplatePro = { id: 5, isProctoring: true };
        const trialPro = { id: 15, expired: 0, openId: 'correct-open-id' };

        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 1 },
            { trialTemplate: trialTemplatePro, section }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 0 },
            { trialTemplate: trialTemplatePro, section }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            {
                trial: trialPro,
                trialTemplate: trialTemplatePro,
                question: { id: 27 },
                section,
                user,
                authType
            }
        );

        yield request
            .post('/v1/attempt/15/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ requestNullify: true })
            .set('Accept', 'application/json')
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({ isNullified: true })
            .end();

        const actualTrial = yield Trial.findById(15, findTrialOptions);
        const actual = actualTrial.toJSON();

        expect(actual.nullified).to.equal(1);
        expect(actual.nullifyReason).to.equal('manual');
    });

    it('should not nullify attempt by metrics if nullifies limit by metrics exceeded', function *() {
        const started = new Date();

        const trialTemplatePro = { id: 9, isProctoring: true };
        const trialPro = { id: 19, expired: 0, nullified: 0, nullifyReason: null, openId: 'correct-open-id', started };

        yield trialsFactory.createWithRelations(
            { id: 20, nullified: 1, nullifyReason: 'metrics', started },
            { trialTemplate: trialTemplatePro, user }
        );
        yield trialsFactory.createWithRelations(
            { id: 21, nullified: 1, nullifyReason: 'metrics', started },
            { trialTemplate: trialTemplatePro, user }
        );
        yield trialsFactory.createWithRelations(
            { id: 22, nullified: 1, nullifyReason: 'metrics', started },
            { trialTemplate: trialTemplatePro, user }
        );

        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 1 },
            { trialTemplate: trialTemplatePro, section }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 0 },
            { trialTemplate: trialTemplatePro, section }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            {
                trial: trialPro,
                trialTemplate: trialTemplatePro,
                question: { id: 27 },
                section,
                user,
                authType
            }
        );

        yield request
            .post('/v1/attempt/19/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ isCritMetrics: true, requestNullify: true })
            .set('Accept', 'application/json')
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({ isNullified: false })
            .end();

        const actualTrial = yield Trial.findById(19, findTrialOptions);
        const actual = actualTrial.toJSON();

        expect(actual.nullified).to.equal(0);
        expect(actual.nullifyReason).to.equal(null);
    });

    it('should nullify attempt manually if nullifies limit by metrics exceeded', function *() {
        const started = new Date();

        const trialTemplatePro = { id: 9, isProctoring: true };
        const trialPro = { id: 19, expired: 0, nullified: 0, nullifyReason: null, openId: 'correct-open-id', started };

        yield trialsFactory.createWithRelations(
            { id: 20, nullified: 1, nullifyReason: 'metrics', started },
            { trialTemplate: trialTemplatePro, user }
        );
        yield trialsFactory.createWithRelations(
            { id: 21, nullified: 1, nullifyReason: 'metrics', started },
            { trialTemplate: trialTemplatePro, user }
        );
        yield trialsFactory.createWithRelations(
            { id: 22, nullified: 1, nullifyReason: 'metrics', started },
            { trialTemplate: trialTemplatePro, user }
        );

        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 1 },
            { trialTemplate: trialTemplatePro, section }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 0 },
            { trialTemplate: trialTemplatePro, section }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            {
                trial: trialPro,
                trialTemplate: trialTemplatePro,
                question: { id: 27 },
                section,
                user,
                authType
            }
        );

        yield request
            .post('/v1/attempt/19/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ isCritMetrics: false, requestNullify: true })
            .set('Accept', 'application/json')
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({ isNullified: true })
            .end();

        const actualTrial = yield Trial.findById(19, findTrialOptions);
        const actual = actualTrial.toJSON();

        expect(actual.nullified).to.equal(1);
        expect(actual.nullifyReason).to.equal('manual');
    });

    // EXPERTDEV-1225: Ходить в API Директа и отдавать uid сдавшего тест
    it('should send cert to geoadv when attempt is passed and has suitable exam slug', function *() {
        const geoadvTrialTemplate = { id: 57, slug: 'msp' };
        const geoadvTrial = { id: 5678 };

        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 1 },
            {
                trialTemplate: geoadvTrialTemplate,
                section
            }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 1 },
            {
                trialTemplate: geoadvTrialTemplate,
                section
            }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            {
                trial: geoadvTrial,
                question: { id: 1234 },
                section,
                trialTemplate: geoadvTrialTemplate,
                user,
                authType
            }
        );

        const tvmNock = nockTvm.getTicket({ 'geoadv-testing': { ticket: 'someTicket' } }, 1);
        const geoadvNock = nock(config.geoadv.host)
            .put(config.geoadv.path)
            .reply(200);

        yield request
            .post('/v1/attempt/5678/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();

        const actualCert = yield Certificate.findOne({
            where: { trialId: 5678 },
            attributes: ['isSentToGeoadv'],
            raw: true
        });

        expect(actualCert).to.deep.equal({ isSentToGeoadv: true });
        expect(tvmNock.isDone()).to.be.true;
        expect(geoadvNock.isDone()).to.be.true;

        tvm.cache.reset();
    });

    it('should not send request to geoadv when attempt is not passed but has suitable exam slug', function *() {
        const geoadvTrialTemplate = { id: 57, slug: 'msp' };
        const geoadvTrial = { id: 9999 };

        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 1 },
            {
                trialTemplate: geoadvTrialTemplate,
                section
            }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 0 },
            {
                trialTemplate: geoadvTrialTemplate,
                section
            }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 0 },
            {
                trial: geoadvTrial,
                question: { id: 1234 },
                section,
                trialTemplate: geoadvTrialTemplate,
                user,
                authType
            }
        );

        yield request
            .post('/v1/attempt/9999/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .end();

        const actualCert = yield Certificate.findOne({ where: { trialId: 9999 } });

        expect(actualCert).to.be.null;
    });

    it('should not send request to geoadv when need to nullify attempt', function *() {
        const geoadvTrialTemplate = { id: 57, slug: 'msp' };
        const geoadvTrial = { id: 345 };

        yield trialTemplateToSectionsFactory.createWithRelations(
            { quantity: 1 },
            {
                trialTemplate: geoadvTrialTemplate,
                section
            }
        );
        yield trialTemplateAllowedFailsFactory.createWithRelations(
            { allowedFails: 0 },
            {
                trialTemplate: geoadvTrialTemplate,
                section
            }
        );
        yield trialToQuestionsFactory.createWithRelations(
            { seq: 1, answered: 1, correct: 1 },
            {
                trial: geoadvTrial,
                question: { id: 1234 },
                section,
                trialTemplate: geoadvTrialTemplate,
                user,
                authType
            }
        );

        yield request
            .post('/v1/attempt/345/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ requestNullify: true })
            .expect(200)
            .expect('Content-Type', /json/)
            .end();

        const actualCert = yield Certificate.findOne({
            where: { trialId: 345 },
            attributes: ['isSentToGeoadv'],
            raw: true
        });

        expect(actualCert).to.deep.equal({ isSentToGeoadv: false });
    });

    it('should return 400 when attempt id is invalid', function *() {
        yield request
            .post('/v1/attempt/abc/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect({
                message: 'Attempt id is invalid',
                internalCode: '400_AII',
                attemptId: 'abc'
            })
            .end();
    });

    it('should return 400 when crit metrics param is invalid', function *() {
        yield request
            .post('/v1/attempt/17/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ isCritMetrics: 'not_boolean' })
            .set('Accept', 'application/json')
            .expect(400)
            .expect({
                message: 'Crit metrics param is invalid',
                internalCode: '400_CMI',
                isCritMetrics: 'not_boolean'
            })
            .end();
    });

    it('should return 401 when user is not authorized', function *() {
        yield request
            .post('/v1/attempt/3/finish')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 404 when attempt not found', function *() {
        yield request
            .post('/v1/attempt/4/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(404)
            .expect({
                message: 'Attempt not found',
                internalCode: '404_ATF'
            })
            .end();
    });

    it('should throw 403 when user not author of attempt', function *() {
        yield usersFactory.createWithRelations({ id: 123, uid: 11111111111 }, { role: { id: 1 } });
        yield Trial.update({ userId: 123 }, { where: { id: trial.id } });

        yield request
            .post('/v1/attempt/3/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'Illegal user for attempt',
                internalCode: '403_IUA'
            })
            .end();
    });

    it('should throw 403 when attempt already finished', function *() {
        yield Trial.update({ expired: 1 }, { where: { id: trial.id } });

        yield request
            .post('/v1/attempt/3/finish')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'Attempt have already finished',
                internalCode: '403_AAF'
            })
            .end();
    });

    it('should throw 404 when user not found', function *() {
        yield User.update({ uid: 12345 }, { where: { id: 2345 } });

        yield request
            .post('/v1/attempt/3/finish')
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
        before(() => {
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

        after(() => {
            mockery.disable();
            mockery.deregisterAll();
        });

        it('should not finish attempt when certificate was not created', function *() {
            yield request
                .post('/v1/attempt/3/finish')
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
