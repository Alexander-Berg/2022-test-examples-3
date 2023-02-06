const assert = require('assert');
const catchErrorAsync = require('catch-error-async');

const { DbType } = require('db/constants');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const db = require('db');
const AccountHistory = require('models/accountHistory');

const testDbType = DbType.internal;

describe('Account history model', () => {
    beforeEach(cleanDb);

    describe('dumpEntity', () => {
        it('should return data of entity using "history" scope', async() => {
            const data = { id: 8, firstName: 'Иван', lastName: 'Иванов' };

            await factory.account.create(data);

            const actual = await AccountHistory.dumpEntity({ entityId: 8, dbType: testDbType });
            const expected = {
                ...factory.account.defaultData,
                ...data,
            };

            assert.deepEqual(actual, expected);
        });

        it('should throw error if entityType is wrong', async() => {
            const error = await catchErrorAsync(
                AccountHistory.dumpEntity.bind(AccountHistory, { entityType: 'wrong', dbType: testDbType }),
            );

            assert.equal(
                error.message,
                'History.getTableByType: Table wrong is not existed',
            );
        });
    });

    describe('create', () => {
        it('should create history record', async() => {
            await factory.account.create({ id: 8, firstName: 'Иван', lastName: 'Иванов' });

            const history = new AccountHistory({ entityId: 8, authorLogin: 'testLogin' }, { dbType: testDbType });

            await history.create(AccountHistory.operationTypes.create, { dbType: testDbType });

            const historyRecords = await db.history.findAll();

            assert.equal(historyRecords.length, 1);
            assert.equal(historyRecords[0].entityId, 8);
            assert.equal(historyRecords[0].authorLogin, 'testLogin');
            assert.equal(historyRecords[0].operation, 'create');
            assert.equal(historyRecords[0].entityType, 'account');
        });

        it('should throw error if updating and previous data is not passed', async() => {
            const eventHistory = new AccountHistory({ authorLogin: 'testLogin' }, { dbType: testDbType });
            const error = await catchErrorAsync(
                eventHistory.create.bind(eventHistory, AccountHistory.operationTypes.update, { dbType: testDbType }),
            );

            assert.equal(
                error.message,
                'History.create: Previous data is required while updating',
            );
        });
    });
});
