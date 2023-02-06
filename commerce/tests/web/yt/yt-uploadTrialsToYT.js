require('co-mocha');

let api = require('api');
let request = require('co-supertest').agent(api.callback());
const dbHelper = require('tests/helpers/clear');
const { expect } = require('chai');
const mockery = require('mockery');
const moment = require('moment');
const sinon = require('sinon');
const nock = require('nock');
const ip = require('ip');
const config = require('yandex-config');
const { URL } = require('url');

let YT = require('models/yt');

const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');
const userIdentificationsFactory = require('tests/factory/userIdentificationsFactory');
const proctoringVideosFactory = require('tests/factory/proctoringVideosFactory');

const nockProctorEdu = require('tests/helpers/proctorEdu');
const nockYT = require('tests/helpers/yt');
const mockMailer = require('tests/helpers/mailer');
const nockSeveralUids = require('tests/helpers/blackbox').nockExtSeveralUids;

const { ProctoringResponses } = require('db/postgres');

describe('YT controller', () => {
    const url = '/v1/yt/uploadTrialsToYT';
    const trial = {
        id: 1,
        passed: 1,
        nullified: 0,
        filesStatus: 'saved',
        openId: 'open-id-to-upload'
    };
    const user = { id: 234, uid: 1234567890 };
    const trialTemplate = { id: 4, isProctoring: true };
    const userIdentification = { face: 'faces/face' };
    const proctoringResponse = {
        source: 'proctoring',
        verdict: 'pending',
        isSentToToloka: false,
        isRevisionRequested: false,
        isLast: true
    };
    const protocol = {
        startedAt: '2018-01-01T00:00:00.000Z', // 1514764800000
        timesheet: {
            xaxis: [1, 2, 5],
            yaxis: [
                { b1: 0, b2: 0, b3: 0, c1: 8, c2: 2, c3: 0, c4: 0, m1: 2, m2: 20, k1: 0, n1: 0 },
                { b1: 0, b2: 4, b3: 0, c1: 0, c2: 0, c3: 53, c4: 5, m1: 2, m2: 0, k1: 8, n1: 0 },
                { b1: 0, b2: 4, b3: 44, c1: 0, c2: 0, c3: 0, c4: 5, m1: 2, m2: 0, k1: 8, n1: 0 }
            ]
        }
    };

    before(() => {
        mockMailer();

        api = require('api');
        request = require('co-supertest').agent(api.callback());
        YT = require('models/yt');
    });

    after(() => {
        mockery.disable();
        mockery.deregisterAll();
    });

    beforeEach(function *() {
        yield dbHelper.clear();

        nockYT({
            create: { response: { statusCode: 200 } },
            proxy: { response: ['heavy-proxy'] },
            write: { response: 'write-body' }
        });

        nock(new URL(config.tracker.options.endpoint).origin)
            .post('/v2/issues')
            .reply(201);

        sinon.spy(YT, 'upload');

        yield userIdentificationsFactory.createWithRelations(
            userIdentification,
            { user, trialTemplate }
        );
    });

    afterEach(() => {
        YT.upload.restore();

        nock.cleanAll();
    });

    describe('`uploadTrialsToYT`', () => {
        it('should send trials to YT when they are too few and expired', function *() {
            nockProctorEdu.protocol({ openId: 'open-id-to-upload', response: protocol });

            trial.started = moment(Date.now())
                .subtract(2, 'days')
                .subtract(1, 'hours')
                .toDate();

            yield proctoringResponsesFactory.createWithRelations(
                proctoringResponse,
                { user, trial }
            );
            yield proctoringVideosFactory.createWithRelations(
                {
                    name: '1.webm',
                    startTime: 1514764830000, // '2018-01-01T00:00:30.000Z'
                    duration: 600000 // 10 min
                },
                { user, trial }
            );

            yield request
                .get(url)
                .expect(204);

            const response = yield ProctoringResponses.findOne({
                where: { trialId: trial.id },
                attributes: ['isSentToToloka'],
                raw: true
            });

            expect(YT.upload.called).to.be.true;
            expect(response.isSentToToloka).to.be.true;
        });

        it('should not send trials to YT when there are too few and are not expired', function *() {
            nockProctorEdu.protocol({ openId: 'open-id-to-upload', response: protocol });

            trial.started = new Date();

            yield proctoringResponsesFactory.createWithRelations(
                proctoringResponse,
                { user, trial }
            );
            yield proctoringVideosFactory.createWithRelations(
                {
                    name: '1.webm',
                    startTime: 1514764830000, // '2018-01-01T00:00:30.000Z'
                    duration: 600000 // 10 min
                },
                { user, trial }
            );

            yield request
                .get(url)
                .expect(204);

            const response = yield ProctoringResponses.findOne({
                where: { trialId: trial.id },
                attributes: ['isSentToToloka'],
                raw: true
            });

            expect(YT.upload.called).to.be.false;
            expect(response.isSentToToloka).to.be.false;
        });

        it('should send correct markup to yt', function *() {
            nockProctorEdu.protocol({ openId: 'open-id-to-upload', response: protocol });
            nockProctorEdu.protocol({ openId: 'open-id-to-upload-2', response: protocol });

            trial.started = moment(Date.now())
                .subtract(2, 'days')
                .subtract(1, 'hours')
                .toDate();

            yield proctoringResponsesFactory.createWithRelations(
                proctoringResponse,
                { user, trial }
            );
            yield proctoringVideosFactory.createWithRelations(
                {
                    name: '1.webm',
                    startTime: 1514764830000, // '2018-01-01T00:00:30.000Z'
                    duration: 600000 // 10 min
                },
                { user, trial }
            );

            const otherTrial = {
                id: 2,
                passed: 1,
                nullified: 0,
                filesStatus: 'saved',
                openId: 'open-id-to-upload-2',
                started: moment(Date.now()).subtract(2, 'hours').toDate()
            };
            const otherProctoringResponse = {
                source: 'toloka',
                verdict: 'failed',
                isSentToToloka: false,
                isRevisionRequested: true,
                isLast: true
            };

            yield proctoringResponsesFactory.createWithRelations(
                otherProctoringResponse,
                { user, trial: otherTrial }
            );
            yield proctoringVideosFactory.createWithRelations(
                {
                    name: '2.webm',
                    startTime: 1514764830000, // '2018-01-01T00:00:30.000Z'
                    duration: 600000 // 10 min
                },
                { user, trial: otherTrial }
            );

            yield request
                .get(url)
                .expect(204);

            const expectedRows = [
                {
                    trialId: 1,
                    userPhoto: 'https://yastatic.net/s3/expert/testing/faces/face',
                    videos: [
                        {
                            url: 'https://yastatic.net/s3/expert/testing/videos/1.webm',
                            intervals: [
                                { start: 30, end: 90 },
                                { start: 88, end: 270 }
                            ]
                        }
                    ],
                    isRevision: false
                },
                {
                    trialId: 2,
                    userPhoto: 'https://yastatic.net/s3/expert/testing/faces/face',
                    videos: [
                        {
                            url: 'https://yastatic.net/s3/expert/testing/videos/2.webm',
                            intervals: [
                                { start: 0, end: 30 },
                                { start: 30, end: 90 },
                                { start: 88, end: 270 }
                            ]
                        }
                    ],
                    isRevision: true
                }
            ];

            expect(YT.upload.withArgs(expectedRows).calledOnce).to.be.true;
        });

        // eslint-disable-next-line max-statements
        it('should send limited trials', function *() {
            nockProctorEdu.protocol({ openId: 'open-id-to-upload', response: protocol });
            nockProctorEdu.protocol({ openId: 'open-id-to-upload-2', response: protocol });
            nockProctorEdu.protocol({ openId: 'open-id-to-upload-3', response: protocol });

            trial.started = moment(new Date()).subtract(2, 'hour').toDate();

            const secondTrial = {
                id: 2,
                passed: 1,
                nullified: 0,
                filesStatus: 'saved',
                started: moment(new Date()).subtract(1, 'hour').toDate(),
                openId: 'open-id-to-upload-2'
            };
            const thirdTrial = {
                id: 3,
                passed: 1,
                nullified: 0,
                filesStatus: 'saved',
                started: new Date(),
                openId: 'open-id-to-upload-3'
            };

            yield proctoringResponsesFactory.createWithRelations(
                proctoringResponse,
                { user, trial }
            );
            yield proctoringResponsesFactory.createWithRelations(
                proctoringResponse,
                { user, trial: secondTrial }
            );
            yield proctoringResponsesFactory.createWithRelations(
                proctoringResponse,
                { user, trial: thirdTrial }
            );
            yield proctoringVideosFactory.createWithRelations(
                {
                    name: '1.webm',
                    startTime: 1514764830000, // '2018-01-01T00:00:30.000Z'
                    duration: 600000 // 10 min
                },
                { user, trial }
            );
            yield proctoringVideosFactory.createWithRelations(
                {
                    name: '2.webm',
                    startTime: 1514764830000, // '2018-01-01T00:00:30.000Z'
                    duration: 600000 // 10 min
                },
                { user, trial: secondTrial }
            );
            yield proctoringVideosFactory.createWithRelations(
                {
                    name: '3.webm',
                    startTime: 1514764830000, // '2018-01-01T00:00:30.000Z'
                    duration: 600000 // 10 min
                },
                { user, trial: thirdTrial }
            );

            yield request
                .get(url)
                .expect(204);

            const expectedRows = [
                {
                    trialId: 1,
                    userPhoto: 'https://yastatic.net/s3/expert/testing/faces/face',
                    videos: [
                        {
                            url: 'https://yastatic.net/s3/expert/testing/videos/1.webm',
                            intervals: [
                                { start: 30, end: 90 },
                                { start: 88, end: 270 }
                            ]
                        }
                    ],
                    isRevision: false
                },
                {
                    trialId: 2,
                    userPhoto: 'https://yastatic.net/s3/expert/testing/faces/face',
                    videos: [
                        {
                            url: 'https://yastatic.net/s3/expert/testing/videos/2.webm',
                            intervals: [
                                { start: 30, end: 90 },
                                { start: 88, end: 270 }
                            ]
                        }
                    ],
                    isRevision: false
                }
            ];
            const responses = yield ProctoringResponses.findAll({
                attributes: ['isSentToToloka'],
                order: [['time']],
                raw: true
            });

            expect(YT.upload.withArgs(expectedRows).calledOnce).to.be.true;
            expect(responses.map(res => res.isSentToToloka)).to.deep.equal([true, true, false]);
        });

        it('should set `isSentToToloka` for trials without video', function *() {
            nockProctorEdu.protocol({ openId: 'open-id-to-upload', response: protocol });

            trial.started = new Date();

            yield proctoringResponsesFactory.createWithRelations(
                proctoringResponse,
                { user, trial }
            );

            yield request
                .get(url)
                .expect(204);

            const response = yield ProctoringResponses.findOne({
                where: { trialId: trial.id },
                attributes: ['isSentToToloka'],
                raw: true
            });

            expect(YT.upload.called).to.be.false;
            expect(response.isSentToToloka).to.be.true;
        });

        it('should set `isSentToToloka` for trials without session data', function *() {
            nockProctorEdu.protocol({ openId: 'open-id-to-upload', code: 500 });

            trial.started = new Date();

            yield proctoringResponsesFactory.createWithRelations(
                proctoringResponse,
                { user, trial }
            );

            yield request
                .get(url)
                .expect(204);

            const response = yield ProctoringResponses.findOne({
                where: { trialId: trial.id },
                attributes: ['isSentToToloka'],
                raw: true
            });

            expect(YT.upload.called).to.be.false;
            expect(response.isSentToToloka).to.be.true;
        });

        it('should not send trials when there are no video for expired trial', function *() {
            nockProctorEdu.protocol({ openId: 'open-id-to-upload', response: protocol });
            nockProctorEdu.protocol({ openId: 'open-id-to-upload-2', response: protocol });

            trial.started = moment(Date.now()).subtract(2, 'days').subtract(1, 'hours').toDate();

            const secondTrial = {
                id: 2,
                passed: 1,
                nullified: 0,
                filesStatus: 'saved',
                started: new Date(),
                openId: 'open-id-to-upload-2'
            };

            yield proctoringResponsesFactory.createWithRelations(
                proctoringResponse,
                { user, trial }
            );
            yield proctoringResponsesFactory.createWithRelations(
                proctoringResponse,
                { user, trial: secondTrial }
            );
            yield proctoringVideosFactory.createWithRelations(
                {
                    name: '2.webm',
                    startTime: 1514764830000, // '2018-01-01T00:00:30.000Z'
                    duration: 600000 // 10 min
                },
                { user, trial: secondTrial }
            );

            yield request
                .get(url)
                .expect(204);

            const responses = yield ProctoringResponses.findAll({
                attributes: ['isSentToToloka'],
                order: [['time']],
                raw: true
            });

            expect(YT.upload.called).to.be.false;
            expect(responses.map(res => res.isSentToToloka)).to.deep.equal([true, false]);
        });

        // eslint-disable-next-line max-statements, max-len
        it('should failed trials when metrics from proctoring are high and it is not revision', function *() {
            const sessionData = {
                averages: {
                    b2: 91,
                    b3: 90,
                    b4: 98,
                    c1: 0,
                    c2: 20,
                    c3: 0,
                    c4: 43,
                    c5: 98,
                    m1: 0,
                    m2: 6,
                    n1: 0
                }
            };

            nockSeveralUids({
                uid: '1234567890',
                userip: ip.address(),
                response: {
                    users: [
                        {
                            uid: { value: 1234567890 },
                            'address-list': [
                                { address: 'email@yandex.ru' }
                            ]
                        }
                    ]
                }
            });

            nockProctorEdu.protocol({ openId: 'open-id-to-upload', response: sessionData });
            nockProctorEdu.protocol({ openId: 'open-id-to-upload-2', response: sessionData });

            nock(config.sender.host)
                .post(/\/api\/0\/sales\/transactional\/[A-Z0-9-]+\/send/)
                .query(true)
                .times(Infinity)
                .reply(200, {});

            trial.started = moment(Date.now()).subtract(2, 'days').subtract(1, 'hours').toDate();

            yield proctoringResponsesFactory.createWithRelations(
                proctoringResponse,
                { user, trial }
            );
            yield proctoringVideosFactory.createWithRelations(
                {
                    name: '1.webm',
                    startTime: 1514764830000, // '2018-01-01T00:00:30.000Z'
                    duration: 600000 // 10 min
                },
                { user, trial }
            );

            const otherTrial = {
                id: 2,
                passed: 1,
                nullified: 0,
                filesStatus: 'saved',
                openId: 'open-id-to-upload-2',
                started: moment(Date.now()).subtract(2, 'hours').toDate()
            };
            const otherProctoringResponse = {
                source: 'proctoring',
                verdict: 'failed',
                isSentToToloka: false,
                isRevisionRequested: true,
                isLast: true
            };

            yield proctoringResponsesFactory.createWithRelations(
                otherProctoringResponse,
                { user, trial: otherTrial }
            );
            yield proctoringVideosFactory.createWithRelations(
                {
                    name: '2.webm',
                    startTime: 1514764830000, // '2018-01-01T00:00:30.000Z'
                    duration: 600000 // 10 min
                },
                { user, trial: otherTrial }
            );

            yield request
                .get(url)
                .expect(204);

            const responses = yield ProctoringResponses.findAll({
                attributes: ['trialId', 'source', 'verdict', 'isLast'],
                order: [
                    ['trialId'],
                    ['time']
                ],
                raw: true
            });
            const expected = [
                { trialId: 1, source: 'proctoring', verdict: 'pending', isLast: false },
                { trialId: 1, source: 'metrics', verdict: 'failed', isLast: true },
                { trialId: 2, source: 'proctoring', verdict: 'failed', isLast: true }
            ];

            expect(YT.upload.called).to.be.false;
            expect(responses).to.deep.equal(expected);
        });
    });
});
