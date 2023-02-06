const assert = require('assert');
const catchErrorAsync = require('catch-error-async');
const config = require('yandex-cfg');
const _ = require('lodash');
const shortid = require('shortid');

const { DbType } = require('db/constants');
const db = require('db');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const Account = require('models/account');

const testDbType = DbType.internal;

describe('Account model', () => {
    beforeEach(cleanDb);

    describe('findOne', () => {
        it('should find a account', async() => {
            const data = {
                id: '11',
                yandexuid: '1518583433759763986',
                birthDate: new Date('1940-12-12'),
                email: 'darkside@yandex.ru',
                firstName: 'Энакин',
                lastName: 'Скайуокер',
                isEmailConfirmed: false,
            };

            await factory.account.create(data);

            const actual = await Account.findOne({ id: 11, dbType: testDbType });

            assert.equal(actual.id, data.id);
            assert.equal(actual.yandexuid, data.yandexuid);
            assert.equal(actual.email, data.email);
            assert.equal(actual.isEmailConfirmed, false);
        });

        it('should throw if account not found', async() => {
            const error = await catchErrorAsync(
                Account.findOne.bind(Account), { id: 11, dbType: testDbType },
            );

            assert.equal(error.message, 'Account not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
                dbType: testDbType,
            });
        });
    });

    describe('count', () => {
        it('should count all accounts', async() => {
            const items = _.range(1, 26)
                .map(num => ({
                    id: num,
                    yandexuid: shortid.generate(),
                    email: `test${num}@yandex.ru`,
                }));

            await factory.account.create(items);

            const actual = await Account.count({ scope: 'page', dbType: testDbType });

            assert.equal(actual, 25);
        });
    });

    describe('findList', () => {
        it('should find list of accounts', async() => {
            const accounts = [
                {
                    yandexuid: '1518583433759763986',
                    email: 'darkside@yandex.ru',
                    firstName: 'Энакин',
                    lastName: 'Скайуокер',
                },
                {
                    yandexuid: '1518583433759764444',
                    email: 'darkside2@yandex.ru',
                    firstName: 'Дарт',
                    lastName: 'Вейдер',
                },
            ];

            await factory.account.create(accounts);

            const options = { pageSize: 3, pageNumber: 1, scope: 'page', dbType: testDbType };
            const actual = await Account.findPage(options);

            assert.equal(actual.meta.totalSize, 2);
            assert.equal(actual.rows[0].yandexuid, accounts[1].yandexuid);
            assert.equal(actual.rows[0].email, accounts[1].email);
        });
    });

    describe('findPage', () => {
        it('should find accounts with limit and offset', async() => {
            const items = _.range(1, 26)
                .map(num => ({
                    id: num,
                    yandexuid: shortid.generate(),
                    email: `test${num}@yandex.ru`,
                }));

            await factory.account.create(items);

            const options = { pageSize: 3, pageNumber: 3, scope: 'page', dbType: testDbType };
            const actual = await Account.findPage(options);

            assert.equal(actual.rows.length, 3);
            assert.equal(actual.rows[0].id, '19');
            assert.equal(actual.rows[1].id, '18');
            assert.equal(actual.rows[2].id, '17');
            assert.equal(actual.meta.totalSize, 25);
            assert.equal(actual.meta.pageNumber, 3);
            assert.equal(actual.meta.pageSize, 3);
        });

        it('should find accounts filtered by condition', async() => {
            const filterParams = {
                and: [
                    {
                        type: 'string',
                        name: 'firstName',
                        value: '3',
                        compare: 'cont',
                    },
                ],
            };
            const items = _.range(1, 26)
                .map(num => ({
                    id: num,
                    yandexuid: shortid.generate(),
                    email: `test${num}@yandex.ru`,
                    firstName: `test${num}`,
                }));

            await factory.account.create(items);

            const options = { pageSize: 10, pageNumber: 1, scope: 'page', filterParams, dbType: testDbType };
            const actual = await Account.findPage(options);

            assert.equal(actual.rows.length, 3);
            assert.equal(actual.rows[0].id, 23);
            assert.equal(actual.rows[1].id, 13);
            assert.equal(actual.rows[2].id, 3);
            assert.equal(actual.meta.totalSize, 3);
            assert.equal(actual.meta.pageNumber, 1);
            assert.equal(actual.meta.pageSize, 10);
        });
    });

    describe('create', () => {
        it('should save a new account', async() => {
            const data = {
                yandexuid: '1518583433759763986',
                avatar: null,
                sex: config.schema.userSexEnum.man,
                createdAt: new Date(),
                birthDate: new Date('1940-12-12'),
                isEmailConfirmed: true,
                email: 'darkside@yandex.ru',
                phone: '999',
                city: 'Татуин',
                website: 'https//ya.ru',
                blog: 'https//ya.ru',
                firstName: 'Энакин',
                lastName: 'Скайуокер',
                middleName: 'Иванович',
                about: 'Я человек, и моё имя — Энакин!',
                jobPlace: 'Орден джедаев',
                jobPosition: 'Джуниор-падаван',
                studyPlace: null,
            };
            const item = new Account(data, { dbType: testDbType, authorLogin: 'testLogin' });
            const id = await item.create({ dbType: testDbType });

            const actual = await db.account.findById(id, { dbType: testDbType });

            assert.equal(actual.lastName, data.lastName);
            assert.equal(actual.sex, data.sex);
            assert.equal(actual.about, data.about);
            assert.equal(actual.jobPlace, data.jobPlace);
            assert.deepEqual(actual.birthDate, '1940-12-12');
        });
    });

    describe('patch', () => {
        it('should patch an existing account', async() => {
            const accountData = {
                id: '5',
                email: 'darkside@yandex.ru',
                firstName: 'Энакин',
                lastName: 'Скайуокер',
            };

            await factory.account.create(accountData);

            const account = new Account({
                id: 5,
                email: 'kek@yandex.ru',
            }, {
                dbType: testDbType,
                authorLogin: 'testLogin',
            });
            const accountId = await account.patch({ dbType: testDbType });
            const actual = await db.account.findOne({ dbType: testDbType, where: { id: accountId } });

            assert.equal(actual.id, accountData.id);
            assert.equal(actual.firstName, accountData.firstName);
            assert.equal(actual.email, 'kek@yandex.ru');
        });

        it('should throw on nonexistent account', async() => {
            const account = new Account({ id: 13 }, { dbType: testDbType, authorLogin: 'testLogin' });
            const error = await catchErrorAsync(account.patch.bind(account, { dbType: testDbType }));

            assert.equal(error.message, 'Account not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
            });
        });
    });

    describe('delete', () => {
        it('should delete an existing account', async() => {
            const accountId = 5;
            const accountData = {
                id: accountId,
                email: 'darkside@yandex.ru',
                firstName: 'Энакин',
                lastName: 'Скайуокер',
            };

            await factory.account.create(accountData);

            await Account.destroy(accountId, { dbType: testDbType, authorLogin: 'testLogin' });

            const actual = await db.account.findOne({ dbType: testDbType, where: { id: accountId } });

            assert.equal(actual, null);
        });

        it('should throw on nonexistent account', async() => {
            const error = await catchErrorAsync(Account.destroy.bind(Account, 13, { dbType: testDbType }));

            assert.equal(error.message, 'Account not found');
            assert.equal(error.statusCode, 404);
            assert.deepEqual(error.options, {
                internalCode: '404_ENF',
                id: 13,
            });
        });
    });
});
