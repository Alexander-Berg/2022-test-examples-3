const { expect } = require('chai');

const BansByLoginReport = require('models/report/items/bansByLoginReport');

const catchError = require('tests/helpers/catchError').generator;
const dbHelper = require('tests/helpers/clear');

const bansFactory = require('tests/factory/bansFactory');
const usersFactory = require('tests/factory/usersFactory');

describe('Bans by login report', () => {
    beforeEach(dbHelper.clear);

    it('should return correct bans data when user is banned', function *() {
        const firstTrialTemplate = { id: 1, slug: 'winter' };
        const secondTrialTemplate = { id: 2, slug: 'summer' };
        const role = { id: 1 };
        const authType = { id: 1, code: 'web' };
        const globalUser = {
            id: 4,
            actualLogin: 'second',
            isBanned: false
        };
        const firstUserAccount = { id: 1, uid: 123, login: 'first' };
        const secondUserAccount = { id: 2, uid: 456, login: 'second' };

        yield usersFactory.createWithRelations(firstUserAccount, { globalUser, role, authType });
        yield usersFactory.createWithRelations(secondUserAccount, { globalUser, role, authType });
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
            action: 'unban',
            startedDate: new Date(4, 4, 4),
            expiredDate: null,
            reason: 'it is sunny',
            userLogin: 'second'
        }, {
            globalUser,
            trialTemplate: firstTrialTemplate,
            admin: { id: 2, login: 'rinka' }
        });
        yield bansFactory.createWithRelations({
            id: 3,
            action: 'ban',
            startedDate: new Date(3, 3, 3),
            expiredDate: new Date(7, 7, 7),
            reason: 'hackers everywhere',
            userLogin: 'second'
        }, {
            globalUser,
            trialTemplate: secondTrialTemplate,
            admin: { id: 3, login: 'anyok' }
        });

        const actual = yield BansByLoginReport.apply({ login: 'first' });

        expect(actual).to.deep.equal([
            {
                searchedLogin: 'first',
                associatedLogins: 'first,second',
                actualLogin: 'second',
                isSuperBanned: 'нет',
                examSlug: 'winter',
                action: 'ban',
                adminLogin: 'dotokoto',
                reason: 'it is rainy',
                startedDate: new Date(1, 1, 1),
                expiredDate: new Date(5, 5, 5),
                userLogin: 'first'
            },
            {
                searchedLogin: 'first',
                associatedLogins: 'first,second',
                actualLogin: 'second',
                isSuperBanned: 'нет',
                examSlug: 'winter',
                action: 'unban',
                adminLogin: 'rinka',
                reason: 'it is sunny',
                startedDate: new Date(4, 4, 4),
                expiredDate: '',
                userLogin: 'second'
            },
            {
                searchedLogin: 'first',
                associatedLogins: 'first,second',
                actualLogin: 'second',
                isSuperBanned: 'нет',
                examSlug: 'summer',
                action: 'ban',
                adminLogin: 'anyok',
                reason: 'hackers everywhere',
                startedDate: new Date(3, 3, 3),
                expiredDate: new Date(7, 7, 7),
                userLogin: 'second'
            }
        ]);
    });

    it('should process superban correctly', function *() {
        const globalUser = {
            id: 4,
            actualLogin: 'expert',
            isBanned: true
        };

        yield usersFactory.createWithRelations({ id: 1, login: 'expert' }, { globalUser });
        yield bansFactory.createWithRelations({
            id: 1,
            isLast: true,
            action: 'ban',
            startedDate: new Date(1, 1, 1),
            expiredDate: null,
            userLogin: 'expert',
            reason: 'I am bored'
        }, {
            globalUser,
            trialTemplate: { slug: 'rain' },
            admin: { login: 'oktokoto' }
        });

        const actual = yield BansByLoginReport.apply({ login: 'expert' });

        expect(actual).to.deep.equal([
            {
                searchedLogin: 'expert',
                associatedLogins: 'expert',
                actualLogin: 'expert',
                isSuperBanned: 'да',
                examSlug: 'rain',
                action: 'ban',
                adminLogin: 'oktokoto',
                reason: 'I am bored',
                startedDate: new Date(1, 1, 1),
                expiredDate: '',
                userLogin: 'expert'
            }
        ]);
    });

    it('should find bans by alias for login', function *() {
        const globalUser = {
            id: 4,
            actualLogin: 'super-expert',
            isBanned: false
        };

        yield usersFactory.createWithRelations({ id: 1, login: 'super-expert' }, { globalUser });
        yield bansFactory.createWithRelations({
            id: 1,
            isLast: true,
            action: 'ban',
            startedDate: new Date(1, 1, 1),
            expiredDate: new Date(2, 2, 2),
            userLogin: 'super-expert',
            reason: 'summer time'
        }, {
            globalUser,
            trialTemplate: { slug: 'rain' },
            admin: { login: 'oktokoto' }
        });

        const actual = yield BansByLoginReport.apply({ login: 'super.expert' });

        expect(actual).to.deep.equal([
            {
                searchedLogin: 'super.expert',
                associatedLogins: 'super-expert',
                actualLogin: 'super-expert',
                isSuperBanned: 'нет',
                examSlug: 'rain',
                action: 'ban',
                adminLogin: 'oktokoto',
                reason: 'summer time',
                startedDate: new Date(1, 1, 1),
                expiredDate: new Date(2, 2, 2),
                userLogin: 'super-expert'
            }
        ]);
    });

    it('should filter by global user', function *() {
        const trialTemplate = { id: 1, slug: 'winter' };
        const authType = { id: 1, code: 'web' };
        const firstGlobalUser = { id: 4, actualLogin: 'first', isBanned: false };
        const secondGlobalUser = { id: 5, actualLogin: 'second', isBanned: false };
        const firstUserAccount = { id: 1, uid: 123, login: 'first' };
        const secondUserAccount = { id: 2, uid: 456, login: 'second' };

        yield usersFactory.createWithRelations(firstUserAccount, { globalUser: firstGlobalUser, authType });
        yield usersFactory.createWithRelations(secondUserAccount, { globalUser: secondGlobalUser, authType });
        yield bansFactory.createWithRelations({
            id: 1,
            action: 'ban',
            startedDate: new Date(1, 1, 1),
            expiredDate: new Date(5, 5, 5),
            reason: 'it is rainy',
            userLogin: 'first'
        }, {
            globalUser: firstGlobalUser,
            trialTemplate,
            admin: { id: 1, login: 'dotokoto' }
        });
        yield bansFactory.createWithRelations({
            id: 2,
            action: 'ban',
            startedDate: new Date(4, 4, 4),
            expiredDate: new Date(6, 6, 6),
            reason: 'it is sunny',
            userLogin: 'second'
        }, {
            globalUser: secondGlobalUser,
            trialTemplate,
            admin: { id: 2, login: 'rinka' }
        });

        const actual = yield BansByLoginReport.apply({ login: 'first' });

        expect(actual).to.deep.equal([
            {
                searchedLogin: 'first',
                associatedLogins: 'first',
                actualLogin: 'first',
                isSuperBanned: 'нет',
                examSlug: 'winter',
                action: 'ban',
                adminLogin: 'dotokoto',
                reason: 'it is rainy',
                startedDate: new Date(1, 1, 1),
                expiredDate: new Date(5, 5, 5),
                userLogin: 'first'
            }
        ]);
    });

    it('should return `[]` when user does not have global user', function *() {
        yield usersFactory.createWithRelations({ id: 3, login: 'expert', globalUserId: null });

        const actual = yield BansByLoginReport.apply({ login: 'expert' });

        expect(actual).to.deep.equal([]);
    });

    it('should return `[]` when user does not exist', function *() {
        const actual = yield BansByLoginReport.apply({ login: 'not-exist' });

        expect(actual).to.deep.equal([]);
    });

    it('should throw error when login is incorrect', function *() {
        const error = yield catchError(BansByLoginReport.apply.bind(null, { login: 4 }));

        expect(error.message).to.equal('Login is invalid');
        expect(error.status).to.equal(400);
        expect(error.options).to.deep.equal({
            internalCode: '400_LII',
            searchedLogin: 4
        });
    });
});
