const assert = require('assert');
const nock = require('nock');
const _ = require('lodash');
const shortid = require('shortid');
const request = require('supertest');
const config = require('yandex-cfg');

const { nockTvmtool, nockBlackbox } = require('tests/mocks');

const app = require('app');
const db = require('db');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');

describe('Account controller', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('findOne', () => {
        it('should return an account', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

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

            await request(app.listen())
                .get('/v1/admin/accounts/11')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.id, data.id);
                    assert.equal(body.yandexuid, data.yandexuid);
                    assert.equal(body.email, data.email);
                    assert.equal(body.isEmailConfirmed, false);
                });
        });

        it('should throw error when accountId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/accounts/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Account ID is invalid',
                    value: 'inv@lid',
                });
        });
    });

    describe('findPage', () => {
        it('should return accounts with default pagination and sort', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const items = _.range(1, 26)
                .map(num => ({
                    id: num,
                    yandexuid: shortid.generate(),
                    email: `test${num}@yandex.ru`,
                }));

            await factory.account.create(items);

            await request(app.listen())
                .get('/v1/admin/accounts')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.meta.totalSize, 25);
                    assert.equal(body.meta.pageNumber, config.pagination.pageNumber);
                    assert.equal(body.meta.pageSize, config.pagination.pageSize);
                    assert.equal(body.rows.length, 20);
                    assert.equal(body.rows[0].email, 'test25@yandex.ru');
                });
        });

        it('should return accounts by pageSize and pageNumber', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const items = _.range(1, 26)
                .map(num => ({
                    id: num,
                    yandexuid: shortid.generate(),
                    email: `test${num}@yandex.ru`,
                }));

            await factory.account.create(items);

            await request(app.listen())
                .get('/v1/admin/accounts')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 3, pageSize: 3 })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.meta.totalSize, 25);
                    assert.equal(body.meta.pageNumber, 3);
                    assert.equal(body.meta.pageSize, 3);
                    assert.equal(body.rows.length, 3);
                    assert.equal(body.rows[0].email, 'test19@yandex.ru');
                });
        });

        it('should throw error if pageNumber is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/accounts')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 'inv@lid' })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_PII',
                    message: 'Page number is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error if sortBy is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/accounts')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ sortBy: 'wrongField' })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_SNI',
                    message: 'Parameter sortBy is not allowed in SortableFields',
                    value: 'wrongField',
                });
        });
    });

    describe('Create', () => {
        it('should return created account', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const data = {
                yandexuid: '1518583433759763986',
                birthDate: '1940-12-12',
                email: 'darkside@yandex.ru',
                firstName: 'Энакин',
                lastName: 'Скайуокер',
                isEmailConfirmed: false,
            };

            await request(app.listen())
                .post('/v1/admin/accounts')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.firstName, data.firstName);
                    assert.equal(body.yandexuid, data.yandexuid);
                });

            const actual = await db.account.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].firstName, data.firstName);
        });

        it('should throw error when data is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const data = {
                yandexuid: false,
                email: 'darkside@yandex.ru',
                firstName: 'Энакин',
                lastName: 'Скайуокер',
            };

            await request(app.listen())
                .post('/v1/admin/accounts')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.equal(body.errors[0].message, 'should be string,null');
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/accounts')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({})
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'solo',
                });
        });
    });

    describe('Patch', () => {
        it('should patch a account', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const accountData = {
                id: 5,
                email: 'darkside@yandex.ru',
                firstName: 'Энакин',
                lastName: 'Скайуокер',
            };

            await factory.account.create(accountData);

            await request(app.listen())
                .patch('/v1/admin/accounts')
                .send({ id: 5, lastName: 'Test', yandexuid: '12345' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.id, accountData.id);
                    assert.equal(body.firstName, accountData.firstName);
                    assert.equal(body.lastName, 'Test');
                    assert.equal(body.yandexuid, '12345');
                });

            const actual = await db.account.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].id, accountData.id);
            assert.equal(actual[0].yandexuid, '12345');
        });

        it('should throw error when accountId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .patch('/v1/admin/accounts')
                .send({ id: 'inv@lid' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Account ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when data is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const accountData = {
                id: 5,
                email: 'darkside@yandex.ru',
                firstName: 'Энакин',
                lastName: 'Скайуокер',
            };

            await factory.account.create(accountData);

            await request(app.listen())
                .patch('/v1/admin/accounts')
                .send({ id: 5, email: 'invalid' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.equal(body.errors[0].message, 'should match format "email"');
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .patch('/v1/admin/accounts')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({})
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'solo',
                });
        });
    });

    describe('Destroy', () => {
        it('should destroy account by id', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.account.create({
                id: 7,
                yandexuid: '123123123',
                email: 'mail@ya.ru',
                firstName: 'asd',
                lastName: 'dsa',
            });

            await request(app.listen())
                .delete('/v1/admin/accounts/7')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(204);
        });

        it('should throw error when accountId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .delete('/v1/admin/accounts/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Account ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .delete('/v1/admin/accounts/1')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'solo',
                });
        });
    });

    describe('findMailPage', () => {
        it('should return mails with default pagination and sort', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const items = _.range(1, 36)
                .map(num => ({
                    id: num,
                    accountId: { id: num > 10 ? 33 : 44, email: `m${num}@yandex.ru` },
                }));

            await factory.accountMail.create(items);

            await request(app.listen())
                .get('/v1/admin/accounts/33/mails')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.meta.totalSize, 25);
                    assert.equal(body.meta.pageNumber, config.pagination.pageNumber);
                    assert.equal(body.meta.pageSize, config.pagination.pageSize);
                    assert.equal(body.rows.length, 20);
                    assert.equal(body.rows[0].id, 35);
                    assert.equal(body.rows[0].title, 'Вы приглашены на галактическое мероприятие');
                });
        });

        it('should return mails only for one user', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.accountMail.create([
                { id: 1, accountId: { id: 1, email: 'user1@yandex.ru' } },
                { id: 2, accountId: { id: 1, email: 'user1@yandex.ru' } },
                { id: 3, accountId: { id: 2, email: 'user2@yandex.ru' } },
            ]);
            await request(app.listen())
                .get('/v1/admin/accounts/1/mails')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.meta.totalSize, 2);
                });
        });

        it('should return mails by pageSize and pageNumber', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const items = _.range(1, 26)
                .map(num => ({
                    id: num,
                    accountId: { id: num > 10 ? 33 : 44, email: `m${num}@yandex.ru` },
                }));

            await factory.accountMail.create(items);

            await request(app.listen())
                .get('/v1/admin/accounts/33/mails')
                .query({ pageNumber: 3, pageSize: 3 })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.meta.totalSize, 15);
                    assert.equal(body.meta.pageNumber, 3);
                    assert.equal(body.meta.pageSize, 3);
                    assert.equal(body.rows.length, 3);
                    assert.equal(body.rows[0].id, 19);
                    assert.equal(body.rows[0].title, 'Вы приглашены на галактическое мероприятие');
                    assert.equal(body.rows[1].id, 18);
                    assert.equal(body.rows[2].id, 17);
                });
        });

        it('should return mails by filters and sort', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const now = Date.now();
            const items = _.range(1, 26)
                .map(num => ({
                    id: num,
                    accountId: { id: num < 10 ? 33 : 44, email: `m${num}@yandex.ru` },
                    wasSent: num < 4,
                    sentAt: new Date(now - num * 1000),
                }));

            await factory.accountMail.create(items);

            await request(app.listen())
                .get('/v1/admin/accounts/33/mails')
                .query({ filters: JSON.stringify({ and: [{ wasSent: 'true' }] }), sortBy: 'sentAt', sortOrder: 'DESC' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.meta.totalSize, 3);
                    assert.equal(body.rows.length, 3);
                    assert.equal(body.rows[0].id, 1);
                    assert.equal(body.rows[0].title, 'Вы приглашены на галактическое мероприятие');
                    assert.equal(body.rows[1].id, 2);
                    assert.equal(body.rows[2].id, 3);
                });
        });

        it('should throw error if accountId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/accounts/inv@lid/mails')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Account ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error if pageNumber is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/accounts/33/mails')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 'inv@lid' })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_PII',
                    message: 'Page number is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error if sortBy is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/accounts/33/mails')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ sortBy: 'wrongField' })
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_SNI',
                    message: 'Parameter sortBy is not allowed in SortableFields',
                    value: 'wrongField',
                });
        });
    });
});
