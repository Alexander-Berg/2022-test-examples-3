const assert = require('assert');
const request = require('supertest');
const config = require('yandex-cfg');
const nock = require('nock');
const moment = require('moment');
const _ = require('lodash');

const { nockTvmtool, nockBlackbox } = require('tests/mocks');

const db = require('db');
const { DbType } = require('db/constants');
const app = require('app');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');

describe('Tag controller', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('findOne', () => {
        it('should return a tag', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const tagData = {
                id: 7,
                slug: 'php',
                name: 'php',
                description: 'not bad',
                isPublished: true,
                isVisibleInCatalog: false,
                order: 0,
                type: 1,
                createdAt: '2018-10-26T13:00:00.000Z',
            };

            const tag = await factory.tag.create(tagData);

            await request(app.listen())
                .get('/v1/admin/tags/7')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect({
                    ...tag.toJSON(),
                    createdAt: '2018-10-26T13:00:00.000Z',
                    category: null,
                    events: [],
                    dbType: DbType.internal,
                });
        });

        it('should return a tag with events', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const tagData = {
                id: 7,
                slug: 'php',
                name: 'php',
                createdAt: '2018-10-26T13:00:00.000Z',
            };

            const eventsData = [
                { id: 1, slug: 'codefest', title: 'codefest' },
                { id: 2, slug: 'highload', title: 'highload' },
            ];

            const events = await factory.event.create(eventsData);
            const tag = await factory.tag.create(tagData);

            await tag.addEvents(events);

            await request(app.listen())
                .get('/v1/admin/tags/7')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect({
                    ...tag.toJSON(),
                    category: null,
                    createdAt: '2018-10-26T13:00:00.000Z',
                    events: eventsData,
                    dbType: DbType.internal,
                });
        });

        it('should throw error when tagId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/tags/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Tag ID is invalid',
                    value: 'inv@lid',
                });
        });
    });

    describe('findPage', () => {
        it('should return tags with default pagination and sort', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const tagsData = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}` }));

            await factory.tag.create(tagsData);

            await request(app.listen())
                .get('/v1/admin/tags')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.meta.totalSize, 25);
                    assert.equal(body.meta.pageNumber, config.pagination.pageNumber);
                    assert.equal(body.meta.pageSize, config.pagination.pageSize);
                    assert.equal(body.rows.length, 20);
                    assert.equal(body.rows[0].slug, '#25');
                });
        });

        it('should return tags by pageSize and pageNumber', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const tagsData = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}` }));

            await factory.tag.create(tagsData);

            await request(app.listen())
                .get('/v1/admin/tags')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageNumber: 3, pageSize: 3 })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.meta.totalSize, 25);
                    assert.equal(body.meta.pageNumber, 3);
                    assert.equal(body.meta.pageSize, 3);
                    assert.equal(body.rows.length, 3);
                    assert.equal(body.rows[0].slug, '#19');
                });
        });

        it('should throw error if pageNumber is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/tags')
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

        it('should return tags by custom sort', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const tagsData = _.range(1, 26)
                .map(num => ({ id: num, slug: `#${num}` }));

            await factory.tag.create(tagsData);

            await request(app.listen())
                .get('/v1/admin/tags')
                .set('Cookie', ['Session_id=user-session-id'])
                .query({ pageSize: 5, sortBy: 'slug', sortOrder: 'ASC' })
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.rows.length, 5);
                    assert.equal(body.rows[0].slug, '#1');
                    assert.equal(body.rows[1].slug, '#10');
                });
        });

        it('should throw error if sortBy is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/tags')
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
        it('should return created tag', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            const category = await factory.tagCategory.create({ id: 2 });

            const data = {
                slug: 'php',
                name: 'php',
                categoryId: 2,
                category,
            };

            await request(app.listen())
                .post('/v1/admin/tags')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.slug, data.slug);
                    assert.equal(body.name, data.name);
                });

            const actual = await db.tag.findAll();

            assert.equal(actual.length, 1);
            assert.deepStrictEqual(actual[0].slug, data.slug);
        });

        it('should throw error when data is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/tags')
                .send({ id: '13' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.equal(body.errors[0].message, 'should have required property \'slug\'');
                });
        });

        it('should throw error if user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/tags')
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
        it('should patch an tag', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.tag.create({ id: 13, slug: 'tag', name: 'tag' });

            const category = await factory.tagCategory.create({ id: 2 });

            const data = { id: 13, name: 'new tag', categoryId: 2, category };

            await request(app.listen())
                .patch('/v1/admin/tags')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.id, 13);
                    assert.equal(body.slug, 'tag');
                    assert.equal(body.name, 'new tag');
                });

            const actual = await db.tag.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].id, 13);
            assert.equal(actual[0].name, 'new tag');
        });

        it('should throw error when tagId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .patch('/v1/admin/tags')
                .send({ id: 'inv@lid' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Tag ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when tag is not found', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.tag.create({ id: 13, slug: 'tag', name: 'tag' });

            const category = await factory.tagCategory.create({ id: 2 });

            await request(app.listen())
                .patch('/v1/admin/tags')
                .send({ id: 23, slug: 'slugslug', categoryId: 2, category })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(404)
                .expect(({ body }) => {
                    assert.equal(body.message, 'Tag not found');
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .patch('/v1/admin/tags')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    slug: 'wrong',
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
        it('should destroy a tag by id', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.tag.create({
                id: 7,
                slug: 'php',
                name: 'php',
            });

            await request(app.listen())
                .delete('/v1/admin/tags/7')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect(204);
        });

        it('should throw error when tagId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .delete('/v1/admin/tags/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .send()
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Tag ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .delete('/v1/admin/tags/1')
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
        it('should response tags contains text', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await factory.tag.create([
                {
                    id: 1,
                    name: 'котлин',
                    slug: 'kotlin',
                    createdAt: '2018-10-26T13:00:00.000Z',
                },
                {
                    id: 2,
                    name: 'эндпоинт',
                    slug: 'endpoint',
                    createdAt: '2018-10-26T13:00:00.000Z',
                },
                {
                    id: 3,
                    name: 'фронтэнд',
                    slug: 'frontend',
                    createdAt: '2018-10-26T13:00:00.000Z',
                },
                {
                    id: 4,
                    name: 'бэкенд',
                    slug: 'backend',
                    createdAt: '2018-10-26T13:00:00.000Z',
                },
            ]);

            await request(app.listen())
                .get('/v1/admin/tags/suggest' +
                    '?text=%D1%8D%D0%BD%D0%B4' + // Text=энд
                    '&limit=2')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect([
                    {
                        id: 2,
                        name: 'эндпоинт',
                        slug: 'endpoint',
                        createdAt: '2018-10-26T13:00:00.000Z',

                        description: 'Awesome programming language',
                        isPublished: true,
                        isVisibleInCatalog: false,
                        order: 0,
                        type: 1,
                        categoryId: null,
                    },
                    {
                        id: 3,
                        name: 'фронтэнд',
                        slug: 'frontend',
                        createdAt: '2018-10-26T13:00:00.000Z',

                        description: 'Awesome programming language',
                        isPublished: true,
                        isVisibleInCatalog: false,
                        order: 0,
                        type: 1,
                        categoryId: null,
                    },
                ]);
        });

        it('should limit by default value', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const tags = _
                .range(15)
                .map(i => ({ name: `эндотег #${i}`, slug: `slug-${i}` }));

            await factory.tag.create(tags);
            await request(app.listen())
                .get('/v1/admin/tags/suggest' +
                    '?text=%D1%8D%D0%BD%D0%B4') // Text=энд
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
                .get('/v1/admin/tags/suggest?text=tv&limit=abc')
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

    describe('exportSubscribers', () => {
        it('should return tag subscribers', async() => {
            nockBlackbox();
            nockTvmtool();

            const commonAccountData = {
                birthDate: new Date(),
                isEmailConfirmed: true,
                phone: 'phone',
                city: 'city',
                website: 'website',
                blog: 'blog',
                firstName: 'Энакин',
                lastName: 'Скайуокер',
                middleName: 'Петрович',
                about: 'about',
                jobPlace: 'jobPlace',
                jobPosition: 'jobPosition',
                studyPlace: 'studyPlace',
            };
            const accounts = [{
                id: 1,
                sex: 'man',
                email: 'darkside1@yandex.ru',
                yandexuid: '1518583433759763986',
                login: 'darkside1',
                ...commonAccountData,
            }, {
                id: 2,
                sex: 'man',
                email: 'darkside2@yandex.ru',
                yandexuid: '2518583433759763986',
                login: 'darkside2',
                ...commonAccountData,
            }, {
                id: 3,
                sex: 'man',
                email: 'darkside3@yandex.ru',
                yandexuid: '3518583433759763986',
                login: 'darkside3',
                ...commonAccountData,
            }, {
                id: 3,
                sex: 'man',
                email: 'darkside3@yandex.ru',
                yandexuid: '3518583433759763986',
                login: 'darkside3',
                ...commonAccountData,
                isEmailConfirmed: false,
            }];
            const formattedDate = moment().format('DDMMYYYY');
            const fileName = `1-subscribers-${formattedDate}.xlsx`;

            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.tag.create([{ id: 1, slug: '1' }, { id: 2, slug: '2' }]);
            await factory.account.create(accounts);
            await factory.subscription.create([
                { tagId: 1, accountId: 1 },
                { tagId: 1, accountId: 3 },
                { tagId: 2, accountId: 2 },
                { tagId: 1, accountId: 2, isActive: false },
            ]);

            await request(app.listen())
                .get('/v1/admin/tags/1/subscribers')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')
                .expect('Content-Disposition', `attachment; filename="${fileName}"`)
                .expect(200)
                .expect(({ buffered }) => {
                    assert.equal(buffered, true);
                });
        });

        it('should throw 404 error when tag doesn\'t exists', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/tags/1/subscribers')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(404)
                .expect({
                    internalCode: '404_ENF',
                    message: 'Tag not found',
                    id: '1',
                    dbType: DbType.internal,
                });
        });

        it('should throw 400 error when tagId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/tags/inv@lid/subscribers')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'Tag ID is invalid',
                    value: 'inv@lid',
                });
        });
    });
});
