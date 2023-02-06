require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const nock = require('nock');
const sinon = require('sinon');
const config = require('yandex-config');

const Notifier = require('models/notifier');

const { ProctoringResponses, Certificate } = require('db/postgres');
const dbHelper = require('tests/helpers/clear');
const nockYT = require('tests/helpers/yt');
const nockAvatars = require('tests/helpers/mdsServices').avatars;
const trialsFactory = require('tests/factory/trialsFactory');

describe('`loadTolokaResults`', () => {
    const correctAnswer = { statusCode: 200 };
    const writeBody = 'write-body';
    const heavyProxy = 'heavy-proxy';
    const directory = ['2018-07-31_13:26:35'];

    function *createTrials(trialIds, relations) {
        for (const id of trialIds) {
            yield trialsFactory.createWithRelations({ id }, relations);
        }
    }

    function getMap(rows) {
        return rows
            .map(JSON.stringify)
            .join('\n');
    }

    beforeEach(function *() {
        yield dbHelper.clear();

        nockAvatars.success();

        sinon.spy(Notifier, 'sendLetterToUser');
    });

    afterEach(() => {
        Notifier.sendLetterToUser.restore();

        nock.cleanAll();
    });

    it('should create certificate only when Toloka`s verdict is OK', function *() {
        const rows = [
            {
                trialId: 1,
                start: 0,
                end: 30,
                violations: false,
                isRevision: false
            },
            {
                trialId: 1,
                start: 31,
                end: 60,
                violations: true,
                isRevision: false
            },
            {
                trialId: 2,
                start: 0,
                end: 30,
                violations: false,
                isRevision: false
            }
        ];

        nockYT({
            create: { response: correctAnswer },
            proxy: { response: [heavyProxy] },
            write: { response: writeBody },
            list: { response: directory },
            read: { response: getMap(rows) },
            remove: { response: correctAnswer }
        });

        yield createTrials([1, 2], {});

        yield request
            .get('/v1/yt/loadTolokaResults')
            .expect(204);

        const proctoringResponses = yield ProctoringResponses.findAll({
            attributes: ['trialId', 'source', 'verdict'],
            order: [['trialId']],
            raw: true
        });
        const certificates = yield Certificate.findAll({
            where: { trialId: { $in: [1, 2] } },
            attributes: ['trialId'],
            raw: true
        });

        expect(proctoringResponses).to.deep.equal([
            {
                trialId: 1,
                source: 'toloka',
                verdict: 'failed'
            },
            {
                trialId: 2,
                source: 'toloka',
                verdict: 'correct'
            }
        ]);
        expect(certificates).to.deep.equal([{ trialId: 2 }]);
    });

    it('should not create certificate when Toloka`s verdict is not OK', function *() {
        const rows = [
            {
                trialId: 1,
                start: 0,
                end: 30,
                violations: false,
                isRevision: false
            },
            {
                trialId: 1,
                start: 31,
                end: 60,
                violations: true,
                isRevision: false
            }
        ];

        nockYT({
            create: { response: correctAnswer },
            proxy: { response: [heavyProxy] },
            write: { response: writeBody },
            list: { response: directory },
            read: { response: getMap(rows) },
            remove: { response: correctAnswer }
        });

        yield createTrials([1], {});

        yield request
            .get('/v1/yt/loadTolokaResults')
            .expect(204);

        const certificates = yield Certificate.findAll();

        expect(certificates).to.deep.equal([]);
    });

    it('should write correct source to proctoring responses', function *() {
        const rows = [
            {
                trialId: 1,
                start: 0,
                end: 30,
                violations: false,
                isRevision: true
            },
            {
                trialId: 2,
                start: 31,
                end: 60,
                violations: true,
                isRevision: false
            }
        ];

        nockYT({
            create: { response: correctAnswer },
            proxy: { response: [heavyProxy] },
            write: { response: writeBody },
            list: { response: directory },
            read: { response: getMap(rows) },
            remove: { response: correctAnswer }
        });

        nock(config.sender.host)
            .post(/\/api\/0\/sales\/transactional\/[A-Z0-9-]+\/send/)
            .query(true)
            .times(Infinity)
            .reply(200, {});

        yield createTrials([1, 2], { user: { id: 2, email: 'example@ya.ru' } });

        yield request
            .get('/v1/yt/loadTolokaResults')
            .expect(204);

        const proctoringResponses = yield ProctoringResponses.findAll({
            attributes: ['trialId', 'source', 'verdict'],
            order: [['trialId']],
            raw: true
        });

        expect(proctoringResponses).to.deep.equal([
            {
                trialId: 1,
                source: 'toloka-revision',
                verdict: 'correct'
            },
            {
                trialId: 2,
                source: 'toloka',
                verdict: 'failed'
            }
        ]);

        expect(Notifier.sendLetterToUser.calledOnce).to.be.true;
        expect(Notifier.sendLetterToUser.calledWith('example@ya.ru', 'correct')).to.be.true;
    });

    it('should write additional info to proctoring responses', function *() {
        const rows = [
            {
                trialId: 1,
                start: 0,
                end: 30,
                violations: false,
                isRevision: true,
                'no_vio_audio_problems': false,
                'no_vio_no_relate': false,
                'no_vio_other': false,
                'no_vio_other_text': null,
                'no_vio_video_problems': false,
                'vio_cheating': false,
                'vio_diff_user': false,
                'vio_other': false,
                'vio_other_people': false,
                'vio_other_text': null,
                'vio_tips': false,
                'vio_walk_away_screen': false
            }
        ];

        nockYT({
            create: { response: correctAnswer },
            proxy: { response: [heavyProxy] },
            write: { response: writeBody },
            list: { response: directory },
            read: { response: getMap(rows) },
            remove: { response: correctAnswer }
        });

        nock(config.sender.host)
            .post(/\/api\/0\/sales\/transactional\/[A-Z0-9-]+\/send/)
            .query(true)
            .times(Infinity)
            .reply(200, {});

        yield createTrials([1], { user: { id: 2, email: 'example@ya.ru' } });

        yield request
            .get('/v1/yt/loadTolokaResults')
            .expect(204);

        const proctoringResponses = yield ProctoringResponses.findAll({
            attributes: ['trialId', 'source', 'verdict', 'additionalInfo'],
            order: [['trialId']],
            raw: true
        });

        expect(proctoringResponses).to.deep.equal([
            {
                trialId: 1,
                source: 'toloka-revision',
                verdict: 'correct',
                additionalInfo: [
                    {
                        start: 0,
                        end: 30,
                        hasViolations: false,
                        answers: [
                            {
                                noVioAudioProblems: false,
                                noVioNoRelate: false,
                                noVioOther: false,
                                noVioOtherText: null,
                                noVioVideoProblems: false,
                                vioCheating: false,
                                vioDiffUser: false,
                                vioOther: false,
                                vioOtherPeople: false,
                                vioOtherText: null,
                                vioTips: false,
                                vioWalkAwayScreen: false,
                                violations: false
                            }
                        ]
                    }
                ]
            }
        ]);
    });

    it('should throw 500 when can not move toloka responses to YT archive', function *() {
        const rows = [
            {
                trialId: 1,
                start: 0,
                end: 30,
                violations: false,
                isRevision: true
            }
        ];

        nockYT({
            list: { response: directory },
            read: { response: getMap(rows) },
            proxy: { response: [heavyProxy] },
            write: { code: 500 }
        });

        yield request
            .get('/v1/yt/loadTolokaResults')
            .expect(500);
    });
});
