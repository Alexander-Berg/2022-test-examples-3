require('co-mocha');

const { expect } = require('chai');
const _ = require('lodash');

const dbHelper = require('tests/helpers/clear');

const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');
const trialsFactory = require('tests/factory/trialsFactory');

const ProctoringResponseModel = require('models/proctoringResponse');
const { ProctoringResponses } = require('db/postgres');

describe('ProctoringResponse model', () => {
    beforeEach(function *() {
        yield dbHelper.clear();
        yield trialsFactory.createWithRelations({ id: 1 });
    });

    describe('`create`', () => {
        it('should create new proctoringResponse', function *() {
            yield ProctoringResponseModel.create(1, [{
                source: 'proctoring',
                verdict: 'correct',
                evaluation: 45,
                additionalInfo: [{
                    start: 0,
                    end: 10
                }]
            }]);

            const actual = yield ProctoringResponses.findOne({ raw: true });

            expect(actual.trialId).to.equal(1);
            expect(actual.source).to.equal('proctoring');
            expect(actual.verdict).to.equal('correct');
            expect(actual.evaluation).to.equal(45);
            expect(actual.time).to.not.be.null;
            expect(actual.isLast).to.be.true;
            expect(actual.additionalInfo).to.deep.equal([{
                start: 0,
                end: 10
            }]);
        });

        it('should set `evaluation` and `additionalInfo` to null when it is unknown', function *() {
            yield ProctoringResponseModel.create(1, [{
                source: 'proctoring',
                verdict: 'pending'
            }]);

            const actual = yield ProctoringResponses.findOne({
                attributes: ['evaluation', 'additionalInfo'],
                raw: true
            });

            expect(actual.evaluation).to.be.null;
            expect(actual.additionalInfo).to.be.null;
        });

        it('should set `isLast` to false for previous proctoringResponse for current trial', function *() {
            yield proctoringResponsesFactory.createWithRelations({
                id: 1,
                source: 'proctoring',
                verdict: 'pending',
                isLast: true
            }, { trial: { id: 1 } });
            yield proctoringResponsesFactory.createWithRelations({
                id: 2,
                source: 'proctoring',
                verdict: 'correct',
                isLast: true,
                time: new Date(2016, 1, 2, 3, 4, 5)
            }, { trial: { id: 2 } });

            yield ProctoringResponseModel.create(1, [{
                source: 'toloka',
                verdict: 'correct'
            }]);

            const anotherTrialProctoring = yield ProctoringResponses.findOne({
                where: { id: 2 },
                raw: true
            });

            expect(anotherTrialProctoring.isLast).to.be.true;

            const actual = yield ProctoringResponses.findAll({
                where: { trialId: 1 },
                order: [['time']],
                raw: true
            });

            expect(actual).to.have.length(2);
            expect(actual[0].isLast).to.be.false;
            expect(actual[1].isLast).to.be.true;
        });

        it('should not create new proctoringResponse when proctoring data array is empty', function *() {
            yield ProctoringResponseModel.create(1, []);
            const actual = yield ProctoringResponses.findAll();

            expect(actual).to.deep.equal([]);
        });
    });

    describe('`tryFindLast`', () => {
        it('should return null if proctoring response not found', function *() {
            const actual = yield ProctoringResponseModel.tryFindLast(1);

            expect(actual).to.be.null;
        });

        it('should return last response', function *() {
            const verdictTime = new Date(2018, 1, 2, 3, 4, 5);

            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'pending',
                isLast: false
            }, { trial: { id: 1 } });
            yield proctoringResponsesFactory.createWithRelations({
                source: 'toloka',
                verdict: 'failed',
                isLast: true,
                time: verdictTime
            }, { trial: { id: 1 } });

            const proctoringResponse = yield ProctoringResponseModel.tryFindLast(1);
            const actual = proctoringResponse.toJSON();

            expect(actual).to.deep.equal({
                evaluation: null,
                trialId: 1,
                source: 'toloka',
                verdict: 'failed',
                time: verdictTime,
                isLast: true
            });
        });
    });

    describe('`setSentToTolokaByTrialIds`', () => {
        const trial = { id: 1 };
        const otherTrial = { id: 2 };

        it('should set `isSentToToloka` only for last trials', function *() {
            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'pending',
                isSentToToloka: false,
                isRevisionRequested: false,
                isLast: true
            }, { trial });

            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'pending',
                isSentToToloka: false,
                isRevisionRequested: false,
                isLast: false
            }, { trial: otherTrial });

            yield ProctoringResponseModel.setSentToTolokaByTrialIds([trial.id, otherTrial.id]);

            const responses = yield ProctoringResponses.findAll({
                attributes: ['isSentToToloka'],
                raw: true,
                order: [['trialId']]
            });

            expect(responses).to.deep.equal([
                { isSentToToloka: true },
                { isSentToToloka: false }
            ]);
        });

        it('should set field `isSentToToloka` for proctoring source', function *() {
            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'pending',
                isSentToToloka: false,
                isRevisionRequested: false,
                isLast: true
            }, { trial });

            yield proctoringResponsesFactory.createWithRelations({
                source: 'appeal',
                verdict: 'pending',
                isSentToToloka: false,
                isRevisionRequested: false,
                isLast: true
            }, { trial: otherTrial });

            yield ProctoringResponseModel.setSentToTolokaByTrialIds([trial.id, otherTrial.id]);

            const responses = yield ProctoringResponses.findAll({
                attributes: ['isSentToToloka'],
                raw: true,
                order: [['trialId']]
            });

            expect(responses).to.deep.equal([
                { isSentToToloka: true },
                { isSentToToloka: false }
            ]);
        });

        it('should set field `isSentToToloka` for pending trials', function *() {
            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'pending',
                isSentToToloka: false,
                isRevisionRequested: false,
                isLast: true
            }, { trial });

            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'failed',
                isSentToToloka: false,
                isRevisionRequested: false,
                isLast: true
            }, { trial: otherTrial });

            yield ProctoringResponseModel.setSentToTolokaByTrialIds([trial.id, otherTrial.id]);

            const responses = yield ProctoringResponses.findAll({
                attributes: ['isSentToToloka'],
                raw: true,
                order: [['trialId']]
            });

            expect(responses).to.deep.equal([
                { isSentToToloka: true },
                { isSentToToloka: false }
            ]);
        });

        it('should set field `isSentToToloka` for specified trials', function *() {
            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'pending',
                isSentToToloka: false,
                isRevisionRequested: false,
                isLast: true
            }, { trial });

            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'pending',
                isSentToToloka: false,
                isRevisionRequested: false,
                isLast: true
            }, { trial: otherTrial });

            yield ProctoringResponseModel.setSentToTolokaByTrialIds([trial.id]);

            const responses = yield ProctoringResponses.findAll({
                attributes: ['isSentToToloka'],
                raw: true,
                order: [['trialId']]
            });

            expect(responses).to.deep.equal([
                { isSentToToloka: true },
                { isSentToToloka: false }
            ]);
        });

        it('should set field `isSentToToloka` for trials with request to revision', function *() {
            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'failed',
                isSentToToloka: false,
                isRevisionRequested: true,
                isLast: true
            }, { trial });

            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'failed',
                isSentToToloka: false,
                isRevisionRequested: false,
                isLast: true
            }, { trial: otherTrial });

            yield ProctoringResponseModel.setSentToTolokaByTrialIds([trial.id, otherTrial.id]);

            const responses = yield ProctoringResponses.findAll({
                attributes: ['isSentToToloka'],
                raw: true,
                order: [['trialId']]
            });

            expect(responses).to.deep.equal([
                { isSentToToloka: true },
                { isSentToToloka: false }
            ]);
        });

        it('should do nothing when specified trial do not exist', function *() {
            yield ProctoringResponseModel.setSentToTolokaByTrialIds([trial.id]);

            const responses = yield ProctoringResponses.findAll();

            expect(responses).to.deep.equal([]);
        });
    });

    describe('findByTrialIds', () => {
        it('should return all records', function *() {
            const firstResponse = {
                source: 'proctoring',
                verdict: 'pending',
                time: new Date(2018, 1, 1),
                isSentToToloka: true,
                isRevisionRequested: false,
                isLast: false
            };
            const secondResponse = {
                source: 'toloka',
                verdict: 'failed',
                time: new Date(2018, 2, 2),
                isSentToToloka: false,
                isRevisionRequested: true,
                isLast: true
            };
            const thirdResponse = {
                source: 'proctoring',
                verdict: 'correct',
                time: new Date(2018, 4, 4),
                isSentToToloka: false,
                isRevisionRequested: false,
                isLast: true
            };

            yield proctoringResponsesFactory.createWithRelations(firstResponse, { trial: { id: 3 } });
            yield proctoringResponsesFactory.createWithRelations(secondResponse, { trial: { id: 3 } });
            yield proctoringResponsesFactory.createWithRelations(thirdResponse, { trial: { id: 1 } });
            yield proctoringResponsesFactory.createWithRelations({
                source: 'proctoring',
                verdict: 'failed',
                time: new Date(2018, 2, 2),
                isSentToToloka: false,
                isRevisionRequested: false
            }, { trial: { id: 2 } });

            const actual = yield ProctoringResponseModel.findByTrialIds([1, 3]);

            const expected = [
                _.assign({}, { trialId: 3 }, firstResponse),
                _.assign({}, { trialId: 3 }, secondResponse),
                _.assign({}, { trialId: 1 }, thirdResponse)
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should return `[]` when trials without proctoring', function *() {
            const actual = yield ProctoringResponseModel.findByTrialIds([1, 2, 3]);

            expect(actual).to.deep.equal([]);
        });
    });

    describe('`setIsRevisionRequested`', () => {
        it('should set field `isRevisionRequested` by `trialId`', function *() {
            yield proctoringResponsesFactory.createWithRelations({
                id: 1,
                source: 'proctoring',
                verdict: 'failed',
                isLast: true
            }, { trial: { id: 1 } });
            yield proctoringResponsesFactory.createWithRelations({
                id: 2,
                source: 'proctoring',
                verdict: 'failed',
                isLast: true
            }, { trial: { id: 2 } });

            yield ProctoringResponseModel.setIsRevisionRequested(2);

            const responses = yield ProctoringResponses.findAll({
                attributes: ['isRevisionRequested'],
                raw: true,
                order: [['id']]
            });

            expect(responses).to.deep.equal([
                { isRevisionRequested: false },
                { isRevisionRequested: true }
            ]);
        });

        it('should set field `isRevisionRequested` by `isLast`', function *() {
            const trial = { id: 1 };

            yield proctoringResponsesFactory.createWithRelations({
                id: 1,
                source: 'proctoring',
                verdict: 'pending',
                isLast: false
            }, { trial });
            yield proctoringResponsesFactory.createWithRelations({
                id: 2,
                source: 'toloka',
                verdict: 'failed',
                isLast: true
            }, { trial });

            yield ProctoringResponseModel.setIsRevisionRequested(1);

            const responses = yield ProctoringResponses.findAll({
                attributes: ['isRevisionRequested'],
                raw: true,
                order: [['id']]
            });

            expect(responses).to.deep.equal([
                { isRevisionRequested: false },
                { isRevisionRequested: true }
            ]);
        });

        it('should do nothing when there no suitable records', function *() {
            yield proctoringResponsesFactory.createWithRelations({
                id: 1,
                source: 'proctoring',
                verdict: 'failed',
                isLast: true
            }, { trial: { id: 1 } });

            yield ProctoringResponseModel.setIsRevisionRequested(3);

            const responses = yield ProctoringResponses.findAll({
                attributes: ['isRevisionRequested', 'trialId'],
                raw: true
            });

            expect(responses).to.deep.equal([{ isRevisionRequested: false, trialId: 1 }]);
        });
    });

    describe('`getAdditionalInfo`', () => {
        it('should filter data by `trialId`', function *() {
            const firstData = {
                start: 0,
                end: 25,
                answers: [
                    { noVioAudioProblems: false, vioCheating: true },
                    { vioWalkAwayScreen: false, vioDiffUser: false }
                ]
            };
            const secondData = {
                start: 13,
                end: 67,
                answers: [
                    { vioOther: false, vioOtherText: 'vse ploho' }
                ]
            };

            yield proctoringResponsesFactory.createWithRelations(
                { additionalInfo: firstData, source: 'toloka' },
                { trial: { id: 1 } }
            );
            yield proctoringResponsesFactory.createWithRelations(
                { additionalInfo: secondData, source: 'toloka' },
                { trial: { id: 2 } }
            );

            const actual = yield ProctoringResponseModel.getAdditionalInfo(1);

            expect(actual).to.deep.equal([{
                source: 'toloka',
                additionalInfo: firstData
            }]);
        });

        it('should return all data', function *() {
            const firstData = {
                start: 0,
                end: 25,
                answers: [
                    { noVioAudioProblems: false, vioCheating: true },
                    { vioWalkAwayScreen: false, vioDiffUser: false }
                ]
            };
            const secondData = {
                start: 13,
                end: 67,
                answers: [
                    { vioOther: false, vioOtherText: 'vse ploho' }
                ]
            };
            const trial = { id: 1 };

            yield proctoringResponsesFactory.createWithRelations(
                { source: 'proctoring', additionalInfo: null, time: new Date(1, 1, 1) },
                { trial }
            );
            yield proctoringResponsesFactory.createWithRelations(
                { source: 'toloka', additionalInfo: firstData, time: new Date(2, 2, 2) },
                { trial }
            );
            yield proctoringResponsesFactory.createWithRelations(
                { source: 'toloka-revision', additionalInfo: secondData, time: new Date(3, 3, 3) },
                { trial }
            );

            const actual = yield ProctoringResponseModel.getAdditionalInfo(1);

            expect(actual).to.deep.equal([
                { source: 'toloka', additionalInfo: firstData },
                { source: 'toloka-revision', additionalInfo: secondData }
            ]);
        });
    });

    describe('`findLastByResponses`', () => {
        it('should find last response', () => {
            const lastResponse = {
                source: 'toloka',
                verdict: 'failed',
                time: new Date(2018, 2, 2),
                isSentToToloka: false,
                isRevisionRequested: true,
                isLast: true
            };
            const responses = [
                {
                    source: 'proctoring',
                    verdict: 'pending',
                    time: new Date(2018, 1, 1),
                    isSentToToloka: true,
                    isRevisionRequested: false,
                    isLast: false
                },
                lastResponse
            ];

            const actual = ProctoringResponseModel.findLastByResponses(responses);
            const expected = lastResponse;

            expect(actual).to.deep.equal(expected);
        });

        it('should find last response if it is not last in list', () => {
            const lastResponse = {
                source: 'crit-metrics',
                verdict: 'failed',
                time: new Date(2018, 2, 2),
                isSentToToloka: false,
                isRevisionRequested: true,
                isLast: true
            };
            const responses = [
                lastResponse,
                {
                    source: 'proctoring',
                    verdict: 'failed',
                    time: new Date(2018, 2, 2),
                    isSentToToloka: true,
                    isRevisionRequested: false,
                    isLast: false
                }
            ];

            const actual = ProctoringResponseModel.findLastByResponses(responses);
            const expected = lastResponse;

            expect(actual).to.deep.equal(expected);
        });
    });

    it('should return undefined if there is no last response', () => {
        const responses = [
            {
                source: 'crit-metrics',
                verdict: 'failed',
                time: new Date(2018, 2, 2),
                isSentToToloka: false,
                isRevisionRequested: true,
                isLast: false
            },
            {
                source: 'proctoring',
                verdict: 'failed',
                time: new Date(2018, 2, 2),
                isSentToToloka: true,
                isRevisionRequested: false,
                isLast: false
            }
        ];

        const actual = ProctoringResponseModel.findLastByResponses(responses);

        expect(actual).to.be.undefined;
    });
});
