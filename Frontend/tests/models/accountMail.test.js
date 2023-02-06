const assert = require('assert');
const catchErrorAsync = require('catch-error-async');

const { DbType } = require('db/constants');
const db = require('db');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const AccountMail = require('models/accountMail');

const testDbType = DbType.internal;

describe('AccountMail model', () => {
    beforeEach(cleanDb);

    describe('batchCreate', () => {
        it('should save a few new accountMails', async() => {
            await factory.account.create({ id: 42 });
            await factory.account.create({ id: 43, email: 'lightside@yandex.ru' });
            await factory.distribution.create({ id: 11 });

            const data = [
                {
                    accountId: 42,
                    distributionId: 11,
                    title: 'My New Mail',
                },
                {
                    accountId: 43,
                    distributionId: 11,
                    title: 'My New Mail',
                },
            ];

            await AccountMail.batchCreate(data, { dbType: testDbType });

            const actuals = await db.accountMail.findAll();

            assert.equal(actuals[0].title, data[0].title);
            assert.equal(actuals[1].title, data[1].title);
        });
    });

    describe('create', () => {
        it('should save a new accountMail', async() => {
            await factory.account.create({ id: 42 });
            await factory.distribution.create({ id: 11 });

            const data = {
                id: 5,
                accountId: 42,
                distributionId: 11,
                title: 'My New Mail',
            };

            const item = new AccountMail(data, { dbType: testDbType });
            const id = await item.create();

            const actual = await db.accountMail.findById(id);

            assert.equal(actual.title, data.title);
        });
    });

    describe('delete', () => {
        it('should delete an existing accountMail', async() => {
            const id = 5;

            await factory.accountMail.create({ id });

            await AccountMail.destroy(id, { dbType: testDbType });

            const actual = await db.accountMail.findOne({ where: { id } });

            assert.equal(actual, null);
        });

        it('should throw on nonexistent accountMail', async() => {
            const error = await catchErrorAsync(AccountMail.destroy.bind(AccountMail, 13, { dbType: testDbType }));

            assert.equal(error.message, 'Account mail not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
            });
        });
    });
});
