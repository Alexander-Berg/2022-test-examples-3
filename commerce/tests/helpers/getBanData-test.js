const { expect } = require('chai');
const moment = require('moment');

const getBanData = require('helpers/getBanData');

const banFactory = require('tests/factory/bansFactory');
const usersFactory = require('tests/factory/usersFactory');

const dbHelper = require('tests/helpers/clear');

describe('Get ban data helper', () => {
    const authType = { id: 2, code: 'web' };

    beforeEach(dbHelper.clear);

    it('should return correct data when user does not have global id', function *() {
        yield usersFactory.createWithRelations({
            id: 1,
            uid: 123,
            globalUserId: null
        }, { authType });

        const actual = yield getBanData(1, { uid: 123, authTypeCode: 'web' });

        expect(actual).to.deep.equal({
            isBannedOnTest: false,
            isSuperBanned: false,
            actualLogin: '',
            expiredDate: null
        });
    });

    it('should return correct data when global user is not banned', function *() {
        yield usersFactory.createWithRelations(
            { id: 1, uid: 123 },
            {
                globalUser: { id: 10, isBanned: false, actualLogin: 'actual' },
                authType
            }
        );

        const actual = yield getBanData(1, { uid: 123, authTypeCode: 'web' });

        expect(actual).to.deep.equal({
            isBannedOnTest: false,
            isSuperBanned: false,
            actualLogin: 'actual',
            expiredDate: null
        });
    });

    it('should return correct data when global user is banned on any test', function *() {
        const globalUser = { id: 10, isBanned: false, actualLogin: 'actual' };

        yield usersFactory.createWithRelations(
            { id: 1, uid: 123 },
            { globalUser, authType }
        );

        const expiredDate = moment().add(1, 'year').startOf('day').toDate();

        yield banFactory.createWithRelations({
            id: 2,
            action: 'ban',
            isLast: true,
            expiredDate
        }, {
            trialTemplate: { id: 1 },
            globalUser,
            admin: { id: 1234 }
        });

        const actual = yield getBanData(1, { uid: 123, authTypeCode: 'web' });

        expect(actual).to.deep.equal({
            isBannedOnTest: true,
            isSuperBanned: false,
            actualLogin: 'actual',
            expiredDate
        });
    });

    it('should return correct data when global user is super banned', function *() {
        const globalUser = { id: 10, isBanned: true, actualLogin: 'actual' };

        yield usersFactory.createWithRelations(
            { id: 1, uid: 123 },
            { globalUser, authType }
        );

        yield banFactory.createWithRelations({
            id: 2,
            action: 'ban',
            isLast: true,
            expiredDate: null
        }, {
            trialTemplate: { id: 1 },
            globalUser,
            admin: { id: 1234 }
        });

        const actual = yield getBanData(1, { uid: 123, authTypeCode: 'web' });

        expect(actual).to.deep.equal({
            isBannedOnTest: false,
            isSuperBanned: true,
            actualLogin: 'actual',
            expiredDate: null
        });
    });

    it('should return correct data when ban is expired', function *() {
        const globalUser = { id: 10, isBanned: false, actualLogin: 'actual' };

        yield usersFactory.createWithRelations(
            { id: 1, uid: 123 },
            { globalUser, authType }
        );

        yield banFactory.createWithRelations({
            id: 2,
            action: 'ban',
            isLast: true,
            expiredDate: moment().subtract(1, 'hour').toDate()
        }, {
            trialTemplate: { id: 1 },
            globalUser,
            admin: { id: 1234 }
        });

        const actual = yield getBanData(1, { uid: 123, authTypeCode: 'web' });

        expect(actual).to.deep.equal({
            isBannedOnTest: false,
            isSuperBanned: false,
            actualLogin: 'actual',
            expiredDate: null
        });
    });

    it('should return correct data when user is banned on other exam', function *() {
        const globalUser = { id: 9, isBanned: false, actualLogin: 'actual' };

        yield usersFactory.createWithRelations(
            { id: 1, uid: 123 },
            { globalUser, authType }
        );

        yield banFactory.createWithRelations({
            id: 2,
            action: 'ban',
            isLast: true,
            expiredDate: moment().add(1, 'year').toDate()
        }, {
            trialTemplate: { id: 2 },
            globalUser,
            admin: { id: 1234 }
        });

        const actual = yield getBanData(1, { uid: 123, authTypeCode: 'web' });

        expect(actual).to.deep.equal({
            isBannedOnTest: false,
            isSuperBanned: false,
            actualLogin: 'actual',
            expiredDate: null
        });
    });
});
