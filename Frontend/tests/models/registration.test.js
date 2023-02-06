const assert = require('assert');
const catchErrorAsync = require('catch-error-async');
const _ = require('lodash');

const { DbType } = require('db/constants');
const db = require('db');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');
const Registration = require('models/registration');
const defaultSchema = require('lib/schema/registration');

const testDbType = DbType.internal;

describe('Registration model', () => {
    beforeEach(cleanDb);

    describe('findOne', () => {
        it('should find a registration', async() => {
            const data = {
                id: 4,
                answers: [],
                formAnswerId: 535297,
                source: 'forms',
                invitationStatus: 'not_decided',
                visitStatus: 'not_come',
                createdAt: new Date(2018, 10, 15),
                yandexuid: '1518583433759763986',
                isEmailConfirmed: true,
                isRegisteredBefore: true,
            };

            await factory.registration.create(data);

            const registration = await Registration.findOne({ id: 4, scope: 'one', dbType: testDbType });
            const actual = _.omit(registration.toJSON(), ['secretKey']);

            const expected = {
                ..._.omit(data, ['eventId', 'accountId']),
                accountId: 1,
                eventId: 5,
                account: {
                    id: 1,
                    email: 'solo1@starwars.ru',
                    avatar: null,
                    login: null,
                    city: 'Татуин',
                    firstName: 'Энакин',
                    isEmailConfirmed: true,
                    lastName: 'Скайуокер',
                    jobPlace: 'Орден джедаев',
                    yandexuid: '1518583433759763986',
                },
                yandexuid: '1518583433759763986',
                isEmailConfirmed: true,
                isRegisteredBefore: true,
            };

            assert.deepStrictEqual(actual, expected);
        });

        it('should throw if registration is not found', async() => {
            const error = await catchErrorAsync(
                Registration.findOne.bind(Registration), { id: 11, scope: 'one', dbType: testDbType },
            );

            assert.strictEqual(error.message, 'Registration not found');
            assert.strictEqual(error.statusCode, 404);
            assert.deepStrictEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
                scope: 'one',
                dbType: testDbType,
            });
        });
    });

    describe('findPage', () => {
        it('should find registrations with limit and offset by eventId', async() => {
            const items = _.range(1, 26)
                .map(num => ({
                    id: num,
                    eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
                    accountId: { id: num + 100, email: `solo${num}@starwars-team.ru` },
                    answers: [],
                    createdAt: new Date(2018, 10, num),
                }))
                .concat({
                    id: 100,
                    eventId: { id: 6, slug: 'front-talks-2019', title: 'front-talks' },
                    accountId: { id: 1111, email: 'solo1111@starwars-team.ru' },
                });

            await factory.registration.create(items);

            const options = { pageSize: 3, pageNumber: 3, scope: 'page', eventId: 5, dbType: testDbType };
            const actual = await Registration.findPage(options);

            assert.strictEqual(actual.rows.length, 3);
            assert.strictEqual(actual.rows[0].id, 19);
            assert.strictEqual(actual.rows[1].id, 18);
            assert.strictEqual(actual.rows[2].id, 17);
            assert.strictEqual(actual.meta.totalSize, 25);
            assert.strictEqual(actual.meta.pageNumber, 3);
            assert.strictEqual(actual.meta.pageSize, 3);
        });

        it('should find registrations with limit and offset by accountId', async() => {
            const accounts = [
                { id: 7, email: 'solo@starwars-team.ru', yandexuid: '123' },
                { id: 8, email: 'skywalker@starwars-team.ru', yandexuid: '124' },
            ];
            const items = _.range(1, 26)
                .map(num => ({
                    id: num,
                    eventId: { id: num, slug: `front-talks-2018-${num}`, title: 'front-talks' },
                    accountId: accounts[num % 2 === 0 ? 0 : 1],
                    answers: [],
                    createdAt: new Date(2018, 10, num),
                }));

            await factory.registration.create(items);

            const options = { pageSize: 3, pageNumber: 3, scope: 'page', accountId: 7, dbType: testDbType };
            const actual = await Registration.findPage(options);

            assert.strictEqual(actual.rows.length, 3);
            assert.strictEqual(actual.rows[0].id, 12);
            assert.strictEqual(actual.rows[1].id, 10);
            assert.strictEqual(actual.rows[2].id, 8);
            assert.strictEqual(actual.meta.totalSize, 12);
            assert.strictEqual(actual.meta.pageNumber, 3);
            assert.strictEqual(actual.meta.pageSize, 3);
        });

        it('should find registrations by eventId and filtered by condition', async() => {
            const filterParams = {
                and: [
                    {
                        type: 'string',
                        name: 'invitationStatus',
                        value: 'online',
                    },
                    {
                        type: 'string',
                        name: 'firstName',
                        value: 'solo',
                        parent: 'account',
                    },
                ],
            };

            const items = _.range(1, 26)
                .map(num => ({
                    id: num,
                    accountId: { id: num, email: `solo${num}@starwars-team.ru`, firstName: 'solo' },
                    answers: [],
                    createdAt: new Date(2018, 10, num),
                    invitationStatus: num > 22 ? 'online' : 'not_decided',
                }))
                .concat({
                    id: 100,
                    eventId: { id: 6, slug: 'front-talks-2019', title: 'front-talks' },
                    accountId: { id: 777, email: 'solo@starwars-team.ru' },
                });

            await factory.registration.create(items);

            const options = {
                pageSize: 10, pageNumber: 1, scope: 'page', eventId: 5, filterParams, dbType: testDbType,
            };
            const actual = await Registration.findPage(options);

            assert.strictEqual(actual.rows.length, 3);
            assert.strictEqual(actual.rows[0].id, 25);
            assert.strictEqual(actual.rows[1].id, 24);
            assert.strictEqual(actual.rows[2].id, 23);
            assert.strictEqual(actual.meta.totalSize, 3);
            assert.strictEqual(actual.meta.pageNumber, 1);
            assert.strictEqual(actual.meta.pageSize, 10);
        });
    });

    describe('findAll', () => {
        it('should find registrations without pagination by eventId', async() => {
            const items = _.range(1, 26)
                .map(num => ({
                    id: num,
                    answers: [],
                    createdAt: new Date(2018, 10, num),
                }))
                .concat({
                    id: 100,
                    eventId: { id: 6, slug: 'front-talks-2019', title: 'front-talks' },
                    accountId: { id: 777, email: 'solo@starwars-team.ru' },
                });

            await factory.registration.create(items);

            const options = { scope: 'list', eventId: 5, order: [['id', 'ASC']], dbType: testDbType };
            const actual = await Registration.findAll(options);

            assert.strictEqual(actual.length, 25);
            assert.strictEqual(actual[0].id, 1);
            assert.strictEqual(actual[24].id, 25);
        });
    });

    describe('delete', () => {
        it('should delete registration', async() => {
            await factory.registration.create({
                id: 4,
            });

            const deleted = await Registration.destroy(4, { dbType: testDbType });

            const registrationsCount = await db.registration.count();

            assert.ok(deleted);
            assert.strictEqual(registrationsCount, 0);
        });

        it('should throw on nonexistent event', async() => {
            const error = await catchErrorAsync(
                Registration.destroy.bind(Registration), 11, { dbType: testDbType },
            );

            assert.strictEqual(error.message, 'Registration not found');
            assert.strictEqual(error.statusCode, 404);
            assert.deepStrictEqual(error.options, {
                internalCode: '404_ENF',
                id: 11,
            });
        });
    });

    describe('batchDelete', () => {
        it('should delete registrations by ids', async() => {
            await factory.registration.create({
                id: 4,
                eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
                accountId: { id: 7, email: 'solo@starwars-team.ru' },
            });
            await factory.registration.create({
                id: 5,
                eventId: { id: 5, slug: 'front-talks-2018', title: 'front-talks' },
                accountId: { id: 8, email: 'luk@starwars-team.ru' },
            });

            const deleted = await Registration.batchDestroy([4, 5], { dbType: testDbType });

            const registrationsCount = await db.registration.count();

            assert.ok(deleted);
            assert.strictEqual(registrationsCount, 0);
        });

        it('should throw on empty conditions', async() => {
            const error = await catchErrorAsync(
                Registration.batchDestroy.bind(Registration), { dbType: testDbType },
            );

            assert.strictEqual(
                error.message,
                'batchDestroy should have one required argument: ids or options.where',
            );
        });
    });

    describe('getSchemaByEventId', () => {
        it('should return schema with custom fields', async() => {
            /* eslint-disable camelcase */

            const anotherEventId = { id: 6, slug: 'dev-fest-2018', title: 'dev-fest' };

            await factory.registration.create([
                {
                    answers: {
                        email: {
                            label: 'Email',
                            value: 'saaaaaaaaasha@yandex-team.ru',
                        },
                        answerChoices72343: {
                            label: 'Ваша специальность',
                            value: 'javascript, go',
                        },
                    },
                },
                {
                    answers: {
                        email: {
                            label: 'Email',
                            value: 'art00@yandex-team.ru',
                        },
                        firstName: {
                            label: 'Имя',
                            value: 'Александр',
                        },
                        lastName: {
                            label: 'Фамилия',
                            value: 'Иванов',
                        },
                    },
                },
                {
                    eventId: anotherEventId,
                    answers: {
                        answerChoices72344: {
                            label: 'Город',
                            value: 'Новосибирск',
                        },
                    },
                },
            ]);

            const actual = await Registration.getSchemaByEventId(5, testDbType);
            const expected = {
                ...defaultSchema,
                properties: {
                    ...defaultSchema.properties,
                    ...Registration.accountSchemaProps,
                    answerAnswerChoices72343: { title: 'Ваша специальность', type: 'string' },
                    answerEmail: { title: 'Email', type: 'string' },
                    answerFirstName: { title: 'Имя', type: 'string' },
                    answerLastName: { title: 'Фамилия', type: 'string' },
                },
            };

            assert.deepStrictEqual(_.omit(actual, 'sortableFields'), expected);
        });

        it('should return default schema if event hasn\'t registrations', async() => {
            await factory.event.create({ id: 5, slug: 'front-talks-2018', title: 'front-talks' });

            const actual = await Registration.getSchemaByEventId(5, testDbType);

            assert.deepStrictEqual(_.omit(actual, 'sortableFields'), {
                ...defaultSchema,
                properties: {
                    ...defaultSchema.properties,
                    ...Registration.accountSchemaProps,
                },
            });
        });

        it('should return default schema if event is not exists', async() => {
            const actual = await Registration.getSchemaByEventId(5, testDbType);

            assert.deepStrictEqual(_.omit(actual, 'sortableFields'), {
                ...defaultSchema,
                properties: {
                    ...defaultSchema.properties,
                    ...Registration.accountSchemaProps,
                },
            });
        });
    });
});
