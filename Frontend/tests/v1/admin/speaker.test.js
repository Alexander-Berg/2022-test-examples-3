const assert = require('assert');
const nock = require('nock');
const _ = require('lodash');
const request = require('supertest');
const config = require('yandex-cfg');

const { nockTvmtool, nockBlackbox } = require('tests/mocks');

const app = require('app');
const db = require('db');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');

describe('Speaker controller', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('findOne', () => {
        it('should return an speaker', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const data = {
                id: 1,
                email: 'darkside@yandex.ru',
                firstName: 'Энакин',
                lastName: 'Скайуокер',
            };

            await factory.speaker.create(data);

            await request(app.listen())
                .get('/v1/admin/speakers/1')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.id, data.id);
                    assert.equal(body.email, data.email);
                });
        });

        it('should throw error when speakerId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/speakers/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Speaker ID is invalid',
                    value: 'inv@lid',
                });
        });
    });

    describe('findPage', () => {
        it('should return speakers with default pagination and sort', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const items = _.range(1, 26)
                .map(num => ({
                    id: num,
                    email: `test${num}@yandex.ru`,
                    firstName: 'Энакин',
                    lastName: 'Скайуокер',
                }));

            await factory.speaker.create(items);

            await request(app.listen())
                .get('/v1/admin/speakers')
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

        it('should return speakers by pageSize and pageNumber', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const items = _.range(1, 26)
                .map(num => ({
                    id: num,
                    email: `test${num}@yandex.ru`,
                }));

            await factory.speaker.create(items);

            await request(app.listen())
                .get('/v1/admin/speakers')
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
                .get('/v1/admin/speakers')
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
                .get('/v1/admin/speakers')
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
        it('should return created speaker', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const data = {
                email: 'darkside@yandex.ru',
                firstName: 'Энакин',
                lastName: 'Скайуокер',
            };

            await request(app.listen())
                .post('/v1/admin/speakers')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.firstName, data.firstName);
                });

            const actual = await db.speaker.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].firstName, data.firstName);
        });

        it('should throw error when data is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const data = {
                email: 'darkside@yandex.ru',
                firstName: 'Энакин',
                lastName: 2,
            };

            await request(app.listen())
                .post('/v1/admin/speakers')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.equal(body.errors[0].message, 'should be string');
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/speakers')
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
        it('should patch a speaker', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const speakerData = {
                id: 5,
                email: 'darkside@yandex.ru',
                firstName: 'Энакин',
                lastName: 'Скайуокер',
            };

            await factory.speaker.create(speakerData);

            await request(app.listen())
                .patch('/v1/admin/speakers')
                .send({ id: 5, lastName: 'Test' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.id, speakerData.id);
                    assert.equal(body.firstName, speakerData.firstName);
                    assert.equal(body.lastName, 'Test');
                });

            const actual = await db.speaker.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].id, speakerData.id);
        });

        it('should throw error when speakerId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .patch('/v1/admin/speakers')
                .send({ id: 'inv@lid' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Speaker ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when data is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const speakerData = {
                id: 5,
                email: 'darkside@yandex.ru',
                firstName: 'Энакин',
                lastName: 'Скайуокер',
            };

            await factory.speaker.create(speakerData);

            await request(app.listen())
                .patch('/v1/admin/speakers')
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
                .patch('/v1/admin/speakers')
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
        it('should destroy speaker by id', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.speaker.create({
                id: 7,
                email: 'mail@ya.ru',
                firstName: 'asd',
                lastName: 'dsa',
            });

            await request(app.listen())
                .delete('/v1/admin/speakers/7')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(204);
        });

        it('should throw error when speakerId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .delete('/v1/admin/speakers/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Speaker ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .delete('/v1/admin/speakers/1')
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

    describe('suggest', () => {
        it('should response speakers contains text', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await factory.speaker.create([
                {
                    id: 1,
                    firstName: 'Энакин',
                    lastName: 'Скайуокер',
                    email: 'q1@ya.ru',
                },
                {
                    id: 2,
                    firstName: 'Люк',
                    lastName: 'Скайуокер',
                    email: 'q2@ya.ru',
                },
                {
                    id: 3,
                    firstName: 'Хан',
                    lastName: 'Соло',
                    email: 'q3@ya.ru',
                },
                {
                    id: 4,
                    firstName: 'Ещё',
                    lastName: 'Каййй',
                    email: 'q4@ya.ru',
                },
            ]);

            await request(app.listen())
                .get('/v1/admin/speakers/suggest' +
                    '?text=%D0%BA%D0%B0%D0%B9' + // Text=кай
                    '&limit=2')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.length, 2);
                    assert.deepStrictEqual(_.pick(body[0], ['id', 'firstName', 'lastName']), {
                        id: 4,
                        firstName: 'Ещё',
                        lastName: 'Каййй',
                    });
                    assert.deepStrictEqual(_.pick(body[1], ['id', 'firstName', 'lastName']), {
                        id: 2,
                        firstName: 'Люк',
                        lastName: 'Скайуокер',
                    });
                });
        });

        it('should limit by default value', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const speakers = _
                .range(15)
                .map(i => ({ firstName: `кай_${i}`, email: `q${i}@ya.ru` }));

            await factory.speaker.create(speakers);

            await request(app.listen())
                .get('/v1/admin/speakers/suggest' +
                    '?text=%D0%BA%D0%B0%D0%B9')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.length, 10);
                });
        });

        it('should return 400 when limit is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/speakers/suggest?text=tv&limit=abc')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    message: 'Limit is invalid',
                    value: 'abc',
                    internalCode: '400_PII',
                });
        });
    });
});
