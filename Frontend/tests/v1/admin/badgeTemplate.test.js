const assert = require('assert');
const request = require('supertest');
const nock = require('nock');

const { nockTvmtool, nockBlackbox } = require('tests/mocks');

const app = require('app');
const { DbType } = require('db/constants');
const db = require('db');
const factory = require('tests/db/factory');
const cleanDb = require('tests/db/clean');

const testDbType = DbType.internal;

describe('BadgeTemplate controller', () => {
    beforeEach(cleanDb);
    afterEach(nock.cleanAll);

    describe('findById', () => {
        it('should find badge template', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const date = '2018-12-13T11:20:25.581Z';
            const data = {
                id: 4,
                slug: 'tpl1',
                createdAt: date,
                updatedAt: date,
            };

            await factory.badgeTemplate.create(data);

            await request(app.listen())
                .get('/v1/admin/badgeTemplates/4')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect({
                    ...data,
                    data: factory.badgeTemplate.defaultData.data,
                    createdByLogin: 'art00',
                    updatedByLogin: 'art00',
                    dbType: DbType.internal,
                });
        });

        it('should throw error when badge template not found', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/badgeTemplates/5')
                .set('Cookie', 'Session_id=user-session-id')
                .expect(404)
                .expect({
                    message: 'Badge template not found',
                    id: '5',
                    internalCode: '404_ENF',
                    dbType: testDbType,
                });
        });

        it('should throw error when badgeTemplateId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .get('/v1/admin/badgeTemplates/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'BadgeTemplate ID is invalid',
                    value: 'inv@lid',
                });
        });
    });

    describe('findAll', () => {
        it('should find all badge templates', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const data = [
                {
                    id: 4,
                    slug: 'tpl1',
                },
                {
                    id: 5,
                    slug: 'tpl2',
                },
                {
                    id: 6,
                    slug: 'tpl3',
                },

            ];

            await factory.badgeTemplate.create(data);

            await request(app.listen())
                .get('/v1/admin/badgeTemplates')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body[0].id, 6);
                    assert.equal(body[0].slug, 'tpl3');
                    assert.deepEqual(body[0].data, factory.badgeTemplate.defaultData.data);
                    assert.equal(body[1].id, 5);
                    assert.equal(body[1].slug, 'tpl2');
                    assert.deepEqual(body[1].data, factory.badgeTemplate.defaultData.data);
                    assert.equal(body[2].id, 4);
                    assert.equal(body[2].slug, 'tpl1');
                    assert.deepEqual(body[2].data, factory.badgeTemplate.defaultData.data);
                });
        });
    });

    describe('Create', () => {
        it('should create an badge template', async() => {
            nockBlackbox('solo');
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const content = [{ test: 1 }];
            const data = {
                slug: 'tpl',
                data: content,
            };

            await request(app.listen())
                .post('/v1/admin/badgeTemplates')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.slug, 'tpl');
                    assert.equal(body.createdByLogin, 'solo');
                    assert.deepEqual(body.data, content);
                });

            const actual = await db.badgeTemplate.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].slug, data.slug);
        });

        it('should throw error when data is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .post('/v1/admin/badgeTemplates')
                .send({ id: 13 })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.equal(body.errors[0].message, 'should have required property \'slug\'');
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .post('/v1/admin/badgeTemplates')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    slug: 'wrong',
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

    describe('delete', () => {
        it('should delete an badge template', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            const data = {
                id: 11,
                slug: 'tpl',
                data: [],
            };

            await db.badgeTemplate.create(data);

            await request(app.listen())
                .delete('/v1/admin/badgeTemplates/11')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect(204)
                .expect({});
        });

        it('should throw error when badgeTemplateId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .delete('/v1/admin/badgeTemplates/inv@lid')
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'BadgeTemplate ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .delete('/v1/admin/badgeTemplates/11')
                .set('Cookie', ['Session_id=user-session-id'])
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
        it('should patch an event', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });
            await factory.badgeTemplate.create({ id: 13, slug: 'tpl' });

            const content = [{ test: 1 }];
            const data = { id: 13, data: content };

            await request(app.listen())
                .patch('/v1/admin/badgeTemplates')
                .send(data)
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(200)
                .expect(({ body }) => {
                    assert.equal(body.id, 13);
                    assert.equal(body.slug, 'tpl');
                    assert.deepEqual(body.data, content);
                });

            const actual = await db.badgeTemplate.findAll();

            assert.equal(actual.length, 1);
            assert.equal(actual[0].id, 13);
            assert.deepEqual(actual[0].data, content);
        });

        it('should throw error when badgeTemplateId is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .patch('/v1/admin/badgeTemplates')
                .send({ id: 'inv@lid' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect({
                    internalCode: '400_III',
                    message: 'BadgeTemplate ID is invalid',
                    value: 'inv@lid',
                });
        });

        it('should throw error when data is invalid', async() => {
            nockBlackbox();
            nockTvmtool();
            await factory.userRole.create({ role: 'admin', login: 'solo' });

            await request(app.listen())
                .patch('/v1/admin/badgeTemplates')
                .send({ id: 13, data: 'not an array value' })
                .set('Cookie', ['Session_id=user-session-id'])
                .expect('Content-Type', /json/)
                .expect(400)
                .expect(({ body }) => {
                    assert.equal(body.errors[0].message, 'should be array');
                });
        });

        it('should throw error user has no access', async() => {
            nockBlackbox();
            nockTvmtool();

            await request(app.listen())
                .patch('/v1/admin/badgeTemplates')
                .set('Cookie', ['Session_id=user-session-id'])
                .send({
                    slug: 'wrong',
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
});
