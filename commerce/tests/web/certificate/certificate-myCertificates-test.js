require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const moment = require('moment');
const nock = require('nock');

const dbHelper = require('tests/helpers/clear');
const { convert } = require('tests/helpers/dateHelper');
const nockBlackbox = require('tests/helpers/blackbox').nockExtBlackbox;

const MdsModel = require('models/mds');

const certificateFactory = require('tests/factory/certificatesFactory');
const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');
const trialsFactory = require('tests/factory/trialsFactory');

describe('Certificate myCertificates controller', () => {
    const user = { id: 2376483, uid: 1234567890 };
    const authType = { id: 2, code: 'web' };
    const finished = new Date(2015, 5, 11);
    const dueDate = new Date(2016, 1, 1);
    const trialTemplate = {
        id: 3,
        delays: '1M, 1M, 1M',
        timeLimit: 90000,
        slug: 'direct',
        previewImagePath: 'path/to/preview/image'
    };
    const service = {
        id: 12,
        code: 'direct',
        title: 'Yandex.Direct'
    };
    const type = { id: 2, code: 'test' };
    const certificate = {
        id: 13,
        dueDate,
        firstname: 'Vasya',
        lastname: 'Pupkin',
        active: 1,
        confirmedDate: finished,
        imagePath: '255/38472434872_13'
    };

    before(() => {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });
    });

    after(nock.cleanAll);

    beforeEach(function *() {
        yield dbHelper.clear();
    });

    it('should return correct fields when user has not certificates', function *() {
        const res = yield request
            .get('/v1/certificates/my')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .end();

        const actual = res.body;

        expect(actual.hashedUserId).to.equal('7bX4uf9cpA');
        expect(actual.certificates.length).to.equal(0);
        expect(actual.failedTrials).to.deep.equal([]);
        expect(actual.pendingTrials).to.deep.equal([]);
        expect(actual.nullifiedCerts).to.deep.equal([]);
    });

    it('should return correct fields when user has certificates', function *() {
        const trial = {
            id: 2,
            started: moment(finished).subtract(1, 'hour').toDate(),
            finished,
            passed: 1,
            nullified: 0,
            expired: 1
        };

        yield certificateFactory.createWithRelations(
            certificate,
            { trial, user, trialTemplate, service, type, authType }
        );

        const res = yield request
            .get('/v1/certificates/my')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .end();

        const actual = res.body;
        const expectedCertificates = [
            {
                certId: 13,
                certType: 'test',
                dueDate: convert(dueDate),
                firstname: 'Vasya',
                lastname: 'Pupkin',
                active: 1,
                confirmedDate: convert(finished),
                imagePath: MdsModel.getAvatarsPath('255/38472434872_13'),
                service,
                exam: {
                    slug: 'direct',
                    id: 3
                },
                check: {
                    state: 'enabled',
                    hasValidCert: false
                },
                previewImagePath: MdsModel.getAvatarsPath('path/to/preview/image')
            }
        ];

        expect(actual.hashedUserId).to.equal('7bX4uf9cpA');
        expect(actual.certificates.length).to.equal(1);
        expect(actual.certificates).to.deep.equal(expectedCertificates);
        expect(actual.failedTrials).to.deep.equal([]);
        expect(actual.pendingTrials).to.deep.equal([]);
        expect(actual.nullifiedCerts).to.deep.equal([]);
    });

    it('should not return certificate for nullified trial', function *() {
        const trial = {
            id: 2,
            started: moment(finished).subtract(1, 'hour').toDate(),
            finished,
            passed: 1,
            nullified: 1,
            expired: 1
        };

        yield certificateFactory.createWithRelations(
            certificate,
            { trial, user, trialTemplate, service, authType }
        );

        const res = yield request
            .get('/v1/certificates/my')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .end();

        const actual = res.body;

        expect(actual.hashedUserId).to.equal('7bX4uf9cpA');
        expect(actual.certificates).to.deep.equal([]);
        expect(actual.failedTrials).to.deep.equal([]);
        expect(actual.pendingTrials).to.deep.equal([]);
        expect(actual.nullifiedCerts).to.deep.equal([]);
    });

    it('should return failed trials data when user failed trial', function *() {
        const now = new Date();
        const started = moment(now).subtract(1, 'hour').toDate();
        const trial = {
            id: 13,
            expired: 1,
            passed: 0,
            started,
            finished: now,
            nullified: 0
        };

        yield trialsFactory.createWithRelations(trial, { trialTemplate, user, authType });

        const res = yield request
            .get('/v1/certificates/my?exams=direct')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .end();

        const actual = res.body;

        expect(actual.hashedUserId).to.equal('7bX4uf9cpA');
        expect(actual.certificates).to.deep.equal([]);
        expect(actual.failedTrials).to.deep.equal([{
            examSlug: 'direct',
            state: 'disabled',
            availabilityDate: moment(now).add(1, 'month').startOf('day').toISOString(),
            started: started.toISOString(),
            trialId: trial.id,
            passed: 0,
            sections: []
        }]);
        expect(actual.pendingTrials).to.deep.equal([]);
        expect(actual.nullifiedCerts).to.deep.equal([]);
    });

    it('should return pending trials data when they exist', function *() {
        const now = new Date();
        const started = moment(now).subtract(1, 'hour').toDate();
        const startedString = started.toISOString();
        const responseTime = moment(now).subtract(1, 'month').subtract(1, 'day');
        const trial = {
            id: 14,
            expired: 1,
            passed: 1,
            started,
            finished: now,
            nullified: 0
        };
        const otherTrialTemplate = {
            id: 4,
            delays: '1M, 1M, 1M',
            timeLimit: 90000,
            slug: 'direct_pro',
            isProctoring: true
        };
        const proctoringResponse = {
            trialId: 14,
            source: 'proctoring',
            verdict: 'pending',
            isLast: true,
            time: responseTime
        };

        yield trialsFactory.createWithRelations(trial, {
            trialTemplate: otherTrialTemplate,
            user,
            authType
        });
        yield proctoringResponsesFactory.createWithRelations(proctoringResponse, { trial });

        const res = yield request
            .get('/v1/certificates/my?exams=direct_pro')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .end();

        const actual = res.body;

        expect(actual.hashedUserId).to.equal('7bX4uf9cpA');
        expect(actual.certificates).to.deep.equal([]);
        expect(actual.failedTrials).to.deep.equal([]);
        expect(actual.pendingTrials).to.deep.equal([
            {
                trialId: 14,
                examSlug: 'direct_pro',
                started: startedString
            }
        ]);
        expect(actual.nullifiedCerts).to.deep.equal([]);
    });

    it('should return proctoring fail reason', function *() {
        const now = new Date();
        const started = moment(now).subtract(1, 'hour').toDate();
        const responseTime = now;
        const trial = {
            id: 123,
            started,
            time: now,
            expired: 1
        };
        const otherTrialTemplate = {
            id: 4,
            delays: '1M',
            slug: 'direct_pro',
            isProctoring: true
        };
        const proctoringResponse = {
            source: 'proctoring',
            verdict: 'failed',
            isLast: true,
            time: responseTime
        };
        const availabilityDate = moment(now).add(1, 'month').startOf('day').toDate();

        yield trialsFactory.createWithRelations(trial, {
            trialTemplate: otherTrialTemplate,
            user,
            authType
        });
        yield proctoringResponsesFactory.createWithRelations(proctoringResponse, { trial });

        const res = yield request
            .get('/v1/certificates/my?exams=direct_pro')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .end();

        const actual = res.body;

        expect(actual.failedTrials).to.deep.equal([
            {
                examSlug: 'direct_pro',
                state: 'disabled',
                started: started.toISOString(),
                trialId: trial.id,
                source: proctoringResponse.source,
                availabilityDate: availabilityDate.toISOString(),
                passed: 0,
                sections: []
            }
        ]);
    });

    it('should return correct fields when certificate is nullified', function *() {
        const trial = { id: 2, nullified: 0 };
        const actualDueDate = moment().add(1, 'year').toDate();
        const confirmedDate = new Date(2, 2, 2);
        const certType = { id: 7, code: 'cert' };

        yield certificateFactory.createWithRelations(
            { id: 3, active: 0, dueDate: actualDueDate, confirmedDate },
            {
                trial,
                user,
                trialTemplate,
                service,
                type: certType,
                authType
            }
        );

        const res = yield request
            .get('/v1/certificates/my?exams=direct')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .end();

        expect(res.body).to.deep.equal({
            hashedUserId: '7bX4uf9cpA',
            certificates: [],
            failedTrials: [],
            pendingTrials: [],
            nullifiedCerts: [
                {
                    id: 3,
                    confirmedDate: confirmedDate.toISOString(),
                    dueDate: actualDueDate.toISOString(),
                    exam: { id: 3, slug: 'direct' },
                    service: 'direct'
                }
            ]
        });
    });

    it('should return 401 when user is not authorized', function *() {
        yield request
            .get('/v1/certificates/my')
            .expect(401)
            .expect({ message: 'User not authorized', internalCode: '401_UNA' })
            .end();
    });

    it('should return 400 when exam slug is invalid', function *() {
        yield request
            .get('/v1/certificates/my?exams=inv@lid')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect({
                message: 'Exam slug contains invalid characters',
                internalCode: '400_EIC',
                exam: 'inv@lid'
            })
            .end();
    });
});
