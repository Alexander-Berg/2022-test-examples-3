const assert = require('assert');

const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const IDM = require('models/idm');
const databases = require('db/databases');
const { DbType } = require('db/constants');

const testDbType = DbType.internal;

describe('IDM model', () => {
    beforeEach(cleanDb);

    describe('addRole', () => {
        it('should create new role', async() => {
            await IDM.addRole('zhigalov', { role: 'manager' }, { dbType: testDbType });

            const actual = await databases[testDbType].userRole.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].login, 'zhigalov');
            assert.equal(actual[0].role, 'manager');
        });

        it('should not create duplicate role', async() => {
            await factory.userRole.create({
                login: 'zhigalov',
                role: 'manager',
            });
            await IDM.addRole('zhigalov', { role: 'manager' }, { dbType: testDbType });

            const actual = await databases[testDbType].userRole.count();

            assert.equal(actual, 1);
        });
    });

    describe('removeRole', () => {
        it('should remove role', async() => {
            await factory.userRole.create([
                { login: 'zhigalov', role: 'viewer' },
                { login: 'yuu-mao', role: 'viewer' },
                { login: 'yuu-mao', role: 'admin' },
            ]);

            await IDM.removeRole('yuu-mao', { role: 'viewer' }, { dbType: testDbType });

            const actual = await databases[testDbType].userRole.findAll({
                order: [['login'], ['role']],
                attributes: ['login', 'role'],
                raw: true,
            });

            assert.equal(actual.length, 2);
            assert.deepEqual(actual, [
                { login: 'yuu-mao', role: 'admin' },
                { login: 'zhigalov', role: 'viewer' },
            ]);
        });

        it('should do nothing if role does not exist', async() => {
            await factory.userRole.create([
                { login: 'zhigalov', role: 'viewer' },
                { login: 'yuu-mao', role: 'viewer' },
                { login: 'yuu-mao', role: 'admin' },
            ]);

            await IDM.removeRole('zhigalov', { role: 'admin' }, { dbType: testDbType });

            const actual = await databases[testDbType].userRole.findAll();

            assert.equal(actual.length, 3);
        });
    });

    describe('getAllRoles', () => {
        it('should return all roles', async() => {
            await factory.userRole.create([
                { login: 'saaaaaaaaasha', role: 'viewer' },
                { login: 'saaaaaaaaasha', role: 'admin' },
                { login: 'serenity-dust', role: 'viewer' },
            ]);

            const actual = await IDM.getAllRoles({ dbType: testDbType });
            const expected = [
                {
                    login: 'saaaaaaaaasha',
                    roles: [{ role: 'admin' }, { role: 'viewer' }],
                },
                {
                    login: 'serenity-dust',
                    roles: [{ role: 'viewer' }],
                },
            ];

            assert.deepEqual(actual, expected);
        });
    });

    describe('isAdmin', () => {
        it('should return true when role exists', async() => {
            await factory.group.create({ id: 1 });
            await factory.userRole.create({
                login: 'c-3po',
                role: 'admin',
                eventGroupId: 1,
            });

            const actual = await IDM.isAdmin('c-3po', { dbType: testDbType });

            assert(actual);
        });

        it('should return false when role not exist', async() => {
            await factory.userRole.create([
                { login: 'r2d2', role: 'admin' },
                { login: 'superilya', role: 'viewer' },
            ]);

            const actual = await IDM.isAdmin('superilya', { dbType: testDbType });

            assert(!actual);
        });
    });

    describe('isManager', () => {
        it('should return true when either manager or admin role exists', async() => {
            await factory.userRole.create([
                { login: 'c-3po', role: 'admin' },
                { login: 'r2d2', role: 'viewer' },
                { login: 'lea', role: 'manager' },
            ]);

            const actualAdmin = await IDM.isManager('c-3po', { dbType: testDbType });
            const actualManager = await IDM.isManager('lea', { dbType: testDbType });
            const actualViewer = await IDM.isManager('r2d2', { dbType: testDbType });

            assert.strictEqual(actualAdmin, true);
            assert.strictEqual(actualManager, true);
            assert.strictEqual(actualViewer, false);
        });

        it('should return false when role does not exist', async() => {
            await factory.userRole.create([
                { login: 'superilya', role: 'viewer' },
            ]);

            const actual = await IDM.isManager('superilya', { dbType: testDbType });

            assert.strictEqual(actual, false);
        });
    });

    describe('isViewer', () => {
        it('should return true when either viewer, manager or admin role exists', async() => {
            await factory.userRole.create([
                { login: 'c-3po', role: 'admin' },
                { login: 'r2d2', role: 'viewer' },
                { login: 'lea', role: 'manager' },
            ]);

            const actualAdmin = await IDM.isViewer('c-3po', { dbType: testDbType });
            const actualManager = await IDM.isViewer('lea', { dbType: testDbType });
            const actualViewer = await IDM.isViewer('r2d2', { dbType: testDbType });

            assert.strictEqual(actualAdmin, true);
            assert.strictEqual(actualManager, true);
            assert.strictEqual(actualViewer, true);
        });

        it('should return false when role does not exist', async() => {
            const actual = await IDM.isViewer('superilya', { dbType: testDbType });

            assert.strictEqual(actual, false);
        });
    });
});
