require('co-mocha');

const { expect } = require('chai');

const dbHelper = require('tests/helpers/clear');

const ProctoringVideosModel = require('models/proctoringVideos');
const { ProctoringVideos } = require('db/postgres');
const trialsFactory = require('tests/factory/trialsFactory');
const proctoringVideosFactory = require('tests/factory/proctoringVideosFactory');

describe('ProctoringVideos Model', () => {
    beforeEach(dbHelper.clear);

    describe('`create`', () => {
        const trial = { id: 123 };

        beforeEach(function *() {
            yield trialsFactory.createWithRelations(trial);
        });

        it('should create proctoring videos records', function *() {
            const proctoringVideos = [
                {
                    name: 'video1.webm',
                    startTime: 4567838476,
                    duration: 46789,
                    source: 'webcam'
                },
                {
                    name: 'video2.webm',
                    startTime: 28746891984,
                    duration: 134715,
                    source: 'screen'
                }
            ];

            yield ProctoringVideosModel.create(trial.id, proctoringVideos);

            const actual = yield ProctoringVideos.findAll({
                attributes: ['trialId', 'name', 'startTime', 'duration', 'source'],
                order: [['name']],
                raw: true
            });

            const expected = [
                {
                    trialId: 123,
                    name: 'video1.webm',
                    startTime: 4567838476,
                    duration: 46789,
                    source: 'webcam'
                },
                {
                    trialId: 123,
                    name: 'video2.webm',
                    startTime: 28746891984,
                    duration: 134715,
                    source: 'screen'
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should do nothing when videos list is empty', function *() {
            yield ProctoringVideosModel.create(trial.id, []);

            const actual = yield ProctoringVideos.findAll();

            expect(actual).to.deep.equal([]);
        });
    });

    describe('`getVideosByTrial`', () => {
        it('should return all videos grouped by trial id', function *() {
            const firstTrial = { id: 1 };
            const secondTrial = { id: 2 };

            yield proctoringVideosFactory.createWithRelations(
                { name: '11', startTime: 5678, duration: 3345, source: 'webcam' },
                { trial: firstTrial }
            );
            yield proctoringVideosFactory.createWithRelations(
                { name: '12', startTime: 2846, duration: 5335, source: 'webcam' },
                { trial: firstTrial }
            );
            yield proctoringVideosFactory.createWithRelations(
                { name: '21', startTime: 7462, duration: 9278, source: 'webcam' },
                { trial: secondTrial }
            );

            const actual = yield ProctoringVideosModel.getVideosByTrial([1, 2], ['webcam']);

            const expected = {
                1: [
                    { trialId: 1, name: '12', startTime: 2846, duration: 5335 },
                    { trialId: 1, name: '11', startTime: 5678, duration: 3345 }
                ],
                2: [
                    { trialId: 2, name: '21', startTime: 7462, duration: 9278 }
                ]
            };

            expect(actual).to.deep.equal(expected);
        });

        it('should return videos only for specified trial', function *() {
            yield proctoringVideosFactory.createWithRelations(
                { name: '11', startTime: 5678, duration: 3345, source: 'webcam' },
                { trial: { id: 1 } }
            );
            yield proctoringVideosFactory.createWithRelations(
                { name: '12', startTime: 2846, duration: 5335, source: 'webcam' },
                { trial: { id: 2 } }
            );

            const actual = yield ProctoringVideosModel.getVideosByTrial([2], ['webcam']);

            const expected = {
                2: [
                    { trialId: 2, name: '12', startTime: 2846, duration: 5335 }
                ]
            };

            expect(actual).to.deep.equal(expected);
        });

        it('should return videos only for specified sources', function *() {
            const trial = { id: 1 };

            yield proctoringVideosFactory.createWithRelations(
                { name: '11', startTime: 5678, duration: 3345, source: 'webcam' },
                { trial }
            );
            yield proctoringVideosFactory.createWithRelations(
                { name: '12', startTime: 2846, duration: 5335, source: 'screen' },
                { trial }
            );

            const actual = yield ProctoringVideosModel.getVideosByTrial([1, 2], ['screen']);

            const expected = {
                1: [
                    { trialId: 1, name: '12', startTime: 2846, duration: 5335 }
                ]
            };

            expect(actual).to.deep.equal(expected);
        });

        it('should return {} when no video for specified trial', function *() {
            const actual = yield ProctoringVideosModel.getVideosByTrial([2], ['webcam']);

            expect(actual).to.deep.equal({});
        });
    });

    describe('`getVideosNames`', () => {
        it('should return several names of videos for trial in correct order', function *() {
            const trial = { id: 3 };

            yield proctoringVideosFactory.createWithRelations(
                { name: '11', startTime: 111, source: 'webcam' },
                { trial }
            );
            yield proctoringVideosFactory.createWithRelations(
                { name: '22', startTime: 22, source: 'webcam' },
                { trial }
            );

            const actual = yield ProctoringVideosModel.getVideosNames(3, ['webcam']);

            expect(actual).to.deep.equal(['22', '11']);
        });

        it('should return names of videos only for current trial', function *() {
            yield proctoringVideosFactory.createWithRelations(
                { name: '11', source: 'webcam' },
                { trial: { id: 1 } }
            );
            yield proctoringVideosFactory.createWithRelations(
                { name: '12', source: 'webcam' },
                { trial: { id: 2 } }
            );

            const actual = yield ProctoringVideosModel.getVideosNames(1, ['webcam']);

            expect(actual).to.deep.equal(['11']);
        });

        it('should return names of videos only for specified sources', function *() {
            const trial = { id: 1 };

            yield proctoringVideosFactory.createWithRelations(
                { name: '11', source: 'webcam' },
                { trial }
            );
            yield proctoringVideosFactory.createWithRelations(
                { name: '12', source: 'screen' },
                { trial }
            );

            const actual = yield ProctoringVideosModel.getVideosNames(1, ['screen']);

            expect(actual).to.deep.equal(['12']);
        });

        it('should return `[]` when video does not exist', function *() {
            const actual = yield ProctoringVideosModel.getVideosNames(1, ['webcam']);

            expect(actual).to.deep.equal([]);
        });
    });
});
