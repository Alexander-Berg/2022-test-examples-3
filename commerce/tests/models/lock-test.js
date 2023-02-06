const { expect } = require('chai');

const LockModel = require('models/lock');
const { Lock } = require('db/postgres');

const dbHelper = require('tests/helpers/clear');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const adminsFactory = require('tests/factory/adminsFactory');
const locksFactory = require('tests/factory/locksFactory');

describe('`Lock model`', () => {
    beforeEach(dbHelper.clear);

    describe('`lock`', () => {
        it('should create new record', function *() {
            yield trialTemplatesFactory.createWithRelations({ id: 2, slug: 'winter' });
            yield adminsFactory.create({ id: 7, login: 'anyok' });

            const lockData = {
                adminId: 7,
                trialTemplateId: 2
            };

            yield LockModel.lock(lockData);

            const actual = yield Lock.findAll({
                attributes: ['adminId', 'trialTemplateId', 'lockDate', 'unlockDate'],
                raw: true
            });

            expect(actual.length).to.equal(1);
            expect(actual[0].adminId).to.equal(7);
            expect(actual[0].trialTemplateId).to.equal(2);
            expect(actual[0].lockDate).to.be.below(new Date());
            expect(actual[0].unlockDate).to.be.null;
        });
    });

    describe('`tryFindLast`', () => {
        const admin = { id: 7, login: 'sinseveria' };
        const trialTemplate = { id: 2 };

        it('should return last lock for exam', function *() {
            yield locksFactory.createWithRelations(
                { id: 2, lockDate: new Date(2017, 3, 3) },
                { trialTemplate, admin }
            );

            const lockDate = new Date(2017, 4, 3);

            yield locksFactory.createWithRelations(
                { id: 3, lockDate },
                { trialTemplate, admin }
            );

            const actual = yield LockModel.tryFindLast(2);

            expect(actual.toJSON()).to.deep.equal({
                id: 3,
                lockDate,
                unlockDate: null,
                admin: { login: 'sinseveria' }
            });
        });

        it('should return row for current exam', function *() {
            const otherTrialTemplate = { id: 3 };
            const otherAdmin = { id: 15, login: 'semenmakhaev' };
            const lockDate = new Date(2019, 1, 25);

            yield locksFactory.createWithRelations(
                { id: 3, lockDate },
                { trialTemplate, admin }
            );

            yield locksFactory.createWithRelations(
                { id: 4, lockDate: new Date(2019, 3, 3) },
                { trialTemplate: otherTrialTemplate, admin: otherAdmin }
            );

            const actual = yield LockModel.tryFindLast(2);

            expect(actual.toJSON()).to.deep.equal({
                id: 3,
                lockDate,
                unlockDate: null,
                admin: { login: 'sinseveria' }
            });
        });

        it('should return null when this exam has not yet been edited', function *() {
            yield adminsFactory.create({ id: 7, login: 'dotokoto' });
            yield trialTemplatesFactory.createWithRelations({ id: 2 });

            const actual = yield LockModel.tryFindLast(2);

            expect(actual).to.be.null;
        });
    });

    describe('`findLockedExams`', () => {
        it('should return only locked exams', function *() {
            const firstAdmin = { id: 1, login: 'anyok' };
            const secondAdmin = { id: 2, login: 'dotokoto' };
            const firstLockDate = new Date(2019, 7, 7);
            const secondLockDate = new Date(2019, 10, 5);

            yield locksFactory.createWithRelations(
                { id: 3, lockDate: firstLockDate },
                { trialTemplate: { id: 1, slug: 'winter' }, admin: firstAdmin }
            );
            yield locksFactory.createWithRelations(
                { id: 4, lockDate: firstLockDate },
                { trialTemplate: { id: 2, slug: 'spring' }, admin: firstAdmin }
            );
            yield locksFactory.createWithRelations(
                { id: 5, lockDate: secondLockDate },
                { trialTemplate: { id: 3, slug: 'summer' }, admin: secondAdmin }
            );
            yield locksFactory.createWithRelations(
                { id: 6, lockDate: secondLockDate, unlockDate: new Date(2019, 11, 7) },
                { trialTemplate: { id: 4, slug: 'autumn' }, admin: secondAdmin }
            );

            const actual = yield LockModel.findLockedExams();

            const expected = {
                winter: {
                    login: 'anyok',
                    lockDate: firstLockDate
                },
                spring: {
                    login: 'anyok',
                    lockDate: firstLockDate
                },
                summer: {
                    login: 'dotokoto',
                    lockDate: secondLockDate
                }
            };

            expect(actual).to.deep.equal(expected);
        });

        it('should return `{}` when there are no locks', function *() {
            const actual = yield LockModel.findLockedExams();

            expect(actual).to.deep.equal({});
        });
    });

    describe('`unlock`', () => {
        it('should update only one record', function *() {
            yield locksFactory.createWithRelations(
                { id: 3, lockDate: new Date(2019, 3, 6) },
                { trialTemplate: { id: 1 }, admin: { id: 4 } }
            );

            yield locksFactory.createWithRelations(
                { id: 4, lockDate: new Date(2019, 5, 9) },
                { trialTemplate: { id: 2 }, admin: { id: 4 } }
            );

            yield LockModel.unlock(3);

            const actual = yield Lock.findAll({
                attributes: ['unlockDate'],
                order: [['id']],
                raw: true
            });

            expect(actual.length).to.equal(2);
            expect(actual[0].unlockDate).to.not.be.null;
            expect(actual[1].unlockDate).to.be.null;
        });

        it('should do nothing when there is no specified record in db', function *() {
            yield locksFactory.createWithRelations({ id: 3 });

            yield LockModel.unlock(7);

            const actual = yield Lock.findOne({
                where: { id: 3 },
                attributes: ['unlockDate']
            });

            expect(actual.unlockDate).to.be.null;
        });
    });
});
