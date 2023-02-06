require('co-mocha');

const { expect } = require('chai');

const dbHelper = require('tests/helpers/clear');
const usersFactory = require('tests/factory/usersFactory');
const rolesFactory = require('tests/factory/rolesFactory');
const authTypesFactory = require('tests/factory/authTypesFactory');

const baseTests = require('tests/models/user/test-base');

const TelegramUser = require('models/user/telegramUser');
const { User } = require('db/postgres');

describe('Telegram user model', () => {
    beforeEach(dbHelper.clear);

    baseTests(TelegramUser);

    describe('`findAndRenew`', () => {
        it('should return stored user', function *() {
            const role = { id: 1 };
            const authType = { id: 2, code: 'telegram' };

            yield usersFactory.createWithRelations({ id: 23, uid: 123456789012345 }, { role, authType });

            const telegramUser = {
                uid: { value: '123456789012345' }
            };
            const actual = yield TelegramUser.findAndRenew(telegramUser);

            expect(actual.id).to.equal(23);
            expect(actual.get('uid')).to.equal(123456789012345);
            expect(actual.get('yandexUid')).to.be.null;
            expect(actual.get('roleId')).to.equal(1);
            expect(actual.get('authTypeId')).to.equal(2);
        });

        it('should create new user', function *() {
            yield rolesFactory.create({ id: 1 });
            yield authTypesFactory.create({ id: 3, code: 'telegram' });
            const telegramUser = {
                uid: { value: '123456789012345' },
                username: 'new-user',
                firstname: 'user-firstname',
                lastname: 'user-lastname'
            };
            const actual = yield TelegramUser.findAndRenew(telegramUser);

            expect(actual.get('uid')).to.equal(123456789012345);
            expect(actual.get('yandexUid')).to.be.null;
            expect(actual.get('login')).to.equal('new-user');
            expect(actual.get('firstname')).to.equal('user-firstname');
            expect(actual.get('lastname')).to.equal('user-lastname');
            expect(actual.get('roleId')).to.equal(1);
            expect(actual.get('authTypeId')).to.equal(3);
        });

        it('should update user', function *() {
            const role = { id: 1 };
            const authType = { id: 2, code: 'telegram' };

            yield usersFactory.createWithRelations({
                id: 1234,
                uid: 123456789012345,
                login: 'old-login',
                firstname: 'old-firstname',
                lastname: 'old-lastname'
            }, { role, authType });

            const telegramUser = {
                uid: { value: '123456789012345' },
                username: 'new-user',
                firstname: 'user-firstname',
                lastname: 'user-lastname'
            };
            const actual = yield TelegramUser.findAndRenew(telegramUser);

            expect(actual.id).to.equal(1234);
            expect(actual.get('uid')).to.equal(123456789012345);
            expect(actual.get('login')).to.equal('new-user');
            expect(actual.get('firstname')).to.equal('user-firstname');
            expect(actual.get('lastname')).to.equal('user-lastname');
            expect(actual.get('roleId')).to.equal(1);
            expect(actual.get('authTypeId')).to.equal(2);
        });

        it('should store', function *() {
            const role = { id: 1 };
            const authType = { id: 2, code: 'telegram' };

            yield usersFactory.createWithRelations({
                id: 123,
                uid: 123456789012345,
                login: 'current-user'
            }, { role, authType });

            const telegramUser = { uid: { value: '123456789012345' }, username: 'new-user' };

            yield TelegramUser.findAndRenew(telegramUser);

            const actual = yield User.findAll({ where: { uid: 123456789012345, authTypeId: 2 } });

            expect(actual).to.have.length(1);
            expect(actual[0].id).to.equal(123);
            expect(actual[0].get('uid')).to.equal(123456789012345);
            expect(actual[0].get('yandexUid')).to.be.null;
            expect(actual[0].get('login')).to.equal('new-user');
        });

        it('should not update `roleId` field when this field is not null', function *() {
            const role = { id: 3 };
            const authType = { id: 2, code: 'telegram' };

            yield usersFactory.createWithRelations({
                id: 123,
                uid: 123456789012345,
                login: 'current-user'
            }, { role, authType });

            const telegramUser = { uid: { value: '123456789012345' } };

            const actual = yield TelegramUser.findAndRenew(telegramUser);

            expect(actual.get('roleId')).to.equal(3);
        });

        it('should not update user with same uid and other authType', function *() {
            yield authTypesFactory.create({ code: 'telegram' });
            yield rolesFactory.create({ id: 1 });
            yield usersFactory.createWithRelations({
                id: 1234,
                uid: 123456,
                login: 'old-login',
                firstname: 'old-firstname',
                lastname: 'old-lastname'
            }, { authType: { code: 'web' } });

            const telegramUser = {
                uid: { value: '123456' },
                username: 'new-user',
                firstname: 'user-firstname',
                lastname: 'user-lastname'
            };

            yield TelegramUser.findAndRenew(telegramUser);

            const actual = yield User.findById(1234);

            expect(actual.id).to.equal(1234);
            expect(actual.get('uid')).to.equal(123456);
            expect(actual.get('login')).to.equal('old-login');
            expect(actual.get('firstname')).to.equal('old-firstname');
            expect(actual.get('lastname')).to.equal('old-lastname');
        });
    });
});
