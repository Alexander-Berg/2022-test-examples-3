require('co-mocha');

const { expect } = require('chai');

const dbHelper = require('tests/helpers/clear');
const usersFactory = require('tests/factory/usersFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const globalUsersFactory = require('tests/factory/globalUsersFactory');
const { User } = require('db/postgres');

module.exports = function (Model) {
    beforeEach(dbHelper.clear);

    describe('`findUser`', () => {
        it('should return user when user exists', function *() {
            const user = { id: 123, uid: 1234567890 };
            const authType = { id: 2, code: Model.authType };

            yield usersFactory.createWithRelations(user, { authType });

            const actual = yield Model.findUser({ where: { uid: user.uid } });

            expect(actual.get('id')).to.equal(user.id);
            expect(actual.get('uid')).to.equal(user.uid);
        });

        it('should return null when user does not exist', function *() {
            const actual = yield Model.findUser({ where: { uid: 12345 } });

            expect(actual).to.be.null;
        });

        it('should return null when user have same uid but other authType', function *() {
            const user = { id: 123, uid: 1234567890 };
            const authTypeCode = Model.authType === 'web' ? 'telegram' : 'web';
            const authType = { id: 2, code: authTypeCode };

            yield usersFactory.createWithRelations(user, { authType });

            const actual = yield Model.findUser({ where: { uid: 1234567890 } });

            expect(actual).to.be.null;
        });

        it('should return correct attributes when param `attributes` exists', function *() {
            const user = {
                id: 123,
                uid: 1234567890,
                login: 'pupkin',
                firstname: 'Vasya',
                lastname: 'Pupkin'
            };
            const authType = { id: 2, code: Model.authType };

            yield usersFactory.createWithRelations(user, { authType });

            const attributes = ['id', 'uid', 'login', 'firstname', 'lastname', 'authTypeId'];
            const actual = yield Model.findUser({ where: { uid: user.uid }, attributes });

            expect(actual.get('id')).to.equal(user.id);
            expect(actual.get('uid')).to.equal(user.uid);
            expect(actual.get('login')).to.equal(user.login);
            expect(actual.get('firstname')).to.equal(user.firstname);
            expect(actual.get('lastname')).to.equal(user.lastname);
            expect(actual.get('authTypeId')).to.equal(2);
        });
    });

    describe('`findUserByTrialId`', () => {
        const attributes = ['id', 'email'];

        it('should return user by trialId', function *() {
            const user = { id: 123, email: 'some@ya.ru' };
            const authType = { id: 2, code: Model.authType };

            yield trialsFactory.createWithRelations({ id: 23 }, { user, authType });

            const actual = yield Model.findUserByTrialId({ trialId: 23, attributes });

            expect(actual).to.deep.equal(user);
        });

        it('should return `null` when trial does not exist', function *() {
            const actual = yield Model.findUserByTrialId({ trialId: 23, attributes });

            expect(actual).to.be.null;
        });
    });

    describe('`setGlobalUserId`', () => {
        it('should set globalUserId to users', function *() {
            yield globalUsersFactory.create({ id: 1 });
            const first = yield usersFactory.createWithRelations({
                id: 1,
                login: 'a',
                globalUserId: null
            }, {});
            const second = yield usersFactory.createWithRelations({
                id: 2,
                login: 'b'
            }, { globalUser: { id: 2 } });

            yield usersFactory.createWithRelations({
                id: 3,
                login: 'c',
                globalUserId: null
            }, {});

            yield Model.setGlobalUserId({ users: [first, second], globalUserId: 1 });

            const actual = yield User.findAll({ attributes: ['globalUserId'], order: [['id']], raw: true });

            expect(actual).to.deep.equal([
                { globalUserId: 1 },
                { globalUserId: 1 },
                { globalUserId: null }
            ]);
        });

        it('should do nothing if users is empty', function *() {
            yield usersFactory.createWithRelations({ id: 1, globalUserId: null });

            yield Model.setGlobalUserId({ users: [], globalUserId: 1 });

            const actual = yield User.findAll({ attributes: ['globalUserId'], raw: true });

            expect(actual).to.deep.equal([{ globalUserId: null }]);
        });
    });

    describe('`getAssociatedUsers`', () => {
        const authType = { id: 2, code: Model.authType };

        it('should return logins by globalUserId', function *() {
            yield usersFactory.createWithRelations({
                id: 1, uid: 1, login: 'user-1'
            }, { authType, globalUser: { id: 1 } });
            yield usersFactory.createWithRelations({
                id: 2, uid: 2, login: 'user-2'
            }, { authType, globalUser: { id: 2 } });
            yield usersFactory.createWithRelations({
                id: 3, uid: 3, login: 'user-3'
            }, { authType, globalUser: { id: 1 } });

            const actual = yield Model.getAssociatedUsers([1]);

            expect(actual).to.deep.equal([
                { id: 1, login: 'user-1' },
                { id: 3, login: 'user-3' }
            ]);
        });

        it('should return logins by authType', function *() {
            const anotherAuth = { id: 4, code: 'other' };
            const globalUser = { id: 1 };

            yield usersFactory.createWithRelations({
                id: 1, uid: 1, login: 'user-1'
            }, { authType, globalUser });
            yield usersFactory.createWithRelations({
                id: 2, uid: 2, login: 'user-2'
            }, { authType: anotherAuth, globalUser });

            const actual = yield Model.getAssociatedUsers([1]);

            expect(actual).to.deep.equal([{ id: 1, login: 'user-1' }]);
        });

        it('should return empty array if users not found', function *() {
            const actual = yield Model.getAssociatedUsers([1]);

            expect(actual).to.deep.equal([]);
        });
    });
};
