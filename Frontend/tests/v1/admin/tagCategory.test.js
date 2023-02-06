const assert = require('assert');
const request = require('supertest');
const config = require('yandex-cfg');
const nock = require('nock');
const _ = require('lodash');

const { nockTvmtool, nockBlackbox } = require('tests/mocks');

const db = require('db');
const { DbType } = require('db/constants');
const app = require('app');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');

describe('Tag category controller', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('findOne', () => {
        it('should return a tag category', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const tagCategoryData = {
                id: 7,
                name: 'php',
                order: 0,
                createdAt: '2018-10-26T13:00:00.000Z',
            };

            const tagCategory = await factory.tagCategory.create(tagCategoryData);

            await request(app.listen())
                .get('/v1/admin/tagCategories/7')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect({
                    ...tagCategory.toJSON(),
                    dbType: DbType.internal,
                    createdAt: '2018-10-26T13:00:00.000Z',
                });
        });

        it('should throw error when tagCategoryId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/tagCategories/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'TagCategory ID is invalid',
                    value: 'inv@lid',
                });
        });
    });

    describe('findPage', () => {
        it('should return tag categories with default pagination and sort', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const data = _.range(1, 26)
                .map(num => ({ id: num, name: `#${num}`, order: num }));

            await factory.tagCategory.create(data);

            await request(app.listen())
                .get('/v1/admin/tagCategories')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.meta.totalSize, 25);
                    assert.strictEqual(body.meta.pageNumber, config.pagination.pageNumber);
                    assert.strictEqual(body.meta.pageSize, config.pagination.pageSize);
                    assert.strictEqual(body.rows.length, 20);
                    assert.strictEqual(body.rows[0].name, '#25');
                });
        });

        it('should return tag categories by pageSize and pageNumber', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const tagCategoriesData = _
                .range(1, 26)
                .map(num => ({ id: num, name: `#${num}`, order: num }));

            await factory.tagCategory.create(tagCategoriesData);

            await request(app.listen())
                .get('/v1/admin/tagCategories')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 3, pageSize: 3 })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.meta.totalSize, 25);
                    assert.strictEqual(body.meta.pageNumber, 3);
                    assert.strictEqual(body.meta.pageSize, 3);
                    assert.strictEqual(body.rows.length, 3);
                    assert.strictEqual(body.rows[0].name, '#19');
                });
        });

        it('should throw error if pageNumber is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/tagCategories')
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

        it('should return tag categories by custom sort', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const tagCategoriesData = _.range(1, 26)
                .map(num => ({ id: num, name: `#${num}`, order: num }));

            await factory.tagCategory.create(tagCategoriesData);

            await request(app.listen())
                .get('/v1/admin/tagCategories')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageSize: 5, sortBy: 'name', sortOrder: 'ASC' })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.rows.length, 5);
                    assert.strictEqual(body.rows[0].name, '#1');
                    assert.strictEqual(body.rows[1].name, '#10');
                });
        });

        it('should throw error if sortBy is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/tagCategories')
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
        it('should return created tag category', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const data = {
                name: 'php',
            };

            await request(app.listen())
                .post('/v1/admin/tagCategories')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.name, data.name);
                });

            const actual = await db.tagCategory.findAll();

            assert.strictEqual(actual.length, 1);
            assert.deepStrictEqual(actual[0].name, data.name);
        });

        it('should throw error when data is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/tagCategories')
                .send({ id: '13' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.strictEqual(body.errors[0].message, 'should have required property \'name\'');
                });
        });

        it('should throw error if user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/tagCategories')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    slug: 'dart',
                    name: 'dart',
                })
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
        it('should patch an tag category', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.tagCategory.create({ id: 13, name: 'tag' });

            const data = { id: 13, name: 'new tag' };

            await request(app.listen())
                .patch('/v1/admin/tagCategories')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.id, 13);
                    assert.strictEqual(body.name, 'new tag');
                });

            const actual = await db.tagCategory.findAll();

            assert.strictEqual(actual.length, 1);
            assert.strictEqual(actual[0].id, 13);
            assert.strictEqual(actual[0].name, 'new tag');
        });

        it('should throw error when tagCategoryId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .patch('/v1/admin/tagCategories')
                .send({ id: 'inv@lid' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'TagCategory ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when tag category is not found', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.tag.create({ id: 13, slug: 'tag', name: 'tag' });

            await request(app.listen())
                .patch('/v1/admin/tagCategories')
                .send({ id: 23, slug: 'slugslug' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(404)
                .expect(({ body }) => {
                    assert.strictEqual(body.message, 'Tag category not found');
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .patch('/v1/admin/tagCategories')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    name: 'Totally wrong',
                })
                .expect('Content-Type', /json/)
                .expect(403)
                .expect({
                    message: 'User has no access',
                    internalCode: '403_UNA',
                    login: 'solo',
                });
        });
    });

    describe('destroy', () => {
        it('should destroy a tag category by id', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.tagCategory.create({
                id: 7,
                name: 'php',
            });

            await request(app.listen())
                .delete('/v1/admin/tagCategories/7')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(204);
        });

        it('should throw error when tagCategoryId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .delete('/v1/admin/tagCategories/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'TagCategory ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .delete('/v1/admin/tagCategories/1')
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
        it('should response tag categories contains text', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await factory.tagCategory.create([
                {
                    id: 1,
                    name: 'котлин',
                    order: 5,
                    createdAt: '2018-10-26T13:00:00.000Z',
                },
                {
                    id: 2,
                    name: 'эндпоинт',
                    order: 1,
                    createdAt: '2018-10-26T13:00:00.000Z',
                },
                {
                    id: 3,
                    name: 'фронтэнд',
                    order: 5,
                    createdAt: '2018-10-26T13:00:00.000Z',
                },
                {
                    id: 4,
                    name: 'бэкенд',
                    createdAt: '2018-10-26T13:00:00.000Z',
                },
            ]);

            await request(app.listen())
                .get('/v1/admin/tagCategories/suggest' +
                    '?text=%D1%8D%D0%BD%D0%B4' + // Text=энд
                    '&limit=2')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect([
                    {
                        id: 2,
                        name: 'эндпоинт',
                        order: 1,
                        createdAt: '2018-10-26T13:00:00.000Z',
                    },
                    {
                        id: 3,
                        name: 'фронтэнд',
                        order: 5,
                        createdAt: '2018-10-26T13:00:00.000Z',
                    },
                ]);
        });

        it('should limit by default value', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const data = _
                .range(15)
                .map(i => ({ name: `эндотег #${i}` }));

            await factory.tagCategory.create(data);
            await request(app.listen())
                .get('/v1/admin/tagCategories/suggest' +
                    '?text=%D1%8D%D0%BD%D0%B4') // Text=энд
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.strictEqual(body.length, 10);
                });
        });

        it('should return 400 when limit is invalid', async() => {
            nockBlackbox();
            nockTvmtool();

            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/tagCategories/suggest?text=tv&limit=abc')
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
