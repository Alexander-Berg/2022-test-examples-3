require('co-mocha');

const _ = require('lodash');
const { expect } = require('chai');
const moment = require('moment');
const freezingTime = require('yandex-config').freezing.time;

const dbHelper = require('tests/helpers/clear');
const freezingFactory = require('tests/factory/freezingFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');

const FreezingModel = require('models/freezing');
const { Freezing } = require('db/postgres');

describe('Freezing model', () => {
    beforeEach(function *() {
        yield dbHelper.clear();
    });

    describe('`freeze`', () => {
        it('should do nothing if trialTemplateIds is empty', function *() {
            yield FreezingModel.freeze(1234567890);
            const freezingData = yield Freezing.findAll();

            expect(freezingData).to.be.empty;
        });

        it('should freeze exams', function *() {
            const now = moment().startOf('second').toDate();
            const examIds = [1, 2];

            yield trialTemplatesFactory.createWithRelations({ id: 1 });
            yield trialTemplatesFactory.createWithRelations({ id: 2 });

            const finishTime = yield FreezingModel.freeze(1234567890, examIds);
            const freezingData = yield Freezing.findAll({
                attributes: ['frozenBy', 'trialTemplateId', 'startTime', 'finishTime'],
                order: [['trialTemplateId', 'ASC']]
            });

            expect(freezingData.length).to.equal(2);
            expect(freezingData[0].get('frozenBy')).to.equal(1234567890);
            expect(freezingData[0].get('trialTemplateId')).to.equal(1);
            expect(freezingData[1].get('trialTemplateId')).to.equal(2);

            const actualStartTime = moment(freezingData[0].get('startTime')).toDate();
            const actualFinishTime = moment(freezingData[0].get('finishTime')).toDate();

            expect(actualStartTime).to.be.at.least(now);
            expect(actualFinishTime).to.be.at.least(now);
            expect(finishTime).to.deep.equal(actualFinishTime);
        });
    });

    describe('`unfreeze`', () => {
        it('should update finishTime for frozen exams', function *() {
            const startTime = moment(new Date());
            const finishTime = startTime.add(freezingTime, 'ms').toDate();

            yield freezingFactory.createWithRelations(
                { frozenBy: 1234567890, startTime: startTime.add(1, 'h'), finishTime },
                { trialTemplate: { id: 1 } }
            );

            yield freezingFactory.createWithRelations(
                { frozenBy: 1234567890, startTime: startTime.add(1, 'h'), finishTime },
                { trialTemplate: { id: 2 } }
            );

            yield freezingFactory.createWithRelations(
                { frozenBy: 1234567890, startTime, finishTime },
                { trialTemplate: { id: 3 } }
            );

            yield FreezingModel.unfreeze(912345678, [1, 2]);

            const freezingData = yield Freezing.findAll({
                attributes: ['finishTime', 'unfrozenBy'],
                order: [['trialTemplateId', 'ASC']]
            });
            const firstActualFinishTime = moment(freezingData[0].get('finishTime')).toDate();
            const secondActualFinishTime = moment(freezingData[1].get('finishTime')).toDate();

            expect(firstActualFinishTime).to.be.below(finishTime);
            expect(freezingData[0].get('unfrozenBy')).to.equal(912345678);

            expect(secondActualFinishTime).to.be.below(finishTime);
            expect(freezingData[1].get('unfrozenBy')).to.equal(912345678);

            expect(freezingData[2].get('unfrozenBy')).to.be.null;
            expect(freezingData[2].get('finishTime')).to.deep.equal(finishTime);
        });

        it('should update `finishTime` field only on the last record', function *() {
            const now = new Date();
            const firstStartTime = moment(now).subtract(1, 'day').toDate();
            const firstFinishTime = moment(firstStartTime).add(2, 'hour').startOf('second').toDate();
            const secondStartTime = moment(now).subtract(5, 'hour').toDate();
            const secondFinishTime = moment(now).add(4, 'hour').startOf('second').toDate();

            const trialTemplate = { id: 1 };

            yield freezingFactory.createWithRelations(
                { frozenBy: 1234567890, startTime: firstStartTime, finishTime: firstFinishTime },
                { trialTemplate }
            );

            yield freezingFactory.createWithRelations(
                { frozenBy: 1234567890, startTime: secondStartTime, finishTime: secondFinishTime },
                { trialTemplate }
            );

            yield FreezingModel.unfreeze(912345678, [1]);

            const freezingData = yield Freezing.findAll({
                attributes: ['finishTime'],
                order: [['startTime', 'ASC']]
            });

            const actualFirstFinishTime = freezingData[0].get('finishTime');
            const actualSecondFinishTime = freezingData[1].get('finishTime');

            expect(actualFirstFinishTime).to.deep.equal(firstFinishTime);
            expect(actualSecondFinishTime).to.be.below(secondFinishTime);
        });
    });

    describe('`getLastAttemptFinish`', () => {
        it('should return correct time', function *() {
            const user = { id: 1234567890 };
            const secondStarted = moment().startOf('second');
            const firstStarted = moment(secondStarted).subtract(30000, 'ms').toDate();
            const thirdStarted = moment(secondStarted).add(30, 'm').toDate();

            yield trialsFactory.createWithRelations(
                { id: 3, timeLimit: 190000, started: firstStarted },
                { user, trialTemplate: { id: 1 } }
            );
            yield trialsFactory.createWithRelations(
                { id: 4, timeLimit: 90000, started: secondStarted },
                { user, trialTemplate: { id: 2 } }
            );

            yield trialsFactory.createWithRelations(
                { id: 5, timeLimit: 90000, started: thirdStarted },
                { user, trialTemplate: { id: 3 } }
            );

            const actual = yield FreezingModel.getLastAttemptFinish([1, 2]);
            const expected = moment(firstStarted).add(190000, 'ms').toISOString();

            expect(actual).to.deep.equal(expected);
        });

        it('should return `now` when there are no unfinished attempts', function *() {
            const now = moment().startOf('second').toDate();
            const actual = yield FreezingModel.getLastAttemptFinish();

            expect(moment(actual).toDate()).to.be.at.least(now);
        });
    });

    describe('`getCurrentFrozenExams`', () => {
        it('should return empty array if no frozen exams exists', function *() {
            const actual = yield FreezingModel.getCurrentFrozenExams();

            expect(actual).to.deep.equal([]);
        });

        it('should return current frozen exams', function *() {
            const now = new Date();

            const firstStartTime = moment(now).subtract(1, 'day').toDate();
            const firstFinishTime = moment(firstStartTime).add(2, 'hour').toDate();
            const secondFinishTime = moment(now).add(4, 'hour').startOf('second').toDate();
            const thirdStartTime = moment(now).add(1, 'hour').toDate();

            yield freezingFactory.createWithRelations({
                frozenBy: 12345,
                startTime: firstStartTime,
                finishTime: firstFinishTime
            }, { trialTemplate: { id: 1 } });

            yield freezingFactory.createWithRelations({
                frozenBy: 12345,
                startTime: now,
                finishTime: secondFinishTime
            }, { trialTemplate: { id: 2 } });

            yield freezingFactory.createWithRelations({
                frozenBy: 12345,
                startTime: now,
                finishTime: secondFinishTime
            }, { trialTemplate: { id: 3 } });

            yield freezingFactory.createWithRelations({
                frozenBy: 12345,
                startTime: thirdStartTime,
                finishTime: secondFinishTime
            }, { trialTemplate: { id: 3 } });

            let actual = yield FreezingModel.getCurrentFrozenExams();

            actual = _.sortBy(actual, 'trialTemplateId');

            expect(actual).to.deep.equal([
                { trialTemplateId: 2, startTime: now },
                { trialTemplateId: 3, startTime: thirdStartTime }
            ]);
        });
    });
});
